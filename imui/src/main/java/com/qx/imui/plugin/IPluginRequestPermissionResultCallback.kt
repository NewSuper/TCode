package com.qx.imui.plugin

import android.app.Activity
import com.qx.imui.QXExtension

interface IPluginRequestPermissionResultCallback {


    val REQUEST_CODE_PERMISSION_PLUGIN: Int get() = 255

    fun onRequestPermissionResult(activity: Activity, extension: QXExtension, requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean
}