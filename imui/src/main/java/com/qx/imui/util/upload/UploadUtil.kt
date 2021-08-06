package com.qx.imui.util.upload

import android.text.TextUtils
import com.qx.imlib.QXIMClient
import com.qx.imlib.utils.file.BitmapUtil
import com.qx.imlib.utils.file.FileUtil
import com.qx.imui.QXContext
import com.qx.imui.QXIMKit
import com.qx.imui.R
import com.qx.imui.util.UploadCallback
import com.qx.message.*

object UploadUtil {
    interface UIUploadCallback {
        /**
         * 执行进度
         * @param progress 取值范围：1-100
         */
        fun onProgress(progress: Int)

        /**
         * 执行完毕（成功）
         */
        fun onCompleted(message: Message)

        /**
         * 失败
         * @param errorCode 错误码，UI层定义
         * @param errorMsg 错误消息，UI层定义
         */
        fun onFailed(errorCode: Int, errorMsg: String?, message: Message?)
    }

    fun uploadVideo(message: Message,callback:UIUploadCallback?){
        var videoMessage = message.messageContent as VideoMessage
        var bitmap =MediaUtil.getVideoFirstFrame(videoMessage!!.localPath)
        var bitmapPath = BitmapUtil.saveBitmap(bitmap)
        bitmap.recycle()
    if (QXContext.getInstance().uploadProvider == null){
        callback?.onFailed(-1,QXContext.getString(R.string.qx_provider_upload_not_implement),message)
    return
    }
        QXContext.getInstance().uploadProvider.upload(QXIMKit.FileType.TYPE_IMAGE,bitmapPath,object :UploadCallback{
            override fun onProgress(progress: Int) {
                callback?.onProgress(progress)
            }

            override fun onCompleted(url: String?) {
                //拿到上传后的第一帧图片地址
                if (url.isNullOrEmpty()){
                    callback?.onFailed(-1,QXContext.getString(R.string.qx_upload_fail,0,url?:""),message)
                    return
                }
                //删除图片
                FileUtil.deleteFile(bitmapPath)
                //更新视频第一帧url
                QXIMClient.instance.updateVideoHeadUrl(message.messageId,url,object :QXIMClient.OperationCallback(){
                    override fun onSuccess() {

                    }

                    override fun onFailed(error: QXError) {

                    }
                })
                //上传视频
                uploadRealVideo(message,videoMessage.localPath,url,callback)
            }

            override fun onFailed(errorCode: Int, errorMsg: String?) {
                callback?.onFailed(-1, QXContext.getString(R.string.qx_upload_fail, errorCode, errorMsg
                    ?: ""), message)
            }
        })
    }

    private fun uploadRealVideo(message: Message, path: String, headerUrl: String,
                                callback: UIUploadCallback?) {

        if (QXContext.getInstance().uploadProvider == null) {
            callback?.onFailed(-1, QXContext.getString(R.string.qx_provider_upload_not_implement), message)
            return
        }
        QXContext.getInstance().uploadProvider.upload(QXIMKit.FileType.TYPE_VIDEO, path, object : UploadCallback {
            override fun onProgress(progress: Int) {
                callback?.onProgress(progress)
            }

            override fun onCompleted(url: String?) {
                if (url?.isNotEmpty()!!) {
                    var video = message.messageContent as VideoMessage
                    video.headUrl = headerUrl
                    video.originUrl = url
                    callback?.onCompleted(message)
                }
            }

            override fun onFailed(errorCode: Int, errorMsg: String?) {
                callback?.onFailed(-1, QXContext.getString(R.string.qx_upload_fail, errorCode, errorMsg
                    ?: ""), message)
            }

        })
    }

