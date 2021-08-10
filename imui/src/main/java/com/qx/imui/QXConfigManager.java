package com.qx.imui;

public class QXConfigManager {

    private static FileConfig qxFileConfig;

    private QXConfigManager() {

    }

    /**
     * 初始化操作
     */
    public static void initConfig() {
        qxFileConfig = FileConfig.newBuilder().build();
    }

    /**
     * 初始化基础的Config配置
     * @param config
     */
    public static void initConfig(FileConfig config) {
        qxFileConfig = config;
    }


    /**
     * 获取到初始化后的参数
     */
    public static FileConfig getQxFileConfig() {
        if (qxFileConfig == null){
            throw new ExceptionInInitializerError("Please Add QXConfigManager initialize()~");
        }
        return qxFileConfig;
    }


}
