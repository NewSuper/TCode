package com.qx.imlib.db.entity

import androidx.room.*
import com.qx.im.model.UserInfoCache
import com.qx.imlib.utils.UUIDUtil

/**
 * 转发消息
 */
@Entity(tableName = "retransmission_message")
class TBRetransmissionMessage() {

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
     * 转发的消息，以json数组格式存储
     */
    @ColumnInfo(name = "msg")
    var msg: String = ""

    @ColumnInfo(name = "extra")
    var extra: String = ""

    companion object {
        fun obtain(conversationId : String, messageId: String, msg: String, extra: String): TBRetransmissionMessage {
            var message = TBRetransmissionMessage()
            message.conversationId = conversationId
            message.ownerId = UserInfoCache.getUserId()
            message.messageId = messageId
            message.msg = msg
            message.extra = extra
            return message
        }
    }
}