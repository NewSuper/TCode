package com.qx.imlib.db

import android.util.Log
import com.qx.im.model.ConversationType
import com.qx.im.model.InsertMessageResult
import com.qx.im.model.SearchConversationResult
import com.qx.im.model.UserInfoCache
import com.qx.imlib.ConversationUtil
import com.qx.imlib.SystemCmd
import com.qx.imlib.db.dao.*
import com.qx.imlib.db.entity.*
import com.qx.imlib.job.JobManagerUtil
import com.qx.imlib.netty.S2CSndMessage
import com.qx.imlib.qlog.QLog
import com.qx.imlib.utils.ContextUtils
import com.qx.imlib.utils.TargetIdUtil
import com.qx.it.protos.C2SMessageLoad
import com.qx.message.MessageType

class IMDatabaseRepository {
    private val TAG = "IMDatabaseRepository"

    private var userDao: UserDao?
    private var messageDao: MessageDao?
    private var conversationDao: ConversationDao?
    private var textMessageDao: TextMessageDao?
    private var atToMessageDao: AtToMessageDao?
    private var noticeMessageDao: NoticeMessageDao?

    private var audioMessageDao: AudioMessageDao?
    private var fileMessageDao: FileMessageDao?
    private var geoMessageDao: GeoMessageDao?
    private var imageMessageDao: ImageMessageDao?
    private var imageTextMessageDao: ImageTextMessageDao?
    private var videoMessageDao: VideoMessageDao?
    private var replyMessageDao: ReplyMessageDao?
    private var retransmisstionMessageDao: RetransmissionMessageDao?
    private var customMessageDao: CustomMessageDao?
    private var callMessageDao: CallMessageDao
    private var unTrustTimeDao: UnTrustTimeDao

    init {
        var db = NewIMDatabase.getDatabase(ContextUtils.getInstance().context)
        userDao = db!!.userDao()
        messageDao = db.messageDao()
        conversationDao = db.conversationDao()
        textMessageDao = db.textMessageDao()
        atToMessageDao = db.atToMessageDao()
        noticeMessageDao = db.noticeMessageDao()
        audioMessageDao = db.audioMessageDao()
        fileMessageDao = db.fileMessageDao()
        geoMessageDao = db.geoMessageDao()
        imageMessageDao = db.imageMessageDao()
        imageTextMessageDao = db.imageTextMessageDao()
        videoMessageDao = db.videoMessageDao()
        customMessageDao = db.customMessageDao()
        replyMessageDao = db.replyMessageDao()
        retransmisstionMessageDao = db.retransmissionMessageDao()
        callMessageDao = db.callMessageDao()!!
        unTrustTimeDao = db.unTrustTimeDao()!!
    }

    companion object {
        val instance = Holder.holder

        /**
         * 获取比timestamp值还小的数据
         */
        val SEARCH_TYPE_BEFORE = 0

        /**
         * 获取比timestamp值还大的数据
         */
        val SEARCH_TYPE_AFTER = 0
    }

    private object Holder {
        val holder = IMDatabaseRepository()
    }

    fun insertUser(userEntity: UserEntity): Long {
        return userDao!!.insertUser(userEntity)
    }

    fun calcUnTrustTime(conversationType: String, targetId: String, min: Long, max: Long) {
        Log.e(TAG, "calcUnTrustTime: ============ 历史消息后端查询成功，开始计算不信任时间窗:$min    $max")
        unTrustTimeDao.startsWithAndUpdate(conversationType, targetId, UserInfoCache.getUserId(), min, max)
        unTrustTimeDao.endsWithAndUpdate(conversationType, targetId, UserInfoCache.getUserId(), min, max)
        var list = unTrustTimeDao.containsWithAndUpdate(conversationType, targetId, UserInfoCache.getUserId(), min, max)//获取当前这个时间窗包含的消息窗列表
        Log.e(TAG, "calcUnTrustTime:  包含在数据库的时间窗列表：$list")
        //如果list不为空，则说明不信任区域时间区间包含min和max，则需要拆分该时间区域一分为二，然后再删除该时间区域
        if (!list.isNullOrEmpty()) {
            for (time in list) {
                Log.e(TAG, "calcUnTrustTime: 拿到的单条时间窗：$time")
                var time1 = TBUnTrustTime.obtain(time.ownerId, time.conversationType, time.targetId, time.startTime, min)
                var time2 = TBUnTrustTime.obtain(time.ownerId, time.conversationType, time.targetId, max, time.endTime)
                unTrustTimeDao.insert(time1)
                unTrustTimeDao.insert(time2)
                unTrustTimeDao.delete(time.id)
                QLog.e(TAG, "calcUnTrustTime: insert target:${targetId}, time1 start: ${time1.startTime} end:${time1.endTime}," +
                        "time2 start:${time2.startTime} end: ${time2.endTime}, min:$min,max:$max, and delete: ${time.startTime}")
            }
        }
        unTrustTimeDao.beContain(conversationType, targetId, UserInfoCache.getUserId(), min, max)
    }

    fun calcUnTrustTime2(conversationType: String, targetId: String, min: Long, max: Long) {
        //获取当前会话存在的所有时间窗集合
        var currentConversationUnTrustTimes = unTrustTimeDao.getConversationAllUnTrustTimes(conversationType, targetId, UserInfoCache.getUserId()) as List<TBUnTrustTime>

        if (currentConversationUnTrustTimes.size < 2) {
            return
        }

        //合并时间窗
        for (i in 0 until currentConversationUnTrustTimes.size) {
            unTrustTimeDao.delete(currentConversationUnTrustTimes[i].id)//删除当前会话对应的时间窗
        }

        var result = arrayListOf<TBUnTrustTime>()
        var first = currentConversationUnTrustTimes[0]
        for (i in 1 until currentConversationUnTrustTimes.size) {
            val next: TBUnTrustTime = currentConversationUnTrustTimes[i]
            // 合并区间，时间精确到秒（差1秒则为连续）
            if (next.startTime <= first.endTime) {
//                var timeTemp = TBUnTrustTime.obtain(first.ownerId, first.conversationType, first.targetId, first.startTime, (first.endTime).coerceAtLeast(next.endTime))
                first.startTime = first.startTime
                first.endTime = (first.endTime).coerceAtLeast(next.endTime)
            } else {
                // 没有交集，直接添加
                result.add(first)
                first = next
            }
        }
        result.add(first)

        //将计算合并好的时间窗，插入到数据库
        for (i in 0 until result.size) {
            unTrustTimeDao.insert(result[i])
        }
    }

