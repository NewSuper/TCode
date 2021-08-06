package com.qx.imui.view.bottom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.qx.imui.R;
import com.qx.imui.util.StringUtils;

import java.util.List;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class BottomMenuDialog extends BaseDialogFragment {
    private Context context;
    /**
     * 菜单列表
     */
    RecyclerView recyclerView;

    /**
     * 标题
     */
    TextView titleText;
    /**
     * 分割线
     */
    TextView titleSplitLine;

    /**
     * 菜单颜色
     */
    private int menuColor;

    /**
     * 标题
     */
    private String titleTextStr;

    /**
     * 菜单列表适配器
     */
    private BottomPopupListAdapter commonAdapter;
    /**
     * 菜单列表数据
     */
    private List<BottomMenuItem> menuItemList;

    /**
     * 点击回调
     */
    private OnMenuItemClickListener onMenuItemClickListener;


    /**
     * 定义点击时间
     */
    public interface OnMenuItemClickListener {
        void callback(int itemId, BottomMenuItem ob);
    }

    /**
     * 设置菜单列表
     *
     * @param context
     * @param menuItemList
     */
    public void setBottomMenuList(Context context, List<BottomMenuItem> menuItemList) {
        this.context = context;
        this.menuItemList = menuItemList;
    }

    /**
     * 事件回调
     *
     * @param callBack
     */
    public void setOnMenuItemClickListener(OnMenuItemClickListener callBack) {
        this.onMenuItemClickListener = callBack;
    }

    /**
     * 设置对话框标题内容
     * @param titleText
     */
    public void setTitleText(String titleText){
        this.titleTextStr = titleText;
    }

    /**
     * 设置菜单颜色
     * @param menuColor
     */
    public void setMenuColor(int menuColor){
        this.menuColor = menuColor;
    }


    @SuppressLint("WrongConstant")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialog);
    }


    @Override
    public void onStart() {
        super.onStart();
        mWindow.setGravity(Gravity.BOTTOM);
        mWindow.setWindowAnimations(R.style.BottomAnimation);
        mWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT );
    }

    @Override
    protected int setLayoutId() {
        return R.layout.dialog_bottom_menu;
    }


    @Override
    protected void initView(View view) {
        titleText = view.findViewById(R.id.title_text);
        titleSplitLine = view.findViewById(R.id.splint_line);
        if(!TextUtils.isEmpty(titleTextStr)){
            titleText.setVisibility(View.VISIBLE);
            titleSplitLine.setVisibility(View.VISIBLE);
            titleText.setText(titleTextStr);
        }
        recyclerView = view.findViewById(R.id.recyclerView);
        view.findViewById(R.id.cancel_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //取消
                dismiss();
            }
        });
        if (commonAdapter == null) {
            recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.addItemDecoration(new LinearDividerItemDecoration(context,LinearLayoutManager.VERTICAL,
                    1, StringUtils.getResourceColor(R.color.splint_line_color)));
            commonAdapter = new BottomPopupListAdapter(context, menuItemList,menuColor);
            recyclerView.setAdapter(commonAdapter);
            commonAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                    BottomMenuItem menuItem = commonAdapter.getItem(position);
                    if(menuItem != null){
                        if(onMenuItemClickListener != null){
                            onMenuItemClickListener.callback(menuItem.getItemMenuId(),menuItem);
                        }
                        dismiss();
                    }
                }
            });
        } else {
            commonAdapter.setNewData(menuItemList);
        }
    }
}

