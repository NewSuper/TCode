package com.qx.imui.adapter

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.qx.imlib.QXIMClient
import com.qx.imlib.qlog.QLog
import com.qx.imui.QXContext
import com.qx.imui.QXIMKit
import com.qx.imui.R
import com.qx.imui.util.DensityUtil
import com.qx.imui.util.DownloadCallback
import com.qx.message.AudioMessage
import com.qx.message.Message
import com.qx.message.MessageContent
import com.qx.message.QXError

class ChatVoiceMessageHandler : ChatBaseMessageHandler() {

    override fun setContentView(itemView: View, contentLayout: ViewGroup, message: Message) {
        var viewHolder: ViewHolder?
        if (itemView.tag != null) {
            viewHolder = itemView.tag as ViewHolder
        } else {
            viewHolder = ViewHolder()
            itemView.tag = viewHolder
        }
        var contentView = createContentView(itemView, contentLayout, R.layout.imui_layout_msg_content_voice)
        super.setContentView(itemView, contentLayout, message)
        var rootView = (contentView!!.getChildAt(0)) as ConstraintLayout
        viewHolder?.voiceIcon = rootView.findViewById<AppCompatImageView>(R.id.iv_voice_icon)
        viewHolder?.durationTv = rootView.findViewById<TextView>(R.id.tv_voice_duration)

        bindView(itemView.context, viewHolder!!, message, false)
    }

    private fun bindView(context: Context, viewHolder: ViewHolder, message: Message, playing: Boolean) {

        var content: MessageContent? = getMessageContent(message) ?: return
        content = content as AudioMessage

        if (content.localPath.isNullOrEmpty()) {
            var length : Long = -1
            if(message != null) {
                if (message?.messageContent is AudioMessage) {
                    length = (message?.messageContent as AudioMessage).size
                }
            }
            QLog.d("voice","download url: ${content.originUrl}")
            QXContext.getInstance().downloadProvider.download(QXIMKit.FileType.TYPE_VOICE, length, content.originUrl, object :
                DownloadCallback {
                override fun onFailed(errorCode: Int, errorMsg: String?) {
                    QLog.d("voice","download url onFailed : $errorCode,$errorMsg")
                }

                override fun onProgress(progress: Int) {
                    QLog.d("voice","download url progress : $progress")
                }

                override fun onCompleted(path: String?) {
                    QLog.d("voice","download url onCompleted: ${path}")
                    path?.let {
                        if (message.messageContent is AudioMessage) {
                            (message.messageContent as AudioMessage).localPath = it
                            QXIMClient.instance!!.saveOnly(message, object : QXIMClient.SendMessageCallback() {
                                override fun onAttached(message: Message?) {

                                }

                                override fun onSuccess() {
                                }

                                override fun onError(error: QXError, message: Message?) {
                                }

                            })
                        }
                    }
                }

            })
        }
        var second = context.resources.getString(R.string.qx_message_voice_duration)
        if (content.duration < 1) {
            content.duration = 1
        } else if (content.duration == 59 || content.duration > 60) {
            content.duration = 60
        }
        var duration = String.format(second, content.duration)

        viewHolder.durationTv?.text = duration
        var lp = viewHolder.durationTv?.layoutParams as ConstraintLayout.LayoutParams
        lp.rightMargin = getVoiceLayoutLength(context, content.duration.toFloat())
        viewHolder.durationTv?.layoutParams = lp

        var animationDrawable: AnimationDrawable
        if (message.direction == Message.Direction.DIRECTION_SEND) {
            animationDrawable = (context.resources.getDrawable(R.drawable.imui_an_voice_sent) as AnimationDrawable)
            if (playing) {
                viewHolder.voiceIcon?.setImageDrawable(animationDrawable)
                animationDrawable.start()
            } else {
                viewHolder.voiceIcon?.setImageDrawable(context.resources.getDrawable(R.drawable.imui_ic_voice_sent_level_3))
                animationDrawable.stop()
            }
        } else {
            animationDrawable = (context.resources.getDrawable(R.drawable.imui_an_voice_receive) as AnimationDrawable)
            viewHolder.voiceIcon?.setImageDrawable(animationDrawable)
            if (playing) {
                viewHolder.voiceIcon?.setImageDrawable(animationDrawable)
                animationDrawable.start()
            } else {
                viewHolder.voiceIcon?.setImageDrawable(context.resources.getDrawable(R.drawable.imui_ic_voice_level_3))
                animationDrawable.stop()
            }
        }
    }

    private fun getVoiceLayoutLength(context: Context, duration: Float): Int {
        var maxLength = 120F
        when {
            duration < 5 -> {
                return DensityUtil.dip2px(context, duration / 5 * (1.4F / 6F) * maxLength)
            }
            duration < 10 -> {
                return DensityUtil.dip2px(context, duration / 10 * (1.8F / 6F) * maxLength)
            }
            duration < 20 -> {
                return DensityUtil.dip2px(context, duration / 20 * (2.2F / 6F) * maxLength)
            }
            duration < 30 -> {
                return DensityUtil.dip2px(context, duration / 30 * (2.8F / 6F) * maxLength)
            }
            duration < 40 -> {
                return DensityUtil.dip2px(context, duration / 40 * (4F / 6F) * maxLength)
            }
            duration < 50 -> {
                return DensityUtil.dip2px(context, duration / 50 * (5F / 6F) * maxLength)
            }
            else -> {
                return DensityUtil.dip2px(context, duration / 60 * maxLength)
            }
        }
    }

    class ViewHolder {
        var voiceIcon: AppCompatImageView? = null
        var durationTv: TextView? = null
    }

    inner class VoiceMessagePlayListener(var context: Context, var message: Message, var holder: ViewHolder) : IAudioPlayListener {
        override fun onComplete(uri: Uri?) {
            bindView(context, holder, message, false)
        }

        override fun onStart(uri: Uri?) {
            bindView(context, holder, message, true)
        }

        override fun onStop(uri: Uri?) {
            bindView(context, holder, message, false)
        }
    }

}