package com.qx.imui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.qx.imui.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

public class ChatItemView extends ConstraintLayout {

    AppCompatImageView mIcon;
    TextView mText;

    public ChatItemView(@NonNull Context context) {
        super(context);
        initView(context, null);
    }

    public ChatItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public ChatItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    public ChatItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs){
        LayoutInflater.from(context).inflate(R.layout.imui_plugin_root_emotion_item,this,true);
        mIcon = ((AppCompatImageView)findViewById(R.id.iv_icon));
        mText = ((TextView)findViewById(R.id.tv_content));

        if (attrs != null){
            TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.ChatItemView);
            int icon = array.getResourceId(R.styleable.ChatItemView_icon, R.drawable.imui_ic_chat_gallery);
            String text = array.getString(R.styleable.ChatItemView_text);
            array.recycle();
            mIcon.setImageResource(icon);
            mText.setText(text);
        }
    }

    public void setIcon(int iconResId) {
        mIcon.setImageResource(iconResId);
    }

    public void setIcon(Drawable drawable) {
        mIcon.setImageDrawable(drawable);
    }

    public void setText(int textResId) {
        mText.setText(textResId);
    }

    public void setText(String title) {
        mText.setText(title);
    }
}
