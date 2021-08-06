package com.qx.im.model

import com.google.gson.Gson
import com.qx.im.model.json.JSonMessage
import com.qx.imlib.db.entity.*
import com.qx.imlib.qlog.QLog

import com.qx.imlib.utils.MessageConvertUtil
import com.qx.message.*
import java.lang.Exception

class MessageEntityToMessage {

    private val TAG = "MessageEntityToMessage"

    private fun convertMessage(messageEntity: MessageEntity, content: MessageContent?, extra: String): Message? {
        var message = Message.obtain(
            messageEntity.from,
            messageEntity.messageId,
            messageEntity.to,
            messageEntity.sendType,
            messageEntity.messageType,
            content,
            messageEntity.timestamp,
            extra
        )
        message.conversationId = messageEntity.conversationId
        message.senderUserId = messageEntity.from
        message.direction = messageEntity.direction
        message.state = messageEntity.state
        message.failedReason = messageEntity.failedReason
        return message
    }

    fun createImageMsg(messageEntity: MessageEntity): Message? {
        var image: TBImageMessage? = messageEntity.imageMessage
            ?: return null
        if (image != null) {
            var content = ImageMessage(
                image!!.localPath, image.breviaryUrl, image.size, image.originUrl, image.width, image.height
            )
            content.extra = messageEntity.imageMessage!!.extra
            return convertMessage(messageEntity, content, image.extra)
        }
        return null
    }

    fun createTextMsg(messageEntity: MessageEntity): Message? {
        var text: TBTextMessage? = messageEntity.textMessage
            ?: return null
        if (text != null) {

            var content = TextMessage(text!!.content)
            if (messageEntity.atToMessage != null) {
                for (at in messageEntity.atToMessage!!) {
                    var atToMessage = AtToMessage(at.atTo, at.read)
                    content.atToMessageList.add(atToMessage)
                }
            }
            content.extra = messageEntity.textMessage!!.extra
            return convertMessage(messageEntity, content, text.extra!!)
        }
        return null
    }

    fun createFileMsg(messageEntity: MessageEntity): Message? {
        var file: TBFileMessage? = messageEntity.fileMessage
            ?: return null
        var content = FileMessage(file!!.localPath, file.size, file.fileName, file.originUrl, file.type)
        content.extra = messageEntity.fileMessage!!.extra
        return convertMessage(messageEntity, content, file.extra)
    }

    fun createAudioMsg(messageEntity: MessageEntity): Message? {

        var audio: TBAudioMessage? = messageEntity.audioMessage
            ?: return null
        if (audio != null) {
            var content = AudioMessage(audio!!.localPath, audio.duration, audio.size, audio!!.originUrl)
            content.extra = messageEntity.audioMessage!!.extra
            return convertMessage(messageEntity, content, audio.extra)
        }
        return null
    }

    fun createVideoMsg(messageEntity: MessageEntity): Message? {
        var video: TBVideoMessage? = messageEntity.videoMessage
            ?: return null
        if (video != null) {

            var content = VideoMessage(
                video!!.localPath, video.duration, video.size, video.headUrl, video.originUrl, video.width, video.height
            )
            content.extra = messageEntity.videoMessage!!.extra
            return convertMessage(messageEntity, content, video.extra)
        }
        return null
    }

    fun createCustomMsg(messageEntity: MessageEntity): Message? {

        var custom: TBCustomMessage? = messageEntity.customMessage
            ?: return null
        if (custom != null) {
            var content = CustomMessage(custom!!.content)
            content.extra = messageEntity.customMessage!!.extra
            return convertMessage(messageEntity, content, custom.extra)
        }
        return null
    }

    fun createGeoMsg(messageEntity: MessageEntity): Message? {
        var geo: TBGeoMessage? = messageEntity.geoMessage
            ?: return null
        if (geo != null) {
            var content = GeoMessage(geo!!.title, geo!!.address, geo!!.previewUrl, geo.localPath, geo.lon, geo.lat)
            content.extra = messageEntity.geoMessage!!.extra
            return convertMessage(messageEntity, content, geo.extra)
        }
        return null

    }

