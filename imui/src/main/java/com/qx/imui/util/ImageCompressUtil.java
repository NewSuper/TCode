package com.qx.imui.util;

import android.content.Context;
import android.net.Uri;

import com.qx.imlib.utils.file.AlbumType;
import com.qx.imlib.utils.file.AlbumUtils;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class ImageCompressUtil {

    public static void compressImage(Context context, Uri uri, String targetPath, OnCompressListener listener) {
        Luban.with(context).load(uri).setTargetDir(targetPath).setCompressListener(listener).launch();
    }


    /**
     * 图片压缩
     * @param context
     * @param filePath 文件路径
     * @param listener 压缩回调
     */
    public static void compressImage(Context context, String filePath, OnCompressListener listener) {
        Luban.with(context)
                .load(filePath)
                .ignoreBy(200)  // 忽略不压缩图片的大小,默认是100
                .setTargetDir(AlbumUtils.getAlbumTypeDir(AlbumType.IMAGE)) //设置压缩后文件存储位置
                .setCompressListener(listener).launch();
    }

}
