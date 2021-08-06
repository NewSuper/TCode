package com.qx.imui.plugin.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.qx.imui.QXExtension
import com.qx.imui.R
import com.qx.imui.plugin.IPluginModule
import com.qx.imui.plugin.IPluginRequestPermissionResultCallback
import com.qx.imui.util.PermissionCheckUtil

class ImagePlugin : IPluginModule, IPluginRequestPermissionResultCallback {

    override fun obtainDrawable(context: Context): Drawable {
        return context.resources.getDrawable(R.drawable.imui_ic_chat_gallery)
    }

    override fun obtainTitle(context: Context): String {
        return context.resources.getString(R.string.qx_chat_add_panel_gallery)
    }

    override fun onClick(activity: Activity, extension: QXExtension) {
        val permissions = arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.CAMERA")
        if (PermissionCheckUtil.checkPermissions(activity, permissions)) {
            showImage(activity,extension)
        } else {
            extension.requestPermissionForPluginResult(permissions, 255, this)
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    }

    override fun onRequestPermissionResult(activity: Activity, extension: QXExtension, requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        if (PermissionCheckUtil.checkPermissions(activity, permissions)) {
            showImage(activity,extension)
        } else {
            extension.showRequestPermissionFailedAlter(PermissionCheckUtil.getNotGrantedPermissionMsg(activity, permissions, grantResults))

        }
        return true
    }

    fun showImage(activity: Activity, extension: QXExtension) {
        val intent = Intent(activity, PictureSelectorActivity::class.java)
        extension.startActivityForPluginResult(intent,23,this)
    }
}