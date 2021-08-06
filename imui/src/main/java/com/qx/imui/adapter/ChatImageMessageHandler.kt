package com.qx.imui.adapter

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.qx.imui.QXIMKit
import com.qx.imui.R
import com.qx.imui.util.SizeUtil
import com.qx.message.ImageMessage
import com.qx.message.Message
import com.qx.message.MessageContent

class ChatImageMessageHandler : ChatBaseMessageHandler() {

    override fun setContentView(itemView: View, contentLayout: ViewGroup, message: Message) {
        var contentView = createContentView(itemView, contentLayout, R.layout.imui_layout_msg_content_image)
        super.setContentView(itemView, contentLayout, message)

        var content: MessageContent? = getMessageContent(message) ?: return
        content = content as ImageMessage

        var imageView = (contentView!!.getChildAt(0)) as ImageView

        var imgPath: String = content.localPath
        if (TextUtils.isEmpty(imgPath)) {
            imgPath = content.originUrl
        }

        //按图片宽高比自适应控件宽高
        var lp: ConstraintLayout.LayoutParams = imageView.layoutParams as ConstraintLayout.LayoutParams
        var width = content.width.toFloat()
        var height = content.height.toFloat()

        imageView.layoutParams = SizeUtil.calcSize(width, height, 90, lp, imageView.context)

        Glide.with(itemView.context).load(QXIMKit.getInstance().getRealUrl(imgPath))
            .diskCacheStrategy(DiskCacheStrategy.ALL).apply(
                RequestOptions
                    .bitmapTransform(RoundedCorners(6))
                    .placeholder(R.mipmap.default_img)
            ).into(imageView)

    }
}
