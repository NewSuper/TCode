package com.qx.imui.adapter

import android.content.Context
import android.text.TextUtils
import com.google.gson.Gson
import com.qx.im.model.ConversationType
import com.qx.im.model.QXGroupUserInfo
import com.qx.im.model.QXUserInfo
import com.qx.imui.R
import com.qx.imui.bean.RecordExtra
import com.qx.message.*
import java.lang.Exception

object UserInfoUtil {
    fun  getMessageSimpleText(context: Context,message: Message,userId:String?):String{
        if (message == null || userId.isNullOrEmpty())return ""
        var userInfo = getUserInfo(message, userId)
        if(userInfo!=null){
            when(message.messageType){
                MessageType.TYPE_TEXT -> {
                    var content = message.messageContent as TextMessage
                    return String.format(context.resources.getString(R.string.qx_reply_text), message.userInfo?.displayName, content.content)
                }
                MessageType.TYPE_IMAGE -> {
                    return String.format(context.resources.getString(R.string.qx_reply_image), message.userInfo?.displayName)
                }
                MessageType.TYPE_AUDIO -> {
                    return String.format(context.resources.getString(R.string.qx_reply_audio), message.userInfo?.displayName)
                }
                MessageType.TYPE_VIDEO -> {
                    return String.format(context.resources.getString(R.string.qx_reply_video), message.userInfo?.displayName)
                }
                MessageType.TYPE_IMAGE_AND_TEXT -> {
                    var content = message.messageContent as ImageTextMessage
                    return String.format(context.resources.getString(R.string.qx_reply_image_text), message.userInfo?.displayName, content.content)
                }
                MessageType.TYPE_FILE -> {
                    var content = message.messageContent as FileMessage
                    return String.format(context.resources.getString(R.string.qx_reply_file), message.userInfo?.displayName, content.fileName)
                }
                MessageType.TYPE_GEO -> {
                    var content = message.messageContent as GeoMessage
                    return String.format(context.resources.getString(R.string.qx_reply_geo), message.userInfo?.displayName, content.address)
                }
                MessageType.TYPE_REPLY -> {
                    var reply = message.messageContent as ReplyMessage
                    return getMessageSimpleText(context, reply.answer, userId)
                }
                MessageType.TYPE_RECORD -> {
                    return String.format(context.resources.getString(R.string.qx_reply_record), message.userInfo?.displayName)
                }
            }
        }
        return ""
    }

    fun getMemberInfo(groupId: String, memberId: String): QXGroupUserInfo? {
        return QXUserInfoManager.getInstance().getGroupUserInfo(groupId, memberId)
    }

    fun getUserInfo(message: Message, userId: String): QXUserInfo? {
        if (message != null) {
            when (message.conversationType) {
                Conversation.Type.TYPE_PRIVATE -> {
                    var userInfo = QXUserInfoManager.getInstance().getUserInfo(userId)
                    if (userInfo != null) {
                        message.userInfo = userInfo
                    }
                }
                Conversation.Type.TYPE_GROUP -> {
                    var memberInfo = QXUserInfoManager.getInstance().getGroupUserInfo(message.targetId, message.senderUserId)
                    if (memberInfo != null) {
                        var userInfo = QXUserInfo()
                        userInfo.id = memberInfo.userId
                        userInfo.name = memberInfo.displayName
                        userInfo.avatarUri = memberInfo.avatarUri
                        message.userInfo = userInfo
                    }
                }
                Conversation.Type.TYPE_SYSTEM -> {

                }
                Conversation.Type.TYPE_CHAT_ROOM -> {

                }
            }
        }
        return message.userInfo
    }

    fun getUserInfo(id: String): QXUserInfo? {
        return QXUserInfoManager.getInstance().getUserInfo(id)
    }

    fun getTargetName(context: Context, type: String, targetId: String): String? {
        when (type) {
            ConversationType.TYPE_PRIVATE -> {
                var userInfo = getUserInfo(targetId)
                return userInfo?.displayName
            }
            ConversationType.TYPE_GROUP -> {
                var groupInfo = QXUserInfoManager.getInstance().getGroup(targetId)
                return groupInfo?.name
            }
            ConversationType.TYPE_SYSTEM -> {
                context.resources.getString(R.string.qx_target_name_system)
            }

            ConversationType.TYPE_CHAT_ROOM -> {
                context.resources.getString(R.string.qx_target_name_chatroom)

            }
        }
        return ""
    }

