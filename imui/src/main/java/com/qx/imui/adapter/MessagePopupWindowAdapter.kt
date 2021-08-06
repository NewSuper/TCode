package com.qx.imui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qx.imui.R
import com.qx.imui.bean.menu.QXMenu
import com.qx.imui.util.StringUtils
import kotlinx.android.synthetic.main.imui_item_msg_pop.view.*

class MessagePopupWindowAdapter(context: Context, private var mDataSet: List<QXMenu>) : RecyclerView.Adapter<MessagePopupWindowAdapter.Holder>() {
    var context: Context = context;
    lateinit var mListener: OnMessagePopupItemClickListener

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.imui_item_msg_pop, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, postition: Int) {
        val menu = mDataSet[postition];
        holder.itemView.iv_icon.setImageResource(menu.icon)
        holder.itemView.tv_text.text = StringUtils.getResourceStr(context,menu.text)
        holder.itemView.setOnClickListener {
            mListener?.onClick(menu)
        }
    }

    override fun getItemCount() = mDataSet.size

    fun setOnMessagePopupItemClickListener(listener: OnMessagePopupItemClickListener) {
        mListener = listener
    }

    interface OnMessagePopupItemClickListener {
        fun onClick(menu: QXMenu)
    }
}