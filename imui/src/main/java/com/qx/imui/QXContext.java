package com.qx.imui;

import android.content.Context;
import android.net.Uri;

import com.qx.im.model.QXUserInfo;
import com.qx.imui.adapter.QXUserInfoManager;
import com.qx.push.QXPushClient;

import org.greenrobot.eventbus.EventBus;

import androidx.annotation.StringRes;

public class QXContext {

    private QXIMKit.QXUserInfoProvider mUserInfoProvider;
    private QXIMKit.QXGroupInfoProvider mGroupProvider;
    private QXIMKit.QXGroupUserInfoProvider mGroupUserInfoProvider;
    private QXIMKit.QXUploadProvider mUploadProvider;
    private QXIMKit.QXDownloadProvider mDownloadProvider;
    private QXIMKit.QXFavoriteProvider mFavoriteProvider;
    private QXIMKit.QXChatBackgroundProvider mChatBackgroundProvider;
    private QXIMKit.QXSelectGroupMemberProvider mSelectGroupMemberProvider;
    private QXIMKit.QXSelectTargetProvider mSelectTargetProvider;
    private QXIMKit.QXGetGroupNoticeProvider mQXGetGroupNotice;
    private QXIMKit.QXCustomDomainProvider mCustomDomainProvider;

    private QXIMKit.ConversationStickerClickListener mConversationEmotionClickListener;
    private QXIMKit.QXStickerProvider mEmotionStickerProvider;
    private QXIMKit.QXUIEventProvider mQXUIEventProvider;
    private QXIMKit.QXSendMessageFailedProvider mSendMessageFiledProvider;
    private QXIMKit.ConversationClickListener mConversationClickListener;
    private QXIMKit.OnSendMessageListener mOnSendMessageListener;

    private static QXContext sContext;
    private Context mContext;
    private Uri notificationSound;
    private EventBus mBus = EventBus.getDefault();

    public QXUserInfo getCurrentUserInfo() {
        return mCurrentUserInfo;
    }

    public void setCurrentUserInfo(QXUserInfo mCurrentUserInfo) {
        this.mCurrentUserInfo = mCurrentUserInfo;
    }

    private QXUserInfo mCurrentUserInfo;

    private QXContext(Context context) {
        this.mContext = context;
    }

    public static void init(Context context) {
        sContext = new QXContext(context);
    }

    public QXIMKit.QXUserInfoProvider getUserInfoProvider() {
        return mUserInfoProvider;
    }

    public void setUserInfoProvider(QXIMKit.QXUserInfoProvider userInfoProvider, boolean isCache) {
        this.mUserInfoProvider = userInfoProvider;
        QXUserInfoManager.getInstance().setIsCacheUserInfo(isCache);
    }

    public QXIMKit.QXGroupInfoProvider getGroupProvider() {
        return mGroupProvider;
    }

    public void setGroupProvider(QXIMKit.QXGroupInfoProvider groupProvider, boolean isCache) {
        this.mGroupProvider = groupProvider;
        QXUserInfoManager.getInstance().setIsCacheGroupInfo(isCache);
    }

    public QXIMKit.QXGroupUserInfoProvider getGroupUserInfoProvider() {
        return mGroupUserInfoProvider;
    }

    public void setGroupUserInfoProvider(QXIMKit.QXGroupUserInfoProvider groupUserInfoProvider, boolean isCache) {
        this.mGroupUserInfoProvider = groupUserInfoProvider;
        QXUserInfoManager.getInstance().setIsCacheGroupUserInfo(isCache);
    }


    public QXIMKit.QXUploadProvider getUploadProvider() {
        return mUploadProvider;
    }

    public void setUploadProvider(QXIMKit.QXUploadProvider mUploadProvider) {
        this.mUploadProvider = mUploadProvider;
    }

    public QXIMKit.QXDownloadProvider getDownloadProvider() {
        return mDownloadProvider;
    }

    public void setDownloadProvider(QXIMKit.QXDownloadProvider mDownloadProvider) {
        this.mDownloadProvider = mDownloadProvider;
    }

    public void setFavoriteProvider(QXIMKit.QXFavoriteProvider provider) {
        mFavoriteProvider = provider;
    }

