package com.qx.imui.adapter

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qx.imui.R
import com.qx.imui.bean.menu.QXMenu
import com.qx.imui.util.DensityUtil
import com.qx.message.Message
import kotlinx.android.synthetic.main.imui_layout_message_popup_window.view.*

object MessagePopupWindowUtil {

    private var contentView: View? = null
    private var mHorizonalOffset: Int = 0
    private var mVerticalOffset: Int = 0
    private lateinit var mPaint: Paint
    private var mPopupWindow: PopupWindow? = null

    //消息长按弹窗适配器
    private lateinit var mMessagePopAdapter: MessagePopupWindowAdapter
    private lateinit var mMessagePopManager: GridLayoutManager

    /**
     * 箭头view
     */
    private var mTopAllowView: View? = null
    private var mBottomAllowView: View? = null
    private var mContext: Context? = null

    private var mOnItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mOnItemClickListener = listener
    }

    interface OnItemClickListener {
        fun onMenuItemClick(menu: QXMenu, message: Message)
    }

    /**
     * 弹出Popupwindow菜单窗口
     */
    fun show(context: Context, message: Message, menuList:List<QXMenu>, titleBarHeight: Float, view: View) {
        mContext = context
        initPopupWindow(context,message,menuList)
        mMessagePopAdapter.notifyDataSetChanged()
        var statusBarHeight = getStatusBarHeight(context)
        var x = 0
        var y = 0
        var location = IntArray(2)
        view.getLocationInWindow(location)
        var allow: View? = null
        if (location[1] - titleBarHeight - statusBarHeight < mPopupWindow!!.height) {
            //如果item靠近顶部，并且空间不足以显示弹窗，则显示在item的下方
            mTopAllowView!!.visibility = View.VISIBLE
            mBottomAllowView!!.visibility = View.GONE
            allow = mTopAllowView
        } else {
            //显示在上方
            mTopAllowView!!.visibility = View.GONE
            mBottomAllowView!!.visibility = View.VISIBLE
            allow = mBottomAllowView

            var lp = view.layoutParams as ConstraintLayout.LayoutParams
            var marginTop = lp.topMargin
            y = -(view.bottom - view.top) - mPopupWindow!!.height + marginTop +
                    DensityUtil.dip2px(context, context.resources.getDimension(R.dimen.message_content_top_margin))
        }
        //获取弹窗的x、y坐标
        var popLocation = IntArray(2)
        mPopupWindow!!.contentView.getLocationOnScreen(popLocation)
        val displayWidth = DensityUtil.getScreenWidth(context)
        val isLeft = location[0] <=  DensityUtil.dip2px(context, 54f)
        var itemViewWidth = view.right - view.left
        var allowLayoutParam = allow!!.layoutParams as LinearLayout.LayoutParams
//        消息内容的x坐标-弹窗x坐标+消息内容长度/2=小三角的x坐标
        if (isLeft) {
            allowLayoutParam.leftMargin = itemViewWidth / 2 - DensityUtil.dip2px(context, 8f)
        } else {
            allowLayoutParam.gravity = Gravity.RIGHT
            allowLayoutParam.rightMargin = DensityUtil.dip2px(context, 46f) + itemViewWidth / 2
        }
        allow!!.layoutParams = allowLayoutParam
        mPopupWindow!!.showAsDropDown(view, x, y)
    }

    private fun initPopupWindow(context: Context, message: Message, menuList:List<QXMenu>) {
        mHorizonalOffset = DensityUtil.dip2px(context, 16f)
        mVerticalOffset = DensityUtil.dip2px(context, 10f)
        contentView = LayoutInflater.from(context).inflate(R.layout.imui_layout_message_popup_window, null, false)
        mTopAllowView = contentView!!.iv_allow_top
        mBottomAllowView = contentView!!.iv_allow_bottom
        initMessagePopItemRecyclerView(context, contentView!!,message,menuList)

        mPopupWindow = PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mPopupWindow!!.isOutsideTouchable = true
        mPopupWindow!!.isFocusable = true
        mPopupWindow!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        mPopupWindow!!.contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        mPopupWindow!!.height = mPopupWindow!!.contentView.measuredHeight
    }

    private fun initMessagePopItemRecyclerView(context: Context, contentView: View, message: Message, menuList:List<QXMenu>) {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.color = context.resources.getColor(R.color.message_pop_line)

        var spanCount = menuList.size
        if(menuList.size > 5) {
            spanCount = 5
        }
        mMessagePopManager = GridLayoutManager(context, spanCount)
        mMessagePopAdapter = MessagePopupWindowAdapter(context,menuList)
        var dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            dividerItemDecoration.setDrawable(context.resources.getDrawable(R.drawable.imui_shape_msg_pop_divider_bg, null))
        } else {
            dividerItemDecoration.setDrawable(context.resources.getDrawable(R.drawable.imui_shape_msg_pop_divider_bg))
        }
        contentView.recycler_view_msg_pop.apply {
            setHasFixedSize(true)
            layoutManager = mMessagePopManager
            adapter = mMessagePopAdapter
//            addItemDecoration(dividerItemDecoration)
            addItemDecoration(object : RecyclerView.ItemDecoration() {

                override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                    super.onDraw(canvas, parent, state)
                    //画分割线
                    val gridLayoutManager: GridLayoutManager? = parent.layoutManager as GridLayoutManager?
                    val spanCount: Int = gridLayoutManager!!.spanCount
                    if (parent.childCount > 0) {
                        var left = parent.getChildAt(0).left.toFloat()
                        var right: Float = 0f
                        right = if (parent.childCount > spanCount) {
                            parent.getChildAt(spanCount - 1).right.toFloat()
                        } else {
                            parent.getChildAt(parent.childCount - 1).right.toFloat()
                        }
                        var top = parent.getChildAt(0).bottom.toFloat() + mVerticalOffset
                        var bottom = top + 1
                        canvas.drawRect(left, top, right, bottom, mPaint)
                    }
                }

                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    super.getItemOffsets(outRect, view, parent, state)
                    var off_h = DensityUtil.dip2px(context, 16f)
                    var off_v = DensityUtil.dip2px(context, 10f)
                    outRect.set(off_h, off_v, off_h, off_v)

                }
            })
        }
        mMessagePopAdapter.setOnMessagePopupItemClickListener(object : MessagePopupWindowAdapter.OnMessagePopupItemClickListener {
            override fun onClick(menu: QXMenu) {
                mPopupWindow?.dismiss()
                mOnItemClickListener?.onMenuItemClick(menu, message!!)
            }

        })
    }

    fun getStatusBarHeight(context: Context): Int {
        val resources: Resources = context.resources
        val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }
}