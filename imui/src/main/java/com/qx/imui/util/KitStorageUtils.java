package com.qx.imui.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.qx.imlib.qlog.QLog;
import com.qx.imui.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

import androidx.annotation.StringRes;

public class KitStorageUtils {

    private static final String TAG = "KitStorageUtils";
    private static final String IMAGE_PREFIX = "QXIM_Image_";
    private static final String VIDEO_PREFIX = "QXIM_Video_";
    private static final String VIDEO_SUFFIX = ".mp4";

    public KitStorageUtils() {
    }

    public static boolean isQMode(Context context) {
        return LibStorageUtils.isQMode(context);
    }

    public static boolean isBuildAndTargetForQ(Context context) {
        return LibStorageUtils.isBuildAndTargetForQ(context);
    }

    public static String getImageSavePath(Context context) {
        return getSavePath(context, "image", R.string.qx_image_default_saved_path);
    }

    public static String getVideoSavePath(Context context) {
        return getSavePath(context, "video", R.string.qx_video_default_saved_path);
    }

    public static String getFileSavePath(Context context) {
        return getSavePath(context, "file", R.string.qx_file_default_saved_path);
    }

    public static String getSavePath(Context context, String type, @StringRes int res) {
        if (!SavePathUtils.isSavePathEmpty()) {
            String savePath = SavePathUtils.getSavePath();
            File imageDir = new File(savePath, type);
            if (!imageDir.exists() && !imageDir.mkdirs()) {
                QLog.e(TAG, "getSavePath mkdirs error path is  " + imageDir.getAbsolutePath());
            }

            return imageDir.getAbsolutePath();
        } else {
            boolean sdCardExist = Environment.getExternalStorageState().equals("mounted");
            String result = context.getCacheDir().getPath();
            if (!sdCardExist) {
                QLog.e(TAG,"getSavePath error, sdcard does not exist.");
                return result;
            } else {
                if (isQMode(context)) {
                    File path = context.getExternalFilesDir("QXIM");
                    File file = new File(path, type);
                    if (!file.exists() && !file.mkdirs()) {
                        result = path.getPath();
                    } else {
                        result = file.getPath();
                    }
                } else {
                    String path = Environment.getExternalStorageDirectory().getPath();
                    String defaultPath = context.getString(res);
                    StringBuilder builder = new StringBuilder(defaultPath);
                    String appName = LibStorageUtils.getAppName(context);
                    if (!TextUtils.isEmpty(appName)) {
                        builder.append(appName).append(File.separator);
                    }

                    String appPath = builder.toString();
                    path = path + appPath;
                    File dir = new File(path);
                    if (!dir.exists() && !dir.mkdirs()) {
                        QLog.e(TAG, "mkdirs error path is  " + path);
                        result = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    } else {
                        result = path;
                    }
                }

                return result;
            }
        }
    }

