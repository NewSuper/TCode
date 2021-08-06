package com.qx.imui.bean;

public class RecordItem {

    private String messageId;
    private String localPath;
    private String originUrl;
    private String type;
    private String ownerId;

    public RecordItem(String messageId, String localPath, String originUrl, String type, String ownerId) {
        this.messageId = messageId;
        this.localPath = localPath;
        this.originUrl = originUrl;
        this.type = type;
        this.ownerId = ownerId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
