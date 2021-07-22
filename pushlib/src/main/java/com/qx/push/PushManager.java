package com.qx.push;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.qx.push.platform.IPush;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class PushManager {
    private static class SingletonHolder {
        private static PushManager sIns = new PushManager();

        private SingletonHolder() {
        }
    }
    public static PushManager getInstance() {
        return SingletonHolder.sIns;
    }
    private final static String TAG = PushManager.class.getSimpleName();
    private PushConfig pushConfig;
    private ConcurrentHashMap<String, IPush>registeredPushMap;
    private PushType serverPushType;
    private final int MAX_RETRY_CONUT;
    private int retryTimes;

    private PushManager(){
     this.MAX_RETRY_CONUT = 5;
     this.retryTimes= 0;
    }
    public void init(Context context,PushConfig pushConfig){
        this.pushConfig = pushConfig;
        this.registeredPushMap = new ConcurrentHashMap<>();
        this.retryTimes = 0;
   PushType pushType = PushUtils.getPreferPushType(context,pushConfig);
   if (isCacheIMToken(context)&& isCachePushToken(context)){
       register(context,pushType);
   }
    }
    public void register(Context context,PushType pushType){
        IPush iPush = this.registeredPushMap.get(pushType.getName());
        if (iPush == null){
            iPush  = PushFactory.getPushCenterByType(pushType);
        }
        serverPushType = pushType;
        if (iPush!=null){
            iPush.register(context,this.pushConfig);
            this.registeredPushMap .put(pushType.getName(),iPush);
        }
    }

    public void clearCache(Context context) {
        PushCacheHelper.getInstance().clearCache(context);
    }

    private boolean isCachePushToken(Context context) {
        String pushToken = PushCacheHelper.getInstance().getPushToken(context);
        String pushType = PushCacheHelper.getInstance().getPushType(context);
        Log.d(TAG, "isCachePushToken pushToken:" + pushToken+",pushType:"+pushType);
        if (!TextUtils.isEmpty(pushToken) && !TextUtils.isEmpty(pushType)) {
            return true;
        }
        return false;
    }

    private boolean isCacheIMToken(Context context) {
        String imToken = PushCacheHelper.getInstance().getCacheIMToken(context);
        Log.d(TAG, "isCacheIMToken imToken:" + imToken);
        if (!TextUtils.isEmpty(imToken) ) {
            return true;
        }
        return false;
    }

    public void onReceiveToken(Context context, PushType pushType, String token) {
        Log.d(TAG, "onReceiveToken pushType:" + pushType.getName() + ",token:" + token);
        PushCacheHelper.getInstance().cachePushType(context, pushType.getName());
        PushCacheHelper.getInstance().cachePushToken(context, token);
        if (isCacheIMToken(context)) {
            updatePushToken(context);
        }
    }
    public void onNotificationMessageArrived(Context context, PushType pushType, PushNotificationMessage pushNotificationMessage) {
        Log.d(TAG, "onNotificationMessageArrived is called. " + pushNotificationMessage.toString());
        Intent intent = new Intent();
        intent.setAction("qxim.push.intent.MESSAGE_ARRIVED");
        intent.putExtra("pushType", pushType.getName());
        intent.putExtra("message", pushNotificationMessage);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    public void onNotificationMessageClicked(Context context, PushType pushType, PushNotificationMessage pushNotificationMessage) {
        Log.d(TAG, "onNotificationMessageClicked is called. " + pushNotificationMessage.toString());
        Intent intent = new Intent();
        intent.setAction("qxim.push.intent.MESSAGE_CLICKED");
        intent.putExtra("pushType", pushType.getName());
        intent.putExtra("message", pushNotificationMessage);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    public void onPushRawData(Context context, PushType pushType, String data) {
        PushNotificationMessage notificationMessage = PushUtils.transformToPushMessage(data);
        if (notificationMessage == null) {
            Log.e(TAG, "notification message is null. Ignore this event.");
        } else {
            Intent intent = new Intent();
            intent.setAction("qxim.push.intent.MESSAGE_ARRIVED");
            intent.putExtra("pushType", pushType.getName());
            intent.putExtra("message", notificationMessage);
            intent.setPackage(context.getPackageName());
            context.sendBroadcast(intent);
        }
    }

    public void onErrorResponse(Context context, PushType pushType, String action, long resultCode) {
        Log.e(TAG, "onErrorResponse pushType:" + pushType.getName()+",resultCode:"+resultCode);
//        if (resultCode == (long)PushErrorCode.NOT_SUPPORT_BY_OFFICIAL_PUSH.getCode()) {
//            this.register(context, PushType.RONG);
//            if (this.pushConfigManager != null) {
//                this.pushConfigManager.finishConfig(context, PushType.RONG.getName());
//            }
//        } else {
        Intent intent = new Intent();
        intent.setAction("qxim.push.intent.THIRD_PARTY_PUSH_STATE");
        intent.putExtra("pushType", pushType.getName());
        intent.putExtra("action", action);
        intent.putExtra("resultCode", resultCode);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
//        }
    }

    public void updateIMToken(Context context, String token, String host, String rsaKey) {
        if (TextUtils.isEmpty(token))
            return;
        Log.d(TAG, "updateIMToken token:" +token + ",host:" + host+",rsaKey:"+rsaKey);
        PushCacheHelper.getInstance().cacheIMToken(context, token);
        PushCacheHelper.getInstance().cacheAddress(context, host);
        PushCacheHelper.getInstance().cacheRsaKey(context, rsaKey);
        if (isCachePushToken(context)) {
            updatePushToken(context);
        } else {
            register(context, PushUtils.getPreferPushType(context, pushConfig));
        }
    }

    private void updatePushToken(Context context) {
        String host = PushCacheHelper.getInstance().getCacheAddress(context);
        String imtoken = PushCacheHelper.getInstance().getCacheIMToken(context);
        String pushTypeName = PushCacheHelper.getInstance().getPushType(context);
        PushType pushType = PushType.getType(pushTypeName);
        String pushToken = PushCacheHelper.getInstance().getPushToken(context);
        HashMap<String, String> param = new HashMap<>();
        String inputPushToken = pushConfig.getAppKey() + "," + imtoken + "," + pushType.getType() + "," + pushToken;
        Log.d(TAG, "updatePushToken inputPushToken:" +inputPushToken);
        inputPushToken = Http.encode(context,inputPushToken);
        param.put("inputPushToken", inputPushToken);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Http.postDataByJson(host + Http.PUSH_TOKEN_API, param, new Http.HttpResponseListener() {
                    @Override
                    public void onProcess() {

                    }

                    @Override
                    public void onSuccess(Object obj) {
                        String result = (String) obj;
                        BeanResponse response = new Gson().fromJson(result, BeanResponse.class);
                        Log.d(TAG,"updatePushToken:"+response.toString());
                    }

                    @Override
                    public void onFailed(int code, String message) {
                        Log.d(TAG,"updatePushToken code:"+code+",message:"+message);
                    }
                });
            }
        }).start();

    }

    public PushType getServerPushType() {
        if (serverPushType == null) {
            serverPushType = PushType.UNKNOWN;
        }
        return this.serverPushType;
    }

    public PushConfig getPushConfig() {
        return this.pushConfig;
    }

    public IPush getRegisteredPush(String key) {
        return this.registeredPushMap == null ? null : (IPush)this.registeredPushMap.get(key);
    }


}
