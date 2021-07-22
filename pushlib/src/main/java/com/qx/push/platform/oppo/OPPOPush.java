package com.qx.push.platform.oppo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import com.heytap.msp.push.HeytapPushManager;
import com.heytap.msp.push.callback.ICallBackResultService;
import com.qx.push.PushConfig;
import com.qx.push.PushManager;
import com.qx.push.PushType;
import com.qx.push.platform.IPush;


public class OPPOPush implements IPush {
    private final String TAG = "OPPOPush";

    @Override
    public void register(Context context, PushConfig pushConfig) {
        HeytapPushManager.init(context, true);
        if (!HeytapPushManager.isSupportPush()) {
            Log.e(this.TAG, "the phone is not support oppo push!");
            PushManager.getInstance().onErrorResponse(context, PushType.OPPO, "request_token", 1);
        } else {
            Log.d(this.TAG, "Oppo push start to register");
            HeytapPushManager.register(context.getApplicationContext(), pushConfig.getOppoAppKey(), pushConfig.getOppoAppSecret(), new ICallBackResultService() {
                public void onRegister(int responseCode, String registerID) {
                    Log.d(TAG, "Oppo Push onRegister responseCode " + String.valueOf(responseCode) + ",registerID:" + registerID);
                    if (responseCode == 0) {
                        PushManager.getInstance().onReceiveToken(context, PushType.OPPO, registerID);
                    } else {
                        PushManager.getInstance().onErrorResponse(context, PushType.OPPO, "request_token", (long)responseCode);
                    }

                }

                public void onUnRegister(int responseCode) {
                    Log.d(TAG, "OPPO Push onUnRegister - responseCode:" + responseCode);
                }

                public void onSetPushTime(int responseCode, String pushTime) {
                    Log.d(TAG, "OPPO Push onSetPushTime - responseCode:" + responseCode + ",pushTime:" + pushTime);
                }

                public void onGetPushStatus(int responseCode, int status) {
                    Log.d(TAG, "OPPO Push onGetPushStatus - responseCode:" + responseCode + ",status:" + status);
                }

                public void onGetNotificationStatus(int responseCode, int status) {
                    Log.d(TAG, "OPPO Push onGetNotificationStatus - responseCode:" + responseCode + ",status:" + status);
                }
            });
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    int importance = NotificationManager.IMPORTANCE_DEFAULT;
                    String channelName = context.getResources().getString(context.getResources().getIdentifier("rc_notification_channel_name", "string", context.getPackageName()));
                    NotificationChannel notificationChannel = new NotificationChannel("im_push_notify", channelName, importance);
                    notificationChannel.enableLights(true);
                    notificationChannel.setLightColor(Color.BLUE);
                    nm.createNotificationChannel(notificationChannel);
                }
            }

        }
    }
}
