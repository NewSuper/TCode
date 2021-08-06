package com.qx.imui.menu;

import com.qx.imui.bean.menu.QXMenu;
import com.qx.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QXMenuManager {
    private HashMap<String, List<QXMenu>> menuHashMap = new HashMap<>();

    public static QXMenuManager getInstance() {
        return Holder.instance;
    }

    public void putMenuList(String key, List<QXMenu> list) {
        menuHashMap.put(key, list);
    }

    public List<QXMenu> getMenuList(Message message) {
        List<QXMenu> temp = menuHashMap.get(message.getMessageType());
        List<QXMenu> list = new ArrayList<>();
        if (temp == null)
            return list;
        list.addAll(temp);
        if (list != null && list.size() > 0) {
            //处理菜单过滤
            for (QXMenu menu : temp) {
                if (menu.getFilter() != null && menu.getFilter().filter(message)) {
                    list.remove(menu);
                }
            }
        }
        return list;
    }

    public void addMenuToList(String key, QXMenu menu) {
        List<QXMenu> list = menuHashMap.get(key);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(menu);
        menuHashMap.put(key, list);
    }

    public void addMenuToList(int index, String key, QXMenu menu) {
        List<QXMenu> list = menuHashMap.get(key);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(index, menu);
        menuHashMap.put(key, list);
    }

    static class Holder {
        static final QXMenuManager instance = new QXMenuManager();
    }
}