package com.qx.imui.plugin.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.qx.imui.QXExtension
import com.qx.imui.R
import com.qx.imui.plugin.IPluginModule
import com.qx.imui.plugin.IPluginRequestPermissionResultCallback
import com.qx.imui.util.LibStorageUtils
import com.qx.imui.util.PermissionCheckUtil

class LocationPlugin : IPluginModule, IPluginRequestPermissionResultCallback {
    override fun obtainDrawable(context: Context): Drawable {
        return context.resources.getDrawable(R.drawable.imui_ic_chat_location)
    }

    override fun obtainTitle(context: Context): String {
        return context.resources.getString(R.string.qx_chat_add_panel_location)
    }

    override fun onClick(context: Activity, extension: QXExtension) {
        val permissions: Array<String>
        permissions = if (LibStorageUtils.isBuildAndTargetForQ(context)) {
            arrayOf("android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.ACCESS_NETWORK_STATE")
        } else {
            arrayOf("android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_NETWORK_STATE")
        }
        if (PermissionCheckUtil.checkPermissions(context, permissions)) {
            startLocation(context,extension)
        } else {
            extension.requestPermissionForPluginResult(permissions, 255, this)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    }

    override fun onRequestPermissionResult(activity: Activity, extension: QXExtension, requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        if (PermissionCheckUtil.checkPermissions(activity, permissions)) {
            this.startLocation(activity, extension)
        } else{
            extension.showRequestPermissionFailedAlter(PermissionCheckUtil.getNotGrantedPermissionMsg(activity, permissions, grantResults))
        }
        return true
    }

    private fun startLocation(context: Activity, extension: QXExtension) {
        val intent = Intent(context, LocationActivity::class.java)
        intent.putExtra("targetId", extension.targetId)
        intent.putExtra("conversationType", extension.conversationType)
        extension.startActivityForPluginResult(intent, 23,this)
    }
}