package com.qx.imui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qx.imui.util.AudioPlayManager

class QXKitReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            it.action?.let {
                // 电话响铃时 关闭语音播放
                AudioPlayManager.getInstance().stopPlay()
            }
        }
    }
}