package com.qx.imui.adapter;


import com.qx.im.model.QXGroupInfo;
import com.qx.im.model.QXGroupUserInfo;
import com.qx.im.model.QXUserInfo;
import com.qx.imlib.utils.event.EventBusUtil;
import com.qx.imui.QXContext;

public class QXUserCacheListener implements IQXCacheListener {

    public QXUserCacheListener() {

    }

    @Override
    public void onUserInfoUpdated(QXUserInfo qxUserInfo) {
        EventBusUtil.post(qxUserInfo);
    }

    @Override
    public void onGroupUserInfoUpdated(QXGroupUserInfo qxGroupUserInfo) {
        EventBusUtil.post(qxGroupUserInfo);
    }

    @Override
    public void onGroupUpdated(QXGroupInfo qxGroupInfo) {
        EventBusUtil.post(qxGroupInfo);
    }

    @Override
    public QXUserInfo getUserInfo(String userId) {
        return QXContext.getInstance().getUserInfoProvider() != null ? QXContext.getInstance().getUserInfoProvider().getUserInfo(userId) : null;
    }

    @Override
    public QXGroupUserInfo getGroupUserInfo(String groupId, String userId) {
        return QXContext.getInstance().getGroupUserInfoProvider() != null ? QXContext.getInstance().getGroupUserInfoProvider().getGroupUserInfo(groupId,userId) : null;
    }

    @Override
    public QXGroupInfo getGroupInfo(String groupId) {
        return QXContext.getInstance().getGroupProvider() != null ? QXContext.getInstance().getGroupProvider().getGroupInfo(groupId) : null;
    }
}
