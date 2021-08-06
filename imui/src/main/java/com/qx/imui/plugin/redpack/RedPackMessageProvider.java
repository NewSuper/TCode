package com.qx.imui.plugin.redpack;

import android.view.View;
import android.widget.TextView;

import com.qx.imui.R;
import com.qx.imui.plugin.MessageProvider;
import com.qx.message.CustomMessage;
import com.qx.message.Message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RedPackMessageProvider extends MessageProvider {

    @NotNull
    @Override
    public String getProviderTag() {
        return "QX:cccc";
    }

    @Override
    public int getViewId() {
        return R.layout.layout_custom;
    }

    @Override
    public void bindView(@Nullable View view, @NotNull Message data) {
        CustomMessage customMessage;
        if(data != null) {
            customMessage = (CustomMessage) data.getMessageContent();
            TextView tv_title = view.findViewById(R.id.tv_title);
            tv_title.setText(customMessage.getContent());
        }
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
