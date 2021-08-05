package com.qx.rtc.call_lib

import android.util.Log
import com.qx.imlib.QXIMClient
import com.qx.imlib.qlog.QLog
import com.qx.imlib.qlog.QLogTrace
import com.qx.imlib.utils.TimeUtil.getTime
import com.qx.message.QXError
import com.qx.message.rtc.*
import org.json.JSONObject
import org.webrtc.*

class QXCallConn(var roomType:String,
                 var rtcCallMedia: ICallMedia,
                 var connectListener: IConnectListener
) : IRtcCallServer {

    private val TAG =  "QXCallConn"
    private var host: String = "https://qx-webrtc-beta.aitdcoin.com"
    var socketId: String = QXIMClient.instance.getCurUserId()!!
    var roomId: String = ""
    var token: String = ""

    //Peer集合
    private val peers = mutableMapOf<String, Peer>()

    fun connect() {
        host = getSocketHost()
    }

    private fun getSocketHost(): String {
        val sb = StringBuilder(host)
//            sb.append(host)
                .append("/api/webrtc/socket.io/")
                .append("?token=")
                .append(token)
                .append("&roomId=")
                .append(roomId)
                .append("&roomType=")
                .append(roomType)
                .toString()
        Log.d(TAG, "sockethost:$socketId,$sb")
        return sb
    }

    fun join(roomId: String, otherSocketId: String) {
        val join = RTCJoin()
        join.roomId = roomId
        join.roomType = roomType
        QXIMClient.instance.rtcJoin(join, object : QXIMClient.ResultCallback<String>() {
            override fun onSuccess(data: String) {
                // 创建peer 发offer
//                getOrCreateRtcConnect(otherSocketId)
            }

            override fun onFailed(error: QXError) {
            }

        })
    }

    fun joined(roomId: String) {
        val join = RTCJoined()
        join.roomId = roomId
        QXIMClient.instance.rtcJoined(join, object : QXIMClient.ResultCallback<String>() {
            override fun onSuccess(data: String) {

            }

            override fun onFailed(error: QXError) {
            }

        })
    }


    fun createAnswer(socketId: String, sdp: String) {
        QLogTrace.instance.log("createAnswer ${getTime(System.currentTimeMillis())}")
        val pc = getRtcPeer(socketId)
        QLog.e(TAG, "createAnswer socketId:$socketId")
        if (pc != null && pc.pc != null) {
            val sdp = SessionDescription(
                    SessionDescription.Type.fromCanonicalForm("offer"), sdp)
            //设置远端setRemoteDescription
            pc!!.pc.setRemoteDescription(pc, sdp)
            //设置answer
            pc.pc.createAnswer(pc, rtcCallMedia.getSdp())
        }
    }

    fun receiveAnswer(socketId: String, sdp: String) {
        QLogTrace.instance.log("receiveAnswer  ${getTime(System.currentTimeMillis())}")
        val pc = getRtcPeer(socketId)
        QLog.e(TAG, "receiveAnswer socketId:$socketId")
        if (pc != null && pc.pc != null) {
            val sdp = SessionDescription(
                    SessionDescription.Type.fromCanonicalForm("answer"),
                    sdp
            )
            //设置远端setRemoteDescription
            pc!!.pc.setRemoteDescription(pc, sdp)
        }
    }

    fun updateCandidate(fromId: String, candidate: RTCSdp) {
        QLog.e(TAG,  "updateCandidate socketId:$socketId")
        val pc = getOrCreateRtcConnect(fromId)
        if (pc != null && pc.pc != null) {
            //获取candidate
            val iceCandidate = IceCandidate(
                    candidate.sdpMid,  //描述协议id
                    candidate.sdpMLineIndex!!.toInt(),  //描述协议的行索引
                    candidate.sdp!! //描述协议
            )
            //添加远端设备路由描述
            pc!!.pc.addIceCandidate(iceCandidate)
        }
    }


    fun getRtcPeer(socketId: String): Peer? {
        QLog.e(TAG,  "getRtcPeer socketId:$socketId")
        var pc = peers[socketId]
        if (pc == null) {
            //构建RTCPeerConnection PeerConnection相关回调进入Peer中
            pc = Peer(
                    socketId,
                    rtcCallMedia.getFacstory(),
                    rtcCallMedia.getRtcConfig(),
                    this@QXCallConn
            )
            //设置本地数据流
//            pc.pc.addTrack(rtcCallMedia.getVideoTrack())
//            pc.pc.addTrack(rtcCallMedia.getAudioTrack())
            if (pc.pc != null && rtcCallMedia != null && rtcCallMedia.getLocalMediaStream() != null) {
                QLog.e(TAG, "getRtcPeer socketId:$socketId addStream")
                pc.pc.addStream(rtcCallMedia.getLocalMediaStream())
                //保存peer连接
                peers.put(socketId, pc)
            }
        }
        return pc
    }

    fun getOrCreateRtcConnect(socketId: String): Peer? {
        QLog.e(TAG, "getOrCreateRtcConnect socketId:$socketId")
        var pc = peers[socketId]
        if (pc == null) {
            //构建RTCPeerConnection PeerConnection相关回调进入Peer中
            pc = Peer(
                    socketId,
                    rtcCallMedia.getFacstory(),
                    rtcCallMedia.getRtcConfig(),
                    this@QXCallConn
            )
            //设置本地数据流
//            pc.pc.addTrack(rtcCallMedia.getVideoTrack())
//            pc.pc.addTrack(rtcCallMedia.getAudioTrack())
            if (pc.pc != null && rtcCallMedia != null && rtcCallMedia.getLocalMediaStream() != null) {
                QLog.e(TAG, "getOrCreateRtcConnect socketId:$socketId addStream")
                pc.pc.addStream(rtcCallMedia.getLocalMediaStream())
                //保存peer连接
                peers.put(socketId, pc)
            }
        }

        return pc
    }

    fun createOffer(pc :Peer) {
        if (pc != null && pc!!.pc != null) {
            QLogTrace.instance.log("send offer  ${getTime(System.currentTimeMillis())}")
            pc!!.pc.createOffer(pc, rtcCallMedia.getSdp())
        }
    }

    fun exitRoom(state: QXCallState) {
        try {
            QLog.e(TAG, ">>> Emitter exitRoom socketId:${socketId} roomId:${roomId}")
            for (pc in peers.values) {
                pc.pc.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socketId = ""
            roomId = ""
            peers.clear()
            //通知UI清空远端摄像头
            connectListener.disconnect(state)
        }
    }

    override fun sendMessageFromPeer(event: String, message: JSONObject) {
        QLog.e(TAG,  "sendMessage event:$event,message:${message.toString()}")
        if (event.equals("offer", true)) {
            val offer = RTCOffer()
            offer.from = message.optString("from")
            offer.to = message.optString("to")
            offer.roomId = message.optString("room")
            offer.sdp = message.optString("sdp")
            QXIMClient.instance.rtcOffer(offer, object : QXIMClient.ResultCallback<String>() {
                override fun onSuccess(data: String) {

                }

                override fun onFailed(error: QXError) {
                }

            })
        } else if (event.equals("answer", true)) {
            val offer = RTCOffer()
            offer.from = message.optString("from")
            offer.to = message.optString("to")
            offer.roomId = message.optString("room")
            offer.sdp = message.optString("sdp")
            QXIMClient.instance.rtcAnswer(offer, object : QXIMClient.ResultCallback<String>() {
                override fun onSuccess(data: String) {

                }

                override fun onFailed(error: QXError) {
                }

            })
        } else if (event.equals("candidate", true)) {
            val candidate = RTCCandidate()
            candidate.from = message.optString("from")
            candidate.to = message.optString("to")
            candidate.roomId = message.optString("room")
            val obj = message.optJSONObject("candidate")
            val sdp = RTCSdp()
            candidate.candidate = sdp
            sdp.sdp = obj.optString("sdp")
            sdp.sdpMid = obj.optString("sdpMid")
            sdp.sdpMLineIndex = obj.optString("sdpMLineIndex")
            QLog.e(TAG, "sendMessage event:$event,candidate:${candidate}")
            QXIMClient.instance.rtcCandidate(candidate, object : QXIMClient.ResultCallback<String>() {
                override fun onSuccess(data: String) {

                }

                override fun onFailed(error: QXError) {
                }

            })
        }
    }

    override fun getConnSocketId(): String? {
        return socketId
    }

    override fun getConnRoomId(): String? {
        return roomId
    }

    override fun onAddRemoteStream(peerId: String?, mediaStream: MediaStream) {
        rtcCallMedia.renderMediaStream(peerId!!, mediaStream)
    }

    override fun onAddRemoteStream(peerId: String?, videoTrack: VideoTrack?) {
        rtcCallMedia.renderVideoTrack(peerId, videoTrack)
    }

    override fun onAddRemoteStream(peerId: String?, audioTrac: AudioTrack?) {
        rtcCallMedia.renderAudioTrack(peerId, audioTrac)
    }

    override fun onRemoveRemoteStream(peerId: String?) {
//        rtcCallMedia.onRemoveRemoteStream(peerId)
    }

    override fun onIceConnectChange(iceState: PeerConnection.IceConnectionState) {
        QLog.e(TAG,"onIceConnectChange ${iceState.name}")
        if (iceState == PeerConnection.IceConnectionState.DISCONNECTED) {
            connectListener.disconnect(QXCallState.DISCONNECTED)
        } else if (iceState == PeerConnection.IceConnectionState.FAILED) {
//            connectListener.error(QXCallState.ERROR_NET)
        } else if (iceState == PeerConnection.IceConnectionState.CONNECTED) {
            connectListener.connected()
        }
    }

    fun release() {
    }
}