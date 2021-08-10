package com.qx.imui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.qx.im.model.QXGroupInfo;
import com.qx.im.model.QXGroupUserInfo;
import com.qx.im.model.QXUserInfo;
import com.qx.im.model.SearchConversationResult;
import com.qx.imlib.ModuleManager;
import com.qx.imlib.QXIMClient;
import com.qx.imlib.qlog.QLog;
import com.qx.imlib.utils.LocalMedia;
import com.qx.imlib.utils.MediaUtil;
import com.qx.imlib.utils.MessageCreator;
import com.qx.imlib.utils.SystemUtil;
import com.qx.imlib.utils.event.EventBusUtil;
import com.qx.imlib.utils.file.BitmapUtil;
import com.qx.imlib.utils.file.FileUtil;
import com.qx.imui.adapter.QXUserCacheListener;
import com.qx.imui.adapter.QXUserInfoManager;
import com.qx.imui.bean.CollectionItem;
import com.qx.imui.bean.KickedEvent;
import com.qx.imui.bean.Member;
import com.qx.imui.bean.QXFavorite;
import com.qx.imui.bean.QXGroupNotice;
import com.qx.imui.bean.RecordItem;
import com.qx.imui.bean.TargetItem;
import com.qx.imui.bean.menu.QXMenu;
import com.qx.imui.emotion.StickerItem;
import com.qx.imui.emotion.StickerManager;
import com.qx.imui.menu.MenuActionType;
import com.qx.imui.menu.MenuType;
import com.qx.imui.menu.QXMenuManager;
import com.qx.imui.menu.QXMessageLongClickManager;
import com.qx.imui.notification.MessageNotificationManager;
import com.qx.imui.notification.QXNotificationManager;
import com.qx.imui.plugin.AlbumBitmapCacheHelper;
import com.qx.imui.plugin.QXDefaultExtensionModule;
import com.qx.imui.plugin.QXExtensionManager;
import com.qx.imui.plugin.rtc.RTCModuleManager;
import com.qx.imui.util.DownloadCallback;
import com.qx.imui.util.MuteCache;
import com.qx.imui.util.UploadCallback;
import com.qx.imui.util.upload.MediaMessageEmitter;
import com.qx.message.AudioMessage;
import com.qx.message.Conversation;
import com.qx.message.FileMessage;
import com.qx.message.ICustomEventProvider;
import com.qx.message.ImageMessage;
import com.qx.message.Message;
import com.qx.message.MessageContent;
import com.qx.message.MessageType;
import com.qx.message.QXError;
import com.qx.message.SensitiveWordResult;
import com.qx.message.UserProperty;
import com.qx.message.VideoMessage;
import com.qx.push.PushManager;
import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsListener;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * 此类为增加接口回调之用----开放给外部回调
 */
public class QXIMKit {

    private static final String TAG = "QXIMKit";
    //第三方服务器域名
    public static String mCustomDomain = "";

    private static QXFowardCallBack qxFowardCallBack;
    private static QXAtGroupMemberCallBack qxAtGroupMemberCallBack;
    private static QXIMClient.ConnectionStatusListener sConnectionStatusListener;
    private static QXIMClient.OnMessageReceiveListener sOnReceiveMessageListener;
    //IM 主UI消息回调
    private QXIMClient.SendMessageCallback mIMUIMessageCallback;

    private IStatusBar iStatusBar;

    public static final int RESULT_CODE_ADVANCE_CLEAR_MESSAGE = 100;
    public static final int RESULT_CODE_ADVANCE_EXIT_GROUP = RESULT_CODE_ADVANCE_CLEAR_MESSAGE + 1;

    private OnMessageClickedListener onMessageClickedListener;

    private QXIMKit() {
    }

    private QXIMClient.ConnectionStatusListener mConnectionStatusListener = new QXIMClient.ConnectionStatusListener() {
        @Override
        public void onChanged(int code) {
            if (sConnectionStatusListener != null) {
                sConnectionStatusListener.onChanged(code);
            }
            Status status = Status.getStatus(code);
            if (status != null) {
                if (status == Status.KICKED) {
                    EventBusUtil.post(new KickedEvent());
                    PushManager.getInstance().clearCache(QXContext.getInstance().getContext());
                } else if (status == Status.CONNECTED) {
                    setPushLanguage(QXContext.getInstance().getContext());
                    RTCModuleManager.getInstance().onConnected(QXIMClient.getInstance().getMToken(), "");
                }
            }
        }
    };