    fun insertUntrustTime(unTrustTime: TBUnTrustTime) {
        unTrustTimeDao.insert(unTrustTime)
    }

    fun insertMessage(messageEntity: MessageEntity): InsertMessageResult {
        Log.e(
            TAG,
            "insertMessage: 传过来的消息:" + messageEntity.messageType + " 时间为：" + messageEntity.timestamp
        )
        var result = InsertMessageResult()
        if (messageEntity == null || messageEntity.messageType == MessageType.TYPE_STATUS) {
            return result
        }
        if (messageEntity.from == UserInfoCache.getUserId()) {
            messageEntity.direction = MessageEntity.Direction.DIRECTION_SEND
        } else {
            messageEntity.direction = MessageEntity.Direction.DIRECTION_RECEIVED
        }
        //插入新消息
        var row = messageDao!!.insertMessage(messageEntity)
        if (row > 0) {
            result.newMessageCount++
            //插入文本消息
            if (messageEntity.textMessage != null) {
                // 已经查过了一次---过滤敏感词
//                var text = messageEntity.textMessage?.content
//                var result = SensitiveWordsUtils.checkSensitiveWord(text, GlobalContextManager.getInstance().context)
//                if (result != null) {
//                    if (!result.isBan) {
//                        text = result.text
//                        messageEntity.textMessage?.content = text
//                    }
//                }
                messageEntity!!.textMessage!!.conversationId = messageEntity.conversationId
                insertTextMessage(messageEntity.textMessage!!)
            }
            //插@某人消息
            if (messageEntity.atToMessage != null) {
                if (messageEntity!!.atToMessage!!.isNotEmpty()) {
                    for (element in messageEntity.atToMessage!!) {
                        element.conversationId = messageEntity.conversationId
                    }
                    insertAtToMessage(messageEntity.atToMessage!!.toTypedArray())
                }
            }
            //通知消息
            //插入通知消息
            if (messageEntity.noticeMessage != null) {
                messageEntity!!.noticeMessage!!.conversationId = messageEntity.conversationId
                insertNoticeMessage(messageEntity!!.noticeMessage!!)
            }
            //插入图片消息
            if (messageEntity.imageMessage != null) {
                messageEntity!!.imageMessage!!.conversationId = messageEntity.conversationId
                insertImageMessage(messageEntity!!.imageMessage!!)
            }

            //插入文件消息
            if (messageEntity.fileMessage != null) {
                messageEntity!!.fileMessage!!.conversationId = messageEntity.conversationId
                insertFileMessage(messageEntity!!.fileMessage!!)
            }
            //插入图文消息
            if (messageEntity.imageTextMessage != null) {
                messageEntity!!.imageTextMessage!!.conversationId = messageEntity.conversationId
                insertImageTextMessage(messageEntity!!.imageTextMessage!!)
            }
            //插入地理位置消息
            if (messageEntity.geoMessage != null) {
                messageEntity!!.geoMessage!!.conversationId = messageEntity.conversationId
                insertGeoMessage(messageEntity!!.geoMessage!!)
            }
            //插入音频消息
            if (messageEntity.audioMessage != null) {
                messageEntity!!.audioMessage!!.conversationId = messageEntity.conversationId
                insertAudioMessage(messageEntity!!.audioMessage!!)
            }
            //插入视频消息
            if (messageEntity.videoMessage != null) {
                messageEntity!!.videoMessage!!.conversationId = messageEntity.conversationId
                insertVideoMessage(messageEntity!!.videoMessage!!)
            }
            //插入回复消息
            if (messageEntity.replyMessage != null) {
                messageEntity!!.replyMessage!!.conversationId = messageEntity.conversationId
                insertReplyMessage(messageEntity!!.replyMessage!!)
            }

            //插入转发消息
            if (messageEntity.retransmissionMessage != null) {
                messageEntity!!.retransmissionMessage!!.conversationId =
                    messageEntity.conversationId
                insertRetransmisstionMessage(messageEntity!!.retransmissionMessage!!)
            }
            //插入自定义消息
            if (messageEntity.customMessage != null) {
                messageEntity!!.customMessage!!.conversationId = messageEntity.conversationId
                insertCustomMessage(messageEntity!!.customMessage!!)
            }
            //音视频呼叫消息
            messageEntity.callMessage?.apply {
                conversationId = messageEntity.conversationId
                insertOrReplace(this)
            }
            result.messages.add(messageEntity)
        }
        return result
    }

    fun markMessageDelete(messageIds: Array<String>): Int {
        var result =
            messageDao!!.markMessageDelete(messageIds.toList(), 1, UserInfoCache.getUserId())
        return result
    }

    private fun deleteMessageContentByTimestamp(timestamp: Long, conversationId: String) {
        atToMessageDao!!.deleteByTimestamp(timestamp, conversationId)
        audioMessageDao!!.deleteByTimestamp(timestamp, conversationId)
        customMessageDao!!.deleteByTimestamp(timestamp, conversationId)
        fileMessageDao!!.deleteByTimestamp(timestamp, conversationId)
        geoMessageDao!!.deleteByTimestamp(timestamp, conversationId)
        imageMessageDao!!.deleteByTimestamp(timestamp, conversationId)
        imageTextMessageDao!!.deleteByTimestamp(timestamp, conversationId)
        noticeMessageDao!!.deleteByTimestamp(timestamp, conversationId)
        textMessageDao!!.deleteByTimestamp(timestamp, conversationId)
        videoMessageDao!!.deleteByTimestamp(timestamp, conversationId)
        callMessageDao.deleteByTimestamp(timestamp, conversationId)
//        conversationDao!!.updateLastMessage(conversationId, "")
//        conversationDao!!.updateTimeIndicator(conversationId, -1, -1,UserInfoCache.getUserId())
    }

