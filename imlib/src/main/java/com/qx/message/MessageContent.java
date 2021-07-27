package com.qx.message;

import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * 所有消息类型基类
 */
public abstract class MessageContent implements Parcelable , Cloneable {
    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
