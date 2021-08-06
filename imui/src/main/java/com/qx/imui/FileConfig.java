package com.qx.imui;

import com.qx.imlib.utils.file.FileSizeUtil;

import java.util.concurrent.TimeUnit;

public class FileConfig {

    /**
     * 视频可拍摄最大时长
     */
    private long videoShotMaxDuration;

    /**
     * 视频可拍摄最小时长
     */
    private long videoShotMinDuration;

    /**
     * 相册中视频可选择最大显示
     */
    private double videoMaxSize;
    /**
     * 相册中视频可选择最长时间显示
     */
    private long videoMaxDuration;

    /**
     * 图片消息文件大小限制
     */
    private double imageMaxSize;

    /**
     * 文字消息长度限制(单位Char长度)
     */
    private int textMessageMaxLength;

    /**
     * 语音消息长度限制
     */
    private long voiceMessageMaxDuration;

    /**
     * 文件消息大小限制
     */
    private double fileMessageMaxSize;

    public long getVideoShotMaxDuration(TimeUnit unit) {
        return getDuration("video_shot_max", videoShotMaxDuration, unit);
    }

    public long getVideoShotMinDuration(TimeUnit unit) {
        return getDuration("video_shot_min", videoShotMinDuration, unit);
    }

    public double getImageMaxSize(int sizeType) {
        return formartFileSize("image_max_size", imageMaxSize, sizeType);
    }

    public double getVideoMaxSize(int sizeType) {
        return formartFileSize("video_max_size", videoMaxSize, sizeType);
    }

    public double getVideoMaxDuration(TimeUnit unit) {
        return getDuration("video_max_duration", videoMaxDuration, unit);
    }

    public int getTextMessageMaxLength() {
        return textMessageMaxLength;
    }

    public long getVoiceMessageMaxDuration(TimeUnit unit) {
        return getDuration("voice_max_dutation", voiceMessageMaxDuration, unit);
    }

