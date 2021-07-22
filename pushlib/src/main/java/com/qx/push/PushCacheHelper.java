package com.qx.push;

import android.content.Context;
import android.content.SharedPreferences;


import static android.content.Context.MODE_PRIVATE;

public class PushCacheHelper {

    private final String QX_IM_PUSH_INFO = "QXPushAppConfig";
    private final String ADDRESS = "address";
    private final String RSAKEY = "rsakey";
    private final String IMTOKEN = "imtoken";
    private final String PUSHTYPE = "pushtype";
    private final String PUSHTOKEN = "pushtoken";

    public static PushCacheHelper getInstance() {
        return PushCacheHelper.Singleton.sInstance;
    }

    public void clearCache(Context context) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        sp.edit().clear().commit();
    }

    public void cacheAddress(Context context,String address) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        sp.edit().putString(ADDRESS,address).commit();
    }

    public String getCacheAddress(Context context) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        return sp.getString(ADDRESS,"");
    }

    public void cacheRsaKey(Context context,String rsaKey) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        sp.edit().putString(RSAKEY,rsaKey).commit();
    }

    public String getCacheRsaKey(Context context) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        return sp.getString(RSAKEY,"");
    }

    public void cacheIMToken(Context context,String imtoken) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        sp.edit().putString(IMTOKEN,imtoken).commit();
    }

    public String getCacheIMToken(Context context) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        return sp.getString(IMTOKEN,"");
    }

    public void cachePushToken(Context context, String pushToken) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        sp.edit().putString(PUSHTOKEN,pushToken).commit();
    }

    public String getPushToken(Context context) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        return sp.getString(PUSHTOKEN,"");
    }

    public void cachePushType(Context context,String pushType) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        sp.edit().putString(PUSHTYPE,pushType).commit();
    }

    public String getPushType(Context context) {
        SharedPreferences sp = SharedPreferencesUtils.get(context,QX_IM_PUSH_INFO,MODE_PRIVATE);
        return sp.getString(PUSHTYPE,"");
    }

    public void setPushContentShowStatus(Context context, boolean isShowDetail) {
        SharedPreferences sp = context.getSharedPreferences(QX_IM_PUSH_INFO, 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("isShowDetail", isShowDetail);
        editor.apply();
    }

    public boolean getPushContentShowStatus(Context context) {
        SharedPreferences sp = context.getSharedPreferences(QX_IM_PUSH_INFO, 0);
        return sp.getBoolean("isShowDetail", true);
    }

    private static class Singleton {
        static PushCacheHelper sInstance = new PushCacheHelper();
        private Singleton() {
        }
    }

}
