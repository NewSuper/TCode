package com.qx.imui.emotion

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qx.imui.QXExtension
import com.qx.imui.R
import com.qx.imui.plugin.IPluginModule
import com.qx.imui.view.ChatItemView

open class ChatPanelAdapter(var mDataSet: List<IPluginModule>,var qxExtension: QXExtension) : RecyclerView.Adapter<ChatPanelAdapter.Holder>() {

    lateinit var mListener: OnChatPanelItemClickListener

    class Holder(itemView: ChatItemView) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.imui_plugin_root_panel, parent, false) as ChatItemView)
    }

    override fun onBindViewHolder(holder: Holder, postition: Int) {
        var item = holder.itemView as ChatItemView
        item.setIcon(mDataSet[postition].obtainDrawable(holder.itemView.context))
        item.setText(mDataSet[postition].obtainTitle(holder.itemView.context))
        item.setOnClickListener {
            mDataSet[postition].onClick(holder.itemView.context as Activity,qxExtension)
            mListener?.onClick(mDataSet[postition],postition)
        }
    }

    override fun getItemCount() = mDataSet.size

    fun setOnChatPanelItemClickListener(listener: OnChatPanelItemClickListener) {
        mListener = listener
    }

    fun getPluginPosition(plugin: IPluginModule): Int {
        for (index in mDataSet.indices) {
            if (mDataSet[index] == plugin) {
                return index
            }
        }
        return 0
    }

    fun getPluginModule(position: Int) : IPluginModule {
        return mDataSet[position]
    }

    interface OnChatPanelItemClickListener {
        fun onClick(pluginmodule:IPluginModule,position: Int)
    }

}