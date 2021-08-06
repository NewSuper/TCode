package com.qx.imui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.qx.imui.R
import com.qx.imui.util.DurationUtil
import com.qx.message.CallMessage
import com.qx.message.Message
import com.qx.message.MessageContent

class ChatCallMessageHandler : ChatBaseMessageHandler() {
    override fun setContentView(itemView: View, contentLayout: ViewGroup, message: Message) {
        var contentView = createContentView(itemView, contentLayout, R.layout.imui_layout_msg_content_call)
        super.setContentView(itemView, contentLayout, message)
        var content: MessageContent? = getMessageContent(message) ?: return
        content = content as CallMessage
        var rootView = (contentView!!.getChildAt(0)) as ConstraintLayout

        var iv_call_icon = rootView.findViewById<AppCompatImageView>(R.id.iv_call_icon)
        var tv_call = rootView.findViewById<TextView>(R.id.tv_call)


        var format = itemView.context.resources.getString(R.string.qx_message_call_accepted)
        var duration = String.format(format, DurationUtil.getDuration(content.duration.toInt()))

        if (content.callType == "1") {
            // audio
            if (content.endType == 0) {
                tv_call.text = duration
                //已通话
                iv_call_icon.setImageResource(R.drawable.imui_ic_call_audio_accepted)
            } else {
                //已取消
                iv_call_icon.setImageResource(R.drawable.imui_ic_call_audio_cancel)
                tv_call.text = getCallState(itemView.context,content.endType,message.direction)
            }
        }  else {
            if (content.endType == 0) {
                //已通话
                tv_call.text = duration
            } else {
                tv_call.text = getCallState(itemView.context,content.endType,message.direction)
            }
            //video
            iv_call_icon.setImageResource(R.drawable.imui_ic_call_video)
        }
    }

    private fun getCallState(context: Context, endType:Int, direction: Int): String {
        return when(endType) {
            1 -> context.resources.getString(R.string.qx_message_call_canceled)
            2 -> context.resources.getString(R.string.qx_message_call_refuse)
            3 -> context.resources.getString(R.string.qx_message_call_error)
            4 -> context.resources.getString(R.string.qx_message_call_interrupt)
            6 ->  {
                //对方忙碌
                if (direction == Message.Direction.DIRECTION_SEND) {
                    context.resources.getString(R.string.qx_message_call_busy)
                } else {
                    context.resources.getString(R.string.qx_message_call_canceled)
                }
            }

            else -> {
                //对方无响应
                if (direction == Message.Direction.DIRECTION_SEND) {
                    context.resources.getString(R.string.qx_message_call_not_responding)
                } else {
                    context.resources.getString(R.string.qx_message_call_canceled)
                }
            }
        }
    }
}