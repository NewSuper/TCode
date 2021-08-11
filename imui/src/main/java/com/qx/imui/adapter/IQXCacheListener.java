package com.qx.imui.adapter;


import com.qx.im.model.QXGroupInfo;
import com.qx.im.model.QXGroupUserInfo;
import com.qx.im.model.QXUserInfo;

public interface IQXCacheListener {

    void onUserInfoUpdated(QXUserInfo qxUserInfo);

    void onGroupUserInfoUpdated(QXGroupUserInfo qxGroupUserInfo);

    void onGroupUpdated(QXGroupInfo qxGroupInfo);

    QXUserInfo getUserInfo(String userId);

    QXGroupUserInfo getGroupUserInfo(String groupId, String userId);

    QXGroupInfo getGroupInfo(String groupId);
}
