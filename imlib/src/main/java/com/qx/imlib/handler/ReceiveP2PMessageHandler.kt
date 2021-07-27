package com.qx.imlib.handler

import android.util.Log
import com.qx.imlib.db.IMDatabaseRepository
import com.qx.imlib.netty.S2CRecMessage
import com.qx.imlib.utils.event.EventBusUtil
import com.qx.it.protos.S2CCustomMessage
import com.qx.message.MessageType
import io.netty.channel.ChannelHandlerContext

class ReceiveP2PMessageHandler:ReceiveNewMessageUtilHandler() {
    private  val TAG = "ReceiveP2PMessageHandle"
    override fun handle(ctx: ChannelHandlerContext?, recMessage: S2CRecMessage?) {
        var msg = S2CCustomMessage.Msg.parseFrom(recMessage!!.contents)
        Log.e(TAG, "handle: 收到单聊消息：" + msg.messageType)
        when (msg.messageType) {
            MessageType.TYPE_REPLY, MessageType.TYPE_RECORD, MessageType.TYPE_NOTICE, MessageType.TYPE_TEXT, MessageType.TYPE_IMAGE, MessageType.TYPE_IMAGE_AND_TEXT, MessageType.TYPE_AUDIO, MessageType.TYPE_VIDEO, MessageType.TYPE_FILE, MessageType.TYPE_GEO, MessageType.TYPE_AUDIO_CALL, MessageType.TYPE_VIDEO_CALL, MessageType.TYPE_CARD, MessageType.TYPE_RPP -> {
                super.handle(ctx, recMessage)
            }
            MessageType.TYPE_STATUS -> {
                //如果是消息输入状态
                EventBusUtil.postInputStatusMessage(msg.from)
            }
            MessageType.TYPE_RECALL -> {
                //如果是撤回消息，则更新消息类型为撤回类型
                var row = IMDatabaseRepository.instance.updateMessageToRecallType(msg.recall.targetMessageId)
                //如果能在本地找到该记录
                if (row > 0) {
                    var messageEntity = IMDatabaseRepository.instance.getMessageById(
                        msg.recall.targetMessageId
                    )
                    if (messageEntity != null) {
                        IMDatabaseRepository.instance.refreshConversationInfo(messageEntity)
                        EventBusUtil.postRecallMessage(
                            messageEntity
                        )
                    }
                } else {
                    //如果找不到该记录，则应该插入数据库，这时调用基类方法，走插入消息流程
                    //这种情况一般出现于：当服务器向客户端发送撤回消息时，这时客户端刚好不在线，没收到，当客户端换一台设备登录时，
                    // 这时客户端发一条撤回消息过来，但是被撤回的原本消息在本地找不到，服务器也不会通过历史消息传过来，
                    // 所以此时要插入一下recall的消息
//                    super.handle(ctx, recMessage)
                }
            }
            else -> {
                //此处为自定义消息
                super.handle(ctx, recMessage)
                when (msg.messageType) {
                    MessageType.CustomMsge.THROUGH_VERIFY_MSG_TYPE,MessageType.CustomMsge.SAY_HELLO_MSG_TYPE -> {
                    }
                }
            }
        }
    }
}