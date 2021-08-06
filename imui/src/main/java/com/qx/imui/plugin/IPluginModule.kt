package com.qx.imui.plugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.qx.imui.QXExtension

interface IPluginModule {
    fun obtainDrawable(context: Context): Drawable

    fun obtainTitle(context: Context): String

    fun onClick(context: Activity, extension: QXExtension)

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}