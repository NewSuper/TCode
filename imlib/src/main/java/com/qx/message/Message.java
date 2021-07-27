package com.qx.message;

import android.os.Parcel;
import android.text.TextUtils;

import com.qx.im.model.QXUserInfo;
import com.qx.imlib.QXIMClient;
import com.qx.imlib.utils.UUIDUtil;

import java.util.Objects;

import androidx.annotation.NonNull;


public class Message extends Data implements Comparable<Message>, Cloneable {
    private static final String TAG = "Message";

    /**
     * 2分钟内可以撤回
     */
    private final long RECALL_TIME_DURATION = 120000L;
    /**
     * 消息id
     */
    private String messageId;
    /**
     * 消息类型，详情见MessageType类
     */
    private String messageType;
    /**
     * 发送中id
     */
    private String senderUserId;

    /**
     * 接收者id
     */
    private String targetId;

    /**
     * 消息内容
     */
    private MessageContent messageContent;
    /**
     * 消息发送时间，以服务器时间为准
     */
    private long timestamp = System.currentTimeMillis();
    /**
     * 消息状态，见:
     *
     * @see State
     */
    private int state;
    /**
     * 阅读次数，暂时用不上，可能会被废弃
     */
    private int readCount;
    /**
     * 是否已删除，0未删除，1已删除，UI层可能用不上，后续会去掉该字段
     */
    private int deleted;

    private String thumb;//UI辅助字段，后续会优化
    private boolean fileLoading;//UI辅助字段，后续会优化
    private int currProgress;//UI辅助字段，后续会优化

    /**
     * 聊天类型（会话类型）
     */
    private String conversationType;
    /**
     * 会话id
     */
    private String conversationId;


    /**
     * 用户信息
     */
    private QXUserInfo userInfo;

    /**
     * 消息方向，见：
     */
    private int direction;

    private String failedReason;

    private boolean checked;

    // 1：兼容社交金融聊天室 ChatRoomTextAdapter 使用
    // 2: 此数据不发送
    private String extra;

    public Message() {
        super();

    }


    protected Message(Parcel in) {
        conversationType = in.readString();
        messageContent = in.readParcelable(MessageContent.class.getClassLoader());
        messageType = in.readString();
        targetId = in.readString();
        conversationId = in.readString();
        messageId = in.readString();
        senderUserId = in.readString();
        timestamp = in.readLong();
        state = in.readInt();
        readCount = in.readInt();
        deleted = in.readInt();
        thumb = in.readString();
        this.fileLoading = in.readInt() == 1 ? true : false;
        this.currProgress = in.readInt();
        userInfo = in.readParcelable(QXUserInfo.class.getClassLoader());
        direction = in.readInt();
        failedReason = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(conversationType);
        dest.writeParcelable(messageContent, flags);
        dest.writeString(messageType);
        dest.writeString(targetId);
        dest.writeString(conversationId);
        dest.writeString(messageId);
        dest.writeString(senderUserId);
        dest.writeLong(timestamp);
        dest.writeInt(state);
        dest.writeInt(readCount);
        dest.writeInt(deleted);
        dest.writeString(thumb);
        dest.writeInt(fileLoading ? 1 : 0);
        dest.writeInt(currProgress);
        dest.writeParcelable(userInfo, flags);
        dest.writeInt(direction);
        dest.writeString(failedReason);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public static Message obtain(String senderId, String targetId, String conversationType, String messageType,
                                 MessageContent messageContent) {
        Message message = new Message();
        message.messageId = UUIDUtil.getUUID();
        message.targetId = targetId;
        message.senderUserId = senderId;
        message.messageContent = messageContent;
        message.messageType = messageType;
        message.conversationType = conversationType;
        return message;
    }

    public static Message obtain(String senderId, String targetId, String conversationType, String messageType,
                                 MessageContent messageContent, String extra) {
        Message message = new Message();
        message.messageId = UUIDUtil.getUUID();
        message.targetId = targetId;
        message.senderUserId = senderId;
        message.messageContent = messageContent;
        message.messageType = messageType;
        message.conversationType = conversationType;
        message.extra = extra;
        return message;
    }

    /**
     * 兼容社交金融写好的消息体
     * @param senderId
     * @param targetId
     * @param conversationType
     * @param messageType
     * @param messageContent
     * @return
     */
    public static Message obtain(String senderId, String messageId, String targetId, String conversationType, String messageType,
                                 MessageContent messageContent, long timestamp,String extra) {
        Message message = new Message();
        if (messageId.isEmpty()) {
            message.messageId = UUIDUtil.getUUID();
        } else {
            message.messageId = messageId;
        }
        message.targetId = targetId;
        message.senderUserId = senderId;
        message.messageContent = messageContent;
        message.messageType = messageType;
        message.conversationType = conversationType;
        message.timestamp = timestamp;
        message.extra = extra;
        return message;
    }


    public String getConversationType() {
        return conversationType;
    }

    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }

    public MessageContent getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(MessageContent messageContent) {
        this.messageContent = messageContent;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getConversationId() {
        if (null == conversationId) {
            return "";
        }
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public int getCurrProgress() {
        return currProgress;
    }

    public void setCurrProgress(int currProgress) {
        this.currProgress = currProgress;
    }

    public boolean isFileLoading() {
        return fileLoading;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public void setFileLoading(boolean fileLoading) {
        this.fileLoading = fileLoading;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Message message = (Message) o;
        if (message != null && !TextUtils.isEmpty(message.messageId)) {
            return messageId.equals(message.messageId);
        }
        return false;
    }

    public QXUserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(QXUserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public void setFailedReason(String failedReason) {
        this.failedReason = failedReason;
    }


    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    /**
     * 是否可以撤回
     *
     * @return
     */
    public boolean isRecallable() {
        if (direction == Direction.DIRECTION_SEND) {
            if (state == State.STATE_SENT || state == State.STATE_RECEIVED || state == State.STATE_READ) {
                if (System.currentTimeMillis() - timestamp <= RECALL_TIME_DURATION) {
                    return true;
                }
            }
        }
        return false;

    }

    @Override
    public int compareTo(Message o) {
        if (this.timestamp - o.timestamp > 0) {
            return 1;
        } else if (this.timestamp - o.timestamp < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    @NonNull
    @Override
    public Message clone() throws CloneNotSupportedException {
        Message message = (Message) super.clone();
        message.senderUserId = QXIMClient.getInstance().getCurUserId();
        message.messageContent = (MessageContent) message.messageContent.clone();
        message.messageId = UUIDUtil.getUUID();
        return message;
    }

    public static class Direction {
        /**
         * 发送（自己发送的消息）
         */
        public static final int DIRECTION_SEND = 0;
        /**
         * 接收（接收的消息）
         */
        public static final int DIRECTION_RECEIVED = 1;

    }

    public static class State {
        /**
         * 发送中 0
         */
        public static final int STATE_SENDING = 0;

        /**
         * 发送成功 1
         */
        public static final int STATE_SENT = STATE_SENDING + 1;

        /**
         * 消息送达 2
         */
        public static final int STATE_RECEIVED = STATE_SENT + 1;

        /**
         * 消息已读 3
         */
        public static final int STATE_READ = STATE_RECEIVED + 1;

        /**
         * 发送失败 4
         */
        public static final int STATE_FAILED = STATE_READ + 1;
    }
}
