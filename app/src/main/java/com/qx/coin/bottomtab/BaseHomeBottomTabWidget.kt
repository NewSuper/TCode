package com.qx.coin.bottomtab


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.qx.coin.R
import com.qx.coin.fragment.ChatFragment
import com.qx.coin.fragment.FindFragment
import com.qx.coin.fragment.MeFragment
import com.qx.coin.fragment.WealthFragment

abstract class BaseHomeBottomTabWidget @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    layoutId: Int
) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {

    private var mFragmentManager: FragmentManager? = null
    private var mFragments: ArrayList<Fragment>? = null
    private lateinit var mViewModel: MainViewModel
    private var currentFragment: Fragment? = null

    /**
     * 外部调用初始化，传入必要的参数
     *
     * @param fm
     */
    fun init(fm: FragmentManager?, viewModel: MainViewModel) {
        mFragmentManager = fm
        mViewModel = viewModel
        if (mFragments == null) {
            mFragments = arrayListOf()
            mFragments?.apply {
                add(getCurrentFragment(0)!!)
                add(getCurrentFragment(1)!!)
                add(getCurrentFragment(2)!!)
                add(getCurrentFragment(3)!!)
            }
        }
        fragmentManger(viewModel.getPage() ?: 0)
    }

    /**
     * 初始化 设置点击事件。
     *
     * @param view /
     */
    @Suppress("LeakingThis")
    abstract fun initView(view: View)

    /**
     * 销毁，避免内存泄漏
     */
    open fun destroy() {
        if (null != mFragmentManager) {
            if (!mFragmentManager!!.isDestroyed)
                mFragmentManager = null
        }
        if (!mFragments.isNullOrEmpty()) {
            mFragments?.clear()
            mFragments = null
        }
    }

    /**
     * fragment的切换 实现底部导航栏的切换
     *
     * @param position 序号
     */
    protected open fun fragmentManger(position: Int) {
        mViewModel.setPage(position)
        val targetFg: Fragment = mFragments!![position]
        val transaction = mFragmentManager!!.beginTransaction()
        transaction.apply {
            if (currentFragment != null) {
                hide(currentFragment!!)
            }
            setReorderingAllowed(true)
            if (!targetFg.isAdded) {
                add(R.id.flHomeFragment, targetFg).commit()
            } else {
                show(targetFg).commit()
            }
        }
        currentFragment = targetFg
    }

    init {
        initView(View.inflate(context, layoutId, this))
    }

    private val mHomeFragment: ChatFragment by lazy { ChatFragment.newInstance() }
    private val mProjectFragment: WealthFragment by lazy { WealthFragment.newInstance() }
    private val mObjectListFragment: FindFragment by lazy { FindFragment.newInstance() }
    private val mProfileFragment: MeFragment by lazy { MeFragment.newInstance() }

    private fun getCurrentFragment(index: Int): Fragment? {
        return when (index) {
            0 -> mHomeFragment
            1 -> mProjectFragment
            2 -> mObjectListFragment
            3 -> mProfileFragment
            else -> null
        }
    }

}