    public void setCustomDomainProvider(QXIMKit.QXCustomDomainProvider provider) {
        mCustomDomainProvider = provider;
    }

    public QXIMKit.QXCustomDomainProvider getCustomDomainProvider() {
        return mCustomDomainProvider;
    }

    public QXIMKit.QXFavoriteProvider getFavoriteProvider() {
        return mFavoriteProvider;
    }

    public QXIMKit.QXChatBackgroundProvider getChatBackgroundProvider() {
        return mChatBackgroundProvider;
    }

    public void setChatBackgroundProvider(QXIMKit.QXChatBackgroundProvider mChatBackgroundProvider) {
        this.mChatBackgroundProvider = mChatBackgroundProvider;
    }


    public QXIMKit.QXSelectGroupMemberProvider getSelectGroupMemberProvider() {
        return mSelectGroupMemberProvider;
    }

    public void setSelectGroupMemberProvider(QXIMKit.QXSelectGroupMemberProvider mSelectGroupMemberProvider) {
        this.mSelectGroupMemberProvider = mSelectGroupMemberProvider;
    }

    public QXIMKit.QXSelectTargetProvider getSelectTargetProvider() {
        return mSelectTargetProvider;
    }

    public void setSelectTargetProvide(QXIMKit.QXSelectTargetProvider mSelectTargetProvider) {
        this.mSelectTargetProvider = mSelectTargetProvider;
    }


    public QXIMKit.QXGetGroupNoticeProvider getQXGetGroupNotice() {
        return mQXGetGroupNotice;
    }

    public void setQXGetGroupNotice(QXIMKit.QXGetGroupNoticeProvider mQXGetGroupNotice) {
        this.mQXGetGroupNotice = mQXGetGroupNotice;

    }

    public void setConversationStickerClickListener(QXIMKit.ConversationStickerClickListener mConversationEmotionClickListener) {
        this.mConversationEmotionClickListener = mConversationEmotionClickListener;
    }

    public QXIMKit.ConversationStickerClickListener getConversationEmotionClickListener() {
        return mConversationEmotionClickListener;
    }

    public QXIMKit.QXStickerProvider getEmotionStickerProvider() {
        return mEmotionStickerProvider;
    }

    public void setEmotionStickerProvider(QXIMKit.QXStickerProvider mEmotionStickerProvider) {
        this.mEmotionStickerProvider = mEmotionStickerProvider;
    }

    public QXIMKit.QXUIEventProvider getUIEventProvider() {
        return mQXUIEventProvider;
    }

    public void setUIEventProvider(QXIMKit.QXUIEventProvider mQXUIEventProvider) {
        this.mQXUIEventProvider = mQXUIEventProvider;
    }

    public QXIMKit.QXSendMessageFailedProvider getSendMessageFiledProvider() {
        return mSendMessageFiledProvider;
    }

    public void setSendMessageFiledProvider(QXIMKit.QXSendMessageFailedProvider mSendMessageFiledProvider) {
        this.mSendMessageFiledProvider = mSendMessageFiledProvider;
    }

    public static QXContext getInstance() {
        return sContext;
    }

    public Context getContext() {
        return sContext.mContext;
    }

    public static String getString(@StringRes int resId, Object... formatArgs) {
        return sContext.mContext.getString(resId, formatArgs);
    }

    public Object getSystemService(String name) {
        return sContext.mContext.getSystemService(name);
    }

    public void setNotificationSound(Uri uri) {
        this.notificationSound = uri;
        QXPushClient.setNotifiationSound(uri);
    }

    public Uri getNotificationSound() {
        return this.notificationSound;
    }

    public EventBus getEventBus() {
        return this.mBus;
    }

    public void setConversationClickListener(QXIMKit.ConversationClickListener conversationClickListener) {
        this.mConversationClickListener = conversationClickListener;
    }

    public QXIMKit.ConversationClickListener getConversationClickListener() {
        return mConversationClickListener;
    }

    public QXIMKit.OnSendMessageListener getOnSendMessageListener() {
        return this.mOnSendMessageListener;
    }

    public void setOnSendMessageListener(QXIMKit.OnSendMessageListener onSendMessageListener) {
        this.mOnSendMessageListener = onSendMessageListener;
    }

}

