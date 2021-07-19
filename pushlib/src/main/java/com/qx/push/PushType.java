package com.qx.push;


import android.text.TextUtils;

public enum PushType {
    UNKNOWN("UNKNOWN",0),
    QX("QX",1),
    HUAWEI("HW",2),
    XIAOMI("MI",3),
    GOOGLE_FCM("FCM",7),
    MEIZU("MEIZU",6),
    VIVO("VIVO",5),
    OPPO("OPPO",4);

    private String name;
    private int type;

    private PushType(String name,int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public int getType() {
        return this.type;
    }
    public static PushType getType(String name) {
        if (!TextUtils.isEmpty(name)) {
            PushType[] pushTypes = values();
            int length = pushTypes.length;

            for (int i = 0; i < length; ++i) {
                PushType type = pushTypes[i];
                if (type.getName().equals(name)) {
                    return type;
                }
            }
        }
        return UNKNOWN;
    }
}