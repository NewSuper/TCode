package com.qx.imui.base

import com.blankj.utilcode.util.ToastUtils

fun showToast(msg: String) {
    ToastUtils.showShort(msg)
}

fun showLongToast(msg: String) {
    ToastUtils.showLong(msg)
}