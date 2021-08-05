package com.qx.rtc.call_lib

import android.content.Context
import android.os.*
import android.util.Log
import android.view.SurfaceView
import com.google.gson.Gson
import com.qx.im.model.RTCServerConfig
import com.qx.imlib.QXIMClient
import com.qx.imlib.qlog.QLog
import com.qx.imlib.qlog.QLogTrace
import com.qx.imlib.utils.TimeUtil
import com.qx.imlib.utils.UUIDUtil
import com.qx.imlib.utils.VibratorUtils
import com.qx.message.QXError
import com.qx.message.rtc.RTCCandidate
import com.qx.message.rtc.RTCJoined
import com.qx.message.rtc.RTCOffer
import com.qx.message.rtc.RTCVideoParam

class QXCallManager private constructor() {
    private val TAG = "QXCallManager"
    private lateinit var callEngine: QXCallEngine
    private var roomId: String = ""
    private lateinit var context: Context
    private var callSession: QXCallSession? = null
    private lateinit var workHandler: Handler
    private lateinit var handThread: HandlerThread
    private var isCalling = false
    private var userToken: String = ""
    private var serverHost: String = ""
    private var isNormalHangup = false
    private var cameraType = Const.FONT_FACTING
    private var callActionCountDown: CallActionCountDown? = null
    private var rtcConnectCountDown: RTCConnectCountDown? = null

    fun init(context: Context) {
        Log.e(TAG, " 呼叫管理 初始化 init")
        this.context = context
        callEngine = QXCallEngine(context)
        handThread = HandlerThread("call-work-thread")
        handThread.start()
        workHandler = Handler(handThread.looper)
        isCalling = false
    }

    fun reInit(context: Context) {
        this.context = context
        Log.e(TAG, " 呼叫管理 初始化 reInit ")
        isCalling = false
    }