    fun uploadAudio(message: Message?, callback: UIUploadCallback?) {
        if (QXContext.getInstance().uploadProvider == null) {
            callback?.onFailed(-1, QXContext.getString(R.string.qx_provider_upload_not_implement), message)
            return
        }

        var audio = message!!.messageContent as AudioMessage
        QXContext.getInstance().uploadProvider.upload(QXIMKit.FileType.TYPE_VOICE, audio.localPath, object : UploadCallback {
            override fun onProgress(progress: Int) {
                callback?.onProgress(progress)
            }

            override fun onCompleted(url: String?) {
                if (url?.isNotEmpty()!!) {
                    //TODO 上传语音成功，发送语音消息
                    var voice = message.messageContent as AudioMessage
                    voice.originUrl = url
                    callback?.onCompleted(message)
                }
            }

            override fun onFailed(errorCode: Int, errorMsg: String?) {
                callback?.onFailed(-1, QXContext.getString(R.string.qx_upload_fail, errorCode, errorMsg
                    ?: ""), message)
            }

        })
    }

    fun uploadImage(message: Message?, callback: UIUploadCallback?) {

        if (QXContext.getInstance().uploadProvider == null) {
            callback?.onFailed(-1, QXContext.getString(R.string.qx_provider_upload_not_implement), message)
            return
        }
        var originUrl = (message!!.messageContent as ImageMessage).originUrl
        if(!TextUtils.isEmpty(originUrl)) {
            callback?.onCompleted(message)
            return
        }
        var path = (message!!.messageContent as ImageMessage).localPath

        QXContext.getInstance().uploadProvider.upload(QXIMKit.FileType.TYPE_IMAGE, path, object : UploadCallback {
            override fun onProgress(progress: Int) {
                callback?.onProgress(progress)
            }

            override fun onCompleted(url: String?) {
                if (url?.isNotEmpty()!!) {
                    var imageMessage = message.messageContent as ImageMessage
                    imageMessage.originUrl = url
                    callback?.onCompleted(message)
                }
            }

            override fun onFailed(errorCode: Int, errorMsg: String?) {
                callback?.onFailed(-1, QXContext.getString(R.string.qx_upload_fail, errorCode, errorMsg
                    ?: ""), message)
            }
        })
    }

    fun uploadFile(message: Message?, callback: UIUploadCallback?) {
        if (QXContext.getInstance().uploadProvider == null) {
            callback?.onFailed(-1, QXContext.getString(R.string.qx_provider_upload_not_implement), message)
            return
        }
        var file = message!!.messageContent as FileMessage
        QXContext.getInstance().uploadProvider.upload(QXIMKit.FileType.TYPE_FILE, file.localPath, object : UploadCallback {
            override fun onProgress(progress: Int) {
                callback?.onProgress(progress)
            }

            override fun onCompleted(url: String?) {
                if (url?.isNotEmpty()!!) {
                    var file = message.messageContent as FileMessage
                    file.originUrl = url
                    callback?.onCompleted(message)
                }
            }

            override fun onFailed(errorCode: Int, errorMsg: String?) {
                callback?.onFailed(-1, QXContext.getString(R.string.qx_upload_fail, errorCode, errorMsg
                    ?: ""), message)

            }

        })
    }

    fun uploadGeoShot(message: Message?, callback: UIUploadCallback?) {

        if (QXContext.getInstance().uploadProvider == null) {
            callback?.onFailed(-1, QXContext.getString(R.string.qx_provider_upload_not_implement), message)
            return
        }
        var geo = message!!.messageContent as GeoMessage
        QXContext.getInstance().uploadProvider.upload(QXIMKit.FileType.TYPE_IMAGE, geo.localPath, object : UploadCallback {
            override fun onProgress(progress: Int) {
                callback?.onProgress(progress)
            }

            override fun onCompleted(url: String?) {
                geo.previewUrl = url
                callback?.onCompleted(message)
            }

            override fun onFailed(errorCode: Int, errorMsg: String?) {
                callback?.onFailed(-1, QXContext.getString(R.string.qx_upload_fail, errorCode, errorMsg
                    ?: ""), message)

            }

        })
    }

}