    private fun deleteMessageContentByConversationId(conversationId: String) {
        atToMessageDao!!.deleteAll(conversationId)
        audioMessageDao!!.deleteAll(conversationId)
        customMessageDao!!.deleteAll(conversationId)
        fileMessageDao!!.deleteAll(conversationId)
        geoMessageDao!!.deleteAll(conversationId)
        imageMessageDao!!.deleteAll(conversationId)
        imageTextMessageDao!!.deleteAll(conversationId)
        noticeMessageDao!!.deleteAll(conversationId)
        textMessageDao!!.deleteAll(conversationId)
        videoMessageDao!!.deleteAll(conversationId)
        conversationDao!!.updateTimeIndicator(conversationId, -1, -1, UserInfoCache.getUserId())
        callMessageDao.deleteAll(conversationId)
    }

    fun deleteLocalMessageByTimestamp(
        conversationType: String,
        targetId: String,
        timestamp: Long
    ): Int {
        var conversation = getConversation(conversationType, targetId)
        var latest = messageDao!!.getLatestMessaged(conversation!!.conversationId)
        deleteMessageContentByTimestamp(timestamp, conversation.conversationId)
        conversationDao!!.updateDeleteTime(latest.timestamp, conversation.conversationId)
        return messageDao!!.deleteMessageByTimestamp(
            UserInfoCache.getUserId(),
            timestamp,
            conversation.conversationId
        )
    }

    fun updateAtMessageReadState(messageId: String, conversationId: String, read: Int): Int {
        var row = atToMessageDao!!.updateReadState(messageId, conversationId, read)
        var list = atToMessageDao!!.getUnReadAtToMessage(conversationId, UserInfoCache.getUserId())
        if (list.isNullOrEmpty()) {
            //如果@消息已经全部被阅读，则清空会话atTo字段值
            var conversation = getConversationById(conversationId)
            if (conversation != null) {
                updateConversationAtTO(conversation.targetId, "")
            }
        }
        return row
    }

    fun clearAtMessage(conversationId: String): Int {
        var row = atToMessageDao!!.updateReadStateByConversationId(conversationId, 1)
        var conversation = getConversationById(conversationId)
        if (conversation != null) {
            updateConversationAtTO(conversation.targetId, "")
        }
        return row
    }

    fun getConversationById(conversationId: String): ConversationEntity? {
        return conversationDao!!.getConversationById(conversationId)
    }

    fun getConversation(conversationType: String, targetId: String): ConversationEntity? {
        return conversationDao!!.getConversationByTargetId(
            conversationType,
            targetId,
            UserInfoCache.getUserId())
//        return when (conversationType) {
//            ConversationType.TYPE_PRIVATE -> {
//                conversationDao!!.getP2PConversation(conversationType, targetId,UserInfoCache.getUserId())
//            }
//            ConversationType.TYPE_GROUP -> {
//                conversationDao!!.getGroupConversation(conversationType, targetId,UserInfoCache.getUserId())
//            }
//            else -> {
//                //系统
//                conversationDao!!.getSystemConversation(conversationType,UserInfoCache.getUserId())
//            }
//        }
    }

    fun updateMessageState(messageId: String, state: Int, reason: String): Int {
        return messageDao!!.updateMessageState(messageId, state, reason)
    }

    fun updateOriginPath(messageEntity: MessageEntity): Int {
        when (messageEntity.messageType) {
            MessageType.TYPE_AUDIO -> {
                return audioMessageDao!!.updateOriginUrl(
                    messageEntity.messageId,
                    messageEntity.audioMessage!!.originUrl
                )
            }
            MessageType.TYPE_VIDEO -> {
                return videoMessageDao!!.updateOriginUrl(
                    messageEntity.messageId,
                    messageEntity.videoMessage!!.originUrl
                )
            }
            MessageType.TYPE_IMAGE -> {
                return imageMessageDao!!.updateOriginUrl(
                    messageEntity.messageId,
                    messageEntity.imageMessage!!.originUrl
                )
            }
            MessageType.TYPE_FILE -> {
                return fileMessageDao!!.updateOriginUrl(
                    messageEntity.messageId,
                    messageEntity.fileMessage!!.originUrl
                )
            }
            MessageType.TYPE_GEO -> {
                return geoMessageDao!!.updatePreviewUrl(
                    messageEntity.messageId,
                    messageEntity.geoMessage!!.previewUrl
                )
            }
            else -> {
                return 0
            }
        }
    }

    fun isMessageExist(messageId: String): Boolean {
        if (messageDao!!.isMessageExist(messageId, UserInfoCache.getUserId()) > 0) {
            return true
        }
        return false
    }

    fun updateVideoHeadUrl(messageId: String, headUrl: String): Int {
        return videoMessageDao!!.updateHeadUrl(messageId, headUrl)
    }

    fun updateLocalPath(messageEntity: MessageEntity): Int {
        when (messageEntity.messageType) {
            MessageType.TYPE_AUDIO -> {
                return audioMessageDao!!.updateLocalPath(
                    messageEntity.messageId, messageEntity.audioMessage!!.localPath
                )
            }
            MessageType.TYPE_VIDEO -> {
                return videoMessageDao!!.updateLocalPath(
                    messageEntity.messageId, messageEntity.videoMessage!!.localPath
                )
            }
            MessageType.TYPE_IMAGE -> {
                return imageMessageDao!!.updateLocalPath(
                    messageEntity.messageId, messageEntity.imageMessage!!.localPath
                )
            }
            MessageType.TYPE_FILE -> {
                return fileMessageDao!!.updateLocalPath(
                    messageEntity.messageId, messageEntity.fileMessage!!.localPath
                )
            }
            MessageType.TYPE_GEO -> {
                return geoMessageDao!!.updateLocalPath(
                    messageEntity.messageId, messageEntity.geoMessage!!.localPath
                )
            }
            else -> {
                return 0
            }
        }

    }

