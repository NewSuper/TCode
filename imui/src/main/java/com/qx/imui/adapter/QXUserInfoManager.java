package com.qx.imui.adapter;


import android.content.Context;
import android.text.TextUtils;
import android.util.LruCache;

import com.qx.im.model.QXGroupInfo;
import com.qx.im.model.QXGroupUserInfo;
import com.qx.im.model.QXUserInfo;

import java.util.ArrayList;
import java.util.List;

public class QXUserInfoManager {
    private static class SingletonHolder{
        static QXUserInfoManager sInstance = new QXUserInfoManager();

        private SingletonHolder(){

        }
    }

    private LruCache<String, QXUserInfo> userInfoLruCache;
    private LruCache<String, QXGroupUserInfo>groupUserInfoLruCache;
    private LruCache<String, QXGroupInfo>groupInfoLruCache;
    private boolean mIsCacheUserInfo;
    private boolean mIsCacheGroupInfo;
    private boolean mIsCacheGroupUserInfo;
    private String mAppKey;
    private Context mContext;
    private IQXCacheListener mCacheListener;
    private boolean mInitialized;
    private List<String> groupUserInfoKey = new ArrayList<>();

    private QXUserInfoManager(){
        mInitialized = false;
        userInfoLruCache = new LruCache<>(256);
        groupInfoLruCache = new LruCache<>(256);
        groupUserInfoLruCache = new LruCache<>(256);
    }
    public void  init(Context context,String appKey,IQXCacheListener listener){
        if (TextUtils.isEmpty(appKey)){

        }else if (mInitialized){

        }else {
            mContext = context;
            mAppKey = appKey;
            mCacheListener = listener;
            mInitialized = true;
        }
    }
    public static QXUserInfoManager getInstance() {
        return QXUserInfoManager.SingletonHolder.sInstance;
    }

    public void setIsCacheUserInfo(boolean mIsCacheUserInfo) {
        this.mIsCacheUserInfo = mIsCacheUserInfo;
    }

    public void setIsCacheGroupInfo(boolean mIsCacheGroupInfo) {
        this.mIsCacheGroupInfo = mIsCacheGroupInfo;
    }

    public void setIsCacheGroupUserInfo(boolean mIsCacheGroupUserInfo) {
        this.mIsCacheGroupUserInfo = mIsCacheGroupUserInfo;
    }

    public void setUserInfo(QXUserInfo qxUserInfo) {
        if (this.mIsCacheUserInfo) {
            if (!TextUtils.isEmpty(qxUserInfo.getId())) {
                this.userInfoLruCache.put(qxUserInfo.getId(), qxUserInfo);
            }
        }
        if (this.mCacheListener != null) {
            this.mCacheListener.onUserInfoUpdated(qxUserInfo);
        }
    }

    public void setGroupInfo(QXGroupInfo qxGroupInfo) {
        if (this.mIsCacheGroupInfo) {
            if (!TextUtils.isEmpty(qxGroupInfo.getId())) {
                this.groupInfoLruCache.put(qxGroupInfo.getId(), qxGroupInfo);
            }
        }
        if (this.mCacheListener != null) {
            this.mCacheListener.onGroupUpdated(qxGroupInfo);
        }
    }

    public QXUserInfo getUserInfo(String userId) {
        if (TextUtils.isEmpty(userId))
            return null;
        QXUserInfo userInfo = null;
        if (this.mIsCacheUserInfo) {
            userInfo = this.userInfoLruCache.get(userId);
            if (userInfo != null) {
                return userInfo;
            }
        }

        return this.mCacheListener.getUserInfo(userId);
    }

    public void requestUserInfoUpdate(String userId) {
        mCacheListener.getUserInfo(userId);
    }

    public void requestGroupInfoUpdate(String groupId) {
        mCacheListener.getGroupInfo(groupId);
    }

    public void removeUserInfo(String userId) {
        if (this.mIsCacheUserInfo && this.userInfoLruCache != null) {
            this.userInfoLruCache.remove(userId);
        }
    }

