package com.qx.imui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.qx.imui.R
import com.qx.imui.plugin.CustomMessageManager
import com.qx.imui.plugin.MessageProvider
import com.qx.message.Message
import com.qx.message.MessageContent
import com.qx.message.MessageType
import com.qx.message.ReplyMessage
import java.lang.Exception

abstract class ChatBaseMessageHandler : BaseMessageHandler() {

    override fun setContentView(itemView: View, contentLayout: ViewGroup, message: Message) {
        setMsgBackground(itemView, contentLayout, message) //设置气泡背景
        messageRotation(itemView, contentLayout, message) //镜像翻转
        updateReplyUI(itemView, message) //设置回复消息UI
    }

    /**
     * 处理消息内容，注意区分回复、转发等类型
     */
    fun getMessageContent(message: Message): MessageContent? {
        try {

            return when (message.messageType) {
                MessageType.TYPE_REPLY -> {
                    //回复类型
                    (message.messageContent as ReplyMessage).answer.messageContent
                }
                else -> {
                    message.messageContent
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 这里处理对话中被回复的Text显示隐藏
     */
    private fun updateReplyUI(itemView: View, message: Message) {
        var tv_answer = itemView.findViewById<TextView>(R.id.tv_answer)
        if (message.messageType == MessageType.TYPE_REPLY) {
            tv_answer.visibility = View.VISIBLE
            var reply = message.messageContent as ReplyMessage
            tv_answer.text = UserInfoUtil.getMessageSimpleText(
                itemView.context,
                reply.origin,
                reply.origin.senderUserId
            )
        } else {
            tv_answer.visibility = View.GONE
        }
    }

    private fun isNotCustomMessage(msg: Message): Boolean {
        return msg.messageType == MessageType.TYPE_TEXT ||
                msg.messageType == MessageType.TYPE_IMAGE ||
                msg.messageType == MessageType.TYPE_IMAGE_AND_TEXT ||
                msg.messageType == MessageType.TYPE_AUDIO ||
                msg.messageType == MessageType.TYPE_VIDEO ||
                msg.messageType == MessageType.TYPE_AUDIO_CALL ||
                msg.messageType == MessageType.TYPE_VIDEO_CALL ||
                msg.messageType == MessageType.TYPE_FILE ||
                msg.messageType == MessageType.TYPE_NOTICE ||
                msg.messageType == MessageType.TYPE_RECORD ||
                msg.messageType == MessageType.TYPE_GEO ||
                msg.messageType == MessageType.TYPE_REPLY
    }

    private fun rotation(view: View, degree: Float) {
        view.rotationX = degree
        if (view is ViewGroup) {
            if (view.childCount > 0) {
                for (index in 0 until view.childCount) {
                    rotation(view.getChildAt(index), degree)
                }
            }
        }
    }

    private fun messageRotation(itemView: View, contentLayout: ViewGroup, message: Message) {
        //对通话消息和音频消息做镜像翻转

        if (message.direction == Message.Direction.DIRECTION_SEND) {
            when (message.messageType) {
                MessageType.TYPE_AUDIO_CALL, MessageType.TYPE_VIDEO_CALL -> {
                    var view = (contentLayout.getChildAt(0) as ViewGroup).getChildAt(0)
                    rotation(view, 180F)
                    view.findViewById<View>(R.id.iv_call_icon).rotationY = 0F
                }
                MessageType.TYPE_AUDIO -> {
                    var view = (contentLayout.getChildAt(0) as ViewGroup).getChildAt(0)
                    rotation(view, 180F)
//                    view.findViewById<View>(R.id.iv_voice_icon).rotationY = 0F
                }
            }
        }
    }
    private fun setMsgBackground(itemView: View, contentLayout: ViewGroup, message: Message) {
        //消息内容根view
        var contentRootView = contentLayout.getChildAt(0)
        var msg = message
        //图片、视频消息不需要气泡
        if (msg.messageType != MessageType.TYPE_VIDEO && msg.messageType != MessageType.TYPE_IMAGE) {
            //方向：发送
            if (msg.direction == Message.Direction.DIRECTION_SEND) {
                //处理回复类型
                if (message.messageType == MessageType.TYPE_REPLY) {
                    var reply = msg.messageContent as ReplyMessage
                    msg = reply.answer
                }
                //文件、图文消息的气泡为白色
                if (msg.messageType == MessageType.TYPE_FILE
                    || msg.messageType == MessageType.TYPE_IMAGE_AND_TEXT
                    || msg.messageType == MessageType.TYPE_RECORD) {
                    contentRootView.setBackgroundResource(R.mipmap.chat_bg_right_white)
                } else if (msg.messageType == MessageType.TYPE_GEO) {
                    contentRootView.setBackgroundResource(R.mipmap.bg_geo_right_bg)
                } else {
//                    LogUtil.error(this.javaClass, msg.messageType + " - " + isNotCustomMessage(msg))
                    if (isNotCustomMessage(msg)) {
                        //其它为蓝色
                        contentRootView.setBackgroundResource(R.mipmap.chat_bg_right_blue)
                    } else {
                        //如果为自定义消息
                        var bubbleStyle = CustomMessageManager.getBubbleStyle(msg.messageType)
                        if (bubbleStyle?.right == MessageProvider.BubbleStyle.Style.BLUE) {
                            contentRootView.setBackgroundResource(R.mipmap.chat_bg_right_blue)
                        } else if (bubbleStyle?.right == MessageProvider.BubbleStyle.Style.WHITE) {
                            contentRootView.setBackgroundResource(R.mipmap.chat_bg_right_white)
                        }
                    }
                }
            } else {
                //方向：接收
                //如果为预设消息（非自定义消息）
                if (isNotCustomMessage(msg)) {
                    if (msg.messageType == MessageType.TYPE_GEO) {
                        contentRootView.setBackgroundResource(R.mipmap.bg_geo_left_bg)
                    } else {
                        contentRootView.setBackgroundResource(R.mipmap.chat_bg_left_white)
                    }
                } else {
                    //如果为自定义消息
                    var bubbleStyle = CustomMessageManager.getBubbleStyle(msg.messageType)
                    if (bubbleStyle != null) {
                        contentRootView.setBackgroundResource(R.mipmap.chat_bg_left_white)
                    }
                }

            }
        }
    }
}