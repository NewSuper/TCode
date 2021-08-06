package com.qx.imui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.qx.imui.QXIMKit
import com.qx.imui.R
import com.qx.imui.util.DensityUtil
import com.qx.imui.util.DurationUtil
import com.qx.message.Message
import com.qx.message.MessageContent
import com.qx.message.VideoMessage

class ChatVideoMessageHandler : ChatBaseMessageHandler() {
    override fun setContentView(itemView: View, contentLayout: ViewGroup, message: Message) {
        var contentView = createContentView(itemView, contentLayout, R.layout.imui_layout_msg_content_video)
        super.setContentView(itemView, contentLayout, message)

        var content: MessageContent? = getMessageContent(message) ?: return
        content = content as VideoMessage

        var rootView = (contentView!!.getChildAt(0)) as ConstraintLayout

        var iv_video = rootView.findViewById<ImageView>(R.id.iv_video_msg)
        var tv_duration = rootView.findViewById<TextView>(R.id.tv_duration)

        var imgPath: String = content.headUrl

        //按图片宽高比自适应控件宽高
        var lp: ConstraintLayout.LayoutParams = iv_video.layoutParams as ConstraintLayout.LayoutParams
        var width = content.width.toFloat()
        var height = content.height.toFloat()


        when {
            width < height -> {
                var scale: Float = height / width
                lp.width = DensityUtil.dip2px(itemView.context, 90f)
                lp.height = (lp.width * scale).toInt()

            }
            width > height -> {
                var scale: Float = width / height
                lp.height = DensityUtil.dip2px(itemView.context, 90f)
                lp.width = (lp.height * scale).toInt()
            }
            else -> {
                lp.width = DensityUtil.dip2px(itemView.context, 90f)
                lp.height = lp.width
            }

        }
        iv_video.layoutParams = lp

        Glide.with(itemView.context).load(QXIMKit.getInstance().getRealUrl(imgPath)).apply(
            RequestOptions
                .bitmapTransform(RoundedCorners(6))
                .placeholder(R.mipmap.default_img)
        ).into(iv_video)

        var duration = DurationUtil.getDuration(content.duration)

        tv_duration.text = duration
    }
}