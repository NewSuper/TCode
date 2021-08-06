package com.qx.imui.plugin.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.qx.imlib.qlog.QLog
import com.qx.imlib.utils.event.EventBusUtil
import com.qx.imui.QXExtension
import com.qx.imui.QXIMKit
import com.qx.imui.R
import com.qx.imui.plugin.IPluginModule
import com.qx.imui.plugin.IPluginRequestPermissionResultCallback
import com.qx.imui.plugin.photovideo.Camera2Config
import com.qx.imui.plugin.photovideo.Camera2RecordActivity
import com.qx.imui.util.ImageCompressUtil
import com.qx.imui.util.PermissionCheckUtil
import com.qx.imui.util.upload.MediaMessageEmitter
import com.qx.message.Message
import com.qx.message.MessageType
import com.qx.message.QXError
import top.zibin.luban.OnCompressListener
import java.io.File


/**
 * [拍照+短视频] 组件
 */
class TakePhotoVideoPlugin : IPluginModule, IPluginRequestPermissionResultCallback {

    private val TAG = "TakePhotoVideoPlugin"

    private lateinit var activity: Activity
    var mTakePictureUri: Uri? = null
    private var extension: QXExtension? = null

    companion object StaticParams{
        //定义请求码常量
        val  TAKE_PICTURE_REQUESt_CODE = 250;
    }

    override fun obtainDrawable(context: Context): Drawable {
        return context.resources.getDrawable(R.drawable.imui_ic_chat_shot)
    }

    override fun obtainTitle(context: Context): String {
        return context.resources.getString(R.string.qx_chat_add_panel_shot)
    }

    /**
     * 处理组件的点击事件
     */
    override fun onClick(context: Activity, extension: QXExtension) {
        this.activity = context
        this.extension = extension
        val permissions = arrayOf("android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA")
        if (PermissionCheckUtil.checkPermissions(extension.context, permissions)) {
            requestTakePhoto(extension)
        } else {
            extension.requestPermissionForPluginResult(permissions, TAKE_PICTURE_REQUESt_CODE, this)
        }
    }

    /**
     * 权限处理结果
     */
    override fun onRequestPermissionResult(activity: Activity, extension: QXExtension, requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        if (PermissionCheckUtil.checkPermissions(activity, permissions)) {
            requestTakePhoto(extension)
        } else {
            extension.showRequestPermissionFailedAlter(PermissionCheckUtil.getNotGrantedPermissionMsg(activity, permissions, grantResults))
        }
        return true
    }

    /**
     * 组件相关配置初始化
     */
    private fun initCamera2Config(context: Context){
        Camera2Config.RECORD_PROGRESS_VIEW_COLOR = R.color.colorAccent
        Camera2Config.PREVIEW_MAX_HEIGHT = 1300
        Camera2Config.ENABLE_CAPTURE = true
        Camera2Config.ENABLE_RECORD = true
    }

    /**
     * 拍照或拍摄视频
     */
    private fun requestTakePhoto(extension: QXExtension) {
        val context = extension.context
        //配置Camera2相关参数
        initCamera2Config(context);
        val intent = Intent(context, Camera2RecordActivity::class.java)
        extension.startActivityForPluginResult(intent, TAKE_PICTURE_REQUESt_CODE, this);
    }

    /**
     * 拍照或者录制视频后的路径地址结果处理
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QLog.e(TAG, "resultCode:$resultCode,$data")
        if (requestCode ==  TAKE_PICTURE_REQUESt_CODE && resultCode != 0) {
            val tackType = data!!.getIntExtra(Camera2Config.INTENT_PLUGIN_TYPE_KEY, 0);
            val path = data!!.getStringExtra(Camera2Config.INTENT_PLUGIN_PATH_KEY);
            Log.i(Camera2Config.CAMERA2_CONFIG_TAG, "tackType==>$tackType    path==>$path")
            if(TextUtils.isEmpty(path)){
                Log.i(Camera2Config.CAMERA2_CONFIG_TAG, "path is Empty!")
                return;
            }
            when(tackType){
                Camera2Config.INTENT_PATH_SAVE_PIC ->{
                    //拍照
                    ImageCompressUtil.compressImage(activity,path,object : OnCompressListener {
                        override fun onSuccess(file: File) {
                            sendMediaMessage(MessageType.TYPE_IMAGE, file.path!!)
                        }

                        override fun onError(e: Throwable) {
                            Log.i(Camera2Config.CAMERA2_CONFIG_TAG, "onError()~~~")
                        }

                        override fun onStart() {
                        }

                    })
                }
                Camera2Config.INTENT_PATH_SAVE_VIDEO ->{
                    //录像
                    sendMediaMessage(MessageType.TYPE_VIDEO,path!!)
                }
            }
        }
    }

    /**
     * 发送消息
     */
    private fun sendMediaMessage(messageType: String,path:String){
        Log.i(Camera2Config.CAMERA2_CONFIG_TAG, "SendMessage-->Path:"+path+"   messageType:"+messageType)
        QXIMKit.getInstance().sendMediaMessage(activity, extension!!.conversationType, extension!!.targetId,messageType, Uri.parse(path),
            object : MediaMessageEmitter.SendMediaMessageCallback {
                override fun onProgress(progress: Int) {

                }

                override fun onUploadCompleted(message: Message?) {
                }

                override fun onUploadFailed(errorCode: Int, msg: String, message: Message?) {
                    Log.i(Camera2Config.CAMERA2_CONFIG_TAG, "SendMessage-->Path:"+path+"   messageType:"+messageType);
                }

                override fun onAttached(message: Message?) {
                    message?.let {
                        EventBusUtil.post(it)
                    }
                }

                override fun onSuccess() {
                    Log.i(Camera2Config.CAMERA2_CONFIG_TAG, "SendMessage-->onSuccess()");
                }

                override fun onError(error: QXError, message: Message?) {
                    Log.i(Camera2Config.CAMERA2_CONFIG_TAG, "SendMessage-->onError()");
                }

            })
    }


}