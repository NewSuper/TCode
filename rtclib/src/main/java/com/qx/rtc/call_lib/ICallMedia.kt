package com.qx.rtc.call_lib

import org.webrtc.*

interface ICallMedia {

    fun getSdp() : MediaConstraints?

    fun getFacstory() : PeerConnectionFactory?

    fun getRtcConfig() : PeerConnection.RTCConfiguration?

    fun getVideoTrack() : VideoTrack?
    fun getAudioTrack() : AudioTrack?

    fun getLocalMediaStream() : MediaStream?

    fun renderVideoTrack(peerId:String?,videoTrack: VideoTrack?)
    fun renderAudioTrack(peerId:String?,audioTrack: AudioTrack?)

    fun renderMediaStream(peerId:String,videoTrack: MediaStream)

}