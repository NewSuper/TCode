package com.qx.rtc.call_lib

import android.content.Context
import android.os.Message
import com.qx.im.model.ConversationType
import com.qx.imlib.ModuleManager
import com.qx.imlib.QXIMClient
import com.qx.imlib.SystemCmd
import com.qx.imlib.qlog.QLog

import com.qx.message.CallReceiveMessage
import com.qx.message.Data
import com.qx.message.rtc.*
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_INCOMING
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_OUTGOING
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_REC_ACCEPT
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_REC_CANCEL
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_REC_HANGUP
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_REC_REFUSE
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_REC_SWITCH_AUDIO
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_REC_TIME_OUT
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_SEND_ACCEPT
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_SEND_CANCEL
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_SEND_HANGUP
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_SEND_REFUSE
import com.qx.rtc.call_lib.QXCallManager.Companion.CALL_SEND_SWITCH_AUDIO
import com.qx.rtc.call_lib.QXCallManager.Companion.RTC_REC_VIDEO_PARAM
import com.qx.rtc.call_lib.QXCallManager.Companion.RTC_SIGNAL_JOIN
import com.qx.rtc.call_lib.QXCallManager.Companion.RTC_SIGNAL_JOINED
import com.qx.rtc.call_lib.QXCallManager.Companion.RTC_SIGNAL_REC_ANSWER
import com.qx.rtc.call_lib.QXCallManager.Companion.RTC_SIGNAL_REC_CANDIDATE
import com.qx.rtc.call_lib.QXCallManager.Companion.RTC_SIGNAL_REC_OFFER
import org.json.JSONObject
import java.lang.Exception

/**
 * 注册消息接收
 *
 */
class QXCallClient @JvmOverloads constructor(var context: Context) {


    init {
        QLog.d(TAG,"初始化 $sIntance")
        if (sIntance == null) {
            sIntance = this
            initClient(false)
        } else {
            initClient(true)
        }
    }

