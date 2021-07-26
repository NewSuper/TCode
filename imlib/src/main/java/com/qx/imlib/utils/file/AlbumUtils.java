package com.qx.imlib.utils.file;


import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

//图片或者视频保存到本地类型

public class AlbumUtils {

    /**
     * App默认的存储目录
     */
    private static final String DEFAULT_FILE_DIRECTORY = "QXIM";

    /**
     * 获取App存储目录
     * @return
     */
    private static String getExternalStorageDir(){
        String dirName = Environment.getExternalStorageDirectory() + "/"+DEFAULT_FILE_DIRECTORY+"/";
        File parentFile = new File(dirName);
        //不存在创建
        if (!parentFile.exists()) {
            parentFile.mkdir();
        }
        return dirName;
    }

    /**
     * 获取指定类型的存储目录
     * @param albumType
     * @return
     */
    private static String getAlbumDirNameBy(AlbumType albumType){
        switch (albumType){
            case TEMP:
                return "temp";
            case VIDEO:
                return "video";
            case IMAGE:
                return "image";
        }
        return "";
    }


    /**
     * 获取指定类型的存储目录
     * @param albumType 类型
     * @return
     */
    public static String getAlbumTypeDir(AlbumType albumType){
        String dirName = getExternalStorageDir()+getAlbumDirNameBy(albumType)+"/";
        File parentFile = new File(dirName);
        //不存在创建
        if (!parentFile.exists()) {
            parentFile.mkdir();
            //创建隐藏文件
            createNomedia(dirName);
        }
        return dirName;
    }


    /**
     * 组装一个指定类型+后缀名的完整文件路径
     * @param albumType 类型
     * @param suffixStr 后缀名例如：.mp4
     * @return
     */
    public static String getAlbumTypeFile(AlbumType albumType,String suffixStr){
        String prefixStr = "";
        switch (albumType){
            case TEMP:
                prefixStr =  "TEMP_";
                break;
            case VIDEO:
                prefixStr =  "VIDEO_";
                break;
            case IMAGE:
                prefixStr =  "IMG_";
                break;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return getAlbumTypeDir(albumType) + prefixStr + timeStamp + suffixStr;
    }


    /**
     * 组装一个指定类型+后缀名的完整文件路径
     * @param albumType 类型
     * @param originUrl 网络路径
     * @return
     */
    public static String getAlbumTypeFileByUrl(AlbumType albumType,String originUrl){
        String prefixStr = "";
        switch (albumType){
            case TEMP:
                prefixStr =  "TEMP_";
                break;
            case VIDEO:
                prefixStr =  "VIDEO_";
                break;
            case IMAGE:
                prefixStr =  "IMG_";
                break;
        }
        return getAlbumTypeDir(albumType) + prefixStr + createFileFromCache(originUrl);
    }


    /**
     * 获取文件后缀
     * @param url
     * @return
     */
    public static String getFileSuffix(String url) {
        String suffixStr = "";
        if(url.contains(".")) {
            String fileSuffix = url.substring(url.lastIndexOf("."), url.length());
            if(fileSuffix != null && fileSuffix.length() < 7) {
                suffixStr = fileSuffix;
            }
        }
        return suffixStr;
    }

    /**
     * 截取Url中文件名称+后缀
     * @param url
     * @return
     */
    public static String createFileFromCache(String url) {
        if(!TextUtils.isEmpty(url) && url.contains("/")){
            String fileName = url.substring(url.lastIndexOf("/") + 1, url.length());
            return fileName;
        }
        return null;
    }

    /**
     * 创建隐藏文件,不会在相册中显示
     * @param dirpath
     */
    private static void createNomedia(String dirpath){
        File nomedia = new File(dirpath ,".nomedia");
        try {
            if (!nomedia.exists())nomedia.createNewFile();
        } catch (IOException e) {
            Log.e("IOException", "exception in createNewFile() method");
        }
    }

    /**
     * 复制单个文件
     * @param oldPath String 原文件路径+文件名 如：data/user/0/com.test/files/abc.txt
     * @param newPath String 复制后路径+文件名 如：data/user/0/com.test/cache/abc.txt
     * @return boolean
     */
    public static boolean copyFile(String oldPath, String newPath) {
        boolean isCopySuccess = false;
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            File oldFile = new File(oldPath);
            if (!oldFile.exists()) {
                return false;
            } else if (!oldFile.isFile()) {
                return false;
            } else if (!oldFile.canRead()) {
                return false;
            }
            fileInputStream = new FileInputStream(oldPath);
            fileOutputStream = new FileOutputStream(newPath);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = fileInputStream.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            fileOutputStream.flush();
            isCopySuccess = true;
        } catch (Exception e) {
            Log.i("AlbumUtils",Log.getStackTraceString(e));
        }finally {
            try {
                if(fileInputStream != null){
                    fileInputStream.close();
                }
                if(fileOutputStream != null){
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                Log.i("AlbumUtils",Log.getStackTraceString(e));
            }
        }
        return isCopySuccess;
    }

}
