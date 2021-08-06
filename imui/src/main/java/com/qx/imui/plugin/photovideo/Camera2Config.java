package com.qx.imui.plugin.photovideo;

import com.qx.imlib.utils.file.AlbumType;
import com.qx.imlib.utils.file.AlbumUtils;
import com.qx.imui.R;

public class Camera2Config {

    public static final String CAMERA2_CONFIG_TAG = "Camera2Config_TAG";

    //进度条颜色，默认蓝色
    public static int RECORD_PROGRESS_VIEW_COLOR = R.color.colorPrimary;

    //最大高度预览尺寸，默认大于1000的第一个
    public static int PREVIEW_MAX_HEIGHT = 1000;

    //小视频存放地址，不设置的话默认在根目录的Camera2文件夹
    public static String PATH_SAVE_VIDEO = AlbumUtils.getAlbumTypeDir(AlbumType.VIDEO);
    //图片保存地址，不设置的话默认在根目录的Camera2文件夹
    public static String PATH_SAVE_PIC = AlbumUtils.getAlbumTypeDir(AlbumType.IMAGE);

    //是否需要录像功能
    public static boolean ENABLE_RECORD = true;
    //是否需要拍照功能
    public static boolean ENABLE_CAPTURE = true;
    //intent类型
    public static String INTENT_PLUGIN_TYPE_KEY = "INTENT_PLUGIN_TYPE_KEY";
    //intent路径
    public static String INTENT_PLUGIN_PATH_KEY = "INTENT_PLUGIN_PATH_KEY";
    //拍照类型
    public static int INTENT_PATH_SAVE_PIC = 1;
    //视频类型
    public static int INTENT_PATH_SAVE_VIDEO = 2;
}
