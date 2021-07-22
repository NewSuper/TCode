package com.qx.push.platform;

import android.content.Context;

import com.qx.push.PushConfig;

public interface IPush {
    void register(Context context, PushConfig pushConfig);
}