    fun updateCustomMessage(
        conversationId: String,
        messageId: String,
        content: String,
        extra: String
    ): Int {
        return customMessageDao!!.update(content, extra, messageId, conversationId)
    }

    fun updateAllMessageReadState(sendType: String, to: String): Int {
        return messageDao!!.updateAllMessageStateToRead(
            sendType,
            UserInfoCache.getUserId(),
            to,
            MessageEntity.State.STATE_READ,
            UserInfoCache.getUserId()
        )
    }

    fun updateMessageToRecallType(messageId: String): Int {
        return messageDao!!.updateMessageType(messageId, MessageType.TYPE_RECALL)
    }

    fun updateMessageStateByConversationId(conversationId: String, state: Int): Int {
        return messageDao!!.updateMessageStateByConversationId(
            conversationId,
            state,
            UserInfoCache.getUserId()
        )
    }

    fun updateDraft(draft: String, conversation_id: String): Int {
        return conversationDao!!.updateDraft(draft, conversation_id)
    }

    fun updateConversationTitle(type: String, targetId: String, title: String): Int {
        return conversationDao!!.updateConversationTitle(type, targetId, title)
    }

    fun updateConversationIcon(type: String, targetId: String, icon: String): Int {
        return conversationDao!!.updateConversationIcon(type, targetId, icon)
    }

    fun updateConversationTitleAndIcon(
        type: String,
        targetId: String,
        title: String,
        icon: String
    ): Int {
        return conversationDao!!.updateConversationTitleAndIcon(type, targetId, title, icon)
    }

    fun updateMessageStateAndTime(state: Int, timestamp: Long, messageId: String): Int {
        return messageDao!!.updateMessageStateAndTime(state, timestamp, messageId)
    }

    fun updateP2PTopTimeAndTimeIndicator(
        sendType: String,
        targetId: String,
        topTime: Long,
        timeIndicator: Long
    ): Int {
        return conversationDao!!.updateTimeIndicator(
            sendType,
            targetId,
            topTime,
            timeIndicator,
            UserInfoCache.getUserId()
        )
    }

    fun updateGroupTopTimeAndTimeIndicator(
        sendType: String,
        from: String,
        topTime: Long,
        timeIndicator: Long
    ): Int {
        return conversationDao!!.updateGroupTimeIndicator(
            sendType,
            from,
            UserInfoCache.getUserId(),
            topTime,
            timeIndicator
        )
    }

    fun updateTimeIndicator(conversationId: String, timeIndicator: Long): Int {
        return conversationDao!!.updateTimeIndicator(
            conversationId,
            timeIndicator,
            UserInfoCache.getUserId()
        )
    }

    fun getMessages(): List<MessageEntity>? {
        return messageDao!!.getMessage(UserInfoCache.getUserId())
    }

    fun getMessagesByConversationID(conversationId: String): List<MessageEntity>? {
        return messageDao!!.getMessagesByConversationID(conversationId)
    }

    fun getMessageById(messageId: String): MessageEntity? {
        var messageEntity = messageDao!!.getMessageById(messageId)
        return fillMessageContent(messageEntity)
    }

    fun getLatestMessage(
        sendType: String,
        from: String,
        to: String,
        ownerId: String
    ): MessageEntity? {
        if (sendType.isNullOrEmpty() || to.isNullOrEmpty() || ownerId.isNullOrEmpty()) {
            return null
        }
        val messageEntity = messageDao!!.getLatestMessage(sendType, from, to, ownerId)
        fillMessageContent(messageEntity)
        return messageEntity
    }

    fun getOldestMessage(conversationId: String, ownerId: String): MessageEntity? {
        if (conversationId.isNullOrEmpty() || ownerId.isNullOrEmpty()) {
            return null
        }
        var messageEntity = messageDao!!.getOldestMessage(conversationId, ownerId)
        fillMessageContent(messageEntity)
        return messageEntity
    }

    fun getLatestMessageByConversationId(conversationId: String, ownerId: String): MessageEntity? {
        if (conversationId.isNullOrEmpty() || ownerId.isNullOrEmpty()) {
            return null
        }
        var messageEntity = messageDao!!.getLatestMessaged(conversationId, ownerId)
        fillMessageContent(messageEntity)
        return messageEntity
    }

    fun checkSendingMessage(): List<MessageEntity> {
        var list = messageDao!!.getMessageByState(
            MessageEntity.State.STATE_SENDING,
            UserInfoCache.getUserId()
        )
        for (message in list) {
            fillMessageContent(message)
        }
        return list
    }

    fun getMessagesByTimestamp(
        conversationType: String,
        targetId: String,
        timestamp: Long,
        searchType: Int,
        pageSize: Int
    ): List<MessageEntity>? {
        var list = arrayListOf<MessageEntity>()

        var conversation = conversationDao!!.getConversationByTargetId(
            conversationType,
            targetId,
            UserInfoCache.getUserId()
        )
        var conversationId = ""
        if (conversation != null) {
            conversationId = conversation.conversationId
        }

        //从本地获取数据
        list = getLocalMessageByTimestamp(
            conversationId,
            timestamp,
            pageSize,
            searchType
        ) as ArrayList<MessageEntity>
        calcData(list, conversationId, conversationType, targetId, pageSize, searchType)
        return list
    }

