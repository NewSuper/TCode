package com.qx.rtc.call_lib

interface IReceiveCallListener {
    fun onReceivedCall(session:QXCallSession)
}