    private val uiHandler = Handler(Looper.getMainLooper(), Handler.Callback { message ->
        when (message.what) {
            CALL_OUTGOING -> {
                QLog.e(TAG, "CALL_OUTGOING ")
                cameraType = Const.FONT_FACTING
                callSession = message.obj as QXCallSession
                callEngine.mediaType = getMediaType()
                // 返回本地预览
                val localView = callEngine.createRendererView(context, true)
                val localUser = QXUser(callSession!!.callId)
                localUser.surfaceView = localView
                callSession!!.userProfie.add(localUser)
                callEngine.setLocalVideo(localView)
                val remoteView = callEngine.createRendererView(context, false)
                val remoteUser = QXUser(callSession!!.targetId)
                remoteUser.surfaceView = remoteView
                callSession!!.userProfie.add(remoteUser)
                callEngine.setRemoteVideo(remoteView)
                callListener?.onCallOutgoing(callEngine.rtcCallMedia?.localSurfaceViewRenderer!!)
                if (isVideoCall()) {
                    callEngine.startPreview()
                }
                // 连接rtc服务 并进入房间
                roomId = UUIDUtil.getUUID()
                callSession!!.roomId = roomId
                // 发送tcp邀请信息
                callSession?.apply {
                    if (callId == QXIMClient.instance.getCurUserId()) {
                        // 发消息
                        QLogTrace.instance.log("send call type: ${getMediaType().name} ${TimeUtil.getTime(System.currentTimeMillis())}")
                        QXIMClient.instance.startCall(conversionType, targetId, roomId, callType, userIds, object : QXIMClient.OperationCallback() {

                            override fun onSuccess() {
                                // tcp成功 初始化界面
                                callState = CallState.CALL_START
                                startTime = System.currentTimeMillis()
                                connect()
                            }

                            override fun onFailed(error: QXError) {
                                callError(error)
                            }
                        })
                    }
                }
            }

            CALL_INCOMING -> {
                cameraType = Const.FONT_FACTING
                Log.e(TAG, "收到呼叫")
                QLogTrace.instance.log("receive call type: ${getMediaType().name} ${TimeUtil.getTime(System.currentTimeMillis())}")
                callSession = message.obj as QXCallSession
                this.roomId = callSession!!.roomId
                callEngine.mediaType = getMediaType()
                // 唤起界面
                receiveListener?.onReceivedCall(callSession!!)
                callSession?.apply {
                    callState = CallState.CALL_START
                    startTime = System.currentTimeMillis()
                }
            }
            CALL_SEND_CANCEL -> {
                isNormalHangup = true
                QXIMClient.instance.cancelCall(roomId, object : QXIMClient.OperationCallback() {

                    override fun onSuccess() {
                    }

                    override fun onFailed(error: QXError) {
                    }

                })
                callSession?.apply {
                    callState = CallState.CANCELED
//                    endTime = System.currentTimeMillis()
                }
                if (isVideoCall()) {
                    callEngine.stopPreview()
                }
                callDisconnected(QXCallState.CANCEL)
            }
            CALL_SEND_REFUSE -> {
                isNormalHangup = true
                callSession?.apply {
                    QXIMClient.instance.refuseCall(roomId, object : QXIMClient.OperationCallback() {

                        override fun onSuccess() {
                        }

                        override fun onFailed(error: QXError) {
                        }

                    })
                    callState = CallState.REFUSE
                }
                callDisconnected(QXCallState.REFRUSE)
            }
            CALL_SEND_ACCEPT -> {
                // 返回本地预览
                try {
                    val localView = callEngine.createRendererView(context, true)
                    val localUser = QXUser(QXIMClient.instance.getCurUserId()!!)
                    localUser.surfaceView = localView
                    callSession!!.userProfie.add(localUser)
                    callEngine.setLocalVideo(localView)
                    val remoteView = callEngine.createRendererView(context, false)
                    val remoteUser = QXUser(callSession!!.callId)
                    remoteUser.surfaceView = remoteView
                    callSession!!.userProfie.add(remoteUser)
                    callEngine.setRemoteVideo(remoteView)

                    callListener?.onCallOutgoing(callEngine.rtcCallMedia!!.localSurfaceViewRenderer!!)
                    if (isVideoCall()) {
                        callEngine.startPreview()
                    }
                    QLogTrace.instance.log("send accept  ${TimeUtil.getTime(System.currentTimeMillis())}")

                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                callSession?.apply {
                    QXIMClient.instance.acceptCall(roomId, object : QXIMClient.OperationCallback() {

                        override fun onSuccess() {
                            callState = CallState.CALL_ING
                            activeTime = System.currentTimeMillis()
                            callListener?.onCallConnecting()
                            connect()
                            // 加入房间
                            QLogTrace.instance.log("join room: ${TimeUtil.getTime(System.currentTimeMillis())}")
                            joinRoom(targetId)
                        }

                        override fun onFailed(error: QXError) {
                            callState = CallState.CALL_OVER
                            endTime = System.currentTimeMillis()
                            callError(error)
                        }

                    })

                }
            }
            CALL_SEND_HANGUP -> {
                QLog.e(TAG, "CALL_SEND_HANGUP ")
                isNormalHangup = true
                if (isVideoCall()) {
                    callEngine.stopPreview()
                }
                callSession?.apply {
                    callState = CallState.CALL_OVER
                    endTime = System.currentTimeMillis()
                }
                callLeaveRoom(QXCallState.HANGUP)
            }
            CALL_REC_ACCEPT -> {
                callListener?.onCallConnecting()
                QLogTrace.instance.log("receive call accept ${TimeUtil.getTime(System.currentTimeMillis())}")
            }
            CALL_REC_REFUSE -> {
                // 对方拒接
                if (isVideoCall()) {
                    callEngine.stopPreview()
                }
                callSession?.apply {
                    callState = CallState.REFUSE
                }
                callDisconnected(QXCallState.OTHER_REFRUSE)
            }
            CALL_REC_HANGUP -> {
                val oldRoomId = message.obj as String
                QLog.e(TAG, "CALL_REC_HANGUP oldRoomId $oldRoomId ,roomId:$roomId")
                if (oldRoomId == roomId) {
                    isNormalHangup = true
                    callSession?.apply {
                        endTime = System.currentTimeMillis()
                        callState = CallState.CALL_OVER
                    }
                    if (callSession != null) {
                        callLeaveRoom(QXCallState.OTHER_HANGUP)
                    }
                }
            }
            CALL_REC_CANCEL -> {
                isNormalHangup = true
                callSession?.apply {
                    callState = CallState.CANCELED
                }
                callDisconnected(QXCallState.CANCEL)
            }
            CALL_REC_TIME_OUT -> {
                if (isVideoCall()) {
                    callEngine.stopPreview()
                }
                callSession?.apply {
                    callState = CallState.CANCELED
                }
                callDisconnected(QXCallState.TIME_OUT)
                QLogTrace.instance.log("receive time out  ${TimeUtil.getTime(System.currentTimeMillis())}")
            }
            CALL_SEND_SWITCH_AUDIO -> {
                callSession?.callType = "1"
                QXIMClient.instance.switchAudio(callSession!!.roomId, object : QXIMClient.OperationCallback() {
                    override fun onSuccess() {
                        runOnUiThread(Runnable {
                            switchAudio()
                        })
                        if (callSession?.activeTime!! == 0L) {
                            val curLoginUserId = QXIMClient.instance.getCurUserId()
                            if (curLoginUserId == callSession!!.targetId) {
                                // 自动发起接受邀请
                                val message = Message.obtain()
                                message.what = CALL_SEND_ACCEPT
                                sendMessage(message)
                            }
                        }
                    }

                    override fun onFailed(error: QXError) {
                    }

                })
            }

            CALL_REC_SWITCH_AUDIO -> {
                callSession?.callType = "1"
                switchAudio()
            }

            RTC_SIGNAL_JOIN -> {
                QLogTrace.instance.log("receive join ${TimeUtil.getTime(System.currentTimeMillis())}")
                val joined = message.obj as RTCJoined
                for (peer in joined.peers!!) {
                    val peer = callEngine.createPeer(peer)
                    if (peer != null) {
                        callEngine.createOffer(peer)
                    }
                }
//                connected()
            }

            RTC_SIGNAL_JOINED -> {
                QLogTrace.instance.log("receive joined  ${TimeUtil.getTime(System.currentTimeMillis())}")
//                val joined = message.obj as RTCJoined
//                for(peer in joined.peers!!) {
//                     callEngine.createPeer(peer)
//                }
            }

            RTC_SIGNAL_REC_OFFER -> {
                val offer = message.obj as RTCOffer
                callEngine.createAnswer(offer.from!!, offer.sdp!!)
            }

            RTC_SIGNAL_REC_ANSWER -> {
                val offer = message.obj as RTCOffer
                callEngine.receiveAnswer(offer.from!!, offer.sdp!!)
            }

            RTC_SIGNAL_REC_CANDIDATE -> {
                val candidate = message.obj as RTCCandidate
                callEngine.updateCandidate(candidate.from!!, candidate.candidate!!)
            }

            RTC_REC_VIDEO_PARAM -> {
                val data = message.obj as RTCVideoParam
                callEngine.remoteVideoInfoChange(data.userId, data.param)
            }

            RTC_SEND_VIDEO_PARAM -> {

            }

            else -> {

            }
        }

        return@Callback true
    })

    private fun callError(error:QXError) {
        when (error) {
            QXError.MESSAGE_OBJECT_BUSING ->  callDisconnected(QXCallState.MESSAGE_OBJECT_BUSING)
            QXError.MESSAGE_BLACK_LIST ->  callDisconnected(QXCallState.NOT_FRIEND)
            QXError.PARAMS_INCORRECT ->  callDisconnected(QXCallState.ERROR_PARAM)
            else ->  callDisconnected(QXCallState.UNKOWN)
        }
    }

    /**
     * 已经加入房间 先退出RTC房间
     */
    private fun callLeaveRoom(state: QXCallState) {
        QLog.e(TAG, ">>>  callLeaveRoom : ${state.name}")
        callEngine.leaveRoom(state)
    }

    private fun callDisconnected(state: QXCallState) {
        QLog.e(TAG, ">>> callDisconnected : ${state.name}")
        callSession?.apply {
            QLog.e(TAG, "callDisconnected  isCalling:$isCalling hangUp")
            var cancelId = ""
            cancelId = if (targetId == QXIMClient.instance.getCurUserId()) {
                callId
            } else {
                targetId
            }
            if (state == QXCallState.OTHER_REFRUSE || state == QXCallState.REFRUSE || state == QXCallState.CANCEL) {
                if (endTime > 0) {
                    hangUp(roomId,cancelId)
                }
            } else {
                hangUp(roomId,cancelId)
            }

            // update ui
            runOnUiThread(Runnable {
                callActionCountDown = CallActionCountDown {
                    callListener?.onCallDisconnected(state)
                    callEngine.destory()
                    clearCallSession()
                }
                callActionCountDown?.start()
            })
            rtcConnectCountDown?.cancel()
        }

        //停止震动.
        VibratorUtils.cancel(context);
    }

    private fun connectedRefreshUi() {
        isCalling = true
        callSession?.apply {
            activeTime = System.currentTimeMillis()
            callState = CallState.CALL_ING
        }
        runOnUiThread(Runnable {
            if (callEngine.rtcCallMedia != null && callEngine.rtcCallMedia!!.remoteSurfaceViewRenderer != null) {
                callListener?.onCallConnected(callEngine.rtcCallMedia!!.remoteSurfaceViewRenderer!!)
            }
        })
    }

    private fun isVideoCall(): Boolean {
        callSession?.let {
            return it.callType == "2"
        }
        return false
    }

    private fun getMediaType(): QXCallMediaType {
        return if (callSession?.callType == "1") QXCallMediaType.AUDIO else QXCallMediaType.VIDEO
    }

    private var receiveListener: IReceiveCallListener? = null

    var callListener: IQXCallListener? = null
        set(value) {
            field = object : IQXCallListener {

                override fun onCallOutgoing(localView: SurfaceView) {
                    runOnUiThread(Runnable {
                        value?.onCallOutgoing(localView)
                    })
                }

                override fun onCallConnecting() {
                    runOnUiThread(Runnable {
                        value?.onCallConnecting()
                    })
                }

                override fun onCallConnected(remoteView: SurfaceView) {
                    runOnUiThread(Runnable {
                        value?.onCallConnected(remoteView)
                    })

                }

                override fun onCallDisconnected(callState: QXCallState) {
                    runOnUiThread(Runnable {
                        value?.onCallDisconnected(callState)
                    })

                }

                override fun onRemoteUserRinging(userId: String) {
                    runOnUiThread(Runnable {
                        value?.onRemoteUserRinging(userId)
                    })

                }

                override fun onRemoteUserJoined(viewType: Int, view: SurfaceView) {
                    runOnUiThread(Runnable {
                        value?.onRemoteUserJoined(viewType, view)
                    })

                }

                override fun onRemoteUserInvited(view: SurfaceView) {
                    runOnUiThread(Runnable {
                        value?.onRemoteUserInvited(view)
                    })

                }

                override fun mediaTypeChange() {
                    runOnUiThread(Runnable {
                        value?.mediaTypeChange()
                    })
                }

            }
        }

    private fun getRoomType(): String {
        if (callSession != null) {
            if (callSession!!.conversionType.equals("group", true)) {
                return "group"
            }
        }
        return "private"
    }

    private fun connect() {
        callSession?.roomId = roomId
        rtcConnectCountDown = RTCConnectCountDown {
            QLog.e(TAG, "WEB RTC connect time out")
            callSession?.apply {
                var cancelId = ""
                cancelId = if (targetId == QXIMClient.instance.getCurUserId()) {
                    callId
                } else {
                    targetId
                }
                QXIMClient.instance.sendCallError(roomId, cancelId,null)
            }
            callDisconnected(QXCallState.TIME_OUT)
        }
        rtcConnectCountDown?.start()
        callEngine.connect(userToken, serverHost, roomId, getRoomType(), object : IConnectListener {
            override fun join() {

            }

            override fun joined() {
                isCalling = true
                callSession?.apply {
                    activeTime = System.currentTimeMillis()
                }
                runOnUiThread(Runnable {
                    callListener?.onCallConnected(callEngine.rtcCallMedia!!.remoteSurfaceViewRenderer!!)
                })
            }

            override fun connecting() {

            }

            override fun connected() {
//                // ice已经连接完成
//                if (errorList.size > 0) {
//                    for (roomId in errorList) {
//                        hangUp(roomId)
//                    }
//                    errorList.clear()
//                }
                rtcConnectCountDown?.cancel()
                connectedRefreshUi()

            }

            override fun disconnect(callState: QXCallState) {
                QLog.e(TAG, "WEB RTC receive disconnect callSession:${callSession}")
                if (callSession == null)
                    return
                callDisconnected(callState)
            }

            override fun error(callState: QXCallState) {
                QLog.e(TAG, "WEB RTC error ${callState.name} ")
                callSession?.apply {
                    QLog.e(TAG, "WEB RTC error isCalling:$isCalling send CallError")
                    var cancelId = ""
                    cancelId = if (targetId == QXIMClient.instance.getCurUserId()) {
                        callId
                    } else {
                        targetId
                    }
                    QXIMClient.instance.sendCallError(roomId, cancelId,null)
                }
                callDisconnected(callState)
            }
        })
    }

    private fun hangUp(roomId: String,userId:String) {
        QLog.e(TAG, "hangUp roomId:$roomId,userid:$userId")
      //  ThreadPoolUtils.run {
            QXIMClient.instance.hangUp(roomId, userId,object : QXIMClient.OperationCallback() {

                override fun onSuccess() {
                    QLog.e(TAG, "hangUp onSuccess $roomId")
                    instance.roomId = ""
                }

                override fun onFailed(error: QXError) {
                    QLog.e(TAG, "hangUp onFailed $roomId")

                }

            })
       // }
    }


    fun joinRoom(otherId: String) {
        // 发送加入房间信令
        callEngine.joinRoom(roomId)
    }

    fun joined() {
        callEngine.joined(roomId)
    }

    fun switchCamera() {
        callEngine.switchCamera()
        cameraType = if (cameraType == Const.BACK_FACING) Const.FONT_FACTING else Const.BACK_FACING
        val data = RTCVideoParam()
        data.roomId = roomId
        data.userId = QXIMClient.instance.getCurUserId()
        val param = RTCVideoParam.Param()
        param.camera = cameraType
        data.param = Gson().toJson(param)
        QXIMClient.instance.rtcVideoParam(data, object : QXIMClient.ResultCallback<String>() {
            override fun onSuccess(data: String) {

            }

            override fun onFailed(error: QXError) {
            }

        })
    }

    fun mute(mute: Boolean) {
        callEngine.mute(mute)
    }

    fun handsFree(handsFree: Boolean) {
        callEngine.handsFree(handsFree)
    }

    fun switchAudio() {
        callEngine.switchAudio()
        callListener?.mediaTypeChange()

    }

    fun runOnUiThread(runnable: Runnable) {
        uiHandler.post(runnable)
    }

    fun setServerHost(host: String) {
        this.serverHost = host
    }

    fun setUserToken(userToken: String) {
        this.userToken = userToken
    }

    private inner class RTCConnectCountDown(var block: () -> Unit) {
        private var timer: CountDownTimer? = null

        fun start() {
            timer?.onFinish()
            timer = object : CountDownTimer(35000, 1000) {

                override fun onFinish() {
                    block()
                    QLog.e(TAG, "RTCConnectCountDown onFinish")
                }

                override fun onTick(millisUntilFinished: Long) {
                    QLog.e(TAG, "RTCConnectCountDown onTick")
                }

            }
            timer?.start()
        }

        fun cancel() {
            timer?.cancel()
        }
    }

    private inner class CallActionCountDown(var block: () -> Unit) {

        private var timer: CountDownTimer? = null

        fun start() {
            timer?.onFinish()
            timer = object : CountDownTimer(2000, 1000) {

                override fun onFinish() {
                    block()
                    QLog.e(TAG, "CallActionCountDown onFinish")
                }

                override fun onTick(millisUntilFinished: Long) {
                    QLog.e(TAG, "CallActionCountDown onTick")
                }

            }
            timer?.start()
        }


    }


    companion object {

        const val CALL_OUTGOING = 100

        /**
         * 收到呼叫
         */
        const val CALL_INCOMING = 101
        const val CALL_SEND_CANCEL = 102
        const val CALL_SEND_REFUSE = 103
        const val CALL_SEND_ACCEPT = 104

        /**
         * 挂断
         */
        const val CALL_SEND_HANGUP = 105
        const val CALL_REC_ACCEPT = 106
        const val CALL_REC_REFUSE = 107
        const val CALL_REC_HANGUP = 108
        const val CALL_REC_CANCEL = 109
        const val CALL_REC_TIME_OUT = 110
        const val CALL_SEND_SWITCH_AUDIO = 111
        const val CALL_REC_SWITCH_AUDIO = 112

        const val RTC_SIGNAL_JOIN = 120
        const val RTC_SIGNAL_JOINED = 121
        const val RTC_SIGNAL_REC_OFFER = 122
        const val RTC_SIGNAL_REC_ANSWER = 123
        const val RTC_SIGNAL_REC_CANDIDATE = 124

        const val RTC_REC_VIDEO_PARAM = 127
        const val RTC_SEND_VIDEO_PARAM = 128

        val errorList: MutableList<String> = mutableListOf()

        var rtcServerConfig: RTCServerConfig? = null
            get() {
                if (field == null)
                    field = QXIMClient.instance.rtcSercerConfig()
                return field
            }

        @JvmStatic
        val instance: QXCallManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            QXCallManager()
        }

    }

    fun setReceivedListener(listener: IReceiveCallListener) {
        receiveListener = object : IReceiveCallListener {
            override fun onReceivedCall(session: QXCallSession) {
                listener.onReceivedCall(session)
            }

        }
    }

    fun sendMessage(message: Message) {
        uiHandler.sendMessage(message)
    }

    fun getCallSession(): QXCallSession? {
        return callSession
    }

    fun clearCallSession() {
        callSession = null
    }

}
