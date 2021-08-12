package com.qx.imui.notification;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;

import com.qx.im.model.ConversationType;
import com.qx.im.model.QXGroupInfo;
import com.qx.im.model.QXGroupUserInfo;
import com.qx.im.model.QXUserInfo;
import com.qx.imlib.QXIMClient;
import com.qx.imlib.qlog.QLog;
import com.qx.imui.QXContext;
import com.qx.imui.R;
import com.qx.imui.adapter.QXUserInfoManager;
import com.qx.imui.bean.ConversationKey;
import com.qx.imui.plugin.CustomMessageManager;
import com.qx.imui.plugin.MessageProvider;
import com.qx.message.Message;
import com.qx.message.MessageType;
import com.qx.message.TextMessage;
import com.qx.push.PushNotificationMessage;
import com.qx.push.QXPushClient;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.ConcurrentHashMap;

//内置push离线消息到达负责推送
public class QXNotificationManager {

    private static final String TAG = "QXNotificationManager";
    private static QXNotificationManager sS = new QXNotificationManager();
    QXContext mContext;
    ConcurrentHashMap<String, Message> messageMap = new ConcurrentHashMap();

    private QXNotificationManager() {
    }

    public void init(QXContext context) {
        this.mContext = context;
        this.messageMap.clear();
        if (!context.getEventBus().isRegistered(this)) {
            context.getEventBus().register(this);
        }

    }

    public static QXNotificationManager getInstance() {
        if (sS == null) {
            sS = new QXNotificationManager();
        }

        return sS;
    }

    public void onReceiveMessageFromApp(Message message) {
        this.onReceiveMessageFromApp(message, 0);
    }

    public void onReceiveMessageFromApp(Message message, int left) {
        String type = message.getConversationType();
        String targetName = "未知";
        String userName = "未知";
        Spannable content = new SpannableString(getNoticeContent(message));
        Log.i(TAG, "onReceiveMessageFromApp. conversationType:" + type+ ",content:"+content);
        PushNotificationMessage pushMsg;
        String targetId = "";
        if (type.equals(ConversationType.TYPE_PRIVATE)) {
            targetId = message.getSenderUserId();
        } else {
            targetId = message.getTargetId();
        }
        ConversationKey targetKey = ConversationKey.obtain(targetId, message.getConversationType());
        if (!type.equals(ConversationType.TYPE_PRIVATE) &&
                !type.equals(ConversationType.TYPE_CHAT_ROOM) &&
                !type.equals(ConversationType.TYPE_SYSTEM)) {
            QXUserInfo userInfo;
            if (type.equals(ConversationType.TYPE_GROUP)) {
                QXGroupInfo groupInfo = QXUserInfoManager.getInstance().getGroup(message.getTargetId());
                userInfo = QXUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
                QXGroupUserInfo groupUserInfo = QXUserInfoManager.getInstance().getGroupUserInfo(message.getTargetId(), message.getSenderUserId());
                if (groupInfo != null) {
                    targetName = groupInfo.getName();
                }

                if (groupUserInfo != null) {
                    userName = groupUserInfo.getDisplayName();
                }

                if (TextUtils.isEmpty(userName) && userInfo != null) {
                    userName = userInfo.getName();
                    Log.d(TAG, "onReceiveMessageFromApp the nickName of group user is null");
                }

                if (!TextUtils.isEmpty(targetName) && !TextUtils.isEmpty(userName)) {
                    String notificationContent;
//                    if (this.isMentionedMessage(message)) {
//                        if (TextUtils.isEmpty(message.getContent().getMentionedInfo().getMentionedContent())) {
//                            notificationContent = this.mContext.getString(string.rc_message_content_mentioned) + userName + " : " + content.toString();
//                        } else {
//                            notificationContent = message.getContent().getMentionedInfo().getMentionedContent();
//                        }
//                    } else if (message.getContent() instanceof RecallNotificationMessage) {
//                        notificationContent = content.toString();
//                    } else {
                    notificationContent = userName + " : " + content.toString();
//                    }

                    pushMsg = this.transformToPushMessage(message, notificationContent, targetName, "");
                    QXPushClient.sendNotification(this.mContext.getContext(), pushMsg, left);
                } else {
                    if (TextUtils.isEmpty(targetName) && targetKey != null) {
                        this.messageMap.put(targetKey.getKey(), message);
                    }

                    if (TextUtils.isEmpty(userName)) {
                        ConversationKey senderKey = ConversationKey.obtain(message.getSenderUserId(), type);
                        if (senderKey != null) {
                            this.messageMap.put(senderKey.getKey(), message);
                        } else {
                            QLog.e(TAG, "onReceiveMessageFromApp senderKey is null");
                        }
                    }
                    QLog.e(TAG, "No popup notification cause of the sender name is null, please set UserInfoProvider");
                }
            } else {

            }
        } else {
            QXUserInfo userInfo = QXUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            QLog.e(TAG,  "getUserInfo conversationType:"+message.getConversationType()+",userinfo:"+userInfo);
            if (userInfo != null) {
                targetName = userInfo.getName();
            }

            if (!TextUtils.isEmpty(targetName)) {
                pushMsg = this.transformToPushMessage(message, content.toString(), targetName, targetName);
                QXPushClient.sendNotification(this.mContext.getContext(), pushMsg, left);
            } else {
                if (targetKey != null) {
                    this.messageMap.put(targetKey.getKey(), message);
                }
                QLog.e(TAG,  "No popup notification cause of the sender name is null, please set UserInfoProvider");
            }
        }

    }

