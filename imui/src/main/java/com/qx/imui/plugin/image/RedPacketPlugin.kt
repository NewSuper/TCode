package com.qx.imui.plugin.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.qx.imui.QXExtension
import com.qx.imui.R
import com.qx.imui.plugin.IPluginModule
import com.qx.imui.util.ToastUtil

class RedPacketPlugin : IPluginModule {

    override fun obtainDrawable(context: Context): Drawable {
        return context.resources.getDrawable(R.drawable.imui_ic_chat_red_paper)
    }

    override fun obtainTitle(context: Context): String {
        return context.resources.getString(R.string.qx_chat_add_panel_red_paper)
    }

    override fun onClick(context: Activity, extension: QXExtension) {
        ToastUtil.toast(context,"发红包了")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    }
}