    private fun initClient(reInit: Boolean) {
        if (reInit) {
            QXCallManager.instance.reInit(context)
        } else {
            QXCallManager.instance.init(context)
            val routeMessage = object : ModuleManager.MessageRouter {
                override fun onReceived(data: Data) {
                    if (data is CallReceiveMessage) {
                        // 处理消息
                        when (data.cmd) {
                            SystemCmd.S2C_VIDEO_CALL -> {
                                //音视频呼叫
                                QLog.d(TAG,"收到音视频呼叫:${data.roomId}")
                                val message = Message.obtain()
                                message.what = CALL_INCOMING
                                val callSession = QXCallSession()
                                callSession.conversionType = data.sendType
                                if (data.sendType.equals(ConversationType.TYPE_PRIVATE,true)) {
                                    callSession.targetId = QXIMClient.instance.getCurUserId()!!
                                } else {
                                    callSession.targetId = data.targetId
                                }
                                callSession.callId = data.userId
                                callSession.userIds = data.members
                                callSession.callType = data.type
                                callSession.roomId = data.roomId
                                message.obj = callSession
                                QXCallManager.instance.sendMessage(message)
                            }
                            SystemCmd.S2C_VIDEO_ANSWER -> {
                                //对方接听
                                QLog.d(TAG,"收到对方接听:${data.roomId}")
                                val message = Message.obtain()
                                message.what = CALL_REC_ACCEPT
                                QXCallManager.instance.sendMessage(message)
                            }
                            SystemCmd.S2C_VIDEO_REFUSE -> {
                                //对方拒接
                                QLog.d(TAG,"收到对方拒接:${data.roomId}")
                                val message = Message.obtain()
                                message.what = CALL_REC_REFUSE
                                QXCallManager.instance.sendMessage(message)
                            }
                            SystemCmd.S2C_VIDEO_OTHER_ANSWER -> {
                                //其他设备接听
                                QLog.d(TAG, "收到其他设备接听:${data.roomId}")
                            }
                            SystemCmd.S2C_VIDEO_CANCEL -> {
                                //对方已取消
                                QLog.d(TAG,"收到对方已取消:${data.roomId}")
                                val message = Message.obtain()
                                message.what = CALL_REC_CANCEL
                                QXCallManager.instance.sendMessage(message)
                            }
                            SystemCmd.S2C_VIDEO_OUT_TIME -> {
                                //呼叫超时
                                QLog.d(TAG,"收到呼叫超时:${data.roomId}")
                                val message = Message.obtain()
                                message.what = CALL_REC_TIME_OUT
                                QXCallManager.instance.sendMessage(message)
                            }
                            SystemCmd.S2C_VIDEO_RING_OFF -> {
                                //挂断
                                QLog.d(TAG,"收到挂断:${data.roomId}")
                                val message = Message.obtain()
                                message.what = CALL_REC_HANGUP
                                message.obj = data.roomId
                                QXCallManager.instance.sendMessage(message)
                            }
                            SystemCmd.S2C_VIDEO_SWITCH -> {
                                val message = Message.obtain()
                                message.what = CALL_REC_SWITCH_AUDIO
                                QXCallManager.instance.sendMessage(message)
                            }
                            SystemCmd.C2S_VIDEO_PARAM -> {
                                val message = Message.obtain()
                                val videoParam = RTCVideoParam()
                                videoParam.roomId = data.roomId
                                videoParam.userId = data.userId
                                videoParam.param = data.param
                                message.what = RTC_REC_VIDEO_PARAM
                                message.obj = videoParam
                                QXCallManager.instance.sendMessage(message)
                            }
                        }
                    }
                }
            }
            ModuleManager.addMessageRouter(routeMessage)
            val rtcRouteMessage = object : ModuleManager.RTCSingalMessageRouter {
                override fun onReceived(signalData: RTCSignalData) {
                    QLog.d(TAG, "RTCSingalMessageRouter:${signalData}")
                    when(signalData.cmd) {

                        SystemCmd.S2C_RTC_SIGNAL_JOIN -> {
                            // 发offer
                            try {
                                val objects = JSONObject(signalData.data)
                                val joined = RTCJoined()
                                val peers = objects.optJSONArray("peers_")
                                joined.peers = ArrayList<String>()
                                if (peers != null && peers.length() > 0) {
                                    for (index in 0 until peers.length()) {
                                        val p = peers[index] as String
                                        QLog.d(TAG,"S2C_RTC_SIGNAL_JOIN peers length:${peers.length()},peer:$p")
                                        joined.peers!!.add(p)
                                    }
                                }
                                joined.roomId = objects.optString("roomId_")
                                val message = Message.obtain()
                                message.obj = joined
                                message.what = RTC_SIGNAL_JOIN
                                QXCallManager.instance.sendMessage(message)
                            } catch (e:Exception) {
                                e.printStackTrace()
                            }
                        }

                        SystemCmd.S2C_RTC_SIGNAL_JOINED -> {
                            // 发offer
                            try {
                                val objects = JSONObject(signalData.data)
                                val joined = RTCJoined()
                                val peers = objects.optJSONArray("peers_")
                                joined.peers = ArrayList<String>()
                                if (peers != null && peers.length() > 0) {
                                    for (index in 0 until peers.length()) {
                                        val p = peers[index] as String
                                        QLog.d(TAG,"S2C_RTC_SIGNAL_JOINED peers length:${peers.length()},peer:$p")
                                        joined.peers!!.add(p)
                                    }
                                }
                                joined.roomId = objects.optString("roomId_")
                                val message = Message.obtain()
                                message.obj = joined
                                message.what = RTC_SIGNAL_JOINED
                                QXCallManager.instance.sendMessage(message)
                            } catch (e:Exception) {
                                e.printStackTrace()
                            }

                        }
                        SystemCmd.S2C_RTC_SIGNAL_OFFER -> {
                            val objects = JSONObject(signalData.data)
                            val offer = RTCOffer()
                            offer.from = objects.optString("from_")
                            offer.roomId = objects.optString("roomId_")
                            offer.to = objects.optString("to_")
                            offer.sdp = objects.optString("sdp_")
                            val message = Message.obtain()
                            message.obj = offer
                            message.what = RTC_SIGNAL_REC_OFFER
                            QXCallManager.instance.sendMessage(message)
                        }

                        SystemCmd.S2C_RTC_SIGNAL_ANSWER -> {
                            val objects = JSONObject(signalData.data)
                            val offer = RTCOffer()
                            offer.from = objects.optString("from_")
                            offer.roomId = objects.optString("roomId_")
                            offer.to = objects.optString("to_")
                            offer.sdp = objects.optString("sdp_")
                            val message = Message.obtain()
                            message.obj = offer
                            message.what = RTC_SIGNAL_REC_ANSWER
                            QXCallManager.instance.sendMessage(message)
                        }

                        SystemCmd.S2C_RTC_SIGNAL_CANDIDATE -> {
                            val objects = JSONObject(signalData.data)
                            val candidate = RTCCandidate()
                            candidate.from = objects.optString("from_")
                            candidate.roomId = objects.optString("roomId_")
                            candidate.to = objects.optString("to_")
                            val sdp = RTCSdp()
                            candidate.candidate = sdp
                            val sdpDesc = objects.optJSONObject("candidate_")
                            sdp.sdp = sdpDesc.optString("sdp_")
                            sdp.sdpMLineIndex = sdpDesc.optString("sdpMLineIndex_")
                            sdp.sdpMid = sdpDesc.optString("sdpMid_")
                            val message = Message.obtain()
                            message.obj = candidate
                            message.what = RTC_SIGNAL_REC_CANDIDATE
                            QXCallManager.instance.sendMessage(message)
                        }
                    }

                }

            }
            ModuleManager.addRTCMessageRouter(rtcRouteMessage)
        }

    }

