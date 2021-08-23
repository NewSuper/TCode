package com.qx.imui.util

import android.app.Activity

interface AcitivityImpl {
    fun add(activity: Activity)
    fun remove(activity: Activity)
    fun removeAll()
}