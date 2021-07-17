package com.qx.imlib

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Parcel
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.google.protobuf.GeneratedMessageV3
import com.qx.im.model.ConversationModel
import com.qx.im.model.ConversationType
import com.qx.im.model.RTCServerConfig
import com.qx.im.model.UserInfoCache
import com.qx.im.model.UserInfoCache.setToken
import com.qx.im.model.UserInfoCache.setUserId
import com.qx.imlib.db.IMDatabaseRepository
import com.qx.imlib.db.entity.MessageEntity
import com.qx.imlib.handler.CustomEventManager
import com.qx.imlib.job.JobManagerUtil
import com.qx.imlib.job.ResultCallback
import com.qx.imlib.netty.*
import com.qx.imlib.qlog.QLog
import com.qx.imlib.utils.MessageConvertUtil
import com.qx.imlib.utils.SharePreferencesUtil
import com.qx.imlib.utils.encry.Key
import com.qx.imlib.utils.http.*
import com.qx.imlib.utils.mingan.SensitiveWordsUtils
import com.qx.imlib.utils.net.NetWorkMonitorManager
import com.qx.it.protos.*
import com.qx.message.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

internal class LibHandlerStub constructor(
    private val mContext: Context,
    private val mAppkey: String
) : IHandler.Stub() {

    private val mWorkHandler: Handler
    private var mConnectStringCallback: IConnectStringCallback? = null
    private var mOnReceiveMessageListener: IOnReceiveMessageListener? = null
    private var mConversationListener: IConversationListener? = null
    private var mMessageReceiptListener: IMessageReceiptListener? = null
    private var mOnChatRoomMessageReceiveListener: IOnChatRoomMessageReceiveListener? = null
    private var mOnChatNoticeReceivedListenerList = arrayListOf<IOnChatNoticeReceivedListener>()
    private var connectionStatus: ConnectionStatusListener.Status? = null
    private var mConnectionStatusListener: IConnectionStatusListener? = null

    private var mNettyClientModel: NettyClientModel? = null
    private var isNetworkAvailable = false
    private var mCallReceiveMessageListener: ICallReceiveMessageListener? = null

    // RTC信令消息回调
    private var mRTCReceiveMessageListener: IRTCMessageListener? = null

    private var rtcConfig: RTCServerConfig? = null

    companion object {
        private val TAG = LibHandlerStub::class.java.simpleName
    }

    init {
        val handlerThread = HandlerThread("IPC_SERVICE")
        handlerThread.start()
        mWorkHandler = Handler(handlerThread.looper)
        mNettyClientModel = NettyClientModel(mContext)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        if (mContext.applicationContext is Application) {
            NetWorkMonitorManager.getInstance().init(mContext.applicationContext as Application)
            NetWorkMonitorManager.getInstance().register(this)
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        try {
            return super.onTransact(code, data, reply, flags)
        } catch (e: java.lang.Exception) {
            throw e;
        }
    }

    override fun connectServer(
        token: String,
        imServerUrl: String,
        callback: IConnectStringCallback
    ) {
        UserInfoCache.appKey = mAppkey
        HttpUtil.SERVER_URL = imServerUrl
        UserInfoCache.setToken(token)
        mConnectStringCallback = callback
        getHttpPubKey()
    }

    private fun getHttpPubKey() {
        HttpUtil.getHttpPubKey(object : HttpUtil.HttpResponseListener {
            override fun onProcess() {
            }

            override fun onSuccess(obj: Any?) {
                var result = obj as BeanPubKey
                Key.HTTP_SERVER_PUB_KEY = result.data.p
                getServerIp()
            }

            override fun onFailed(code: Int, message: String?) {
                QLog.e(TAG, "code=$code message=$message")
                mConnectStringCallback?.onFailure(code, message)
            }

        })
    }

    private fun getServerIp() {
        HttpUtil.getServerIp(
            mAppkey,
            UserInfoCache.getToken(),
            object : HttpUtil.HttpResponseListener {
                override fun onProcess() {
                }

                override fun onSuccess(obj: Any?) {
                    var result = obj as BeanResponse
                    if (obj.data != null) {
                        saveServerIp(result.data as String)
                        getTextFilter()
                        connect()
                    } else {
                        mConnectStringCallback?.onFailure(result.code, result.message)
                    }
                }

                override fun onFailed(code: Int, message: String?) {
                    QLog.e(TAG, "code=$code message=$message")
                    mConnectStringCallback?.onFailure(
                        code,
                        if (message.isNullOrBlank()) "get server ip error" else message
                    )
                }
            })
    }

    private fun getTextFilter() {
        HttpUtil.getSensitiveWord(mAppkey, object : HttpUtil.HttpResponseListener {
            override fun onProcess() {

            }

            override fun onSuccess(obj: Any?) {
                if (obj != null) {
                    var data = obj as BeanGetSensitiveWord
                    SharePreferencesUtil.getInstance(mContext)
                        .saveSensitiveWord(Gson().toJson(data.data))
                }
            }

            override fun onFailed(code: Int, message: String?) {

            }
        })
    }

    private fun saveServerIp(serverList: String) {
        var result = serverList.split(":".toRegex()).toTypedArray()
        TcpServer.host = result[0]
        TcpServer.port = result[1].toInt()
    }

    private fun updatePushInfo() {
        HttpUtil.uploadPushToken(
            mAppkey,
            UserInfoCache.getToken(),
            "3",
            "I7ZXt4OFXBfrO3A9FUnFPFW2MZAeFkV2udW4PmM9gyRkds3zw58MnkmzMGIAkzV5",
            object : HttpUtil.HttpResponseListener {
                override fun onProcess() {
                }

                override fun onSuccess(obj: Any?) {
                }

                override fun onFailed(code: Int, message: String?) {
                }

            })
    }

    private fun connect() {
        mNettyClientModel!!.initNettyClient(UserInfoCache.getToken())
    }

    override fun disconnect() {
    }

    override fun setConversationNoDisturbing(
        conversationId: String?,
        isNoDisturbing: Boolean,
        callback: IResultCallback?
    ) {
        var type = "cancel"
        if (isNoDisturbing) {
            type = "set"
        }
        var noDisturbing = 0
        if (isNoDisturbing) {
            noDisturbing = 1
        }
        if (IMDatabaseRepository.instance.updateConversationNoDisturbing(
                noDisturbing,
                conversationId!!
            ) > 0
        ) {
            var conversationEntity =
                IMDatabaseRepository.instance.getConversationById(conversationId)
            if (conversationEntity != null) {
                val body = S2CSpecialOperation.SpecialOperation.newBuilder()
                    .setSendType(conversationEntity.conversationType)
                    .setTargetId(conversationEntity.targetId)
                    .setUserId(UserInfoCache.getUserId())
                    .setType(type).build()
                postOperation(SystemCmd.C2S_MESSAGE_MUTED, body, callback!!)
                return
            }
        }
        callback?.onFailed(QXError.DB_NO_ROW_FOUND.ordinal)
    }

    private fun postOperation(cmd: Short, body: GeneratedMessageV3?, callback: IResultCallback) {
        val msg = S2CSndMessage()
        msg.cmd = cmd
        if (body != null) {
            msg.body = body
        }
        post(msg, callback)
    }

    private fun post(msg: S2CSndMessage, callback: IResultCallback, taskId: String = "") {
        JobManagerUtil.instance.postMessage(msg, object : ResultCallback {
            override fun onSuccess() {
                try {
                    if (callback != null) {
                        callback?.onSuccess()
                    }
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun onFailed(error: QXError) {
                try {
                    if (callback != null) {
                        callback?.onFailed(error.ordinal)
                    }
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }, taskId)
    }

    override fun startCall(
        conversationType: String?,
        targetId: String?,
        roomId: String?,
        callType: String?,
        userIds: MutableList<String>?,
        callback: IResultCallback
    ) {
        val body = C2SVideoLaunch.VideoLaunch.newBuilder().setRoomId(roomId)
            .setSendType(conversationType)
            .setTargetId(targetId)
            .addAllUserIds(userIds)
            .setType(callType)
            .build()
        postOperation(SystemCmd.C2S_VIDEO_LAUNCH, body, callback)
    }

    override fun acceptCall(roomId: String?, callback: IResultCallback) {
        val body = C2SVideoAnswer.VideoAnswer.newBuilder().setRoomId(roomId).build()
        postOperation(SystemCmd.C2S_VIDEO_ANSWER, body, callback)
    }

    override fun cancelCall(roomId: String?, callback: IResultCallback) {
        val body = C2SVideoAnswer.VideoAnswer.newBuilder().setRoomId(roomId).build()
        postOperation(SystemCmd.C2S_VIDEO_CANCEL, body, callback)
    }

    override fun refuseCall(roomId: String?, callback: IResultCallback) {
        val body = C2SVideoAnswer.VideoAnswer.newBuilder().setRoomId(roomId).build()
        postOperation(SystemCmd.C2S_VIDEO_REFUSE, body, callback)
    }

    override fun hangUp(roomId: String?, userId: String?, callback: IResultCallback) {
        val body =
            C2SVideoAnswer.VideoAnswer.newBuilder().setRoomId(roomId).setUserId(userId).build()
        postOperation(SystemCmd.C2S_VIDEO_RING_OFF, body, callback)
    }

    override fun setChatRoom(
        chatRoomId: String?,
        name: String?,
        value: String?,
        autDel: Int,
        callback: IResultCallback
    ) {
        val body = C2SChatroomProperty.SetProperty.newBuilder().setChatroomId(chatRoomId)
            .setPropName(name)
            .setPropValue(value)
            .setAutoDelete(autDel).build()
        postOperation(SystemCmd.C2S_CHATROOM_SET_PROP, body, callback)
    }

    override fun delChatRoom(chatRoomId: String?, name: String?, callback: IResultCallback) {
        val body =
            C2SChatroomProperty.SetProperty.newBuilder().setChatroomId(chatRoomId).setPropName(name)
                .build()
        postOperation(SystemCmd.C2S_CHATROOM_DEL_PROP, body, callback)
    }

    override fun getChatRoom(chatRoomId: String?, name: String?, callback: IResultCallback) {
        val body =
            C2SChatroomProperty.SetProperty.newBuilder().setChatroomId(chatRoomId).setPropName(name)
                .build()
        postOperation(SystemCmd.C2S_CHATROOM_GET_PROP, body, callback)
    }

    override fun joinChatRoom(chatRoomId: String?, callback: IResultCallback) {
        val body = S2CChatroomJoin.ChatroomJoin.newBuilder().setChatroomId(chatRoomId).build()
        postOperation(SystemCmd.C2S_JOIN_CHATROOM, body, callback)
    }

    override fun exitChatRoom(chatRoomId: String?, callback: IResultCallback) {
        val body = S2CChatroomJoin.ChatroomJoin.newBuilder().setChatroomId(chatRoomId).build()
        postOperation(SystemCmd.C2S_EXIT_CHATROOM, body, callback)
    }

    override fun sendOnly(message: com.qx.message.Message, callback: ISendMessageCallback) {
        val s2CSndMessage = MessageConvertUtil.instance.messageToS2CSndMsg(message)
        when (message.messageType) {
            MessageType.TYPE_AUDIO, MessageType.TYPE_IMAGE, MessageType.TYPE_VIDEO, MessageType.TYPE_FILE, MessageType.TYPE_GEO -> {
                updateOriginPath(message)
            }
        }
        JobManagerUtil.instance.postMessage(s2CSndMessage, object : ResultCallback {
            override fun onSuccess() {
                callback.onSuccess()
            }

            override fun onFailed(error: QXError) {
                message.state = Message.State.STATE_FAILED
                message.failedReason = error.code.toString() + "-" + error.extra
                if (IMDatabaseRepository.instance.updateMessageState(
                        message.messageId,
                        MessageEntity.State.STATE_FAILED, message.failedReason
                    ) > 0
                ) {
                    callback.onError(error.ordinal, message)
                }
            }
        }, message.messageId)
    }

    override fun saveOnly(message: com.qx.message.Message, callback: ISendMessageCallback) {
        if (message.conversationType == ConversationType.TYPE_PRIVATE || message.conversationType == ConversationType.TYPE_GROUP) {
            //如果为输入状态消息，则不保存
            if (message.messageType !== MessageType.TYPE_STATUS) {
                val messageEntity = MessageConvertUtil.instance.messageToMessageEntity(message)
                saveToDB(messageEntity, callback)
            }
        } else {
            callback.onAttached(message)
        }
    }

    override fun updateMessageState(messageId: String, state: Int): Int {
        return IMDatabaseRepository.instance.updateMessageState(messageId, state, "")
    }

    override fun updateMessageStateAndTime(messageId: String?, timestamp: Long, state: Int): Int {
        return IMDatabaseRepository.instance.updateMessageStateAndTime(
            state,
            timestamp,
            messageId!!
        )
    }

    private fun saveToDB(messageEntity: MessageEntity, callback: ISendMessageCallback) {
        GlobalScope.launch {
            var conversationEntity = ConversationModel.instance.generateConversation(messageEntity)
            if (IMDatabaseRepository.instance.insertConversation(conversationEntity) > 0) {
                IMDatabaseRepository.instance.updateDraft("", conversationEntity.conversationId)
                messageEntity.conversationId = conversationEntity.conversationId
                messageEntity.state = MessageEntity.State.STATE_SENDING
                var result = IMDatabaseRepository.instance.insertMessage(messageEntity)
                if (result.messages.isNotEmpty()) {
                    IMDatabaseRepository.instance!!.updateTimeIndicator(
                        conversationEntity.conversationId, messageEntity.timestamp
                    )
                    for (msg in result.messages) {
                        var message = MessageConvertUtil.instance.convertToMessage(msg)!!
                        if (message != null) {
                            callback.onAttached(message)
                        } else {
                            callback.onError(QXError.PARAMS_INCORRECT.ordinal, message)
                        }
                    }
                }
                if (conversationEntity.isNew) {
                    ConversationModel.instance.getConversationProperty(conversationEntity!!)
                }
            }
        }
    }

    override fun getConversationProperty(
        conversation: com.qx.message.Conversation,
        callback: IResultCallback?
    ) {
        var conversationEntity = ConversationUtil.toConversationEntity(conversation)
        ConversationModel.instance.getConversationProperty(conversationEntity)
    }

    override fun sendHeartBeat() {
        QLog.i(TAG, "发送心跳包")
        if (HeartBeatHolder.getInstance().isNeedSendHeatBeat) {
            HeartBeatTimeCheck.getInstance().startTimer()
        }
        var msg = S2CSndMessage()
        msg.cmd = SystemCmd.C2S_HEARTBEAT
        JobManagerUtil.instance.postHeatBeat(msg)
    }

    override fun sendLogout(pushName: String?, callback: IResultCallback) {
        setUserId("")
        setToken("")
        val body = C2SLogOut.LogOut.newBuilder()
            .setManufacturer(PushType.getType(pushName).type.toString()).build()
        postOperation(SystemCmd.C2S_LOGOUT, body, callback)
    }

    override fun sendMessageReadReceipt(
        conversationType: String,
        targetId: String,
        lastTimestamp: Long,
        callback: IResultCallback
    ) {
        QLog.i(TAG, "sendMessageReceipt 发送已读回执")
        var body = C2SMessageRead.MessageReadConfirm.newBuilder().setSendType(conversationType)
            .setTargetId(targetId)
            .setLastTimestamp(lastTimestamp).build()
        GlobalScope.launch {
            var conversationEntity =
                IMDatabaseRepository.instance.getConversation(conversationType, targetId)
            //根据会话id,设置当前会话所有消息为已读状态
            if (conversationEntity != null) {
                IMDatabaseRepository.instance.updateMessageStateByConversationId(
                    conversationEntity.conversationId,
                    MessageEntity.State.STATE_READ
                )
                //清空内存会话中的未读数量
                IMDatabaseRepository.instance.updateConversationUnReadCount(
                    conversationType,
                    targetId,
                    0
                )
                //如果是群组，则清空会话@信息
                if (conversationType == ConversationType.TYPE_GROUP) {
                    //      IMDatabaseRepository.instance.updateConversationAtTO(targetId, "")
                }
            }
        }
        postOperation(SystemCmd.C2S_MESSAGE_READ, body, callback)
    }

    override fun deleteRemoteMessageByMessageId(
        conversationType: String,
        targetId: String,
        messageIds: MutableList<String>,
        callback: IResultCallback
    ) {
        var body = C2SMessageDelete.MessageDelete.newBuilder().setSendType(conversationType)
            .setTargetId(targetId)
            .addAllMessageIds(messageIds).build()
        postOperation(SystemCmd.C2S_MESSAGE_DEL, body, callback)
    }

    override fun deleteRemoteMessageByTimestamp(
        conversationType: String,
        targetId: String,
        timestamp: Long,
        callback: IResultCallback
    ) {
        var body = C2SMessageDelete.MessageDelete.newBuilder().setSendType(conversationType)
            .setTargetId(targetId)
            .setTimestamp(timestamp).build()
        postOperation(SystemCmd.C2S_MESSAGE_DEL, body, callback)
    }

    override fun deleteLocalMessageByTimestamp(
        conversationType: String,
        targetId: String,
        timestamp: Long
    ): Int {
        return IMDatabaseRepository.instance.deleteLocalMessageByTimestamp(
            conversationType,
            targetId,
            timestamp
        )
    }

    override fun updateOriginPath(message: com.qx.message.Message): Int {
        return IMDatabaseRepository.instance.updateOriginPath(
            MessageConvertUtil.instance.messageToMessageEntity(
                message
            )
        )
    }

    override fun updateLocalPath(message: com.qx.message.Message): Int {
        return IMDatabaseRepository.instance.updateLocalPath(
            MessageConvertUtil.instance.messageToMessageEntity(
                message
            )
        )
    }

    override fun updateHearUrl(messageId: String?, headUrl: String?): Int {
        return IMDatabaseRepository.instance.updateVideoHeadUrl(messageId!!, headUrl!!)
    }

    override fun sendRecall(message: com.qx.message.Message?, callback: IResultCallback?) {
        var messageEntity = MessageConvertUtil.instance.messageToMessageEntity(message!!)
        var body = C2SMessageRecall.MessageRecall.newBuilder().setSendType(messageEntity.sendType)
            .setMessageId(message.messageId).setTargetId(messageEntity.to).build()
        postOperation(SystemCmd.C2S_MESSAGE_RECALL, body, callback!!)
    }

    override fun deleteLocalMessageById(messageIds: Array<String>?): Int {
        return IMDatabaseRepository.instance.markMessageDelete(messageIds!!)
    }

    override fun setConversationTop(
        conversationId: String?,
        isTop: Boolean,
        callback: IResultCallback?
    ) {
        var type = if (isTop) {
            "set"
        } else {
            "cancel"
        }
        var top = if (isTop) {
            1
        } else {
            0
        }
        if (IMDatabaseRepository.instance.updateConversationTop(top, conversationId!!) > 0) {
            var conversationEntity =
                IMDatabaseRepository.instance.getConversationById(conversationId)
            if (conversationEntity != null) {
                var body = S2CSpecialOperation.SpecialOperation.newBuilder()
                    .setSendType(conversationEntity.conversationType)
                    .setTargetId(conversationEntity.targetId).setUserId(UserInfoCache.getUserId())
                    .setType(type).build()
                postOperation(SystemCmd.C2S_SESSION_TOP, body, callback!!)
                return
            }
        }
        callback?.onFailed(QXError.DB_NO_ROW_FOUND.ordinal)
    }

    override fun deleteConversation(conversationId: String?): Int {
        return IMDatabaseRepository.instance.deleteConversation(conversationId!!)
    }

    override fun deleteConversationByTargetId(type: String?, targetId: String?): Int {
        return IMDatabaseRepository.instance.deleteConversation(type!!, targetId!!)
    }

    override fun deleteAllConversation(): Int {
        return IMDatabaseRepository.instance.deleteAllConversation()
    }

    override fun searchTextMessage(content: String): MutableList<com.qx.message.Message> {
        var result = IMDatabaseRepository.instance.searchTextMessageByContent(content, null)
        var list = arrayListOf<Message>()
        if (result != null) {
            for (msg in result) {
                var m = MessageConvertUtil.instance.convertToMessage(msg)
                if (m != null) {
                    list.add(m)
                }
            }
        }
        return list
    }

    override fun searchTextMessageByConversationId(
        content: String,
        conversationId: String?
    ): MutableList<com.qx.message.Message> {
        var result =
            IMDatabaseRepository.instance.searchTextMessageByContent(content, conversationId)
        var list = arrayListOf<Message>()
        if (result != null) {
            for (msg in result) {
                var m = MessageConvertUtil.instance.convertToMessage(msg)
                if (m != null) {
                    list.add(m)
                }
            }
        }
        return list

    }

    override fun getMessages(
        conversationType: String?,
        targetId: String?,
        offset: Int,
        pageSize: Int
    ): MutableList<com.qx.message.Message> {
        QLog.d(
            TAG,
            "获取本地消息： 从第$offset 条 - " + (offset + pageSize - 1) + " 记录开始    pageSize为:" + pageSize
        )
        var result = IMDatabaseRepository.instance.getMessages(
            conversationType!!,
            targetId!!,
            offset,
            pageSize
        )
        var list = arrayListOf<Message>()
        if (result != null) {
            for (msg in result) {
                var m = MessageConvertUtil.instance.convertToMessage(msg)
                if (m != null) {
                    list.add(m)
                }
            }
        }
        Log.e(TAG, "getMessages: 获取到几条消息：" + list.size)
        return list
    }

    override fun getHistoryMessageFromLocal(
        conversationId: String?,
        conversationType: String?,
        targetId: String?,
        offset: Int,
        pageSize: Int
    ): MutableList<com.qx.message.Message> {
        IMDatabaseRepository.instance.calcUnTrustTime2(conversationId!!, targetId!!, 0, 0)
        var result = IMDatabaseRepository.instance.getHistoryMessagesFromLocal(
            conversationId!!,
            conversationType!!,
            targetId!!,
            offset,
            pageSize
        )
        var list = arrayListOf<Message>()
        if (result != null) {
            for (msg in result) {
                var m = MessageConvertUtil.instance.convertToMessage(msg)
                if (m != null) {
                    list.add(m)
                }
            }
        }
        return list
    }

    override fun getHistoryMessageFromService(
        conversationType: String?,
        targetId: String?,
        timestamp: Long,
        searchType: Int
    ) {
        IMDatabaseRepository.instance.getHistoryMessage(
            conversationType!!,
            targetId!!,
            timestamp,
            searchType
        )
    }

    override fun getAllMessages(): MutableList<com.qx.message.Message> {
        var result = IMDatabaseRepository.instance.getMessages()
        var list = arrayListOf<Message>()
        if (result != null) {
            for (msg in result) {
                var m = MessageConvertUtil.instance.convertToMessage(msg)
                if (m != null) {
                    list.add(m)
                }
            }
        }
        return list
    }

    override fun getMessagesByConversationID(conversationId: String?): MutableList<com.qx.message.Message> {
        var result = IMDatabaseRepository.instance.getMessagesByConversationID(conversationId!!)
        var list = arrayListOf<Message>()
        if (result != null) {
            for (msg in result) {
                var m = MessageConvertUtil.instance.convertToMessage(msg)
                if (m != null) {
                    list.add(m)
                }
            }
        }
        Log.e(
            TAG,
            "getMessagesByConversationID: 查询会话ID为：" + conversationId + "     查询结果为：" + list.size
        )
        return list
    }

    override fun getLatestMessageByConversationId(
        conversationId: String?,
        ownerId: String?
    ): com.qx.message.Message? {
        var result = IMDatabaseRepository.instance.getLatestMessageByConversationId(
            conversationId!!,
            ownerId!!
        )
        var message: Message? = MessageConvertUtil.instance.convertToMessage(result!!)
        if (message != null) {
            Log.e(
                TAG,
                "getMessagesByConversationID: 查询会话ID为：" + conversationId + "     查询结果为：" + message.timestamp
            )
        }
        return message
    }

    override fun getMessagesByTimestamp(
        conversationType: String?,
        targetId: String?,
        timestamp: Long,
        searchType: Int,
        pageSzie: Int
    ): MutableList<com.qx.message.Message> {
        var result = IMDatabaseRepository.instance.getMessagesByTimestamp(
            conversationType!!,
            targetId!!,
            timestamp,
            searchType,
            pageSzie
        )
        var list = arrayListOf<Message>()
        if (result != null) {
            for (msg in result) {
                var m = MessageConvertUtil.instance.convertToMessage(msg)
                if (m != null) {
                    list.add(m)
                }
            }
        }
        return list
    }

    override fun getMessagesByType(
        conversationId: String?,
        types: MutableList<String>?,
        offset: Int,
        pageSize: Int,
        isAll: Boolean,
        isDesc: Boolean
    ): MutableList<com.qx.message.Message> {
        var result = IMDatabaseRepository.instance.getMessagesByType(
            conversationId!!, types!!, offset, pageSize, isAll, isDesc
        )
        var list = arrayListOf<Message>()
        if (result != null) {
            for (msg in result) {
                var m = MessageConvertUtil.instance.convertToMessage(msg)
                if (m != null) {
                    list.add(m)
                }
            }
        }
        return list
    }

    override fun getUnReadAtMessages(conversationId: String?): MutableList<com.qx.message.Message> {
        if (TextUtils.isEmpty(conversationId)) {
            return null!!
        }
        var result = IMDatabaseRepository.instance.getUnReadAtToMessage(conversationId!!)
        var list = arrayListOf<Message>()
        if (result != null) {
            for (msg in result) {
                MessageConvertUtil.instance.convertToMessage(msg!!)?.let { list.add(it) }
            }
        }
        return list
    }

    override fun getFirstUnReadMessage(conversationId: String?): com.qx.message.Message? {
      var result = IMDatabaseRepository.instance.getFirstUnReadMessage(conversationId!!)
        if (result!= null){
            return MessageConvertUtil.instance.convertToMessage(result!!)
        }
        return null
    }

    override fun getConversation(
        conversationType: String?,
        targetId: String?
    ): com.qx.message.Conversation? {
       var resutl  = IMDatabaseRepository.instance.getConversation(conversationType!!,targetId!!)
        return ConversationUtil.toConversation(resutl!!)
    }

    override fun getAllConversation(): MutableList<com.qx.message.Conversation> {
        var result = IMDatabaseRepository.instance.getAllConversation()
        var list = arrayListOf<Conversation>()
        if (result != null) {
            for (conv in result) {
                list.add(ConversationUtil.toConversation(conv)!!)
            }
        }
        return list
    }

    override fun getConversationInRegion(region: MutableList<String>?): MutableList<com.qx.message.Conversation> {
        var result = IMDatabaseRepository.instance.getConversationInRegion(region!!)
        var list = arrayListOf<Conversation>()
        if (result != null) {
            for (conv in result) {
                //此处因会话列表在滑动时,recyclerview 列表有缓存池，如果此处为空，可能会造成空白
                var toConversation = ConversationUtil.toConversation(conv)
                if (toConversation != null) {
                    list.add(toConversation!!)
                }
            }
        }
        return list
    }

    override fun updateConversationDraft(conversationId: String?, draft: String?): Int {
        return IMDatabaseRepository.instance.updateDraft(draft!!, conversationId!!)
    }

    override fun updateConversationTitle(type: String?, targetId: String?, title: String?): Int {
    return IMDatabaseRepository.instance.updateConversationTitle(type!!,targetId!!,title!!)
    }

    override fun updateConversationIcon(type: String?, targetId: String?, icon: String?): Int {
    return IMDatabaseRepository.instance.updateConversationIcon(type!!,targetId!!,icon!!)
    }

    override fun updateConversationTitleAndIcon(
        type: String?,
        targetId: String?,
        title: String?,
        icon: String?
    ): Int {
      return IMDatabaseRepository.instance.updateConversationTitleAndIcon(type!!,targetId!!,title!!,icon!!)
    }

    override fun clearMessages(conversationId: String?): Int {
        return IMDatabaseRepository.instance.deleteMessageByConversationId(conversationId!!)
    }

    override fun setReceiveMessageListener(listener: IOnReceiveMessageListener?) {
      mOnReceiveMessageListener = listener
    }

    override fun setMessageReceiptListener(listener: IMessageReceiptListener?) {
     mMessageReceiptListener = listener
    }

    override fun addOnChatNoticeReceivedListener(listener: IOnChatNoticeReceivedListener?) {
        mOnChatNoticeReceivedListenerList.add(listener!!)
    }

    override fun setOnChatRoomMessageReceiveListener(listener: IOnChatRoomMessageReceiveListener?) {
        mOnChatRoomMessageReceiveListener = listener
    }

    override fun setConversationListener(listener: IConversationListener?) {
        mConversationListener = listener
    }

    override fun setConnectionStatusListener(listener: IConnectionStatusListener?) {
        mConnectionStatusListener = listener
    }

    override fun setCallReceiveMessageListener(listener: ICallReceiveMessageListener?) {
        mCallReceiveMessageListener = listener
    }

    override fun switchAudioCall(roomId: String?, callback: IResultCallback) {
       var body = C2SVideoAnswer.VideoAnswer.newBuilder().setRoomId(roomId).build()
        postOperation(SystemCmd.C2S_VIDEO_SWITCH,body,callback)
    }

    override fun getServerHost(): String {
       return TcpServer.host
    }

    override fun getCurUserId(): String {
       return  UserInfoCache.getUserId()
    }

    override fun getHttpHost(): String {
        return HttpUtil.SERVER_URL
    }

    override fun getRSAKey(): String {
        return Key.HTTP_SERVER_PUB_KEY
    }

    override fun sendCallError(roomId: String?, targetId: String?, callback: IResultCallback) {
        var body  = C2SVideoAnswer.VideoAnswer.newBuilder().setRoomId(roomId).setUserId(targetId).build()
        postOperation(SystemCmd.C2S_VIDEO_ERROR,body,callback)
    }

    override fun searchConversations(
        keyWord: String?,
        conversationTypes: Array< String>?,
        messageTypes: Array< String>?
    ): MutableList<com.qx.im.model.SearchConversationResult>? {
  return IMDatabaseRepository.instance.searchConversations(keyWord!!,conversationTypes!!,messageTypes!!).toMutableList()
    }

    override fun getAllUnReadCount(region: MutableList<String>?): Int {
        var count = IMDatabaseRepository.instance.getAllUnReadCount(region!!)
        if (count == null){
            count = 0
        }
        return count
    }

    override fun getConversationUnReadCount(
        conversationId: String?,
        isIgnoreNoDisturbing: Boolean
    ): Int {
        var count = IMDatabaseRepository.instance.getConversationUnReadCount(conversationId!!, isIgnoreNoDisturbing)
        if (count == null) {
            count = 0
        }
        return count
    }

    override fun checkSensitiveWord(text: String?): com.qx.message.SensitiveWordResult {
        return SensitiveWordsUtils.checkSensitiveWord(text, mContext)
    }

    override fun registerCustomEventProvider(provider: ICustomEventProvider?): Boolean {
        if (provider != null) {
            return CustomEventManager.registerCustomEventProvider(provider)
        }
        return false
    }

    override fun isMessageExist(messageId: String?): Boolean {
        if (TextUtils.isEmpty(messageId)) {
            return false;
        }
        return IMDatabaseRepository.instance.isMessageExist(messageId!!)
    }

    //检查数据库中正在发送的消息
    override fun checkSendingMessage(): MutableList<com.qx.message.Message> {
        var messages = ArrayList<Message>()
        var result = IMDatabaseRepository.instance.checkSendingMessage()
        if (result.isNullOrEmpty()) {
            return messages
        }
        for (messageEntity in result) {
            var message = MessageConvertUtil.instance.convertToMessage(messageEntity)
            if (message != null) {
                //如果该消息不在消息队列中，则入列
                var isExist = JobManagerUtil.instance.isExist(message.messageId)
                if (!isExist) {
                    messages.add(message)
                }
            }
        }
        return messages
    }

    override fun updateConversationBackground(url: String?, conversationId: String?): Int {
        return IMDatabaseRepository.instance.updateConversationBackground(url!!, conversationId!!)
    }

    override fun setUserProperty(
        property: com.qx.message.UserProperty,
        callback: IResultCallback
    ) {
        val builder = C2SUserProperty.UserProperty.newBuilder()
        if (!property.language.isNullOrEmpty()) {
            builder.language = property.language
        }
        val body = builder.build()
        postOperation(SystemCmd.C2S_USERPROPERTY, body, callback)
    }

    override fun rtcJoin(join: com.qx.message.rtc.RTCJoin, callback: IResultCallback) {
        val body = C2SVideoRtcJoin.VideoRtcJoin.newBuilder().setRoomId(join.roomId)
            .setRoomType(join.roomType)
            .build()
        postOperation(SystemCmd.C2S_RTC_SIGNAL_JOIN, body, callback)
    }

    override fun rtcJoined(joined: com.qx.message.rtc.RTCJoined?, callback: IResultCallback?) {
        //        val body = C2SVideoRtcJoined.VideoRtcJoined.newBuilder().setRoomId(joined.roomId)
//                .addAllPeers(joined.peers)
//                .build()
//        postOperation(SystemCmd.C2S_RTC_SIGNAL_JOIN, body, callback)
    }

    override fun rtcOffer(offer: com.qx.message.rtc.RTCOffer, callback: IResultCallback) {
        val body = C2SVideoRtcOffer.VideoRtcOffer.newBuilder()
            .setRoomId(offer.roomId)
            .setFrom(offer.from)
            .setTo(offer.to)
            .setSdp(offer.sdp)
            .build()
        postOperation(SystemCmd.C2S_RTC_SIGNAL_OFFER, body, callback)
    }

    override fun rtcAnswer(offer: com.qx.message.rtc.RTCOffer, callback: IResultCallback) {
        val body = C2SVideoRtcOffer.VideoRtcOffer.newBuilder()
            .setRoomId(offer.roomId)
            .setFrom(offer.from)
            .setTo(offer.to)
            .setSdp(offer.sdp)
            .build()
        postOperation(SystemCmd.C2S_RTC_SIGNAL_ANSWER, body, callback)
    }

    override fun rtcCandidate(
        candidate: com.qx.message.rtc.RTCCandidate,
        callback: IResultCallback
    ) {
        val sdp = C2SVideoRtcCandidate.Candidate.newBuilder()
            .setSdp(candidate.candidate!!.sdp)
            .setSdpMid(candidate.candidate!!.sdpMid)
            .setSdpMLineIndex(candidate.candidate!!.sdpMLineIndex)
            .build()
        val builder = C2SVideoRtcCandidate.VideoRtcCandidate.newBuilder()
            .setRoomId(candidate.roomId)
            .setFrom(candidate.from)
            .setTo(candidate.to)
        builder.candidate = sdp
        val body = builder.build()
        postOperation(SystemCmd.C2S_RTC_SIGNAL_CANDIDATE, body, callback)
    }

    override fun setRTCSignalMessageListener(lisntener: IRTCMessageListener?) {
        mRTCReceiveMessageListener = lisntener
    }

    override fun getRTCConfig(): com.qx.im.model.RTCServerConfig {
        return rtcConfig!!
    }

    override fun updateAtMessageReadState(
        messageId: String?,
        conversationId: String?,
        read: Int
    ): Int {
     return IMDatabaseRepository.instance.updateAtMessageReadState(messageId!!,conversationId!!,read)
    }

    override fun clearAtMessage(conversationId: String?): Int {
       return IMDatabaseRepository.instance.clearAtMessage(conversationId!!)
    }

    override fun updateCustomMessage(
        conversationId: String?,
        messageId: String?,
        content: String?,
        extra: String?
    ): Int {
     return IMDatabaseRepository.instance.updateCustomMessage(conversationId!!,messageId!!,content!!,extra!!)
    }

    override fun rtcVideoParam(
        data: com.qx.message.rtc.RTCVideoParam?,
        callback: IResultCallback
    ) {
        val body = C2SVideoParam.VideoParam.newBuilder()
            .setRoomId(data?.roomId)
            .setUserId(data?.userId)
            .setParam(data?.param)
            .build()
        postOperation(SystemCmd.C2S_VIDEO_PARAM, body, callback)
    }

    override fun openDebugLog() {
        QLog.openDebug()
    }
}