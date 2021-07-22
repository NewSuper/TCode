package com.qx.push;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.qx.push.platform.IPush;

public class QXPushClient {

    private static final String TAG = QXPushClient.class.getSimpleName();
    private static PushConfig pushConfig;
    private String serverHost = "";

    public static void setPushConfig(PushConfig config) {
        pushConfig = config;
    }

    public static void init(Context context, String appKey) {
        if (TextUtils.isEmpty(appKey)) {
//            Log.e(TAG," QXPushClient init appKey:"+appKey);
        } else {
            if (pushConfig != null) {
                pushConfig.setAppKey(appKey);
            } else {
                pushConfig = (new PushConfig.Builder()).enableFCM(true).build();
                pushConfig.setAppKey(appKey);
            }
//            Log.d(TAG," init appKey:"+appKey);
            PushManager.getInstance().init(context,pushConfig);
        }
    }

    public static void updateIMToken(Context context,String token,String host,String rsaKey){
        PushManager.getInstance().updateIMToken(context,token,host,rsaKey);
    }

    public static void resolveHMSCoreUpdate(Activity activity) throws IllegalStateException {
        if (activity == null) {
            throw new IllegalStateException("resolve HWPush Error activity is null !");
        } else {
            PushType currentPushType = PushManager.getInstance().getServerPushType();
            if ( currentPushType != null && currentPushType.getName().equals(PushType.HUAWEI.getName())) {
                IPush hwPush = PushManager.getInstance().getRegisteredPush(PushType.HUAWEI.getName());
                if (hwPush != null) {
                    hwPush.register(activity, PushManager.getInstance().getPushConfig());
                } else {
                    Log.e(TAG, "no register HWPush");
                }
            } else {
                Log.i(TAG, "current pushType is " + currentPushType);
            }

        }
    }

    public static enum ConversationType {
        PRIVATE(0,"PRIVATE"),
        GROUP(1,"GROUP"),
        CHATROOM(2,"CHATROOM"),
        SYSTEM(3,"SYSTEM");
        private int value = 0;
        private String name = "";

        private ConversationType(int value,String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return this.value;
        }

        public String getName() {
            return this.name;
        }

        public static ConversationType setValue(int code) {
            ConversationType[] conversationTypes = values();
            int length = conversationTypes.length;
            for(int i = 0; i < length; ++i) {
                ConversationType c = conversationTypes[i];
                if (code == c.getValue()) {
                    return c;
                }
            }
            return PRIVATE;
        }

        public static ConversationType setName(String name) {
            ConversationType[] conversationTypes = values();
            int length = conversationTypes.length;
            for(int i = 0; i < length; ++i) {
                ConversationType c = conversationTypes[i];
                if (name.equalsIgnoreCase(c.getName())) {
                    return c;
                }
            }
            return PRIVATE;
        }

    }

    public static void sendNotification(Context context, PushNotificationMessage notificationMessage) {
        String packageName = context.getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            Log.e(TAG, "package name can't empty!");
        } else if (null == notificationMessage) {
            Log.e(TAG, "notificationMessage  can't be  null!");
        } else {
            Intent intent = new Intent();
            intent.setAction("qxim.push.intent.MESSAGE_ARRIVED");
            intent.setPackage(packageName);
            intent.putExtra("pushType", PushType.QX.getName());
            intent.putExtra("message", notificationMessage);
            if (Build.VERSION.SDK_INT >= 12) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            context.sendBroadcast(intent);
        }
    }

    public static void sendNotification(Context context, PushNotificationMessage notificationMessage, int left) {
        String packageName = context.getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            Log.e(TAG, "package name can't empty!");
        } else if (null == notificationMessage) {
            Log.e(TAG, "notificationMessage  can't be  null!");
        } else {
            Intent intent = new Intent();
            intent.setAction("qxim.push.intent.MESSAGE_ARRIVED");
            intent.setPackage(packageName);
            intent.putExtra("pushType",  PushType.QX.getName());
            intent.putExtra("message", notificationMessage);
            intent.putExtra("left", left);
            if (Build.VERSION.SDK_INT >= 12) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            context.sendBroadcast(intent);
        }
    }

    public static void setNotifiationSound(Uri uri) {
        QXNotificationInterface.setNotificationSound(uri);
    }
}
