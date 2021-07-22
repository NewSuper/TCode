package com.qx.push.platform.meizu;

import android.content.Context;
import android.text.TextUtils;

import com.meizu.cloud.pushsdk.PushManager;
import com.qx.push.PushConfig;
import com.qx.push.PushType;
import com.qx.push.platform.IPush;


public class MZPush implements IPush {
    @Override
    public void register(Context context, PushConfig pushConfig) {
        if (!TextUtils.isEmpty(pushConfig.getMzAppId()) && !TextUtils.isEmpty(pushConfig.getMzAppKey())) {
            String pushId = PushManager.getPushId(context);
            if (TextUtils.isEmpty(pushId)) {
                PushManager.register(context, pushConfig.getMzAppId(), pushConfig.getMzAppKey());
            } else {
              com.qx.push.PushManager.getInstance().onReceiveToken(context, PushType.MEIZU, pushId);
            }

        } else {
           com.qx.push.PushManager.getInstance().onErrorResponse(context, PushType.MEIZU, "request_token", 1);
        }
    }
}
