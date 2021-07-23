package com.qx.rtc.call_lib

enum class QXCallState {
    ERROR_NET,//网络错误
    TIME_OUT,//超时
    DISCONNECTED,//断连
    HANGUP,//挂断
    OTHER_HANGUP,
    REFRUSE,
    OTHER_REFRUSE,
    CANCEL,
    OTHER_CANCEL,
    EXIT,
    MESSAGE_OBJECT_BUSING,
    NOT_FRIEND,//非好友
    ERROR_PARAM,//参数错误
    UNKOWN

}