package com.qx.imui.emotion

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import com.qx.imui.R

class EmotionTab : FrameLayout {

    private var mIvIcon: ImageView? = null
    private var mStickerCoverPath: String? = null
    private var mIconSrc: Int = 0
    constructor(context: Context, icon:Int) : super(context) {
        mIconSrc = icon
        init(context)
    }

    constructor(context: Context, stickerCoverPath:String) : super(context) {
        mStickerCoverPath = stickerCoverPath
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.imui_plugin_emoji_three_tab,this)
        mIvIcon = findViewById(R.id.ivIcon)
        if (mStickerCoverPath.isNullOrEmpty()) {
            mIvIcon?.setImageResource(mIconSrc)
        } else {

        }
    }

}