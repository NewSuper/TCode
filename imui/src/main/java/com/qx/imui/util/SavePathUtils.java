package com.qx.imui.util;


import android.text.TextUtils;
import android.util.Log;

import java.io.File;

public class SavePathUtils {

    private static final String TAG = SavePathUtils.class.getSimpleName();
    private static String savePath = "";

    public SavePathUtils() {
    }

    public static String getSavePath() {
        return savePath;
    }

    public static String getSavePath(String defaultPath) {
        return isSavePathEmpty() ? defaultPath : savePath;
    }

    public static File getSavePath(File defaultFile) {
        return isSavePathEmpty() ? defaultFile : new File(savePath);
    }

    public static void setSavePath(String path) {
        if (isDir(path)) {
            savePath = path;
        }

    }

    public static boolean isSavePathEmpty() {
        return TextUtils.isEmpty(savePath);
    }

    public static boolean isDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            Log.e(TAG, "SavePath not exists..");
            return false;
        } else if (!file.isDirectory()) {
            Log.e(TAG, "SavePath not Directory..");
            return false;
        } else {
            return true;
        }
    }
}
