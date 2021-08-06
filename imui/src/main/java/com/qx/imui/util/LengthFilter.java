package com.qx.imui.util;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;

public class LengthFilter implements InputFilter {

    private final int MAX_EN; // 最大英文/数字长度 一个汉字算两个字母

    public LengthFilter(int MAX_EN) {
        this.MAX_EN = MAX_EN;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

        // 新输入的字符串为空直接不接收（删除剪切等）
        if (TextUtils.isEmpty(source)) return "";

        // 输入前就存在的字符长度
        int destCount = getCount(dest.toString());

        // 输入前就已满直接不接收
        if (destCount >= MAX_EN) return "";

        // 新输入的字符长度
        int sourceCount = getCount(source.toString());

        // 如果拼接后不超长，直接拼接
        if (destCount + sourceCount <= MAX_EN) return source;

        // 超长时不应该直接拒绝，应在允许范围内尽量拼接
        return getByCount(source.toString(), MAX_EN - destCount);
    }

    /**
     * 超长时根据剩余长度在字符范围内截取字符串
     */
    private String getByCount(String s, int count) {
        String temp = "";
        int tempCount = 0;

        char[] cs = s.toCharArray();
        for (char c : cs) {
            if (tempCount + getCount(c) <= count) {
                tempCount += getCount(c);
                temp += c;
            } else {
                break;
            }
        }

        return temp;
    }

    /**
     * 计算字符串长度
     */
    private int getCount(String s) {
        if (TextUtils.isEmpty(s)) {
            return 0;
        }

        int count = 0;
        char[] cs = s.toCharArray();
        for (char c : cs) {
            count += getCount(c);
        }

        return count;
    }

    /**
     * 单字符占位判定
     */
    private int getCount(char c) {
        return String.valueOf(c).getBytes().length > 2 ? 2 : 1;
    }
}
