package com.qx.imui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.qx.message.Message

abstract class BaseMessageHandler {

    fun createContentView(itemView: View, viewGroup: ViewGroup, layoutId: Int): ViewGroup? {
        return LayoutInflater.from(itemView.context).inflate(layoutId, viewGroup, true) as ViewGroup?
    }

    abstract fun setContentView(itemView: View, contentLayout: ViewGroup, message: Message)

}