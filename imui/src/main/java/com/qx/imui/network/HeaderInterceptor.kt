package com.qx.imui.network

import com.blankj.utilcode.util.AppUtils
import com.qx.imui.BaseApplication
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.*

//添加header的拦截器
class HeaderInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        //添加header固定参数
        val mFixedHeaderParam = HashMap<String, String>().let {
            it["type"] = "android"
            it["appVersion"] = AppUtils.getAppVersionCode().toString()
//            it["langCode"] = LanguageSpUtil.getLanguageType().code
            if (BaseApplication.getUserBean().token.isNotEmpty()) {
                it["token"] = BaseApplication.getUserBean().token
            }
            it
        }
        for ((key, value) in mFixedHeaderParam) {
            requestBuilder.header(key, value)
        }
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}