package com.qx.imui.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qx.im.model.QXUserInfo
import com.qx.imui.plugin.CustomMessageManager
import com.qx.imui.util.ViewTypeUtil
import com.qx.message.Message
import com.qx.message.MessageType

class MessageAdapter(context: Context, private val myDataSet: List<Message>) : RecyclerView.Adapter<BaseViewHolder>() {

    private lateinit var mItemListener: ItemListener
    private var mCheckable = false
    var mContext = context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        //根据viewType加载布局
        return ViewCreator.create(mContext,viewType, parent)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        var last = myDataSet[position]
        if (position > 0) {
            last = myDataSet[position - 1]
        }
        var data = myDataSet[position]
        holder.fill(last, data, mItemListener, position, mCheckable)
    }

    override fun getItemCount() = myDataSet.size

    override fun getItemViewType(position: Int): Int {
        //根据消息类型，设置viewType
        var message = myDataSet[position]
        return when (message.messageType) {
            MessageType.TYPE_NOTICE, MessageType.TYPE_RECALL -> {
                ViewTypeUtil.mViewTypeMap[message.messageType]!!
            }
            else -> {
                var provider = CustomMessageManager.getNoticeProvider(message.messageType)
                if (provider != null && provider.isNotice()) {
                    ViewTypeUtil.mViewTypeMap[MessageType.TYPE_NOTICE]!!
                } else if (message.direction == Message.Direction.DIRECTION_SEND) {
                    ViewTypeUtil.mViewTypeMap[ViewTypeUtil.TYPE_RIGHT]!!
                } else {
                    ViewTypeUtil.mViewTypeMap[ViewTypeUtil.TYPE_LEFT]!!
                }
            }
        }
    }

    fun setOnItemClickListener(listener: ItemListener) {
        mItemListener = listener
    }

    fun setMultipleCheckable(checkable: Boolean) {
        this.mCheckable = checkable
        notifyDataSetChanged()
    }

    interface ItemListener {

        /**
         * Item点击
         */
        fun onItemClick(position: Int)

        /**
         * 短按
         */
        fun onClick(position: Int, v: View)

        /**
         * 长按
         */
        fun onLongClick(position: Int, v: View): Boolean

        /**
         * 重发按钮被点击
         */
        fun onResend(position: Int, v: View)

        /**
         * 消息选择
         */
        fun onChecked(message: Message, isCheck: Boolean)

        /**
         * 头像点击
         */
        fun onAvatarClick(userId: String)

        fun onUserPortraitLongClick(context: Context, conversationType: String, userInfo: QXUserInfo, targetId: String)

        /**
         * 回复消息处理点击事件
         */
        fun onReplyMessageClick(message: Message, v: View)
    }


}