    private QXIMClient.OnChatNoticeReceivedListener mChatNoticeListener = new QXIMClient.OnChatNoticeReceivedListener() {

        @Override
        public void onGroupGlobalMute(boolean isEnabled) {
            QLog.d(TAG, "全局群组禁言，isEnabled=$isEnabled");
            MuteCache.Companion.setGroupGlobalMute(isEnabled);
        }

        @Override
        public void onGroupMute(@NotNull String groupId, boolean isEnabled) {
            QLog.d(TAG, "群组成员禁言，groupId=$groupId+ isEnabled=$isEnabled");
            MuteCache.Companion.setGroupMute(groupId, isEnabled);
        }

        @Override
        public void onGroupAllMute(@NotNull String groupId, boolean isEnabled) {
            QLog.d(TAG, "群【整体】禁言，groupId=$groupId+ isEnabled=$isEnabled");
            MuteCache.Companion.setGroupAllMute(groupId, isEnabled);
        }

        @Override
        public void onChatRoomGlobalMute(boolean isEnabled) {
            QLog.d(TAG, "全局聊天室禁言，isEnabled=$isEnabled");
            MuteCache.Companion.setChatRoomGlobalMute(isEnabled);
        }

        @Override
        public void onChatRoomBan(@NotNull String chatRoomId, boolean isEnabled) {
            QLog.d(TAG, "聊天室成员封禁，chatRoomId=$chatRoomId+ isEnabled=$isEnabled");

        }

        @Override
        public void onChatRoomMute(@NotNull String chatRoomId, boolean isEnabled) {
            QLog.d(TAG, "聊天室成员禁言，chatRoomId=$chatRoomId+ isEnabled=$isEnabled");
            MuteCache.Companion.setChatRoomMute(chatRoomId, isEnabled);
        }

        @Override
        public void onChatRoomDestroy() {
            QLog.d(TAG, "聊天室销毁");

        }
    };


    public static QXIMKit getInstance() {
        return SingletonHolder.sQXIM;
    }

    public static void init(Context context, String appKey, String imServerUrl) {
        SingletonHolder.sQXIM.initSdk(context, appKey, imServerUrl);
    }

    private void initSdk(Context context, String appKey, String imServerUrl) {
        String current = SystemUtil.getCurrentProcessName(context);
        String mainProcessName = context.getPackageName();
        if (!mainProcessName.equals(current)) {
            QLog.e(TAG, "QXIM current process：" + current);
        } else {
            QXIMClient.getInstance().addOnChatNoticeReceivedListener(mChatNoticeListener);
            QXUserInfoManager.getInstance().init(context, appKey, new QXUserCacheListener());
            QXContext.init(context);
            initListener();
            QXIMClient.init(context, appKey, imServerUrl);
            ModuleManager.init(context);
            StickerManager.getInstance().initSticker();
            QXExtensionManager.init(context, appKey);
            QXExtensionManager.getInstance().registerExtensionModule(new QXDefaultExtensionModule(context));
            RTCModuleManager.init(context, appKey);
            RTCModuleManager.getInstance().onInitialized(appKey);
            AlbumBitmapCacheHelper.init(context);
            StickerManager.getInstance().getDefaultStickerList();
            QXNotificationManager.getInstance().init(QXContext.getInstance());

            //x5内核初始化接口
            initX5Environment(context);
            //初始化基础配置参数
            QXConfigManager.initConfig();
        }
    }

    public QXIMClient.SendMessageCallback getIMUIMessageCallback() {
        return mIMUIMessageCallback;
    }

    public void setIMUIMessageCallback(QXIMClient.SendMessageCallback mIMUIMessageCallback) {
        this.mIMUIMessageCallback = mIMUIMessageCallback;
    }

    /**
     * x5内核初始化接口
     */
    private void initX5Environment(Context context) {
        // 在调用TBS初始化、创建WebView之前进行如下配置
        HashMap map = new HashMap();
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
        QbSdk.initTbsSettings(map);

        //监听下载
        QbSdk.setDownloadWithoutWifi(true);
        QbSdk.setTbsListener(
                new TbsListener() {
                    @Override
                    public void onDownloadFinish(int i) {
                        Log.d("x5内核", "onDownloadFinish -->下载X5内核完成：" + i);
                    }

                    @Override
                    public void onInstallFinish(int i) {
                        Log.d("x5内核", "onInstallFinish -->安装X5内核进度：" + i);
                    }

                    @Override
                    public void onDownloadProgress(int i) {
                        Log.d("x5内核", "onDownloadProgress -->下载X5内核进度：" + i);
                    }
                });

        //监听初始化
        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean arg0) {
                //x5內核初始化完成的回调，true表x5内核加载成功，否则表加载失败，会自动切换到系统内核。
                Log.d("x5内核", arg0 ? "x5内核加载成功" : "x5内核加载失败");
            }

