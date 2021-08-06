package com.qx.imui.plugin.image;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.qx.imlib.qlog.QLog;

import androidx.viewpager.widget.ViewPager;

public class HackyViewPager extends ViewPager {

    private static final String TAG = "HackyViewPager";

    public HackyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException var3) {
            QLog.e(TAG, "onInterceptTouchEvent");
            return false;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }
}
