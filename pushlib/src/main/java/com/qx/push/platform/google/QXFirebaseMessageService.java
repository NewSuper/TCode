package com.qx.push.platform.google;


import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.qx.push.PushManager;
import com.qx.push.PushType;

import org.json.JSONException;
import org.json.JSONObject;

public class QXFirebaseMessageService extends FirebaseMessagingService {

    private  static final String TAG = "FirebaseMessageService";

    public QXFirebaseMessageService() {
    }

    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (null != remoteMessage) {
            try {
                Log.d(TAG,"onMessageReceived:"+remoteMessage.toString());
                JSONObject json = new JSONObject(remoteMessage.getData());
                String message = json.getString("message");
                PushManager.getInstance().onPushRawData(this, PushType.GOOGLE_FCM, message);
            } catch (JSONException var4) {
                var4.printStackTrace();
            }

        }
    }

    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.d(TAG,"onNewToken:"+s);
        PushManager.getInstance().onReceiveToken(this, PushType.GOOGLE_FCM, s);
    }
}