    fun createImageTextMsg(messageEntity: MessageEntity): Message? {
        var imageText: TBImageTextMessage? =
            messageEntity.imageTextMessage ?: return null
        if (imageText != null) {

            var content = ImageTextMessage(imageText!!.title, imageText.content, imageText.imageUrl, imageText.redirectUrl, imageText.tag)
            content.extra = messageEntity.imageTextMessage!!.extra
            return convertMessage(messageEntity, content, imageText.extra)
        }
        return null
    }

    fun createNoticeMsg(messageEntity: MessageEntity): Message? {
        var notice: TBNoticeMessage? = messageEntity.noticeMessage
            ?: return null
        if (notice != null) {

            var content = NoticeMessage(notice!!.content, notice.operateUser, notice.users, notice.type)
            content.extra = messageEntity.noticeMessage!!.extra
            return convertMessage(messageEntity, content, notice.extra)
        }
        return null
    }

    fun createInputStatusMsg(messageEntity: MessageEntity): Message? {
        var stus: TBInputStatusMessage? = messageEntity.inputStatusMessage
            ?: return null
        if (stus != null) {
            var content = InputStatusMessage(stus!!.content)
            content.extra = messageEntity.inputStatusMessage!!.extra
            return convertMessage(messageEntity, content, stus.extra)
        }
        return null
    }

    fun createRecallMsg(messageEntity: MessageEntity): Message? {
        return convertMessage(messageEntity, null, "")
    }

    fun createCallMsg(messageEntity: MessageEntity): Message? {
        var call: TBCallMessage? = messageEntity.callMessage
            ?: return null
        if (call != null) {
            var content =
                CallMessage(call!!.roomId, call.callType, call.callState, call.userIds, call.content, call.duration, call.endType)
            content.extra = messageEntity.callMessage!!.extra
            return convertMessage(messageEntity, content, "")
        }
        return null
    }

    fun createReplyMsg(messageEntity: MessageEntity): Message? {
        var reply = messageEntity.replyMessage ?: return null
        if (reply != null) {
            try {

                var origin = MessageConvertUtil.instance.jsonMessageToMessage(Gson().fromJson(reply.origin!!, JSonMessage::class.java))
                var answer = MessageConvertUtil.instance.jsonMessageToMessage(Gson().fromJson(reply.body!!, JSonMessage::class.java))

                var content = ReplyMessage(origin, answer, reply.extra)
                content.extra = messageEntity.replyMessage!!.extra
                return convertMessage(messageEntity, content, "")
            } catch (e: Exception) {
                QLog.e(TAG, "回复消息解析失败：origin > " + reply.origin + " answer > " + reply.body)
                e.printStackTrace()
            }
        }
        return null
    }

    fun createRetransmissionMsg(messageEntity: MessageEntity): Message? {
        var retransmission = messageEntity.retransmissionMessage ?: return null
        if (retransmission != null) {
            var messages = Gson().fromJson(retransmission.msg!!, Array<JSonMessage>::class.java).toList()
            var list = arrayListOf<Message>()
            for (index in 0 until messages.size) {
                var msg = MessageConvertUtil.instance.jsonMessageToMessage(messages[index])
                if (msg != null) {
                    list.add(msg)
                }
            }
            var content = RecordMessage(list, retransmission.extra)
            content.extra = messageEntity.retransmissionMessage!!.extra
            return convertMessage(messageEntity, content, "")
        }
        return null
    }

    fun createEventMsg(messageEntity: MessageEntity): Message? {
        var eventMessage: EventMessage? = messageEntity.eventMessage
            ?: return null
        if (eventMessage != null) {
            var content =
                EventMessage(eventMessage.event, eventMessage.extra)
            content.extra = messageEntity.eventMessage!!.extra
            return convertMessage(messageEntity, content, "")
        }
        return null
    }

    companion object {
        val instance = Holder.holder
    }

    object Holder {
        val holder = MessageEntityToMessage()
    }

}