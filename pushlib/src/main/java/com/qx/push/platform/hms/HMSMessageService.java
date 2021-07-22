package com.qx.push.platform.hms;

import android.util.Log;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.qx.push.PushManager;
import com.qx.push.PushType;

public class HMSMessageService extends HmsMessageService {


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.e("HMSMessageService","onMessageReceived:"+remoteMessage.toString());
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("HMSMessageService","onNewToken:"+s);
        PushManager.getInstance().onReceiveToken(this, PushType.HUAWEI, s);
    }

}
