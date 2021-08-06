package com.qx.imui.adapter

import android.view.View
import android.view.ViewGroup
import com.qx.imui.plugin.CustomMessageManager
import com.qx.imui.plugin.MessageProvider
import com.qx.message.Message

class ChatCustomMessageHandler : ChatBaseMessageHandler() {
    override fun setContentView(itemView: View, contentLayout: ViewGroup, message: Message) {
        var provider: MessageProvider? = CustomMessageManager.getMessageProvider(message.messageType)
            ?: return

        val layout = provider?.getViewId() ?: return
        createContentView(itemView, contentLayout, layout)
        super.setContentView(itemView, contentLayout, message)

        provider?.bindView(itemView, message)
    }

}