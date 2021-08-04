package com.qx.imlib.utils

import com.qx.message.*

class MessageCreator {

    /**
     * 发送文本消息
     */
    fun createTextMessage(conversationType: String, senderId: String, targetId: String, content: String, extra: String, atTos: List<String>): Message {

        var textMessage = TextMessage.obtain(content)
        textMessage.extra = extra
        var message = Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_TEXT, textMessage)
        var atToMessage = arrayListOf<AtToMessage>()
        for (at in atTos) {
            val at = AtToMessage(at, AtToMessage.ReadState.STATE_UN_READ)
            atToMessage.add(at)
        }
        textMessage.atToMessageList = atToMessage
        return message
    }

    /**
     * 发送图片消息
     */
    fun createImageMessage(conversationType: String, senderId: String, targetId: String, localPath: String, originUrl: String,
                           breviaryUrl: String, width: Int, height: Int, size: Long, extra: String): Message {

        var imageMessage = ImageMessage(localPath, breviaryUrl, size, originUrl, width, height)
        imageMessage.extra = extra
        return Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_IMAGE, imageMessage)
    }

    /**
     * 发送音频消息
     */
    fun createAudioMessage(conversationType: String, senderId: String, targetId: String, localPath: String, originUrl: String, duration: Int,
                           size: Long, extra: String): Message {

        var audioMessage = AudioMessage(localPath, duration, size, originUrl)
        audioMessage.extra = extra
        return Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_AUDIO, audioMessage)
    }

    /**
     * 发送视频消息
     */
    fun createVideoMessage(conversationType: String,
                           senderId: String, targetId: String,
                           localPath: String,
                           headUrl: String,
                           originUrl: String,
                           width: Int,
                           height: Int,
                           size: Long,
                           duration: Int,
                           extra: String): Message {
        var videoMessage = VideoMessage(
            localPath, duration, size, headUrl, originUrl, width, height
        )
        videoMessage.extra = extra
        return Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_VIDEO, videoMessage)
    }

    /**
     * 发送文件消息
     */
    fun createFileMessage(conversationType: String,
                          senderId: String, targetId: String,
                          localPath: String,
                          fileName: String,
                          originUrl: String,
                          type: String,
                          size: Long,
                          extra: String): Message {
        var fileMessage = FileMessage(localPath, size, fileName, originUrl, type)
        fileMessage.extra = extra
        return Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_FILE, fileMessage)
    }

    /**
     * 发送位置消息
     */
    fun createGeoMessage(conversationType: String,
                         senderId: String, targetId: String,
                         title: String,
                         address: String,
                         previewUrl: String,
                         localPath: String,
                         lon: Float,
                         lat: Float,
                         extra: String): Message {
        var geoMessage = GeoMessage(title, address, previewUrl,localPath, lon, lat)
        geoMessage.extra = extra
        return Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_GEO, geoMessage)
    }

    /**
     * 发送图文消息
     */
    fun createImageTextMessage(conversationType: String,
                               senderId: String, targetId: String,
                               title: String,
                               content: String,
                               imageUrl: String,
                               redirectUrl: String,
                               tag: String,
                               extra: String): Message {
        var imageTextMessage = ImageTextMessage(title, content, imageUrl, redirectUrl, tag)
        imageTextMessage.extra = extra
        return Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_IMAGE_AND_TEXT, imageTextMessage)

    }


    /**
     * 发送输入状态消息
     */
    fun createInputStatusMessage(conversationType: String, senderId: String, targetId: String, content: String, extra: String): Message {
        var inputStatusMessage = InputStatusMessage(content)
        inputStatusMessage.extra = extra
        return Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_STATUS, inputStatusMessage)
    }

    /**
     * 发送通知消息
     */
    fun createNoticeMessage(conversationType: String, senderId: String, targetId: String,operateUser : String, users: String, type : Int, content: String, extra: String): Message {
        var noticeMessage = NoticeMessage(content, operateUser, users, type)
        noticeMessage.extra = extra
        return Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_NOTICE, noticeMessage)
    }

    fun createReplyMessage(conversationType: String, senderId: String, targetId: String, origin: Message, answer: Message, extra: String): Message {
        var replyMessage = ReplyMessage(origin, answer, extra)
        replyMessage.extra =extra
        return Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_REPLY, replyMessage)
    }

    fun createRetransmissionMessage(conversationType: String, senderId: String, targetId: String, messages: List<Message>, extra: String): Message {
        var retransmissionMessage = RecordMessage(messages, extra)
        retransmissionMessage.extra = extra
        return Message.obtain(senderId, targetId, conversationType, MessageType.TYPE_RECORD, retransmissionMessage)
    }

    fun createCustomMessage(conversationType: String, senderId: String, messageType: String,targetId: String, content: String, extra: String): Message {
        var customMessage = CustomMessage(content)
        customMessage.extra = extra
        return Message.obtain(senderId, targetId, conversationType, messageType, customMessage)
    }

    companion object {
        val instance = Holder.emitter
    }

    object Holder {
        val emitter = MessageCreator()
    }
}