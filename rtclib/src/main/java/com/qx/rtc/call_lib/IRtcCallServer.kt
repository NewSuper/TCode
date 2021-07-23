package com.qx.rtc.call_lib

import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.VideoTrack

interface IRtcCallServer {

    fun sendMessageFromPeer(event: String, message: JSONObject)

    fun getConnSocketId(): String?

    fun getConnRoomId(): String?

    fun onAddRemoteStream(peerId: String?, videoTrack: MediaStream)

    //远程音视频流加入 Peer通道
    fun onAddRemoteStream(peerId: String?, videoTrack: VideoTrack?)
    fun onAddRemoteStream(peerId: String?, audioTrac: AudioTrack?)

    //远程音视频流移除 Peer通道销毁
    fun onRemoveRemoteStream(peerId: String?)

    fun onIceConnectChange(iceState: PeerConnection.IceConnectionState)
}