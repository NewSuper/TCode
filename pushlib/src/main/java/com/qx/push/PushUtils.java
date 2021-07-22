package com.qx.push;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class PushUtils {

    public static PushType getPreferPushType(Context context, PushConfig pushConfig) {
        String os = getDeviceManufacturer();
        Log.e("PushUtils","os:"+os);
        ArrayList<PushType> pushTypes = pushConfig.getEnabledPushTypes();
        if (os.contains("Xiaomi") && pushTypes.contains(PushType.XIAOMI)) {
            return PushType.XIAOMI;
        } else if (os.contains("HUAWEI") && pushTypes.contains(PushType.HUAWEI)) {
            return PushType.HUAWEI;
        } else if (os.contains("Meizu") && pushTypes.contains(PushType.MEIZU)) {
            return PushType.MEIZU;
        } else if (os.toLowerCase().contains("oppo") && pushTypes.contains(PushType.OPPO)) {
            return PushType.OPPO;
        } else if (os.contains("VIVO") && pushTypes.contains(PushType.VIVO)) {
            return PushType.VIVO;
        } else if (pushTypes.contains(PushType.GOOGLE_FCM)){
            return  PushType.GOOGLE_FCM;
        } else {
            return PushType.UNKNOWN;
        }
    }

    public static PushNotificationMessage transformToPushMessage(String jsonStr) {
        try {
            Log.e("PushUtils", jsonStr);
            JSONObject jsonObject = new JSONObject(jsonStr);
            PushNotificationMessage pushNotificationMessage = new PushNotificationMessage();
            String sendType = jsonObject.optString("sendType");
            QXPushClient.ConversationType conversationType = QXPushClient.ConversationType.setName(sendType);
            pushNotificationMessage.setConversationType(conversationType);
            pushNotificationMessage.setMessageType(jsonObject.optString("messageType"));
            pushNotificationMessage.setTargetId(jsonObject.optString("senderId"));
            pushNotificationMessage.setPushFlag("true");
            Log.e("PushUtils", pushNotificationMessage.toString());
            return pushNotificationMessage;
        } catch (JSONException jsonException) {
            Log.e("PushUtils", jsonException.getMessage());
            return null;
        }
    }

    public static long checkPlayServices(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        return (long)apiAvailability.isGooglePlayServicesAvailable(context);
    }

    public static String getDeviceManufacturer() {
        String line = "";
        BufferedReader input = null;
        String propName = "ro.miui.ui.version.name";
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException exception) {
            exception.printStackTrace();
            Log.e("DeviceUtils", "Unable to read sysprop " + propName);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException var11) {
                }
            }

        }

        if (TextUtils.isEmpty(line)) {
            String manufacturer = Build.MANUFACTURER.replace("-", "_");
            if ("vivo".equals(manufacturer)) {
                manufacturer = manufacturer.toUpperCase();
            }

            return manufacturer;
        } else {
            return "Xiaomi";
        }
    }
}
