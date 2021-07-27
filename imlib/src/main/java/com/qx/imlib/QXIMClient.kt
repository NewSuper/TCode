package com.qx.imlib

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import android.text.TextUtils
import android.util.Log
import com.qx.im.model.RTCServerConfig
import com.qx.im.model.SearchConversationResult
import com.qx.imlib.netty.HeartBeatHolder
import com.qx.imlib.netty.HeartBeatTimeCheck
import com.qx.imlib.netty.ReConnectManager
import com.qx.imlib.qlog.QLog
import com.qx.imlib.qlog.QLogTrace
import com.qx.imlib.utils.*
import com.qx.imlib.utils.event.HeartBeatEvent
import com.qx.imlib.utils.event.ReconnectEvent
import com.qx.imlib.utils.file.BitmapUtil
import com.qx.imlib.utils.file.FileUtil
import com.qx.imlib.utils.net.NetStateUtils
import com.qx.imlib.utils.net.NetWorkMonitorManager
import com.qx.imlib.utils.net.NetWorkState
import com.qx.message.*
import com.qx.message.Message
import com.qx.message.rtc.*
import com.qx.push.PushManager
import com.qx.push.QXPushClient
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.lang.Exception

class QXIMClient private constructor() {
    @Volatile
    var mToken: String? = null
    private var mImServerUrl: String = ""
    private var mCheckSendingMessageListener: CheckSendingMessageListener? = null
    private var mMessageReceiptListener: MessageReceiptListener? = null
    private var mOnMessageReceiveListener: OnMessageReceiveListener? = null
    private var mOnChatNoticeReceivedListenerList = arrayListOf<OnChatNoticeReceivedListener>()
    private var mConversationListener: ConversationListener? = null
    private var mOnChatRoomMessageReceiveListener: OnChatRoomMessageReceiveListener? = null
    private var mLibHandler: IHandler? = null
    private var mContext: Context? = null
    private var mAppKey: String? = null
    private val mAidlConnection: AidlConnection
    private val mWorkHandler: Handler
    private var mConnectRunnable: ConnectRunnable? = null
    private var mConnectCallBack: ConnectCallBack? = null
    private var isNetworkAvailable = false
    private var topForegroundActivity: Activity? = null
    private var mCurrentUserId: String? = null
    private var isDeadNotConnect = false
    var mConnectStatus: ConnectionStatusListener.Status? = null


    private object Holder {
        var instance = QXIMClient()
    }

    companion object {
        private val TAG = QXIMClient::class.java.simpleName
        private var mHandler: Handler? = null

        @Volatile
        private var needCallBackDBOpen = false
        private var isInForeground = false
        private var mConnectionStatusListener: ConnectionStatusListener? = null

        @JvmStatic
        val instance: QXIMClient
            get() = Holder.instance

        @JvmStatic
        fun init(context: Context, appKey: String, imServerUrl: String) {
            Holder.instance.initSdk(context, appKey, imServerUrl)
        }

        @JvmStatic
        fun connect(token: String, callBack: ConnectCallBack?) {
            Holder.instance.mToken = token
            if (token.isNullOrEmpty()) {
                QLog.e(TAG, "token 不能为空")
            } else {
                needCallBackDBOpen = true
                Holder.instance.mConnectCallBack = object : ConnectCallBack() {
                    override fun onSuccess(result: String?) {
                        callBack?.onSuccess(result)
                    }

                    override fun onError(errorCode: String?) {
                        callBack?.onError(errorCode)
                    }

                    override fun onDatabaseOpened(code: Int) {
                        callBack?.onDatabaseOpened(code)
                        needCallBackDBOpen = false
                    }
                }
                instance.connectServer(token)
            }
        }
    }

    init {
        mAidlConnection = AidlConnection()
        mHandler = Handler(Looper.getMainLooper())
        val workThread = HandlerThread("IPC_WORK")
        workThread.start()
        mWorkHandler = Handler(workThread.looper)
    }

