package com.qx.imui.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import com.qx.imui.R
import kotlinx.android.synthetic.main.imui_dialog_retransimission.*

class RetransmissionDialog(context: Context) : Dialog(context) {

    var mContentView: View? = null
    var mListener: OnButtonClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        mContentView = LayoutInflater.from(context).inflate(R.layout.imui_dialog_retransimission, null, false)
        setContentView(mContentView!!)

        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window!!.setGravity(Gravity.BOTTOM)

        initView()
    }

    private fun initView() {

        btn_one_by_one.setOnClickListener {
            mListener?.onOneByOne()
            dismiss()
        }
        btn_combine.setOnClickListener {
            mListener?.onCombine()
            dismiss()
        }
        btn_cancel.setOnClickListener {
            dismiss()
        }
    }

    fun setOnButtonClickListener(listener: OnButtonClickListener) {
        mListener = listener
    }

    interface OnButtonClickListener {
        /**
         * 逐条转发
         */
        fun onOneByOne()

        /**
         * 合并转发
         */
        fun onCombine()
    }
}