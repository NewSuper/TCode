package com.qx.imui.util.language

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.text.TextUtils
import com.qx.imlib.qlog.QLog
import com.qx.imui.bean.SupportLanguage
import com.qx.imui.util.SPUtils
import java.util.*


object LanguageUtil {

    const val TAG = "LanguageUtil"

    val languageList = mutableListOf<SupportLanguage>()

    init {
        var lanauge = SupportLanguage("Chinese Simplified", "简体中文", AppLanaguage.CHINESE_SIMPLE.desc, false)
        languageList.add(lanauge)
        lanauge = SupportLanguage("Chinese Tradition", "繁体中文", AppLanaguage.CHINESE_TW.desc, false)
        languageList.add(lanauge)
        lanauge = SupportLanguage("English", "English", AppLanaguage.ENGLISH.desc, false)
        languageList.add(lanauge)
        lanauge = SupportLanguage("Français", "French", AppLanaguage.FRENSH.desc, false)
        languageList.add(lanauge)
        lanauge = SupportLanguage("Español", "Spain", AppLanaguage.SPAIN.desc, false)
        languageList.add(lanauge)
        lanauge = SupportLanguage("日本語の言語", "Japanese", AppLanaguage.JAPAN.desc, false)
        languageList.add(lanauge)
        lanauge = SupportLanguage("한국의", "Korean", AppLanaguage.KOERA.desc, false)
        languageList.add(lanauge)
    }


    fun changeAppLanguage(activity: Activity, language: String, cls: Class<*>) {
        val locale: Locale = getLocaleByLanguage(if (language.isNullOrEmpty()) "zh_CN" else language)
        QLog.e(TAG, "设置的语言：$language")
        setAppLanguage(activity, locale!!)
        SPUtils.cacheLanguage(activity, locale.language, locale.country)
        val intent = Intent(activity, cls)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
    }

//    fun checkLanguage(context: Context) : Context{
//        val locale = getSystemLocale(context)
//        val lan = "${locale.language}_${locale.country}"
//        val cacheLanguage = SPUtils.getCacheLanguage(context)
//        QLog.e(TAG, "checkLanguage: $lan,cacheLanguage: $cacheLanguage")
//        if (cacheLanguage.isNullOrEmpty()) {
//            if (isContainsKeyLanguage(lan)) {
//                SPUtils.cacheLanguage(context, locale.language, locale.country)
//            } else {
//                SPUtils.cacheLanguage(context, "zh", "CN")
//            }
//        } else {
//            val codes = cacheLanguage.split("_")
//            return setAppLanguage(context, Locale(codes[0], codes[1]))
//        }
//        return  context
//    }

    /**
     * 设置默认的语言
     */
    fun setLocal(context: Context): Context? {
        val language = SPUtils.getCacheLanguage(context)
        val locale = getLocaleByLanguage(if (language.isNullOrEmpty()) "zh_CN" else language)
        QLog.e(TAG, "设置的语言setLocal：$language")
        if (TextUtils.isEmpty(language)) {
            //保存
            SPUtils.cacheLanguage(context, "zh", "CN")
            return setAppLanguage(context, locale)
        } else {
            val codes = language?.split("_")
            return setAppLanguage(context, Locale(codes?.get(0), codes?.get(1)))
        }
    }


    /**
     * 切换语言
     */
    fun setAppLanguage(context: Context, locale: Locale) : Context {
        Locale.setDefault(locale);
        val resources = context.resources
        val configuration = Configuration(resources.getConfiguration())
        if (Build.VERSION.SDK_INT >= 17) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
        return context
    }

    private fun getLocaleByLanguage(language: String): Locale {
        if (isContainsKeyLanguage(language)) {
            val support = languageList.find { it.code.equals(language, true) }
            val codes = support!!.code.split("_")
            return Locale(codes[0], codes[1])
        } else {
            val locale: Locale = Locale.getDefault()
            for (support in languageList) {
                if (TextUtils.equals(support.code, locale.language)) {
                    return locale
                }
            }
        }
        return Locale.CHINA
    }

    fun getSystemLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault().get(0)
        } else {
            Locale.getDefault()
        }
    }

    fun isContainsKeyLanguage(language: String): Boolean {
        return languageList.find { it.code.equals(language, true) } != null
    }
}