    /**
     * 优先从本地数据库获取，如检测到当前不是最新数据，再从服务器获取
     */
    fun getMessages(
        conversationType: String,
        targetId: String,
        offset: Int,
        pageSize: Int
    ): List<MessageEntity>? {
        var list = arrayListOf<MessageEntity>()
        var localHasConversation = (getConversation(conversationType, targetId) != null)//当前原本是否有会话
        if (!localHasConversation) {
            var unTrustTimeTemp =
                TBUnTrustTime.obtain(UserInfoCache.getUserId(), conversationType, targetId, 0, 100)
        }

        var conversation = conversationDao!!.getConversationByTargetId(
            conversationType,
            targetId,
            UserInfoCache.getUserId()
        )
        var conversationId = ""
        if (conversation != null) {
            conversationId = conversation.conversationId
        }

        //从本地获取数据
        list = getLocalMessage(conversationId, offset, pageSize) as ArrayList<MessageEntity>
        calcData(list, conversationId, conversationType, targetId, pageSize, SEARCH_TYPE_BEFORE)
        return list
    }

    /**
     * 从本地数据库获取
     */
    fun getHistoryMessagesFromLocal(
        conversationId: String,
        conversationType: String,
        targetId: String,
        offset: Int,
        pageSize: Int
    ): List<MessageEntity>? {
        var list = arrayListOf<MessageEntity>()
        var cId = ""//会话ID

        if (conversationId.isEmpty()) {
            var conversation = conversationDao!!.getConversationByTargetId(
                conversationType,
                targetId,
                UserInfoCache.getUserId()
            )
            if (conversation != null) {
                cId = conversation.conversationId
            }

            //从本地获取数据
            list = getLocalMessage(cId, offset, pageSize) as ArrayList<MessageEntity>
            Log.e(TAG, "getHistoryMessagesFromLocal: >>>>>>>>>>>")
            return list
        } else {
            cId = conversationId
            Log.e(TAG, "getHistoryMessagesFromLocal: |||||||||||||||||||")
        }
        list = getLocalMessage(cId, offset, pageSize) as ArrayList<MessageEntity>//查询本地pageSize条消息数据
        calcData(list, conversationId, conversationType, targetId, pageSize, SEARCH_TYPE_BEFORE)
        return list
    }

    //计算数据连续性，结合多段时间窗
    private fun calcData(
        list: ArrayList<MessageEntity>,
        conversationId: String,
        conversationType: String,
        targetId: String,
        pageSize: Int,
        searchType: Int
    ) {
        Log.e(TAG, "calcData: 本地数据库查询到的消息列表条数为：" + list.size)
        if (!list.isNullOrEmpty()) {
            var min = list.last().timestamp
            var max = list.first().timestamp
            //如果满1页
            if (list.size == pageSize) {
                //判断该页的数据是否跨不信任区域
                var startTime = unTrustTimeDao.startsWith(
                    conversationType,
                    targetId,
                    UserInfoCache.getUserId(),
                    min,
                    max
                )
                var endTime = unTrustTimeDao.endsWith(
                    conversationType,
                    targetId,
                    UserInfoCache.getUserId(),
                    min,
                    max
                )
                var centresTime = unTrustTimeDao.centresWith(
                    conversationType,
                    targetId,
                    UserInfoCache.getUserId(),
                    min,
                    max
                )
                var outSidesTime = unTrustTimeDao.outSidesWith(
                    conversationType,
                    targetId,
                    UserInfoCache.getUserId(),
                    min,
                    max
                )
                Log.e(TAG, "calcData: =========== 查询到的不信任起始时间：$startTime  结束时间：$endTime")

                when {
                    startTime != null -> {
                        QLog.d(TAG, "本地数据数量满一页（${list.size} 条）起始时间有交集,不返回数据，并查询历史消息")
                        getHistoryMessage(conversationType, targetId, startTime.endTime, searchType)
                        list.clear()
                        return
                    }
                    endTime != null -> {
                        QLog.d(TAG, "本地数据数量满一页（${list.size} 条）终止时间有交集,不返回数据，并查询历史消息")
                        //如果终止时间有交集
                        getHistoryMessage(conversationType, targetId, endTime.startTime, searchType)
                        list.clear()
                        return
                    }
                    centresTime != null -> {
                        QLog.d(TAG, "本地数据数量满一页（${list.size} 条）中间时间有交集,不返回数据，并查询历史消息")
                        //如果终止时间有交集
                        getHistoryMessage(
                            conversationType,
                            targetId,
                            centresTime.startTime,
                            searchType
                        )
                        list.clear()
                        return
                    }
                    outSidesTime != null -> {
                        QLog.d(TAG, "本地数据数量满一页（${list.size} 条）外部时间有交集,不返回数据，并查询历史消息")
                        //如果终止时间有交集
                        getHistoryMessage(
                            conversationType,
                            targetId,
                            outSidesTime.startTime,
                            searchType
                        )
                        list.clear()
                        return
                    }
                    else -> {
                        //无交集，可以返回
                        QLog.d(TAG, "本地数据数量满一页（${list.size}条）且连贯,返回数据")
                        return
                    }
                }

            } else {
                //不满一页
                QLog.d(TAG, "本地数据数量不满一页（${list.size}条），返回数据，不查询历史消息")
//                getHistoryMessage(conversationType, targetId, min)
                return
            }
        } else {
//本地数据为空，则尝试获取本地最旧的一条消息
            var oldestMessage = getOldestMessage(conversationId, UserInfoCache.getUserId())
            var timestamp = -1L
            if (oldestMessage != null) {
                //说明数据为空是因为本地的数据已完全被加载出来，这时需要从网络上加载
                QLog.d(TAG, "本地数据为空，原因：本地的数据已完全被加载出来")
                timestamp = oldestMessage.timestamp
            } else {
                //说明本地数据为空
                QLog.d(TAG, "本地数据为空，原因：本地的数据为空")
            }
            getHistoryMessage(conversationType, targetId, timestamp, searchType)
        }
    }

