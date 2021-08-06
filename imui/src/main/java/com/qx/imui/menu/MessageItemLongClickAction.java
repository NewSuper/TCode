package com.qx.imui.menu;


public class MessageItemLongClickAction {

    public MenuActionType menuActionType;


    private MessageItemLongClickAction(MenuActionType menuActionType) {
        this.menuActionType =  menuActionType;
    }

    public MenuActionType getMenuActionType() {
        return menuActionType;
    }

    public void setMenuActionType(MenuActionType menuActionType) {
        this.menuActionType = menuActionType;
    }


    public static class Builder {
        private MenuActionType menuActionType;
        private int priority;

        public Builder() {
        }

        public MessageItemLongClickAction.Builder MenuActionType(MenuActionType menuActionType) {
            this.menuActionType = menuActionType;
            return this;
        }

        public MessageItemLongClickAction.Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public MessageItemLongClickAction build() {
            MessageItemLongClickAction action = new MessageItemLongClickAction(this.menuActionType);
            return action;
        }
    }
}
