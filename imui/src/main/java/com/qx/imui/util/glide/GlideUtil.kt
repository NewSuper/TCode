package com.qx.imui.util.glide

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.disklrucache.DiskLruCache
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.engine.cache.SafeKeyGenerator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.EmptySignature
import com.qx.imui.QXIMKit
import com.qx.imui.R
import java.io.File
import java.io.IOException

object GlideUtil {

    fun loadAvatar(context: Context, url: String, imageView: ImageView) {
        Glide.with(context).load(url).apply(
            RequestOptions.bitmapTransform(
                CircleCrop()
            ).placeholder(R.mipmap.chat_avatar_default)
        ).into(imageView)

    }

    fun loadAvatar(context: Context, url: String, imageView: ImageView, transform: CornerTransform) {

        Glide.with(context).load(url).apply(
            RequestOptions.bitmapTransform(
                transform
            ).placeholder(R.mipmap.chat_avatar_default)
        ).into(imageView)
    }

    fun loadImage(context: Context, url: String, imageView: ImageView) {
        Glide.with(context).setDefaultRequestOptions(
            RequestOptions()
            .centerCrop()
            .placeholder(R.mipmap.chat_avatar_default)
            .fitCenter()
        ).load(QXIMKit.getInstance().getRealUrl(url)).into(imageView)
    }

    /**
     * 加载头像标签
     */
    fun loadAvatarFlag(context: Context, url: String, imageView: ImageView) {
        Glide.with(context).load(url).into(imageView)
    }


    fun loadBackground(context: Context, url: String, imageView: ImageView) {
        Glide.with(context).setDefaultRequestOptions(
            RequestOptions()
            .centerCrop()
            .fitCenter()
        ).load(QXIMKit.getInstance().getRealUrl(url)).into(imageView)
    }

    fun loadAvatar(context: Context, resId: Int, imageView: ImageView) {
        Glide.with(context).load(resId).apply(
            RequestOptions.bitmapTransform(
                CircleCrop()
            ).placeholder(R.mipmap.chat_avatar_default)
        ).into(imageView)
    }

    fun getCacheFile(context: Context, url: String?): File? {
        if (url.isNullOrEmpty())
            return null
        val dataCacheKey = DataCacheKey(
            GlideUrl(url),
            EmptySignature.obtain()
        )
        val safeKeyGenerator = SafeKeyGenerator()
        val safeKey = safeKeyGenerator.getSafeKey(dataCacheKey)
        try {
            val cacheSize = 500 * 1000 * 1000
            val diskLruCache = DiskLruCache.open(File(context.cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR), 1, 1, cacheSize.toLong())
            val value = diskLruCache[safeKey]
            if (value != null) {
                return value.getFile(0)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

}