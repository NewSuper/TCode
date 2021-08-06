package com.qx.imui.menu;

import com.qx.message.Message;

public class MenuActionType {

    /**
     * 菜单类型
     */
    private MenuType menuType;

    /**
     * 菜单优先级（默认优先级==1）
     */
    private int priority;

    /**
     * 菜单对应的标题
     */
    private int textResource;

    /**
     * 菜单对应的图片
     */
    private int icon;

    /**
     * 菜单唯一标识（自定义类型的菜单tag是唯一的不可以重复）
     */
    private String custom_tag;

    /**
     * 过滤
     */
    public Filter listener;

    /**
     * 过滤条件
     */
    public interface Filter {
        boolean filter(Message message);
    }



    /**
     * 内置菜单
     *
     * @param menuType
     * @param textResource
     * @param icon
     */
    public MenuActionType(MenuType menuType, int textResource, int icon) {
        this.menuType = menuType;
        this.priority = 1;
        this.textResource = textResource;
        this.icon = icon;
    }

    /**
     * 自定义菜单
     *
     * @param custom_tag
     * @param textResource
     * @param icon
     */
    public MenuActionType(String custom_tag, int textResource, int icon,Filter listener) {
        this.menuType = MenuType.CUSTOM;
        this.custom_tag = custom_tag;
        this.textResource = textResource;
        this.icon = icon;
        this.listener = listener;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public void setMenuType(MenuType menuType) {
        this.menuType = menuType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getTextResource() {
        return textResource;
    }

    public void setTextResource(int textResource) {
        this.textResource = textResource;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getCustom_tag() {
        return custom_tag;
    }

    public void setCustom_tag(String custom_tag) {
        this.custom_tag = custom_tag;
    }

    public Filter getListener() {
        return listener;
    }
}

