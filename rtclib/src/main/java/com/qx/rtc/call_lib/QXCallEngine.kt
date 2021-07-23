package com.qx.rtc.call_lib

import android.content.Context
import android.view.SurfaceView
import com.qx.imlib.QXIMClient
import com.qx.imlib.qlog.QLog

import com.qx.message.rtc.RTCSdp
import org.webrtc.EglBase

class QXCallEngine(
        var context: Context
) : ICallEngine {
    var mediaType: QXCallMediaType? = null
        set(value) {
            field = value
            initMedia()
        }

    private val eglBase: EglBase = EglBase.create()
    var rtcCallMedia: QXCallMedia? = null

    private fun initMedia() {
        rtcCallMedia = QXCallMedia(context, eglBase, mediaType!!)
        rtcCallMedia?.create()
    }

    private val TAG = QXCallEngine::class.java.simpleName

    private var rtcCallConn: QXCallConn? = null
//    private var rtcCallConn: QXCallConnection? = null

    override fun create(roomType: String, listener: IConnectListener) {
        rtcCallConn = QXCallConn(roomType, rtcCallMedia!!, listener)
//        rtcCallConn = QXCallConnection(rtcCallMedia!!, listener)
    }

    override fun createRendererView(context: Context, overlay: Boolean): SurfaceView {
        return rtcCallMedia?.createView(overlay)!!
    }

    fun connect(token: String, host: String, roomId: String, roomType: String, listener: IConnectListener) {
        create(roomType, listener)
        setUserToken(token)
        setServerHost(host)
        rtcCallConn?.roomId = roomId
        rtcCallConn?.connect()
    }

    fun mute(mute: Boolean) {
        rtcCallMedia?.mute(mute)
    }

    fun handsFree(handsFree: Boolean) {
        rtcCallMedia?.handsFree(handsFree)
    }

    /**
     * 释放RTC相关资源
     */
    override fun destory() {
            QLog.d(TAG, "release WebRTC")
            rtcCallMedia?.release()
            rtcCallConn?.release()
    }

    override fun setLocalVideo(localVideoView: SurfaceView) {
        rtcCallMedia?.showLocalView(localVideoView)
    }

    override fun setRemoteVideo(remoteVideoView: SurfaceView) {
        rtcCallMedia?.remoteSurfaceViewRenderer = remoteVideoView
    }

    override fun startPreview(): Int {
        rtcCallMedia?.startCamera()
        return 1
    }

    override fun stopPreview(): Int {
        rtcCallMedia?.stopCamera()
        return 1
    }

    override fun switchCamera(): Int {
        rtcCallMedia?.switchCamera()
        return 1
    }

    override fun joinRoom(roomId: String) {
        rtcCallConn?.roomId = roomId
        rtcCallConn?.join(roomId, QXIMClient.instance.getCurUserId()!!)
    }

    fun joined(roomId: String) {
        rtcCallConn?.joined(roomId)
    }

    fun switchAudio() {
        rtcCallMedia?.switchAudio()
    }

    override fun leaveRoom(state: QXCallState) {
        rtcCallConn?.exitRoom(state)
    }

    override fun muteLocalAudioStream(mute: Boolean) {
    }

    override fun muteRemoteVideoStream(mute: Boolean) {
    }

    fun setServerHost(host: String) {
//        rtcCallConn?.host = host
    }

    fun setUserToken(userToken: String) {
        rtcCallConn?.token = userToken
    }

    fun createPeer(fromId: String): Peer? {
        return rtcCallConn?.getOrCreateRtcConnect(fromId)
    }

    fun createOffer(peer: Peer) {
        rtcCallConn?.createOffer(peer)
    }

    fun createAnswer(fromId: String, sdp: String) {
        rtcCallConn?.createAnswer(fromId, sdp)
    }

    fun receiveAnswer(fromId: String, sdp: String) {
        rtcCallConn?.receiveAnswer(fromId, sdp)
    }

    fun updateCandidate(fromId: String, sdpDesc: RTCSdp) {
        rtcCallConn?.updateCandidate(fromId, sdpDesc)
    }

    fun remoteVideoInfoChange(userId: String?, info: String?) {
        rtcCallMedia?.remoteVideoInfoChange(userId, info)
    }


}