    private String getNoticeContent(Message message) {
        if (message.getMessageType().equals(MessageType.TYPE_TEXT)) {
            return ((TextMessage)message.getMessageContent()).getContent();
        } else if (message.getMessageType().equals(MessageType.TYPE_IMAGE)) {
            return mContext.getContext().getString(R.string.qx_target_name_image);
        } else if (message.getMessageType().equals( MessageType.TYPE_AUDIO)) {
            return mContext.getContext().getString(R.string.qx_target_name_voice);
        } else if (message.getMessageType().equals(MessageType.TYPE_FILE)) {
            return mContext.getContext().getString(R.string.qx_target_name_file,"");
        } else if (message.getMessageType().equals(MessageType.TYPE_IMAGE_AND_TEXT)) {
            return mContext.getContext().getString(R.string.qx_target_name_image_text,"");
        } else if (message.getMessageType().equals(MessageType.TYPE_GEO)) {
            return mContext.getContext().getString(R.string.qx_target_name_geo,"");
        } else if (message.getMessageType().equals(MessageType.TYPE_RECALL)) {
            return mContext.getContext().getString(R.string.qx_recall_by_other,"");
        } else if (message.getMessageType().equals(MessageType.TYPE_VIDEO)) {
            return mContext.getContext().getString(R.string.qx_target_name_video);
        } else if (message.getMessageType().equals(MessageType.TYPE_AUDIO_CALL)) {
            return mContext.getContext().getString(R.string.qx_target_name_call_audio);
        } else if (message.getMessageType().equals(MessageType.TYPE_VIDEO_CALL)) {
            return mContext.getContext().getString(R.string.qx_target_name_call_video);
        } else {
            MessageProvider messageProvider =  CustomMessageManager.getMessageProvider(message.getMessageType());
            if (messageProvider != null) {
                return messageProvider.getNoticeText(mContext.getContext());
            } else {
                return "";
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(QXUserInfo userInfo) {
        String[] types = new String[]{ConversationType.TYPE_PRIVATE, ConversationType.TYPE_GROUP, ConversationType.TYPE_CHAT_ROOM, ConversationType.TYPE_SYSTEM};
//        LogUtil.info(QXNotificationManager.class, "onEventMainThread. userInfo" + userInfo);
        String[] var5 = types;
        int var6 = types.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            String type = var5[var7];
            ConversationKey conversationKey = ConversationKey.obtain(userInfo.getId(), type);
            String key = "";
            if (conversationKey != null) {
                key = conversationKey.getKey();
            }
            if (this.messageMap.containsKey(key)) {
                Message message = (Message)this.messageMap.get(key);
                String targetName = "";
                String notificationContent = "";
                String content = getNoticeContent(message);
                this.messageMap.remove(key);
                if (type.equals(ConversationType.TYPE_GROUP)) {
                    QXGroupInfo groupInfo = QXUserInfoManager.getInstance().getGroup(message.getTargetId());
                    QXGroupUserInfo groupUserInfo = QXUserInfoManager.getInstance().getGroupUserInfo(message.getTargetId(), message.getSenderUserId());
                    String userName = "";
                    if (groupInfo == null) {
                        QLog.e(TAG,  "onEventMainThread userInfo : groupInfo is null, return directly");
                        return;
                    }

                    targetName = groupInfo.getName();
                    if (groupUserInfo != null) {
                        userName = groupUserInfo.getDisplayName();
                    }

                    if (TextUtils.isEmpty(userName) && userInfo != null) {
                        userName = userInfo.getName();
                        QLog.e(TAG,  "onReceiveMessageFromApp the nickName of group user is null");
                    }

//                    if (this.isMentionedMessage(message)) {
//                        if (TextUtils.isEmpty(message.getContent().getMentionedInfo().getMentionedContent())) {
//                            notificationContent = this.mContext.getString(string.rc_message_content_mentioned) + userName + " : " + content.toString();
//                        } else {
//                            notificationContent = message.getContent().getMentionedInfo().getMentionedContent();
//                        }
//                    } else {
                    notificationContent = userName + " : " + content.toString();
//                    }
                }  else {
                    targetName = userInfo.getName();
                    notificationContent = content.toString();
                }

                if (TextUtils.isEmpty(targetName)) {
                    return;
                }

                PushNotificationMessage pushMsg = this.transformToPushMessage(message, notificationContent, targetName, "");
                QXPushClient.sendNotification(this.mContext.getContext(), pushMsg);
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(QXGroupInfo groupInfo) {
        String key = ConversationKey.obtain(groupInfo.getId(), ConversationType.TYPE_GROUP).getKey();
        QLog.e(TAG,  "onEventMainThread. groupInfo" + groupInfo);
        if (this.messageMap.containsKey(key)) {
            Message message = (Message)this.messageMap.get(key);
            String userName = "";
            String content = getNoticeContent(message);
            this.messageMap.remove(key);
            QXUserInfo userInfo = QXUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            if (userInfo == null) {
                QLog.e(TAG, "onEventMainThread Group : userInfo is null, return directly");
                return;
            }

            userName = userInfo.getName();
            if (TextUtils.isEmpty(userName)) {
                QLog.e(TAG, "onEventMainThread Group : userName is empty, return directly");
                return;
            }

            String pushContent = userName + " : " + content.toString();
            PushNotificationMessage pushMsg = this.transformToPushMessage(message, pushContent, groupInfo.getName(), "");
            QXPushClient.sendNotification(this.mContext.getContext(), pushMsg);
        }

    }
//
//    public void onEventMainThread(Discussion discussion) {
//        String key = ConversationKey.obtain(discussion.getId(), ConversationType.DISCUSSION).getKey();
//        if (this.messageMap.containsKey(key)) {
//            String userName = "";
//            Message message = (Message)this.messageMap.get(key);
//            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass()).getContentSummary(this.mContext, message.getContent());
//            this.messageMap.remove(key);
//            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
//            if (userInfo != null) {
//                userName = userInfo.getName();
//                if (TextUtils.isEmpty(userName)) {
//                    return;
//                }
//            }
//
//            String pushContent = userName + " : " + content.toString();
//            PushNotificationMessage pushMsg = this.transformToPushMessage(message, pushContent, discussion.getName(), "");
//            RongPushClient.sendNotification(this.mContext, pushMsg);
//        }
//
//    }
//
//    public void onEventMainThread(PublicServiceProfile info) {
//        String key = ConversationKey.obtain(info.getTargetId(), info.getConversationType()).getKey();
//        if (this.messageMap.containsKey(key)) {
//            Message message = (Message)this.messageMap.get(key);
//            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass()).getContentSummary(this.mContext, message.getContent());
//            PushNotificationMessage pushMsg = this.transformToPushMessage(message, content.toString(), info.getName(), "");
//            RongPushClient.sendNotification(this.mContext, pushMsg);
//            this.messageMap.remove(key);
//        }
//
//    }

    // 是否是at某人信息
//    private boolean isMentionedMessage(Message message) {
//        MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
//        return mentionedInfo != null && (mentionedInfo.getType().equals(MentionedType.ALL) || mentionedInfo.getType().equals(MentionedType.PART) && mentionedInfo.getMentionedUserIdList() != null && mentionedInfo.getMentionedUserIdList().contains(RongIMClient.getInstance().getCurrentUserId()));
//    }

    // 消息转换
    private PushNotificationMessage transformToPushMessage(Message message, String content, String targetUserName, String senderName) {
        PushNotificationMessage pushMsg = new PushNotificationMessage();
        pushMsg.setPushTitle(targetUserName);
        pushMsg.setPushContent(content);
        pushMsg.setConversationType(QXPushClient.ConversationType.setName(message.getConversationType()));
        pushMsg.setTargetId(message.getTargetId());
        pushMsg.setTargetUserName(targetUserName);
        pushMsg.setSenderId(message.getSenderUserId());
        pushMsg.setSenderName(senderName);
        pushMsg.setPushFlag("false");
        pushMsg.setToId(QXIMClient.getInstance().getCurUserId());
        pushMsg.setSourceType(PushNotificationMessage.PushSourceType.LOCAL_MESSAGE);
        pushMsg.setPushId(message.getMessageId());
        return pushMsg;
    }
}