    fun userAvatarExtraUrl(message: Message): String? {
        when (message.conversationType) {
            ConversationType.TYPE_PRIVATE -> {
                return getUserInfo(message, message.senderUserId)?.avatarExtraUrl
            }
            ConversationType.TYPE_GROUP -> {
                return QXUserInfoManager.getInstance().getGroupUserInfo(message.targetId, message.senderUserId)?.avatarExtraUrl
            }
            ConversationType.TYPE_SYSTEM -> {
                return ""
            }

            ConversationType.TYPE_CHAT_ROOM -> {
                return ""
            }
        }
        return ""
    }

    fun userNameExtraUrl(message: Message): String? {
        when (message.conversationType) {
            ConversationType.TYPE_PRIVATE -> {
                return getUserInfo(message, message.senderUserId)?.nameExtraUrl
            }
            ConversationType.TYPE_GROUP -> {
                return QXUserInfoManager.getInstance().getGroupUserInfo(message.targetId, message.senderUserId)?.nameExtraUrl
            }
            ConversationType.TYPE_SYSTEM -> {
                return ""
            }

            ConversationType.TYPE_CHAT_ROOM -> {
                return ""
            }
        }
        return ""
    }

    fun getTargetName(context: Context, message: Message, userId: String): String? {
        when (message.conversationType) {
            ConversationType.TYPE_PRIVATE -> {
                var userInfo = getUserInfo(message, userId)
                return userInfo?.name

            }
            ConversationType.TYPE_GROUP -> {
                var memberInfo = QXUserInfoManager.getInstance().getGroupUserInfo(userId, message.senderUserId)
                if (memberInfo != null && !memberInfo.displayName.isNullOrEmpty()) {
                    return memberInfo.displayName
                }
            }
            ConversationType.TYPE_SYSTEM -> {
                context.resources.getString(R.string.qx_target_name_system)
            }

            ConversationType.TYPE_CHAT_ROOM -> {
                context.resources.getString(R.string.qx_target_name_chatroom)

            }
        }
        return ""
    }

    fun getSenderName(context: Context, message: Message, userId: String): String? {
        when (message.conversationType) {
            ConversationType.TYPE_PRIVATE -> {
                var userInfo = getUserInfo(message, message.senderUserId)
                return userInfo?.name

            }
            ConversationType.TYPE_GROUP -> {
                var memberInfo = QXUserInfoManager.getInstance().getGroupUserInfo(userId, message.senderUserId)
                if (memberInfo != null && !memberInfo.displayName.isNullOrEmpty()) {
                    return memberInfo?.displayName
                }
            }
            ConversationType.TYPE_SYSTEM -> {
                context.resources.getString(R.string.qx_target_name_system)
            }

            ConversationType.TYPE_CHAT_ROOM -> {
                context.resources.getString(R.string.qx_target_name_chatroom)

            }
        }
        return ""
    }

    fun getAvatar(message: Message, userId: String?): String {
        if (TextUtils.isEmpty(userId)) {
            return ""
        }
        return getUserInfo(message, userId!!)?.avatarUri.toString()
    }

    fun getRecordTitle(context: Context, message: Message): String? {
        try {

            var recordMessage = message.messageContent as RecordMessage
            var recordExtra = Gson().fromJson(recordMessage.extra, RecordExtra::class.java)

            if (recordExtra != null && !TextUtils.isEmpty(recordExtra.type)) {
                when (recordExtra.type) {
                    ConversationType.TYPE_PRIVATE -> {
                        if (!recordExtra.userId.isNullOrEmpty()) {

                            var name1 = getUserInfo(recordExtra.userId[0])?.name
                            var name2 = getUserInfo(recordExtra.userId[1])?.name

                            return String.format(context.getString(R.string.qx_record_title_private), name1, name2)
                        }
                        return ""
                    }
                    ConversationType.TYPE_GROUP -> {
                        return context.getString(R.string.qx_record_title_group)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun getRecordText(context: Context, content: RecordMessage): String? {
        var text = ""
        for (index in 0 until content.messages.size) {
            var message = content.messages[index]
            var content = getMessageSimpleText(context, message, message.senderUserId)
            text += content + "\n"
            if (index == 2) {
                return text
            }
        }
        return text
    }
}