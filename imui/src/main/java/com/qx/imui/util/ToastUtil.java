package com.qx.imui.util;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {

    public static void toast(Context context, String content) {
        Toast.makeText(context, content, Toast.LENGTH_LONG).show();
    }

    public static void toast(Context context,int resId) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_LONG).show();
    }
}

