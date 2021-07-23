package com.qx.rtc.call_lib

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class QXCallSession(var callId: String = "",
                    var targetId: String = "",
                    var sessionId: String = "",
                    var callState:CallState = CallState.CALL_START, // 0 呼叫中 1 已取消 2 被拒接 3 通话中 4 已结束
                    var conversionType: String = "", // 转发类型，PRIVATE-单聊(点对点)；GROUP-群聊；
                    var startTime:Long = 0L,
                    var activeTime:Long = 0L,
                    var endTime:Long = 0L,
                    var callType: String = "",  // 1-音频 2-视频
                    var userIds: List<String> = mutableListOf(),
                    var userProfie: MutableList<QXUser> = mutableListOf<QXUser>(),
                    var roomId: String = "") : Parcelable {


    fun getMediaType() :QXCallMediaType {
        return  if (callType == "1") QXCallMediaType.AUDIO else QXCallMediaType.VIDEO
    }
}

enum class CallState {
    CALL_START,CANCELED,REFUSE,CALL_ING,CALL_OVER
}