    fun getHistoryMessage(
        conversationType: String, targetId: String,
        timestamp: Long, searchType: Int) {
        QLog.d(TAG,
            "从服务器上获取消息记录： conversationType=$conversationType targetId=$targetId timestamp=$timestamp searchType=$searchType")
        var body = C2SMessageLoad.MessageLoad.newBuilder().setSendType(conversationType)
            .setSearchType(searchType).setTargetId(targetId).setTimestamp(timestamp).build()
        var msg = S2CSndMessage()
        msg.cmd = SystemCmd.C2S_MESSAGE_LOAD_PAGED
        msg.body = body
        JobManagerUtil.instance.postMessage(msg)
    }


    fun addConversationUnReadCount(sendType: String, from: String, to: String, count: Int): Int {
        var targetId = TargetIdUtil.getTargetId(sendType, from, to)
        return conversationDao!!.addConversationUnReadCount(sendType, targetId, count, UserInfoCache.getUserId())
    }

    fun updateConversationUnReadCount(sendType: String, targetId: String, count: Int): Int {
        return conversationDao!!.updateConversationUnReadCount(sendType, targetId, count, UserInfoCache.getUserId())
    }

    /**
     * 刷新会话最新信息：时间、最后一条消息内容
     */
    fun refreshConversationInfo(sendType: String, from: String, to: String): Int {
        //刷新时间
        var message: MessageEntity? = null
        message = messageDao!!.getLatestMessage(sendType, from, to, UserInfoCache.getUserId())
        if (message == null) {
            return 0
        }
        message = fillMessageContent(message)
        if (message != null) {
            return conversationDao!!.refreshConversationInfo(sendType, to, message.timestamp, UserInfoCache.getUserId())
        }
        return 0
    }

    fun fillMessageContent(entity: MessageEntity?): MessageEntity? {
        when (entity?.messageType) {
            MessageType.TYPE_TEXT -> {
                var textMessage = getTextMessageByMessageId(entity.messageId)
                var atToMessage = getAtToMessageByMessageId(entity.messageId)
                entity.textMessage = textMessage
                entity.atToMessage = atToMessage
            }
            MessageType.TYPE_AUDIO -> {
                var content = getAudioMessageByMessageId(entity.messageId)
                entity.audioMessage = content
            }
            MessageType.TYPE_VIDEO -> {
                var content = getVideoMessageByMessageId(entity.messageId)
                entity.videoMessage = content
            }
            MessageType.TYPE_IMAGE -> {
                var content = getImageMessageByMessageId(entity.messageId)
                entity.imageMessage = content
            }
            MessageType.TYPE_IMAGE_AND_TEXT -> {
                var content = getImageTextMessageByMessageId(entity.messageId)
                entity.imageTextMessage = content
            }
            MessageType.TYPE_GEO -> {
                var content = getGeoMessageByMessageId(entity.messageId)
                entity.geoMessage = content
            }
            MessageType.TYPE_FILE -> {
                var content = getFileMessageByMessageId(entity.messageId)
                entity.fileMessage = content
            }
            MessageType.TYPE_NOTICE -> {
                var noticeMessage = getNoticeMessageByMessageId(entity.messageId)
                entity.noticeMessage = noticeMessage
            }
            MessageType.TYPE_RECALL -> {
                //TODO 待处理原有数据体
            }
            MessageType.TYPE_AUDIO_CALL, MessageType.TYPE_VIDEO_CALL -> {
                val callMessage = queryCallMessageByMsgId(entity.messageId)
                entity.callMessage = callMessage
            }
            MessageType.TYPE_REPLY -> {
                var replyMessage = getReplyMessageByMessageId(entity.messageId)
                entity.replyMessage = replyMessage
            }
            MessageType.TYPE_RECORD -> {
                var retransmissionMessage = getRetransimisstionMessageByMessageId(entity.messageId)
                if (retransmissionMessage != null) {
                    entity.retransmissionMessage = retransmissionMessage
                }
            }
            else -> {
                var customMessage = getCustomMessageById(entity?.messageId)
                entity?.customMessage = customMessage
            }
        }
        return entity
    }


    /**
     * 刷新会话最新信息：时间、最后一条消息内容
     */
    fun refreshConversationInfo(message: MessageEntity) {
        if (message != null) {

            conversationDao!!.refreshConversationInfo(
                message.sendType, message.to, message.timestamp, UserInfoCache.getUserId()
            )
        }
    }

    /**
     * 更新会话聊天背景
     */
    fun updateConversationBackground(url: String, conversationId: String): Int {
        return conversationDao!!.updateBackground(url, conversationId)
    }

    /**
     * 更新群组会话@内容
     */
    fun updateConversationAtTO(to: String, atTo: String) {
        conversationDao!!.updateConversationAtTO(ConversationType.TYPE_GROUP, to, atTo, UserInfoCache.getUserId())
    }

    private fun getLocalMessage(conversationId: String, offset: Int, pageSize: Int): List<MessageEntity>? {
        QLog.d(TAG, "查询本地数据")
        var list =
            messageDao!!.getMessageByConversationId(conversationId!!, offset, pageSize) as ArrayList<MessageEntity>
        //此处需要根据消息类型，再查一次消息内容
        for (entity in list) {
            fillMessageContent(entity)
        }
        Log.e(TAG, "getLocalMessage: 查询本地消息总共多少条：" + list.size)
        return list
    }

    private fun getLocalMessageByTimestamp(conversationId: String, timestamp: Long, pageSize: Int, searchType: Int): List<MessageEntity>? {
        var list = arrayListOf<MessageEntity>()
        list = if (searchType == 0) {
            //查以前
            messageDao!!.getMessagesBefore(conversationId!!, timestamp, pageSize) as ArrayList<MessageEntity>
        } else {
            //查以后
            messageDao!!.getMessagesAfter(conversationId!!, timestamp, pageSize) as ArrayList<MessageEntity>
        }
        //此处需要根据消息类型，再查一次消息内容
        for (entity in list) {
            fillMessageContent(entity)
        }
        return list
    }

