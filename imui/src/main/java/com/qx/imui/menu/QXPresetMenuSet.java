package com.qx.imui.menu;

import com.qx.imui.QXIMKit;
import com.qx.imui.R;
import com.qx.imui.bean.menu.QXMenu;
import com.qx.imui.bean.menu.QXMenuType;
import com.qx.message.Message;

public class QXPresetMenuSet {


    /**
     * 复制
     */
    public static final QXMenu QX_MENU_COPY = new QXMenu.QXMenuBuilder().setType(QXMenuType.MENU_TYPE_COPY).
            setIcon(R.drawable.imui_ic_msg_pop_copy).
            setText(R.string.qx_msg_pop_copy).
            build();
    /**
     * 回复
     */
    public static final QXMenu QX_MENU_REPLY = new QXMenu.QXMenuBuilder().setType(QXMenuType.MENU_TYPE_REPLY).
            setIcon(R.drawable.imui_ic_msg_pop_reply).
            setText(R.string.qx_msg_pop_reply).
            build();
    /**
     * 转发
     */
    public static final QXMenu QX_MENU_FORWARD = new QXMenu.QXMenuBuilder().setType(QXMenuType.MENU_TYPE_FORWARD).
            setIcon(R.drawable.imui_ic_msg_pop_retransmission).
            setText(R.string.qx_msg_pop_retransmission).
            build();

    /**
     * 收藏
     */
    public static final QXMenu QX_MENU_FAVORITE = new QXMenu.QXMenuBuilder().setType(QXMenuType.MENU_TYPE_FAVORITE).
            setIcon(R.drawable.imui_ic_msg_pop_favorite).
            setText(R.string.qx_msg_pop_favorite).
            build();

    /**
     * 多选
     */
    public static final QXMenu QX_MENU_CHECK = new QXMenu.QXMenuBuilder().setType(QXMenuType.MENU_TYPE_CHECK).
            setIcon(R.drawable.imui_ic_msg_pop_check).
            setText(R.string.qx_msg_pop_check).
            build();

    /**
     * 撤回
     */
    public static final QXMenu QX_MENU_RECALL = new QXMenu.QXMenuBuilder().setType(QXMenuType.MENU_TYPE_RECALL).
            setIcon(R.drawable.imui_ic_msg_pop_recall).
            setText(R.string.qx_msg_pop_recall).
            setFilter(new QXMenu.MenuFilter() {
                @Override
                public boolean filter(Message message) {
                    // 不可以撤回别人的消息，只能撤回自己两分钟内的
                    long offset =  System.currentTimeMillis() - message.getTimestamp();
                    boolean isSelf = message.getSenderUserId().equals(QXIMKit.getInstance().getCurUserId());
                    return  !isSelf || offset > 120000;
                }
            }).build();

    /**
     * 删除
     */
    public static final QXMenu QX_MENU_DELETE = new QXMenu.QXMenuBuilder().setType(QXMenuType.MENU_TYPE_DELETE).
            setIcon(R.drawable.imui_ic_msg_pop_delete).
            setText(R.string.qx_msg_pop_delete).
            build();


}
