package com.qx.imui.plugin

import com.google.gson.Gson
import com.qx.imui.bean.QXFavorite
import com.qx.message.Conversation
import com.qx.message.Message
import com.qx.message.MessageType
import com.qx.message.ReplyMessage
import java.lang.Exception

object ConvertUtil {
    fun convertToFavorite(favorites: List<Message>): List<QXFavorite> {
        var list = arrayListOf<QXFavorite>()
        for (message in favorites) {
            var f = convert(message)
            if(f != null) {
                if (message.messageType == MessageType.TYPE_REPLY) {
                    var replyMessage = message.messageContent as ReplyMessage
                    f = convert(replyMessage.answer)
                }
                if(f != null) {
                    list.add(f!!)
                }
            }
        }

        return list
    }

    private fun convert(message: Message): QXFavorite? {
        try {
            var messageType = getMessageType(message)
            var sessionType = getSessionType(message.conversationType)
            if (messageType == -1 || sessionType == -1) {

            }
            var content = Gson().toJson(message.messageContent)
            return QXFavorite(0, message.messageId, messageType,
                message.senderUserId, sessionType, content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getMessageType(message: Message): Int {
        when (message.messageType) {
            MessageType.TYPE_TEXT -> {
                return 0
            }
            MessageType.TYPE_AUDIO -> {
                return 1
            }
            MessageType.TYPE_VIDEO -> {
                return 2
            }
            MessageType.TYPE_IMAGE -> {
                return 3
            }
            MessageType.TYPE_FILE -> {
                return 4
            }
            MessageType.TYPE_IMAGE_AND_TEXT -> {
                return 5
            }
            MessageType.TYPE_GEO -> {
                return 6
            }
        }
        return -1
    }

    fun getSessionType(type: String): Int {
        when (type) {
            Conversation.Type.TYPE_PRIVATE -> {
                return 0
            }
            Conversation.Type.TYPE_GROUP -> {
                return 1
            }
        }
        return -1
    }
}