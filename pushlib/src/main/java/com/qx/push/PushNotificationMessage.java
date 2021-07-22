package com.qx.push;


import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class PushNotificationMessage implements Parcelable {

    private String pushId;
    private QXPushClient.ConversationType conversationType;
    private long receivedTime;
    private String messageType;
    private String senderId;
    private String senderName;
    private Uri senderPortrait;
    private String targetId;
    private String targetUserName;
    private String toId;
    private String pushTitle;
    private String pushContent;
    private String pushData;
    private String extra;
    private boolean disablePushTitle;
    private String isFromPush;
    private PushNotificationMessage.PushSourceType sourceType;
    private String notificationId;
    private boolean isShowDetail;
    private String channelIdMi;
    private String channelIdHW;
    private String channelIdOPPO;

    public PushNotificationMessage() {

    }
    protected PushNotificationMessage(Parcel in) {
        pushId = in.readString();
        receivedTime = in.readLong();
        messageType = in.readString();
        senderId = in.readString();
        senderName = in.readString();
        senderPortrait = in.readParcelable(Uri.class.getClassLoader());
        targetId = in.readString();
        targetUserName = in.readString();
        toId = in.readString();
        pushTitle = in.readString();
        pushContent = in.readString();
        pushData = in.readString();
        extra = in.readString();
        disablePushTitle = in.readByte() != 0;
        isFromPush = in.readString();
        notificationId = in.readString();
        isShowDetail = in.readByte() != 0;
        channelIdMi = in.readString();
        channelIdHW = in.readString();
        channelIdOPPO = in.readString();
        String temp = in.readString();
        conversationType = TextUtils.isEmpty(temp) ? null : QXPushClient.ConversationType.setName(temp);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pushId);
        dest.writeLong(receivedTime);
        dest.writeString(messageType);
        dest.writeString(senderId);
        dest.writeString(senderName);
        dest.writeParcelable(senderPortrait, flags);
        dest.writeString(targetId);
        dest.writeString(targetUserName);
        dest.writeString(toId);
        dest.writeString(pushTitle);
        dest.writeString(pushContent);
        dest.writeString(pushData);
        dest.writeString(extra);
        dest.writeByte((byte) (disablePushTitle ? 1 : 0));
        dest.writeString(isFromPush);
        dest.writeString(notificationId);
        dest.writeByte((byte) (isShowDetail ? 1 : 0));
        dest.writeString(channelIdMi);
        dest.writeString(channelIdHW);
        dest.writeString(channelIdOPPO);
        dest.writeString(conversationType == null ? "" : conversationType.getName());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PushNotificationMessage> CREATOR = new Creator<PushNotificationMessage>() {
        @Override
        public PushNotificationMessage createFromParcel(Parcel in) {
            return new PushNotificationMessage(in);
        }

        @Override
        public PushNotificationMessage[] newArray(int size) {
            return new PushNotificationMessage[size];
        }
    };

    public String getPushId() {
        return pushId;
    }

    public void setPushId(String pushId) {
        this.pushId = pushId;
    }

    public QXPushClient.ConversationType getConversationType() {
        return conversationType;
    }

    public void setConversationType(QXPushClient.ConversationType conversationType) {
        this.conversationType = conversationType;
    }

    public long getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(long receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Uri getSenderPortrait() {
        return senderPortrait;
    }

    public void setSenderPortrait(Uri senderPortrait) {
        this.senderPortrait = senderPortrait;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetUserName() {
        return targetUserName;
    }

    public void setTargetUserName(String targetUserName) {
        this.targetUserName = targetUserName;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    public String getPushTitle() {
        return pushTitle;
    }

    public void setPushTitle(String pushTitle) {
        this.pushTitle = pushTitle;
    }

    public String getPushContent() {
        return pushContent;
    }

    public void setPushContent(String pushContent) {
        this.pushContent = pushContent;
    }

    public String getPushData() {
        return pushData;
    }

    public void setPushData(String pushData) {
        this.pushData = pushData;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public boolean isDisablePushTitle() {
        return disablePushTitle;
    }

    public void setDisablePushTitle(boolean disablePushTitle) {
        this.disablePushTitle = disablePushTitle;
    }

    public void setPushFlag(String value) {
        this.isFromPush = value;
    }

    public String getPushFlag() {
        return this.isFromPush;
    }

    public PushSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(PushSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public boolean isShowDetail() {
        return isShowDetail;
    }

    public void setShowDetail(boolean showDetail) {
        isShowDetail = showDetail;
    }

    public String getChannelIdMi() {
        return channelIdMi;
    }

    public void setChannelIdMi(String channelIdMi) {
        this.channelIdMi = channelIdMi;
    }

    public String getChannelIdHW() {
        return channelIdHW;
    }

    public void setChannelIdHW(String channelIdHW) {
        this.channelIdHW = channelIdHW;
    }

    public String getChannelIdOPPO() {
        return channelIdOPPO;
    }

    public void setChannelIdOPPO(String channelIdOPPO) {
        this.channelIdOPPO = channelIdOPPO;
    }

    public static enum PushSourceType {
        FROM_OFFLINE_MESSAGE,
        FROM_ADMIN,
        LOCAL_MESSAGE,
        UNKNOWN;

        private PushSourceType() {
        }
    }

    @Override
    public String toString() {
        return "PushNotificationMessage{" +
                "pushId='" + pushId + '\'' +
                ", conversationType=" + conversationType +
                ", receivedTime=" + receivedTime +
                ", messageType='" + messageType + '\'' +
                ", senderId='" + senderId + '\'' +
                ", senderName='" + senderName + '\'' +
                ", senderPortrait=" + senderPortrait +
                ", targetId='" + targetId + '\'' +
                ", targetUserName='" + targetUserName + '\'' +
                ", toId='" + toId + '\'' +
                ", pushTitle='" + pushTitle + '\'' +
                ", pushContent='" + pushContent + '\'' +
                ", pushData='" + pushData + '\'' +
                ", extra='" + extra + '\'' +
                ", disablePushTitle=" + disablePushTitle +
                ", isFromPush='" + isFromPush + '\'' +
                ", sourceType=" + sourceType +
                ", notificationId='" + notificationId + '\'' +
                ", isShowDetail=" + isShowDetail +
                ", channelIdMi='" + channelIdMi + '\'' +
                ", channelIdHW='" + channelIdHW + '\'' +
                ", channelIdOPPO='" + channelIdOPPO + '\'' +
                '}';
    }
}
