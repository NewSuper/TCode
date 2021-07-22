package com.qx.push;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class QXNotificationInterface {

    private static final String TAG = "QXNotificationInterface";
    private static HashMap<String, List<PushNotificationMessage>> messageCache = new HashMap();
    private static HashMap<String, Integer> messageNoticationId = new HashMap();
    private static int NOTIFICATION_ID = 1000;
    private static int REQUESTCODE = 200;
    private static int PUSH_SERVICE_NOTIFICATION_ID = 2000;
    private static int VOIP_NOTIFICATION_ID = 3000;
    private static final int NEW_NOTIFICATION_LEVEL = 11;
    private static final int PUSH_REQUEST_CODE = 200;
    private static final int NEGLECT_TIME = 3000;
    private static long lastNotificationTimestamp;
    private static Uri mSound;
    private static boolean recallUpdate = false;

    public QXNotificationInterface() {
    }

    public static void sendNotification(Context context, PushNotificationMessage message) {
        sendNotification(context, message, 0);
    }

    public static void sendNotification(Context context, PushNotificationMessage message, int left) {
        if (messageCache == null) {
            messageCache = new HashMap();
        }

        QXPushClient.ConversationType conversationType = message.getConversationType();
        String content = message.getPushContent();
        boolean isMulti = false;

        QXNotificationInterface.SoundType soundType = QXNotificationInterface.SoundType.DEFAULT;
        Log.i(TAG, "sendNotification() messageType: " + message.getConversationType() + " content: " + content);
        if (conversationType != null) {
            long now = System.currentTimeMillis();
            if (now - lastNotificationTimestamp < 3000L) {
                soundType = QXNotificationInterface.SoundType.SILENT;
            } else {
                lastNotificationTimestamp = now;
            }

            String title = "未知";
            int notificationId;
            if (!conversationType.equals(QXPushClient.ConversationType.SYSTEM) ) {
                // 语音消息
                // if (objName != null && (objName.equals("RC:VCInvite") || objName.equals("RC:VCModifyMem") || objName.equals("RC:VCHangup"))) {
//                    if (objName.equals("RC:VCHangup")) {
//                        removeNotification(context, VOIP_NOTIFICATION_ID);
//                        return;
//                    }
//
//                    notificationId = VOIP_NOTIFICATION_ID;
//                    soundType = QXNotificationInterface.SoundType.VOIP;
//                    requestCode = 400;
//                    title = (String)context.getPackageManager().getApplicationLabel(context.getApplicationInfo());
//                    content = message.getPushContent();
//                } else {
                String notificationTargetId = "";
                if (message.getConversationType().equals(QXPushClient.ConversationType.GROUP)) {
                    notificationTargetId = message.getTargetId();
                } else  {
                    notificationTargetId = message.getSenderId();
                }
                Log.e(TAG,"messageCache targetId "+notificationTargetId);
                List<PushNotificationMessage> messages = (List)messageCache.get(notificationTargetId);
                if (messages == null) {
                    List<PushNotificationMessage> messageList = new ArrayList();
                    messageList.add(message);
                    messageCache.put(notificationTargetId, messageList);
                    messageNoticationId.put(notificationTargetId,++NOTIFICATION_ID);
                    Log.e(TAG,"new messageCache size "+messageCache.size());
                } else {
                    messages.add(message);
//                        recallUpdate = true;
                    Log.e(TAG,"old messageCache size "+messageCache.size()+",messages size:"+messages.size());
//                        if (messages.size() == 0) {
//                            if (messageCache.size() == 1) {
//                                messages.add(message);
//                            } else {
//                                messageCache.remove(message.getTargetId());
//                                if (messageCache.size() == 1) {
//                                    recallUpdate = true;
//                                }
//                            }
//                        }
                }

                if (messageCache.size() > 1) {
                    isMulti = true;
                }

                title = getNotificationTitle(context,message);
                notificationId = messageNoticationId.get(notificationTargetId);
                Log.e(TAG,"!conversationType.equals(QXPushClient.ConversationType.SYSTEM)  notificationId: "+notificationId+",title:"+title+",content :"+content);
//                }
            } else {
                title = message.getPushTitle();
                if (TextUtils.isEmpty(title)) {
                    title = (String)context.getPackageManager().getApplicationLabel(context.getApplicationInfo());
                }
                content = message.getPushContent();
                notificationId = PUSH_SERVICE_NOTIFICATION_ID;
                REQUESTCODE = 300;
                ++PUSH_SERVICE_NOTIFICATION_ID;
                Log.e(TAG,"getPushTitle  "+message.getPushTitle()+",getPushContent :"+message.getPushContent());
            }

            if (left <= 0) {
                PendingIntent intent;
                ++REQUESTCODE;
                if (recallUpdate) {
                    intent = updateRecallPendingIntent(context, REQUESTCODE, isMulti);
                } else {
                    intent = createPendingIntent(context, message, REQUESTCODE, isMulti);
                }
                Log.e(TAG,"title  "+title+",content : "+content);
                if(TextUtils.isEmpty(content) ) {
                    content = "有一条新消息";
                }
                Notification notification = createNotification(context, message,title, intent, content, soundType);
                NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= 26) {
                    String channelName = context.getResources().getString(context.getResources().getIdentifier("qx_notification_channel_name", "string", context.getPackageName()));
                    NotificationChannel notificationChannel = new NotificationChannel("qx_notification_id", channelName, NotificationManager.IMPORTANCE_DEFAULT);
                    notificationChannel.enableLights(true);
                    notificationChannel.setLightColor(-16711936);
                    if (notification != null && notification.sound != null) {
                        notificationChannel.setSound(notification.sound, (AudioAttributes)null);
                    }

                    nm.createNotificationChannel(notificationChannel);
                }

                if (notification != null) {
                    Log.i(TAG, "sendNotification() real notify! notificationId: " + notificationId + " notification: " + notification.toString());
                    nm.notify(notificationId, notification);
                }

            }
        }
    }

    private static PendingIntent updateRecallPendingIntent(Context context, int requestCode, boolean isMulti) {
        Collection<List<PushNotificationMessage>> collection = messageCache.values();
        List<PushNotificationMessage> msg = (List)collection.iterator().next();
        PushNotificationMessage notificationMessage = (PushNotificationMessage)msg.get(0);
        Intent intent = new Intent();
        intent.setAction("qxim.push.intent.MESSAGE_CLICKED");
        intent.putExtra("message", notificationMessage);
        intent.putExtra("isMulti", isMulti);
        intent.setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    public static void removeAllNotification(Context context) {
        messageCache.clear();
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            nm.cancelAll();
        } catch (Exception var3) {
            Log.e(TAG, "removeAllNotification" + var3.getMessage());
        }

        NOTIFICATION_ID = 1000;
    }

    public static void removeAllPushNotification(Context context) {
        messageCache.clear();
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
        nm.cancel(VOIP_NOTIFICATION_ID);
    }

    public static void removeAllPushServiceNotification(Context context) {
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        for(int i = PUSH_SERVICE_NOTIFICATION_ID; i >= 1000; --i) {
            nm.cancel(i);
        }

        PUSH_SERVICE_NOTIFICATION_ID = 2000;
    }

    public static void removeNotification(Context context, int notificationId) {
        if (notificationId >= 0) {
            if (notificationId >= NOTIFICATION_ID && notificationId < PUSH_SERVICE_NOTIFICATION_ID) {
                messageCache.clear();
            }

            NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(notificationId);
        }
    }

    private static PendingIntent createPendingIntent(Context context, PushNotificationMessage message, int requestCode, boolean isMulti) {
        Log.e(TAG,"getPushTitle  "+message.getPushTitle()+",getPushContent :"+message.getPushContent() + ",conversationType:"+message.getConversationType());
        Intent intent = new Intent();
        intent.setAction("qxim.push.intent.MESSAGE_CLICKED");
        intent.putExtra("message", message);
        intent.putExtra("isMulti", isMulti);
        intent.setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    private static String getNotificationContent(Context context,PushNotificationMessage message) {
        String rc_notification_new_msg = context.getResources().getString(context.getResources().getIdentifier("qx_notification_new_msg", "string", context.getPackageName()));
        String rc_notification_new_plural_msg = context.getResources().getString(context.getResources().getIdentifier("qx_notification_new_plural_msg", "string", context.getPackageName()));
        String content = "";
        if (messageCache.size() == 1) {
            Collection<List<PushNotificationMessage>> collection = messageCache.values();
            List<PushNotificationMessage> msg = (List)collection.iterator().next();
            PushNotificationMessage notificationMessage = (PushNotificationMessage)msg.get(msg.size() - 1);
            if (msg.size() == 1) {
                content = notificationMessage.getPushContent();
            }else {
                content = String.format(rc_notification_new_msg,msg.size(),notificationMessage.getPushContent());
            }
        } else {
            Iterator<Map.Entry<String,List<PushNotificationMessage>>> iterator =   messageCache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String,List<PushNotificationMessage>> entry = iterator.next();
                if (message.getConversationType().equals(QXPushClient.ConversationType.GROUP)) {
                    if (entry.getKey().equals(message.getTargetId())) {
                        int size = entry.getValue().size();
                        content = String.format(rc_notification_new_msg, size, entry.getValue().get(size-1).getPushContent());
                    }
                } else {
                    if (entry.getKey().equals(message.getSenderId())) {
                        int size = entry.getValue().size();
                        content = String.format(rc_notification_new_msg,entry.getValue().size(),entry.getValue().get(size-1).getPushContent());
                    }
                }
            }

//            content = String.format(rc_notification_new_plural_msg, messageCache.size(), count);
        }

        return content;
    }

    private static String getNotificationTitle(Context context,PushNotificationMessage message) {
        String title = "";
        if (messageCache.size() == 1) {
            Collection<List<PushNotificationMessage>> collection = messageCache.values();
            List<PushNotificationMessage> msg = (List)collection.iterator().next();
            PushNotificationMessage notificationMessage = (PushNotificationMessage)msg.get(0);
            title = notificationMessage.getTargetUserName();
        } else {
            Collection<List<PushNotificationMessage>> collection = messageCache.values();
            Iterator<List<PushNotificationMessage>> iterator =  collection.iterator();
            while (iterator.hasNext()) {
                List<PushNotificationMessage> messageList = iterator.next();
                for(PushNotificationMessage pushMsg : messageList) {
                    if (!TextUtils.isEmpty(pushMsg.getPushId()) && !TextUtils.isEmpty(message.getPushId()) && pushMsg.getPushId().equals(message.getPushId())) {
                        title =  pushMsg.getTargetUserName();
                        break;
                    }
                }
            }
        }

        return title;
    }

    public static Notification createNotification(Context context,PushNotificationMessage notificationMessage, String title, PendingIntent pendingIntent, String content, QXNotificationInterface.SoundType soundType) {
        String tickerText = context.getResources().getString(context.getResources().getIdentifier("qx_notification_ticker_text", "string", context.getPackageName()));
        if (messageCache.size() >= 1) {
            content = getNotificationContent(context,notificationMessage);
        }

        Notification notification;
        if (Build.VERSION.SDK_INT < 11) {
            try {
                notification = new Notification(context.getApplicationInfo().icon, tickerText, System.currentTimeMillis());
                Class<?> classType = Notification.class;
                Method method = classType.getMethod("setLatestEventInfo", Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
                method.invoke(notification, context, title, content, pendingIntent);
                notification.flags = 16;
                notification.defaults = -1;
            } catch (Exception var18) {
                var18.printStackTrace();
                return null;
            }
        } else {
            boolean isLollipop = Build.VERSION.SDK_INT >= 21;
            int smallIcon = context.getResources().getIdentifier("notification_small_icon", "drawable", context.getPackageName());
            if (smallIcon <= 0 || !isLollipop) {
                smallIcon = context.getApplicationInfo().icon;
            }

            int defaults = 1;
            Uri sound = null;
            if (soundType.equals(QXNotificationInterface.SoundType.SILENT)) {
                defaults = 4;
            } else if (soundType.equals(QXNotificationInterface.SoundType.VOIP)) {
                defaults = 6;
                sound = RingtoneManager.getDefaultUri(1);
            } else {
                sound = RingtoneManager.getDefaultUri(2);
            }

            Drawable loadIcon = context.getApplicationInfo().loadIcon(context.getPackageManager());
            Bitmap appIcon = null;

            try {
                if (Build.VERSION.SDK_INT >= 26 && loadIcon instanceof AdaptiveIconDrawable) {
                    appIcon = Bitmap.createBitmap(loadIcon.getIntrinsicWidth(), loadIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(appIcon);
                    loadIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    loadIcon.draw(canvas);
                } else {
                    appIcon = ((BitmapDrawable)loadIcon).getBitmap();
                }
            } catch (Exception var19) {
                var19.printStackTrace();
            }

            Notification.Builder builder = new Notification.Builder(context);
            builder.setLargeIcon(appIcon);
            if (!soundType.equals(QXNotificationInterface.SoundType.SILENT)) {
                builder.setVibrate(new long[]{0L, 200L, 250L, 200L});
            }

            builder.setSmallIcon(smallIcon);
            builder.setTicker(tickerText);
            builder.setContentTitle(title);
            builder.setContentText(content);
            Log.e(TAG,"createNotification title:"+title+",content:"+content);
//            if (PushCacheHelper.getInstance().getPushContentShowStatus(context)) {
//                builder.setContentTitle(title);
//                builder.setContentText(content);
//            } else {
//                PackageManager pm = context.getPackageManager();
//                String name;
//                try {
//                    name = pm.getApplicationLabel(pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)).toString();
//                } catch (PackageManager.NameNotFoundException var17) {
//                    name = "";
//                }
//
//                builder.setContentTitle(name);
//                builder.setContentText(tickerText);
//            }

            builder.setContentIntent(pendingIntent);
            builder.setLights(-16711936, 3000, 3000);
            builder.setAutoCancel(true);
            if (Build.VERSION.SDK_INT >= 26) {
                builder.setChannelId("qx_notification_id");
            }

            if (mSound != null && !TextUtils.isEmpty(mSound.toString())) {
                builder.setSound(mSound);
            } else {
                builder.setSound(sound);
                builder.setDefaults(defaults);
            }

            notification = builder.build();
            notification.flags = 1;
        }

        return notification;
    }

    public static void setNotificationSound(Uri uri) {
        mSound = uri;
    }

    public static enum SoundType {
        DEFAULT(0),
        SILENT(1),
        VOIP(2);

        int value;

        private SoundType(int v) {
            this.value = v;
        }
    }
}
