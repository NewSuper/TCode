package com.qx.imui.plugin

import android.content.Intent
import java.util.LinkedHashMap

interface IExtensionClickListener {

    fun onImageResult(data: LinkedHashMap<String, Int>?, sendOrigin: Boolean)

    fun onLocationResult(data: Intent)

    fun onFileReuslt(data: Intent)
}