    private static boolean copyVideoToPublicDir(Context context, File file, String outputFileName) {
        if (file != null && file.exists()) {
            boolean result = true;
            if (!isBuildAndTargetForQ(context)) {
                File dirFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                if (dirFile != null && !dirFile.exists()) {
                    boolean mkdirResult = dirFile.mkdirs();
                    if (!mkdirResult) {
                        QLog.e(TAG,"mkdir fail,dir path is " + dirFile.getAbsolutePath());
                        return false;
                    }
                }

                if (dirFile == null) {
                    QLog.e(TAG,"dirFile is null");
                    return false;
                }

                FileInputStream fis = null;
                FileOutputStream fos = null;

                try {
                    String name;
                    String suffix;
                    if (!TextUtils.isEmpty(outputFileName)) {
                        name = outputFileName;
                    } else {
                        name = "QXIM_Video_" + System.currentTimeMillis();
                        suffix = FileUtils.getSuffix(file.getName());
                        if (suffix != null) {
                            name = name + "." + suffix;
                        } else {
                            name = name + ".mp4";
                        }
                    }

                    suffix = dirFile.getPath() + "/" + name;
                    fis = new FileInputStream(file);
                    fos = new FileOutputStream(suffix);
                    copy(fis, fos);
                    File destFile = new File(suffix);
                    updatePhotoMedia(destFile, context);
                } catch (FileNotFoundException var22) {
                    result = false;
                    QLog.e(TAG,"copyVideoToPublicDir file not found");
                } finally {
                    try {
                        if (fis != null) {
                            fis.close();
                        }
                    } catch (IOException var21) {
                        var21.printStackTrace();
                        QLog.e(TAG,"copyVideoToPublicDir: "+var21.toString());
                    }

                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException var20) {
                        var20.printStackTrace();
                        QLog.e(TAG,"copyVideoToPublicDir: "+ var20.toString());
                    }

                }
            } else {
                result = copyVideoToPublicDirForQ(context, file, outputFileName);
            }

            return result;
        } else {
            QLog.e(TAG,"file is not exist");
            return false;
        }
    }

    public static void updatePhotoMedia(File file, Context context) {
        if (file != null && file.exists()) {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
            intent.setData(Uri.fromFile(file));
            context.sendBroadcast(intent);
        }

    }

    private static boolean copyVideoToPublicDirForQ(Context context, File file, String outputFileName) {
        boolean result = true;
        String filePath = "";
        if (file.exists() && file.isFile() && context != null) {
            String name;
            if (!TextUtils.isEmpty(outputFileName)) {
                name = outputFileName;
            } else {
                name = file.getName();
            }

            Uri uri = insertVideoIntoMediaStore(context, name);
            if (uri != null) {
                filePath = uri.getPath();
            }

            try {
                ParcelFileDescriptor w = context.getContentResolver().openFileDescriptor(uri, "w");
                writeToPublicDir(file, w);
            } catch (FileNotFoundException var8) {
                var8.printStackTrace();
                QLog.e(TAG,"copyVideoToPublicDir uri is not Found, uri is" + uri.toString());
                result = false;
            }

            File destFile = new File(filePath);
            updatePhotoMedia(destFile, context);
        } else {
            QLog.e(TAG, "file is not Found or context is null ");
            result = false;
        }

        return result;
    }

    private static boolean copyImageToPublicDir(Context pContext, File pFile, String outputFileName) {
        boolean result = true;
        File file = pFile;
        if (pFile.exists() && pFile.isFile() && pContext != null) {
            if (!isBuildAndTargetForQ(pContext)) {
                File dirFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                if (dirFile != null && !dirFile.exists()) {
                    boolean mkdirResult = dirFile.mkdirs();
                    if (!mkdirResult) {
                        QLog.e(TAG, "mkdir fail,dir path is " + dirFile.getAbsolutePath());
                        return false;
                    }
                }

                if (dirFile == null) {
                    QLog.e(TAG, "dirFile is null");
                    return false;
                }

                FileInputStream fis = null;
                FileOutputStream fos = null;

                try {
                    String name;
                    String imgMimeType;
                    if (!TextUtils.isEmpty(outputFileName)) {
                        name = outputFileName;
                    } else {
                        imgMimeType = getImgMimeType(file);
                        int i = imgMimeType.lastIndexOf(47);
                        name = "QXIM_Image_" + System.currentTimeMillis();
                        if (i != -1) {
                            name = name + "." + imgMimeType.substring(i + 1);
                        }
                    }

                    imgMimeType = dirFile.getPath() + "/" + name;
                    fis = new FileInputStream(file);
                    fos = new FileOutputStream(imgMimeType);
                    copy(fis, fos);
                    File destFile = new File(imgMimeType);
                    updatePhotoMedia(destFile, pContext);
                } catch (FileNotFoundException var25) {
                    var25.printStackTrace();
                    result = false;
                    QLog.e(TAG, "copyImageToPublicDir file not found"+ var25);
                } finally {
                    try {
                        if (fis != null) {
                            fis.close();
                        }
                    } catch (IOException var23) {
                        var23.printStackTrace();
                        QLog.e(TAG, "copyImageToPublicDir: "+ var23.toString());
                    }

                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException var22) {
                        var22.printStackTrace();
                        QLog.e(TAG,"copyImageToPublicDir: "+ var22.toString());
                    }

                }
            } else {
                String imgMimeType = getImgMimeType(pFile);
                String name;
                if (!TextUtils.isEmpty(outputFileName)) {
                    name = outputFileName;
                } else {
                    int i = imgMimeType.lastIndexOf(47);
                    name = "QXIM_Image_" + System.currentTimeMillis();
                    if (i != -1) {
                        name = name + "." + imgMimeType.substring(i + 1);
                    }
                }

                Uri uri = insertImageIntoMediaStore(pContext, name, imgMimeType);

                try {
                    ParcelFileDescriptor w = pContext.getContentResolver().openFileDescriptor(uri, "w");
                    writeToPublicDir(file, w);
                } catch (FileNotFoundException var24) {
                    var24.printStackTrace();
                    result = false;
                    QLog.e(TAG, "copyImageToPublicDir uri is not Found, uri is" + uri.toString());
                }
            }
        } else {
            result = false;
            QLog.e(TAG, "file is not Found or context is null ");
        }

        return result;
    }

    public static Uri insertImageIntoMediaStore(Context context, String fileName, String mimeType) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("_display_name", fileName);
        contentValues.put("datetaken", System.currentTimeMillis());
        contentValues.put("mime_type", mimeType);
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        return uri;
    }

    public static Uri insertVideoIntoMediaStore(Context context, String fileName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("_display_name", fileName);
        contentValues.put("datetaken", System.currentTimeMillis());
        contentValues.put("mime_type", "video/mp4");
        Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        return uri;
    }

    public static void writeToPublicDir(File pFile, ParcelFileDescriptor pParcelFileDescriptor) {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(pFile);
            fos = new FileOutputStream(pParcelFileDescriptor.getFileDescriptor());
            copy(fis, fos);
        } catch (FileNotFoundException var17) {
            var17.printStackTrace();
            QLog.e(TAG, "writeToPublicDir file is not found file path is " + pFile.getAbsolutePath());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException var16) {
                var16.printStackTrace();
                QLog.e(TAG, "writeToPublicDir: "+var16.toString());
            }

            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException var15) {
                var15.printStackTrace();
                QLog.e(TAG,"writeToPublicDir: "+ var15.toString());
            }

        }

    }

    public static void read(ParcelFileDescriptor parcelFileDescriptor, File dst) throws IOException {
        FileInputStream istream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());

        try {
            FileOutputStream ostream = new FileOutputStream(dst);

            try {
                copy(istream, ostream);
            } finally {
                ostream.close();
            }
        } finally {
            istream.close();
        }

    }

    public static void copy(FileInputStream ist, FileOutputStream ost) {
        if (ist != null && ost != null) {
            FileChannel fileChannelInput = null;
            FileChannel fileChannelOutput = null;

            try {
                fileChannelInput = ist.getChannel();
                fileChannelOutput = ost.getChannel();
                fileChannelInput.transferTo(0L, fileChannelInput.size(), fileChannelOutput);
            } catch (IOException var13) {
                var13.printStackTrace();
                QLog.e(TAG,"copy method error"+ var13.toString());
            } finally {
                try {
                    ist.close();
                    if (fileChannelInput != null) {
                        fileChannelInput.close();
                    }

                    ost.close();
                    if (fileChannelOutput != null) {
                        fileChannelOutput.close();
                    }
                } catch (IOException var12) {
                    var12.printStackTrace();
                    QLog.e(TAG,"copy method error"+ var12.toString());
                }

            }

        }
    }

    public static String getImgMimeType(File imgFile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgFile.getPath(), options);
        return options.outMimeType;
    }

    public Uri getContentUri(int type, String id) {
        Uri uri;
        switch(type) {
            case 0:
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
                break;
            case 1:
                uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
                break;
            case 2:
                uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
                break;
            default:
                uri = null;
        }

        return uri;
    }

    public InputStream getFileInputStreamWithUri(Context pContext, Uri pUri) {
        InputStream inputStream = null;
        ContentResolver cr = pContext.getContentResolver();

        try {
            AssetFileDescriptor r = cr.openAssetFileDescriptor(pUri, "r");
            ParcelFileDescriptor parcelFileDescriptor = r.getParcelFileDescriptor();
            if (parcelFileDescriptor != null) {
                inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            }
        } catch (FileNotFoundException var7) {
            var7.printStackTrace();
            QLog.e(TAG, "getFileInputStreamWithUri: "+ var7);
        }

        return inputStream;
    }

    public static boolean saveMediaToPublicDir(Context context, File file, String type) {
        return saveMediaToPublicDir(context, file, (String)null, type);
    }

    public static boolean saveMediaToPublicDir(Context context, File file, String outputFileName, String type) {
        if ("image".equals(type)) {
            return copyImageToPublicDir(context, file, outputFileName);
        } else if ("video".equals(type)) {
            return copyVideoToPublicDir(context, file, outputFileName);
        } else {
            QLog.e(TAG,"type is error");
            return false;
        }
    }

    public static class MediaType {
        public static final String IMAGE = "image";
        public static final String VIDEO = "video";

        public MediaType() {
        }
    }
}
