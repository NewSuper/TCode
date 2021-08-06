package com.qx.imlib.db.entity

import androidx.room.*
import com.qx.im.model.UserInfoCache
import com.qx.imlib.utils.UUIDUtil

/**
 * 消息回复
 */
@Entity(tableName = "reply_message")
class TBReplyMessage() {

    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: String = UUIDUtil.getUUID()

    @ColumnInfo(name = "message_id")
    var messageId: String = ""


    @ColumnInfo(name = "owner_id")
    var ownerId: String = ""

    @ColumnInfo(name = "conversation_id")
    var conversationId : String = ""

    /**
     * 被引用的源消息，以json格式存储
     */
    @ColumnInfo(name = "origin_msg")
    var origin: String = ""

    /**
     * 真实消息内容，以json格式存储
     */
    @ColumnInfo(name = "msg_body")
    var body: String = ""

    @ColumnInfo(name = "extra")
    var extra: String = ""

    companion object {
        fun obtain(conversationId :String, messageId: String, origin: String, body: String, extra: String): TBReplyMessage {
            var message = TBReplyMessage()
            message.messageId = messageId
            message.origin = origin
            message.body = body
            message.extra = extra
            message.ownerId = UserInfoCache.getUserId()
            message.conversationId = conversationId
            return message
        }
    }
}