package com.qx.imlib.utils.sensitive;

import android.content.Context;

import com.qx.im.model.BeanSensitiveWord;
import com.qx.imlib.qlog.QLog;
import com.qx.imlib.utils.ContextUtils;
import com.qx.imlib.utils.SharePreferencesUtil;
import com.qx.message.SensitiveWordResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SensitiveWordsUtils {
    private static BeanSensitiveWord[] mBeans;

    private static void init(Context context) {
        if (mBeans == null || mBeans.length == 0) {
            readSensitiveWord(context);
        }
    }

    private static Set<BeanSensitiveWord> readSensitiveWord(Context context) {
        Set<BeanSensitiveWord> set = new HashSet<>();
        mBeans = SharePreferencesUtil.Companion.getInstance(context).loadSensitiveWord();
        if (mBeans != null) {
            for (BeanSensitiveWord word : mBeans) {
                set.add(word);
            }
        }
        return set;
    }

    public static SensitiveWordResult checkSensitiveWord(String text, Context context) {
        long time = System.currentTimeMillis();
        init(context);
        Set<String> banSet = new HashSet<>();
        Map<String, String> replaceMap = new HashMap<>();
        if (mBeans != null && mBeans.length > 0) {
            for (BeanSensitiveWord bean : mBeans) {
                if (bean.getType().equals(BeanSensitiveWord.Type.TYPE_REPLACE)) {
                    replaceMap.put(bean.getSensitiveWord(), bean.getReplaceWord());
                } else {
                    banSet.add(bean.getSensitiveWord());
                }
            }
            boolean isBan = isBan(text);
            if (isBan) {
                text = replace(text);
            }
            return new SensitiveWordResult(isBan, text);
        }
        return null;
    }

    private static String replace(String text) {
        Map<String, String> replaceMap = new HashMap<>();
        for (BeanSensitiveWord bean : mBeans) {
            if (bean.getType().equals(BeanSensitiveWord.Type.TYPE_REPLACE)) {
                replaceMap.put(bean.getSensitiveWord(), bean.getReplaceWord());
            }
        }
        Set<Map.Entry<String, String>> entries = replaceMap.entrySet();
        SubSensitive sensitive = new SubSensitive();
        sensitive.setSpecialCharacters(getSpecialCharacters());
        for (Map.Entry<String, String> entry : entries) {
            long start = System.currentTimeMillis();
            String key = entry.getKey();
            String value = entry.getValue();
            text = sensitive.setSensitiveWord(key).replaceSensitiveWord(text, value);
            QLog.d("SensitiveWordsUtils", "replace key" + key + ",time: " + (System.currentTimeMillis() - start));
        }
        return text;
    }

    private static char[] getSpecialCharacters() {
        return SharePreferencesUtil.Companion.getInstance(ContextUtils.getInstance().getContext()
        ).loadSpecialCharacters().toCharArray();
    }

    private static boolean isBan(String text) {
        Set<String> banSet = new HashSet<>();
        for (BeanSensitiveWord bean : mBeans) {
            if (bean.getType().equals(BeanSensitiveWord.Type.TYPE_BAN)) {
                banSet.add(bean.getSensitiveWord());
            }
        }
        Sensitive sensitive = new Sensitive(banSet);
        return sensitive.contains(text);
    }

    interface SenstiveWordCallback {
        void onResult(SensitiveWordResult result);
    }

}
