package com.qx.imui.emotion

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.qx.imui.R
import com.qx.imui.emotion.StickerManager.Companion.STICKER_DEFAULT_USER_FAV_ADD


class StickerAdapter(layout: Int) : BaseQuickAdapter<StickerItem, BaseViewHolder>(layout) {
    override fun convert(helper: BaseViewHolder, item: StickerItem) {
        val imageView = helper.getView<ImageView>(R.id.ivStickerGrid)
        if (item.name == STICKER_DEFAULT_USER_FAV_ADD) {
            helper.setVisible(R.id.llStickerAdd, true)
            helper.setVisible(R.id.ivStickerGrid, false)
        } else {
            helper.setVisible(R.id.llStickerAdd, false)
            helper.setVisible(R.id.ivStickerGrid, true)
            var path = if (TextUtils.isEmpty(item.localPath)) item.originUrl else item.localPath
            Glide.with(helper.itemView.context)
                .asBitmap()
                .load(path)
                .error(R.mipmap.chat_avatar_default)
                .placeholder(StickerManager.placeHolderId)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(5)))
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        item.width = resource.width
                        item.height = resource.height
                        val drawable: Drawable = BitmapDrawable(imageView.resources, resource)
                        imageView.setImageDrawable(drawable)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }
                })
        }
    }
}