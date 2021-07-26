package com.qx.imlib.utils.file


import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import kotlin.experimental.and

object FileUtil {

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


}