    public void removeGroupUserInfo(String userId) {
        for (String key : groupUserInfoKey) {
            if (key.endsWith(userId)) {
                if (this.mIsCacheGroupInfo && this.groupUserInfoLruCache != null) {
                    this.groupUserInfoLruCache.remove(key);
                    return;
                }
            }
        }
    }

    public void setGroupUserInfo(QXGroupUserInfo qxGroupUserInfo) {
        if (this.mIsCacheGroupUserInfo) {
            String key = qxGroupUserInfo.getGroupId() + "_tqxd_" + qxGroupUserInfo.getUserId();
            if (!groupUserInfoKey.contains(key)) {
                groupUserInfoKey.add(key);
            }
            this.groupUserInfoLruCache.put(key, qxGroupUserInfo);
        }
        if (this.mCacheListener != null) {
            this.mCacheListener.onGroupUserInfoUpdated(qxGroupUserInfo);
        }
    }

    public QXGroupInfo getGroup(String groupId) {
        if (TextUtils.isEmpty(groupId))
            return null;
        QXGroupInfo qxGroupInfo = null;
        if (this.mIsCacheGroupInfo) {
            qxGroupInfo = this.groupInfoLruCache.get(groupId);
            // 如果缓存没有
            if (qxGroupInfo == null) {
                // 调用第三方的信息提供
                qxGroupInfo = this.mCacheListener.getGroupInfo(groupId);
            }
        } else if (this.mCacheListener != null) {
            qxGroupInfo = this.mCacheListener.getGroupInfo(groupId);
        }
        return qxGroupInfo;
    }

    public QXGroupInfo refreshGroupInfo(String groupId) {
        return this.mCacheListener.getGroupInfo(groupId);
    }

    public QXGroupUserInfo getGroupUserInfo(String groupId, String userId) {
        if (TextUtils.isEmpty(groupId) || TextUtils.isEmpty(userId))
            return null;
        QXGroupUserInfo qxGroupUserInfo = null;
        String key = groupId + "_tqxd_" + userId;
        if (this.mIsCacheGroupUserInfo) {
            qxGroupUserInfo = this.groupUserInfoLruCache.get(key);
            if (qxGroupUserInfo == null) {
                qxGroupUserInfo = this.mCacheListener.getGroupUserInfo(groupId, userId);
            }
        } else if (this.mCacheListener != null) {
            qxGroupUserInfo = this.mCacheListener.getGroupUserInfo(groupId, userId);
        }
        return qxGroupUserInfo;
    }

    public void refreshGroupUserInfo(QXGroupUserInfo info) {
        if(info == null) {
            return;
        }
        if (TextUtils.isEmpty(info.getUserId())) {
            return;
        }

        if (this.mIsCacheGroupUserInfo) {
            for (String key : groupUserInfoKey) {
                if (key.endsWith(info.getUserId())) {
                    QXGroupUserInfo groupUserInfo  = groupUserInfoLruCache.get(key);
                    groupUserInfo.setNoteName(info.getNoteName());
                    groupUserInfo.setAvatarUri(info.getAvatarUri());
                    groupUserInfo.setAvatarExtraUrl(info.getAvatarExtraUrl());
                    groupUserInfo.setNameExtraUrl(info.getNameExtraUrl());
                    groupUserInfoLruCache.remove(key);
                    groupUserInfoLruCache.put(key, groupUserInfo);
                    mCacheListener.onGroupUserInfoUpdated(groupUserInfo);
                }
            }
        }
    }

    public QXGroupUserInfo getGroupUserInfo(String userId) {
        if (TextUtils.isEmpty(userId))
            return null;

        if (this.mIsCacheGroupUserInfo) {
            for (String key : groupUserInfoKey) {
                if (key.endsWith(userId)) {
                    return this.groupUserInfoLruCache.get(key);
                }
            }
        }
        return null;
    }


}
