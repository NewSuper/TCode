package com.qx.imui.bean.menu;

import com.qx.imui.R;
import com.qx.message.Message;

public class QXMenu {
    /**
     * 标签，用于区分自定义菜单
     */
    private String tag;
    private int icon;
    private int text;
    private QXMenuAction action;
    private QXMenuType type;
    private MenuFilter filter;

    public QXMenu(QXMenuBuilder builder) {
        this.tag = builder.tag;
        this.icon = builder.icon;
        this.text = builder.text;
        this.action = builder.action;
        this.type = builder.type;
        this.filter = builder.filter;
    }

    public String getTag() {
        return tag;
    }

    public int getIcon() {
        return icon;
    }

    public int getText() {
        return text;
    }

    public QXMenuAction getAction() {
        return action;
    }

    public QXMenuType getType() {
        return type;
    }

    public MenuFilter getFilter() {
        return filter;
    }

    public static class QXMenuBuilder {
        private String tag;
        private int icon = R.drawable.imui_ic_msg_pop_copy;
        private int text = R.string.qx_msg_pop_copy;
        private QXMenuAction action;
        private QXMenuType type;
        private MenuFilter filter;

        public QXMenuBuilder() {
        }

        public QXMenuBuilder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public QXMenuBuilder setType(QXMenuType type) {
            this.type = type;
            return this;
        }

        public QXMenuBuilder setIcon(int icon) {
            this.icon = icon;
            return this;
        }

        public QXMenuBuilder setText(int text) {
            this.text = text;
            return this;
        }

        public QXMenuBuilder setAction(QXMenuAction action) {
            this.action = action;
            return this;
        }

        public QXMenuBuilder setFilter(MenuFilter filter) {
            this.filter = filter;
            return this;
        }

        public QXMenu build() {
            return new QXMenu(this);
        }
    }

    public interface MenuFilter {
        boolean filter(Message message);
    }
}

