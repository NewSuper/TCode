package com.qx.imui.emotion;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.qx.imui.R;

import java.util.List;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class EmotionPageAdapter extends PagerAdapter {

    private List<EmojiBean> mListEmoji;
    private IEmotionClickLisntener clickLisntener;

    public IEmotionClickLisntener getClickLisntener() {
        return clickLisntener;
    }

    private EmojiAdapter entranceAdapter;
    private StickerAdapter stickerAdapter;

    private View delView;
    private View sendView;
    private boolean isSend = false;

    public void setClickLisntener(IEmotionClickLisntener clickLisntener) {
        this.clickLisntener = clickLisntener;
    }

    public EmotionPageAdapter(Context context) {
        mListEmoji = EmojiDao.getInstance(context).getEmojiBean();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view;
        Context context = container.getContext();
        if (position == 0) {
            // emoji
            view = initEmojiView(context);
        } else {
            // sticker
            view = LayoutInflater.from(context).inflate(R.layout.imui_plugin_emoji_recyclerview, container, false);
            RecyclerView recyclerView = view.findViewById(R.id.rvSticker);
            recyclerView.setLayoutManager(new GridLayoutManager(context, 4));
            StickerAdapter adapter = new StickerAdapter(R.layout.imui_plugin_emoji_add_favorite);
            List<StickerItem> data;
            if (position == 2) {
                // 系統表情
//                data = StickerManager.getInstance().getDefaultStickerList();
                StickerManager.getInstance().asyncDefaultStickerList(new Function1<List<StickerItem>, Unit>() {
                    @Override
                    public Unit invoke(List<StickerItem> stickerItems) {
                        view.post(() -> {
                            adapter.getData().addAll(stickerItems);
                            recyclerView.setAdapter(adapter);
                        });
                        return null;
                    }
                });
            } else {
                stickerAdapter = adapter;
//                data = StickerManager.getInstance().getUserStickerList();
                StickerManager.getInstance().asyncUserStickerList(new Function1<List<StickerItem>, Unit>() {
                    @Override
                    public Unit invoke(List<StickerItem> stickerItems) {
                        view.post(() -> {
                            adapter.getData().addAll(stickerItems);
                            recyclerView.setAdapter(adapter);
                        });
                        return null;
                    }
                });
            }

            if (position == 1) {
                adapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                        if (position != 0) {
                            StickerItem stickerItem = (StickerItem) adapter.getData().get(position);
                            StickerManager.getInstance().onLongClickSticker(view.getContext(),stickerItem);
                        }
                        return true;
                    }
                });
            }
            adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(BaseQuickAdapter adapter, View view, int itemPosi) {
                    if (clickLisntener != null) {
                        if (position == 1 && itemPosi == 0) {
                            clickLisntener.stickerManager(view.getContext());
                        } else {
                            StickerItem stickerItem = (StickerItem) adapter.getData().get(itemPosi);
                            if (stickerItem.getWidth() == null || stickerItem.getHeight() == null) {
                                // 图片还没下载好
                                return;
                            }
                            clickLisntener.stickerClick(view.getContext(), stickerItem);
                        }

                    }
                }
            });
        }
        container.addView(view);
        return view;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    private View initEmojiView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.imui_plugin_emoji_system, null);
        delView = view.findViewById(R.id.ivEmojiDel);
        sendView = view.findViewById(R.id.ivEmojiSend);
        RecyclerView recyclerView = view.findViewById(R.id.rvEmoji);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 8));
        entranceAdapter = new EmojiAdapter(mListEmoji, 0, 0);
        entranceAdapter.addFooterView(LayoutInflater.from(context).inflate(R.layout.imui_plugin_emoji_footer, null));
        recyclerView.setAdapter(entranceAdapter);
        entranceAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (clickLisntener != null) {
                    clickLisntener.emojiClick((EmojiBean) adapter.getData().get(position));
                }
                delView.setSelected(true);
                sendView.setSelected(true);
            }
        });
        delView.setOnClickListener(v -> {
            if (clickLisntener != null) {
                clickLisntener.emojiDelClick();
            }
        });
        sendView.setOnClickListener(v -> {
            if (clickLisntener != null) {
                clickLisntener.emojiSendClick();
            }
        });
        delView.setSelected(isSend);
        sendView.setSelected(isSend);
        return view;
    }

    public void changeInputStatus(boolean status) {
        isSend = status;
        if (delView != null && sendView != null) {
            delView.setSelected(status);
            sendView.setSelected(status);
        }
    }

    public void refreshAddStickerAdater(StickerItem stickerItem) {
        if (stickerAdapter != null) {
            stickerAdapter.addData(1, stickerItem);
            stickerAdapter.notifyDataSetChanged();
        }
    }

    public void refreshDelStickerAdater(StickerItem stickerItem) {
        if (stickerAdapter != null) {
            stickerAdapter.getData().remove(stickerItem);
            stickerAdapter.notifyDataSetChanged();
        }
    }

    public void refreshStickerAdater(List<StickerItem> stickerItems) {
        if (stickerAdapter != null && stickerItems.size() > 0) {
            stickerAdapter.getData().removeAll(stickerItems);
            stickerAdapter.notifyDataSetChanged();
        }
    }

    public void notifyPosition(int from, int to) {
        if (stickerAdapter != null) {
            stickerAdapter.notifyItemMoved(from, to);
        }
    }
}