    fun getMessagesByType(conversationId: String, types: List<String>, offset: Int, pageSize: Int, isAll: Boolean, isDesc: Boolean): List<MessageEntity>? {
        QLog.d(TAG, "查询本地数据")
        var list = arrayListOf<MessageEntity>()
        list = if (isDesc) {
            if (isAll) {
                messageDao!!.getAllMessageByTypeDesc(conversationId!!, types) as ArrayList<MessageEntity>
            } else {
                messageDao!!.getMessageByTypeDesc(conversationId!!, types, offset, pageSize) as ArrayList<MessageEntity>
            }
        } else {
            if (isAll) {
                messageDao!!.getAllMessageByTypeAsc(conversationId!!, types) as ArrayList<MessageEntity>
            } else {
                messageDao!!.getMessageByTypeAsc(conversationId!!, types, offset, pageSize) as ArrayList<MessageEntity>
            }
        }
        //此处需要根据消息类型，再查一次消息内容
        for (entity in list) {
            fillMessageContent(entity)
        }
        return list
    }


    fun getMessageInRegion(messageId: List<String>): List<MessageEntity>? {
        return messageDao!!.getMessageInIdRegion(messageId)
    }


    fun insertConversation(conversationEntity: ConversationEntity): Long {
        return conversationDao!!.insertConversation(conversationEntity)
    }


    fun deleteAllConversation(): Int {
        messageDao!!.deleteAllMessage(UserInfoCache.getUserId())
        return conversationDao!!.deleteAllConversation(UserInfoCache.getUserId())
    }

    fun deleteConversation(conversationId: String): Int {
        //删除会话的同时，清空该会话下的消息
        deleteMessageByConversationId(conversationId)
        return conversationDao!!.markConversationDelete(1, conversationId)
    }

    fun deleteConversation(type: String, targetId: String): Int {
        var conversation = conversationDao!!.getConversationByTargetId(type, targetId, UserInfoCache.getUserId())
        if (conversation != null) {
            //删除会话的同时，清空该会话下的消息
            deleteMessageByConversationId(conversation.conversationId)
            return conversationDao!!.markConversationDelete(1, type, targetId, UserInfoCache.getUserId())
        }
        return 0
    }

    fun deleteMessageByConversationId(conversationId: String): Int {
        //需要设置会话最近消息为空
        var latest = messageDao!!.getLatestMessaged(conversationId)
        var row = messageDao!!.deleteMessageByConversationId(conversationId)
        if (row > 0 && latest != null && latest.timestamp != null) {
            deleteMessageContentByConversationId(conversationId)
            conversationDao!!.updateDeleteTime(latest.timestamp, conversationId)
        }
        return row
    }

    fun getAllConversation(): List<ConversationEntity>? {
        return conversationDao!!.getAllConversation(UserInfoCache.getUserId())
    }

    fun getConversationInRegion(region: List<String>): List<ConversationEntity>? {
        return conversationDao!!.getConversationInRegion(UserInfoCache.getUserId(), region)
    }

    fun updateConversationTop(is_top: Int, conversation_id: String): Int {
        Log.e(TAG, "updateConversationTop: 更新会话数据库")
        return conversationDao!!.updateP2PTop(is_top, conversation_id)
    }

    fun updateConversationTop(sendType: String, targetId: String, top: Int): Int {
        return if (sendType == ConversationType.TYPE_PRIVATE) {
            conversationDao!!.updateP2PTop(sendType, targetId, top, UserInfoCache.getUserId())
        } else {
            conversationDao!!.updateGroupTop(sendType, targetId, top, UserInfoCache.getUserId())
        }
    }

    fun updateConversationNoDisturbing(no_disturbing: Int, conversation_id: String): Int {
        return conversationDao!!.updateNoDisturbing(no_disturbing, conversation_id)
    }

    fun updateConversationNoDisturbing(sendType: String, targetId: String, isDisturbing: Int): Int {

        return if (sendType == ConversationType.TYPE_PRIVATE) {
            return conversationDao!!.updateP2PNoDisturbing(sendType, targetId, isDisturbing, UserInfoCache.getUserId())
        } else {
            conversationDao!!.updateGroupNoDisturbing(sendType, targetId, isDisturbing, UserInfoCache.getUserId())
        }
    }

    fun searchTextMessageByContent(content: String, conversationId: String?): List<MessageEntity> {
        var data = arrayListOf<MessageEntity>()
        //内容为空时不处理
        if (content.isNullOrEmpty()) {
            return data
        }
        var text = listOf<TBTextMessage>()
        //1.先搜索text表
        text = if (conversationId.isNullOrEmpty()) {
            textMessageDao!!.searchMessageByText(UserInfoCache.getUserId(), "%$content%")
        } else {
            textMessageDao!!.searchMessageByText(UserInfoCache.getUserId(), conversationId, "%$content%")
        }
        //2.再搜索message表
        for (t in text) {
            var message = messageDao!!.getMessageById(t.messageId)
            message.textMessage = t//设置文本消息内容
            message.atToMessage = atToMessageDao!!.getAtToMessageByMessageId(t.messageId)//设置@某人内容
            data.add(message)
        }
        return data
    }

    fun searchConversations(keyword: String, conversationTypes: Array<String>, messageTypes: Array<String>): List<SearchConversationResult> {
        var results = arrayListOf<SearchConversationResult>()
        //1.根据关键字和会话类型匹配会话
        var conversations = conversationDao!!.searchConversations("%$keyword%", conversationTypes, UserInfoCache.getUserId())

        for (conversation in conversations) {
            var searchConversationResult = SearchConversationResult(ConversationUtil.toConversation(conversation), 0)
            results.add(searchConversationResult)
        }
        //2.根据关键字和消息类型匹配文本内容
        for (type in messageTypes) {
            when (type) {
                MessageType.TYPE_TEXT -> {
                    var matchCounts = textMessageDao?.searchMatchTextCount("%$keyword%", UserInfoCache.getUserId())
                    if (matchCounts != null) {
                        for (count in matchCounts) {
                            var conversation = conversationDao?.getConversationById(count.conversationId)
                            var searchConversationResult = SearchConversationResult(ConversationUtil.toConversation(conversation), count.matchCount)
                            results.add(searchConversationResult)
                        }
                    }
                }
            }
        }
        return results
    }

