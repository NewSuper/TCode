package com.qx.imui.util;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.qx.imlib.qlog.QLog;
import com.qx.imui.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.AppOpsManagerCompat;
import androidx.fragment.app.Fragment;

public class PermissionCheckUtil {

    private static final String TAG = PermissionCheckUtil.class.getSimpleName();

    public PermissionCheckUtil() {
    }

    public static boolean requestPermissions(Fragment fragment, String[] permissions) {
        return requestPermissions((Fragment)fragment, permissions, 0);
    }

    public static boolean requestPermissions(final Fragment fragment, String[] permissions, int requestCode) {
        if (permissions.length != 0 && fragment.getActivity() != null && !fragment.isDetached()) {
            List<String> permissionsNotGranted = new ArrayList();
            boolean result = false;
            String[] var5 = permissions;
            int var6 = permissions.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                String permission = var5[var7];
                if ((isFlyme() || Build.VERSION.SDK_INT < 23) && permission.equals("android.permission.RECORD_AUDIO")) {
                    showPermissionAlert(fragment.getContext(), fragment.getString(R.string.qx_permission_grant_needed) + fragment.getString(R.string.qx_permission_microphone), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (-1 == which) {
                                fragment.startActivity(new Intent("android.settings.MANAGE_APPLICATIONS_SETTINGS"));
                            }

                        }
                    });
                    return false;
                }

