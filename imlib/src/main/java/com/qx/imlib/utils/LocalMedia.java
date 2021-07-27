package com.qx.imlib.utils;

public class LocalMedia {

    private String name;
    /**
     * 缩略图路径
     */
    private String path;
    /**
     * 时长
     */
    private int duration;
    /**
     * 大小
     */
    private long size;
    /**
     * 后缀名
     */
    private String mimeType;
    private int width;
    private int height;
    /**
     * 媒体类型
     */
    private int mediaType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }


    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }


    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }


    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getMediaType() {
        return mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }


    public static class MediaType {
        public static final int MEDIA_TYPE_UNKNOWN = -1;
        public static final int MEDIA_TYPE_AUDIO = 0;
        public static final int MEDIA_TYPE_IMAGE = 1;
        public static final int MEDIA_TYPE_VIDEO = 2;
    }
}