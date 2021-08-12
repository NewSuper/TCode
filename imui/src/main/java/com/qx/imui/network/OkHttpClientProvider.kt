package com.qx.imui.network

import com.blankj.utilcode.util.AppUtils
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.*
import java.util.concurrent.TimeUnit

//OkHttpClient提供类
object OkHttpClientProvider {
    private const val CONNECTTIMEOUT = 60 //连接超时时间
    private const val READTIMEOUT = 60 //读取超时时间
    private const val WRITETIMEOUT = 60 //读取超时时间//公共日志//创建httpclient

    private var okHttpClient: OkHttpClient

    init {
        val builder = OkHttpClient.Builder().apply {
            //设置连接超时时间
            connectTimeout(CONNECTTIMEOUT.toLong(), TimeUnit.SECONDS)
            readTimeout(READTIMEOUT.toLong(), TimeUnit.SECONDS)
            writeTimeout(WRITETIMEOUT.toLong(), TimeUnit.SECONDS)

            addInterceptor(HeaderInterceptor()) //添加header
            //日志相关
            HttpLoggingInterceptor().let {
                it.level = HttpLoggingInterceptor.Level.BODY
                if (AppUtils.isAppDebug()) {
                    addInterceptor(it)
                }
            }
        }
        okHttpClient = builder.build()
    }


    /**
     * 获取全局默认的okhttpClient
     *
     * @return
     */
    fun getDefaultOkHttpClient(): OkHttpClient {
        return okHttpClient
    }
}