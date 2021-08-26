package com.qx.imui.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.Set;

public class PreferenceUtils {
    public static final String CONFIGS = "configs";
    public static SharedPreferences sp;
    /** 首页定位城市*/
    public static final String CITY_NAME = "city_name";
    /** 每次进入APP重新定位时候的标识-是否定位成功*/
    public static final String INTO_LOCATION = "into_location";
    /**
     * 得到SharedPreferences实例并初始化
     */
    public static SharedPreferences getPreferences(Context context) {
        if (sp == null) {
            sp = context.getSharedPreferences(CONFIGS, Context.MODE_PRIVATE);
        }
        return sp;
    }

    /**
     * @param devalue 默认值为false
     */
    public static boolean getBoolean(Context context, String key, boolean devalue) {
        SharedPreferences sp = getPreferences(context);
        return sp.getBoolean(key, devalue);
    }

    /**
     * 如果没有拿到,则返回默认值为false;
     */
    public static boolean getBoolean(Context context, String key) {
        return getBoolean(context, key, false);
    }

    /**
     * 设置值
     */
    public static void setBoolean(Context context, String key, boolean value) {
        SharedPreferences sp = getPreferences(context);
        Editor edit = sp.edit();
        edit.putBoolean(key, value);
        edit.commit();
    }

    /**
     * @param devalue 默认值为null
     */
    public static String getString(Context context, String key, String devalue) {
        SharedPreferences sp = getPreferences(context);
        return sp.getString(key, devalue);
    }

    /**
     * @param context
     * @param key
     * @return
     */
    public static String getString(Context context, String key) {
        return getString(context, key, "");
    }

    /**
     * 设置值
     */
    public static void setString(Context context, String key, String value) {
        SharedPreferences sp = getPreferences(context);
        Editor edit = sp.edit();
        edit.putString(key, value);
        edit.commit();
    }

    /**
     * @param devalue 默认值为null
     */
    public static Set<String> getSet(Context context, String key, Set<String> devalue) {
        SharedPreferences sp = getPreferences(context);
        return sp.getStringSet(key, devalue);
    }

    /**
     * @param context
     * @param key
     * @return
     */
    public static Set<String> getSet(Context context, String key) {
        return getSet(context, key, null);
    }

    /**
     * 设置值
     */
    public static void setSet(Context context, String key, Set<String> value) {
        SharedPreferences sp = getPreferences(context);
        Editor edit = sp.edit();
        edit.putStringSet(key, value);
        edit.commit();
    }

    /**
     * @param devalue 默认值为null
     */
    public static int getInt(Context context, String key, int devalue) {
        SharedPreferences sp = getPreferences(context);
        return sp.getInt(key, devalue);
    }

    /**
     * @param context
     * @param key
     * @return
     */
    public static int getInt(Context context, String key) {
        return getInt(context, key, 0);
    }

    /**
     * 设置值
     */
    public static void setInt(Context context, String key, int value) {
        SharedPreferences sp = getPreferences(context);
        Editor edit = sp.edit();
        edit.putInt(key, value);
        edit.commit();
    }

    /**
     * @param devalue 默认值为null
     */
    public static long getLong(Context context, String key, long devalue) {
        SharedPreferences sp = getPreferences(context);
        return sp.getLong(key, devalue);
    }

    /**
     * @param context
     * @param key
     * @return
     */
    public static long getLong(Context context, String key) {
        return getLong(context, key, 0);
    }

    /**
     * 设置值
     */
    public static void setLong(Context context, String key, long value) {
        SharedPreferences sp = getPreferences(context);
        Editor edit = sp.edit();
        edit.putLong(key, value);
        edit.commit();
    }


    /**
     * @param context
     * @param key
     * @return
     */
    public static long getFloat(Context context, String key) {
        return getFloat(context, key);
    }

    /**
     * 设置值
     */
    public static void setFloat(Context context, String key, Float value) {
        SharedPreferences sp = getPreferences(context);
        Editor edit = sp.edit();
        edit.putFloat(key, value);
        edit.commit();
    }

}