                if (!hasPermission(fragment.getActivity(), permission)) {
                    permissionsNotGranted.add(permission);
                }
            }

            if (permissionsNotGranted.size() > 0) {
                int size = permissionsNotGranted.size();
                fragment.requestPermissions((String[])permissionsNotGranted.toArray(new String[size]), requestCode);
            } else {
                result = true;
            }

            return result;
        } else {
            return true;
        }
    }

    public static boolean requestPermissions(Activity activity, @NonNull String[] permissions) {
        return requestPermissions((Activity)activity, permissions, 0);
    }

    @TargetApi(23)
    public static boolean requestPermissions(Activity activity, @NonNull String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        } else if (permissions.length == 0) {
            return true;
        } else {
            List<String> permissionsNotGranted = new ArrayList();
            boolean result = false;
            String[] var5 = permissions;
            int var6 = permissions.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                String permission = var5[var7];
                if (!hasPermission(activity, permission)) {
                    permissionsNotGranted.add(permission);
                }
            }

            if (permissionsNotGranted.size() > 0) {
                int size = permissionsNotGranted.size();
                activity.requestPermissions(permissionsNotGranted.toArray(new String[size]), requestCode);
            } else {
                result = true;
            }

            return result;
        }
    }

    public static boolean checkPermissions(Context context, @NonNull String[] permissions) {
        if (permissions.length == 0) {
            return true;
        } else {
            String[] var2 = permissions;
            int var3 = permissions.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                String permission = var2[var4];
                if ((isFlyme() || Build.VERSION.SDK_INT < 23) && permission.equals("android.permission.RECORD_AUDIO")) {
                    QLog.e(TAG, "Build.MODEL = " + Build.MODEL);
                    if (Build.BRAND.toLowerCase().equals("meizu") && Build.MODEL.equals("M1852")) {
                        return hasPermission(context, permission);
                    }

                    if (!hasRecordPermision(context)) {
                        return false;
                    }
                } else if (!hasPermission(context, permission)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static boolean isFlyme() {
        String osString = "";

        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method get = clz.getMethod("get", String.class, String.class);
            osString = (String)get.invoke(clz, "ro.build.display.id", "");
        } catch (Exception var3) {
            QLog.e(TAG, "isFlyme");
        }

        return osString != null && osString.toLowerCase().contains("flyme");
    }

    private static boolean hasRecordPermision(Context context) {
        boolean hasPermission = false;
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(44100, 12, 2);
        if (bufferSizeInBytes < 0) {
            QLog.e(TAG,"bufferSizeInBytes = " + bufferSizeInBytes);
            return false;
        } else {
            AudioRecord audioRecord = null;

            try {
                audioRecord = new AudioRecord(1, 44100, 12, 2, bufferSizeInBytes);
                audioRecord.startRecording();
            } catch (Exception var5) {
                QLog.e(TAG,"hasRecordPermision");
                hasPermission = false;
            }

            if (audioRecord == null) {
                QLog.e(TAG,"audioRecord is null");
                return false;
            } else {
                if (audioRecord.getRecordingState() == 3) {
                    audioRecord.stop();
                    hasPermission = true;
                }

                audioRecord.release();
                return hasPermission;
            }
        }
    }

    public static String getNotGrantedPermissionMsg(Context context, String[] permissions, int[] grantResults) {
        if (permissions != null && permissions.length != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(context.getResources().getString(R.string.qx_permission_grant_needed));
            sb.append("(");

            try {
                for(int i = 0; i < permissions.length; ++i) {
                    if (grantResults[i] == -1) {
                        String permissionName = context.getString(context.getResources().getIdentifier("rc_" + permissions[i], "string", context.getPackageName()), new Object[]{0});
                        sb.append(permissionName);
                        if (i != permissions.length - 1) {
                            sb.append(" ");
                        }
                    }
                }
            } catch (Resources.NotFoundException var6) {
                QLog.e(TAG,"One of the permissions is not recognized by SDK." + Arrays.toString(permissions));
                return "";
            }

            sb.append(")");
            return sb.toString();
        } else {
            return "";
        }
    }

    private static String getNotGrantedPermissionMsg(Context context, List<String> permissions) {
        if (permissions != null && permissions.size() != 0) {
            HashSet permissionsValue = new HashSet();

            try {
                Iterator var4 = permissions.iterator();

                while(var4.hasNext()) {
                    String permission = (String)var4.next();
                    String permissionValue = context.getString(context.getResources().getIdentifier("rc_" + permission, "string", context.getPackageName()), new Object[]{0});
                    permissionsValue.add(permissionValue);
                }
            } catch (Resources.NotFoundException var7) {
                QLog.e(TAG, "one of the permissions is not recognized by SDK." + permissions.toString());
                return "";
            }

            StringBuilder result = new StringBuilder("(");
            Iterator var9 = permissionsValue.iterator();

            while(var9.hasNext()) {
                String value = (String)var9.next();
                result.append(value).append(" ");
            }

            result = new StringBuilder(result.toString().trim() + ")");
            return result.toString();
        } else {
            return "";
        }
    }

    @TargetApi(11)
    private static void showPermissionAlert(Context context, String content, DialogInterface.OnClickListener listener) {
        (new AlertDialog.Builder(context)).setMessage(content).setPositiveButton(R.string.qx_confirm, listener).setNegativeButton(R.string.qx_cancel, listener).setCancelable(false).create().show();
    }

    @TargetApi(19)
    public static boolean canDrawOverlays(Context context) {
        return canDrawOverlays(context, true);
    }

    @TargetApi(19)
    public static boolean canDrawOverlays(final Context context, boolean needOpenPermissionSetting) {
        boolean result = true;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                boolean booleanValue = (Boolean) Settings.class.getDeclaredMethod("canDrawOverlays", Context.class).invoke((Object)null, context);
                if (!booleanValue && needOpenPermissionSetting) {
                    ArrayList<String> permissionList = new ArrayList();
                    permissionList.add("android.settings.action.MANAGE_OVERLAY_PERMISSION");
                    showPermissionAlert(context, context.getString(R.string.qx_permission_grant_needed) + getNotGrantedPermissionMsg(context, permissionList), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (-1 == which) {
                                Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + context.getPackageName()));
                                context.startActivity(intent);
                            }

                        }
                    });
                }

                QLog.e(TAG,  "isFloatWindowOpAllowed allowed: " + booleanValue);
                return booleanValue;
            } catch (Exception var7) {
                QLog.e(TAG,  String.format("getDeclaredMethod:canDrawOverlays! Error:%s, etype:%s", var7.getMessage(), var7.getClass().getCanonicalName()));
                return true;
            }
        } else if (Build.VERSION.SDK_INT < 19) {
            return true;
        } else {
            Object systemService = context.getSystemService(Context.APP_OPS_SERVICE);

            Method method;
            try {
                method = Class.forName("android.app.AppOpsManager").getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
            } catch (NoSuchMethodException var8) {
                QLog.e(TAG,  String.format("NoSuchMethodException method:checkOp! Error:%s"));
                method = null;
            } catch (ClassNotFoundException var9) {
                QLog.e(TAG, "canDrawOverlays");
                method = null;
            }

            if (method != null) {
                try {
                    Integer tmp = (Integer)method.invoke(systemService, 24, context.getApplicationInfo().uid, context.getPackageName());
                    result = tmp != null && tmp == 0;
                } catch (Exception var10) {
                    QLog.e(TAG,  String.format("call checkOp failed: %s etype:%s"));
                }
            }

            QLog.e(TAG, "isFloatWindowOpAllowed allowed: " + result);
            return result;
        }
    }

    private static boolean hasPermission(Context context, String permission) {
        String opStr = AppOpsManagerCompat.permissionToOp(permission);
        if (opStr == null) {
            return true;
        } else {
            return context == null ? false : context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static void showRequestPermissionFailedAlter(final Context context, String content) {
        if (!TextUtils.isEmpty(content)) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    switch(which) {
                        case -1:
                            Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                            Uri uri = Uri.fromParts("package", context.getPackageName(), (String)null);
                            intent.setData(uri);
                            context.startActivity(intent);
                        case -2:
                        default:
                    }
                }
            };
            if (Build.VERSION.SDK_INT >= 21) {
                (new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog)).setMessage(content).setPositiveButton(R.string.qx_confirm, listener).setNegativeButton(R.string.qx_cancel, listener).setCancelable(false).create().show();
            } else {
                (new AlertDialog.Builder(context)).setMessage(content).setPositiveButton(R.string.qx_confirm, listener).setNegativeButton(R.string.qx_cancel, listener).setCancelable(false).create().show();
            }

        }
    }
}
