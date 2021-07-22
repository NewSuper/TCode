package com.qx.push.platform.xiaomi;

import android.content.Context;
import android.text.TextUtils;

import com.qx.push.PushConfig;
import com.qx.push.PushManager;
import com.qx.push.PushType;
import com.qx.push.platform.IPush;
import com.xiaomi.mipush.sdk.MiPushClient;


public class XMPush implements IPush {
    @Override
    public void register(Context context, PushConfig pushConfig) {
        if (!TextUtils.isEmpty(pushConfig.getMiAppId()) && !TextUtils.isEmpty(pushConfig.getMiAppKey())) {
            MiPushClient.registerPush(context, pushConfig.getMiAppId(), pushConfig.getMiAppKey());
        } else {
            PushManager.getInstance().onErrorResponse(context, PushType.XIAOMI, "request_token", 1);
        }
    }
}
