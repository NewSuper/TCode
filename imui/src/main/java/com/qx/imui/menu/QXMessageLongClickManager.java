package com.qx.imui.menu;

import android.content.Context;

import com.qx.imui.R;
import com.qx.imui.util.StringUtils;
import com.qx.message.Message;
import com.qx.message.MessageType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class QXMessageLongClickManager {

    //当前内置消息菜单列表 <消息类型,菜单列表>
    private HashMap<String, List<MessageItemLongClickAction>> messageMenuActionList = new HashMap<>();

    //内置菜单移除列表 <消息类型,菜单类型列表>
    private HashMap<String, List<MenuType>> removeMenuList = new HashMap<>();

    //内置菜单移除项 <消息类型>
    private List<String> removeMessageType = new ArrayList<>();

    //菜单最大个数限制
    private static final int MAX_MENU_COUNT = 10;

    //外部事件回调处理
    private HashMap<MenuType,QXMessageLongListener> qxMessageLongListener = new HashMap<>();

    //自定义的菜单临时记录列表
    private HashMap<String,List<MenuActionType>> tempCustomActions = new HashMap<>();

    public interface QXMessageLongListener{
        boolean messageOnLongCallBack(Context context, MenuActionType menuType, Message message);
    }

    /**
     * 获取设置的事件
     * @return
     */
    public QXMessageLongListener getQxMessageLongListener(MenuType menuType) {
        if(qxMessageLongListener.containsKey(menuType)){
            return qxMessageLongListener.get(menuType);
        }
        return null;
    }

    /**
     * 内置的事件处理
     * @param menuType
     * @param qxMessageLongListener
     */
    public void addQxMessageLongListener(MenuType menuType,QXMessageLongListener qxMessageLongListener) {
        this.qxMessageLongListener.put(menuType,qxMessageLongListener);
    }

    /**
     * 内置的事件处理
     * @param messageType
     * @param qxMessageLongListener
     */
    public void addQxMessageCustomLongListener(String messageType, QXMessageLongListener qxMessageLongListener, MenuActionType... custonAction) {
        this.qxMessageLongListener.put(MenuType.CUSTOM,qxMessageLongListener);

        List<MenuActionType> menusList = new ArrayList<>();
        if(tempCustomActions.containsKey(messageType)){
            if(tempCustomActions.get(messageType) != null && tempCustomActions.get(messageType).size() > 0){
                menusList = tempCustomActions.get(messageType);
            }
            menusList.addAll(Arrays.asList(custonAction));
        }else{
            menusList = Arrays.asList(custonAction);
        }
        tempCustomActions.put(messageType,menusList);
    }

    private QXMessageLongClickManager() {

    }

    private static class Holder {
        static QXMessageLongClickManager instance = new QXMessageLongClickManager();

        private Holder() {

        }
    }

    public static QXMessageLongClickManager getInstance() {
        return QXMessageLongClickManager.Holder.instance;
    }


    /**
     * 创建默认菜单
     *
     * @param menuType
     * @param
     * @param icon
     * @return
     */
    private MenuActionType createMenu(MenuType menuType, int textResource, int icon) {
        return new MenuActionType(menuType, textResource, icon);
    }

    /**
     * 构建[回复]菜单
     *
     * @return
     */
    private MessageItemLongClickAction buildReplyAtion() {
        MenuActionType menuActionType = createMenu(MenuType.REPLY, R.string.qx_msg_pop_reply, R.drawable.imui_ic_msg_pop_reply);
        return new MessageItemLongClickAction.Builder().MenuActionType(menuActionType).build();
    }

    /**
     * 构建[转发]菜单
     *
     * @return
     */
    private MessageItemLongClickAction buildForwardAtion() {
        MenuActionType menuActionType = createMenu(MenuType.FORWARD, R.string.qx_msg_pop_retransmission, R.drawable.imui_ic_msg_pop_retransmission);
        return new MessageItemLongClickAction.Builder().MenuActionType(menuActionType).build();
    }

    /**
     * 构建[收藏]菜单
     *
     * @return
     */
    private MessageItemLongClickAction buildCollectionAtion() {
        MenuActionType menuActionType = createMenu(MenuType.COLLECTION, R.string.qx_msg_pop_favorite, R.drawable.imui_ic_msg_pop_favorite);
        return new MessageItemLongClickAction.Builder().MenuActionType(menuActionType).build();
    }

    /**
     * 构建[多选]菜单
     *
     * @return
     */
    private MessageItemLongClickAction buildMuitChoiceAtion() {
        MenuActionType menuActionType = createMenu(MenuType.MUIT_CHOICE, R.string.qx_msg_pop_check, R.drawable.imui_ic_msg_pop_check);
        return new MessageItemLongClickAction.Builder().MenuActionType(menuActionType).build();
    }

    /**
     * 构建[撤销]菜单
     *
     * @return
     */
    private MessageItemLongClickAction buildRevokeAtion() {
        MenuActionType menuActionType = createMenu(MenuType.REVOKE, R.string.qx_msg_pop_recall, R.drawable.imui_ic_msg_pop_recall);
        return new MessageItemLongClickAction.Builder().MenuActionType(menuActionType).build();
    }

    /**
     * 构建[删除]菜单
     *
     * @return
     */
    private MessageItemLongClickAction buildDeleteAtion() {
        MenuActionType menuActionType = createMenu(MenuType.DELETE, R.string.qx_msg_pop_delete, R.drawable.imui_ic_msg_pop_delete);
        return new MessageItemLongClickAction.Builder().MenuActionType(menuActionType).build();
    }

//    /**
//     * 构建[添加表情]菜单
//     *
//     * @return
//     */
//    private MessageItemLongClickAction buildAddEmoAtion() {
//        MenuActionType menuActionType = createMenu(MenuType.ADD_EMO, StringUtils.getResourceStr(context,R.string.qx_msg_pop_emoji), R.drawable.imui_ic_msg_pop_emoji);
//        return new MessageItemLongClickAction.Builder().MenuActionType(menuActionType).build();
//    }

    /**
     * 构建[拷贝]菜单
     *
     * @return
     */
    private MessageItemLongClickAction buildCopyAtion() {
        MenuActionType menuActionType = createMenu(MenuType.COPY, R.string.qx_msg_pop_copy, R.drawable.imui_ic_msg_pop_copy);
        return new MessageItemLongClickAction.Builder().MenuActionType(menuActionType).build();
    }

    /**
     * 构建[自定义]菜单
     *
     * @return
     */
    private MessageItemLongClickAction buildCustomAtion(String custom_tag, int textResource, int icon, MenuActionType.Filter listener) {
        MenuActionType menuActionType = new MenuActionType(custom_tag, textResource, icon,listener);
        return new MessageItemLongClickAction.Builder().MenuActionType(menuActionType).build();
    }


    /**
     * 构建默认的列表
     *
     * @param messageType
     * @return
     */
    private List<MessageItemLongClickAction> buildDefaultActionsBy(String messageType) {
        List<MessageItemLongClickAction> actionList = new ArrayList<>();
        if (MessageType.TYPE_TEXT.equals(messageType)) {
            //文本消息->复制
            actionList.add(buildCopyAtion());
        }
//        if (MessageType.TYPE_IMAGE.equals(messageType)) {
//            //图片消息->添加到表情
//            actionList.add(buildAddEmoAtion());
//        }
        if (MessageType.TYPE_TEXT.equals(messageType) || MessageType.TYPE_IMAGE.equals(messageType) || MessageType.TYPE_AUDIO.equals(messageType) || MessageType.TYPE_VIDEO.equals(messageType) || MessageType.TYPE_IMAGE_AND_TEXT.equals(messageType) || MessageType.TYPE_FILE.equals(messageType) || MessageType.TYPE_GEO.equals(messageType) || MessageType.TYPE_REPLY.equals(messageType)) {
            //文本，图片，音频，视频，图文，文件，位置,回复->(回复，转发，收藏，多选，撤销，删除)
            actionList.addAll(buildDefaultActions());
        }
        if (MessageType.TYPE_AUDIO_CALL.equals(messageType) || MessageType.TYPE_VIDEO_CALL.equals(messageType)) {
            //音视频通话消息->添加删除
            actionList.add(buildDeleteAtion());
        }
        return actionList;
    }


    /**
     * 构建默认的通用列表(回复，转发，收藏，多选，撤销，删除)
     *
     * @return
     */
    private List<MessageItemLongClickAction> buildDefaultActions() {
        List<MessageItemLongClickAction> actionList = new ArrayList<>();
        actionList.add(buildReplyAtion());
        actionList.add(buildForwardAtion());
        actionList.add(buildCollectionAtion());
        actionList.add(buildMuitChoiceAtion());
        actionList.add(buildRevokeAtion());
        actionList.add(buildDeleteAtion());
        return actionList;
    }


    /**
     * 初始化SDK内置的菜单列表
     */
    private void initClickActions() {
        messageMenuActionList.clear();
        //文本消息
        messageMenuActionList.put(MessageType.TYPE_TEXT, buildDefaultActionsBy(MessageType.TYPE_TEXT));
        //图片消息
        messageMenuActionList.put(MessageType.TYPE_IMAGE, buildDefaultActionsBy(MessageType.TYPE_IMAGE));
        //音频消息
        messageMenuActionList.put(MessageType.TYPE_AUDIO, buildDefaultActionsBy(MessageType.TYPE_AUDIO));
        //视频消息
        messageMenuActionList.put(MessageType.TYPE_VIDEO, buildDefaultActionsBy(MessageType.TYPE_VIDEO));
        //图文消息
        messageMenuActionList.put(MessageType.TYPE_IMAGE_AND_TEXT, buildDefaultActionsBy(MessageType.TYPE_IMAGE_AND_TEXT));
        //文件消息
        messageMenuActionList.put(MessageType.TYPE_FILE, buildDefaultActionsBy(MessageType.TYPE_FILE));
        //位置消息
        messageMenuActionList.put(MessageType.TYPE_GEO, buildDefaultActionsBy(MessageType.TYPE_GEO));
        //音频通话消息
        messageMenuActionList.put(MessageType.TYPE_AUDIO_CALL, buildDefaultActionsBy(MessageType.TYPE_AUDIO_CALL));
        //视频通话消息
        messageMenuActionList.put(MessageType.TYPE_VIDEO_CALL, buildDefaultActionsBy(MessageType.TYPE_VIDEO_CALL));
        //回复消息(缺失)
        messageMenuActionList.put(MessageType.TYPE_REPLY, buildDefaultActionsBy(MessageType.TYPE_REPLY));
    }


    /**
     * 移除指定消息类型内置的菜单
     */
    public void removeDefaultMenu(String messsageType, MenuType... menuType) {
        List<MenuType> actionList = new ArrayList<>();
        if (menuType != null && menuType.length > 0) {
            for (MenuType menuActionType : menuType) {
                if (menuActionType != null) {
                    actionList.add(menuActionType);
                }
            }
        }
        removeMenuList.put(messsageType, actionList);
    }


    /**
     * 移除指定消息类型全部
     */
    public void removeDefaultAllMenus(String messsageType) {
        if(!removeMessageType.contains(messsageType)){
            removeMessageType.add(messsageType);
        }
    }


    /**
     * 获取当前消息菜单列表
     *
     * @param message  消息
     * @param isRecallable 是否可以撤回
     * @return
     */
    public List<MessageItemLongClickAction> getMessageItemLongClickActions(Message message, boolean isRecallable) {
        if (message == null || StringUtils.isEmpty(message.getMessageType())) {
            return null;
        }
        //消息类型
        String messageType = message.getMessageType();

        //移除指定的内置菜单的全部菜单项
        if(removeMessageType != null && removeMessageType.size() > 0){
            if(removeMessageType.contains(messageType)){
                return null;
            }
        }

        //初始化内置的菜单列表
        initClickActions();

        //菜单列表
        List<MessageItemLongClickAction> tempList = new ArrayList<>();
        //自定义的添加条件
        List<MenuActionType> listMenu = tempCustomActions.get(messageType);
        if(listMenu != null && listMenu.size() > 0){
            for (MenuActionType menuActionType : listMenu) {
                if (menuActionType != null) {
                    boolean isFilter = menuActionType.getListener().filter(message);
                    if(!isFilter){
                        //没有过滤的才添加
                        MessageItemLongClickAction clickAction = buildCustomAtion(menuActionType.getCustom_tag(), menuActionType.getTextResource(), menuActionType.getIcon(),menuActionType.getListener());
                        tempList.add(clickAction);
                    }
                }
            }
        }

        //内置消息处理
        if (messageMenuActionList != null && messageMenuActionList.containsKey(messageType) && messageMenuActionList.get(messageType) != null) {
            tempList.addAll(messageMenuActionList.get(messageType));
            if (tempList != null && tempList.size() > 0) {
                for (int i = 0; i < tempList.size(); i++) {
                    MessageItemLongClickAction messageItemAction = tempList.get(i);
                    if(messageItemAction != null){
                        MenuType menuType = messageItemAction.menuActionType.getMenuType();
                        if(MenuType.REVOKE == menuType && !isRecallable){
                            //不能撤回
                            tempList.remove(i);
                            i--;
                        }
                    }
                }
            }
        }

        //移除内置菜单指定类型的多个菜单项
        if (removeMenuList != null && removeMenuList.containsKey(messageType) && removeMenuList.get(messageType) != null) {
            List<MenuType> removeList = removeMenuList.get(messageType);
            for (int i = 0; i < removeList.size(); i++) {
                MenuType menuType = removeList.get(i);
                if (menuType != null) {
                    for (int m = 0; m < tempList.size(); m++) {
                        MessageItemLongClickAction tempItemMenu = tempList.get(m);
                        if (tempItemMenu != null && menuType == tempItemMenu.menuActionType.getMenuType()) {
                            tempList.remove(m);
                            m--;
                        }
                    }
                }
            }
        }

        if (tempList.size() > MAX_MENU_COUNT) {
            //限制最大个数
            tempList = tempList.subList(0, MAX_MENU_COUNT);
        }
        return tempList;
    }

}
