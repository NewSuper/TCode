package com.qx.imui.bean;

import android.text.TextUtils;

import com.qx.imlib.qlog.QLog;

public class ConversationKey {

    private static final String TAG = "ConversationKey";

    private static final String SEPARATOR = "#@6QXIM_CLOUD9@#";
    private String key;
    private String targetId;
    private String type;

    private ConversationKey() {
    }

    public static ConversationKey obtain(String targetId, String type) {
        if (!TextUtils.isEmpty(targetId) && type != null) {
            ConversationKey conversationKey = new ConversationKey();
            conversationKey.setTargetId(targetId);
            conversationKey.setType(type);
            conversationKey.setKey(targetId + SEPARATOR + type);
            return conversationKey;
        } else {
            return null;
        }
    }

    public static ConversationKey obtain(String key) {
        if (!TextUtils.isEmpty(key) && key.contains(SEPARATOR)) {
            ConversationKey conversationKey = new ConversationKey();
            if (key.contains(SEPARATOR)) {
                String[] array = key.split(SEPARATOR);
                conversationKey.setTargetId(array[0]);

                try {
                    conversationKey.setType(array[1]);
                    return conversationKey;
                } catch (NumberFormatException var4) {
                    QLog.e(TAG, "NumberFormatException");
                    return null;
                }
            }
        }

        return null;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTargetId() {
        return this.targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

