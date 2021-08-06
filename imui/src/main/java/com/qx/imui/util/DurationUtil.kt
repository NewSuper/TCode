package com.qx.imui.util

object DurationUtil {
    fun getDuration(duration: Int): String {
        var hour = duration / 3600
        var min = duration % 3600 / 60 //取3600的余数，再除60就是分钟数
        var sec = duration % 3600 % 60 //取3600的余数，再取60的余数，就是秒数
        return if (hour == 0) {
            if (min < 10) {
                if (sec < 10) {
                    "0$min:0$sec"
                } else {
                    "0$min:$sec"
                }
            } else {
                if (sec < 10) {
                    "$min:0$sec"
                } else {
                    "$min:$sec"
                }
            }
        } else {
            if (min < 10) {
                if (sec < 10) {
                    "$hour:0$min:0$sec"
                } else {
                    "$hour:0$min:$sec"
                }
            } else {
                if (sec < 10) {
                    "$hour:$min:0$sec"
                } else {
                    "$hour:$min:$sec"
                }
            }
        }
    }
}