package com.qx.rtc.call_lib

interface IConnectListener {

    fun join()
    fun joined()
    fun connecting()
    fun connected()
    fun disconnect(callState:QXCallState)
    fun error(callState:QXCallState)
}