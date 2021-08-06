package com.qx.imui.plugin.photovideo

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.qx.imui.R
import com.qx.imui.base.BaseActivity
import kotlinx.android.synthetic.main.imui_preview_image_activity.*

/**
 * 本地图片预览
 */
class ImagePreviewActivity : BaseActivity() {
    override fun getLayoutId() = R.layout.imui_preview_image_activity
    override fun onCreate(savedInstanceState: Bundle?) {
        //全屏模式
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        //透明导航栏
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        super.onCreate(savedInstanceState)

        iv_image.enable()
        var url = intent.getStringExtra(Camera2Config.INTENT_PLUGIN_PATH_KEY)
        Glide.with(this).load(url).apply(RequestOptions().placeholder(R.mipmap.default_img).error(R.mipmap.default_img)).into(iv_image)

        iv_close.setOnClickListener {
            onBackPressed()
        }

        tv_save.setOnClickListener {
            var intent = Intent()
            intent.putExtra(Camera2Config.INTENT_PLUGIN_TYPE_KEY, Camera2Config.INTENT_PATH_SAVE_PIC);
            intent.putExtra(Camera2Config.INTENT_PLUGIN_PATH_KEY, url);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    override fun initView() {
        TODO("Not yet implemented")
    }

}