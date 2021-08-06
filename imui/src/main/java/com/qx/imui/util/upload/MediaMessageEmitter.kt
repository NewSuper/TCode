package com.qx.imui.util.upload

import android.content.Context
import android.net.Uri
import com.qx.imlib.QXIMClient
import com.qx.message.Message
import com.qx.message.MessageType
import com.qx.message.QXError

object MediaMessageEmitter {
    interface SendMediaMessageCallback {
        /**
         * 上传中
         * @param progress 进度：0-100
         */
        fun onProgress(progress: Int)

        /**
         * 上传完成
         * @param message 消息体
         */
        fun onUploadCompleted(message: Message?)

        /**
         * 上传失败
         * @param errorCode 错误码
         * @param msg 错误提示
         */
        fun onUploadFailed(errorCode: Int, msg: String, message: Message?)
        /**
         * 插入数据库成功
         */
        fun onAttached(message: Message?)

        /**
         * 发送成功
         */
        fun onSuccess()

        /**
         * 发送失败
         */
        fun onError(error : QXError, message: Message?)

    }

    private var mSendMediaMessageCallback : SendMediaMessageCallback ? = null

    @JvmStatic
    fun send(message: Message?,callback: SendMediaMessageCallback){
        mSendMediaMessageCallback =callback
        QXIMClient.instance.saveMediaMessage(message, sendMessageCallback)
    }

    @JvmStatic
    fun save(context: Context, conversationType: String, targetId: String, messageType: String, uri: Uri, callback: SendMediaMessageCallback) {
        mSendMediaMessageCallback = callback
        //1.先存数据库
        QXIMClient.instance.saveMediaMessage(context, conversationType!!, targetId!!, messageType, uri, sendMessageCallback)
    }

    private var mUIUploadCallback:UploadUtil.UIUploadCallback = object  :UploadUtil.UIUploadCallback{
        override fun onProgress(progress: Int) {
            mSendMediaMessageCallback?.onProgress(progress)
        }

        override fun onCompleted(message: Message) {
            mSendMediaMessageCallback?.onUploadCompleted(message)
            sendOnly(message,sendMessageCallback)
        }

        override fun onFailed(errorCode: Int, errorMsg: String?, message: Message?) {
            if (message!=null){
                message.state = Message.State.STATE_FAILED
                QXIMClient.instance.updateMessageState(message.messageId,Message.State.STATE_FAILED,object :QXIMClient.OperationCallback(){
                    override fun onSuccess() {

                    }

                    override fun onFailed(error: QXError) {

                    }
                })
            }
            mSendMediaMessageCallback?.onUploadFailed(errorCode,errorMsg!!,message)
        }
    }
private fun sendOnly(message: Message,sendMessageCallback:QXIMClient.SendMessageCallback){
    QXIMClient.instance!!.saveOnly(message,sendMessageCallback)
}
fun removeMediaMessageCallback(){
    mSendMediaMessageCallback = null
}
private val sendMessageCallback = object :QXIMClient.SendMessageCallback(){
    override fun onAttached(message: Message?) {
        mSendMediaMessageCallback?.onAttached(message)
        if (message!!.messageType != MessageType.TYPE_STATUS){
            when(message.messageType){
                MessageType.TYPE_AUDIO -> {
                    //保存成功，上传语音消息
                    UploadUtil.uploadAudio(message, mUIUploadCallback)
                }
                MessageType.TYPE_IMAGE -> {
                    //保存成功，上传语音消息
                    UploadUtil.uploadImage(message, mUIUploadCallback)
                }
                MessageType.TYPE_FILE -> {
                    //保存成功，上传语音消息
                    UploadUtil.uploadFile(message, mUIUploadCallback)
                }
                MessageType.TYPE_VIDEO -> {
                    //上传视频
                    UploadUtil.uploadVideo(message, mUIUploadCallback)
                }
                MessageType.TYPE_GEO -> {
                    //上传视频
                    UploadUtil.uploadGeoShot(message, mUIUploadCallback)
                }
            }
        }
    }

    override fun onSuccess() {
        mSendMediaMessageCallback?.onSuccess()
    }


    override fun onError(error: QXError, message: Message?) {
        if(message!= null) {
            QXIMClient.instance.updateMessageState(message.messageId, Message.State.STATE_FAILED, object : QXIMClient.OperationCallback() {
                override fun onSuccess() {

                }

                override fun onFailed(error: QXError) {
                }

            })
        }
        mSendMediaMessageCallback?.onError(error, message)
    }
}












}