package com.qx.imui.network;


import android.net.ParseException;

import com.google.gson.JsonParseException;

import org.json.JSONException;

import java.net.ConnectException;
import java.net.UnknownHostException;

import retrofit2.HttpException;

// 网络异常处理类
public class ExceptionHandle {
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;
    private static final int REQUEST_TIMEOUT = 408;
    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final int BAD_GATEWAY = 502;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final int GATEWAY_TIMEOUT = 504;

    public static ResponeThrowable handleException(Throwable e) {
        ResponeThrowable ex;
        if (e instanceof HttpException) {
            HttpException httpException = (HttpException) e;
            ex = new ResponeThrowable(e, ERROR.HTTP_ERROR);
            switch (httpException.code()) {
                case UNAUTHORIZED:
                case FORBIDDEN:
                case NOT_FOUND:
                case REQUEST_TIMEOUT:
                case GATEWAY_TIMEOUT:
                case INTERNAL_SERVER_ERROR:
                case BAD_GATEWAY:
                case SERVICE_UNAVAILABLE:
                default:
                    ex.code = String.valueOf(httpException.code());
                    ex.errorMessage = "网络错误";
                    break;
            }
            return ex;
        } else if (e instanceof ServerException) {
            // 服务器下发的错误
            ServerException resultException = (ServerException) e;
            ex = new ResponeThrowable(resultException, resultException.code);
            ex.errorMessage = resultException.errorMessage;
            return ex;
        } else if (e instanceof JsonParseException
                || e instanceof JSONException
                || e instanceof ParseException) {
            ex = new ResponeThrowable(e, ERROR.PARSE_ERROR);
            ex.errorMessage = "解析错误";
            return ex;
        } else if (e instanceof ConnectException) {
            ex = new ResponeThrowable(e, ERROR.NETWORD_ERROR);
            ex.errorMessage = "连接失败";
            return ex;
        } else if (e instanceof javax.net.ssl.SSLHandshakeException) {
            ex = new ResponeThrowable(e, ERROR.SSL_ERROR);
            ex.errorMessage = "证书验证失败";
            return ex;
        } else if (e instanceof UnknownHostException) {
            ex = new ResponeThrowable(e, ERROR.UNKNOWN);
            // ex.errorMessage = "未知错误" + e.toString();
            ex.errorMessage = "网络连接错误，可能抛锚了吧，请稍后再试~";
            return ex;
        } else {
            ex = new ResponeThrowable(e, ERROR.UNKNOWN);
            //  ex.errorMessage = "未知错误" + e.toString();
            ex.errorMessage = "未知错误，请稍后再试~";
            return ex;
        }
    }


    /**
     * 约定异常
     */
    public class ERROR {
        /**
         * 未知错误
         */
        public static final String UNKNOWN = "1000";
        /**
         * 解析错误
         */
        public static final String PARSE_ERROR = "1001";
        /**
         * 网络错误
         */
        public static final String NETWORD_ERROR = "1002";
        /**
         * 协议出错
         */
        public static final String HTTP_ERROR = "1003";

        /**
         * 证书出错
         */
        public static final String SSL_ERROR = "1005";
    }

    public static class ResponeThrowable extends Exception {
        public String code;
        public String errorMessage;

        public ResponeThrowable(Throwable throwable, String code) {
            super(throwable);
            this.code = code;
        }
    }
}
