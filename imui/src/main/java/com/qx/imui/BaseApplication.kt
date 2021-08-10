package com.qx.imui


import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils

import com.qx.imlib.utils.SystemUtil
import com.qx.imui.util.AcitivityImpl
import com.qx.imui.util.JsonUtil
import com.qx.imui.util.PreferenceUtils
import com.qx.imui.util.UserResponse
import com.qx.imui.util.language.MultiLanguageUtil
import com.tencent.bugly.crashreport.CrashReport


open class BaseApplication : Application(), AcitivityImpl,
    Thread.UncaughtExceptionHandler {

    private var defaultException: Thread.UncaughtExceptionHandler? = null
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let { MultiLanguageUtil.attachBaseContext(base) })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        MultiLanguageUtil.attachBaseContext(instance)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashReport.initCrashReport(this, "e0ce6d1542", true);
        if (applicationInfo.packageName != SystemUtil.getCurrentProcessName(applicationContext)) {
            return
        }
        defaultException = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)

        initUserCache()
        initLanguage()
        Constans.appVersionSwitch(VersionConstant.VERSION_TEST);
    }

    fun initLanguage(){
        registerActivityLifecycleCallbacks(MultiLanguageUtil.callbacks)
    }

    /**
     * 检验本地目录是否有缓存用户有无登录
     */
    private fun initUserCache() {
        val userJson: String = PreferenceUtils.getString(instance, "user")
        if (!TextUtils.isEmpty(userJson)) {
            userBean = JsonUtil.json2Bean(userJson, UserResponse::class.java)
        }
    }

    //登录成功后对用户数据进行缓存
    fun setUserCache(json: String) {
        PreferenceUtils.setString(instance, "user", json + "")
        userBean = JsonUtil.json2Bean(json, UserResponse::class.java)
    }


    //清楚登录用户的缓存数据
    fun clearUserCache() {
        userBean.clearData()
    }

    companion object {
        var instance: Context? = null
        private var userBean: UserResponse = UserResponse.getInstance()
        val activityList = mutableListOf<Activity>()

        //获取全局user用户
        fun getUserBean(): UserResponse {
            if (userBean == null) {
                userBean = UserResponse.getInstance()
            }
            return userBean
        }

        fun addActivity(activity: Activity) {
            if (!activityList.contains(activity)) {
                activityList.add(activity)
            }
        }

        fun removeActivity(activity: Activity) {
            if (activityList.contains(activity)) {
                activityList.remove(activity)
            }
        }

        fun removeAllActivity() {
            activityList.forEach {
                it?.finish()
            }
        }
    }

    override fun add(activity: Activity) {
        addActivity(activity)
    }

    override fun remove(activity: Activity) {
        removeActivity(activity)
    }

    override fun removeAll() {
        removeAllActivity()
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        var reason = e.toString()
        if (!TextUtils.isEmpty(reason) && reason.contains(":")) {
            reason = reason.substring(0, reason.indexOf(":"))
        }
        removeAllActivity()
        defaultException?.uncaughtException(t, e)
    }

}