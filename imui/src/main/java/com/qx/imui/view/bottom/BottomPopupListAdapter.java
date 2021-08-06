package com.qx.imui.view.bottom;


import android.content.Context;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.qx.imui.R;
import com.qx.imui.util.StringUtils;

import java.util.List;

public class BottomPopupListAdapter extends BaseQuickAdapter<BottomMenuItem, BaseViewHolder> {
    private Context context;

    //菜单颜色
    private int menuColor = 0;

    public BottomPopupListAdapter(Context context, List data) {
        super(R.layout.dialog_adapter_bottom_item_layout, data);
        this.context = context;
    }

    public BottomPopupListAdapter(Context context, List data,int menuColor) {
        super(R.layout.dialog_adapter_bottom_item_layout, data);
        this.context = context;
        this.menuColor = menuColor;
    }

    @Override
    protected void convert(final BaseViewHolder helper, final BottomMenuItem menuItem) {
        if (menuItem == null) {
            return;
        }
        TextView itemDesc = helper.getView(R.id.item_desc);
        itemDesc.setText(menuItem.getItemMenuDesc());
        if(menuColor > 0){
            itemDesc.setTextColor(StringUtils.getResourceColor(menuColor));
        }
    }

}
