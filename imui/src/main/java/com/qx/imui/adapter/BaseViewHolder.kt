package com.qx.imui.adapter

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.qx.imui.R
import com.qx.imui.util.TimeUtil
import com.qx.message.Message
import kotlinx.android.synthetic.main.imui_item_time_msg.view.*

abstract class BaseViewHolder(context: Context, itemView: View) :
    RecyclerView.ViewHolder(itemView) {

    var checkable = false
    var context = context
    private var INTERVAL_MESSAGE_TIME = 300000 //显示消息时间的间隔，默认5min
    open fun fill(lastMsg : Message, currMsg : Message, listener: MessageAdapter.ItemListener, position : Int, checkable : Boolean) {

        //处理消息列表显示的消息时间
        if (currMsg.timestamp - lastMsg.timestamp >= INTERVAL_MESSAGE_TIME || (currMsg.messageId == lastMsg.messageId)) {
            itemView.tv_time_msg_time?.text = TimeUtil.getTimeString(context,currMsg.timestamp)
            itemView.tv_time_msg_time?.visibility = View.VISIBLE
        } else {
            itemView.tv_time_msg_time?.visibility = View.GONE
        }
    }

    protected fun setBroadCastView(to: String, textView: TextView, imageView: ImageView, context: Context) {
        //判断是否为广播，to为空即为广播
        if (to.isEmpty()) {
            textView.setText(context.getString(R.string.qx_message_broadcast))
            Glide.with(context).load(R.mipmap.chat_avatar_system).apply(
                RequestOptions.bitmapTransform(
                    CircleCrop()
                )
            ).into(imageView)
        }
    }
}