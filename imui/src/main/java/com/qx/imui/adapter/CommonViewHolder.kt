package com.qx.imui.adapter

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import com.qx.im.model.ConversationType
import com.qx.im.model.QXUserInfo
import com.qx.imui.QXContext
import com.qx.imui.QXIMKit
import com.qx.imui.R
import com.qx.imui.plugin.CustomMessageManager
import com.qx.imui.util.ToastUtil
import com.qx.imui.util.glide.GlideUtil
import com.qx.message.Conversation
import com.qx.message.Message
import com.qx.message.MessageType

class CommonViewHolder (context: Context,itemView:View):BaseViewHolder(context,itemView){
    var mContext = context
    override fun fill(
        lastMsg: Message,
        currMsg: Message,
        listener: MessageAdapter.ItemListener,
        position: Int,
        checkable: Boolean
    ) {
        var ivAvatar = itemView.findViewById<ImageView>(R.id.iv_avatar)
        var tvNickName = itemView.findViewById<TextView>(R.id.tv_nick_name)
        var tvResend = itemView.findViewById<TextView>(R.id.tv_resend)
        var pbSending = itemView.findViewById<ProgressBar>(R.id.pb_sending)
        var layoutMessageContent =
            itemView.findViewById<ConstraintLayout>(R.id.layout_message_content)
        var checkbox = itemView.findViewById<AppCompatCheckBox>(R.id.iv_check_box)
        //处理多选
        checkbox.visibility = if (checkable) {
            View.VISIBLE
        } else {
            View.GONE
        }
        checkbox.isChecked = currMsg.isChecked

        //处理头像、昵称
        GlideUtil.loadAvatar(
            itemView.context,
            UserInfoUtil.getAvatar(currMsg, currMsg?.senderUserId),
            ivAvatar
        )
        if (currMsg.direction == Message.Direction.DIRECTION_RECEIVED) {
            if (currMsg.conversationType == Conversation.Type.TYPE_PRIVATE) {
                tvNickName.visibility = View.GONE
            } else {
                tvNickName.visibility = View.VISIBLE
                tvNickName.text =
                    UserInfoUtil.getTargetName(itemView.context, currMsg, currMsg?.targetId)
            }
        }
        val avatarExtraUrl = UserInfoUtil.userAvatarExtraUrl(currMsg)
        if (avatarExtraUrl.isNullOrEmpty()) {
            itemView.findViewById<ImageView>(R.id.iv_avatar_official_flag).visibility = View.GONE
        } else {
            itemView.findViewById<ImageView>(R.id.iv_avatar_official_flag).visibility = View.VISIBLE
            GlideUtil.loadAvatarFlag(
                itemView.context,
                avatarExtraUrl,
                itemView.findViewById(R.id.iv_avatar_official_flag)
            )
        }

        val nameExtraUrl = UserInfoUtil.userNameExtraUrl(currMsg)
        if (!nameExtraUrl.isNullOrEmpty() && currMsg.direction == Message.Direction.DIRECTION_RECEIVED
            && currMsg.conversationType == Conversation.Type.TYPE_GROUP
        ) {
            //只有群聊，别人发的消息才有名称标识
            itemView.findViewById<ImageView>(R.id.iv_name_offical_flag).visibility = View.VISIBLE
            GlideUtil.loadAvatarFlag(
                itemView.context,
                nameExtraUrl,
                itemView.findViewById<ImageView>(R.id.iv_name_offical_flag)
            )
        } else {
            itemView.findViewById<ImageView>(R.id.iv_name_offical_flag).visibility = View.GONE
        }

        var sendFailedLayout = itemView.findViewById<ViewGroup>(R.id.layout_send_failed)
        //发送失败，显示重发按钮
        if (currMsg.direction == Message.Direction.DIRECTION_SEND) {
            when (currMsg.state) {
                Message.State.STATE_SENDING -> {
                    tvResend.visibility = View.GONE
                    pbSending.visibility = View.VISIBLE
                    sendFailedLayout.removeAllViews()
                }
                Message.State.STATE_FAILED -> {
                    tvResend.visibility = View.VISIBLE
                    pbSending.visibility = View.GONE
                    postErrorEventToUI(
                        currMsg.failedReason,
                        currMsg,
                        itemView.findViewById(R.id.layout_send_failed)
                    )
                    tvResend.setOnClickListener {
                        listener?.onResend(adapterPosition, tvResend)
                    }
                }
                else -> {
                    sendFailedLayout.removeAllViews()
                    tvResend.visibility = View.GONE
                    pbSending.visibility = View.GONE
                }
            }
        } else {
            pbSending.visibility = View.GONE
        }
        //点击item也可以让checkbox生效
        itemView.setOnClickListener {
            if (checkable) {
                if (isSupportType(currMsg.messageType)) {
                    checkbox.isChecked = !checkbox.isChecked
                    listener?.onChecked(currMsg, checkbox.isChecked)
                } else {
                    ToastUtil.toast(context, R.string.qx_un_support_message_type)
                }
            }
        }

        checkbox.setOnClickListener {
            if (checkable) {
                listener?.onChecked(currMsg, checkbox.isChecked)
            }
        }
        //处理点击事件
        layoutMessageContent.setOnClickListener {
            if (checkable) {
                checkbox.isChecked = !checkbox.isChecked
                listener?.onChecked(currMsg, checkbox.isChecked)
            } else {
                // 点击事件返回 itemView
                var messageProvider = CustomMessageManager.getMessageProvider(currMsg.messageType)
                if (messageProvider != null) {
                    messageProvider?.onClick(itemView, currMsg)
                } else {
                    listener?.onClick(position, itemView)
                }
            }
        }
        //处理长按事件
        layoutMessageContent.setOnLongClickListener {
            if (currMsg.messageType != MessageType.TYPE_RECALL) {
                listener?.onLongClick(position, layoutMessageContent)
            }
            true
        }

        //处理头像点击事件
        ivAvatar.setOnClickListener {
            listener?.onAvatarClick(currMsg?.senderUserId)
        }

        //处理空白区域点击事件
        itemView.findViewById<ConstraintLayout>(R.id.msg_comm_left)?.setOnClickListener {
            listener?.onItemClick(position)
        }
        //处理空白区域点击事件
        itemView.findViewById<ConstraintLayout>(R.id.msg_comm_right)?.setOnClickListener {
            listener?.onItemClick(position)
        }

        ivAvatar.setOnLongClickListener {
            var userinfo = currMsg.userInfo
            if (userinfo == null) {
                userinfo = QXUserInfoManager.getInstance().getUserInfo(currMsg.senderUserId)
            }
            if (userinfo == null) {
                userinfo = QXUserInfo()
                userinfo!!.id = currMsg.senderUserId
                userinfo!!.avatarUri = Uri.parse("")
            }
            if (currMsg.conversationType == ConversationType.TYPE_GROUP) {
                val groupUserInfo = QXUserInfoManager.getInstance()
                    .getGroupUserInfo(currMsg.targetId, currMsg.senderUserId)
                if (groupUserInfo != null && userinfo != null) {
                    userinfo.noteName = groupUserInfo.displayName
                }
            }
            if (currMsg.direction == Message.Direction.DIRECTION_SEND) {
                if (QXContext.getInstance().conversationClickListener != null) {
                    return@setOnLongClickListener QXContext.getInstance().conversationClickListener.onUserPortraitLongClick(
                        it.context,
                        currMsg.conversationType, userinfo, currMsg.targetId
                    )
                }
            } else {
                val conversationClickListener = QXContext.getInstance().conversationClickListener
                if (conversationClickListener != null &&
                    QXContext.getInstance().conversationClickListener.onUserPortraitLongClick(
                        it.context,
                        currMsg.conversationType, userinfo, currMsg.targetId
                    )
                ) {
                    return@setOnLongClickListener true
                }
                listener?.onUserPortraitLongClick(
                    it.context,
                    currMsg.conversationType,
                    userinfo,
                    currMsg.targetId
                )
            }
            return@setOnLongClickListener true
        }

        setBroadCastView(
            currMsg.targetId, tvNickName, ivAvatar, itemView.context
        )

        //处理被回复消息的点击事件
        var tv_answer = itemView.findViewById<TextView>(R.id.tv_answer)
        tv_answer?.setOnClickListener {
            Log.i("CommonViewHolder", "查看回复~")
            if (currMsg.messageType == MessageType.TYPE_REPLY) {
                listener?.onReplyMessageClick(currMsg, itemView)
            }
        }
        //消息UI分发，各种消息UI绘制由各个handler处理
        MessageListDispatcher.dispatch(itemView, layoutMessageContent, currMsg)
    }

    private fun isSupportType(messageType:String?):Boolean{
        return MessageType.isPreSetMessage(messageType)
    }
    private fun postErrorEventToUI(errorMsg:String,message:Message,errorRootView:View){
        var provider = QXIMKit.getSendMessageFailedProvider()
        provider?.onFailed(errorMsg,message,errorRootView)
    }
}