    companion object {

        private val TAG = ""
        private var sIntance: QXCallClient? = null

        fun getInstance(): QXCallClient {
            return sIntance!!
        }

        /**
         * 切换摄像头
         */
        fun switchCamera() {
            QXCallManager.instance.switchCamera()
        }

        /**
         * 静音
         */
        fun mute(mute: Boolean) {
            QXCallManager.instance.mute(mute)
        }

        /**
         * 免提
         */
        fun handsFree(handsFree: Boolean) {
            QXCallManager.instance.handsFree(handsFree)
        }

        /**
         * 拒接
         */
        fun refuseCall() {
            QLog.d(TAG, "refuseCall")
            val message = Message.obtain()
            message.what = CALL_SEND_REFUSE
            QXCallManager.instance.sendMessage(message)
        }

        /**
         * 接听
         */
        fun acceptCall() {
            val message = Message.obtain()
            message.what = CALL_SEND_ACCEPT
            QXCallManager.instance.sendMessage(message)
        }

        /**
         * 挂断
         */
        fun hangUpCall() {
            QLog.d(TAG, "hangUpCall")
            val message = Message.obtain()
            message.what = CALL_SEND_HANGUP
            QXCallManager.instance.sendMessage(message)
        }

        /**
         * 取消呼叫
         */
        fun cancelCall() {
            QLog.d(TAG, "cancelCall")
            val message = Message.obtain()
            message.what = CALL_SEND_CANCEL
            QXCallManager.instance.sendMessage(message)
        }

        /**
         * 切换到语音通话
         */
        fun switchAudio() {
            val message = Message.obtain()
            message.what = CALL_SEND_SWITCH_AUDIO
            QXCallManager.instance.sendMessage(message)
        }

        fun setReceivedCallListener(listener: IReceiveCallListener) {
            QXCallManager.instance.setReceivedListener(listener)
        }

        fun startCall(conversionType: String,
                      targetId: String,
                      userIds: List<String>,
                      mediaType: QXCallMediaType) {
            val message = Message.obtain()
            message.what = CALL_OUTGOING
            val callSession = QXCallSession()
            callSession.conversionType = conversionType
            callSession.targetId = targetId
            callSession.callId = QXIMClient.instance.getCurUserId()!!
            callSession.userIds = userIds
            callSession.callType = "${mediaType.ordinal + 1}"
            message.obj = callSession
            QXCallManager.instance.sendMessage(message)
        }
    }


    fun getCallSession(): QXCallSession? {
        return QXCallManager.instance.getCallSession()
    }

    fun setServerHost(host: String) {
        QXCallManager.instance.setServerHost(host)
    }

    fun setUserToken(userToken: String) {
        QXCallManager.instance.setUserToken(userToken)
    }

    var qxCallListener: IQXCallListener? = null
        set(value) {
            field = value
            QXCallManager.instance.callListener = value
        }

}