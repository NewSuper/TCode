package com.qx.imui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.qx.imui.R
import com.qx.message.Message
import com.qx.message.MessageContent
import com.qx.message.RecordMessage
import kotlinx.android.synthetic.main.imui_layout_msg_content_record.view.*

class ChatRecordMessageHandler : ChatBaseMessageHandler() {
    override fun setContentView(itemView: View, contentLayout: ViewGroup, message: Message) {
        var contentView = createContentView(itemView, contentLayout, R.layout.imui_layout_msg_content_record)
        super.setContentView(itemView, contentLayout, message)
        var content: MessageContent? = getMessageContent(message) ?: return
        content = content as RecordMessage
        var contentLayout = (contentView!!.getChildAt(0)) as ConstraintLayout
        contentLayout.tv_record_title.text = UserInfoUtil.getRecordTitle(itemView.context, message)
        contentLayout.tv_record_content.text = UserInfoUtil.getRecordText(itemView.context, content)
    }
}