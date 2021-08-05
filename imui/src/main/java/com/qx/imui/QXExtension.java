package com.qx.imui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qx.imlib.qlog.QLog;
import com.qx.imui.emotion.ChatPanelAdapter;
import com.qx.imui.emotion.EmojiBean;
import com.qx.imui.emotion.EmotionLayout;
import com.qx.imui.emotion.IEmotionClickLisntener;
import com.qx.imui.emotion.IExtensionBottomFocusCallBack;
import com.qx.imui.emotion.StickerItem;
import com.qx.imui.plugin.IExtensionClickListener;
import com.qx.imui.plugin.IPluginModule;
import com.qx.imui.plugin.image.FilePlugin;
import com.qx.imui.plugin.image.ImagePlugin;
import com.qx.imui.plugin.image.LocationPlugin;
import com.qx.imui.plugin.image.TakePhotoPlugin;
import com.qx.imui.util.PermissionCheckUtil;
import com.qx.imui.view.RecordButton;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

public class QXExtension {

    private static final String SHARE_PREFERENCE_NAME = "com.chat.ui";
    private static final String SHARE_PREFERENCE_TAG = "soft_input_height";
    private Activity mActivity;
    private LinearLayout mContentLayout;//整体界面布局
    private RelativeLayout mBottomLayout;//底部布局
    private EmotionLayout mEmojiLayout;//表情布局
    private ConstraintLayout mAddLayout;//添加布局
    private AppCompatImageView mSendBtn;//发送按钮
    private View mAddButton;//加号按钮
    private Button mAudioButton;//录音按钮
    private AppCompatImageView mAudioIv;//录音图片

    private EditText mEditText;
    private InputMethodManager mInputManager;
    private SharedPreferences mSp;
    private AppCompatImageView mIvEmoji;

    private String targetId;
    private String conversationType;
    private ChatPanelAdapter mChatPanelAdapter;
    private IExtensionClickListener mExtensionClickListener;

    private int offestY;
    //底部布局获取到焦点的回调
    private IExtensionBottomFocusCallBack bottomFocusCallBack;

    public IExtensionBottomFocusCallBack getBottomFocusCallBack() {
        return bottomFocusCallBack;
    }

    public void setBottomFocusCallBack(IExtensionBottomFocusCallBack bottomFocusCallBack) {
        this.bottomFocusCallBack = bottomFocusCallBack;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getConversationType() {
        return conversationType;
    }

    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }

    public IExtensionClickListener getExtensionClickListener() {
        return mExtensionClickListener;
    }

    public void setExtensionClickListener(IExtensionClickListener mExtensionClickListener) {
        this.mExtensionClickListener = mExtensionClickListener;
    }

    public QXExtension() {

    }

