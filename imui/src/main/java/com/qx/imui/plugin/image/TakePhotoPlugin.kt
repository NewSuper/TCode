package com.qx.imui.plugin.image

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.qx.imlib.qlog.QLog
import com.qx.imui.QXExtension
import com.qx.imui.R
import com.qx.imui.plugin.IPluginModule
import com.qx.imui.util.KitStorageUtils
import com.qx.imui.util.PermissionCheckUtil
import java.io.File
import java.util.ArrayList

class TakePhotoPlugin : IPluginModule {

    private val TAG = "TakePhotoPlugin"

    var mTakePictureUri: Uri? = null
    private var extension: QXExtension? = null
    override fun obtainDrawable(context: Context): Drawable {
        return context.resources.getDrawable(R.drawable.imui_ic_chat_shot)
    }

    override fun obtainTitle(context: Context): String {
        return context.resources.getString(R.string.qx_chat_add_panel_shot)
    }

    override fun onClick(context: Activity, extension: QXExtension) {
        this.extension = extension
        val permissions = arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.CAMERA")
        if (PermissionCheckUtil.checkPermissions(extension.context, permissions)) {
            requestCamera(extension)
        } else {
            extension.requestPermissionForPluginResult(permissions, 255, this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QLog.e(TAG,"resultCode:$resultCode,$data")
        if (requestCode == 23 && resultCode != 0) {
            PictureSelectorActivity.PicItemHolder.itemList = ArrayList<PictureSelectorActivity.MediaItem>()
            val item = PictureSelectorActivity.MediaItem()
            item.uri = mTakePictureUri!!.path
            item.mediaType = 1
            PictureSelectorActivity.PicItemHolder.itemList.add(item)
            PictureSelectorActivity.PicItemHolder.itemSelectedList = null
            item.uri_sdk29 = mTakePictureUri.toString()
            val intent = Intent(extension?.context, PicturePreviewActivity::class.java)
            extension?.startActivityForPluginResult(intent, 0,this)
            MediaScannerConnection.scanFile(extension?.context, arrayOf(mTakePictureUri!!.path), null as Array<String?>?) { path, uri -> }
        }
    }

    protected fun requestCamera( extension: QXExtension) {
        val context = extension.context
        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        val resInfoList: List<ResolveInfo> = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resInfoList.isEmpty()) {
            Toast.makeText(context, context.resources.getString(R.string.qx_voip_cpu_error), Toast.LENGTH_SHORT).show()
        } else {
            val name: String
            val uri: Uri
            if (KitStorageUtils.isBuildAndTargetForQ(context)) {
                name = System.currentTimeMillis().toString()
                val values = ContentValues()
                values.put("description", "This is an image")
                values.put("_display_name", name)
                values.put("mime_type", "image/jpeg")
                values.put("title", name)
                values.put("relative_path", "Pictures")
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val resolver: ContentResolver = context.contentResolver
                val insertUri = resolver.insert(uri, values)
                mTakePictureUri = insertUri
                intent.putExtra("output", insertUri)
            } else {
                name = System.currentTimeMillis().toString() + ".jpg"
                val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                if (!path.exists()) {
                    path.mkdirs()
                }
                val file = File(path, name)
                mTakePictureUri = Uri.fromFile(file)
                uri = try {
                    FileProvider.getUriForFile(context, context.packageName + context.getString(R.string.qx_authorities_fileprovider), file)
                } catch (var10: Exception) {
                    QLog.e(TAG, "requestCamera$var10")
                    throw RuntimeException("Please check IMKit Manifest FileProvider config. Please refer to http://support.rongcloud.cn/kb/NzA1")
                }
                val var12: Iterator<*> = resInfoList.iterator()
                while (var12.hasNext()) {
                    val resolveInfo = var12.next() as ResolveInfo
                    val packageName = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                intent.putExtra("output", uri)
            }
            extension.startActivityForPluginResult(intent,23,this)
        }
//        }
    }
}