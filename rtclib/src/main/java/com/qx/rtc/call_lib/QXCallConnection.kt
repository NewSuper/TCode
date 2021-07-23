package com.qx.rtc.call_lib;

import android.util.Log
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import io.socket.parser.Packet
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import java.io.IOException
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import kotlin.jvm.Throws

class QXCallConnection(
    var rtcCallMedia: ICallMedia,
    var connectListener: IConnectListener
) : IRtcCallServer {

    private val TAG = QXCallConnection::class.java.simpleName
//    private var host: String = "https://192.168.3.233:443"
//    var host: String = ""
   private var host: String = "https://qx-webrtc-beta.aitdcoin.com"

    //socket.io信令交互
    private lateinit var client: Socket

    //本地socket id
    var socketId: String = ""
    var roomId: String = ""
    var roomType: String = "private"
    // 测试token
//    var token: String = "796c05da96dc4d93a8391c9721bb9974"
    // 线上token
//    var token: String = "75895474f7574bbc940a5f3e8e51108c"
    var token: String = ""

    //Peer集合
    private val peers = mutableMapOf<String, Peer>()

    fun connect() {
        host = getSocketHost()
        createSocket()
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
        Log.d(TAG, "sockethost:" + sb.toString())
        return sb.toString()
    }


    //创建信令服务器及监听
    private fun createSocket() {
        try {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(LoggingInterceptor())
                .hostnameVerifier { _, _ -> true }
                .sslSocketFactory(getSSLSocketFactory(), TrustAllCerts())
                .build()
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient)
            IO.setDefaultOkHttpCallFactory(okHttpClient)
            val opts = IO.Options()
            opts.callFactory = okHttpClient
            opts.webSocketFactory = okHttpClient
            opts.path = "/api/webrtc/socket.io"
            opts.transports = arrayOf(WebSocket.NAME)
            client = IO.socket(host, opts)
            client = Socket(client.io(), "/", opts)
            //设置消息监听
            //created [id,room,peers]
            client.on("join", joinListener)
            //joined [id,room]
            client.on("joined", joinedListener)
            //offer [from,to,room,sdp]
            client.on("offer", offerListener)
            //answer [from,to,room,sdp]
            client.on("answer", answerListener)
            //candidate [from,to,room,candidate[sdpMid,sdpMLineIndex,sdp]]
            client.on("candidate", candidateListener)
            //exit [from,room]
            client.on("exit", exitListener)
            client.on(
                Socket.EVENT_CONNECTING,
                Emitter.Listener {
                    connectListener.connecting()
                    Log.v(TAG, ">>> Socket EVENT_CONNECTING ") })
            client.on(
                Socket.EVENT_CONNECT,
                Emitter.Listener {
                    connectListener.connected()
                    Log.v(TAG, ">>>Socket  EVENT_CONNECT ") })
            client.on(Socket.EVENT_PING, Emitter.Listener { Log.v(TAG, ">>> Socket EVENT_PING ") })
            client.on(
                Socket.EVENT_MESSAGE,
                Emitter.Listener { Log.v(TAG, ">>> Socket EVENT_MESSAGE ") })
            client.io()
                .on(Manager.EVENT_PING, Emitter.Listener { Log.v(TAG, ">>> Manager EVENT_PING ") })
            client.io()
                .on(Manager.EVENT_PONG, Emitter.Listener { Log.v(TAG, ">>> Manager EVENT_PONG ") })
            client.io().on(
                Manager.EVENT_RECONNECTING,
                Emitter.Listener { Log.v(TAG, ">>> Manager EVENT_RECONNECTING ") })
            client.io().on(Manager.EVENT_PACKET, Emitter.Listener { args ->
                val arg = if (args.isNotEmpty()) args[0] else ""
                if (arg is Packet<*>) {
                    val packet = arg
                    Log.v(TAG, ">>>Manager EVENT_PACKET " + packet.query + "," + packet.data)
                }
            })
            client.on(
                Socket.EVENT_ERROR,
                Emitter.Listener { args -> {
                    stopPingTime()
                    connectListener.error(QXCallState.ERROR_NET)
                    Log.v(TAG, ">>> Socket EVENT_ERROR " + args[0])
                } })
            client.on(
                Socket.EVENT_CONNECT_TIMEOUT,
                Emitter.Listener { Log.v(TAG, ">>> EVENT_CONNECT_TIMEOUT ") })
            client.io()
                .on(Manager.EVENT_OPEN, Emitter.Listener { Log.v(TAG, ">>> Manager EVENT_OPEN ") })
            client.io().on(
                Manager.EVENT_ERROR,
                Emitter.Listener { args ->
                    connectListener.error(QXCallState.ERROR_NET)

                    Log.v(TAG, ">>> Manager EVENT_ERROR  " + args[0])
                 })
            client.io().on(
                Manager.EVENT_CLOSE,
                Emitter.Listener { Log.v(TAG, ">>> Manager EVENT_CLOSE ") })
            client.on(
                Socket.EVENT_MESSAGE,
                Emitter.Listener { Log.v(TAG, ">>> Socket EVENT_MESSAGE ") })
            client.on(
                Socket.EVENT_DISCONNECT,
                Emitter.Listener {
                    Log.v(TAG, ">>> Socket  EVENT_DISCONNECT ")
                    connectListener.disconnect(QXCallState.DISCONNECTED)
                    stopPingTime()
                    })
            //开始连接
            client.connect()
        } catch (e: Exception) {
            Log.d(TAG, ">>>>>> socket 链接失败")
            e.printStackTrace()
        }
    }

    private val time = Timer()
    private val timerTask = object : TimerTask() {
        override fun run() {
            ping()
        }
    }

    private fun pingTimeInterval() {
        time.schedule(timerTask,10*1000,45*1000)
    }

    private fun stopPingTime() {
        time.cancel()
        timerTask.cancel()
    }

    private fun ping() {
//        sendMessage("t",JSONObject())
    }


    /**
     * UI操作相关
     */
    //创建并加入
    fun createAndJoinRoom(roomId: String?) {
        //构建信令数据并发送
        Log.v(TAG, ">>> createAndJoinRoom ")
        try {
            val message = JSONObject()
            message.put("room", roomId)
            //向信令服务器发送信令
            sendMessage("join", message)
            pingTimeInterval()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //退出room
    fun exitRoom() {
        //信令服务器发送 exit [from room]
        try {
            val message = JSONObject()
            message.put("from", socketId)
            message.put("roomId", roomId)
            Log.v(TAG, ">>> Emitter exitRoom socketId:${socketId} roomId:${roomId}")

                //向信令服务器发送信令
                sendMessage("exit", message)
                //循环遍历 peer关闭
                for (pc in peers.values) {
                    pc.pc.close()
                }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            //数据重置
            socketId = ""
            roomId = ""
            peers.clear()
            //通知UI清空远端摄像头
            Log.d(TAG, ">>> 通知UI清空远端摄像头")
            connectListener.disconnect(QXCallState.EXIT)
        }
    }

    /**
     * WebRtc相关
     */
    //构建webRtc连接并返回
    private fun getOrCreateRtcConnect(socketId: String): Peer? {
        var pc = peers[socketId]
        if (pc == null) {
            //构建RTCPeerConnection PeerConnection相关回调进入Peer中
            pc = Peer(
                    socketId,
                    rtcCallMedia.getFacstory(),
                    rtcCallMedia.getRtcConfig(),
                    this@QXCallConnection
            )
            //设置本地数据流
//            pc.pc.addTrack(rtcCallMedia.getVideoTrack())
//            pc.pc.addTrack(rtcCallMedia.getAudioTrack())
            pc.pc.addStream(rtcCallMedia.getLocalMediaStream())
            //保存peer连接
            peers.put(socketId, pc)
        }
        return pc
    }

    /**
     * 信令服务器处理相关
     */
    //created [id,room,peers]
    private val joinListener = Emitter.Listener { args ->
        val data = args[0] as JSONObject
        Log.d(TAG, "Emitter craete and join:$data")
        try {
            if (data.getInt("code") == 2) {
//                connectListener.error(data.getString("message"))
            } else {
                val result = data.getJSONObject("data")
                //设置socket id
                socketId = result.getString("socketId")
                //设置room id
                roomId = result.getString("roomId")
                //获取peer数据
                val peers = result.getJSONArray("peers")
                if (peers.length() > 0) {
                    connectListener.joined()
                } else {
                    connectListener.join()
                }
                //根据回应peers 循环创建WebRtcPeerConnection，创建成功后发送offer消息 [from,to,room,sdp]
                for (i in 0 until peers.length()) {
                    val otherSocketId = peers.getString(0)
                    Log.d(TAG, "循环创建WebRtcPeerConnection:$otherSocketId")
                    //创建WebRtcPeerConnection
                    val pc = getOrCreateRtcConnect(otherSocketId)
                    //设置offer
                    pc!!.pc.createOffer(pc, rtcCallMedia.getSdp())
                }
            }

        } catch (e: java.lang.Exception) {
            e.printStackTrace()

        }
    }

    //joined [id,room]
    private val joinedListener = Emitter.Listener { args ->
        val `object` = args[0] as JSONObject
        try {
            val data = `object`.getJSONObject("data") as JSONObject
            Log.d(TAG, "Emitter joined:$data")
            //获取新加入socketId
            val fromId = data.getString("socketId")
            //构建pcconnection
            craeteRtcConnect(fromId)
            connectListener.joined()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun craeteRtcConnect(socketId: String) {
        try {
            //获取新加入socketId
            //构建pcconnection
            getOrCreateRtcConnect(socketId)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    //offer [from,to,room,sdp]
    private val offerListener = Emitter.Listener { args ->
        val `object` = args[0] as JSONObject
        try {
            val data = `object`.getJSONObject("data") as JSONObject
            Log.d(TAG, "Emitter offer:$data")
            //获取id
            val fromId = data.getString("from")
            //获取peer
            val pc = getOrCreateRtcConnect(fromId)
            //构建RTCSessionDescription参数
            val sdp = SessionDescription(
                SessionDescription.Type.fromCanonicalForm("offer"),
                data.getString("sdp")
            )
            //设置远端setRemoteDescription
            pc!!.pc.setRemoteDescription(pc, sdp)
            //设置answer
            pc.pc.createAnswer(pc, rtcCallMedia.getSdp())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //answer [from,to,room,sdp]
    private val answerListener = Emitter.Listener { args ->
        val `object` = args[0] as JSONObject
        try {
            val data = `object`.getJSONObject("data") as JSONObject
            Log.d(TAG, "Emitter answer:$data")
            //获取id
            val fromId = data.getString("from")
            //获取peer
            val pc = getOrCreateRtcConnect(fromId)
            //构建RTCSessionDescription参数
            val sdp = SessionDescription(
                SessionDescription.Type.fromCanonicalForm("answer"),
                data.getString("sdp")
            )
            //设置远端setRemoteDescription
            pc!!.pc.setRemoteDescription(pc, sdp)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //candidate [from,to,room,candidate[sdpMid,sdpMLineIndex,sdp]]
    private val candidateListener = Emitter.Listener { args ->
        val `object` = args[0] as JSONObject
        try {
            val data = `object`.getJSONObject("data") as JSONObject
            Log.d(TAG, "Emitter candidate:$data")
            //获取id
            val fromId = data.getString("from")
            //获取peer
            val pc = getOrCreateRtcConnect(fromId)
            //获取candidate
            val candidate = data.getJSONObject("candidate")
            val iceCandidate = IceCandidate(
                candidate.getString("sdpMid"),  //描述协议id
                candidate.getInt("sdpMLineIndex"),  //描述协议的行索引
                candidate.getString("sdp") //描述协议
            )

            //添加远端设备路由描述
            pc!!.pc.addIceCandidate(iceCandidate)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //exit [from,room]
    private val exitListener = Emitter.Listener { args ->
        val `object` = args[0] as JSONObject
        try {
            val data = `object`.getJSONObject("data") as JSONObject
            Log.d(TAG, "Emitter exit:$data")
            //获取id
            val fromId = data.getString("roomId")
            //判断是否为当前连接
            val pc = peers?.get(fromId)
            if (pc != null) {
                //peer关闭
                getOrCreateRtcConnect(fromId)!!.pc.close()
                //删除peer对象
                peers.remove(fromId)
            }
            connectListener.disconnect(QXCallState.EXIT)
            Log.d(TAG, "Emitter exit:$fromId done")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    /**
     * 信令服务器发送消息
     */
    fun sendMessage(event: String?, message: JSONObject?) {
        client.emit(event, message)
    }

    //返回SSLSocketFactory 用于ssl连接
    fun getSSLSocketFactory(): SSLSocketFactory? {
        var ssfFactory: SSLSocketFactory? = null
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, arrayOf<TrustManager>(TrustAllCerts()), SecureRandom())
            ssfFactory = sc.socketFactory
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return ssfFactory
    }

    internal class LoggingInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val t1 = System.nanoTime()
            Log.v(
                "LoggingInterceptor", String.format(
                    "Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()
                )
            )
            val response = chain.proceed(request)
            val t2 = System.nanoTime()
            Log.v(
                "LoggingInterceptor", String.format(
                    "Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6, response.headers()
                )
            )
            return response
        }
    }


    override fun sendMessageFromPeer(event: String, message: JSONObject) {
        sendMessage(event, message)
    }

    override fun getConnSocketId(): String? {
        return socketId
    }

    override fun getConnRoomId(): String? {
        return roomId
    }

    override fun onAddRemoteStream(peerId: String?, mediaStream: MediaStream) {
        rtcCallMedia.renderMediaStream(peerId!!,mediaStream)
    }

    override fun onAddRemoteStream(peerId: String?, videoTrack: VideoTrack?) {
        rtcCallMedia.renderVideoTrack(peerId,videoTrack)
    }

    override fun onAddRemoteStream(peerId: String?, audioTrac: AudioTrack?) {
        rtcCallMedia.renderAudioTrack(peerId,audioTrac)
    }

    override fun onRemoveRemoteStream(peerId: String?) {
//        rtcCallMedia.onRemoveRemoteStream(peerId)
    }

    override fun onIceConnectChange(iceState: PeerConnection.IceConnectionState) {

    }

    fun release() {
        client.disconnect()
    }

}