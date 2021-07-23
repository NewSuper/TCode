package com.qx.rtc.call_lib

import android.os.Parcelable
import android.view.SurfaceView
import kotlinx.android.parcel.Parcelize

@Parcelize
class QXUser(var userId: String = "") : Parcelable {
    var surfaceView: SurfaceView? = null
}