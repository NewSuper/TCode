package com.qx.imui.util


import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import android.util.Log
import androidx.core.content.FileProvider
import com.qx.im.model.FileInfo
import com.qx.imlib.qlog.QLog
import com.qx.imui.QXContext
import com.qx.imui.R
import com.qx.message.FileMessage
import java.io.*
import java.util.*
import kotlin.experimental.and

object FileUtils {

    val TAG = "FileUtil"
    var mIconMap = HashMap<String, Int>()
    var mFileTypes = HashMap<String?, String>()
    fun deleteFile(path: String?) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }

    @JvmStatic
    fun getSuffixName(path: String): String {
        if (path.isEmpty()) {
            return ""
        }
        val index = path.lastIndexOf(".")
        return if (index > -1) {
            path.substring(index)
        } else ""
    }

    /**
     * 获取文件类型
     * （获取图片jpg、png等格式、文件word、xml等格式等）
     *
     * @param filePath
     * @return
     */
    fun getFileType(filePath: String): String? {
        return mFileTypes[getFileHeader(filePath)]
    }

    /**
     * 获取文件后缀
     * @param url
     * @return
     */
    fun getFileSuffix(url: String): String? {
        var suffixStr: String? = ""
        if (url.contains(".")) {
            val fileSuffix = url.substring(url.lastIndexOf("."), url.length)
            if (fileSuffix != null && fileSuffix.length < 7) {
                suffixStr = fileSuffix
            }
        }
        return suffixStr
    }

    /**
     * 判断文件是否图片
     */
    fun isImageFile(filePath: String?): Boolean {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)
        if (options.outWidth === -1) {
            return false
        }
        return true
    }

    /**
     * 获取文件头信息
     *
     * @param filePath
     * @return
     */
    private fun getFileHeader(filePath: String): String? {
        var `is`: FileInputStream? = null
        var value: String? = null
        try {
            `is` = FileInputStream(filePath)
            val b = ByteArray(3)
            `is`.read(b, 0, b.size)
            value = bytesToHexString(b)
        } catch (e: Exception) {
        } finally {
            if (null != `is`) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                }
            }
        }
        return value
    }

    /**
     * 将byte字节转换为十六进制字符串
     *
     * @param src
     * @return
     */
    private fun bytesToHexString(src: ByteArray?): String? {
        val builder = StringBuilder()
        if (src == null || src.size <= 0) {
            return null
        }
        var hv: String
        for (i in src.indices) {
            hv = Integer.toHexString((src[i] and 0xFF.toByte()).toInt()).toUpperCase()
            if (hv.length < 2) {
                builder.append(0)
            }
            builder.append(hv)
        }
        return builder.toString()
    }

    init {
        mFileTypes["FFD8FF"] = "jpg"
        mFileTypes["89504E47"] = "png"
        mFileTypes["47494638"] = "gif"
        mFileTypes["49492A00"] = "tif"
        mFileTypes["424D"] = "bmp"
        mFileTypes["41433130"] = "dwg" //CAD
        mFileTypes["38425053"] = "psd"
        mFileTypes["7B5C727466"] = "rtf" //日记本
        mFileTypes["3C3F786D6C"] = "xml"
        mFileTypes["68746D6C3E"] = "html"
        mFileTypes["44656C69766572792D646174653A"] = "eml" //邮件
        mFileTypes["D0CF11E0"] = "doc"
        mFileTypes["5374616E64617264204A"] = "mdb"
        mFileTypes["252150532D41646F6265"] = "ps"
        mFileTypes["255044462D312E"] = "pdf"
        mFileTypes["504B0304"] = "zip"
        mFileTypes["52617221"] = "rar"
        mFileTypes["57415645"] = "wav"
        mFileTypes["41564920"] = "avi"
        mFileTypes["2E524D46"] = "rm"
        mFileTypes["000001BA"] = "mpg"
        mFileTypes["000001B3"] = "mpg"
        mFileTypes["6D6F6F76"] = "mov"
        mFileTypes["3026B2758E66CF11"] = "asf"
        mFileTypes["4D546864"] = "mid"
        mFileTypes["1F8B08"] = "gz"

        mIconMap[".ppt"] = R.drawable.imui_ic_file_ppt
        mIconMap[".pptx"] = R.drawable.imui_ic_file_ppt
        mIconMap[".doc"] = R.drawable.imui_ic_file_word
        mIconMap[".docx"] = R.drawable.imui_ic_file_word
        mIconMap[".xls"] = R.drawable.imui_ic_file_xlsx
        mIconMap[".xlsx"] = R.drawable.imui_ic_file_xlsx
        mIconMap[".txt"] = R.drawable.imui_ic_file_txt
        mIconMap[".pdf"] = R.drawable.imui_ic_file_pdf
        mIconMap[".apk"] = R.drawable.imui_ic_file_apk
        mIconMap[".zip"] = R.drawable.imui_ic_file_compress
        mIconMap[".rar"] = R.drawable.imui_ic_file_compress
        mIconMap[".7z"] = R.drawable.imui_ic_file_compress
        mIconMap[".gzip"] = R.drawable.imui_ic_file_compress
        mIconMap[".tar"] = R.drawable.imui_ic_file_compress
        mIconMap[".png"] = R.drawable.imui_ic_file_image
        mIconMap[".jpg"] = R.drawable.imui_ic_file_image
        mIconMap[".jpeg"] = R.drawable.imui_ic_file_image
        mIconMap[".gif"] = R.drawable.imui_ic_file_image
        mIconMap[".webp"] = R.drawable.imui_ic_file_image
        mIconMap[".svg"] = R.drawable.imui_ic_file_image
        mIconMap[".bmp"] = R.drawable.imui_ic_file_image
        mIconMap[".mp4"] = R.drawable.imui_ic_file_video
        mIconMap[".avi"] = R.drawable.imui_ic_file_video
        mIconMap[".mpg"] = R.drawable.imui_ic_file_video
        mIconMap[".3gp"] = R.drawable.imui_ic_file_video
        mIconMap[".flv"] = R.drawable.imui_ic_file_video
        mIconMap[".rm"] = R.drawable.imui_ic_file_video
        mIconMap[".rmvb"] = R.drawable.imui_ic_file_video
        mIconMap[".mov"] = R.drawable.imui_ic_file_video
        mIconMap[".mpeg"] = R.drawable.imui_ic_file_video
        mIconMap[".wmv"] = R.drawable.imui_ic_file_video
        mIconMap[".mkv"] = R.drawable.imui_ic_file_video
        mIconMap[".f4v"] = R.drawable.imui_ic_file_video
        mIconMap[".mp3"] = R.drawable.imui_ic_file_audio
        mIconMap[".wav"] = R.drawable.imui_ic_file_audio
        mIconMap[".wma"] = R.drawable.imui_ic_file_audio
        mIconMap[".md"] = R.drawable.imui_ic_file_audio
        mIconMap[".ogg"] = R.drawable.imui_ic_file_audio
        mIconMap[".ape"] = R.drawable.imui_ic_file_audio
        mIconMap[".aac"] = R.drawable.imui_ic_file_audio
        mIconMap[".flac"] = R.drawable.imui_ic_file_audio
    }

    @JvmStatic
    fun getResource(type: String): Int {
        var resId = mIconMap[type.toLowerCase()]
        return resId ?: R.drawable.imui_ic_file_unkown
    }

    fun getResource(content: FileMessage): Int {
        for (icon in mIconMap.keys) {
            if (icon.contains(content.type.toLowerCase())) {
                return mIconMap[icon]!!
            }
        }
        return R.drawable.imui_ic_file_unkown
    }


    @JvmStatic
    fun readPictureDegree(context: Context, path: String?): Int {
        var degree: Short = 0
        try {
            var exifInterface: ExifInterface? = null
            if (LibStorageUtils.isBuildAndTargetForQ(context)) {
                if (uriStartWithContent(Uri.parse(path))) {
                    val pfd = context.contentResolver.openFileDescriptor(Uri.parse(path), "r")
                    if (pfd != null && Build.VERSION.SDK_INT >= 24) {
                        exifInterface = ExifInterface(pfd.fileDescriptor)
                    }
                } else {
                    exifInterface = ExifInterface(path!!)
                }
            } else {
                exifInterface = ExifInterface(path!!)
            }
            if (exifInterface == null) {
                return 0
            }
            val orientation = exifInterface.getAttributeInt("Orientation", 1)
            when (orientation) {
                3 -> degree = 180
                6 -> degree = 90
                8 -> degree = 270
            }
        } catch (var5: java.lang.Exception) {
            var5.printStackTrace()
        }
        return degree.toInt()
    }

    @JvmStatic
    fun convertBitmap2File(bm: Bitmap?, dir: String?, name: String): File? {
        return if (bm != null && !TextUtils.isEmpty(dir)) {
            val dirFile = File(dir)
            if (!dirFile.exists()) {
                QLog.e(TAG, "convertBitmap2File: dir does not exist! -" + dirFile.absolutePath)
                val successMkdir = dirFile.mkdirs()
                if (!successMkdir) {
                    QLog.e(TAG, "Created folders unSuccessfully")
                }
            }
            var targetFile = File(dirFile.path + File.separator + name)
            if (targetFile.exists()) {
                val isDelete = targetFile.delete()
                QLog.e(TAG, "convertBitmap2File targetFile isDelete:$isDelete")
            }
            val tmpFile = File(dirFile.path + File.separator + name + ".tmp")
            try {
                val bos = BufferedOutputStream(FileOutputStream(tmpFile))
                bm.compress(CompressFormat.PNG, 100, bos)
                bos.flush()
                bos.close()
            } catch (var7: IOException) {
                QLog.e(TAG, "convertBitmap2File: Exception!$var7")
            }
            targetFile = File(dirFile.path + File.separator + name)
            if (tmpFile.renameTo(targetFile)) targetFile else tmpFile
        } else {
            QLog.e(TAG, "convertBitmap2File bm or dir should not be null!")
            null
        }
    }

    @JvmStatic
    fun getSuffix(path: String): String? {
        return if (TextUtils.isEmpty(path)) {
            null
        } else {
            val lastDot = path.lastIndexOf(".")
            if (lastDot < 0) null else path.substring(lastDot + 1)
        }
    }

    @JvmStatic
    fun getFileNameWithPath(path: String): String? {
        return if (TextUtils.isEmpty(path)) {
            QLog.e(TAG, "getFileNameWithPath path should not be null!")
            null
        } else {
            val start = path.lastIndexOf("/")
            if (start != -1) path.substring(start + 1) else null
        }
    }

    @JvmStatic
    fun isValidateLocalUri(pUri: Uri?): Boolean {
        return uriStartWithFile(pUri) || uriStartWithContent(pUri)
    }

    @JvmStatic
    fun uriStartWithContent(srcUri: Uri?): Boolean {
        return srcUri != null && "content" == srcUri.scheme
    }

    @JvmStatic
    fun uriStartWithFile(pUri: Uri?): Boolean {
        return pUri != null && "file" == pUri.scheme && pUri.toString().length > 7
    }

    @JvmStatic
    fun copyFile(src: File?, path: String, name: String): File? {
        return if (src == null) {
            QLog.e(TAG, "copyFile src should not be null!")
            null
        } else if (!src.exists()) {
            QLog.e(TAG, "copyFile: src file does not exist! -" + src.absolutePath)
            null
        } else {
            var dest = File(path)
            if (!dest.exists()) {
                QLog.d(TAG, "copyFile: dir does not exist!")
                val successMkdir = dest.mkdirs()
                if (!successMkdir) {
                    QLog.e(TAG, "Created folders unSuccessfully")
                }
            }
            dest = File(path + name)
            try {
                val fis = FileInputStream(src)
                val fos = FileOutputStream(dest)
                val buffer = ByteArray(1024)
                var length: Int
                while (fis.read(buffer).also { length = it } != -1) {
                    fos.write(buffer, 0, length)
                }
                fos.flush()
                fos.close()
                fis.close()
                dest
            } catch (var8: IOException) {
                QLog.e(TAG, "copyFile: Exception!$var8")
                dest
            }
        }
    }

    @JvmStatic
    fun copyFile(srcPath: String?, path: String?, name: String?): Boolean {
        return if (TextUtils.isEmpty(srcPath)) {
            QLog.e(TAG, "copyFile src should not be null!")
            false
        } else {
            val src = File(srcPath)
            if (!src.exists()) {
                QLog.e(TAG, "copyFile: src file does not exist! -" + src.absolutePath)
                false
            } else {
                var dest = File(path)
                if (!dest.exists()) {
                    QLog.d(TAG, "copyFile: dir does not exist!")
                    val successMkdir = dest.mkdirs()
                    if (!successMkdir) {
                        QLog.e(TAG, "Created folders unSuccessfully")
                    }
                }
                dest = File(path, name)
                var fis: FileInputStream? = null
                var fos: FileOutputStream? = null
                val var8: Boolean
                try {
                    fis = FileInputStream(src)
                    fos = FileOutputStream(dest)
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (fis.read(buffer).also { length = it } != -1) {
                        fos.write(buffer, 0, length)
                    }
                    fos.flush()
                    return true
                } catch (var22: IOException) {
                    QLog.e(TAG, "copyFile: Exception!$var22")
                    var8 = false
                } finally {
                    try {
                        fos?.close()
                    } catch (var21: IOException) {
                        QLog.e(TAG, "copyFile fos close$var21")
                    }
                    try {
                        fis?.close()
                    } catch (var20: IOException) {
                        QLog.e(TAG, "copyFile fis close$var20")
                    }
                }
                var8
            }
        }
    }

    @JvmStatic
    fun copyFileToInternal(
        context: Context,
        srcUri: Uri,
        desPath: String?,
        name: String?
    ): Boolean {
        return if (uriStartWithFile(srcUri)) {
            copyFile(srcUri.toString(), desPath, name)
        } else {
            if (uriStartWithContent(srcUri)) copyFile(
                context,
                srcUri,
                File(desPath, name).absolutePath
            ) else false
        }
    }

    @JvmStatic
    fun copyFile(context: Context, srcUri: Uri, desPath: String?): Boolean {
        return if (!uriStartWithContent(srcUri)) {
            false
        } else {
            val pfd: ParcelFileDescriptor
            pfd = try {
                context.contentResolver.openFileDescriptor(srcUri, "r")!!
            } catch (var22: FileNotFoundException) {
                QLog.e(TAG, "copyFile srcUri is error uri is $srcUri")
                return false
            }
            if (pfd != null) {
                var fis: FileInputStream? = null
                var fos: FileOutputStream? = null
                val var7: Boolean
                try {
                    fis = FileInputStream(pfd.fileDescriptor)
                    fos = FileOutputStream(desPath)
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (fis.read(buffer).also { length = it } != -1) {
                        fos.write(buffer, 0, length)
                    }
                    fos.flush()
                    return true
                } catch (var23: IOException) {
                    QLog.e(TAG, "copyFile: Exception!$var23")
                    var7 = false
                } finally {
                    if (fis != null) {
                        try {
                            fis.close()
                        } catch (var21: IOException) {
                            QLog.e(TAG, "copyFile: Exception!$var21")
                        }
                    }
                    if (fos != null) {
                        try {
                            fos.close()
                        } catch (var20: IOException) {
                            QLog.e(TAG, "copyFile: Exception!$var20")
                        }
                    }
                }
                var7
            } else {
                true
            }
        }
    }

    @JvmStatic
    fun getInternalCachePath(context: Context, dir: String): String? {
        return if (!SavePathUtils.isSavePathEmpty()) {
            SavePathUtils.getSavePath()
        } else {
            val cacheDir = File(context.cacheDir.path + File.separator + dir)
            if (!cacheDir.exists()) {
                val result = cacheDir.mkdir()
                QLog.w(TAG, "getInternalCachePath = " + cacheDir.path + ", result = " + result)
            }
            cacheDir.path
        }
    }

    @JvmStatic
    fun getFileInfoByUri(context: Context, uri: Uri): FileInfo? {
        return if (uriStartWithContent(uri)) {
            getFileInfoByContent(context, uri)
        } else {
            if (uriStartWithFile(uri)) getFileInfoByFile(uri) else null
        }
    }

    private fun getFileInfoByFile(uri: Uri): FileInfo? {
        return if (uriStartWithFile(uri)) {
            val filePath = uri.toString().substring(7)
            val file = File(filePath)
            if (file.exists()) {
                val fileInfo = FileInfo()
                val name = file.name
                fileInfo.size = file.length()
                fileInfo.name = name
                val lastDotIndex = name.lastIndexOf(".")
                if (lastDotIndex > 0) {
                    val fileSuffix = file.name.substring(lastDotIndex + 1)
                    fileInfo.type = fileSuffix
                }
                fileInfo
            } else {
                null
            }
        } else {
            QLog.e(TAG, "getDocumentByFile uri is not file")
            null
        }
    }

    private fun getFileInfoByContent(context: Context, uri: Uri): FileInfo? {
        return if (uriStartWithContent(uri)) {
            val projection = arrayOf("_display_name", "_size", "mime_type")
            var cursor: Cursor? = null
            var name: String?
            try {
                val fileInfo = FileInfo()
                cursor = context.contentResolver.query(
                    uri,
                    projection,
                    null as String?,
                    null as Array<String?>?,
                    null as String?
                )
                if (cursor != null) {
                    if (!cursor.moveToFirst()) {
                        return null
                    }
                    name = cursor.getString(0)
                    val size = cursor.getLong(1)
                    fileInfo.name = name
                    fileInfo.size = size
                    val var8: FileInfo?
                    if (!TextUtils.isEmpty(name)) {
                        val lastDotIndex = name.lastIndexOf(".")
                        if (lastDotIndex > 0) {
                            val fileSuffix = name.substring(lastDotIndex + 1)
                            fileInfo.type = fileSuffix
                        }
                        if (cursor.moveToNext()) {
                            QLog.e(TAG, "uri is error,cursor has second value,uri is$uri")
                        }
                        var8 = fileInfo
                        return var8
                    }
                    QLog.e(TAG, "getFileInfoByContent getName is empty")
                    var8 = null
                    return var8
                }
                QLog.e(TAG, "getFileInfoByContent cursor is null")
                name = null
            } catch (var13: java.lang.Exception) {
                QLog.e(TAG, "getDocumentByContent is error $var13")
                name = null
                return null
            } finally {
                cursor?.close()
            }
            return null
        } else {
            QLog.e(TAG, "getDocumentByContent uri is not content")
            null
        }
    }

    @JvmStatic
    fun openFileByPath(context: Context, path: String?) {
        if (context == null || path.isNullOrEmpty())
            return
        val intent = Intent()
        intent.setAction(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        var type = ""
        for (index in MATCH_ARRAY.indices) {
            val match = MATCH_ARRAY[index][0]
            if (path!!.contains(match)) {
                type = MATCH_ARRAY[index][1]
                break
            }
        }
        Log.i("open_file_tag", "Type:" + type)
        try {
            if (type.isNullOrEmpty()) {
                ToastUtil.toast(
                    context,
                    QXContext.getString(R.string.qx_open_file_not_support_open)
                )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        context.packageName + context.resources.getString(R.string.qx_authorities_fileprovider),
                        File(path)
                    )
                    Log.i("open_file_tag", "uri:" + uri)
                    intent.setDataAndType(uri, type)
                } else {
                    intent.setDataAndType(Uri.fromFile(File(path)), type)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            ToastUtil.toast(context, QXContext.getString(R.string.qx_open_file_not_support_open))
            Log.i("open_file_tag", "Error:" + e.message)
            e.printStackTrace()
        }

    }


    private val MATCH_ARRAY = arrayOf(
        arrayOf(".3gp", "video/3gpp"),
        arrayOf(".apk", "application/vnd.android.package-archive"),
        arrayOf(".asf", "video/x-ms-asf"),
        arrayOf(".avi", "video/x-msvideo"),
        arrayOf(".bin", "application/octet-stream"),
        arrayOf(".bmp", "image/bmp"),
        arrayOf(".c", "text/plain"),
        arrayOf(".class", "application/octet-stream"),
        arrayOf(".conf", "text/plain"),
        arrayOf(".cpp", "text/plain"),
        arrayOf(".doc", "application/msword"),
        arrayOf(".exe", "application/octet-stream"),
        arrayOf(".gif", "image/gif"),
        arrayOf(".gtar", "application/x-gtar"),
        arrayOf(".gz", "application/x-gzip"),
        arrayOf(".h", "text/plain"),
        arrayOf(".htm", "text/html"),
        arrayOf(".html", "text/html"),
        arrayOf(".jar", "application/java-archive"),
        arrayOf(".java", "text/plain"),
        arrayOf(".jpeg", "image/jpeg"),
        arrayOf(".jpg", "image/jpeg"),
        arrayOf(".js", "application/x-javascript"),
        arrayOf(".log", "text/plain"),
        arrayOf(".m3u", "audio/x-mpegurl"),
        arrayOf(".m4a", "audio/mp4a-latm"),
        arrayOf(".m4b", "audio/mp4a-latm"),
        arrayOf(".m4p", "audio/mp4a-latm"),
        arrayOf(".m4u", "video/vnd.mpegurl"),
        arrayOf(".m4v", "video/x-m4v"),
        arrayOf(".mov", "video/quicktime"),
        arrayOf(".mp2", "audio/x-mpeg"),
        arrayOf(".mp3", "audio/x-mpeg"),
        arrayOf(".mp4", "video/mp4"),
        arrayOf(".mpc", "application/vnd.mpohun.certificate"),
        arrayOf(".mpe", "video/mpeg"),
        arrayOf(".mpeg", "video/mpeg"),
        arrayOf(".mpg", "video/mpeg"),
        arrayOf(".mpg4", "video/mp4"),
        arrayOf(".mpga", "audio/mpeg"),
        arrayOf(".msg", "application/vnd.ms-outlook"),
        arrayOf(".ogg", "audio/ogg"),
        arrayOf(".pdf", "application/pdf"),
        arrayOf(".png", "image/png"),
        arrayOf(".pps", "application/vnd.ms-powerpoint"),
        arrayOf(".ppt", "application/vnd.ms-powerpoint"),
        arrayOf(".prop", "text/plain"),
        arrayOf(".rar", "application/x-rar-compressed"),
        arrayOf(".rc", "text/plain"),
        arrayOf(".rmvb", "audio/x-pn-realaudio"),
        arrayOf(".rtf", "application/rtf"),
        arrayOf(".sh", "text/plain"),
        arrayOf(".tar", "application/x-tar"),
        arrayOf(".tgz", "application/x-compressed"),
        arrayOf(".txt", "text/plain"),
        arrayOf(".wav", "audio/x-wav"),
        arrayOf(".wma", "audio/x-ms-wma"),
        arrayOf(".wmv", "audio/x-ms-wmv"),
        arrayOf(".wps", "application/vnd.ms-works"),
        arrayOf(".xml", "text/plain"),
        arrayOf(".z", "application/x-compress"),
        arrayOf(".zip", "application/zip"),
        arrayOf("", "*/*")
    )

}