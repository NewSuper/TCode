package com.qx.push.platform;

import android.content.Context;

import com.qx.push.bean.PushConfig;

public interface IPush {
    void register(Context context, PushConfig pushConfig);
}
