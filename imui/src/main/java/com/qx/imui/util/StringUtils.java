package com.qx.imui.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import com.qx.imlib.utils.ContextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {

    /**
     * 是否为null或空字符串
     */
    public static boolean isEmpty(String str) {
        return TextUtils.isEmpty(str);
    }

    /**
     * 判断是字符串是否没有（空或者"")
     *
     * @param aData
     * @return
     */
    public static boolean stringEmpty(String aData) {
        if (null == aData || "".equals(aData) || "".equals(aData.trim())) {
            return true;
        }
        return false;
    }


    /**
     * 取指定资源字符串对像值
     *
     * @param context
     * @return
     */
    public static String getResourceStr(Context context, int StringID) {
        if (StringID == -1)
            return "";
        try {
            return context.getString(StringID);
        } catch (Resources.NotFoundException mx) {
        }
        return "";
    }


    /**
     * 取指定资源字符串对像值
     *
     * @param StringID
     * @return
     */
    public static String getResourceStr(int StringID) {
        if (StringID == -1)
            return "";
        if (ContextUtils.getInstance().getContext() != null) {
            try {
                return ContextUtils.getInstance().getContext().getResources().getString(StringID);
            } catch (Resources.NotFoundException mx) {
            }
        }
        return "";
    }

    /**
     * 取指定资源字符串对像值(例如 Hi,%1$s你好吗?)
     *
     * @param StringID
     * @param obj      数据填充列表
     * @return
     */
    public static String getResourceStr(Context context,int StringID, Object... obj) {
        return String.format(getResourceStr(context,StringID), obj);
    }

    /**
     * 返回需要的资源颜色
     *
     * @param colorID
     * @return
     */
    public static int getResourceColor(int colorID) {
        return ContextUtils.getInstance().getContext().getResources().getColor(colorID);
    }

    /**
     * 判断当前内容是否为正确的网址
     * @param urls
     * @return
     */
    public static boolean isHttpUrl(String urls) {
        boolean isurl = false;
        String regex = "(((https|http)?://)?([a-z0-9]+[.])|(www.))"
                + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";//设置正则表达式

        Pattern pat = Pattern.compile(regex.trim());//比对
        Matcher mat = pat.matcher(urls.trim());
        isurl = mat.matches();//判断是否匹配
        if (isurl) {
            isurl = true;
        }
        return isurl;
    }

}