    fun insertTextMessage(tbTextMessage: TBTextMessage): Long {
        return textMessageDao!!.insertTextMessage(tbTextMessage)
    }

    fun insertAtToMessage(atTo: Array<TBAtToMessage>): List<Long> {
        return atToMessageDao!!.insertAtToMessage(*atTo)
    }

    fun insertNoticeMessage(noticeMessage: TBNoticeMessage): Long {
        return noticeMessageDao!!.insertNoticeMessage(noticeMessage)
    }

    fun insertAudioMessage(message: TBAudioMessage): Long {
        return audioMessageDao!!.insertAudioMessage(message)
    }

    fun insertFileMessage(message: TBFileMessage): Long {
        return fileMessageDao!!.insertFileMessage(message)
    }

    fun insertGeoMessage(message: TBGeoMessage): Long {
        return geoMessageDao!!.insertGeoMessage(message)
    }

    fun insertImageMessage(message: TBImageMessage): Long {
        return imageMessageDao!!.insertImageMessage(message)
    }

    fun insertImageTextMessage(message: TBImageTextMessage): Long {
        return imageTextMessageDao!!.insertImageTextMessage(message)
    }

    fun insertVideoMessage(message: TBVideoMessage): Long {
        return videoMessageDao!!.insertVideoMessage(message)
    }

    fun insertReplyMessage(message: TBReplyMessage): Long {
        return replyMessageDao!!.insertMessage(message)
    }

    fun insertRetransmisstionMessage(message: TBRetransmissionMessage): Long {
        return retransmisstionMessageDao!!.insert(message)
    }

    fun insertCustomMessage(message: TBCustomMessage): Long {
        return customMessageDao!!.insertCustomMessage(message)
    }

    fun getNoticeMessageByMessageId(messageId: String): TBNoticeMessage? {
        if (noticeMessageDao != null) {
            return noticeMessageDao!!.getNoticeMessageByMessageId(messageId)
        }
        return null
    }

    fun getTextMessageByMessageId(messageId: String): TBTextMessage? {
        return textMessageDao!!.getTextMessageByMessageId(messageId)
    }

    fun getAtToMessageByMessageId(messageId: String): List<TBAtToMessage>? {
        return atToMessageDao!!.getAtToMessageByMessageId(messageId)
    }

    fun getUnReadAtToMessage(conversationId: String): List<MessageEntity>? {
        var messageList = arrayListOf<MessageEntity>()
        var atToMessages = atToMessageDao!!.getUnReadAtToMessage(conversationId, UserInfoCache.getUserId())
        if (atToMessages.isNullOrEmpty()) {
            return messageList
        }
        for (atTo in atToMessages) {
            var msg = getMessageById(atTo.messageId)
            if (msg != null) {
                messageList.add(msg)
            }
        }
        return messageList
    }

    fun getImageMessageByMessageId(messageId: String): TBImageMessage? {
        return imageMessageDao!!.getImageMessageByMessageId(messageId)
    }

    fun getImageTextMessageByMessageId(messageId: String): TBImageTextMessage? {
        return imageTextMessageDao!!.getImageTextMessageByMessageId(messageId)
    }

    fun getAudioMessageByMessageId(messageId: String): TBAudioMessage? {
        return audioMessageDao!!.getAudioMessageByMessageId(messageId)
    }

    fun getVideoMessageByMessageId(messageId: String): TBVideoMessage? {
        return videoMessageDao!!.getVideoMessageByMessageId(messageId)
    }

    fun getFileMessageByMessageId(messageId: String): TBFileMessage? {
        return fileMessageDao!!.getFileMessageByMessageId(messageId)
    }

    fun getGeoMessageByMessageId(messageId: String): TBGeoMessage? {
        return geoMessageDao!!.getGeoMessageByMessageId(messageId)
    }

    fun getReplyMessageByMessageId(messageId: String): TBReplyMessage? {
        return replyMessageDao!!.getByMessageId(messageId, UserInfoCache.getUserId())
    }

    fun getRetransimisstionMessageByMessageId(messageId: String): TBRetransmissionMessage? {
        return retransmisstionMessageDao!!.getByMessageId(messageId, UserInfoCache.getUserId())
    }

    fun getCustomMessageById(messageId: String?): TBCustomMessage? {
        return customMessageDao!!.getCustomMessageByMessageId(messageId)
    }

    fun getAllUnReadCount(region: List<String>): Int? {
        return conversationDao!!.getAllUnReadCount(UserInfoCache.getUserId(), region)
    }

    fun getConversationUnReadCount(conversationId: String, isIgnoreNoDisturbing: Boolean): Int? {
        return if (isIgnoreNoDisturbing) {
            conversationDao!!.getUnReadCountIgnoreDisturbing(conversationId, UserInfoCache.getUserId())
        } else {
            conversationDao!!.getUnReadCount(conversationId, UserInfoCache.getUserId())
        }
    }

    fun getFirstUnReadMessage(conversationId: String): MessageEntity? {
        var entity = messageDao!!.getFirstUnReadMessage(MessageEntity.State.STATE_RECEIVED, UserInfoCache.getUserId(), conversationId)
        if (entity != null) {
            fillMessageContent(entity)
            return entity
        }
        return null
    }
    fun insertUnTrustTime(unTrustTime: TBUnTrustTime) {
        unTrustTimeDao.insert(unTrustTime)
    }
    ///**** call_message START ****///
    fun insertOrReplace(message: TBCallMessage): Long {
        return callMessageDao.insertOrReplaceCallMessage(message)
    }

    fun queryCallMessageByMsgId(messageId: String): TBCallMessage {
        return callMessageDao.queryCallMsgFromMsgId(messageId)
    }

    fun queryCallMessageByConvId(conversationId: String): TBCallMessage {
        return callMessageDao.queryCallMsgFromConversationId(conversationId)
    }


}