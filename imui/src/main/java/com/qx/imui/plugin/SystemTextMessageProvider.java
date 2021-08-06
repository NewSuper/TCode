package com.qx.imui.plugin;

import android.view.View;
import android.widget.TextView;

import com.qx.imui.R;
import com.qx.message.CustomMessage;
import com.qx.message.Message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

//自定义系统通知消息
public class SystemTextMessageProvider extends MessageProvider {

    @NotNull
    @Override
    public String getProviderTag() {
        return "SYS:TextMsg";
    }

    @Override
    public int getViewId() {
        return R.layout.layout_custom_system_message;
    }

    @Override
    public void bindView(@Nullable View view, @NotNull Message data) {
        CustomMessage customMessage;
        String messageTitle = "";
        String messageContent = "";
        if(data != null) {
            customMessage = (CustomMessage) data.getMessageContent();
            JSONObject json = null;
            try {
                json = new JSONObject(customMessage.getContent());
                if(json != null){
                    messageTitle = json.getString("title");
                    messageContent = json.getString("content");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            TextView tv_title = view.findViewById(R.id.tv_title);
            TextView tv_content = view.findViewById(R.id.tv_content);
            tv_title.setText(messageTitle);
            tv_content.setText(messageContent);
        }
    }


    /**
     * 是否为通知消息
     * @return
     */
    @Override
    public boolean isNotice() {
        return true;
    }

    @NotNull
    @Override
    public BubbleStyle getBubbleStyle() {
        return null;
    }

    @Override
    public void onClick(@Nullable View view, @NotNull Message data) {

    }
}

