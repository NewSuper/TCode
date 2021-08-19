package com.qx.imui.util.language;


import android.content.Context;
import android.content.SharedPreferences;

import com.qx.imui.BaseApplication;

import java.util.Locale;

public class LanguageSpUtil {

    private static String name = "sp_config";
    private static SharedPreferences sp;

    public static final String LOCALE_LANGUAGE = "locale_language";
    public static final String LOCALE_COUNTRY = "locale_country";

    private static SharedPreferences getSp(Context context) {
        if (sp == null) {
            sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        }
        return sp;
    }

    public static String getString(Context context, String key) {
        SharedPreferences sp = getSp(context);
        return sp.getString(key, "");
    }

    public static String getLocaleLanguage(Context context) {
        SharedPreferences sp = getSp(context);
        return sp.getString(LOCALE_LANGUAGE, "");
    }

    public static String getLocaleCountry(Context context) {
        SharedPreferences sp = getSp(context);
        return sp.getString(LOCALE_COUNTRY, "");
    }

    public static Locale getSettingLocale(Context context) {
        return new Locale(getLocaleLanguage(context), getLocaleCountry(context));
    }

    public static void saveLocaleLanguageAndCountry(Context context, String language, String country) {
        SharedPreferences sp = getSp(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(LOCALE_LANGUAGE, language);
        editor.putString(LOCALE_COUNTRY, country);
        editor.commit();
    }

    public static LanguageType getLanguageType() {
        LanguageType languageType = LanguageType.TRADITIONAL_CHINESE;
        Locale locale = MultiLanguageUtil.getAppSettingLocal(BaseApplication.Companion.getInstance());
        if (locale.equals(Locale.SIMPLIFIED_CHINESE)) {
            languageType = LanguageType.SIMPLIFIED_CHINESE;
        }
        if (locale.equals(Locale.TRADITIONAL_CHINESE)) {
            languageType = LanguageType.TRADITIONAL_CHINESE;
        }
        if (locale.equals(Locale.US)) {
            languageType = LanguageType.US;
        }
        if (locale.equals(Locale.KOREA)) {
            languageType = LanguageType.KOREA;
        }
        if (locale.equals(Locale.JAPAN)) {
            languageType = LanguageType.JAPAN;
        }
        return languageType;
    }
}