            @Override
            public void onCoreInitFinished() {
                Log.d("x5内核", "x5内核初始化完成");
            }
        };
        QbSdk.initX5Environment(context, cb);
    }

    private void initListener() {
        QXIMClient.setConnectionStatusListener(mConnectionStatusListener);
        QXIMClient.getInstance().setOnMessageReceiveListener(new QXIMClient.OnMessageReceiveListener() {
            @Override
            public void onReceiveNewMessage(@NotNull List<? extends Message> message) {
                if (sOnReceiveMessageListener != null) {
                    sOnReceiveMessageListener.onReceiveNewMessage(message);
                }
                MessageNotificationManager.getInstance().notifyIfNeed(QXContext.getInstance().getContext(),
                        message.get(0), 0);
            }

            @Override
            public void onReceiveRecallMessage(@NotNull Message message) {
                if (sOnReceiveMessageListener != null) {
                    sOnReceiveMessageListener.onReceiveRecallMessage(message);
                }
            }

            @Override
            public void onReceiveInputStatusMessage(@NotNull String from) {
                if (sOnReceiveMessageListener != null) {
                    sOnReceiveMessageListener.onReceiveInputStatusMessage(from);
                }
            }

            @Override
            public void onReceiveHistoryMessage(@NotNull List<? extends Message> message) {
                if (sOnReceiveMessageListener != null) {
                    sOnReceiveMessageListener.onReceiveHistoryMessage(message);
                }
            }

            @Override
            public void onReceiveP2POfflineMessage(@NotNull List<? extends Message> message) {
                if (sOnReceiveMessageListener != null) {
                    sOnReceiveMessageListener.onReceiveP2POfflineMessage(message);
                }
            }

            @Override
            public void onReceiveGroupOfflineMessage(@NotNull List<? extends Message> message) {
                if (sOnReceiveMessageListener != null) {
                    sOnReceiveMessageListener.onReceiveGroupOfflineMessage(message);
                }
            }

            @Override
            public void onReceiveSystemOfflineMessage(@NotNull List<? extends Message> message) {
                if (sOnReceiveMessageListener != null) {
                    sOnReceiveMessageListener.onReceiveSystemOfflineMessage(message);
                }
            }
        });
    }

    public static QXIMClient.OnMessageReceiveListener getOnReceiveMessageListener() {
        return sOnReceiveMessageListener;
    }

    public static void setOnReceiveMessageListener(QXIMClient.OnMessageReceiveListener sOnReceiveMessageListener) {
        QXIMKit.sOnReceiveMessageListener = sOnReceiveMessageListener;
    }

    public static void setConnectionStatusListener(QXIMClient.ConnectionStatusListener listener) {
        sConnectionStatusListener = listener;
    }

    public QXIMClient.ConnectionStatusListener.Status getCurConnectStatus() {
        return QXIMClient.getInstance().getMConnectStatus();
    }

    /**
     * 跳转到聊天UI
     *
     * @param conversationType 会话类型
     * @param targetName       聊天对象名称
     * @param targetId         聊天对象id
     * @param locateMessage    跳转到聊天UI后，需要滚动定位到的消息位置（可为空）
     */
    public void goToChatUI(Context context, String conversationType, String targetName, String targetId, Message locateMessage) {
        String chatType = "";
        switch (conversationType) {
            case Conversation.Type.TYPE_PRIVATE:
                chatType = "private_chat/";
                break;
            case Conversation.Type.TYPE_GROUP:
                chatType = "group_chat/";
                break;
            case Conversation.Type.TYPE_CHAT_ROOM:
                chatType = "chatroom_chat/";
                break;
            case Conversation.Type.TYPE_SYSTEM:
                chatType = "system_chat/";
                break;
        }
        Intent intent = new Intent();
        Uri.Builder builder = Uri.parse("qxim://" + context.getPackageName()).buildUpon();
        builder.appendPath(chatType);
        intent.putExtra("targetId", targetId);
        intent.putExtra("targetName", targetName);
        intent.putExtra("conversationType", conversationType);

        if (locateMessage != null) {
            intent.putExtra("locateMessage", locateMessage);
        }
        Uri uri = builder.build();
        intent.setData(uri);
        intent.setPackage(context.getPackageName());
        context.startActivity(intent);

    }

    public void getAllMessageUnReadCount(List<String> region, QXIMClient.ResultCallback<Integer> callback) {
        QXIMClient.getInstance().getAllUnReadCount(region, callback);
    }

    public void getAllConversation(QXIMClient.ResultCallback<List<Conversation>> callback) {
        QXIMClient.getInstance().getAllConversation(callback);
    }

    public void getConversationInRegion(List<String> region, QXIMClient.ResultCallback<List<Conversation>> callback) {
        QXIMClient.getInstance().getConversationInRegion(region, callback);
    }

    /**
     * @param messageId 根据messageId查具体某一条消息
     */
    public void getMessageById(String messageId, QXIMClient.ResultCallback<Message> callback) {
        QXIMClient.getInstance().getMessageById(messageId, callback);
    }

    /**
     * @param conversationType
     * @param targetId
     * @param timestamp
     * @param searchType       0：搜索timestamp之前的记录，1：搜索timestamp之后的记录
     * @param pageSize         每页条数
     * @param callback
     */
    public void getMessagesByTimestamp(String conversationType, String targetId, long timestamp,
                                       int searchType, int pageSize, QXIMClient.ResultCallback<List<Message>> callback) {
        QXIMClient.getInstance().getMessagesByTimestamp(conversationType, targetId, timestamp, searchType, pageSize, callback);
    }

    /**
     * @param conversationId
     * @param isIgnoreNoDisturbing 是否忽略免打扰（即在免打扰的情况下，也可以获取会话未读数）
     * @param callback
     */
    public void getConversationUnReadCount(String conversationId, boolean isIgnoreNoDisturbing, QXIMClient.ResultCallback<Integer> callback) {
        QXIMClient.getInstance().getConversationUnReadCount(conversationId, isIgnoreNoDisturbing, callback);
    }

    public void getUnReadAtToMessage(String conversationId, QXIMClient.ResultCallback<List<Message>> callback) {
        QXIMClient.getInstance().getUnReadAtMessage(conversationId, callback);
    }

    public void getFirstUnReadMessage(String conversationId, QXIMClient.ResultCallback<Message> callback) {
        QXIMClient.getInstance().getFirstUnReadMessage(conversationId, callback);
    }

    public static void connect(String token, QXIMClient.ConnectCallBack callBack) {
        QXIMClient.connect(token, new QXIMClient.ConnectCallBack() {
            @Override
            public void onSuccess(@Nullable String result) {
                if (callBack != null) {

                    callBack.onSuccess(result);
                }
            }

            @Override
            public void onError(@Nullable String errorCode) {
                if (callBack != null) {
                    callBack.onError(errorCode);
                }
            }

            @Override
            public void onDatabaseOpened(int code) {
                if (callBack != null) {
                    callBack.onDatabaseOpened(code);
                }
            }
        });
    }

    /**
     * 退出应用程序
     */
    public void logout() {
        QXIMClient.getInstance().logout();
    }

    public void sendCustomMessage(String conversationType, String messageType, String senderId,
                                  String targetId, String content, String extra, QXIMClient.SendMessageCallback callback) {

        if (conversationType == null || messageType == null || targetId == null || content == null) {
            callback.onError(QXError.PARAMS_INCORRECT);
            return;
        }

        if (conversationType.isEmpty() || messageType.isEmpty() || targetId.isEmpty() || content.isEmpty()) {
            callback.onError(QXError.PARAMS_INCORRECT);
            return;
        }

        Message message = MessageCreator.Companion.getInstance().createCustomMessage(conversationType, senderId,
                messageType, targetId, content, extra);

        sendMessage(message, new QXIMClient.SendMessageCallback() {
            @Override
            public void onAttached(@org.jetbrains.annotations.Nullable Message message) {
                if (mIMUIMessageCallback != null) {
                    mIMUIMessageCallback.onAttached(message);
                }
                if (callback != null) {
                    callback.onAttached(message);
                }
            }

            @Override
            public void onSuccess() {
                if (mIMUIMessageCallback != null) {
                    mIMUIMessageCallback.onSuccess();
                }
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(@NotNull QXError error, @org.jetbrains.annotations.Nullable Message message) {
                if (mIMUIMessageCallback != null) {
                    mIMUIMessageCallback.onError(error, message);
                }
                if (callback != null) {
                    callback.onError(error, message);
                }
            }
        });
    }

    public void sendMediaMessage(Message message, MediaMessageEmitter.SendMediaMessageCallback callback) {
        try {
            Message temp = this.filterSendMessage(message);
            if (temp != null) {
                if (temp != message) {
                    message = temp;
                }
                MediaMessageEmitter.INSTANCE.send(message, callback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMediaMessage(Context context, String conversationType, String targetId, String messageType,
                                 Uri uri, MediaMessageEmitter.SendMediaMessageCallback callback) {

        try {
            MessageContent messageContent = null;
            if (messageType.equals(MessageType.TYPE_IMAGE)) {
                LocalMedia media = MediaUtil.toLocalMedia(context, uri);
                messageContent = new ImageMessage(media.getPath(), "", media.getSize(), "", media.getWidth(), media.getHeight());
            } else if (messageType.equals(MessageType.TYPE_AUDIO)) {
                LocalMedia media = MediaUtil.toLocalMedia(context, uri);
                messageContent = new AudioMessage(media.getPath(), media.getDuration(), media.getSize(), "");
            } else if (messageType.equals(MessageType.TYPE_VIDEO)) {
                LocalMedia media = MediaUtil.toLocalMedia(context, uri);
                Bitmap bitmap = MediaUtil.getVideoFirstFrame(media.getPath());
                String bitmapPath = BitmapUtil.saveBitmap(bitmap);
                messageContent = new VideoMessage(media.getPath(), media.getDuration(), media.getSize(), bitmapPath, "", media.getWidth(), media.getHeight());
                bitmap.recycle();
            } else if (messageType.equals(MessageType.TYPE_FILE)) {
                LocalMedia media = MediaUtil.toLocalMedia(context, uri);
                String type = FileUtil.getSuffixName(media.getPath());
                messageContent = new FileMessage(media.getPath(), media.getSize(), media.getName(), "", type);
            }
            if (messageContent != null) {
                Message message = Message.obtain(QXIMClient.getInstance().getCurUserId(), targetId, conversationType, messageType, messageContent);
                sendMediaMessage(message, callback);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void sendMessage(Message message, final QXIMClient.SendMessageCallback callback) {
        try {
            Message temp = this.filterSendMessage(message);
            if (temp != null) {
                if (temp != message) {
                    message = temp;
                }
                QXIMClient.getInstance().sendMessage(message, new QXIMClient.SendMessageCallback() {
                    @Override
                    public void onError(@NotNull QXError error, @org.jetbrains.annotations.Nullable Message message) {
                        if (callback != null) {
                            callback.onError(error, message);
                        }
                    }

                    @Override
                    public void onAttached(@Nullable Message message) {
                        if (callback != null) {
                            callback.onAttached(message);
                        }

                    }

                    @Override
                    public void onSuccess() {
                        if (callback != null) {
                            callback.onSuccess();
                        }

                    }

                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private Message filterSendMessage(String converstationType, String messageType, String targetId) {
        Message message = new Message();
        message.setConversationType(converstationType);
        message.setTargetId(targetId);
        message.setMessageType(messageType);
        return this.filterSendMessage(message);
    }

    private Message filterSendMessage(Message message) {
        QLog.d(TAG, "filterSendMessage message ");
        if (QXContext.getInstance() == null) {
            QLog.e(TAG, "filterSendMessage QXContext is null");
        } else {
            if (QXContext.getInstance().getOnSendMessageListener() != null) {
                message = QXContext.getInstance().getOnSendMessageListener().onSend(message);
            }
        }
        return message;
    }

    public void searchConversations(String keyWord, String[] conversationTypes, String[] messageTypes,
                                    QXIMClient.ResultCallback<List<SearchConversationResult>> callback) {
        QXIMClient.getInstance().searchConversations(keyWord, conversationTypes, messageTypes, callback);
    }

    public void deleteMessage(String messageId, QXIMClient.OperationCallback callback) {
        String[] ids = {messageId};
        QXIMClient.getInstance().deleteLocalMessageById(ids, callback);
    }

    public void updateCustomMessage(String conversationId, String messageId, String content, String extra, QXIMClient.OperationCallback callback) {
        QXIMClient.getInstance().updateCustomMessage(conversationId, messageId, content, extra, callback);
    }

    public void updateAtMessageReadState(String messageId, String conversationId, int read, QXIMClient.OperationCallback callback) {
        QXIMClient.getInstance().updateAtMessageReadState(messageId, conversationId, read, callback);
    }

    public void clearAtMessage(String conversationId, QXIMClient.OperationCallback callback) {
        QXIMClient.getInstance().clearAtMessage(conversationId, callback);
    }

    public void updateConversationBackground(String conversationId, String url, QXIMClient.OperationCallback callback) {
        QXIMClient.getInstance().updateConversationBackground(conversationId, url, callback);
    }

    public void updateConversationTitle(String type, String targetId, String title, QXIMClient.OperationCallback callback) {
        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(targetId) && !TextUtils.isEmpty(type)) {
            QXIMClient.getInstance().updateConversationTitle(type, targetId, title, callback);
        }
    }

    public void updateConversationIcon(String type, String targetId, String icon, QXIMClient.OperationCallback callback) {
        if (!TextUtils.isEmpty(icon) && !TextUtils.isEmpty(targetId) && !TextUtils.isEmpty(type)) {
            QXIMClient.getInstance().updateConversationIcon(type, targetId, icon, callback);
        }
    }

    public void updateConversationTitleAndIcon(String type, String targetId, String title, String icon, QXIMClient.OperationCallback callback) {
        if (!TextUtils.isEmpty(icon) && !TextUtils.isEmpty(targetId) && !TextUtils.isEmpty(type)) {
            QXIMClient.getInstance().updateConversationTitleAndIcon(type, targetId, title, icon, callback);
        }
    }

    public void deleteConversation(String conversationId, QXIMClient.OperationCallback callback) {
        QXIMClient.getInstance().deleteConversation(conversationId, callback);
    }

    public void deleteConversation(String conversationType, String targetId, QXIMClient.OperationCallback callback) {
        QXIMClient.getInstance().deleteConversation(conversationType, targetId, callback);
    }

    public void refreshUserInfoCache(QXUserInfo qxUserInfo) {
        if (qxUserInfo != null) {
            QXUserInfoManager.getInstance().setUserInfo(qxUserInfo);
            if (QXContext.getInstance() != null) {
                String title = qxUserInfo.getDisplayName();
                String avatarUri = "";
                if (qxUserInfo.getAvatarUri() != null) {
                    avatarUri = qxUserInfo.getAvatarUri().toString();
                }

                if (TextUtils.isEmpty(title)) {
                    title = qxUserInfo.getName();
                }

                if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(avatarUri)) {
                    updateConversationTitleAndIcon(Conversation.Type.TYPE_PRIVATE, qxUserInfo.getId(), title, avatarUri, new QXIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailed(@NotNull QXError qxError) {
                        }
                    });
                }
//                else {
//                    if (!TextUtils.isEmpty(title)) {
//                        updateConversationTitle(Conversation.Type.TYPE_PRIVATE, qxUserInfo.getId(), title, new QXIMClient.OperationCallback() {
//                            @Override
//                            public void onSuccess() {
//                            }
//
//                            @Override
//                            public void onFailed(@NotNull QXError qxError) {
//                            }
//                        });
//                    } else {
//                        Log.e(TAG, "refreshUserInfoCache: 刷新用户信息，但是Title为空");
//                    }
//                    if (!TextUtils.isEmpty(avatarUri)) {
//                        updateConversationIcon(Conversation.Type.TYPE_PRIVATE, qxUserInfo.getId(), avatarUri, new QXIMClient.OperationCallback() {
//                            @Override
//                            public void onSuccess() {
//                            }
//
//                            @Override
//                            public void onFailed(@NotNull QXError qxError) {
//                            }
//                        });
//                    } else {
//                        Log.e(TAG, "refreshUserInfoCache: 刷新用户信息，但是Icon为空");
//                    }
//                }

                QXUserInfo curUserInfo = QXContext.getInstance().getCurrentUserInfo();
                if (curUserInfo != null && curUserInfo.getId().equals(qxUserInfo.getId())) {
                    QXContext.getInstance().setCurrentUserInfo(qxUserInfo);
                }
            }
        }
    }

    public void refreshGroupUserInfoCache(QXGroupUserInfo groupUserInfo) {
        if (groupUserInfo != null) {
            QXUserInfoManager.getInstance().setGroupUserInfo(groupUserInfo);
        }
    }

    public void refreshGroupInfoCache(QXGroupInfo qxGroupInfo) {
        if (qxGroupInfo != null) {
            QXUserInfoManager.getInstance().setGroupInfo(qxGroupInfo);

            if (!TextUtils.isEmpty(qxGroupInfo.getName()) && qxGroupInfo.getAvatarUri() != null) {
                updateConversationTitleAndIcon(Conversation.Type.TYPE_GROUP, qxGroupInfo.getId(), qxGroupInfo.getName(), qxGroupInfo.getAvatarUri().toString(), new QXIMClient.OperationCallback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onFailed(@NotNull QXError qxError) {
                    }
                });
            }
//            else {
//                if (!TextUtils.isEmpty(qxGroupInfo.getName())) {
//                    updateConversationTitle(Conversation.Type.TYPE_GROUP, qxGroupInfo.getId(), qxGroupInfo.getName(), new QXIMClient.OperationCallback() {
//                        @Override
//                        public void onSuccess() {
//                        }
//
//                        @Override
//                        public void onFailed(@NotNull QXError qxError) {
//                        }
//                    });
//                } else {
//                    Log.e(TAG, "refreshUserInfoCache: 刷新群信息，但是群Title为空");
//                }
//                if (qxGroupInfo.getAvatarUri() != null) {
//                    updateConversationIcon(Conversation.Type.TYPE_GROUP, qxGroupInfo.getId(), qxGroupInfo.getAvatarUri().toString(), new QXIMClient.OperationCallback() {
//                        @Override
//                        public void onSuccess() {
//                        }
//
//                        @Override
//                        public void onFailed(@NotNull QXError qxError) {
//                        }
//                    });
//                } else {
//                    Log.e(TAG, "refreshUserInfoCache: 刷新群信息，但是群Icon为空");
//                }
//            }
        }
    }

    public void setPushLanguage(Context context) {
//        String language = SPUtils.getCacheLanguage(context);
//        if (!TextUtils.isEmpty(language)) {
            UserProperty userProperty = new UserProperty();
          //  userProperty.setLanguage(language);
            setUserProperty(userProperty);
       // }
    }

    public void setUserProperty(UserProperty userProperty) {
        QXIMClient.getInstance().setUserProperty(userProperty, new QXIMClient.ResultCallback<String>() {
            @Override
            public void onSuccess(String data) {

            }

            @Override
            public void onFailed(@NotNull QXError error) {

            }
        });
    }

    public void setUserProperty(UserProperty userProperty, QXIMClient.ResultCallback callback) {
        QXIMClient.getInstance().setUserProperty(userProperty, callback);
    }

    /**
     * 检查敏感词
     *
     * @param text
     * @param callback
     */
    public void checkSensitiveWord(String text, QXIMClient.ResultCallback<SensitiveWordResult> callback) {
        QXIMClient.getInstance().checkSensitiveWord(text, callback);
    }

    public String getCurUserId() {
        return QXIMClient.getInstance().getCurUserId();
    }

    public void registerCustomEventProvider(ICustomEventProvider provider, QXIMClient.OperationCallback callback) {
//        LogUtil.debug(this.getClass(), "provider=" + provider);
        QXIMClient.getInstance().registerCustomEventProvider(provider, callback);
    }


    /**
     * 设置用户信息提供者，聊天UI需要拿到用户信息进行显示头像、昵称等
     *
     * @param provider
     * @param isCache
     */
    public static void setQXUserInfoProvider(QXUserInfoProvider provider, boolean isCache) {
        QXContext.getInstance().setUserInfoProvider(provider, isCache);
    }

    public static void setQXGroupProvider(QXGroupInfoProvider provider, boolean isCache) {
        QXContext.getInstance().setGroupProvider(provider, isCache);
    }

    public static void setQXGroupUserInfoProvider(QXGroupUserInfoProvider provider, boolean isCache) {
        QXContext.getInstance().setGroupUserInfoProvider(provider, isCache);
    }

    public static void setQXUploadProvider(QXUploadProvider provider) {
        QXContext.getInstance().setUploadProvider(provider);
    }

    public static void setQXDownloadProvider(QXDownloadProvider provider) {
        QXContext.getInstance().setDownloadProvider(provider);
    }

    public static void setFavoriteProvider(QXFavoriteProvider provider) {
        QXContext.getInstance().setFavoriteProvider(provider);
    }

    public static void setGroupNoticeProvider(QXGetGroupNoticeProvider provider) {
        QXContext.getInstance().setQXGetGroupNotice(provider);
    }

    public static void setChatBackgroundProvider(QXChatBackgroundProvider provider) {
        QXContext.getInstance().setChatBackgroundProvider(provider);
    }

    public static void setSelectGroupMemberProvider(QXSelectGroupMemberProvider provider) {
        QXContext.getInstance().setSelectGroupMemberProvider(provider);
    }

    public static void setSelectTargetProvider(QXSelectTargetProvider provider) {
        QXContext.getInstance().setSelectTargetProvide(provider);
    }

    public static void setUIEventProvider(QXUIEventProvider provider) {
        QXContext.getInstance().setUIEventProvider(provider);
    }

    public static void setSendMessageFailedProvider(QXSendMessageFailedProvider provider) {
        QXContext.getInstance().setSendMessageFiledProvider(provider);
    }

    public static QXSendMessageFailedProvider getSendMessageFailedProvider() {
        return QXContext.getInstance().getSendMessageFiledProvider();
    }

    public interface QXGroupInfoProvider {
        QXGroupInfo getGroupInfo(String groupId);
    }

    public interface QXGroupUserInfoProvider {
        QXGroupUserInfo getGroupUserInfo(String groupId, String userId);
    }

    public interface QXUserInfoProvider {
        QXUserInfo getUserInfo(String userId);
    }

    public interface QXUploadProvider {
        void upload(FileType type, String filePath, UploadCallback callback);
    }

    public interface QXDownloadProvider {
        void download(FileType type, long length, String url, DownloadCallback callback);
    }

    public void setCheckSendingMessageListener(QXIMClient.CheckSendingMessageListener listener) {
        QXIMClient.getInstance().setCheckSendingMessageListener(listener);
    }

    public enum FileType {
        /**
         * 文件
         */
        TYPE_FILE,
        /**
         * 图片
         */
        TYPE_IMAGE,
        /**
         * 视频
         */
        TYPE_VIDEO,
        /**
         * 语音
         */
        TYPE_VOICE
    }

    public interface QXChatBackgroundProvider {
        void getBackground(int type, String targetId, QXChatBackgroundCallback callback);

        interface QXChatBackgroundCallback {
            void onSuccess(String imgUrl);

            void onFailed(int code, String msg);
        }
    }

    public interface QXFavoriteProvider {
        void onSave(List<QXFavorite> favorites, QXFavoriteCallback callback);

        CollectionItem queryCollection(String url);

        RecordItem queryRecord(String url);

        long insertCollection(CollectionItem item);

        long insertRecord(RecordItem item);

        interface QXFavoriteCallback {
            void onSuccess();

            void onFailed(int code, String msg);
        }
    }

    public interface QXSendMessageFailedProvider {
        void onFailed(String errorMsg, Message message, View errorRootView);
    }

    public interface QXUIEventProvider {
        /**
         * 聊天UI返回点击事件
         *
         * @param activity       上下文
         * @param type           会话类型
         * @param targetId       目标id，可以是用户id或群组id
         * @param conversationId 会话id
         */
        void onChatBackClick(Activity activity, String type, String targetId, String conversationId);

        /**
         * 聊天UI菜单点击事件
         *
         * @param activity       上下文
         * @param type           会话类型
         * @param targetId       目标id，可以是用户id或群组id
         * @param conversationId 会话id
         */
        void onChatMenuClick(Activity activity, int requestCode, String type, String targetId, String conversationId);

        /**
         * 聊天UI公告点击事件
         *
         * @param activity       上下文
         * @param targetId       群组id
         * @param conversationId 会话id
         * @param noticeContent  公告内容
         */
        void onGroupNoticeClick(Activity activity, String targetId, String conversationId, String noticeContent);

        /**
         * 聊天UI头点击事件
         *
         * @param activity
         * @param conversationType 会话类型
         * @param groupId          群组id，只有当会话类型为群组的时候，该字段才不为空
         * @param userId           被点击的用户id
         */
        void onAvatarClick(Activity activity, String conversationType, String groupId, String userId);
    }

    public interface QXSelectGroupMemberProvider {
        void selectMember(Activity activity, String groupId);
    }

    public interface QXSelectTargetProvider {
        void selectTarget(Context context);
    }

    public interface QXGetGroupNoticeProvider {
        void getGroupNotice(String groupId, QXGetGroupNoticeCallback callback);

        interface QXGetGroupNoticeCallback {
            void onSuccess(QXGroupNotice notice);

            void onFailed(int code, String msg);
        }
    }

    public interface QXStickerProvider {
        /**
         * 获取表情
         *
         * @param userId
         * @param category 系统，自定义
         * @param callback
         */
        void getAllSticker(String userId, String category, QXStickerCallback callback);

        boolean isHasSticker(String originUrl);

        interface QXStickerCallback {
            void onSticker(List<StickerItem> data);
        }
    }


    public interface QXCustomDomainProvider {
        String getCustomDomain();
    }

    public void setCustomDomainProvider(QXCustomDomainProvider provider) {
        QXContext.getInstance().setCustomDomainProvider(provider);
    }

    public String getRealUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        String baseDomain = QXContext.getInstance().getCustomDomainProvider().getCustomDomain();

        if (TextUtils.isEmpty(baseDomain)) {
            return url;
        }

        if (url.startsWith("http://")) {
            url = url.replace("http://", "");
        } else if (url.startsWith("https://")) {
            url = url.replace("https://", "");
        } else {
            //否则为本地路径
            return url;
        }
        int index = url.indexOf("/");
        if (index == -1) {
            return url;
        }

        if (baseDomain.lastIndexOf("/") != -1) {
            if (baseDomain.length() > 0) {
                // baseDomain = baseDomain.substring(0, baseDomain.length() -1);
            }
        }

        url = url.substring(index, url.length());
        return baseDomain + url;
    }

    /**
     * at 群成员
     */
    public interface QXAtGroupMemberCallBack {
        void selectGroupMember(List<Member> list);
    }

    public static void setQXAtGroupMemberData(List<Member> list) {
        if (qxAtGroupMemberCallBack != null) {
            qxAtGroupMemberCallBack.selectGroupMember(list);
        }
    }

    public static void setQXAtGroupMemberCallBack(QXAtGroupMemberCallBack callBack) {
        qxAtGroupMemberCallBack = callBack;
    }

    /**
     * at 群成员
     */
    public interface QXFowardCallBack {
        void setFowardData(List<TargetItem> list, boolean forwardEndContinue);
    }

    public static void setQXFowardDataCallBack(QXFowardCallBack callBack) {
        qxFowardCallBack = callBack;
    }

    public static void setQXFowardData(List<TargetItem> list, boolean forwardEndContinue) {
        if (qxFowardCallBack != null) {
            qxFowardCallBack.setFowardData(list, forwardEndContinue);
        }
    }


    public static void setEmotionProvider(QXStickerProvider emotionProvider) {
        QXContext.getInstance().setEmotionStickerProvider(emotionProvider);
    }

    public static void setConversationStickerClickListener(ConversationStickerClickListener listener) {
        QXContext.getInstance().setConversationStickerClickListener(listener);
    }

    public static void removeCallBack() {
        qxFowardCallBack = null;
        qxAtGroupMemberCallBack = null;
    }

    public void setStatusBar(IStatusBar activityStatusBar) {
        iStatusBar = activityStatusBar;
    }

    public IStatusBar getStatusBarImpl() {
        return iStatusBar;
    }

    public interface IStatusBar {
        void setStatusBar(Activity activity);
    }

    /**
     * 获取会话是否消息免打扰
     */
    public static void getConversationNotificationStatus(String conversationType, String targetId) {

    }

    /**
     * 会话点击事件
     */
    public interface ConversationClickListener {
        boolean onUserPortraitClick(Context context, String conversationType, QXUserInfo user, String targetId);

        boolean onUserPortraitLongClick(Context context, String conversationType, QXUserInfo user, String targetId);

        boolean onMessageClick(Context context, View view, Message message);

        boolean onMessageLinkClick(Context context, String targetId, Message message);

        boolean onMessageLongClick(Context context, View view, Message message);
    }

    public static void setConversationClickListener(ConversationClickListener listener) {
        if (QXContext.getInstance() != null) {
            QXContext.getInstance().setConversationClickListener(listener);
        }

    }

    /**
     * 设置表情事件
     */
    public interface ConversationStickerClickListener {
        /**
         * 长按添加事件
         *
         * @param context
         */
        void addSticker(Context context, StickerItem stickerItem, QXStickerOperationCallback callback);

        /**
         * 表情面板管理事件
         *
         * @param context
         */
        void managerSticker(Context context);

        /**
         * 删除单个表情事件
         *
         * @param context
         * @param stickerItem
         */
        void delSticker(Context context, StickerItem stickerItem, QXStickerOperationCallback callback);

        /**
         * 表情长按
         *
         * @param context
         * @param stickerItem
         */
        void onLongClickSticker(Context context, StickerItem stickerItem, QXStickerOperationCallback callback);

        interface QXStickerOperationCallback {
            void onSuccess();

            void onFail();
        }
    }


    /**
     * 覆盖消息内置菜单点击事件
     */
    public void setMenuOnClickListener(MenuType menuType, QXMessageLongClickManager.QXMessageLongListener listener) {
        if (QXMessageLongClickManager.getInstance() != null) {
            QXMessageLongClickManager.getInstance().addQxMessageLongListener(menuType, listener);
        }
    }

    /**
     * 添加指定消息自定义的菜单
     *
     * @param messsageType
     */
    public void addQxMessageCustomMenu(String messsageType, QXMessageLongClickManager.QXMessageLongListener listener, MenuActionType... menuActionTypes) {
        if (QXMessageLongClickManager.getInstance() != null) {
            QXMessageLongClickManager.getInstance().addQxMessageCustomLongListener(messsageType, listener, menuActionTypes);
        }
    }

    /**
     * 为指定消息类型添加菜单
     *
     * @param messageType 消息类型
     * @param menu        菜单
     */
    public void addQXMenu(String messageType, QXMenu menu) {
        QXMenuManager.getInstance().addMenuToList(messageType, menu);
    }

    /**
     * 为指定消息类型添加菜单
     *
     * @param index       位置
     * @param messageType 消息类型
     * @param menu        菜单
     */
    public void addQXMenu(int index, String messageType, QXMenu menu) {
        QXMenuManager.getInstance().addMenuToList(index, messageType, menu);
    }

    /**
     * 移除指定的消息类型，指定的菜单
     *
     * @param messsageType
     * @param menuType
     */
    public void removeQxMessageMenu(String messsageType, MenuType... menuType) {
        if (QXMessageLongClickManager.getInstance() != null) {
            QXMessageLongClickManager.getInstance().removeDefaultMenu(messsageType, menuType);
        }
    }

    /**
     * 移除指定的消息类型全部菜单
     *
     * @param messsageType
     */
    public void removeQxMessageAllMenus(String messsageType) {
        if (QXMessageLongClickManager.getInstance() != null) {
            QXMessageLongClickManager.getInstance().removeDefaultAllMenus(messsageType);
        }
    }


    public void setSendMessageListener(OnSendMessageListener listener) {
        if (QXContext.getInstance() != null) {
            QXContext.getInstance().setOnSendMessageListener(listener);
        }

    }

    /**
     * 消息发送之前的回调
     */
    public interface OnSendMessageListener {

        Message onSend(Message message);

        boolean onSent(Message message, String error);
    }

    public interface OnMessageClickedListener {
        void onMessageClicked(Message message);
    }

    public OnMessageClickedListener getOnMessageClickedListener() {
        return onMessageClickedListener;
    }

    public void setOnMessageClickedListener(OnMessageClickedListener onMessageClickedListener) {
        this.onMessageClickedListener = onMessageClickedListener;
    }

    public void setMessageClicked(Message message) {
        if (onMessageClickedListener != null) {
            onMessageClickedListener.onMessageClicked(message);
        }
    }

    static class SingletonHolder {
        static QXIMKit sQXIM = new QXIMKit();

        SingletonHolder() {

        }
    }

}
