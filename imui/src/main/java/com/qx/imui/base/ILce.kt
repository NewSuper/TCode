package com.qx.imui.base

import android.view.View
import com.qx.imui.QXContext.getString
import com.qx.imui.R

//策略模式
interface ILce {

    fun startLoading()

    fun loadFinished()

    //当activity中的加载肉容服务器返回失败，通过此方法显示提示给用户
    // fun showLoadErrorView(tip: String = BaseApplication.instance!!.getString(R.string.failed_load_data))
    fun showLoadErrorView(tip: String = getString(R.string.failed_load_data))

    //当activity中的内容因网络原因无法显示，通过此方法提示用户------重新点击加载事件回调
    fun showBadNetworkView(listener: View.OnClickListener)

    //当activity中没设置任何内容，通过此方法提示用户
    fun showNoContentView(tip: String)

}