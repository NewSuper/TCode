package com.qx.push.platform.google;

import android.content.Context;

import com.google.firebase.messaging.FirebaseMessaging;
import com.qx.push.PushManager;
import com.qx.push.PushType;
import com.qx.push.PushUtils;
import com.qx.push.PushConfig;
import com.qx.push.platform.IPush;

public class FCMPush implements IPush {
    @Override
    public void register(Context context, PushConfig pushConfig) {
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        long result = PushUtils.checkPlayServices(context);
        if (result != 0L) {
            PushManager.getInstance().onErrorResponse(context, PushType.GOOGLE_FCM, "checkPlayServices", result);
        }
    }
}
