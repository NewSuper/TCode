package com.qx.coin


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.qx.imui.base.BaseActivity
import com.qx.imui.base.showToast
import com.qx.coin.bottomtab.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess
import androidx.activity.viewModels
import com.qx.coin.http.HttpCallback
import com.qx.coin.http.OkhttpUtils

class MainActivity : BaseActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun initView() {
        homeView.init(supportFragmentManager, viewModel)
    }

//    override fun initData() {
//        super.initData()
//        OkhttpUtils.getUsers(object : HttpCallback<String> {
//            override fun onSuccess(t: String) {
//
//            }
//
//            override fun onError(errorCode: Int, message: String) {
//            }
//
//        })
//    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        //super.onSaveInstanceState(outState)  // 解决fragment重影
    }

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun onBackPressed() {
        super.onBackPressed()
        exitProcess(0)
    }

    private var exitTime: Long = 0

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit()
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun exit() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            showToast(getString(R.string.exit_program))
            exitTime = System.currentTimeMillis()
        } else {
            exitProcess(0)
        }
    }

    companion object {
        fun actionStart(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

}
