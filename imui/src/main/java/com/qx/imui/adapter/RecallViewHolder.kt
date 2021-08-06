package com.qx.imui.adapter

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.qx.imlib.QXIMClient
import com.qx.imui.R
import com.qx.message.Message
import kotlinx.android.synthetic.main.imui_item_recall_msg.view.*

class RecallViewHolder(context: Context, itemView: View) : BaseViewHolder(context, itemView) {
    var mContext = context
    override fun fill(lastMsg: Message, currMsg: Message, listener: MessageAdapter.ItemListener, position: Int, checkable: Boolean) {
        super.fill(lastMsg, currMsg, listener, position, checkable)

        var commRecalllayout = itemView.findViewById<ConstraintLayout>(R.id.msg_comm_recall)
        if (commRecalllayout != null) {
            commRecalllayout.setOnClickListener {
                listener?.onItemClick(position)
            }
        }

        //如果是自己撤回
        if (currMsg.senderUserId == QXIMClient.instance.getCurUserId()) {
            itemView.tv_recall_msg.text = mContext.getString(R.string.qx_recall_by_user)
        } else {
            //如果是别人撤回的
            var name = ""
            var userInfo = UserInfoUtil.getUserInfo(currMsg, currMsg.senderUserId)
            if (userInfo != null) {
                name = currMsg.userInfo.displayName
            }

            itemView.tv_recall_msg.text = String.format(mContext.getString(R.string.qx_recall_by_other), name)
        }
    }
}