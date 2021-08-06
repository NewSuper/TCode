package com.qx.imui.plugin;

import com.qx.imlib.qlog.QLog;
import com.qx.message.ICustomEventProvider;
import com.qx.message.Message;

public class NewFriendEventProvider extends ICustomEventProvider.Stub {

    /**
     * 设置自定义push的tag
     *
     * @return
     */

    @Override
    public String getCustomEventTag() {
        return "QX:NewFriend";
    }

    /**
     * 接收自定义push消息
     *
     * @param message
     */
    @Override
    public void onReceiveCustomEvent(Message message) {
        QLog.i("onReceiveCustomEvent", "" + message.getMessageType() + " " + message.getConversationType());

    }
}