    private fun initSdk(context: Context, appkey: String, imServerUrl: String) {
        mImServerUrl = imServerUrl
        val currentProcess = SystemUtil.getCurrentProcessName(context)
        val mainProcess = context.packageName
        mAppKey = appkey
        QLog.d(TAG, " initSdk appKey:$appkey")
        if (!TextUtils.isEmpty(currentProcess) && !TextUtils.isEmpty(mainProcess)) {
            mConnectStatus = ConnectionStatusListener.Status.UNCONNECT
            mContext = context.applicationContext
            QLog.init(context, appkey, "1.0")
            QLogTrace.instance.initLogThread(appkey)
            QLog.e(TAG, " mainProcess initSdk appkey:$mAppKey start initBindService")
            initBindService()
            if (context is Application) {
                NetWorkMonitorManager.getInstance().init(context as Application)
                NetWorkMonitorManager.getInstance().register(this)
            }
            QXPushClient.init(context, appkey)
            ContextUtils.getInstance().cacheApplicationContext(mContext)
            isNetworkAvailable = NetStateUtils.isNetworkAvailable()
            if (context is Application) {
                context.registerActivityLifecycleCallbacks(object :
                    Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(
                        activity: Activity,
                        savedInstanceState: Bundle?
                    ) {
                    }

                    override fun onActivityStarted(activity: Activity) {
                    }

                    override fun onActivityResumed(activity: Activity) {
                        if (topForegroundActivity == null) {
                            onAppBackgroundChanged(true)
                        }
                        topForegroundActivity = activity
                    }

                    override fun onActivityPaused(activity: Activity) {
                    }

                    override fun onActivityStopped(activity: Activity) {
                        if (topForegroundActivity === activity) {
                            onAppBackgroundChanged(false)
                            topForegroundActivity = null
                        }
                    }

                    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                    }

                    override fun onActivityDestroyed(activity: Activity) {
                    }
                })
            }
        }
    }

    fun getCurUserId(): String? {
        try {
            if (mLibHandler != null) {
                instance.mCurrentUserId = mLibHandler!!.curUserId
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

        return instance.mCurrentUserId

    }

    fun getHttpHost(): String? {
        try {
            return mLibHandler?.httpHost
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return ""
    }

    private inner class AidlConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            QLog.e(TAG, "onServiceConnected ${TimeUtil.getTime(System.currentTimeMillis())}")
            QLog.e(TAG, "onServiceConnected token:$mToken")
            mLibHandler = IHandler.Stub.asInterface(service)
            mConnectionStatusListener?.onChanged(ConnectionStatusListener.STATUS_REMOTE_SERVICE_CONNECTED)
            initRemoteListener()
            try {
                mLibHandler!!.asBinder().linkToDeath(mDeathReceipient, 0)
                QLog.d(TAG, "remove sevice linkToDeath")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (isDeadNotConnect && !mToken.isNullOrEmpty()) {
                connectServer(mToken!!)
            }
            isDeadNotConnect = false
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mLibHandler = null
            QLog.e(TAG, "onServiceDisconnected")
            initBindService()
        }
    }

    private val mDeathReceipient = object : IBinder.DeathRecipient {
        override fun binderDied() {
            QLog.d(TAG, "DeathRecipient binderDied")
            if (mLibHandler != null) {
                mLibHandler!!.asBinder().unlinkToDeath(this, 0)
                QLog.d(TAG, "DeathRecipient unlinkToDeath")
                mLibHandler = null
                isDeadNotConnect = true
                initBindService()
                QLog.d(TAG, "DeathRecipient restart bind service")
            }
        }
    }

    @Synchronized
    private fun connectServer(token: String?) {
        if (!TextUtils.isEmpty(token)) {
            if (mLibHandler == null) {
                QLog.e(TAG, "connectServer mLibHandler is Null")
                mConnectRunnable = ConnectRunnable(token)
                initBindService()
            } else {
                mWorkHandler.post {
                    try {
                        if (mLibHandler == null || token.isNullOrEmpty() || mImServerUrl.isNullOrEmpty()) {
                            QLogTrace.instance.log("a is $mLibHandler,b:$token,c:$mImServerUrl")
                            return@post
                        }
                        mLibHandler?.connectServer(
                            token,
                            mImServerUrl,
                            object : IConnectStringCallback.Stub() {
                                override fun onComplete() {
                                    if (mConnectCallBack != null) {
                                        mConnectCallBack!!.onCallBack("")
                                        mConnectCallBack = null
                                    }
                                    //检查数据库中正在发送的消息
                                    checkSendingMessage()
                                    QXPushClient.updateIMToken(mContext, mToken, mLibHandler!!.httpHost, mLibHandler!!.rsaKey)
                                }

                                override fun onFailure(errorCode: Int, failure: String?) {
                                    if (mConnectCallBack != null) {
                                        mConnectCallBack!!.onFail(failure)
                                        mConnectCallBack = null
                                    }
                                }

                                override fun onDatabaseOpened(state: Int) {
                                    if (mConnectCallBack != null) {
                                        mConnectCallBack!!.onDatabaseOpened(state)
                                    }
                                }
                            })
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun checkSendingMessage() {
        val messages = mLibHandler?.checkSendingMessage()
        if (!messages.isNullOrEmpty()) {
            QLog.d(TAG, "messages size=" + messages.size)
            mCheckSendingMessageListener?.onChecked(messages)
        }
    }

    private fun onAppBackgroundChanged(inForeground: Boolean) {
        isInForeground = inForeground
        try {
            if (mLibHandler != null) {
                if (mConnectStatus == ConnectionStatusListener.Status.CONNECTED) {

                } else if (inForeground) {
                    connectServer(mToken)
                }
            } else if (inForeground) {
                initBindService()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private inner class ConnectRunnable(var token: String?) : Runnable {
        override fun run() {
            connectServer(token)
        }
    }

    private fun runOnUiThread(runnable: Runnable) {
        mHandler?.post(runnable)
    }

    private fun runOnUiThread(runnable: () -> Unit) {
        mHandler?.post(runnable)
    }

    private fun initBindService() {
        if (mLibHandler != null) {
            QLog.d(TAG, "initBindService mLibHandler is not null")
        } else {
            if (mContext != null) {
                QLog.d(TAG, "initBindService mAppKey：$mAppKey")
                QLog.d(TAG, "start bindService ${TimeUtil.getTime(System.currentTimeMillis())}")
                val intent = Intent(mContext, QXIMService::class.java)
                intent.putExtra("appKey", mAppKey)
                mContext!!.bindService(intent, mAidlConnection, Context.BIND_AUTO_CREATE)
            } else {
                QLog.d(TAG, "mContext 为空")
            }
        }
    }

    private fun initRemoteListener() {
        try {
            mLibHandler!!.setOnChatRoomMessageReceiveListener(object :
                IOnChatRoomMessageReceiveListener.Stub() {
                override fun onReceiveNewChatRoomMessage(message: Message?) {
                    runOnUiThread(Runnable {
                        mOnChatRoomMessageReceiveListener?.onReceiveNewChatRoomMessage(message!!)
                    })
                }
            })
            mLibHandler!!.setMessageReceiptListener(object : IMessageReceiptListener.Stub() {
                override fun onMessageReceiptReceived(message: Message?) {
                    runOnUiThread(Runnable {
                        mMessageReceiptListener?.onMessageReceiptReceived(message)
                    })
                }

                override fun onMessageReceiptRead() {
                    runOnUiThread(Runnable {
                        mMessageReceiptListener?.onMessageReceiptRead()
                    })
                }
            })
            mLibHandler!!.setReceiveMessageListener(object : IOnReceiveMessageListener.Stub() {
                override fun onReceiveNewMessage(message: MutableList<Message>?) {
                    runOnUiThread(Runnable {
                        mOnMessageReceiveListener?.onReceiveNewMessage(message!!)
                    })
                }

                override fun onReceiveRecallMessage(message: Message?) {
                    runOnUiThread(Runnable {
                        mOnMessageReceiveListener?.onReceiveRecallMessage(message!!)
                    })
                }

                override fun onReceiveInputStatusMessage(from: String?) {
                    runOnUiThread(Runnable {
                        mOnMessageReceiveListener?.onReceiveInputStatusMessage(from!!)
                    })
                }

                override fun onReceiveHistoryMessage(message: MutableList<Message>?) {
                    runOnUiThread(Runnable {
                        mOnMessageReceiveListener?.onReceiveHistoryMessage(message!!)
                    })
                }

                override fun onReceiveP2POfflineMessage(message: MutableList<Message>?) {
                    runOnUiThread(Runnable {
                        mOnMessageReceiveListener?.onReceiveP2POfflineMessage(message!!)
                    })
                }

                override fun onReceiveGroupOfflineMessage(message: MutableList<Message>?) {
                    runOnUiThread(Runnable {
                        mOnMessageReceiveListener?.onReceiveGroupOfflineMessage(message!!)
                    })
                }

                override fun onReceiveSystemOfflineMessage(message: MutableList<Message>?) {
                    runOnUiThread(Runnable {
                        mOnMessageReceiveListener?.onReceiveSystemOfflineMessage(message!!)
                    })
                }
            })
            mLibHandler!!.setConversationListener(object : IConversationListener.Stub() {

                override fun onChanged(list: MutableList<Conversation>?) {
                    runOnUiThread(Runnable {
                        mConversationListener?.onChanged(list!!)
                    })
                }

            })
            mLibHandler!!.addOnChatNoticeReceivedListener(object :
                IOnChatNoticeReceivedListener.Stub() {
                override fun onGroupGlobalMute(isEnabled: Boolean) {
                    runOnUiThread(Runnable {
                        for (listener in mOnChatNoticeReceivedListenerList) {
                            listener.onGroupGlobalMute(isEnabled)
                        }
                    })
                }

                override fun onGroupMute(groupId: String?, isEnabled: Boolean) {
                    runOnUiThread(Runnable {
                        for (listener in mOnChatNoticeReceivedListenerList) {
                            listener.onGroupMute(groupId!!, isEnabled)
                        }
                    })

                }

                override fun onGroupAllMute(groupId: String?, isEnabled: Boolean) {
                    runOnUiThread(Runnable {
                        for (listener in mOnChatNoticeReceivedListenerList) {
                            listener.onGroupAllMute(groupId!!, isEnabled)
                        }
                    })
                }

                override fun onChatRoomGlobalMute(isEnabled: Boolean) {
                    runOnUiThread(Runnable {
                        for (listener in mOnChatNoticeReceivedListenerList) {
                            listener.onChatRoomGlobalMute(isEnabled)
                        }
                    })
                }

                override fun onChatRoomBan(chatRoomId: String?, isEnabled: Boolean) {
                    runOnUiThread(Runnable {
                        for (listener in mOnChatNoticeReceivedListenerList) {
                            listener.onChatRoomBan(chatRoomId!!, isEnabled)
                        }
                    })
                }

                override fun onChatRoomMute(chatRoomId: String?, isEnabled: Boolean) {
                    runOnUiThread(Runnable {
                        for (listener in mOnChatNoticeReceivedListenerList) {
                            listener.onChatRoomMute(chatRoomId!!, isEnabled)
                        }
                    })
                }

                override fun onChatRoomDestroy() {
                    runOnUiThread(Runnable {
                        for (listener in mOnChatNoticeReceivedListenerList) {
                            listener.onChatRoomDestroy()
                        }
                    })
                }
            })

            mLibHandler!!.setConnectionStatusListener(object : IConnectionStatusListener.Stub() {
                override fun onChanged(code: Int) {
                    onConnectStatusChange(code)
                }

                @Synchronized
                fun onConnectStatusChange(code: Int) {
                    mConnectStatus = ConnectionStatusListener.Status.getStatus(code)
                    if (code === ConnectionStatusListener.STATUS_KICKED) {
                        clearToken()
                    }
                    runOnUiThread {
                        mConnectionStatusListener?.onChanged(code)
                    }
                    QLog.e(TAG, " onConnectStatusChange ${mConnectStatus?.message}")
                }
            })

            mLibHandler?.setCallReceiveMessageListener(object : ICallReceiveMessageListener.Stub() {
                override fun onReceive(receiveMessage: CallReceiveMessage) {
                    QLog.e(TAG, "ICallReceiveMessageListener onReceive")
                    ModuleManager.routeMessage(receiveMessage)
                }
            })
            mLibHandler?.setRTCSignalMessageListener(object : IRTCMessageListener.Stub() {
                override fun onReceive(signalData: RTCSignalData) {
                    QLog.e(TAG, "IRTCMessageListener onReceive")
                    ModuleManager.rtcSignalRouterMessage(signalData)
                }
            })
        } catch (remoteExption: RemoteException) {
            remoteExption.printStackTrace()
        }
    }

    //连接回调
    abstract class ConnectCallBack {
        /**
         * 成功
         */
        abstract fun onSuccess(result: String?)

        /**
         * 失败
         */
        abstract fun onError(errorCode: String?)

        /**
         * 数据库打开
         */
        abstract fun onDatabaseOpened(code: Int)
        fun onFail(message: String?) {
            mHandler?.post {
                onError(
                    message
                )
            }
        }

        fun onCallBack(result: String?) {
            mHandler?.post {
                this@ConnectCallBack.onSuccess(
                    result
                )
            }
        }
    }

    interface ErrorCallback {
        fun onError(error: QXError)
    }

    abstract class ResultCallback<T> : ErrorCallback {

        /**
         * 操作成功
         * @param data 泛型数据
         */
        abstract fun onSuccess(data: T)

        /**
         * 操作失败
         * @param errorCode 错误码
         * @param msg 错误信息
         */
        abstract fun onFailed(error: QXError)

        fun onCallback(data: T) {
            mHandler?.post {
                onSuccess(data)
            }
        }

        override fun onError(error: QXError) {
            mHandler?.post {
                onFailed(error)
            }
        }
    }

    // 操作回调，用于无参数返回的操作
    abstract class OperationCallback : ErrorCallback {
        /**
         * 操作成功，回调到主线程
         */
        abstract fun onSuccess()

        /**
         * 操作失败，回调到主线程
         */
        abstract fun onFailed(error: QXError)

        fun onCallback() {
            mHandler?.post {
                onSuccess()
            }
        }

        override fun onError(error: QXError) {
            mHandler?.post {
                onFailed(error)
            }
        }
    }

    //发送消息回调
    abstract class SendMessageCallback : ErrorCallback {
        /**
         * 插入数据库成功
         */
        abstract fun onAttached(message: Message?)

        /**
         * 发送成功
         */
        abstract fun onSuccess()

        /**
         * 发送失败
         */
        override fun onError(error: QXError) {
            onError(error, null)
        }

        abstract fun onError(error: QXError, message: Message?)

    }

    interface ConnectionStatusListener {
        fun onChanged(code: Int)
        enum class Status(var value: Int, var message: String) {
            UNKNOWN(STATUS_UNKNOWN, "未知状态"), CONN_USER_BLOCKED(
                STATUS_CONN_USER_BLOCKED,
                "用户被禁用"
            ),
            CONNECTED(
                STATUS_CONNECTED, "已连接"
            ),
            CONNECTING(STATUS_CONNECTING, "连接中"), DISCONNECTED(
                STATUS_DISCONNECTED,
                "连接已断开"
            ),
            KICKED(
                STATUS_KICKED, "被踢下线"
            ),
            NETWORK_UNAVAILABLE(STATUS_NETWORK_UNAVAILABLE, "网络不可用"), NETWORK_ERROR(
                STATUS_NETWORK_ERROR, "网络错误"
            ),
            INIT_ERROR(STATUS_INIT_ERROR, "初始化错误，请先调用init方法"), SERVER_INVALID(
                STATUS_SERVER_INVALID, "服务器异常"
            ),
            TOKEN_INCORRECT(STATUS_TOKEN_INCORRECT, "token错误"), REFUSE(
                STATUS_REFUSE,
                "非法访问"
            ),
            LOGOUT(
                STATUS_LOGOUT, "注销成功"
            ),
            TIMEOUT(STATUS_TIME_OUT, "连接超时"),
            UNCONNECT(STATUS_UNCONNECTED, "未连接"),
            REMOTE_SERVICE_CONNECTED(STATUS_REMOTE_SERVICE_CONNECTED, "远程服务已连接");

            companion object {
                @JvmStatic
                fun getStatus(code: Int): Status? {
                    for (status in Status.values()) {
                        if (status.value == code) {
                            return status
                        }
                    }
                    return null
                }
            }
        }

        companion object {
            //未知状态
            const val STATUS_UNKNOWN = 0

            //用户被封禁
            const val STATUS_CONN_USER_BLOCKED = 1

            //已连接
            const val STATUS_CONNECTED = 2

            //连接中
            const val STATUS_CONNECTING = 3

            //断开连接
            const val STATUS_DISCONNECTED = 4

            //被踢下线
            const val STATUS_KICKED = 5

            //网络不可用
            const val STATUS_NETWORK_UNAVAILABLE = 6

            //服务器不可用
            const val STATUS_SERVER_INVALID = 7

            //token错误
            const val STATUS_TOKEN_INCORRECT = 8

            //服务器拒绝登录
            const val STATUS_REFUSE = 9

            //已登出
            const val STATUS_LOGOUT = 10

            //网络错误
            const val STATUS_NETWORK_ERROR = 11

            //初始化错误
            const val STATUS_INIT_ERROR = 12

            //连接超时
            const val STATUS_TIME_OUT = 13

            //未连接
            const val STATUS_UNCONNECTED = 14

            //远程服务已连接
            const val STATUS_REMOTE_SERVICE_CONNECTED = 15
        }
    }

    interface OnChatNoticeReceivedListener {
        /**
         * 群全局禁言/解禁
         * @param isEnabled 是否启动，true为禁言，false为解禁
         */
        fun onGroupGlobalMute(isEnabled: Boolean)

        /**
         * 群组成员禁言
         *@param groupId 群组Id
         * @param isEnabled 是否启动，true为禁言，false为解禁
         *
         */

        fun onGroupMute(groupId: String, isEnabled: Boolean)

        /**
         * 群整体禁言。即：该群所有人都不能发消息
         * @param groupId 群组Id
         * @param isEnabled 是否启动，true为禁言，false为解禁
         */
        fun onGroupAllMute(groupId: String, isEnabled: Boolean)

        /**
         * 聊天室全局禁言/解禁
         * @param isEnabled 是否启动，true为禁言，false为解禁
         */
        fun onChatRoomGlobalMute(isEnabled: Boolean)

        /**
         * 聊天室成员封禁，即：封禁后会被踢出聊天室，并无法再次进入聊天室，直到解禁
         * @param isEnabled 是否启动，true为封禁，false为解封
         */
        fun onChatRoomBan(chatRoomId: String, isEnabled: Boolean)

        /**
         * 聊天室成员禁言
         *@param chatRoomId 聊天室Id
         * @param isEnabled 是否启动，true为禁言，false为解禁
         *
         */
        fun onChatRoomMute(chatRoomId: String, isEnabled: Boolean)

        /**
         * 聊天室销毁
         */
        fun onChatRoomDestroy()
    }

    interface ConversationListener {
        fun onChanged(list: List<Conversation>)
    }

    interface MessageReceiptListener {
        /**
         * 收到消息回执：已送达
         * @param message
         */
        fun onMessageReceiptReceived(message: Message?)

        /**
         * 收到消息已阅读回执
         */
        fun onMessageReceiptRead()
    }

    interface OnChatRoomMessageReceiveListener {
        //聊天室消息
        fun onReceiveNewChatRoomMessage(message: Message)

        //聊天室属性获取回调
        fun onReceiveGetAttribute(data: HashMap<String, String>)
    }

    interface CheckSendingMessageListener {
        fun onChecked(messages: List<Message>)
    }

    interface OnMessageReceiveListener {
        fun onReceiveNewMessage(message: List<Message>)
        fun onReceiveRecallMessage(message: Message)
        fun onReceiveInputStatusMessage(from: String)
        fun onReceiveHistoryMessage(message: List<Message>)
        fun onReceiveP2POfflineMessage(message: List<Message>)
        fun onReceiveGroupOfflineMessage(message: List<Message>)
        fun onReceiveSystemOfflineMessage(message: List<Message>)
    }

    //---------------------开放给外部类调用----start-----------------------
    fun registerCustomEventProvider(provider: ICustomEventProvider, callback: OperationCallback) {
        if (!isReady(callback)) {
            return
        }
        try {
            if (mLibHandler?.registerCustomEventProvider(provider)!!) {
                callback.onCallback()
            } else {
                callback.onError(QXError.PARAMS_INCORRECT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isMessageExist(messageId: String?): Boolean {
        if (TextUtils.isEmpty(messageId)) {
            return false
        }
        if (mLibHandler == null) {
            return false
        }
        try {
            return mLibHandler?.isMessageExist(messageId)!!
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return false
    }
    fun sendMessage(message: Message, callback: SendMessageCallback) {
        try {
            //如果消息已存在
            if (message != null) {
                //如果为输入状态、状态为失败的消息，则只发送，不存储
                if (isMessageExist(message.messageId) || message.messageType == MessageType.TYPE_STATUS || message.state == Message.State.STATE_FAILED) {
                    message.state = Message.State.STATE_SENDING
                    sendOnly(message, callback)
                } else {
                    saveOnly(message, callback)
                }
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
    fun sendOnly(message: Message, callback: SendMessageCallback) {
        if (!isReady(callback, message)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            message.state = Message.State.STATE_SENDING
            mLibHandler?.updateMessageState(message.messageId, message.state)
            //构建用于数据发送的消息体
            mLibHandler!!.sendOnly(message, object : ISendMessageCallback.Stub() {

                override fun onAttached(message: Message?) {
                    runOnUiThread {
//                    callback.onAttached(message!!)
                    }
                }

                override fun onSuccess() {
                    runOnUiThread {
                        callback.onSuccess()
                    }
                }

                override fun onError(errorOrdinal: Int, message: Message?) {
                    runOnUiThread {
                        callback.onError(QXError.values()[errorOrdinal], message)
                    }
                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }

    fun saveOnly(message: Message, callback: SendMessageCallback) {
        if (!isReady(callback, message)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.saveOnly(message, object : ISendMessageCallback.Stub() {
                override fun onAttached(message: Message?) {
                    mHandler?.post {
                        callback.onAttached(message!!)
                        if (message.messageType != MessageType.TYPE_STATUS) {
                            sendOnly(message, callback)
                        }
                    }
                }

                override fun onSuccess() {
                }

                override fun onError(errorOrdinal: Int, message: Message) {
                    mHandler?.post {
                        callback.onError(QXError.values()[errorOrdinal], message)
                    }
                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun saveMediaMessageOnly(message: Message, callback: SendMessageCallback) {
        if (!isReady(callback, message)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.saveOnly(message, object : ISendMessageCallback.Stub() {
                override fun onAttached(message: Message?) {
                    mHandler?.post {
                        callback.onAttached(message!!)
                    }
                }

                override fun onSuccess() {
                }

                override fun onError(errorOrdinal: Int, message: Message) {
                    mHandler?.post {
                        callback.onError(QXError.values()[errorOrdinal], message)
                    }
                }

            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun saveMediaMessage(message: Message?, callback: SendMessageCallback) {
        try {
            //如果该消息已存在，则说明本次操作为重发
            if (message != null) {
                if (!TextUtils.isEmpty(message.messageId)) {
                    if (isMessageExist(message.messageId)) {
                        message.state = Message.State.STATE_SENDING
                        if (mLibHandler!!.updateMessageState(message.messageId, message.state) > 0) {
                            callback.onAttached(message)
                            return
                        }
                    }
                }
            }
            if (message != null) {
                saveMediaMessageOnly(message, callback)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun saveMediaMessage(context: Context, conversationType: String, targetId: String,
                         messageType: String, uri: Uri, callback: SendMessageCallback) {
        if (!isReady(callback, conversationType, targetId, messageType, uri)) {
            return
        }
        checkParams(callback, conversationType, targetId, messageType, uri)

        if (conversationType != Conversation.Type.TYPE_PRIVATE && conversationType != Conversation.Type.TYPE_GROUP) {
            callback.onError(QXError.PARAMS_INCORRECT)
        }
        try {
            //否则为新发送消息
            var media = MediaUtil.toLocalMedia(context, uri)
            if (media != null) {
                saveMediaMessageToDb(conversationType, targetId, messageType, media, callback)
            } else {
                callback.onError(QXError.LOCAL_FILE_URI_ERROR)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    //保存媒体消息到本地
    private fun saveMediaMessageToDb(conversationType: String, targetId: String, messageType: String,
                                     media: LocalMedia, callback: SendMessageCallback) {
        try {
            var message: Message? = null
            when (messageType) {
                MessageType.TYPE_AUDIO -> {
                    message = MessageCreator.instance.createAudioMessage(conversationType!!, getCurUserId()!!,
                        targetId!!, media.path, "", media.duration, media.size, "")
                }
                MessageType.TYPE_IMAGE -> {
                    message = MessageCreator.instance.createImageMessage(conversationType!!, getCurUserId()!!,
                        targetId!!, media.path, "", "", media.width, media.height, media.size, "")
                }
                MessageType.TYPE_VIDEO -> {
                    var bitmap = MediaUtil.getVideoFirstFrame(media!!.path)      //截取视频第一帧
                    var bitmapPath = BitmapUtil.saveBitmap(bitmap)
                    //销毁bitmap
                    bitmap.recycle()
                    //保存消息到数据库
                    var file = File(media.path)
                    message = MessageCreator.instance.createVideoMessage(conversationType!!, getCurUserId()!!,
                        targetId!!, media.path, bitmapPath, "", bitmap.width, bitmap.height, file.length(), media!!.duration, "")
                }
                MessageType.TYPE_FILE -> {
                    var file = File(media.path)
                    var type = FileUtil.getSuffixName(media.path)
                    message = MessageCreator.instance.createFileMessage(conversationType!!, getCurUserId()!!,
                        targetId!!, media.path, file.name, "", type, media.size, "")
                }
                else -> {
                    callback.onError(QXError.PARAMS_INCORRECT)
                }
            }
            if (message != null) {
                saveMediaMessageOnly(message, callback)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun updateMessageState(messageId: String, state: Int, callback: OperationCallback) {
        if (!isReady(callback, messageId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.updateMessageState(messageId, state) > 0) {
                callback?.onCallback()
            } else {
                callback?.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun sendHeartBeat() {
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.sendHeartBeat()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun sendLogout(callback: OperationCallback) {
        if (!isReady(callback)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.sendLogout(PushManager.getInstance().serverPushType.getName(), object : IResultCallback.Stub() {

                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getLatestMessageByConversationId(conversationId: String, ownerId: String, callback: ResultCallback<Message>) {
        if (!isReady(callback, conversationId, ownerId)) {
            return
        }
        if (mLibHandler == null || conversationId == null || ownerId == null) {
            return
        }

        try {
            if (mLibHandler != null) {
                var result = mLibHandler!!.getLatestMessageByConversationId(conversationId, ownerId)
                callback.onCallback(result)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendMessageReadReceipt(conversationType: String, targetId: String, lastTimestamp: Long, callback: OperationCallback) {
        if (!isReady(callback, conversationType, targetId, lastTimestamp)) {
            return
        }
        if (mLibHandler == null || lastTimestamp == 0L) {
            return
        }
        try {
            mLibHandler!!.sendMessageReadReceipt(conversationType, targetId, lastTimestamp, object : IResultCallback.Stub() {

                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])
                }

            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun sendRecall(message: Message, callback: OperationCallback) {
        if (!isReady(callback, message)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.sendRecall(message, object : IResultCallback.Stub() {

                override fun onSuccess() {
                    callback.onSuccess()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])
                }

            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun deleteLocalMessageById(messageIds: Array<String>, callback: OperationCallback) {
        if (!isReady(callback, messageIds)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            var row = mLibHandler!!.deleteLocalMessageById(messageIds)
            if (row > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun deleteRemoteMessageByMessageId(conversationType: String, targetId: String, messageIds: List<String>, callback: OperationCallback) {
        if (!isReady(callback, conversationType, targetId, messageIds)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.deleteRemoteMessageByMessageId(conversationType, targetId, messageIds,
                object : IResultCallback.Stub() {

                    override fun onSuccess() {
                        callback.onCallback()
                    }

                    override fun onFailed(errorOrdinal: Int) {
                        callback.onError(QXError.values()[errorOrdinal])
                    }

                })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }

    fun deleteRemoteMessageByTimestamp(conversationType: String, targetId: String, timestamp: Long, callback: OperationCallback) {
        if (!isReady(callback, conversationType, targetId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.deleteRemoteMessageByTimestamp(conversationType,
                targetId,
                timestamp,
                object : IResultCallback.Stub() {

                    override fun onSuccess() {
                        callback.onCallback()
                    }

                    override fun onFailed(errorOrdinal: Int) {
                        callback.onError(QXError.values()[errorOrdinal])
                    }

                })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun deleteLocalMessageByTimestamp(conversationType: String, targetId: String, timestamp: Long, callback: OperationCallback) {
        if (!isReady(callback, conversationType, targetId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.deleteLocalMessageByTimestamp(conversationType, targetId, timestamp) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getConversationProperty(conversation: Conversation, callback: OperationCallback) {
        if (!isReady(callback)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.getConversationProperty(conversation, object : IResultCallback.Stub() {

                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])
                }

            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun updateFileOriginUrl(message: Message, callback: OperationCallback) {
        if (!isReady(callback)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.updateOriginPath(message) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }

    fun updateFileLocalPath(message: Message, callback: OperationCallback) {
        if (!isReady(callback)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.updateLocalPath(message) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun updateVideoHeadUrl(messageId: String, headUrl: String, callback: OperationCallback) {
        if (!isReady(callback)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.updateHearUrl(messageId, headUrl) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }


    fun searchConversations(keyword: String, conversationTypes: Array<String>, messageTypes: Array<String>,
                            callback: ResultCallback<List<SearchConversationResult>>) {
        if (!isReady(callback)) {
            return
        }
        try {
            var result = mLibHandler?.searchConversations(keyword, conversationTypes, messageTypes)
            if (!result.isNullOrEmpty()) {
                callback.onCallback(result)
            } else {
                callback.onCallback(arrayListOf())
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * 全局搜索文本消息
     * @param content 文本内容
     */
    fun searchTextMessage(content: String, callback: ResultCallback<List<Message>>) {
        if (!isReady(callback)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            var result = mLibHandler!!.searchTextMessage(content)
            callback.onCallback(result)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * 获取本地消息
     *
     * @param offset 从第几条开始查
     * @param pageSize 每次查多少条
     */
    fun getMessages(conversationType: String, targetId: String, offset: Int, pageSize: Int, callback: ResultCallback<List<Message>>) {
        QLog.d(TAG, "获取本地消息--------->： 从第$offset 条 - " + (offset + pageSize - 1) + " 记录开始")
        if (!isReady(callback, conversationType, targetId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler != null) {
                var result = mLibHandler!!.getMessages(conversationType, targetId, offset, pageSize)
                if (result.isNullOrEmpty()) {
                    result = arrayListOf()
                }
                callback.onCallback(result)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * 获取本地消息
     *
     * @param offset 从第几条开始查
     * @param pageSize 每次查多少条
     */
    fun getHistoryMessagesFromLocal(conversationId: String, conversationType: String, targetId: String, offset: Int, pageSize: Int, callback: ResultCallback<List<Message>>) {
        QLog.d(TAG, "获取本地消息getHistoryMessagesFromLocal------>： 从第$offset 条 - " + (offset + pageSize - 1) + " 记录开始")
        if (!isReady(callback, conversationType, targetId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler != null) {
                var result = mLibHandler!!.getHistoryMessageFromLocal(conversationId, conversationType, targetId, offset, pageSize)
                if (result.isNullOrEmpty()) {
                    result = arrayListOf()
                }
                callback.onCallback(result)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    // 获取后端服务器历史消息
    fun getHistoryMessageFromService(conversationType: String?, targetId: String?, timestamp: Long, searchType: Int) {
        QLog.d(TAG, "获取后端历史消息--------->： 从第$timestamp 条 - 记录开始 " + (conversationType == null) + " " + (conversationType == null) + " " + (mLibHandler == null))
        if (conversationType == null || targetId == null) {
            return
        }
        if (conversationType.isEmpty() || targetId.isEmpty()) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler != null) {
                mLibHandler!!.getHistoryMessageFromService(conversationType, targetId, timestamp, searchType)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    //通过会话ID获取本地对应的消息
    fun getMessagesByConversationID(conversationId: String, callback: ResultCallback<List<Message>>) {
        if (!isReady(callback, conversationId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler != null) {
                var result = mLibHandler!!.getMessagesByConversationID(conversationId)
                if (result.isNullOrEmpty()) {
                    result = arrayListOf()
                }
                callback.onCallback(result)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getMessagesByTimestamp(conversationType: String, targetId: String, timestamp: Long, searchType: Int, pageSize: Int, callback: ResultCallback<List<Message>>) {
        if (!isReady(callback, conversationType, targetId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler != null) {
                var result = mLibHandler?.getMessagesByTimestamp(conversationType, targetId, timestamp, searchType, pageSize)
                if (result.isNullOrEmpty()) {
                    result = arrayListOf()
                }
                callback.onCallback(result)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }
    fun getMessageById(messageId: String, callback: ResultCallback<Message>){
        if (!isReady(callback, messageId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler != null) {
                var result = mLibHandler?.getMessageById(messageId)
                callback.onCallback(result!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun getMessagesByType(conversationId: String, types: List<String>, offset: Int, pageSize: Int, isAll: Boolean, isDesc: Boolean, callback: ResultCallback<List<Message>>) {
        if (!isReady(callback, conversationId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            var result = mLibHandler!!.getMessagesByType(conversationId, types, offset, pageSize, isAll, isDesc)

            if (result.isNullOrEmpty()) {
                result = arrayListOf()
            }
            callback.onCallback(result)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getUnReadAtMessage(conversationId: String, callback: ResultCallback<MutableList<Message>>) {
        if (!isReady(callback, String)) {
            return
        }
        try {
            var result = mLibHandler?.getUnReadAtMessages(conversationId)
            if (result != null) {
                callback.onCallback(result)
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getFirstUnReadMessage(conversationId: String, callback: ResultCallback<Message>) {
        if (!isReady(callback, String)) {
            return
        }
        try {
            var result = mLibHandler?.getFirstUnReadMessage(conversationId)
            if (result != null) {
                callback.onCallback(result)
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getConversation(conversationType: String, targetId: String, callback: ResultCallback<Conversation>) {
        if (!isReady(callback, conversationType, targetId)) {
            return
        }
        try {
            var result = mLibHandler?.getConversation(conversationType, targetId)
            if (result != null) {
                callback.onCallback(result)
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getAllConversation(callback: ResultCallback<List<Conversation>>) {
        if (!isReady(callback)) {
            callback.onFailed(QXError.DB_NO_ROW_FOUND)
            return
        }
        try {
            var result = mLibHandler?.allConversation
            result?.apply {
                callback.onCallback(this)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getConversationInRegion(region: List<String>, callback: ResultCallback<List<Conversation>>) {
        if (!isReady(callback)) {
            callback.onFailed(QXError.DB_NO_ROW_FOUND)
            return
        }
        try {
            var result = mLibHandler?.getConversationInRegion(region)
            result?.apply {
                callback.onCallback(this)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getAllUnReadCount(region: List<String>, callback: ResultCallback<Int>) {
        if (!isReady(callback)) {
            return
        }
        try {
            var result = mLibHandler?.getAllUnReadCount(region)
            callback.onCallback(result!!)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getConversationUnReadCount(conversationId: String, isIgnoreNoDisturbing: Boolean, callback: ResultCallback<Int>) {
        if (!isReady(callback)) {
            return
        }
        try {
            var result = mLibHandler?.getConversationUnReadCount(conversationId, isIgnoreNoDisturbing)
            callback.onCallback(result!!)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun updateCustomMessage(conversationId: String, messageId: String, content: String, extra: String,
                            callback: OperationCallback) {
        if (!isReady(callback, conversationId, messageId)) {
            return
        }
        try {
            if (mLibHandler!!.updateCustomMessage(conversationId, messageId, content, extra) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun updateAtMessageReadState(messageId: String, conversationId: String, readState: Int, callback: OperationCallback) {
        if (!isReady(callback, messageId)) {
            return
        }
        try {
            if (mLibHandler!!.updateAtMessageReadState(messageId, conversationId, readState) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun clearAtMessage(conversationId: String, callback: OperationCallback) {
        if (!isReady(callback, conversationId)) {
            return
        }
        try {
            if (mLibHandler!!.clearAtMessage(conversationId) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * 更新聊天背景
     * @param conversationId 会话id
     * @param url 草稿内容，当没有草稿时，请设置为空字符串：""
     */
    fun updateConversationBackground(conversationId: String, url: String, callback: OperationCallback) {
        if (!isReady(callback, conversationId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.updateConversationBackground(url, conversationId) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * 更新草稿
     * @param conversationId 会话id
     * @param draft 草稿内容，当没有草稿时，请设置为空字符串：""
     */
    fun updateConversationDraft(conversationId: String, draft: String, callback: OperationCallback) {
        if (!isReady(callback, conversationId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.updateConversationDraft(conversationId, draft) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * 更新会话标题
     * @param targetId 会话id草稿内容，当没有草稿时，请设置为空字符串：""
     */
    fun updateConversationTitle(type: String, targetId: String, title: String, callback: OperationCallback) {
        if (!isReady(callback, targetId, type, title)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.updateConversationTitle(type, targetId, title) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * 更新会话图片
     * @param targetId 会话id草稿内容
     */
    fun updateConversationIcon(type: String, targetId: String, icon: String, callback: OperationCallback) {
        if (!isReady(callback, targetId, type, icon)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.updateConversationIcon(type, targetId, icon) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * 同时更新会话显示的用户昵称，以及图片
     *
     * @param type
     * @param targetId
     * @param title
     * @param icon
     * @return
     */
    fun updateConversationTitleAndIcon(type: String, targetId: String, title: String, icon: String, callback: OperationCallback) {
        if (!isReady(callback, targetId, type, icon)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.updateConversationTitleAndIcon(type, targetId, title, icon) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * @param conversationId 会话id
     * @param isTop 是否置顶，true为置顶，false为取消置顶
     */
    fun setConversationTop(conversationId: String, isTop: Boolean, callback: OperationCallback) {
        if (!isReady(callback, conversationId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.setConversationTop(conversationId, isTop, object : IResultCallback.Stub() {

                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])
                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun setConversationNoDisturbing(conversationId: String, isNoDisturbing: Boolean, callback: OperationCallback) {
        if (!isReady(callback, conversationId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.setConversationNoDisturbing(conversationId, isNoDisturbing,
                object : IResultCallback.Stub() {

                    override fun onSuccess() {
                        callback.onCallback()
                    }

                    override fun onFailed(errorOrdinal: Int) {
                        callback.onError(QXError.values()[errorOrdinal])
                    }
                })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * 删除会话
     * @param conversationId 会话id
     */
    fun deleteConversation(conversationId: String, callback: OperationCallback) {
        if (!isReady(callback, conversationId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.deleteConversation(conversationId) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * 删除会话
     * @param conversationType 会话类型
     * @param targetId 聊天对象id
     */
    fun deleteConversation(conversationType: String, targetId: String, callback: OperationCallback) {
        if (!isReady(callback, targetId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.deleteConversationByTargetId(conversationType, targetId) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun deleteAllConversation(callback: OperationCallback) {
        if (!isReady(callback)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.deleteAllConversation() > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }

    /**
     * 按回话搜索文本消息
     * @param content 文本内容
     * @param conversationId 会话id
     */
    fun searchTextMessage(content: String, conversationId: String, callback: ResultCallback<List<Message>>) {
        if (!isReady(callback, conversationId, content)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            var result = mLibHandler!!.searchTextMessageByConversationId(content, conversationId)
            callback.onCallback(result)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun clearMessages(conversationId: String, callback: OperationCallback) {
        if (!isReady(callback, conversationId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            if (mLibHandler!!.clearMessages(conversationId) > 0) {
                callback.onCallback()
            } else {
                callback.onError(QXError.DB_NO_ROW_FOUND)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun logout() {
        try {
            clearToken()
            sendLogout(object : OperationCallback() {
                override fun onSuccess() {
                }

                override fun onFailed(error: QXError) {
                }

            })
            closeHeartBeat()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun setOnMessageReceiveListener(listener: OnMessageReceiveListener) {
        mOnMessageReceiveListener = listener
    }

    fun setOnConversationListener(listener: ConversationListener) {
        mConversationListener = listener
    }

    fun setMessageReceiptListener(listener: MessageReceiptListener) {
        mMessageReceiptListener = listener
    }

    fun setChatRoomMessageReceiveListener(listener: OnChatRoomMessageReceiveListener) {
        mOnChatRoomMessageReceiveListener = listener
    }

    fun addOnChatNoticeReceivedListener(listener: OnChatNoticeReceivedListener) {
        if (!mOnChatNoticeReceivedListenerList.contains(listener)) {
            mOnChatNoticeReceivedListenerList.add(listener)
        }
    }

    fun removeChatRoomMessageReceiveListener() {
        mOnChatRoomMessageReceiveListener = null
    }

    fun removeOnMessageReceiveListener() {
        mOnMessageReceiveListener = null
    }

    fun removeMessageReceiptListener() {
        mMessageReceiptListener = null
    }

    fun removeOnChatNoticeReceivedListener(listener: OnChatNoticeReceivedListener) {
        mOnChatNoticeReceivedListenerList.firstOrNull { it == listener }?.apply {
            mOnChatNoticeReceivedListenerList.remove(this)
        }
    }

    /**
     * 接收心跳检测请求
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleHeartBeatCheck(event: HeartBeatEvent) {
        try {
            if (event == HeartBeatEvent.EVENT_CHECK_NORMALLY) {
                if (HeartBeatHolder.getInstance().isNeedSendHeatBeat) {
                    sendHeartBeat()
                }
            } else if (event == HeartBeatEvent.EVENT_CHECK_IMMEDIATELY) {
                sendHeartBeat()
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
    }

    //不加注解默认监听所有的状态，方法名随意，只需要参数是一个NetWorkState即可    //TODO 待优化
    //@NetWorkMonitor(monitorFilter = {NetWorkState.GPRS})//只接受网络状态变为GPRS类型的消息
    fun handleNetWorkStateChange(netWorkState: NetWorkState) {
        isNetworkAvailable = when (netWorkState) {
            NetWorkState.GPRS, NetWorkState.WIFI -> {
                true
            }
            else -> {
                false
            }
        }
        QLog.e(TAG, "onNetWorkStateChange isNetworkAvailable >>> : $isNetworkAvailable ,${netWorkState.name}")
        reconnect()  // 尝试进行重连
    }

    fun setCheckSendingMessageListener(listener: CheckSendingMessageListener) {
        mCheckSendingMessageListener = listener;
    }

    fun checkSensitiveWord(text: String, callback: ResultCallback<SensitiveWordResult>) {
        if (!isReady(callback, text)) {
            return
        }
        try {
            var result = mLibHandler?.checkSensitiveWord(text)
            if (result != null) {
                callback.onCallback(result)
            } else {
                callback.onError(QXError.UNKNOWN)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun setUserProperty(userProperty: UserProperty, callback: ResultCallback<String>) {
        if (!isReady(callback, userProperty)) {
            return
        }
        try {
            mLibHandler?.setUserProperty(userProperty, object : IResultCallback.Stub() {
                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.UNKNOWN)
                }

                override fun onSuccess() {
                    callback.onCallback("")
                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun clearToken() {
        mToken = ""
        PushManager.getInstance().clearCache(mContext)
    }

    fun closeHeartBeat() {
        //关闭心跳检测服务
        HeartBeatHolder.getInstance().stopHeartBeatService()
        //取消心跳超时检测
        HeartBeatTimeCheck.getInstance().cancelTimer()
    }

    private fun reconnect() {
        // 当网络切换到NONE时，会调用两次, 第一次netty会首先发出网络错误发起重连请求, 第二次系统的网络变化广播,重连请求任务稍微延迟后
        // 当系统网络广播到来时会再次判断网络是否可用，不可用则会把重连事件关闭, 当网络切换到可用时会进行重连
        val userToken = ""
        QLog.e(TAG, " reconnect 是否进行重连 userToken:$userToken,网络状态：$isNetworkAvailable")
        if (userToken.isNotEmpty() && isNetworkAvailable) {
            ReConnectManager.instance.startReconnect()
        } else {
            ReConnectManager.instance.stopReconnect()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleReconnect(event: ReconnectEvent) {
        Log.e(TAG, "handleReconnect: 开始进行重连" + (mLibHandler == null))
        if (mLibHandler == null) {
            initBindService()
        } else {
            connectServer(Holder.instance.mToken)
        }
    }

    fun getVideoLimitTime(): Int {
        return 1000 * 60 * 10
    }

    fun getGIFLimitSize(): Int {
        return 1024 * 5
    }

    fun openDebugLog() {
        QLog.openDebug()
        mLibHandler?.openDebugLog()
    }

    /************************------音视频通话--------rtc_lib-------start-----************************************/
    private fun isReady(callback: ErrorCallback?, vararg params: Any): Boolean {
        if (callback == null) return false
        if (mLibHandler == null) {
            if (callback is SendMessageCallback) {
                (callback as SendMessageCallback).onError(QXError.CONNECTION_NOT_READY)
            } else {
                callback?.onError(QXError.CONNECTION_NOT_READY)
            }
            return false
        }
        //确定远程服务是可用
        if (mLibHandler!!.asBinder().isBinderAlive) {
            return checkParams(callback, params)
        }
        return false
    }

    private fun checkParams(callback: ErrorCallback, vararg params: Any): Boolean {
        if (params.isNotEmpty()) {
            for (p in params) {
                if (p is String) {
                    if (p.isNullOrEmpty()) {
                        callback.onError(QXError.PARAMS_INCORRECT)
                        return false
                    }
                } else if (p is Array<*>) {
                    //   checkParams(callback, params)
                }
            }
        } else {
            callback.onError(QXError.PARAMS_INCORRECT)
        }
        return true
    }

    fun startCall(conversationType: String, targetId: String, roomId: String, callType: String, userIds: List<String>, callback: OperationCallback) {
        if (!isReady(callback, conversationType, targetId, roomId, callType, userIds)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.startCall(conversationType, targetId, roomId, callType, userIds,
                object : IResultCallback.Stub() {
                    override fun onSuccess() {
                        callback.onCallback()
                    }

                    override fun onFailed(errorOrdinal: Int) {
                        callback.onError(QXError.values()[errorOrdinal])
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun acceptCall(roomId: String, callback: OperationCallback) {
        if (!isReady(callback, roomId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.acceptCall(roomId, object : IResultCallback.Stub() {

                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun cancelCall(roomId: String, callback: OperationCallback) {
        if (!isReady(callback, roomId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.cancelCall(roomId, object : IResultCallback.Stub() {
                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])
                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun refuseCall(roomId: String, callback: OperationCallback) {
        if (!isReady(callback, roomId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.refuseCall(roomId, object : IResultCallback.Stub() {
                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun hangUp(roomId: String, userId: String, callback: OperationCallback) {
        if (!isReady(callback, userId, roomId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.hangUp(roomId, userId, object : IResultCallback.Stub() {
                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun switchAudio(roomId: String, callback: OperationCallback) {
        if (!isReady(callback, roomId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.switchAudioCall(roomId, object : IResultCallback.Stub() {
                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun sendCallError(roomId: String, targetId: String, callback: OperationCallback?) {
        if (callback != null) {
            if (!isReady(callback, roomId)) {
                return
            }
        }
        try {
            mLibHandler?.sendCallError(roomId, targetId, object : IResultCallback.Stub() {
                override fun onFailed(errorOrdinal: Int) {
                    callback?.onError(QXError.values()[errorOrdinal])
                }

                override fun onSuccess() {
                    callback?.onCallback()
                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun setChatRoom(chatRoomId: String, name: String, value: String, autDel: Int, callback: OperationCallback) {
        if (!isReady(callback, chatRoomId, name, value)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.setChatRoom(chatRoomId, name, value, autDel, object : IResultCallback.Stub() {
                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun delChatRoom(chatRoomId: String, name: String, callback: OperationCallback) {
        if (!isReady(callback, chatRoomId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.delChatRoom(chatRoomId, name, object : IResultCallback.Stub() {
                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun getChatRoomProperty(chatRoomId: String, name: String, callback: OperationCallback) {
        if (!isReady(callback, chatRoomId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.getChatRoom(chatRoomId, name, object : IResultCallback.Stub() {
                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun joinChatRoom(chatRoomId: String, callback: OperationCallback) {
        if (!isReady(callback, chatRoomId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.joinChatRoom(chatRoomId, object : IResultCallback.Stub() {

                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }


    fun exitChatRoom(chatRoomId: String, callback: OperationCallback) {
        if (!isReady(callback, chatRoomId)) {
            return
        }
        if (mLibHandler == null) {
            return
        }
        try {
            mLibHandler!!.exitChatRoom(chatRoomId, object : IResultCallback.Stub() {

                override fun onSuccess() {
                    callback.onCallback()
                }

                override fun onFailed(errorOrdinal: Int) {
                    callback.onError(QXError.values()[errorOrdinal])

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun rtcJoin(join: RTCJoin, callback: ResultCallback<String>) {
        if (!isReady(callback, join)) {
            return
        }
        try {
            mLibHandler!!.rtcJoin(join, object : IResultCallback.Stub() {
                override fun onFailed(errorOrdinal: Int) {

                }

                override fun onSuccess() {
                    callback.onCallback("")
                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun rtcCandidate(candidate: RTCCandidate, callback: ResultCallback<String>) {
        if (!isReady(callback, candidate)) {
            return
        }
        try {
            mLibHandler!!.rtcCandidate(candidate, object : IResultCallback.Stub() {
                override fun onFailed(errorOrdinal: Int) {
                }

                override fun onSuccess() {

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun rtcOffer(offer: RTCOffer, callback: ResultCallback<String>) {
        if (!isReady(callback, offer)) {
            return
        }
        try {
            mLibHandler!!.rtcOffer(offer, object : IResultCallback.Stub() {
                override fun onSuccess() {

                }

                override fun onFailed(errorOrdinal: Int) {

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun rtcAnswer(offer: RTCOffer, callback: ResultCallback<String>) {
        if (!isReady(callback, offer)) {
            return
        }
        try {
            mLibHandler!!.rtcAnswer(offer, object : IResultCallback.Stub() {
                override fun onSuccess() {

                }

                override fun onFailed(errorOrdinal: Int) {

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun rtcJoined(joined: RTCJoined, callback: ResultCallback<String>) {
        if (!isReady(callback, joined)) {
            return
        }
        try {
            mLibHandler!!.rtcJoined(joined, object : IResultCallback.Stub() {
                override fun onFailed(errorOrdinal: Int) {
                }

                override fun onSuccess() {

                }
            })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun rtcVideoParam(param: RTCVideoParam, callback: ResultCallback<String>) {
        if (!isReady(callback, param)) {
            return
        }
        try {
            mLibHandler!!.rtcVideoParam(param, object : IResultCallback.Stub() {
                override fun onFailed(errorOrdinal: Int) {
                }

                override fun onSuccess() {

                }
            })
        } catch (exception: RemoteException) {
            exception.printStackTrace()
        }
    }

    fun rtcSercerConfig(): RTCServerConfig? {
        if (mLibHandler != null && mLibHandler!!.asBinder().isBinderAlive) {
            return mLibHandler!!.rtcConfig
        }
        return null
    }
}