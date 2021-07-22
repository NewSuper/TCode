package com.qx.push;

import android.content.Context;
import android.os.Build;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.Cipher;

public class Http {


    private final static  String UTF_8 = "UTF-8";
    //    private final static  String SERVER_PUB_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjF8tchNMBBdiFlZqfcC8tBOb3tIEV+80SSdu+bKkv2fWlk6GFza3UfhZ0TZJUeNbiy3H3KhKs7WoPdv5sB8n4DNx9OPdjJdMRu2JMtItgIntf06BlIQkEVBQoGXuGUyMhjiuSp6WybxYxRALleWhBUj9VQL82ZqpmvTHRyGnRxbwbL2weaTSy2eY7KnBk0opYtf8Y4APfYFDlOP1qArcERHENja68MnaJJqd3OzSEe/aqe7ykwBDGVHpahwO1pZkh37hJRLf1xeCnGnlYprljxT0T9wNAB6qAewZjGCYpLyuoh8BG4LTaV2LoHY9bbBqjok40uuCoGHobC9uMuxY2QIDAQAB";
    public static final String PUSH_TOKEN_API = "qx-api/app/im/application/inputPushToken";
    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT_BLOCK = 245;

    public interface HttpResponseListener {
        void onProcess();

        void onSuccess(Object obj);

        void onFailed(int code, String message);
    }

    public static String encode(Context context, String content) {
        try {
            String rsakey = PushCacheHelper.getInstance().getCacheRsaKey(context);
            byte[] bytes = RSAEncrypt(content.getBytes(), rsakey);

            byte[] keyBytes;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyBytes = Base64.getEncoder().encode(bytes);
            } else {
                keyBytes = android.util.Base64.encode(bytes, android.util.Base64.NO_WRAP);
            }
            content = new String(keyBytes);
            content = content.replace("\n", "");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return content;
        }
    }

    /**
     * RSA公钥加密
     * @param content 加密内容
     * @param pub 公钥
     * @return 加密byte数组
     * @throws Exception
     * @author	hechuan
     */
    public static byte[] RSAEncrypt(byte[] content, String pub) throws Exception {
        byte[] keyBytes ;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyBytes = Base64.getDecoder().decode(pub.getBytes(UTF_8));
        } else  {
            keyBytes = android.util.Base64.decode(pub.getBytes(UTF_8),android.util.Base64.DEFAULT);
        }
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        int inputLen = content.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(content, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(content, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }

    public static void postDataByJson(String url, HashMap<String, String> param, HttpResponseListener listener) {
        HttpURLConnection connection = null;
        try {
            JSONObject body = new JSONObject();
            for (String key : param.keySet()) {
                body.put(key, param.get(key));

            }
            URL u = new URL(url);
            connection = (HttpURLConnection) u.openConnection();
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setDoOutput(true);
            connection.setDoInput(true);

            connection.setRequestMethod("POST");
            DataOutputStream os = new DataOutputStream(connection.getOutputStream());
            String content = String.valueOf(body);

            os.writeBytes(content);
            os.flush();
            os.close();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStreamReader in = new InputStreamReader(connection.getInputStream());
                BufferedReader bf = new BufferedReader(in);
                String str;
                StringBuilder response = new StringBuilder();

                while ((str = bf.readLine()) != null) {
                    response.append(str);
                }
                in.close();
                connection.disconnect();
                String jsonStr = response.toString();
                listener.onSuccess(jsonStr);
            } else {
                listener.onFailed(connection.getResponseCode(), "访问失败：" + url);
            }

        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailed(-1, "访问异常：" + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
