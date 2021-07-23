package com.qx.rtc.call_lib;

import android.util.Log;

import com.qx.imlib.qlog.QLogTrace;
import com.qx.imlib.utils.TimeUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * PeerConnection通道封装，包括PeerConnection创建及状态回调
 * Created by chengshaobo on 2018/10/23.
 */

public class Peer implements SdpObserver, PeerConnection.Observer {
    //PeerConnection对象
    private PeerConnection pc;
    //PeerConnection标识
    private String id;
    private IRtcCallServer rtcCallServer;

    private final static String TAG = "SignalCallBack";

    private long startTime;

    //构造函数
    public Peer(String id,
                PeerConnectionFactory factory,
                PeerConnection.RTCConfiguration rtcConfig,
                IRtcCallServer rtcCallServer) {
        Log.d(TAG,"new Peer: " + id );
        this.pc = factory.createPeerConnection(rtcConfig,this);
        this.id = id;
        this.rtcCallServer = rtcCallServer;
        startTime = System.currentTimeMillis();
        QLogTrace.getInstance().log("Peer create: " + TimeUtil.getTime(startTime));
    }

    public PeerConnection getPc() {
        return pc;
    }

    public void setPc(PeerConnection pc) {
        this.pc = pc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**SdpObserver是来回调sdp是否创建(offer,answer)成功，是否设置描述成功(local,remote）的接口**/

    //Create{Offer,Answer}成功回调
    @Override
    public void onCreateSuccess(SessionDescription sdp) {
        String type = sdp.type.canonicalForm();
        Log.d(TAG,"onCreateSuccess " + type);
        //设置本地LocalDescription
        pc.setLocalDescription(Peer.this, sdp);
        //构建信令数据
        try {
            JSONObject message = new JSONObject();
            message.put("from",rtcCallServer.getConnSocketId());
            message.put("to",id);
            message.put("room",rtcCallServer.getConnRoomId());
            message.put("sdp",sdp.description);
            //向信令服务器发送信令
            rtcCallServer.sendMessageFromPeer(type,message);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    //Set{Local,Remote}Description()成功回调
    @Override
    public void onSetSuccess() {

    }

    //Create{Offer,Answer}失败回调
    @Override
    public void onCreateFailure(String s) {

    }

    //Set{Local,Remote}Description()失败回调
    @Override
    public void onSetFailure(String s) {

    }

    /**SdpObserver是来回调sdp是否创建(offer,answer)成功，是否设置描述成功(local,remote）的接口**/
    //信令状态改变时候触发
    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {

    }

    //IceConnectionState连接状态改变时候触发
    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d(TAG,"onIceConnectionChange " + iceConnectionState);
        rtcCallServer.onIceConnectChange(iceConnectionState);
        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED){
            /** ice连接中断处理 **/
            long endTime = System.currentTimeMillis();
            QLogTrace.getInstance().log("ice state: " + iceConnectionState + ": " +  TimeUtil.getTime(endTime) + ",time:"+((endTime- startTime) / 1000)+ "s");
        } else if (iceConnectionState == PeerConnection.IceConnectionState.CHECKING) {
            QLogTrace.getInstance().log("ice state: " + iceConnectionState + ": " + TimeUtil.getTime(System.currentTimeMillis()));
        } else  if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
            long endTime = System.currentTimeMillis();
            QLogTrace.getInstance().log("ice state: " + iceConnectionState +  ": " +TimeUtil.getTime(endTime) + ",time:"+((endTime- startTime) / 1000)+ "s");
        } else  if (iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
            long endTime = System.currentTimeMillis();
            QLogTrace.getInstance().log("ice state: " + iceConnectionState +  ": " + TimeUtil.getTime(endTime) + ",time:"+((endTime- startTime) / 1000)+ "s");
        }
    }

    //IceConnectionState连接接收状态改变
    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.d(TAG,"onIceConnectionReceivingChange " + b);
    }

    //IceConnectionState网络信息获取状态改变
    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG,"onIceGatheringChange " + iceGatheringState);
    }

    //新ice地址被找到触发
    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG,"onIceCandidate from: "+rtcCallServer.getConnSocketId() + ",to:"+id);
        try {
            //构建信令数据
            JSONObject message = new JSONObject();
            message.put("from",rtcCallServer.getConnSocketId());
            message.put("to",id);
            message.put("room",rtcCallServer.getConnRoomId());
            //candidate参数
            JSONObject candidate = new JSONObject();
            candidate.put("sdpMid",iceCandidate.sdpMid);
            candidate.put("sdpMLineIndex",iceCandidate.sdpMLineIndex);
            candidate.put("sdp",iceCandidate.sdp);
            message.put("candidate",candidate);
            Log.d(TAG,"onIceCandidate message "+message);
            //向信令服务器发送信令
            rtcCallServer.sendMessageFromPeer("candidate",message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    //ice地址被移除掉触发
    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    //Peer连接远端音视频数据到达时触发 注：用onTrack回调代替
    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d(TAG,"onAddStream "+ mediaStream.getId());
        if (mediaStream.audioTracks.size() > 0) {
            mediaStream.audioTracks.get(0).setEnabled(true);
        }
        rtcCallServer.onAddRemoteStream(id,mediaStream);
    }

    //Peer连接远端音视频数据移除时触发
    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(TAG,"onRemoveStream "+ mediaStream.getId());
        //移除Peer连接 & 通知监听远端音视频数据到达
    }

    //Peer连接远端开启数据传输通道时触发
    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }

    //通道交互协议需要重新协商时触发
    @Override
    public void onRenegotiationNeeded() {

    }

    //Triggered when a new track is signaled by the remote peer, as a result of setRemoteDescription.
    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

    }

    @Override
    public void onTrack(RtpTransceiver transceiver) {
//        MediaStreamTrack track = transceiver.getReceiver().track();
//        Log.d(TAG,"onTrack "+ track.id());
//        if (track instanceof VideoTrack) {
//            rtcCallServer.onAddRemoteStream(id,(VideoTrack)track);
//        }
    }
}