    public static QXExtension with(Activity activity) {
        QXExtension qxExtension = new QXExtension();
        qxExtension.mActivity = activity;
        qxExtension.mInputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        qxExtension.mSp = activity.getSharedPreferences(SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        return qxExtension;
    }

    public Context getContext() {
        return mActivity;
    }

    public QXExtension bindChatPanelAdapter(ChatPanelAdapter adapter) {
        mChatPanelAdapter = adapter;
        return this;
    }

    public QXExtension bindEmojiData(Context context) {
        mEmojiLayout.setemotionClickListener(new IEmotionClickLisntener() {
            @Override
            public void emojiClick(@NotNull EmojiBean emojiBean) {
                mEditText.append(emojiBean.getUnicodeInt());
            }

            @Override
            public void emojiDelClick() {
                mEditText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            }

            @Override
            public void emojiSendClick() {
                if (mActivity != null && mActivity instanceof BaseChatActivity) {
                    BaseChatActivity chatActivity = (BaseChatActivity) mActivity;
                    chatActivity.sendTextMsg();
                }
            }

            @Override
            public void stickerClick(@NotNull Context context, @NotNull StickerItem stickerItem) {
                if (mActivity != null && mActivity instanceof BaseChatActivity) {
                    BaseChatActivity chatActivity = (BaseChatActivity) mActivity;
                    chatActivity.sendGifMessage(stickerItem.getLocalPath(), stickerItem.getOriginUrl(),
                            stickerItem.getWidth(), stickerItem.getHeight(), stickerItem.getIndex());
                }
            }

            @Override
            public void stickerManager(@NotNull Context context) {
                QXContext.getInstance().getConversationEmotionClickListener().managerSticker(context);
            }

            @Override
            public void stickerDel(@NotNull Context context, @NotNull StickerItem sticker) {

            }
        });
        return this;
    }

    public void setInputText(String text) {
        if (mEditText.getText().toString().trim().isEmpty()) {
            mEditText.setText(text);
            mEditText.setSelection(text.length());
        } else {
            mEditText.append(text);
            mEditText.setSelection(mEditText.getText().toString().trim().length());
        }
    }

    //绑定整体界面布局
    public QXExtension bindContentLayout(LinearLayout bottomLayout) {
        mContentLayout = bottomLayout;
        return this;
    }

    private boolean isShowSoft = false;

    public void updateDraft(String draft) {
        mEmojiLayout.getInputStatusListener().inputCallback(draft.trim().length() > 0 ? 1 : 0);
    }

    //绑定输入框
    public QXExtension bindEditText(EditText editText) {
        mEditText = editText;
        mEditText.requestFocus();
        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus &&
                        mEditText.getText().toString().trim().length() > 0) {
                    showSendButton();
                } else {
                    hideSendButton();
                }
                if (hasFocus) {
                    focusStateCallBack();
                }
            }
        });

        mEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusStateCallBack();
            }
        });
        mEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP
                        && mBottomLayout.isShown()) {
                    lockContentHeight();//显示软键盘，锁定内容高度，防止跳闪
                    hideBottomLayout(true);//隐藏表情布局。显示软键盘
                    mIvEmoji.setImageResource(R.drawable.imui_ic_chat_emoji);
                    //软键盘显示后，释放内容高度
                    mEditText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            unlockContentHeightDelayed();
                        }
                    }, 200L);
                    focusStateCallBack();
                }
                return false;
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mEditText.getText().toString().trim().length() > 0) {
                    if (!mEmojiLayout.isShown()) {
                        showSendButton();
                    } else {
                        hideSendButton();
                    }
                } else {
                    mSendBtn.setVisibility(View.GONE);
                    mAddButton.setVisibility(View.VISIBLE);
                    mEmojiLayout.getInputStatusListener().inputCallback(0);
                }
                if (isSoftInputShown()) {
                    isShowSoft = false;
                    focusStateCallBack();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return this;

    }


    //绑定底部布局
    public QXExtension bindBottomLayout(RelativeLayout bottomLayout) {
        mBottomLayout = bottomLayout;
        return this;
    }


    //绑定表情布局
    public QXExtension bindEmojiLayout(EmotionLayout emojiLayout) {
        mEmojiLayout = emojiLayout;
        return this;
    }

    //绑定添加布局
    public QXExtension bindAddLayout(ConstraintLayout addLayout) {
        mAddLayout = addLayout;
        return this;
    }


    //绑定发送按钮
    public QXExtension bindttToSendButton(AppCompatImageView sendbtn) {
        mSendBtn = sendbtn;
        return this;
    }


    //绑定语音按钮点击事件
    public QXExtension bindAudioBtn(RecordButton audioBtn) {
        mAudioButton = audioBtn;
        return this;
    }

    //绑定语音图片点击事件
    public QXExtension bindAudioIv(AppCompatImageView audioIv) {
        mAudioIv = audioIv;
        audioIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAudioButton.isShown()) {
                    hideAudioButton();
                    mEditText.requestFocus();
                    showSoftInput();
                } else {
                    mEditText.clearFocus();
                    showAudioButton();
                    hideEmotionLayout();
                    hideMoreLayout();
                }
            }
        });
        return this;
    }

    /**
     * 9键的录音按钮隐藏
     * 输入框显示
     * 9键的录音按钮 切换为此图
     */
    private void hideAudioButton() {
        mAudioButton.setVisibility(View.GONE);//
        mEditText.setVisibility(View.VISIBLE);//
        mAudioIv.setImageResource(R.drawable.imui_ic_chat_audio_msg);   //
    }


    private void showAudioButton() {
        mAudioButton.setVisibility(View.VISIBLE);
        mEditText.setVisibility(View.GONE);
        mAudioIv.setImageResource(R.drawable.imui_ic_chat_keypad);
        if (mBottomLayout.isShown()) {
            hideBottomLayout(false);
        } else {
            hideSoftInput();
        }
    }


    //绑定表情按钮点击事件
    public QXExtension bindToEmojiButton(AppCompatImageView emojiBtn) {
        mIvEmoji = emojiBtn;
        emojiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.clearFocus();
                if (!mEmojiLayout.isShown()) {
                    focusStateCallBack();
                    showSendButton();
                    if (mAddLayout.isShown()) {
                        showEmotionLayout();
                        hideMoreLayout();
                        hideAudioButton();
                        return;
                    }
                } else if (mEmojiLayout.isShown() && !mAddLayout.isShown()) {
                    mIvEmoji.setImageResource(R.drawable.imui_ic_chat_emoji);
                    if (mBottomLayout.isShown()) {
                        lockContentHeight();//显示软件盘时，锁定内容高度，防止跳闪。
                        hideBottomLayout(true);//隐藏表情布局，显示软件盘
                        unlockContentHeightDelayed();//软件盘显示后，释放内容高度
                        showSendButton();
                    } else {
                        if (isSoftInputShown()) {//同上
                            lockContentHeight();
                            showBottomLayout();
                            unlockContentHeightDelayed();
                        } else {
                            showBottomLayout();//两者都没显示，直接显示表情布局
                        }
                    }
                    return;
                }
                hideSendButton();
                showEmotionLayout();
                hideMoreLayout();
                hideAudioButton();
                if (mBottomLayout.isShown()) {
                    lockContentHeight();//显示软件盘时，锁定内容高度，防止跳闪。
                    hideBottomLayout(true);//隐藏表情布局，显示软件盘
                    unlockContentHeightDelayed();//软件盘显示后，释放内容高度
                } else {
                    if (isSoftInputShown()) {//同上
                        lockContentHeight();
                        showBottomLayout();
                        unlockContentHeightDelayed();
                    } else {
                        showBottomLayout();//两者都没显示，直接显示表情布局
                    }
                }
            }
        });
        return this;
    }


    //绑定底部加号按钮
    public QXExtension bindToAddButton(View addButton) {
        mAddButton = addButton;
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.clearFocus();//清空输入框焦点
                hideAudioButton();
                //如果整个底部布局在显示中
                if (mBottomLayout.isShown()) {
                    if (mAddLayout.isShown()) {
                        lockContentHeight();//显示软件盘时，锁定内容高度，防止跳闪。
                        hideBottomLayout(true);//隐藏表情布局，显示软件盘
                        unlockContentHeightDelayed();//软件盘显示后，释放内容高度
                    } else {
                        showMoreLayout();
                        hideEmotionLayout();
                    }
                } else {
                    focusStateCallBack();
                    if (isSoftInputShown()) {//同上
                        hideEmotionLayout();
                        showMoreLayout();
                        lockContentHeight();
                        showBottomLayout();
                        unlockContentHeightDelayed();
                    } else {
                        showMoreLayout();
                        hideEmotionLayout();
                        showBottomLayout();//两者都没显示，直接显示表情布局
                    }

                }
            }
        });
        return this;
    }

    /**
     * 隐藏整个添加布局
     */
    private void hideMoreLayout() {
        mAddLayout.setVisibility(View.GONE);
    }

    /**
     * 显示整个添加布局
     */
    private void showMoreLayout() {
        mAddLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 显示蓝色箭头发送按钮
     */
    private void showSendButton() {
        mSendBtn.setVisibility(View.VISIBLE);
        mAddButton.setVisibility(View.GONE);
    }

    /**
     * 隐藏蓝色箭头发送按钮
     */
    private void hideSendButton() {
        mSendBtn.setVisibility(View.GONE);
        mAddButton.setVisibility(View.VISIBLE);
    }


    /**
     * 隐藏底部布局
     *
     * @param showSoftInput 是否显示软件盘
     */
    public void hideBottomLayout(boolean showSoftInput) {
        if (mBottomLayout.isShown()) {
            mBottomLayout.setVisibility(View.GONE);
            if (showSoftInput) {
                showSoftInput();
            }
        }
    }

    private int oldHeight = 0;


    private void showBottomLayout() {
        try {
            lock.lock();
            int softInputHeight = calcKeyBoradHeight("showBottomLayout");
            if (softInputHeight == 0) {
                softInputHeight = mSp.getInt(SHARE_PREFERENCE_TAG, dip2Px(270));
                if (softInputHeight == 0) {
                    keyboardHeight = dip2Px(270);
                    softInputHeight = keyboardHeight;
                }
                oldHeight = softInputHeight;
            }
            hideSoftInput();
            mBottomLayout.getLayoutParams().height = softInputHeight;
            QLog.d("QXExtension", "showBottomLayout " + softInputHeight + ",oldHeight:" + oldHeight);
            mBottomLayout.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 显示表情面板
     */
    private void showEmotionLayout() {
        mEmojiLayout.setVisibility(View.VISIBLE);
        mIvEmoji.setImageResource(R.drawable.imui_ic_chat_keypad);
    }

    /**
     * 隐藏表情面板
     */
    private void hideEmotionLayout() {
        mEmojiLayout.setVisibility(View.GONE);
        mIvEmoji.setImageResource(R.drawable.imui_ic_chat_emoji);
    }


    /**
     * 是否显示软件盘
     *
     * @return
     */
    public boolean isSoftInputShown() {
        return getSupportSoftInputHeight() != 0;
    }

    public int dip2Px(int dip) {
        try {
            lock.lock();
            float density = mActivity.getApplicationContext().getResources().getDisplayMetrics().density;
            int px = (int) (dip * density + 0.5f);
            return px;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return 0;

    }


    /**
     * 隐藏软件盘
     */
    public void hideSoftInput() {
        isShowSoft = false;
        mInputManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }


    private ReentrantLock lock = new ReentrantLock();

    //获取软键盘的高度
    private int getSupportSoftInputHeight() {
        try {
            lock.lock();
            Rect r = new Rect();
            //decorview 是window最顶层view,可以从window中getdecorviewA获取
            mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
            int screenHeight = mActivity.getWindow().getDecorView().getRootView().getHeight();
            int softInputHeight = screenHeight - r.bottom;
            if (isNavigationBarExist(mActivity)) {
                softInputHeight = softInputHeight - getNavigationHeight(mActivity);
            }
            return softInputHeight;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return 0;
    }

    public static int keyboardHeight = 0;
    boolean isVisiableForLast = false;
    ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = null;

    public void addOnSoftKeyBoardVisibleListener() {
        if (keyboardHeight > 0) {
            return;
        }
        final View decorView = mActivity.getWindow().getDecorView();
        onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                calcKeyBoradHeight("OnGlobalLayoutListener");
            }
        };
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    private int calcKeyBoradHeight(String from) {
        try {
            lock.lock();
            final View decorView = mActivity.getWindow().getDecorView();
            Rect rect = new Rect();
            decorView.getWindowVisibleDisplayFrame(rect);
            //计算出可见屏幕的高度
            int displayHight = rect.bottom - rect.top;
            //获得屏幕整体的高度
            int hight = decorView.getHeight();
            boolean visible = (double) displayHight / hight < 0.8;
            int statusBarHeight = 0;
            int resourceId = mActivity.getApplicationContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = mActivity.getApplicationContext().getResources().getDimensionPixelSize(resourceId);
            }
            int navHeight = 0;
            if (isNavigationBarExist(mActivity)) {
                navHeight = getNavigationHeight(mActivity);
            }
            QLog.i("QXExtension", from + ", navHeight:" + navHeight);
            if (visible && visible != isVisiableForLast) {
                //获得键盘高度
                keyboardHeight = hight - displayHight - statusBarHeight - navHeight;
                mSp.edit().putInt(SHARE_PREFERENCE_TAG, keyboardHeight).apply();
                QLog.i("QXExtension", from + ", calcKeyBoradHeight:" + keyboardHeight + ",hight：" + hight
                        + ",displayHight:" + displayHight + ",statusBarHeight:" + statusBarHeight + ",navHeight:" + navHeight);
                if (oldHeight > 0) {
                    offestY = keyboardHeight - oldHeight;
                    mContentLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mContentLayout.scrollBy(0, offestY);
                            QLog.i("QXExtension", from + ", calcKeyBoradHeight scrollBy:" + offestY);
                            oldHeight = 0;
                            isShowSoft = false;
                            focusStateCallBack();
                        }
                    }, 10);
                }
            } else {
                mContentLayout.scrollTo(0, 0);
            }
            isVisiableForLast = visible;
            return keyboardHeight;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return 0;
    }

    /**
     * 显示软键盘
     */
    public void showSoftInput() {
        mEditText.requestFocus();
        mEditText.post(new Runnable() {
            @Override
            public void run() {
                mInputManager.showSoftInput(mEditText, 0);
            }
        });
    }

    /**
     * 锁定内容高度，防止跳闪
     */
    private void lockContentHeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mContentLayout.getLayoutParams();
        params.height = mContentLayout.getHeight();
        params.weight = 0.0F;
    }

    /**
     * 释放被锁定的内容高度
     */
    public void unlockContentHeightDelayed() {
        mEditText.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((LinearLayout.LayoutParams) mContentLayout.getLayoutParams()).weight = 1.0F;
            }
        }, 200L);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int getSoftButtonsBarHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        mActivity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        } else {
            return 0;
        }
    }


    private static final String NAVIGATION = "navigationBarBackground";

    // 该方法需要在View完全被绘制出来之后调用，否则判断不了
    //在比如 onWindowFocusChanged（）方法中可以得到正确的结果
    public boolean isNavigationBarExist(@NonNull Activity activity) {
        ViewGroup vp = (ViewGroup) activity.getWindow().getDecorView();
        if (vp != null) {
            for (int i = 0; i < vp.getChildCount(); i++) {
                vp.getChildAt(i).getContext().getPackageName();
                if (vp.getChildAt(i).getId() != View.NO_ID &&
                        NAVIGATION.equals(activity.getResources().getResourceEntryName(vp.getChildAt(i).getId()))) {
                    return true;
                }
            }
        }
        return false;
    }


    public int getNavigationHeight(Context activity) {
        if (activity == null) {
            return 0;
        }
        Resources resources = activity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height",
                "dimen", "android");
        int height = 0;
        if (resourceId > 0) {
            //获取NavigationBar的高度
            height = resources.getDimensionPixelSize(resourceId);
        }
        return height;
    }

    public void onActivityForResult(int requestCode, int resultCode, Intent data) {
        int position = (requestCode >> 8) - 1;
        int reqCode = requestCode & 255;
        IPluginModule pluginModule = this.mChatPanelAdapter.getPluginModule(position);
        try {
            if (pluginModule != null) {
                if (mExtensionClickListener != null && resultCode == Activity.RESULT_OK && data != null) {
                    if (pluginModule instanceof ImagePlugin || pluginModule instanceof TakePhotoPlugin) {
                        boolean sendOrigin = data.getBooleanExtra("sendOrigin", false);
                        String mediaList = data.getStringExtra("android.intent.extra.RETURN_RESULT");
                        Gson gson = new Gson();
                        Type entityType = (new TypeToken<LinkedHashMap<String, Integer>>() {
                        }).getType();
                        LinkedHashMap<String, Integer> mLinkedHashMap = (LinkedHashMap) gson.fromJson(mediaList, entityType);
                        this.mExtensionClickListener.onImageResult(mLinkedHashMap, sendOrigin);
                    } else if (pluginModule instanceof LocationPlugin) {
                        this.mExtensionClickListener.onLocationResult(data);
                    } else if (pluginModule instanceof FilePlugin) {
                        this.mExtensionClickListener.onFileReuslt(data);
                    }
                }
                pluginModule.onActivityResult(reqCode, resultCode, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startActivityForPluginResult(Intent intent, int requestCode, IPluginModule pluginModule) {
        if ((requestCode & -256) != 0) {
            throw new IllegalArgumentException("requestCode must less than 256.");
        } else {
            int position = this.mChatPanelAdapter.getPluginPosition(pluginModule);
            int request = (position + 1 << 8) + (requestCode & 255);
            this.mActivity.startActivityForResult(intent, request);
        }
    }

    public void requestPermissionForPluginResult(String[] permissions, int requestCode, IPluginModule pluginModule) {
        if ((requestCode & -256) != 0) {
            throw new IllegalArgumentException("requestCode must less than 256");
        } else {
            int position = this.mChatPanelAdapter.getPluginPosition(pluginModule);
            int req = (position + 1 << 8) + (requestCode & 255);
            PermissionCheckUtil.requestPermissions(mActivity, permissions, req);
        }
    }

    public void showRequestPermissionFailedAlter(String content) {
        PermissionCheckUtil.showRequestPermissionFailedAlter(mActivity, content);
    }

    /**
     * 底部获取到焦点回调UI处理把布局推上去并滚动到底部
     */
    private void focusStateCallBack() {
        if (bottomFocusCallBack != null && !isShowSoft) {
            bottomFocusCallBack.onBottomFocusCallBack();
            isShowSoft = true;
        }
    }

    /**
     * 关闭底部面板+软键盘
     */
    public void outSideClickCloseKeyboard() {
        mEditText.clearFocus();
        hideEmotionLayout();
        hideMoreLayout();
        hideSoftInput();
        hideBottomLayout(false);
    }

    /**
     * 如果刚才是发送录音消息，回复时需要还原底部输入模式，否则底部回复内容会不显示
     */
    public void resetBottomInputModel() {
        if (mAudioButton.isShown()) {
            hideAudioButton();
            mEditText.requestFocus();
        }
    }
}
