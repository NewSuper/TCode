package com.qx.imui.view.bottom;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.qx.imui.util.KeybordUtils;
import com.qx.imui.util.ScreenUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public abstract class BaseDialogFragment extends DialogFragment {
    protected Activity mActivity;
    protected View mRootView;
    protected Window mWindow;

    public Integer[] mWidthAndHeight;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        //点击外部消失
        dialog.setCanceledOnTouchOutside(true);
        mWindow = dialog.getWindow();
        mWidthAndHeight = ScreenUtils.getWidthAndHeight(mWindow);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(setLayoutId(), container, false);
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
        initView(view);
        setListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        KeybordUtils.hideSoftKeyBoard(mActivity);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mWidthAndHeight = ScreenUtils.getWidthAndHeight(mWindow);
    }


    /**
     * 对应的布局
     * @return
     */
    protected abstract int setLayoutId();

    /**
     * 初始化数据
     */
    protected void initData() {

    }

    /**
     * view与数据绑定
     */
    protected void initView(View view) {

    }

    /**
     * 设置监听
     */
    protected void setListener() {

    }
}
