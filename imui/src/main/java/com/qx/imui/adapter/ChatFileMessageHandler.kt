package com.qx.imui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.qx.imlib.utils.file.FileSizeUtil
import com.qx.imui.R
import com.qx.imui.util.FileUtils
import com.qx.message.FileMessage
import com.qx.message.Message
import com.qx.message.MessageContent

class ChatFileMessageHandler : ChatBaseMessageHandler() {

    override fun setContentView(itemView: View, contentLayout: ViewGroup, message: Message) {
        var contentView = createContentView(itemView, contentLayout, R.layout.imui_layout_msg_content_file)
        super.setContentView(itemView, contentLayout, message)
        var content: MessageContent? = getMessageContent(message) ?: return
        content = content as FileMessage

        var rootView = (contentView!!.getChildAt(0)) as ConstraintLayout

        var tv_file_name = rootView.findViewById<TextView>(R.id.tv_file_name)
        var tv_size = rootView.findViewById<TextView>(R.id.tv_size)
        var iv_icon = rootView.findViewById<AppCompatImageView>(R.id.iv_icon)

        tv_file_name.text = content.fileName
        tv_size.text = FileSizeUtil.FormetFileSize(content.size)

        iv_icon.setImageResource(FileUtils.getResource(content))
    }


}