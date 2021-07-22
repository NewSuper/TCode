package com.qx.push.platform.xiaomi;

import android.content.Context;
import android.util.Log;

import com.qx.push.PushManager;
import com.qx.push.PushNotificationMessage;
import com.qx.push.PushType;
import com.qx.push.PushUtils;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

import java.util.List;

public class MiMessageReceiver extends PushMessageReceiver {
    private static final String TAG = MiMessageReceiver.class.getSimpleName();

    public MiMessageReceiver() {
    }

    public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
        Log.v(TAG, "onReceivePassThroughMessage is called. " + message.toString());
    }

    public void onNotificationMessageClicked(Context context, MiPushMessage message) {
        Log.v(TAG, "onNotificationMessageClicked is called. " + message.toString());
        PushNotificationMessage pushNotificationMessage = PushUtils.transformToPushMessage(message.getContent());
        PushManager.getInstance().onNotificationMessageClicked(context, PushType.XIAOMI, pushNotificationMessage);
    }

    public void onNotificationMessageArrived(Context context, MiPushMessage message) {
        Log.v(TAG, "onNotificationMessageArrived is called. " + message.toString());
        PushNotificationMessage pushNotificationMessage = PushUtils.transformToPushMessage(message.getContent());
        if (pushNotificationMessage != null) {
            PushManager.getInstance().onNotificationMessageArrived(context, PushType.XIAOMI, pushNotificationMessage);
        }

    }

    public void onCommandResult(Context context, MiPushCommandMessage message) {
        Log.v(TAG, "onCommandResult is called. " + message.toString());
    }

    public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
        String command = message.getCommand();
        List<String> arguments = message.getCommandArguments();
        String cmdArg1 = arguments != null && arguments.size() > 0 ? (String)arguments.get(0) : null;
        String cmdArg2 = arguments != null && arguments.size() > 1 ? (String)arguments.get(1) : null;
        Log.d(TAG, "onReceiveRegisterResult. cmdArg1: " + cmdArg1 + "; cmdArg2:" + cmdArg2);
        if ("register".equals(command)) {
            if (message.getResultCode() == 0L) {
                PushManager.getInstance().onReceiveToken(context, PushType.XIAOMI, cmdArg1);
            } else {
                PushManager.getInstance().onErrorResponse(context, PushType.XIAOMI, "request_token", message.getResultCode());
            }
        }

    }
}
