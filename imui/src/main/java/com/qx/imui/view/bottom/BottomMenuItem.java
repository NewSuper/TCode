package com.qx.imui.view.bottom;

public class BottomMenuItem {
    /**
     * Item菜单ID
     */
    private int itemMenuId;
    /**
     * Item菜单描述
     */
    private String itemMenuDesc;

    public BottomMenuItem(int itemMenuId, String itemMenuDesc){
        this.itemMenuId  = itemMenuId;
        this.itemMenuDesc  = itemMenuDesc;
    }

    public int getItemMenuId() {
        return itemMenuId;
    }

    public void setItemMenuId(int itemMenuId) {
        this.itemMenuId = itemMenuId;
    }

    public String getItemMenuDesc() {
        return itemMenuDesc;
    }

    public void setItemMenuDesc(String itemMenuDesc) {
        this.itemMenuDesc = itemMenuDesc;
    }
}