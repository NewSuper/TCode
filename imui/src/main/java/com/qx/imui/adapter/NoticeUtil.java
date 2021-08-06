package com.qx.imui.adapter;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qx.im.model.QXGroupUserInfo;
import com.qx.imui.QXIMKit;
import com.qx.imui.R;
import com.qx.imui.util.SPUtils;
import com.qx.imui.util.StringUtils;
import com.qx.message.NoticeMessage;

import java.util.ArrayList;
import java.util.List;

public class NoticeUtil {

    private static class User {
        String userId;
        String name;
    }

    private static class NoticeContent {
        int type;
        String content;

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    private static final String TAG = "NoticeUtil";
    private static String cacheLanguage;
    private static ArrayList<NoticeContent> noticeContents = new ArrayList<>();

    public static String getNoticeContent(String targetId, NoticeMessage notice, Context context) {
        String language = SPUtils.getCacheLanguage(context);
        if (noticeContents == null ||
                noticeContents.size() == 0 ||
                (!StringUtils.isEmpty(language) && !language.equals(language))) {
            loadNoticeContents(context);
        }
        cacheLanguage = language;
        return getContent(targetId, notice, context);
    }

    private static String getContent(String targetId, NoticeMessage notice, Context context) {
        NoticeContent noticeContent = getNoticeContentByType(notice.getType());
        if (noticeContent == null) return "";
        switch (notice.getType()) {
            case 1:
            case 2:
            case 6:
            case 8:
            case 9:
                return String.format(noticeContent.content, getUsersStr(notice.getUsers()));
            case 7:
                User operateUser = new Gson().fromJson(notice.getOperateUser(), User.class);
                //被操作的人员数组
                List<User> userList = new Gson().fromJson(notice.getUsers(),
                        new TypeToken<List<User>>() {
                        }.getType());
                if (operateUser.userId.equals(QXIMKit.getInstance().getCurUserId())) {
                    //主动
                    if (isOperateItself(userList, operateUser.userId)) {
                        //如果是自己操作自己，则是自己已经退群，显示你已退出群里
                        return "";
                    } else {
                        //如果被操作人是自己，那么是群主移除群成员，显示：你已将xxx移出群聊
                        return String.format(context.getString(R.string.qx_group_remove_member_positive),
                                getUsersStr(notice.getUsers()));
                    }
                } else {
                    //被动
                    if (isOperateItself(userList, operateUser.userId)) {
                        //如果被操作的人是自己，那么是群成员自己被踢出群，显示：你已被xxx踢出群聊
                        QXGroupUserInfo groupUserInfo = UserInfoUtil.INSTANCE.getMemberInfo(targetId, operateUser.userId);
                        if (groupUserInfo == null) {
                            return String.format(context.getString(R.string.qx_group_remove_member_negative),
                                    "");
                        }
                        return String.format(context.getString(R.string.qx_group_remove_member_negative),
                                groupUserInfo.getDisplayName());
                    } else {
                        //否则是群成员主动退群，显示：xxx已退出了群聊
                        if (!isOperateItself(userList, operateUser.userId)) {
                            //如果是自己操作自己，则是自己主动退群：显示你已退出群聊
                            NoticeContent noticeContentTemp = getNoticeContentByType(12);
                            Log.e("TAG", "getContent: ====>> " + (noticeContentTemp.content));
                            return String.format(noticeContentTemp.content, getUsersStr(notice.getUsers()));
                        }
                        return String.format(noticeContent.content, getUsersStr(notice.getUsers()));
                    }
                }
            case 5:
                operateUser = new Gson().fromJson(notice.getOperateUser(), User.class);
                return String.format(noticeContent.content, operateUser.name);
            default:
                return noticeContent.content;
        }
    }

    private static NoticeContent getNoticeContentByType(int type) {
        for (NoticeContent content : noticeContents) {
            if (type == content.getType()) {
                return content;
            }
        }
        return null;
    }

    private static boolean isOperateItself(List<User> users, String operateUserId) {
        for (User user : users) {
            if (user.userId.equals(operateUserId)) {
                return true;
            }
        }
        return false;
    }

    private static String getUsersStr(String users) {
        StringBuffer sb = new StringBuffer();
        List<User> userList = new Gson().fromJson(users, new TypeToken<List<User>>() {
        }.getType());
        for (int i = 0; i < userList.size(); i++) {
            if (userList.size() > 1 && i < userList.size() - 1) {
                sb.append(userList.get(i).name + " ");
            } else {
                sb.append(userList.get(i).name);
            }
        }
        return sb.toString();
    }

    private static void loadNoticeContents(Context context) {
        noticeContents.clear();
        String[] array = context.getResources().getStringArray(R.array.qx_notice_content);
        try {
            for (String item : array) {
                String[] data = item.split(",");
                int type = Integer.parseInt(data[0]);
                String notice = data[1];
                NoticeContent noticeContent = new NoticeContent();
                noticeContent.type = type;
                noticeContent.content = notice;
                noticeContents.add(noticeContent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
