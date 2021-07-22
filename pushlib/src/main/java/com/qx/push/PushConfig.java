package com.qx.push;

import android.text.TextUtils;

import java.util.ArrayList;

public class PushConfig {
    private String miAppId;
    private String miAppKey;
    private String mzAppId;
    private String mzAppKey;
    private String oppoAppKey;
    private String oppoAppSecret;
    private ArrayList<PushType> enabledPushTypes;
    private String appKey;

    public PushConfig() {
    }

    public String getMiAppId() {
        return this.miAppId;
    }

    public String getMiAppKey() {
        return this.miAppKey;
    }

    public String getMzAppId() {
        return this.mzAppId;
    }

    public String getMzAppKey() {
        return this.mzAppKey;
    }

    public String getOppoAppKey() {
        return this.oppoAppKey;
    }

    public String getOppoAppSecret() {
        return this.oppoAppSecret;
    }


    public String getAppKey() {
        return this.appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public ArrayList<PushType> getEnabledPushTypes() {
        return this.enabledPushTypes;
    }


    public static class Builder {
        private String miAppId;
        private String miAppKey;
        private String mzAppId;
        private String mzAppKey;
        private String oppoAppKey;
        private String oppoAppSecret;
        private ArrayList<PushType> enabledPushTypes = new ArrayList();
        private String appKey;

        public Builder() {
        }

        public Builder enableMiPush(String miAppId, String miAppkey) {
            if (!TextUtils.isEmpty(miAppId) && !TextUtils.isEmpty(miAppkey)) {
                this.miAppId = miAppId;
                this.miAppKey = miAppkey;
                this.enabledPushTypes.add(PushType.XIAOMI);
            }
            return this;
        }

        public Builder enableHWPush(boolean isEnable) {
            if (isEnable) {
                this.enabledPushTypes.add(PushType.HUAWEI);
            }

            return this;
        }


        public Builder enableFCM(boolean isEnable) {
            if (isEnable) {
                if (!this.enabledPushTypes.contains(PushType.GOOGLE_FCM)) {
                    this.enabledPushTypes.add(PushType.GOOGLE_FCM);
                } else {
                }
            }

            return this;
        }

        public Builder enableMeiZuPush(String mzAppId, String mzAppKey) {
            if (!TextUtils.isEmpty(mzAppId) && !TextUtils.isEmpty(mzAppKey)) {
                this.mzAppId = mzAppId;
                this.mzAppKey = mzAppKey;
                this.enabledPushTypes.add(PushType.MEIZU);
            }
            return this;
        }

        public Builder enableOppoPush(String oppoAppKey, String oppoAppSecret) {
            if (!TextUtils.isEmpty(oppoAppKey) && !TextUtils.isEmpty(oppoAppSecret)) {
                this.oppoAppKey = oppoAppKey;
                this.oppoAppSecret = oppoAppSecret;
                this.enabledPushTypes.add(PushType.OPPO);
            }
            return this;
        }

        public Builder enableVivoPush(boolean isEnable) {
            if (isEnable) {
                this.enabledPushTypes.add(PushType.VIVO);
            }

            return this;
        }

        public Builder setAppKey(String appKey) {
            this.appKey = appKey;
            return this;
        }

        public PushConfig build() {
            PushConfig pushConfig = new PushConfig();
            pushConfig.miAppId = this.miAppId;
            pushConfig.miAppKey = this.miAppKey;
            pushConfig.mzAppId = this.mzAppId;
            pushConfig.mzAppKey = this.mzAppKey;
            pushConfig.oppoAppKey = this.oppoAppKey;
            pushConfig.oppoAppSecret = this.oppoAppSecret;
            pushConfig.enabledPushTypes = this.enabledPushTypes;
            pushConfig.appKey = this.appKey;
            return pushConfig;
        }
    }
}

