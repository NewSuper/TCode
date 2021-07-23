package com.qx.rtc.call_lib

import android.content.Context
import android.graphics.Point
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import com.qx.imlib.qlog.QLog
import com.qx.imlib.utils.SystemUtil
import com.qx.rtc.call_lib.Const.BACK_FACING
import com.qx.rtc.call_lib.Const.FONT_FACTING
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback
import org.webrtc.audio.LegacyAudioDeviceModule
import org.webrtc.voiceengine.WebRtcAudioManager
import org.webrtc.voiceengine.WebRtcAudioRecord
import org.webrtc.voiceengine.WebRtcAudioRecord.WebRtcAudioRecordErrorCallback
import org.webrtc.voiceengine.WebRtcAudioTrack
import org.webrtc.voiceengine.WebRtcAudioUtils
import java.lang.Exception
import java.util.*

class QXCallMedia(
        var context: Context,
        var eglBase: EglBase,
        var mediaType: QXCallMediaType
) : ICallMedia {
    private val TAG = QXCallMedia::class.java.simpleName

    //IceServer集合 用于构建PeerConnection
    private val iceServers = LinkedList<IceServer>()

    //PeerConnectionFactory工厂类
    private var factory: PeerConnectionFactory? = null

    //Peer集合
    private val peers = HashMap<String, Peer>()

    //PeerConnectFactory构建参数
    private var pcParams: PeerConnectionParameters? = null

    //PeerConnect构建参数
    private var rtcConfig: RTCConfiguration? = null

    //PeerConnect 音频约束
    private var audioConstraints: MediaConstraints = MediaConstraints()

    //PeerConnect sdp约束
    private var sdpMediaConstraints: MediaConstraints = MediaConstraints()

    private var localStream: MediaStream? = null

    //本地Video视频资源
    private var localVideoSource: VideoSource? = null

    //视频Track
    private var localVideoTrack: VideoTrack? = null

    //音频Track
    private var localAudioTrack: AudioTrack? = null
    private var audioSource: AudioSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    //本地摄像头视频捕获
    private var cameraVideoCapturer: CameraVideoCapturer? = null
    var localSurfaceViewRenderer: SurfaceView? = null
    var remoteSurfaceViewRenderer: SurfaceView? = null

    private val remoteViews: HashMap<String?, View?>? = null
    private var audioManager: AudioManager? = null

    ////webRtc定义常量////
    private val AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation"
    private val AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"
    private val AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter"
    private val AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression"
    private val VIDEO_FLEXFEC_FIELDTRIAL = "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/"
    private val VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP9/Enabled/"
    private val DISABLE_WEBRTC_AGC_FIELDTRIAL = "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/"
    private val AUDIO_TRACK_ID = "ARDAMSa0"
    private val VIDEO_TRACK_ID = "ARDAMSv0"
    private var cameraType: Int = FONT_FACTING

    private val mHandler = Handler(Looper.getMainLooper())

    init {
        createIceServers()
    }

    fun create() {
        createPeerConnectionParameters()
        createRtcConfig()
        createMediaConstraints()
        createPeerConnectionFactory()
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.setStreamVolume(
                AudioManager.STREAM_VOICE_CALL,
                audioManager!!.getStreamVolume(AudioManager.STREAM_SYSTEM),
                AudioManager.STREAM_VOICE_CALL
        )
        createLocalStream()
    }

    fun mute(mute: Boolean) {
        localAudioTrack?.setEnabled(mute)
    }

    fun switchAudio() {
        localVideoTrack?.setEnabled(false)
    }

    fun handsFree(handsFree: Boolean) {
        if (handsFree) {
            audioManager?.isSpeakerphoneOn = true
            audioManager?.setStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    audioManager!!.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                    AudioManager.STREAM_VOICE_CALL
            )
        } else {
            audioManager?.isSpeakerphoneOn = false
            audioManager?.setStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    audioManager!!.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                    AudioManager.STREAM_VOICE_CALL
            )
        }
    }

    //创建IceServers参数
    private fun createIceServers() {
        val defaultList = mutableListOf(
                "stun:stun.xten.com",
                //"stun:stun.l.google.com:19302",
               // "stun:23.21.150.121"
        )
        val rtcServerConfig = QXCallManager.rtcServerConfig
        if (rtcServerConfig != null && rtcServerConfig!!.iceServers != null) {
            val remoteUrls = mutableListOf<String>()
            for (host in QXCallManager.rtcServerConfig?.iceServers!!) {
              //  val stun = "stun:$host:3478"
                val turnudp = "turn:$host:5349?transport=udp"
                val turntcp = "turn:$host:5349?transport=tcp"
              //  remoteUrls.add(stun)
                remoteUrls.add(turnudp)
                remoteUrls.add(turntcp)
                val iceServer = IceServer
                        .builder(remoteUrls)
                        .setUsername("admin")
                        .setPassword("878ECbff5DEC472A924c418D98e07f7B")
                        .createIceServer()
                iceServers.add(iceServer)
            }
        } else {
            iceServers.add(IceServer.builder(defaultList).createIceServer())
        }
    }

    //创建配置参数
    private fun createPeerConnectionParameters() {
        //获取webRtc 音视频配置参数
        val displaySize = Point()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getSize(displaySize)
        displaySize.x = 640
        displaySize.y = 480
        pcParams = PeerConnectionParameters(
                mediaType == QXCallMediaType.VIDEO, false,
                false, displaySize.x, displaySize.y, 30,
                0, "vp9",
                true, false, 0, "OPUS",
                false, false, false, true, true, true,
                false, true, true, false
        )
    }

    //创建RTCConfiguration参数
    private fun createRtcConfig() {
        rtcConfig = RTCConfiguration(iceServers)
    }

    //创建PeerConnection工厂类
    private fun createPeerConnectionFactory() {
        try {
            //创建webRtc连接工厂类
            val encoderFactory: VideoEncoderFactory
            val decoderFactory: VideoDecoderFactory
            val enableH264HighProfile = "H264 High" == pcParams!!.videoCodec
            //音频模式
            val adm: AudioDeviceModule =
                    if (pcParams!!.useLegacyAudioDevice) createLegacyAudioDevice() else createJavaAudioDevice()
            //编解码模式【硬件加速，软编码】
            var rtcServerConfig = QXCallManager.rtcServerConfig
            if (!rtcServerConfig?.whiteDeveice.isNullOrEmpty()) {
                val whiteDevices = rtcServerConfig!!.whiteDeveice.split(";")
                val curDevice = SystemUtil.getSystemModel()
                val device = whiteDevices.find { it.equals(curDevice, true) }
                if (device == null) {
                    encoderFactory = SoftwareVideoEncoderFactory()
                    decoderFactory = SoftwareVideoDecoderFactory()
                } else {
                    encoderFactory = DefaultVideoEncoderFactory(
                            eglBase.eglBaseContext, true /* enableIntelVp8Encoder */, enableH264HighProfile
                    )
                    decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
                }
            } else {
                encoderFactory = SoftwareVideoEncoderFactory()
                decoderFactory = SoftwareVideoDecoderFactory()
            }
            var fieldTrials = ""
            if (pcParams!!.videoFlexfecEnabled) {
                fieldTrials += VIDEO_FLEXFEC_FIELDTRIAL
            }
            fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL
            if (pcParams!!.disableWebRtcAGCAndHPF) {
                fieldTrials += DISABLE_WEBRTC_AGC_FIELDTRIAL
            }

            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context)
                            .setFieldTrials(fieldTrials)
                            .setEnableInternalTracer(true)
                            .setInjectableLogger(webrtcLog, Logging.Severity.LS_VERBOSE)
                            .createInitializationOptions()
            )
            //构建PeerConnectionFactory
            val options = PeerConnectionFactory.Options()
            factory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .setAudioDeviceModule(adm)
                    .setVideoEncoderFactory(encoderFactory)
                    .setVideoDecoderFactory(decoderFactory)
                    .createPeerConnectionFactory()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private val webrtcLog = Loggable { p0, p1, p2 ->
//        QLog.d(TAG, " p0:$p0, p1:$p1 ,p2:$p2")
    }

    //创建音频模式LegacyAudioDevice
    private fun createLegacyAudioDevice(): AudioDeviceModule {
        // Enable/disable OpenSL ES playback.
        if (!pcParams!!.useOpenSLES) {
            QLog.d(TAG, "Disable OpenSL ES audio even if device supports it")
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true /* enable */)
        } else {
            QLog.d(TAG, "Allow OpenSL ES audio if device supports it")
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false)
        }
        if (pcParams!!.disableBuiltInAEC) {
            QLog.d(TAG, "Disable built-in AEC even if device supports it")
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true)
        } else {
            QLog.d(TAG, "Enable built-in AEC if device supports it")
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false)
        }
        if (pcParams!!.disableBuiltInNS) {
            QLog.d(TAG, "Disable built-in NS even if device supports it")
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true)
        } else {
            QLog.d(TAG, "Enable built-in NS if device supports it")
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false)
        }

        // Set audio record error callbacks.
        WebRtcAudioRecord.setErrorCallback(object : WebRtcAudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                QLog.e(TAG, "onWebRtcAudioRecordInitError: $errorMessage")
            }

            override fun onWebRtcAudioRecordStartError(
                    errorCode: WebRtcAudioRecord.AudioRecordStartErrorCode, errorMessage: String
            ) {
                QLog.e(TAG, "onWebRtcAudioRecordStartError: $errorCode. $errorMessage")
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                QLog.e(TAG, "onWebRtcAudioRecordError: $errorMessage")
            }
        })
        WebRtcAudioTrack.setErrorCallback(object : WebRtcAudioTrack.ErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                QLog.e(TAG, "onWebRtcAudioTrackInitError: $errorMessage")
            }

            override fun onWebRtcAudioTrackStartError(
                    errorCode: WebRtcAudioTrack.AudioTrackStartErrorCode, errorMessage: String
            ) {
                QLog.e(TAG, "onWebRtcAudioTrackStartError: $errorCode. $errorMessage")
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                QLog.e(TAG, "onWebRtcAudioTrackError: $errorMessage")
            }
        })
        return LegacyAudioDeviceModule()
    }

    //创建音频模式JavaAudioDevice
    private fun createJavaAudioDevice(): AudioDeviceModule {
        // Enable/disable OpenSL ES playback.
        if (!pcParams!!.useOpenSLES) {
            QLog.w(TAG, "External OpenSLES ADM not implemented yet.")
            // TODO(magjed): Add support for external OpenSLES ADM.
        }

        // Set audio record error callbacks.
        val audioRecordErrorCallback: AudioRecordErrorCallback = object : AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                QLog.e(TAG, "onWebRtcAudioRecordInitError: $errorMessage")
            }

            override fun onWebRtcAudioRecordStartError(
                    errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode, errorMessage: String
            ) {
                QLog.e(TAG, "onWebRtcAudioRecordStartError: $errorCode. $errorMessage")
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                QLog.e(TAG, "onWebRtcAudioRecordError: $errorMessage")
            }
        }
        val audioTrackErrorCallback: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                QLog.e(TAG, "onWebRtcAudioTrackInitError: $errorMessage")
            }

            override fun onWebRtcAudioTrackStartError(
                    errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode, errorMessage: String
            ) {
                QLog.e(TAG, "onWebRtcAudioTrackStartError: $errorCode. $errorMessage")
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                QLog.e(TAG, "onWebRtcAudioTrackError: $errorMessage")
            }
        }
        return JavaAudioDeviceModule.builder(context) //.setSamplesReadyCallback(saveRecordedAudioToFile)
                .setUseHardwareAcousticEchoCanceler(!pcParams!!.disableBuiltInAEC)
                .setUseHardwareNoiseSuppressor(!pcParams!!.disableBuiltInNS)
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .createAudioDeviceModule()
    }

    /**
     * WebRtc 音视频相关辅助函数
     */
    //创建Media及Sdp约束
    private fun createMediaConstraints() {
        // 音频约束
        // added for audio performance measurements
        if (pcParams!!.noAudioProcessing) {
            QLog.d(TAG, "Disabling audio processing")
            audioConstraints.mandatory.add(
                    MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true")
            )
            audioConstraints.mandatory.add(
                    MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false")
            )
            audioConstraints.mandatory.add(
                    MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false")
            )
            audioConstraints.mandatory.add(
                    MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true")
            )
        }

        //SDP约束 createOffer  createAnswer
        sdpMediaConstraints.mandatory.add(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
        )
        sdpMediaConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(
                        "OfferToReceiveVideo", "true"
                )
        )
        sdpMediaConstraints.optional.add(
                MediaConstraints.KeyValuePair(
                        "DtlsSrtpKeyAgreement",
                        "true"
                )
        )
    }

    fun switchCamera() {
        cameraVideoCapturer?.switchCamera(null)
        cameraType = if (cameraType == BACK_FACING) FONT_FACTING else BACK_FACING
        if (localSurfaceViewRenderer != null) {
            (localSurfaceViewRenderer as SurfaceViewRenderer).setMirror(cameraType == FONT_FACTING)
        }
    }

    fun remoteVideoInfoChange(userId:String?,info:String?) {
        if (!userId.isNullOrEmpty() && !info.isNullOrEmpty()) {
            val obj = JSONObject(info)
            val cameraType = obj.opt("camera")
            if (remoteSurfaceViewRenderer != null) {
                (remoteSurfaceViewRenderer as SurfaceViewRenderer).setMirror(cameraType == FONT_FACTING)
            }
        }

    }

    //启动设备视频并关联本地video
    fun startCamera(type: Int = BACK_FACING) {
        if (localStream!!.videoTracks.size > 0) {
            localStream!!.videoTracks[0].addSink(localSurfaceViewRenderer as SurfaceViewRenderer)
            cameraVideoCapturer?.startCapture(
                    640,
                    480,
                    15
            )
        }
    }

    fun stopCamera() {
        cameraVideoCapturer?.stopCapture()
        if (remoteSurfaceViewRenderer != null) {
            (remoteSurfaceViewRenderer as SurfaceViewRenderer).pauseVideo()
        }
    }

    private fun createCameraVideoCapture(type: Int) {
        var cameraname: String? = ""
        val camera1Enumerator = Camera1Enumerator()
        val deviceNames = camera1Enumerator.deviceNames
        cameraType = type
        if (type == FONT_FACTING) {
            //前置摄像头
            for (deviceName in deviceNames) {
                if (camera1Enumerator.isFrontFacing(deviceName)) {
                    cameraname = deviceName
                }
            }
        } else {
            //后置摄像头
            for (deviceName in deviceNames) {
                if (camera1Enumerator.isBackFacing(deviceName)) {
                    cameraname = deviceName
                }
            }
        }
        cameraVideoCapturer = camera1Enumerator.createCapturer(cameraname, null)
    }


    override fun getSdp(): MediaConstraints? {
        return sdpMediaConstraints
    }

    override fun getFacstory(): PeerConnectionFactory? {
        return factory
    }

    override fun getRtcConfig(): RTCConfiguration? {
        return rtcConfig
    }

    override fun getVideoTrack(): VideoTrack? {
        return localVideoTrack
    }

    override fun getAudioTrack(): AudioTrack? {
        return localAudioTrack
    }

    override fun getLocalMediaStream(): MediaStream? {
        return localStream
    }

    override fun renderVideoTrack(peerId: String?, videoTrack: VideoTrack?) {
        remoteViews?.put(peerId, remoteSurfaceViewRenderer)
        videoTrack!!.addSink(remoteSurfaceViewRenderer!! as VideoSink)
    }

    override fun renderAudioTrack(peerId: String?, audioTrack: AudioTrack?) {
    }

    override fun renderMediaStream(peerId: String, remoteStream: MediaStream) {
        if (remoteStream.videoTracks.size > 0) {
            remoteStream.videoTracks[0].addSink(remoteSurfaceViewRenderer!! as SurfaceViewRenderer)
        }
    }

    fun createView(overlay: Boolean): SurfaceView {
        val surfaceView = SurfaceViewRenderer(context)
        //初始化渲染源
        surfaceView.init(eglBase.eglBaseContext, null)
        //填充模式
        surfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        surfaceView.setZOrderMediaOverlay(overlay)
        surfaceView.setEnableHardwareScaler(true)
        surfaceView.setMirror(true)
        if (!overlay) {
            surfaceView.addFrameListener({
                QLog.e(TAG, "addFrameListener >>>> ")
            }, 1.0F)
        }

        return surfaceView
    }

    /**
     * 创建本地流
     */
    private fun createLocalStream() {
        localStream = factory?.createLocalMediaStream("ARDAMS")
        // 音频
        audioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack(
                AUDIO_TRACK_ID,
                audioSource
        )
//        var max = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
//        max = if (max <= 0) 1 else max
//        val volume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
//        val v = ((volume * volume * 10) / (max * max)).toDouble()
//        localAudioTrack!!.setVolume(v)
        localStream?.addTrack(localAudioTrack)

        // 视频
        if (mediaType == QXCallMediaType.VIDEO) {
            createCameraVideoCapture(FONT_FACTING)
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            localVideoSource = factory?.createVideoSource(cameraVideoCapturer?.isScreencast!!)
            cameraVideoCapturer?.initialize(
                    surfaceTextureHelper,
                    context,
                    localVideoSource?.capturerObserver
            )

            localVideoTrack = factory?.createVideoTrack(
                    VIDEO_TRACK_ID,
                    localVideoSource
            )
            localStream?.addTrack(localVideoTrack)
        }
    }

    fun showLocalView(localView: SurfaceView) {
        this.localSurfaceViewRenderer = localView
        this.localSurfaceViewRenderer?.background = null
    }

    private var isRelease = false
    fun release() {
        try {
            if (isRelease)
                return
            isRelease = true
            localAudioTrack?.setVolume(0.0)
            localAudioTrack?.setEnabled(false)
//            localAudioTrack?.dispose()
//            localAudioTrack = null
            localStream?.dispose()
            localStream = null

            cameraVideoCapturer?.stopCapture()
            cameraVideoCapturer?.dispose()
            cameraVideoCapturer = null
            localSurfaceViewRenderer?.let {
                (it as SurfaceViewRenderer).release()
            }
            localSurfaceViewRenderer = null
            remoteSurfaceViewRenderer?.let {
                (it as SurfaceViewRenderer).release()
            }
            remoteSurfaceViewRenderer = null
            audioManager?.isSpeakerphoneOn = false
            audioManager?.isMicrophoneMute = false
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}