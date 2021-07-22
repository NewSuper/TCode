package com.qx.push;

import com.qx.push.platform.IPush;
import com.qx.push.platform.google.FCMPush;
import com.qx.push.platform.hms.HWPush;
import com.qx.push.platform.meizu.MZPush;
import com.qx.push.platform.oppo.OPPOPush;
import com.qx.push.platform.vivo.VIVOPush;
import com.qx.push.platform.xiaomi.XMPush;

public class PushFactory {
    public PushFactory() {
    }

    public static boolean isOnlyDefaultPushOS(PushConfig pushConfig) {
        String os = PushUtils.getDeviceManufacturer();
        return (os.contains("Xiaomi") || os.contains("HUAWEI") || os.contains("Meizu"))  && !pushConfig.getEnabledPushTypes().contains(PushType.GOOGLE_FCM);
    }

    public static IPush getPushCenterByType(PushType pushType) {
        if (pushType.equals(PushType.GOOGLE_FCM)) {
            return new FCMPush();
        } else if (pushType.equals(PushType.HUAWEI)) {
            return new HWPush();
        } else if (pushType.equals(PushType.XIAOMI)) {
            return new XMPush();
        } else if (pushType.equals(PushType.MEIZU)) {
            return new MZPush();
        }  else if (pushType.equals(PushType.VIVO)) {
            return new VIVOPush();
        } else if (pushType.equals(PushType.OPPO)) {
            return new OPPOPush();
        } else {
            return null;
        }
    }
}
