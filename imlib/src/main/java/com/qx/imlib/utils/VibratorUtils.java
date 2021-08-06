package com.qx.imlib.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Vibrator;

public class VibratorUtils {

    public static void cancel(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void startVibrate(Context context, long[] pattern, boolean isRepat) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM) //key
                        .build();
                vibrator.vibrate(pattern, isRepat ? 1 : 0, audioAttributes);
            } else {
                vibrator.vibrate(pattern, isRepat ? 1 : 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

