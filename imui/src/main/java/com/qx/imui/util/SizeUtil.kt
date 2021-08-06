package com.qx.imui.util

import android.content.Context
import android.view.ViewGroup
import java.text.DecimalFormat


object SizeUtil {
    private val UNIT_KB = 1024F
    private val UNIT_MB = 1024 * 1024F
    private val UNIT_GB = 1024 * 1024 * 1024F

    /**
     * 获取显示大小
     * @size 大小，单位：b
     */
    @JvmStatic
    fun getDisplaySize(size: Long): String {
        return when {
            size < UNIT_MB -> {
                format(size / (UNIT_KB)) + "KB"
            }
            size < UNIT_GB -> {
                format(size / (UNIT_MB)) + "MB"
            }
            else -> {
                format(size / (UNIT_GB)) + "GB"
            }
        }
    }

    private fun format(number: Float): String {
        val df = DecimalFormat("#.00")
        return df.format(number)
    }

    fun calcSize(width: Float, height: Float, maxSize: Int, lp: ViewGroup.LayoutParams, context : Context): ViewGroup.LayoutParams {
        when {
            width < height -> {
                var scale: Float = height / width
                if(width > maxSize) {
                    lp.width = DensityUtil.dip2px(context, maxSize.toFloat())
                } else {
                    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                lp.height = (lp.width * scale).toInt()

            }
            width > height -> {
                var scale: Float = width / height
                if(height > maxSize) {
                    lp.height = DensityUtil.dip2px(context, maxSize.toFloat())
                } else {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                lp.width = (lp.height * scale).toInt()
            }
            else -> {
                if(width > maxSize) {
                    lp.width = DensityUtil.dip2px(context, maxSize.toFloat())
                } else {
                    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                lp.height = lp.width
            }

        }
        return lp
    }

    @JvmStatic
    fun calcHeight(width: Float, height: Float, maxSize: Int, lp: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        var scale: Float = width / height
        lp.width = maxSize
        lp.height = (maxSize / scale).toInt()
        return lp
    }

}