package com.qx.imui.util;

public interface FileCallback {

    /**
     * 执行进度
     * @param progress 取值范围：1-100
     */
    void onProgress(int progress);

    /**
     * 执行完毕（成功）
     */
    void onCompleted(String url);

    /**
     * 失败
     * @param errorCode 错误码，UI层定义
     * @param errorMsg 错误消息，UI层定义
     */
    void onFailed(int errorCode, String errorMsg);
}
