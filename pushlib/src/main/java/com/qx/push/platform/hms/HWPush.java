package com.qx.push.platform.hms;


import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.qx.push.PushConfig;
import com.qx.push.PushManager;
import com.qx.push.PushType;
import com.qx.push.platform.IPush;

public class HWPush implements IPush {

    private static final String TAG = HWPush.class.getSimpleName();

    @Override
    public void register(Context context, PushConfig pushConfig) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            Log.e(TAG, "Huawei getMainLooper != ");
            this.action(context);
        } else {
            (new Thread(new Runnable() {
                public void run() {
                    HWPush.this.action(context);
                }
            })).start();
        }
    }

    public void action(Context context) {
        try {
            Log.e(TAG, "Huawei action>>>");
            String appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id");
            Log.e(TAG, "Huawei action appId:"+appId);
            String pushtoken = HmsInstanceId.getInstance(context).getToken(appId, "HCM");
            Log.e(TAG, "Huawei action appId:"+appId+",pushtoken:"+pushtoken);
            if (!TextUtils.isEmpty(pushtoken)) {
                PushManager.getInstance().onReceiveToken(context, PushType.HUAWEI, pushtoken);
            }
        } catch (ApiException apiException) {
            apiException.printStackTrace();
            PushManager.getInstance().onErrorResponse(context, PushType.HUAWEI, "request_token", (long)apiException.getStatusCode());
            Log.e(TAG, "getToken failed, " + apiException);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}