    public double getFileMessageMaxSize(int sizeType) {
        return formartFileSize("file_max_size", fileMessageMaxSize, sizeType);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private FileConfig(Builder builder) {
        videoShotMaxDuration = builder.videoShotMaxDuration;
        videoShotMinDuration = builder.videoShotMinDuration;
        videoMaxSize = builder.videoMaxSize;
        videoMaxDuration = builder.videoMaxDuration;
        imageMaxSize = builder.imageMaxSize;
        textMessageMaxLength = builder.textMessageMaxLength;
        voiceMessageMaxDuration = builder.voiceMessageMaxDuration;
        fileMessageMaxSize = builder.fileMessageMaxSize;
    }

    public static final class Builder {

        /**
         * 视频可拍摄最大时长
         */
        private long videoShotMaxDuration;

        /**
         * 视频可拍摄最小时长
         */
        private long videoShotMinDuration;

        /**
         * 相册中视频可选择最大显示
         */
        private double videoMaxSize;
        /**
         * 相册中视频可选择最长时间显示
         */
        private int videoMaxDuration;

        /**
         * 图片消息文件大小限制
         */
        private double imageMaxSize;

        /**
         * 文字消息长度限制
         */
        private int textMessageMaxLength;

        /**
         * 语音消息长度限制
         */
        private long voiceMessageMaxDuration;

        /**
         * 文件消息大小限制
         */
        private double fileMessageMaxSize;

        private Builder() {
            videoShotMaxDuration = checkDuration("video_shot_max", FileConfigConstants.VIDEO_SHOT_MAX_DURATION, TimeUnit.SECONDS);
            videoShotMinDuration = checkDuration("video_shot_min", FileConfigConstants.VIDEO_SHOT_MIN_DURATION, TimeUnit.SECONDS);
            videoMaxSize = checkFileSize("video_max_size", FileConfigConstants.VIDEO_MAX_SIZE, FileSizeUtil.SIZETYPE_MB);
            videoMaxDuration = checkDuration("video_max_duration", FileConfigConstants.VIDEO_MAX_DURATION, TimeUnit.MINUTES);
            imageMaxSize = checkFileSize("image_max_size", FileConfigConstants.IMAGE_MAX_SIZE, FileSizeUtil.SIZETYPE_MB);
            textMessageMaxLength = FileConfigConstants.TEXT_MESSAGE_MAX_LENGTH;
            voiceMessageMaxDuration = checkDuration("voice_max_dutation", FileConfigConstants.VOICE_MESSAGE_MAX_DURATION, TimeUnit.SECONDS);
            fileMessageMaxSize = checkFileSize("file_message_max_size", FileConfigConstants.FILE_MESSAGE_MAX_SIZE, FileSizeUtil.SIZETYPE_MB);
        }

        /**
         * 视频可拍摄最大时长
         *
         * @param videoShotMax
         * @return
         */
        public Builder videoShotMax(int videoShotMax, TimeUnit unit) {
            this.videoShotMaxDuration = checkDuration("video_shot_max", videoShotMax, unit);
            return this;
        }

        /**
         * 视频可拍摄最小时长
         *
         * @param videoShotMin
         * @return
         */
        public Builder videoShotMin(int videoShotMin, TimeUnit unit) {
            this.videoShotMinDuration = checkDuration("video_shot_min", videoShotMin, unit);
            return this;
        }

        /**
         * 视频限定最大
         *
         * @param videoMaxSize
         * @return
         */
        public Builder videoMaxSize(int videoMaxSize, int sizeType) {
            this.videoMaxSize = checkFileSize("video_max_size", videoMaxSize, sizeType);
            return this;
        }

        /**
         * 视频限定最大
         *
         * @param videoMaxDuration
         * @return
         */
        public Builder videoMaxDuration(int videoMaxDuration, TimeUnit unit) {
            this.videoMaxDuration = checkDuration("video_max_duration", videoMaxDuration, unit);
            return this;
        }

        /**
         * 图片限定最大
         *
         * @param imageMaxSize
         * @return
         */
        public Builder imageMaxSize(int imageMaxSize, int sizeType) {
            this.imageMaxSize = checkFileSize("image_max_size", imageMaxSize, sizeType);
            return this;
        }

        /**
         * 文字消息长度限制
         *
         * @param textMessageMaxLength
         * @return
         */
        public Builder textMsgMaxLength(int textMessageMaxLength) {
            this.textMessageMaxLength = textMessageMaxLength;
            return this;
        }

        /**
         * 语音消息长度限制
         *
         * @param voiceMessageMaxDuration
         * @return
         */
        public Builder voiceMsgMaxDuration(int voiceMessageMaxDuration, TimeUnit unit) {
            this.voiceMessageMaxDuration = checkDuration("voice_max_dutation", voiceMessageMaxDuration, unit);
            return this;
        }

        /**
         * 文件消息大小限制
         *
         * @param fileMsgMaxSize
         * @return
         */
        public Builder fileMsgMaxSize(int fileMsgMaxSize, int sizeTyp) {
            this.fileMessageMaxSize = checkFileSize("file_message_max_size", fileMsgMaxSize, sizeTyp);
            return this;
        }

        public FileConfig build() {
            return new FileConfig(this);
        }
    }

    /**
     * 指定类型文件大小转化为B
     * @param name
     * @param fileSize
     * @param sizeType
     * @return
     */
    private static double checkFileSize(String name, double fileSize, int sizeType) {
        if (sizeType < 1 || sizeType > 4)
            throw new IllegalArgumentException(name + " sizeType is Error!");
        switch (sizeType) {
            case FileSizeUtil.SIZETYPE_B:
                return fileSize;
            case FileSizeUtil.SIZETYPE_KB:
                return 1024 * fileSize;
            case FileSizeUtil.SIZETYPE_MB:
                return 1048576 * fileSize;
            case FileSizeUtil.SIZETYPE_GB:
                return 1073741824 * fileSize;
        }
        return 0;
    }

    /**
     * B单位文件大小转化为指定类型
     * @param name
     * @param fileSize
     * @param sizeType
     * @return
     */
    private static double formartFileSize(String name, double fileSize,int sizeType) {
        if (sizeType < 1 || sizeType > 4) throw new IllegalArgumentException(name + " sizeType is Error!");
        return FileSizeUtil.FormetFileSize(fileSize,sizeType);
    }

    /**
     * 时间转换为毫秒
     *
     * @param name
     * @param duration
     * @param unit
     * @return
     */
    private static int checkDuration(String name, long duration, TimeUnit unit) {
        if (duration < 0) throw new IllegalArgumentException(name + " < 0");
        if (unit == null) throw new NullPointerException("unit == null");
        long millis = unit.toMillis(duration);
        if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException(name + " too large.");
        if (millis == 0 && duration > 0) throw new IllegalArgumentException(name + " too small.");
        return (int) millis;
    }

    /**
     * 毫秒转换为指定的类型
     *
     * @param unit
     * @return
     */
    private static long getDuration(String name, long millis, TimeUnit unit) {
        if (millis < 0) throw new IllegalArgumentException(name + " < 0");
        if (unit == null) throw new NullPointerException("unit == null");
        if (unit == TimeUnit.DAYS) {
            //转化为天
            return TimeUnit.MILLISECONDS.toDays(millis);
        } else if (unit == TimeUnit.HOURS) {
            //转化为小时
            return TimeUnit.MILLISECONDS.toHours(millis);
        } else if (unit == TimeUnit.MINUTES) {
            //转化为分钟
            return TimeUnit.MILLISECONDS.toMinutes(millis);
        } else if (unit == TimeUnit.SECONDS) {
            //转化为秒
            return TimeUnit.MILLISECONDS.toSeconds(millis);
        } else if (unit == TimeUnit.MILLISECONDS) {
            //转化为毫秒
            return millis;
        } else {
            throw new NullPointerException("unit is error!");
        }
    }
}

