package com.qx.imui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

public class AutoLinkTextView extends AppCompatImageView {
    public AutoLinkTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public AutoLinkTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public AutoLinkTextView(Context context) {
        super(context);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
//        SpannableString span = new SpannableString(getText());
//        ClickableSpan[] links = span.getSpans(getSelectionStart(),
//                getSelectionEnd(), ClickableSpan.class);
//        if (links.length != 0) {
//            return true;
//        }
        return false;
    }
}
