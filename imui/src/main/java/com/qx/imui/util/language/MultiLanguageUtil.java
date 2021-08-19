package com.qx.imui.util.language;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.Locale;

import androidx.core.os.ConfigurationCompat;

//多语言切换的工具类
public class MultiLanguageUtil {

    /**
     * 绑定当前的语言,在Application的attachBaseContext或者BaseActivity的attachBaseContext中使用
     *
     * @param context
     * @return
     */
    public static Context attachBaseContext(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return createConfigurationResources(context);
        } else {
            setConfiguration(context);
            return context;
        }
    }

    /**
     * 绑定语言Android7.0以下版本
     *
     * @param context
     */
    public static void setConfiguration(Context context) {
        //如果本地有语言信息，以本地为主，如果本地没有使用默认Locale
        Locale locale = getAppSettingLocal(context);
        Configuration configuration = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        resources.updateConfiguration(configuration, dm);
    }

    /**
     * 绑定多语言Android 7.0以上版本
     *
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.N)
    private static Context createConfigurationResources(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale = getAppSettingLocal(context);
        configuration.setLocale(locale);
        configuration.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(configuration);
    }

    /**
     * 获取当前App设置的语言的Local语言
     *
     * @param context
     * @return
     */
    public static Locale getAppSettingLocal(Context context) {
        //如果本地有语言信息，以本地为主，如果本地没有使用默认Locale
        Locale appLocale = getContextLocale(context);
        Locale locale;
        String spLanguage = LanguageSpUtil.getLocaleLanguage(context);
        String spCountry = LanguageSpUtil.getLocaleCountry(context);
        if (TextUtils.isEmpty(spLanguage) && TextUtils.isEmpty(spCountry)) {
            locale = appLocale;
        } else {
            if (isSameLocal(appLocale, spLanguage, spCountry)) {
                locale = appLocale;
            } else {
                locale = new Locale(spLanguage, spCountry);
            }
        }
        return locale;
    }

    /**
     * 获取Context对应的Local语言
     *
     * @param context
     * @return
     */
    public static Locale getContextLocale(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = configuration.getLocales().get(0);
        } else {
            locale = configuration.locale;
        }
        return locale;
    }

    /**
     * 获取整个系统的Local语言
     *
     * @return
     */
    public static Locale getSystemLocale() {
        Configuration configuration = Resources.getSystem().getConfiguration();
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = ConfigurationCompat.getLocales(configuration).get(0);
        } else {
            locale = configuration.locale;
        }
        return locale;
    }

    /**
     * 设置新的语言
     *
     * @param context
     * @param newLocale
     * @return 是否设置了新的语言
     */
    public static boolean setAppLanguage(Context context, Locale newLocale) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(newLocale);
            configuration.setLocales(new LocaleList(newLocale));
            context.createConfigurationContext(configuration);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(newLocale);
        } else {
            configuration.locale = newLocale;
        }
        resources.updateConfiguration(configuration, metrics);
        context.getApplicationContext().getResources().updateConfiguration(configuration, metrics);
        saveLanguageLocale(context, newLocale);
        return true;
    }

    /**
     * 设置新的语言
     *
     * @param context
     * @param language
     * @param country
     * @return
     */
    public static boolean setAppLanguage(Context context, String language, String country) {
        return setAppLanguage(context, new Locale(language, country));
    }


    /**
     * 设置语言跟随系统
     *
     * @param context
     */
    public static void setAppLanguageSystem(Context context) {
        LanguageSpUtil.saveLocaleLanguageAndCountry(context, "", "");
    }

    /**
     * 持久化多语言
     *
     * @param context
     * @param locale
     */
    public static void saveLanguageLocale(Context context, Locale locale) {
        LanguageSpUtil.saveLocaleLanguageAndCountry(context, locale.getLanguage(), locale.getCountry());
    }

    /**
     * 是否是相同的Local
     *
     * @param appLocale
     * @param sp_language
     * @param sp_country
     * @return
     */
    public static boolean isSameLocal(Locale appLocale, String sp_language, String sp_country) {
        String appLanguage = appLocale.getLanguage();
        String appCountry = appLocale.getCountry();
        if (appLanguage.equals(sp_language) && appCountry.equals(sp_country)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断sp中和app中的多语言信息是否相同
     *
     * @param context
     * @return
     */
    public static boolean isSameWithSetting(Context context) {
        Locale locale = getContextLocale(context);
        Locale settingLocale = LanguageSpUtil.getSettingLocale(context);
        if (locale.equals(settingLocale)) {
            return true;
        } else {
            return false;
        }
    }


    //在Application实现类注册Activity生命周期监听回调
    //registerActivityLifecycleCallbacks(callbacks);
    public static Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            String localeLanguage = LanguageSpUtil.getLocaleLanguage(activity);
            String localeCountry = LanguageSpUtil.getLocaleCountry(activity);
            if (!TextUtils.isEmpty(localeLanguage) || !TextUtils.isEmpty(localeCountry)) {
                //强制修改应用语言
                if (!isSameWithSetting(activity)) {
                    Locale locale = new Locale(localeLanguage, localeCountry);
                    setAppLanguage(activity, locale);
                }
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    };
}


