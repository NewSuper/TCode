package com.qx.push.platform.vivo;

import android.content.Context;
import android.util.Log;

import com.qx.push.PushConfig;
import com.qx.push.PushManager;
import com.qx.push.PushType;
import com.qx.push.platform.IPush;
import com.vivo.push.IPushActionListener;
import com.vivo.push.PushClient;


public class VIVOPush implements IPush {
    private final String TAG = VIVOPush.class.getSimpleName();
    public VIVOPush() {
    }
    @Override
    public void register(Context context, PushConfig pushConfig) {
        PushClient.getInstance(context.getApplicationContext()).initialize();
        PushClient.getInstance(context.getApplicationContext()).turnOnPush(new IPushActionListener() {
            public void onStateChanged(int i) {
                Log.d(VIVOPush.this.TAG, "Vivo push onStateChanged:" + i);
                if (i == 0) {
                    PushManager.getInstance().onReceiveToken(context, PushType.VIVO, PushClient.getInstance(context.getApplicationContext()).getRegId());
                } else if (i == 101) {
                    PushManager.getInstance().onErrorResponse(context, PushType.VIVO, "request_token", 101);
                } else {
                    PushManager.getInstance().onErrorResponse(context, PushType.VIVO, "request_token", (long)i);
                }

            }
        });
    }
}
