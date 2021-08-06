package com.qx.imui.util

import android.content.Context
import com.qx.imui.util.language.AppLanaguage
import org.jetbrains.annotations.NotNull

object SPUtils {

    private const val USER_PROPERTY_NAME = "user_config"
    private const val USER_PROPERTY_LANGUAGE = "language"
    private const val USER_LOGINID = "cur_login_userid"

    const val RTC_SERVER_CONFIG = "RTC_SERVER_CONFIG"

    @JvmStatic
    fun cacheString(context: Context, key:String, value:String) {
        val sp = context.getSharedPreferences(USER_PROPERTY_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(key, value).apply()
    }

    @JvmStatic
    fun cacheLanguage(context: Context, @NotNull appLanaguage: AppLanaguage) {
        val sp = context.getSharedPreferences(USER_PROPERTY_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(USER_PROPERTY_LANGUAGE, appLanaguage.desc).apply()

    }

    @JvmStatic
    fun cacheLanguage(context: Context, @NotNull language: String, @NotNull country: String) {
        val sp = context.getSharedPreferences(USER_PROPERTY_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(USER_PROPERTY_LANGUAGE, "${language}_${country}").apply()
    }

    @JvmStatic
    fun getCacheLanguage(context: Context): String? {
        val sp = context.getSharedPreferences(USER_PROPERTY_NAME, Context.MODE_PRIVATE)
        return sp.getString(USER_PROPERTY_LANGUAGE, "")
    }
}