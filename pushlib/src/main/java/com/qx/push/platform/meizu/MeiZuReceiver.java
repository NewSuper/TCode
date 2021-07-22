package com.qx.push.platform.meizu;

import android.content.Context;

import com.meizu.cloud.pushsdk.MzPushMessageReceiver;
import com.meizu.cloud.pushsdk.handler.MzPushMessage;
import com.meizu.cloud.pushsdk.platform.message.PushSwitchStatus;
import com.meizu.cloud.pushsdk.platform.message.RegisterStatus;
import com.meizu.cloud.pushsdk.platform.message.SubAliasStatus;
import com.meizu.cloud.pushsdk.platform.message.SubTagsStatus;
import com.meizu.cloud.pushsdk.platform.message.UnRegisterStatus;
import com.qx.push.PushManager;
import com.qx.push.PushNotificationMessage;
import com.qx.push.PushType;
import com.qx.push.PushUtils;

public class MeiZuReceiver extends MzPushMessageReceiver {

    private final String TAG = "MeiZuReceiver";

    @Override
    public void onRegisterStatus(Context context, RegisterStatus registerStatus) {
        PushManager.getInstance().onReceiveToken(context, PushType.MEIZU, registerStatus.getPushId());
    }

    @Override
    public void onUnRegisterStatus(Context context, UnRegisterStatus unRegisterStatus) {

    }

    @Override
    public void onPushStatus(Context context, PushSwitchStatus pushSwitchStatus) {

    }

    @Override
    public void onSubTagsStatus(Context context, SubTagsStatus subTagsStatus) {

    }

    @Override
    public void onSubAliasStatus(Context context, SubAliasStatus subAliasStatus) {

    }

    @Override
    public void onNotificationArrived(Context context, MzPushMessage mzPushMessage) {
        super.onNotificationArrived(context, mzPushMessage);
        PushNotificationMessage pushNotificationMessage = PushUtils.transformToPushMessage(mzPushMessage.getSelfDefineContentString());
        if (pushNotificationMessage != null) {
            PushManager.getInstance().onNotificationMessageArrived(context, PushType.MEIZU, pushNotificationMessage);
        }

    }

    @Override
    public void onNotificationClicked(Context context, MzPushMessage mzPushMessage) {
        super.onNotificationClicked(context, mzPushMessage);
        PushNotificationMessage pushNotificationMessage = PushUtils.transformToPushMessage(mzPushMessage.getSelfDefineContentString());
        if (pushNotificationMessage != null) {
            PushManager.getInstance().onNotificationMessageClicked(context, PushType.MEIZU, pushNotificationMessage);
        }
    }
}
