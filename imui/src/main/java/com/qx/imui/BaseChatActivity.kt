package com.qx.imui

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.mylhyl.acp.Acp
import com.mylhyl.acp.AcpListener
import com.mylhyl.acp.AcpOptions
import com.qx.im.model.ConversationType
import com.qx.im.model.QXGroupUserInfo
import com.qx.im.model.QXUserInfo
import com.qx.im.model.UserInfoCache
import com.qx.imlib.QXIMClient
import com.qx.imlib.qlog.QLog
import com.qx.imlib.utils.LocalMedia
import com.qx.imlib.utils.file.FilePathUtil
import com.qx.imlib.utils.file.FileSizeUtil
import com.qx.imui.activity.*
import com.qx.imui.adapter.*
import com.qx.imui.base.BaseActivity
import com.qx.imui.bean.Member
import com.qx.imui.bean.RecordExtra
import com.qx.imui.bean.TargetItem
import com.qx.imui.bean.menu.QXMenu
import com.qx.imui.bean.menu.QXMenuType
import com.qx.imui.emotion.ChatPanelAdapter
import com.qx.imui.emotion.IExtensionBottomFocusCallBack
import com.qx.imui.util.AudioPlayManager
import com.qx.imui.plugin.rtc.RTCModuleManager
import com.qx.imui.menu.QXMenuManager
import com.qx.imui.plugin.ConvertUtil
import com.qx.imui.plugin.IExtensionClickListener
import com.qx.imui.plugin.IPluginModule
import com.qx.imui.plugin.QXExtensionManager
import com.qx.imui.plugin.image.PicturePreviewActivity
import com.qx.imui.util.*
import com.qx.imui.util.FileUtils.getSuffixName
import com.qx.imui.util.glide.GlideUtil
import com.qx.imui.util.upload.MediaMessageEmitter
import com.qx.imui.util.upload.MediaUtil
import com.qx.imui.view.bottom.BottomMenuItem
import com.qx.imui.view.RetransmissionDialog
import com.qx.imui.view.bottom.BottomMenuDialog
import com.qx.message.*
import com.qx.push.QXNotificationInterface
import kotlinx.android.synthetic.main.imui_activity_chat.*
import kotlinx.android.synthetic.main.imui_common_title_bar.*
import kotlinx.android.synthetic.main.imui_include_add_layout.*
import kotlinx.android.synthetic.main.imui_layout_chat_bottom.*
import kotlinx.android.synthetic.main.imui_layout_chat_multiple_operation.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import top.zibin.luban.OnCompressListener
import java.io.File
import java.util.LinkedHashMap

abstract class BaseChatActivity : BaseActivity(), IExtensionBottomFocusCallBack,
    IExtensionClickListener {
    private val TAG = "BaseChatActivity"
    private var backgroundUrl: String = ""
    private val TEXT_MAX_LENGTH = QXConfigManager.getQxFileConfig().textMessageMaxLength
    private var mMessageReceiptListener: QXIMClient.MessageReceiptListener? = null
    private var mMessageReceiveListener: QXIMClient.OnMessageReceiveListener? = null
    private var toBeReplyMessage: Message? = null //将要被回复的消息

    //聊天对象id，等价于to字段
    var targetId: String? = ""
    var targetName: String? = ""
    var conversation: Conversation? = null
    var conversationId = ""
    var offset = 0
    var pageSize = 50
    var locateMessage: Message? = null

    //消息列表适配器
    lateinit var mMessageAdapter: MessageAdapter
    lateinit var mMessageViewManager: LinearLayoutManager
    var mMessageList = arrayListOf<Message>()

    //聊天面板适配器
    lateinit var mChatPanelAdapter: ChatPanelAdapter
    lateinit var mChatPanelManager: GridLayoutManager
    val FLAG_NEW_MESSAGE = 0
    val FLAG_OFFLINE_MESSAGE = 1
    val FLAG_HISTORY_MESSAGE = 2
    var conversationType: String? = ""
    var mAtToList = ArrayList<Member>()
    var mForwardList = ArrayList<TargetItem>()
    val FORWARD_TYPE_SINGLE = 0//单条转发，实则发送
    val FORWARD_TYPE_MULTI_ONE_BY_ONE = FORWARD_TYPE_SINGLE + 1//多选一条条的转发
    val FORWARD_TYPE_MULTI_COMBINE = FORWARD_TYPE_MULTI_ONE_BY_ONE + 1//多选合并转发
    var mCurrForwordType = FORWARD_TYPE_SINGLE

    val REQUEST_CODE_GALLREY = 0  // 选择相册

    //选择文件
    val REQUEST_CODE_FILE = REQUEST_CODE_GALLREY + 1
    val REQUEST_CODE_GEO = REQUEST_CODE_FILE + 1
    val REQUEST_CODE_CAMERA = 100

    val REQUEST_CODE_ADVANCE = REQUEST_CODE_CAMERA + 1  // 进入聊天详情
    var isMultiple = false
    var checkMsgList = arrayListOf<Message>()
    var tempMsgList = arrayListOf<Message>()
    lateinit var qxExtension: QXExtension

    private var kitReceiver: QXKitReceiver? = null

    init {
        //支持svg尺量图
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getIntentData()
        if (!targetId.isNullOrEmpty()) {
            initIMListener()
            initView()
            loadConversation()
            loadData()
            //处理转发
            handleForward()
            QXNotificationInterface.removeAllNotification(this)
            QXNotificationInterface.removeAllPushNotification(this)
        }
        kitReceiver = QXKitReceiver()
        val filter = IntentFilter()
        filter.addAction("android.intent.action.PHONE_STATE")
        try {
            registerReceiver(this.kitReceiver, filter)
        } catch (e: java.lang.Exception) {
            QLog.e(TAG, "onCreate")
        }
    }

    override fun initView() {
        handlePermission()//申请权限
        initMessageRecyclerView()
        initChatUi()
        initRefreshView()
        initClickListener()
        initReplyView()
        initMessagePopupWindowListener()
    }

    /**
     * PopupWindow事件长按处理
     */
    private fun initMessagePopupWindowListener() {
        MessagePopupWindowUtil.setOnItemClickListener(object :
            MessagePopupWindowUtil.OnItemClickListener {
            override fun onMenuItemClick(menu: QXMenu, message: Message) {
                if (menu.action != null) {
                    menu.action.onAction(this@BaseChatActivity, message)
                    return
                }
                when (menu.type) {
//                    MenuType.ADD_EMO -> {
//                        //添加表情
//                        val imageMessage = message.messageContent as ImageMessage
//                        ThreadPoolUtils.run {
//                            val file = GlideUtil.getCacheFile(this@BaseChatActivity, imageMessage.originUrl)
//                            if (file != null) {
//                                imageMessage.localPath = file.path
//                                runOnUiThread {
//                                    StickerManager.instance.addFavSticker(this@BaseChatActivity, imageMessage.localPath, imageMessage.originUrl, imageMessage.width, imageMessage.height)
//                                }
//                            }
//                        }
//                    }

                    QXMenuType.MENU_TYPE_COPY -> {
                        //复制
                        if (message.messageContent is TextMessage) {
                            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            var text = message.messageContent as TextMessage
                            val mClipData: ClipData = ClipData.newPlainText("Label", text.content)
                            cm.setPrimaryClip(mClipData)
                            ToastUtil.toast(
                                this@BaseChatActivity,
                                getString(R.string.qx_copy_success)
                            )
                        } else {
                            ToastUtil.toast(this@BaseChatActivity, getString(R.string.qx_copy_fail))
                        }
                    }

                    QXMenuType.MENU_TYPE_REPLY -> {
                        //回复
                        toBeReplyMessage = if (message.messageType == MessageType.TYPE_REPLY) {
                            var reply = message?.messageContent as ReplyMessage
                            reply.answer
                        } else {
                            message
                        }
                        var text = UserInfoUtil.getMessageSimpleText(
                            this@BaseChatActivity,
                            message,
                            message.senderUserId
                        )
                        tv_reply.text = text
                        layout_reply.visibility = View.VISIBLE
                        //如果刚才是发送录音消息，回复时需要还原底部输入模式，否则底部回复内容会不显示
                        qxExtension.resetBottomInputModel()
                    }

                    QXMenuType.MENU_TYPE_FORWARD -> {
                        //转发
                        var msg = message.clone() as Message
                        checkMsgList.clear()
                        checkMsgList.add(removeReplyMsg(msg))
                        mCurrForwordType = FORWARD_TYPE_SINGLE
                        QXContext.getInstance().selectTargetProvider?.selectTarget(this@BaseChatActivity)
                    }

                    QXMenuType.MENU_TYPE_FAVORITE -> {
                        //收藏
                        if (message.messageType == MessageType.TYPE_REPLY) {
                            var replyMessage = message.messageContent as ReplyMessage
                            saveToFavorite(listOf(replyMessage.answer))
                        } else {
                            saveToFavorite(listOf(message))
                        }
                    }

                    QXMenuType.MENU_TYPE_CHECK -> {
                        //多选
                        mMessageAdapter.setMultipleCheckable(true)
                        isMultiple = true
                        updateMultipleUI()


                        //在多选之后将已勾选的内容置为false-->等同于初始化
                        for (msg in mMessageList) {
                            msg.isChecked = false
                        }
                    }

                    QXMenuType.MENU_TYPE_RECALL -> {
                        //撤销
                        recallMessage(message)
                    }

                    QXMenuType.MENU_TYPE_DELETE -> {
                        //删除
                        deleteRemoteMessageByMessageId(listOf(message))
                    }
                }
            }
        })
    }

    /**
     * 删除服务器端消息
     */
    private fun deleteRemoteMessageByMessageId(messages: List<Message>) {
        var messageIds = ArrayList<String>()
        for (element in messages) {
            messageIds.add(element.messageId)
        }
        Log.i("DeleteMessage", "远程消息删除" + messageIds.toString());
        QXIMClient.instance!!.deleteRemoteMessageByMessageId(conversationType!!,
            targetId!!,
            messageIds,
            object : QXIMClient.OperationCallback() {

                override fun onSuccess() {
                    //Toast.makeText(this@BaseChatActivity, getString(R.string.qx_message_delete_remote_success), Toast.LENGTH_SHORT).show()
                    Log.i("DeleteMessage", "远程消息删除成功");
                    //删除远程消息成功后，删除本地消息
                    deleteLocalMessage(messages)
                }

                override fun onFailed(error: QXError) {
                    checkMsgList.clear()
                    Log.i("DeleteMessage", "远程消息删除失败" + error.msg);
                    Toast.makeText(
                        this@BaseChatActivity,
                        getString(R.string.qx_delete_fail),
                        Toast.LENGTH_LONG
                    ).show()
                    //Toast.makeText(this@BaseChatActivity, getString(R.string.qx_message_delete_remote_fail), Toast.LENGTH_SHORT).show()
                }

            })
    }

    /**
     * 删除本地消息
     */
    private fun deleteLocalMessage(messages: List<Message>) {
        var messageIds = ArrayList<String>()
        for (element in messages) {
            messageIds.add(element.messageId)
        }
        QXIMClient.instance!!.deleteLocalMessageById(
            messageIds.toTypedArray(),
            object : QXIMClient.OperationCallback() {
                override fun onSuccess() {
                    Toast.makeText(
                        this@BaseChatActivity,
                        getString(R.string.qx_delete_success),
                        Toast.LENGTH_LONG
                    ).show()
                    Log.i("DeleteMessage", "本地消息删除成功");

                    refreshListForDelete(messages)
                    checkMsgList.clear()
                }

                override fun onFailed(error: QXError) {
                    Log.i("DeleteMessage", "本地消息删除失败：" + error.msg);
                    checkMsgList.clear()
                    Toast.makeText(
                        this@BaseChatActivity,
                        getString(R.string.qx_delete_fail),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun recallMessage(message: Message) {
        QXIMClient.instance!!.sendRecall(message, object : QXIMClient.OperationCallback() {

            override fun onSuccess() {
                message.messageType = MessageType.TYPE_RECALL

                runOnUiThread(Runnable {
                    if (toBeReplyMessage != null) {
                        if (toBeReplyMessage == message) {
                            toBeReplyMessage = null
                            layout_reply.visibility = View.GONE
                        }
                    }
                    mMessageAdapter.notifyDataSetChanged()
                })

                QLog.d(TAG, "thread id =" + Thread.currentThread().id)
            }

            override fun onFailed(error: QXError) {
                runOnUiThread {
                    Toast.makeText(
                        this@BaseChatActivity,
                        getString(R.string.qx_recall_fail, error.code, error.msg),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun saveToFavorite(list: List<Message>) {
        //收藏
        var provider = QXContext.getInstance().favoriteProvider
        if (provider == null) {
            ToastUtil.toast(
                this@BaseChatActivity,
                getString(R.string.qx_provider_collect_not_implement)
            )
            return
        }
        var favorites = ConvertUtil.convertToFavorite(list);
        provider.onSave(favorites, object : QXIMKit.QXFavoriteProvider.QXFavoriteCallback {
            override fun onSuccess() {
                ToastUtil.toast(this@BaseChatActivity, getString(R.string.qx_collect_success))
            }

            override fun onFailed(code: Int, msg: String?) {
                ToastUtil.toast(this@BaseChatActivity, getString(R.string.qx_collect_fail))
            }
        })
        checkMsgList.clear()
        hideMultipleView()
    }

    private fun initClickListener() {

        //发送
        btn_send.setOnClickListener {
            btn_send.isClickable = false
            sendTextMsg()
        }
        //返回
        iv_back.setOnClickListener {
            onBackPressed()
        }

        //菜单
        iv_menu.setOnClickListener {
            var provider = QXContext.getInstance().uiEventProvider
            provider?.onChatMenuClick(
                this@BaseChatActivity,
                REQUEST_CODE_ADVANCE,
                conversationType,
                targetId,
                conversationId
            )
        }

        //多选栏
        //转发
        btn_retransmission.setOnClickListener {
            showForwardDialog()
        }
        //收藏
        btn_favorite.setOnClickListener {
            saveToFavorite(checkMsgList)
        }
        //删除
        btn_delete.setOnClickListener {
            confirmDeleteDialog()
        }
    }

    private fun showForwardDialog() {
        var dialog = RetransmissionDialog(this@BaseChatActivity)
        dialog.setOnButtonClickListener(object : RetransmissionDialog.OnButtonClickListener {
            override fun onOneByOne() {
                mCurrForwordType = FORWARD_TYPE_MULTI_ONE_BY_ONE
                QXContext.getInstance().selectTargetProvider?.selectTarget(this@BaseChatActivity)
            }

            override fun onCombine() {
                mCurrForwordType = FORWARD_TYPE_MULTI_COMBINE
                QXContext.getInstance().selectTargetProvider?.selectTarget(this@BaseChatActivity)
            }
        })
        dialog.show()
    }

    /**
     * 确认删除弹出框
     */
    private fun confirmDeleteDialog() {
        val menuItemList: MutableList<BottomMenuItem> = java.util.ArrayList<BottomMenuItem>()
        menuItemList.add(
            BottomMenuItem(
                1,
                StringUtils.getResourceStr(this, R.string.qx_msg_pop_delete)
            )
        )
        val bottomMenuDialog = BottomMenuDialog()
        bottomMenuDialog.setTitleText(
            StringUtils.getResourceStr(
                this,
                R.string.chat_confirm_dialog_title
            )
        )
        bottomMenuDialog.setMenuColor(R.color.delete_confirm_menu_color)
        bottomMenuDialog.setBottomMenuList(this, menuItemList)
        bottomMenuDialog.setOnMenuItemClickListener { itemId, ob ->
            when (itemId) {
                1 -> {
                    hideMultipleView()
                    deleteRemoteMessageByMessageId(checkMsgList)
                }
            }
        }
        bottomMenuDialog.show(supportFragmentManager, BottomMenuDialog::class.java.simpleName)
    }

    open fun initRefreshView() {
        swipe_refresh_layout.setOnRefreshListener {
            offset = mMessageList.size
            loadData()
            swipe_refresh_layout.isRefreshing = false
        }
    }

    private fun loadConversation() {
        if (conversationType.isNullOrEmpty() || targetId.isNullOrEmpty())
            return
        QXIMClient.instance.getConversation(
            conversationType!!,
            targetId!!,
            object : QXIMClient.ResultCallback<Conversation>() {
                override fun onSuccess(data: Conversation) {
                    conversation = data
                    if (conversation != null) {
                        if (!TextUtils.isEmpty(conversation?.background)) {
                            setBackground(conversation?.background!!)
                        }
                        conversationId = conversation!!.conversationId
                        //设置草稿
                        edt_content.setText(conversation!!.draft)
                        edt_content.setSelection(edt_content.text.length)
                        qxExtension.updateDraft(conversation!!.draft)
                        loadUnReadMessage()
                    }
                    if (!TextUtils.isEmpty(backgroundUrl)) {
                        saveBackground(backgroundUrl)
                    }
                }

                override fun onFailed(error: QXError) {
                }

            })
    }

    open fun loadUnReadMessage() {

    }

    open fun loadData() {
//        getMessages()
//        getHistoryMessagesFromLocal()
        initChatBackground()

        Log.e(TAG, "loadData: 查询历史消息:$offset")
        if (offset == 0) {
            //获取本地数据库历史消息。时间戳传-1，则获取当前会话最新的50条历史消息
            QXIMClient.instance.getHistoryMessageFromService(conversationType!!, targetId!!, -1, 0)
            getHistoryMessagesFromLocal()
        } else {
            getHistoryMessagesFromLocal()
        }

        // TODO: 2021/5/15 时间窗逻辑，暂时注释
        /*//获取本地数据库历史消息。时间戳传-1，则获取当前会话最新的50条历史消息
        QXIMClient.instance.getHistoryMessageFromService(conversationType!!, targetId!!, -1, 0)
        //获取后端数据库历史消息
        QXIMClient.instance.getHistoryMessagesFromLocal(conversationId, conversation!!.conversationType, targetId!!, offset, pageSize, object : QXIMClient.ResultCallback<List<Message>>() {
            override fun onSuccess(data: List<Message>) {
                Log.e(TAG, "onSuccess: 查询本地历史消息成功_共多少条：" + data.size)
                if (data.isNotEmpty()) {
                    var isFirstTimeLoad = false
                    if (mMessageList.isNullOrEmpty()) {
                        //如果第一次加载数据
                        isFirstTimeLoad = true
                    }
                    mMessageList.addAll(data)
                    mMessageList.sort()
                    mMessageAdapter.notifyDataSetChanged()
                    if (isFirstTimeLoad) {
                        Log.e(TAG, "onSuccess: =============== 排序1: " + isFirstTimeLoad + "    " + (mMessageList.size - 1))
                        recycler_view_message.scrollToPosition(mMessageList.size - 1)
                    } else {
                        Log.e(TAG, "onSuccess: ================ 排序2： " + isFirstTimeLoad + "    " + (data.size - 1) + "     mMessageList条数为：" + mMessageList.size)
                        recycler_view_message.scrollToPosition(data.size - 1)
                    }
                }
            }

            override fun onFailed(error: QXError) {
                Log.e(TAG, "onFailed: 查询本地历史消息失败")
            }
        })*/
    }

    private fun initChatBackground() {
        var provider = QXContext.getInstance().chatBackgroundProvider
        if (provider == null) {
            ToastUtil.toast(
                this@BaseChatActivity,
                getString(R.string.qx_provider_chat_bg_not_implement)
            )
        }
        var type = when (conversationType) {
            Conversation.Type.TYPE_GROUP -> {
                1
            }
            Conversation.Type.TYPE_PRIVATE -> {
                0
            }
            else -> {
                -1
            }
        }
        provider.getBackground(
            type,
            targetId,
            object : QXIMKit.QXChatBackgroundProvider.QXChatBackgroundCallback {
                override fun onSuccess(imgUrl: String) {
                    QLog.d(TAG, "聊天背景获取成功:$imgUrl")
                    //刷新数据库缓存
                    backgroundUrl = imgUrl
                    saveBackground(backgroundUrl)
                    setBackground(imgUrl)
                }

                override fun onFailed(code: Int, msg: String?) {
                    QLog.d(TAG, "聊天背景获取失败$msg")
                }

            })
    }

    /**
     * 设置DecorView背景图片
     */
    @SuppressLint("CheckResult")
    fun setBackground(path: String) {
        val myOptions: RequestOptions = RequestOptions();
        val dm = resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        myOptions.override(width, height)
        QLog.d(TAG, "setBackground: $path")
        Glide.with(this).load(path)
            .centerCrop()
            .into(imui_chat_iv_background)
//        Glide.with(this@BaseChatActivity)
//                .asBitmap()
//                .load(path)
//                .centerCrop()
//                .apply(myOptions)
//                .into(object : CustomTarget<Bitmap>() {
//                    override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
//                        QLog.d(TAG,"setBackground onResourceReady")
//                        val bitmap: BitmapDrawable = BitmapDrawable(this@BaseChatActivity.resources, bitmap)
//                        if (bitmap != null) {
//                            imui_chat_iv_background?.setBackgroundDrawable(bitmap)
//                        }
//                    }
//
//                    override fun onLoadCleared(placeholder: Drawable?) {
//                        QLog.d(TAG,"setBackground onLoadCleared")
//                    }
//
//                    override fun onLoadFailed(errorDrawable: Drawable?) {
//                        super.onLoadFailed(errorDrawable)
//                        QLog.d(TAG,"setBackground onLoadFailed")
//                    }
//                })
    }

    override fun onBackPressed() {
        if (isMultiple) {
            hideMultipleView()
            mMessageAdapter.setMultipleCheckable(false)
        } else {
            updateDraft()
            //返回事件
            var provider = QXContext.getInstance().uiEventProvider
            provider?.onChatBackClick(
                this@BaseChatActivity,
                conversationType,
                targetId,
                conversationId
            )
            super.onBackPressed()
        }
    }

    open fun updateDraft() {
        var text = edt_content.text.toString().trim()
        //更新草稿
        QXIMClient.instance.updateConversationDraft(
            conversationId,
            text,
            object : QXIMClient.OperationCallback() {
                override fun onSuccess() {
                }

                override fun onFailed(error: QXError) {
                }

            })
    }

    open fun loadUnReadAtToMessage() {
        //发送消息回执
        sendReadReceipt()
    }

    private fun saveBackground(backgroundUrl: String) {
        if (!TextUtils.isEmpty(conversationId)) {
            QXIMKit.getInstance().updateConversationBackground(conversationId, backgroundUrl,
                object : QXIMClient.OperationCallback() {
                    override fun onSuccess() {
                    }

                    override fun onFailed(error: QXError) {
                    }

                })
        }
    }

    //初始化聊天面板
    private fun initChatUi() {
        QXIMKit.getInstance().imuiMessageCallback = sendMessageCallback
        //mBtnAudio
        qxExtension = QXExtension.with(this)
        qxExtension.addOnSoftKeyBoardVisibleListener()
        qxExtension.bindContentLayout(findViewById(R.id.ll_content))
            .bindttToSendButton(findViewById(R.id.btn_send))
            .bindEditText(findViewById(R.id.edt_content))
            .bindBottomLayout(findViewById(R.id.bottom_layout))
            .bindAddLayout(findViewById(R.id.ll_add))
            .bindToAddButton(findViewById(R.id.iv_add))
            .bindEmojiLayout(findViewById(R.id.rl_emotion))
            .bindToEmojiButton(findViewById(R.id.iv_emoj))
            .bindAudioBtn(findViewById(R.id.btn_record))
            .bindAudioIv(findViewById(R.id.iv_audio))
            .bindEmojiData(this)
        qxExtension.extensionClickListener = this
        qxExtension.bottomFocusCallBack = this
        val listPlugins = mutableListOf<IPluginModule>()
        val iterator = QXExtensionManager.instance.getExtensionModules().iterator()
        while (iterator.hasNext()) {
            val extensionModule = iterator.next()
            val listPlugin = extensionModule.getPluginModules(conversationType!!)
            listPlugins.addAll(listPlugin)//添加插件，例如聊天数据库下面的“相册”、“拍摄”、“语音通话”、“视频通话”、“位置”、“文件”等
        }
        qxExtension.targetId = targetId!!
        qxExtension.conversationType = conversationType!!
        mChatPanelManager = GridLayoutManager(this, 4)
        mChatPanelAdapter = ChatPanelAdapter(listPlugins, qxExtension)
        mChatPanelAdapter.setOnChatPanelItemClickListener(object :
            ChatPanelAdapter.OnChatPanelItemClickListener {
            override fun onClick(pluginmodule: IPluginModule, position: Int) {
                //组件适配器Item点击事件
                outSideClickCloseKeyboard()
            }

        })
        qxExtension.bindChatPanelAdapter(mChatPanelAdapter)
        recycler_view_chat_panel.apply {
            setHasFixedSize(true)
            layoutManager = mChatPanelManager
            adapter = mChatPanelAdapter
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    super.getItemOffsets(outRect, view, parent, state)
                    var off_h = DensityUtil.dip2px(this@BaseChatActivity, 15f)
                    var off_v = DensityUtil.dip2px(this@BaseChatActivity, 10f)
                    outRect.set(off_h, off_v, off_h, off_v)
                }
            })
        }
        //底部布局弹出,聊天列表上滑
        recycler_view_message.addOnLayoutChangeListener(View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                recycler_view_message.post(java.lang.Runnable { /*if (mAdapter.getItemCount() > 0) {
                                    mRvChat.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                                }*/
                })
            }
        })

        //触摸空白区域关闭键盘
        recycler_view_message.setOnTouchListener { v, event ->
            outSideClickCloseKeyboard()
            false
        }


        btn_record.setOnFinishedRecordListener { audioPath, time ->
            var uri = Uri.fromFile(File(audioPath))
            sendMediaMessage(MessageType.TYPE_AUDIO, uri)
        }

        //输入文本长度限制
        edt_content.filters = arrayOf(LengthFilter(TEXT_MAX_LENGTH))
    }

    private fun initMessageRecyclerView() {
        mMessageViewManager = LinearLayoutManager(this)
        mMessageAdapter = MessageAdapter(this@BaseChatActivity, mMessageList)

        recycler_view_message.apply {
            setHasFixedSize(true)
            layoutManager = mMessageViewManager
            // 第一种，直接取消动画
            val animator: RecyclerView.ItemAnimator? = recycler_view_message.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
            // 第二种，设置动画时间为0
            recycler_view_message.itemAnimator?.changeDuration = 0
            adapter?.setHasStableIds(true)

            adapter = mMessageAdapter
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    super.getItemOffsets(outRect, view, parent, state)
                    outRect.set(0, 0, 0, DensityUtil.dip2px(this@BaseChatActivity, 13f))
                }
            })
        }
        mMessageAdapter.setOnItemClickListener(object : MessageAdapter.ItemListener {

            /**
             * Item空白区域点击事件
             */
            override fun onItemClick(position: Int) {
                outSideClickCloseKeyboard()
            }

            /**
             * 会话列表点击事件
             */
            override fun onClick(position: Int, v: View) {
                handleMessageClick(mMessageList[position], v, false)
            }

            /**
             * 会话列表内容长按响应事件
             */
            override fun onLongClick(position: Int, v: View): Boolean {
                var message = mMessageList[position]
                //消息是否可以撤回
                var menuList = QXMenuManager.getInstance().getMenuList(message)
                if (menuList != null && menuList.size > 0) {
                    MessagePopupWindowUtil.show(
                        this@BaseChatActivity,
                        message,
                        menuList,
                        layout_title_bar.height.toFloat(),
                        v
                    )
                }
                return true
            }

            override fun onResend(position: Int, v: View) {
                var message = mMessageList[position]
                if (isMediaMessage(message)) {
                    var uri = getMediaMessageUri(message)
                    if (uri == null) {
                        ToastUtil.toast(
                            this@BaseChatActivity,
                            resources.getString(R.string.qx_file_uri_not_found)
                        )
                    } else {
                        sendMediaMessage(message)
                    }
                } else {
                    sendMessage(message)
                }
//                Toast.makeText(this@BaseChatActivity, "重发消息", Toast.LENGTH_SHORT).show()
            }

            override fun onChecked(message: Message, isCheck: Boolean) {
                message.isChecked = isCheck
                //选择消息
                if (isCheck) {
                    if (!checkMsgList.contains(removeReplyMsg(message))) {
                        checkMsgList.add(message)
                    }
                } else {
                    if (checkMsgList.contains(message)) {
                        checkMsgList.remove(message)
                    }
                }
                updateMultipleButton(checkMsgList.isNotEmpty())
            }

            override fun onAvatarClick(friendId: String) {
                var provider = QXContext.getInstance().uiEventProvider
                provider?.onAvatarClick(this@BaseChatActivity, conversationType, targetId, friendId)
            }

            override fun onUserPortraitLongClick(
                context: Context,
                conversationType: String,
                userInfo: QXUserInfo,
                targetId: String
            ) {
                if (userInfo != null) {
                    onAvatarLongClick(userInfo)
                }
            }

            /**
             * 回复消息UI处理
             */
            override fun onReplyMessageClick(message: Message, v: View) {
                //回复和被回复消息
                var replyMessage = message.messageContent as ReplyMessage
                replyMessage.origin.conversationId = message.conversationId
                handleMessageClick(replyMessage.origin, v, true)
            }
        })
    }

    /**
     * 处理消息点击事件
     */
    private fun handleMessageClick(message: Message, v: View, isReply: Boolean) {
        QXIMKit.getInstance().setMessageClicked(message)
        when (message.messageType) {
            MessageType.TYPE_REPLY -> {
                //被回复文本消息->查看文字
                var replyMessage = message.messageContent as ReplyMessage
                if (replyMessage.answer.messageContent is TextMessage) {
                    var textMessage = replyMessage.answer
                    var intent = Intent(this, ChatTextActivity::class.java)
                    intent.putExtra("message", textMessage)
                    startActivity(intent)
                }
            }
            MessageType.TYPE_TEXT -> {
                //查看文字
                if (message.messageContent is TextMessage) {
                    var intent = Intent(this, ChatTextActivity::class.java)
                    intent.putExtra("message", message)
                    startActivity(intent)
                }
            }

            MessageType.TYPE_RECORD -> {
                var intent = Intent(this, ChatRecordActivity::class.java)
                intent.putExtra("message", message)
                startActivity(intent)
            }
            MessageType.TYPE_GEO -> {
                LocationDetailActivity.startActivity(this, message)
            }
            MessageType.TYPE_IMAGE -> {
                val imageMsg = message.messageContent as ImageMessage
                val extra = imageMsg.extra
                if (!extra.isNullOrEmpty()) {
                    val jsonObject = JSONObject(extra)
                    val type = jsonObject.optString("type")
                    if (!type.isNullOrEmpty() && type == "emoji") {
                        ChatGifActivity.startActivity(this, message)
                    }
                } else {
                    ImagePageActivity.startActivity(this, message)
                }
            }
            MessageType.TYPE_IMAGE_AND_TEXT -> {
                var uri = Uri.parse(
                    (message.messageContent as ImageTextMessage)!!.redirectUrl
                )
                var intent = Intent(Intent.ACTION_VIEW, uri)
                intent.data = uri
                startActivity(intent)
            }
            MessageType.TYPE_FILE -> {
                ChatFileActivity.startActivity(this, message)
            }
            MessageType.TYPE_AUDIO -> {
                if (isReply) {
                    //回复语音消息二级页面播放语音
                    var intent = Intent(this, ChatVoiceActivity::class.java)
                    intent.putExtra("message", message)
                    startActivity(intent)
                } else {
                    var voiceMessageHandler: ChatVoiceMessageHandler.ViewHolder? = null
                    if (v.tag is ChatVoiceMessageHandler.ViewHolder) {
                        voiceMessageHandler = v.tag as ChatVoiceMessageHandler.ViewHolder
                    }
                    val voiceMessage = message.messageContent as AudioMessage
                    if (!AudioPlayManager.getInstance()
                            .isInNormalMode(this) && AudioPlayManager.getInstance()
                            .isInVOIPMode(this)
                    ) {
                        ToastUtil.toast(this, getString(R.string.qx_voip_occupying))
                    } else {
                        //如果正在播放中,点击就使其暂停
                        if (AudioPlayManager.getInstance().isPlaying) {
                            //如果当前播放的不是当前点击的语音，使其播放后return.再停止播放
                            if (AudioPlayManager.getInstance().playingUri != Uri.parse(voiceMessage.localPath)) {
                                AudioPlayManager.getInstance().startPlay(
                                    this, Uri.parse(voiceMessage.localPath),
                                    ChatVoiceMessageHandler().VoiceMessagePlayListener(
                                        this,
                                        message,
                                        voiceMessageHandler!!
                                    )
                                )
                                return
                            }
                            AudioPlayManager.getInstance().stopPlay()
                        } else {
                            //没有播放，就使其播放
                            AudioPlayManager.getInstance().startPlay(
                                this, Uri.parse(voiceMessage.localPath),
                                ChatVoiceMessageHandler().VoiceMessagePlayListener(
                                    this,
                                    message,
                                    voiceMessageHandler!!
                                )
                            )
                        }
                    }
                }
            }
            MessageType.TYPE_VIDEO -> {
                VideoPlayActivity.startActivity(this, message)
            }
            MessageType.TYPE_AUDIO_CALL -> {
                RTCModuleManager.instance.onClick(this, conversationType!!, targetId!!, 0)
            }

            MessageType.TYPE_VIDEO_CALL -> {
                RTCModuleManager.instance.onClick(this, conversationType!!, targetId!!, 1)
            }
        }
    }

    /**
     * 关闭底部面板+软键盘
     */
    fun outSideClickCloseKeyboard() {
        if (qxExtension != null) {
            qxExtension.outSideClickCloseKeyboard()
        }
    }

    /**
     * 底部获取到焦点的监听
     */
    override fun onBottomFocusCallBack() {
        recycler_view_message.postDelayed(java.lang.Runnable {
            if (mMessageAdapter != null && mMessageAdapter.itemCount > 0) {
                //已滚到底部，则不处理
                if (!recycler_view_message.canScrollVertically(1)) {
                    return@Runnable
                }
                var index = mMessageAdapter.itemCount - 1
                if (index > -1) {
                    recycler_view_message.smoothScrollToPosition(index)
                }
            }
        }, 200)
    }

    private fun getMediaMessageUri(message: Message): Uri? {
        if (message.messageContent == null) {
            return null
        }
        when (message.messageType) {
            MessageType.TYPE_IMAGE -> {
                if (message.messageContent is ImageMessage) {
                    var path = (message.messageContent as ImageMessage).localPath
                    return parseUri(path)
                }
            }
            MessageType.TYPE_AUDIO -> {
                if (message.messageContent is AudioMessage) {
                    var path = (message.messageContent as AudioMessage).localPath
                    return parseUri(path)
                }
            }
            MessageType.TYPE_VIDEO -> {
                if (message.messageContent is VideoMessage) {
                    var path = (message.messageContent as VideoMessage).localPath
                    return parseUri(path)
                }
            }
            MessageType.TYPE_FILE -> {
                if (message.messageContent is FileMessage) {
                    var path = (message.messageContent as FileMessage).localPath
                    return parseUri(path)
                }
            }
            MessageType.TYPE_GEO -> {
                if (message.messageContent is GeoMessage) {
                    var path = (message.messageContent as GeoMessage).localPath
                    return parseUri(path)
                }
            }
        }
        return null
    }

    private fun parseUri(path: String?): Uri? {
        return if (TextUtils.isEmpty(path)) {
            null
        } else {
            Uri.parse(path)
        }
    }

    private fun isMediaMessage(message: Message): Boolean {
        return message.messageType == MessageType.TYPE_FILE || message.messageType == MessageType.TYPE_VIDEO
                || message.messageType == MessageType.TYPE_IMAGE || message.messageType == MessageType.TYPE_AUDIO
                || message.messageType == MessageType.TYPE_GEO
    }

    open fun onAvatarLongClick(userInfo: QXUserInfo) {

    }

    /**
     * 消息已显示
     */
    open fun messageDisplay(message: Message) {

    }

    private fun initReplyView() {
        iv_reply_cancel.setOnClickListener {
            layout_reply.visibility = View.GONE
            toBeReplyMessage = null
        }
    }

    //相册 拍照权限
    fun handlePermission() {
        Acp.getInstance(this@BaseChatActivity).request(
            AcpOptions.Builder().setPermissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS
            ).setRationalMessage(resources.getString(R.string.qx_permission_request))
                .setRationalBtn(resources.getString(R.string.qx_permission_ok))
                .setDeniedCloseBtn(resources.getString(R.string.qx_permission_close))
                .setDeniedSettingBtn(resources.getString(R.string.qx_permission_setting))
                .setDeniedMessage(resources.getString(R.string.qx_permission_record_camera))
                .build(),
            object : AcpListener {
                override fun onGranted() {

                }

                override fun onDenied(permissions: List<String>) {}
            })

    }

    override fun onResume() {
        super.onResume()
        setTitleBarName()
        checkAndroid11Permission()
    }

    private fun checkAndroid11Permission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {

                var isManager = Environment.isExternalStorageManager()
                if (!isManager) {

                    ToastUtil.toast(
                        this, String.format(
                            resources.getString(R.string.qx_permission_manage_all_file),
                            LibStorageUtils.getAppName(this)
                        )
                    )
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivity(intent)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getIntentData() {
        if (intent == null) {
            finish()
            return
        }
        targetId = intent.getStringExtra("targetId")

        if (targetId.isNullOrEmpty()) {
            targetId = intent.data?.getQueryParameter("targetId")
        }
        conversationType = setConversationType()
        if (targetId.isNullOrEmpty()) {
            finish()
            return
        }
        handleLocateMessage()
    }

    abstract fun setConversationType(): String
    open fun setTitleBarName() {
        targetName = UserInfoUtil.getTargetName(this, conversationType!!, targetId!!)
        tv_title_bar_name.text = targetName
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent);
        handleLocateMessage()
    }

    private fun handleLocateMessage() {
        //用于滚动定位的消息
        locateMessage = intent.getParcelableExtra<Message>("locateMessage")
        if (locateMessage != null) {
//            //合并时间窗
//            IMDatabaseRepository.instance.calcUnTrustTime2(locateMessage!!.conversationType, locateMessage!!.targetId, 0, 0)
            getMessageByTimestamp(locateMessage!!)
        }
    }

    fun getMessageByTimestamp(message: Message) {
        QXIMKit.getInstance()
            .getMessagesByTimestamp(conversationType, targetId, message!!.timestamp, 1, pageSize,
                object : QXIMClient.ResultCallback<List<Message>>() {
                    override fun onSuccess(data: List<Message>) {
                        if (data.isNotEmpty()) {
                            var isFirstTimeLoad = false
                            if (mMessageList.isNullOrEmpty()) {
                                isFirstTimeLoad = true //如果是第一次加载数据
                            }
                            removeDuplicateMessage(data.toTypedArray())
                            mMessageAdapter.notifyDataSetChanged()
                            if (isFirstTimeLoad) {
                                recycler_view_message.smoothScrollToPosition(mMessageList.size - 1)
                            } else {
                                scrollToMessage(message)//滚动到@消息
                            }
                        }
                    }

                    override fun onFailed(error: QXError) {

                    }
                })
    }

    //滚动到@消息
    fun scrollToMessage(message: Message?) {
        if (message != null) {
            var index = mMessageList.indexOf(message)
            if (index != -1) {
                recycler_view_message.scrollToPosition(index)
            }
        }
    }
    override fun onPause() {
        super.onPause()
        AudioPlayManager.getInstance().stopPlay()

    }

    override fun onDestroy() {
        QLog.d(TAG, "onDestroy")
        QXIMKit.setOnReceiveMessageListener(null)
//        QXIMClient.instance.removeOnMessageReceiveListener()
        QXIMClient.instance.removeMessageReceiptListener()
        MediaMessageEmitter.removeMediaMessageCallback()
        QXIMKit.removeCallBack()
        if (kitReceiver != null) {
            try {
                unregisterReceiver(kitReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onDestroy()
    }
    //判断是否为当前聊天对象的消息
    fun isCurrentTargetIdMessage(message: Message): Boolean {
        QLog.d(
            TAG,
            "isCurrentTargetIdMessage message.conversationType = " + message.conversationType + " targetId:$targetId message.targetId=" + message.targetId
        )
        when (message.conversationType) {
            ConversationType.TYPE_PRIVATE -> {
                val curUserId = QXIMClient.instance.getCurUserId()
                if ((message.senderUserId == targetId && message.targetId == curUserId) || (message.targetId == targetId && message.senderUserId == curUserId)) {
                    return true
                }
            }
            ConversationType.TYPE_SYSTEM -> {
                return conversationType == ConversationType.TYPE_SYSTEM
            }
            else -> {
                //否则为群组消息、聊天室消息、系统消息，这时需要判断to是否和targetId一样
                if (message.targetId == targetId) {
                    return true
                }
            }
        }
        return false
    }

    //发送已读回执
    fun sendReadReceipt() {
        if (conversation == null) return
        QXIMClient.instance.getLatestMessageByConversationId(conversationId,
            UserInfoCache.getUserId(),
            object : QXIMClient.ResultCallback<Message>() {
                override fun onSuccess(data: Message) {
                    QXIMClient.instance.sendMessageReadReceipt(conversation!!.conversationId,
                        targetId!!,
                        data.timestamp,
                        object : QXIMClient.OperationCallback() {
                            override fun onSuccess() {
                                Log.e(TAG, "onSuccess: 发送回执成功")
                            }

                            override fun onFailed(error: QXError) {
                                // if (BuildConfig.DEBUG) {
                                Log.d(TAG, "发送回执失败，错误码：${error.code} 错误信息：${error.msg}")
                                //   }
                            }
                        })

                }

                override fun onFailed(error: QXError) {

                }

            })
    }

    private fun refreshListForDelete(messages: List<Message>) {
        mMessageList.removeAll(messages)
        mMessageList.sort()
        mMessageAdapter.notifyDataSetChanged()
    }

    open fun initIMListener() {
        mMessageReceiveListener = object : QXIMClient.OnMessageReceiveListener {
            override fun onReceiveNewMessage(message: List<Message>) {
                QLog.i(TAG, "收到新消息： ")
                if (message.isNullOrEmpty()) {
                    return
                }
                if (!isCurrentTargetIdMessage(message[0])) {
                    return
                }
                sendReadReceipt()//发送已读回执
                refreshListForInsert(message.toTypedArray(), FLAG_NEW_MESSAGE)//刷新列表
            }

            override fun onReceiveRecallMessage(message: Message) {
                //收到撤回消息
                QLog.i(TAG, "收到撤回消息")
                try {
                    if (!isCurrentTargetIdMessage(message)) {
                        return
                    }
                    refreshListForUpdate(arrayOf(message))
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }

            override fun onReceiveInputStatusMessage(from: String) {
                if (conversationType == Conversation.Type.TYPE_PRIVATE && from == targetId) {
                    //收到正在输入的消息
                    tv_title_bar_name.text = getString(R.string.qx_input_ing)
                    tv_title_bar_name.postDelayed(
                        Runnable { tv_title_bar_name.text = targetName },
                        10000
                    )
                }
            }

            override fun onReceiveHistoryMessage(message: List<Message>) {
                if (message.isNullOrEmpty()) {
                    return
                }
                if (!isCurrentTargetIdMessage(message[0])) {
                    return
                }
                QLog.i(TAG, "收到后台返回历史消息:$message")
                for (i in message.indices) {
                    Log.e(
                        TAG,
                        "onReceiveHistoryMessage: 消息类型：" + message.get(i).messageType + "  时间为：" + message.get(
                            i
                        ).timestamp
                    )
                }
                refreshListForInsert(message.toTypedArray(), FLAG_HISTORY_MESSAGE)
            }

            override fun onReceiveP2POfflineMessage(message: List<Message>) {
                QLog.i(TAG, "收到单聊离线消息，数量：" + message.size)
                if (!isCurrentTargetIdMessage(message[0])) {
                    return
                }
                refreshListForInsert(message.toTypedArray(), FLAG_HISTORY_MESSAGE)
                sendReadReceipt()
            }

            override fun onReceiveGroupOfflineMessage(message: List<Message>) {
                QLog.i(TAG, "收到群组离线消息，数量：" + message.size)
                if (!isCurrentTargetIdMessage(message[0])) {
                    return
                }
                refreshListForInsert(message.toTypedArray(), FLAG_OFFLINE_MESSAGE)
                sendReadReceipt()
            }

            override fun onReceiveSystemOfflineMessage(message: List<Message>) {
                QLog.i(TAG, "收到系统离线消息，数量：" + message.size)
                if (!isCurrentTargetIdMessage(message[0])) {
                    return
                }
                refreshListForInsert(message.toTypedArray(), FLAG_OFFLINE_MESSAGE)
                sendReadReceipt()
            }

        }
        mMessageReceiptListener = object : QXIMClient.MessageReceiptListener {
            override fun onMessageReceiptReceived(message: Message?) {
                if (message != null) {
                    if (isTargetsMessage(message)) {
                        when (message!!.state) {
                            Message.State.STATE_SENT -> {
                                QLog.i(TAG, "收到【已发送】消息回执：state=" + message!!.state)
                                refreshListForUpdate(arrayOf(message))
                            }
                            Message.State.STATE_RECEIVED -> {
                                QLog.i(TAG, "收到【已送达】消息回执：state=" + message!!.state)
                                refreshListForUpdate(arrayOf(message))
                            }
                        }
                    }
                }
            }

            override fun onMessageReceiptRead() {
                QLog.i(TAG, "收到消息【已阅读】回执 threadId=" + Thread.currentThread().id)
                //更新状态
                for (msg in mMessageList) {
                    if (msg.state != Message.State.STATE_SENDING && msg.state != Message.State.STATE_FAILED) {
                        msg.state = Message.State.STATE_READ
                    }
                }
                mMessageAdapter.notifyDataSetChanged()
            }
        }

        //设置新消息监听器
        QXIMKit.setOnReceiveMessageListener(mMessageReceiveListener!!)
        //设置消息回执监听器
        QXIMClient.instance!!.setMessageReceiptListener(mMessageReceiptListener!!)
    }

    var isSmoothScrollToPosition = true  // 接收对方消息，默认滚动到最新的消息

    //检测是否滑动到底部
    private fun isVisBottom(): Boolean {
        return recycler_view_message.canScrollVertically(1)
    }

    //去重
    open fun removeDuplicateMessage(messages: Array<Message>): ArrayList<Message> {
        var temp = arrayListOf<Message>()
        for (remote in messages) {
            if (mMessageList.contains(remote)) {
                var index = mMessageList.indexOf(remote)
                mMessageList[index] = remote
                mMessageAdapter.notifyItemChanged(index)
            } else {
                temp.add(remote)
            }
            //处理@消息已读状态，如果当前页面中收到的@消息，则设置为已读
            if (remote.conversationType == Conversation.Type.TYPE_GROUP) {
                if (remote.messageContent is TextMessage) {
                    var textMessage = remote.messageContent as TextMessage
                    if (textMessage != null && !textMessage.atToMessageList.isNullOrEmpty()) {
                        //设置已读
                        QXIMKit.getInstance().updateAtMessageReadState(
                            remote.messageId,
                            conversationId,
                            1,
                            object : QXIMClient.OperationCallback() {
                                override fun onSuccess() {
                                }

                                override fun onFailed(error: QXError) {

                                }
                            })
                    }
                }
            }
        }
        if (temp.size > 0) {
            mMessageList.addAll(temp)
            mMessageList.sort()
        }
        return temp
    }

    open fun refreshListForInsert(messages: Array<Message>, flag: Int) {
        var isFirstTimeLoad = false
        if (mMessageList.isNullOrEmpty() || offset == 0) {
            isFirstTimeLoad = true
        }
        var temp = removeDuplicateMessage(messages)
        Log.e(
            TAG,
            "refreshListForInsert: 去重前：" + messages.size + "  去重后：" + temp.size + " flag:" + flag
        )
        if (mMessageList.size > 0) {
            when (flag) {
                FLAG_OFFLINE_MESSAGE -> {

                }
                FLAG_NEW_MESSAGE -> {
                    //局部刷新，滚动到最新消息
                    mMessageAdapter.notifyDataSetChanged()
                    if (isSmoothScrollToPosition || !isVisBottom()) {
                        recycler_view_message.smoothScrollToPosition(mMessageList.size - 1)
                    }
                }
                FLAG_HISTORY_MESSAGE -> {
                    if (offset == 0 && !temp.isNullOrEmpty()) {
//                        if (temp.size < 50) {
//                            mMessageList.addAll(temp)
////                            if (mMessageList.size > 50) {
////                                Log.e(TAG, "refreshListForInsert: 历史消息数量：" + mMessageList.size)
////                                val mtempSubMessageList : ArrayList<Message>?
//////                                mtempSubMessageList = mMessageList.subList(0, 50) as ArrayList<Message>
////                                mMessageList.clear()
//////                                mMessageList.addAll(mtempSubMessageList)
////                                mMessageList = ArrayList(mMessageList.subList(0, 50))
////                                Log.e(TAG, "refreshListForInsert: >>>>>>>>>>> 最终的消息数量未;" + mMessageList.size)
////                            }
//                        } else {
//                            mMessageList.clear()
//                            mMessageList.addAll(temp)
//                        }

                        Log.e(TAG, "refreshListForInsert: 最终显示的数量未：" + mMessageList.size)
                        mMessageList.sort()
                        mMessageAdapter.notifyDataSetChanged()
                        recycler_view_message.smoothScrollToPosition(mMessageList.size - 1)
                        return
                    }
                    if (isFirstTimeLoad) {
                        mMessageAdapter.notifyDataSetChanged()
                        recycler_view_message.smoothScrollToPosition(mMessageList.size - 1)
                    } else {
                        mMessageAdapter.notifyDataSetChanged()
                        recycler_view_message.smoothScrollToPosition(temp.size - 1)
                    }
                }
            }
        }
    }

    //发送前，先处理@逻辑
    private fun buildAtToList(text: String): List<String> {
        // 1.先遍历内容中的@是否存在list中，如果存在，则加入list
        var list = arrayListOf<String>()
        for (at in mAtToList) {
            var name = "@" + at.name + " "//空格一定要加
            if (text.contains(name)) {
                list.add(at.id)
            }
        }
        //如果文字中包含@所有人，则加一个-1
        if (text.contains(resources.getString(R.string.qx_at_to_all))) {
            list.add("-1")
        }
        return list
    }
    private fun send(text: String) {
        try {
            if (text.isNotEmpty()) {
                var atToList = buildAtToList(text)
                var message = MessageCreator.instance.createTextMessage(
                    conversationType!!,
                    QXIMClient.instance.getCurUserId()!!,
                    targetId!!,
                    text,
                    "",
                    atToList
                )
                sendMessage(message)
                edt_content.setText("")
                layout_reply.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendMessage(message: Message) {
        mAtToList.clear()
        var targetMessage = message
        //如果是聊天室消息
        if (message.conversationType == ConversationType.TYPE_CHAT_ROOM) {
            recycler_view_message.post {
                mMessageList.add(message)
                mMessageAdapter.notifyDataSetChanged()
                if (mMessageList.size > 0) {
                    recycler_view_message.smoothScrollToPosition(mMessageList.size - 1)
                }
            }
        }
        if (toBeReplyMessage != null) {
            //被回复消息不为空，则说明本次发送为回复消息，创建回复消息体，其中包含被回复的消息和回复的消息
            var replyMessage = MessageCreator.instance.createReplyMessage(
                conversationType!!, QXIMClient.instance.getCurUserId()!!,
                targetId!!, toBeReplyMessage!!, message, ""
            )
            targetMessage = replyMessage
        }
        QXIMKit.getInstance().sendMessage(targetMessage, sendMessageCallback)
        toBeReplyMessage = null
        layout_reply.visibility = View.GONE
    }
    fun sendGifMessage(
        localPath: String?,
        originUri: String?,
        width: Int,
        height: Int,
        index: Int
    ) {
        if (TextUtils.isEmpty(localPath)) {
            if (!TextUtils.isEmpty(originUri)) {
               // ThreadPoolUtils.run {
                    val file = GlideUtil.getCacheFile(this@BaseChatActivity, originUri)
                    if (file != null) {
                        val length = if (file != null && file.exists()) file.length() else 0
                        if (length != 0L) {
                            val message = MessageCreator.instance.createImageMessage(
                                conversationType!!, QXIMClient.instance.getCurUserId()!!,
                                targetId!!, file.path, originUri
                                    ?: "", "", width, height, length, "qx_emoji:$index"
                            )
                            QXIMKit.getInstance()
                                .sendMediaMessage(message, sendMediaMessageCallback)
                        }

                    }
               // }
            }

        } else {
            val file = File(localPath)
            if (file != null) {
                val length = if (file != null && file.exists()) file.length() else 0
                if (length != 0L) {
                    val message = MessageCreator.instance.createImageMessage(
                        conversationType!!, QXIMClient.instance.getCurUserId()!!,
                        targetId!!, file.path, originUri
                            ?: "", "", width, height, length, "qx_emoji:$index"
                    )
                    QXIMKit.getInstance().sendMediaMessage(message, sendMediaMessageCallback)
                }

            }
        }
    }
    /**
     * 发送媒体消息
     */
    private fun sendMediaMessage(messageType: String, uri: Uri) {
        Log.i("sendMediaMessage", "uri:$uri")
        QXIMKit.getInstance().sendMediaMessage(
            this, conversationType!!, targetId!!,
            messageType, uri, sendMediaMessageCallback
        )
    }

    /**
     * 发送媒体消息
     */
    private fun sendMediaMessage(message: Message) {
        QXIMKit.getInstance().sendMediaMessage(message, sendMediaMessageCallback)
    }

    fun sendTextMsg() {
        var text = edt_content.text.toString()
        if (text.length > TEXT_MAX_LENGTH) {
            ToastUtil.toast(
                this,
                String.format(resources.getString(R.string.qx_msg_text_max_length), TEXT_MAX_LENGTH)
            )
            return
        }
        GlobalScope.launch {
            QXIMKit.getInstance()
                .checkSensitiveWord(
                    text,
                    object : QXIMClient.ResultCallback<SensitiveWordResult>() {
                        override fun onSuccess(result: SensitiveWordResult) {
                            btn_send.isClickable = true
                            //如果该消息包含被禁发的敏感词
                            if (result.isBan) {
                                ToastUtil.toast(
                                    this@BaseChatActivity,
                                    resources.getString(R.string.qx_chat_bottom_bar_send_tips_ban)
                                )
                                return
                            }
                            Log.e("SensitiveWordsUtils", "onSuccess: 过滤敏感词完成，开始发送")
                            send(result.text)
                        }

                        override fun onFailed(error: QXError) {
                            btn_send.isClickable = true
                            send(text)
                        }

                    })
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_OK -> {
                when (requestCode) {
                    REQUEST_CODE_FILE -> {
                        handleFileResult(data)
                    }
                    REQUEST_CODE_GALLREY -> {
                        handleAlbumResult(data)
                    }
                    REQUEST_CODE_GEO -> {
                        handleGeoResult(data!!)
                    }

                    REQUEST_CODE_CAMERA -> {

                    }
                    else -> {
                        qxExtension.onActivityForResult(requestCode, resultCode, data)
                    }
                }
            }
            PicturePreviewActivity.RESULT_SEND -> {
                // 点击拍照插件后第一次返回拍摄完的图片地址到takephotoplugin进行跳转到picturepreview
                // picturepreview处理完后返回到这里获取参数
                qxExtension.onActivityForResult(requestCode, RESULT_OK, data)
            }

        }
        if (requestCode == REQUEST_CODE_ADVANCE) {

            when (resultCode) {
                QXIMKit.RESULT_CODE_ADVANCE_CLEAR_MESSAGE -> {
                    onClearMessage()
                }
                QXIMKit.RESULT_CODE_ADVANCE_EXIT_GROUP -> {
                    onExitGroup()
                }
                else -> {
                    initChatBackground()//刷新聊天背景
                    onAdvancedBack()
                }
            }
        }
    }

    abstract fun onAdvancedBack();

    abstract fun onClearMessage()

    protected open fun onExitGroup() {

    }

    private fun handleGeoResult(data: Intent) {
        var message = data.getParcelableExtra<Message>("geo")
        if (message == null) {
            ToastUtil.toast(this, getString(R.string.qx_error_location_data))
            return
        }
        sendMediaMessage(message)
    }

    /**
     * 处理相册选择后的图片或视频
     */
    private fun handleAlbumResult(data: Intent?) {
        var uriList: ArrayList<Uri> = ArrayList()
        val imageNames = data!!.clipData
        if (imageNames != null) {
            for (i in 0 until imageNames.itemCount) {
                val uri = imageNames.getItemAt(i).uri
                uriList.add(uri)
            }
        } else {
            uriList.add(data.data!!)
        }
        for (uri in uriList) {
            var file = File(FilePathUtil.getPath(this, uri))
            var messageType = MessageType.TYPE_IMAGE
            when (MediaUtil.getMediaType(getSuffixName(file.absolutePath))) {
                LocalMedia.MediaType.MEDIA_TYPE_IMAGE -> {
                    messageType = MessageType.TYPE_IMAGE
                }
                LocalMedia.MediaType.MEDIA_TYPE_VIDEO -> {
                    messageType = MessageType.TYPE_VIDEO
                }
            }
            sendMediaMessage(messageType, uri)
        }

    }

    /**
     * 选择的文件返回结果
     */
    private fun handleFileResult(data: Intent?) {
        var fileUriList: ArrayList<Uri> = ArrayList()
        val fileName = data!!.clipData
        if (fileName != null) {
            for (i in 0 until fileName.itemCount) {
                val uri = fileName.getItemAt(i).uri
                fileUriList.add(uri)
            }
        } else {
            fileUriList.add(data.data!!)
        }
        for (index in 0 until fileUriList.size) {
            var uri = fileUriList[index]
            //判断文件大小是否超过边界值
            val file = File(FilePathUtil.getPath(this, uri))
            if (file != null) {
                //当前文件大小
                var fileSize = FileSizeUtil.getFileOrFilesSize(file, 3);
                Log.i("sendMediaMessage", "要发送File文件大小：" + fileSize + "M")
                //限定最大文件大小，单位M
                var maxFileSize: Double = QXConfigManager.getQxFileConfig()
                    .getFileMessageMaxSize(FileSizeUtil.SIZETYPE_MB);
                if (fileSize > maxFileSize) {
                    //超过限定大小
                    Toast.makeText(
                        this@BaseChatActivity,
                        StringUtils.getResourceStr(
                            this@BaseChatActivity,
                            R.string.qx_error_file_exceeded,
                            maxFileSize
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    sendMediaMessage(MessageType.TYPE_FILE, uri)
                }
            } else {
                Toast.makeText(
                    this@BaseChatActivity,
                    getString(R.string.qx_error_file_uri_not_found),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    override fun onImageResult(selectedMedias: LinkedHashMap<String, Int>?, origin: Boolean) {
        if (selectedMedias == null)
            return
        val iterator = selectedMedias.iterator()
        while (iterator.hasNext()) {
            val media = iterator.next()
            val mediaUri = media.key
            when (media.value) {
                1 -> {
                    compressImage(Uri.parse(mediaUri), origin)
                    // image

                }
                3 -> {
                    // video
                    sendMediaMessage(MessageType.TYPE_VIDEO, Uri.parse(mediaUri))
                }
            }
        }
    }


    override fun onLocationResult(data: Intent) {
        handleGeoResult(data)
    }

    override fun onFileReuslt(data: Intent) {
        handleFileResult(data)
    }

    private fun compressImage(originUri: Uri, origin: Boolean) {
//        var newUri = originUri
//        if (KitStorageUtils.isBuildAndTargetForQ(this.applicationContext)) {
//            newUri = Uri.parse("content://${originUri.toString()}")
//        }
        if (originUri.toString().contains(".gif")) {
            sendMediaMessage(MessageType.TYPE_IMAGE, originUri)
        } else {
            QLog.d(TAG, "compressImage uri:$originUri")
            ImageCompressUtil.compressImage(
                this,
                originUri,
                KitStorageUtils.getImageSavePath(this),
                object : OnCompressListener {
                    override fun onSuccess(file: File) {
                        if (origin) {
                            sendMediaMessage(MessageType.TYPE_IMAGE, originUri)
                        } else {
                            sendMediaMessage(MessageType.TYPE_IMAGE, Uri.parse(file.path))
                        }
                    }

                    override fun onError(e: Throwable) {

                    }

                    override fun onStart() {
                    }

                })
        }
    }
    private val sendMessageCallback = object : QXIMClient.SendMessageCallback() {
        override fun onAttached(message: Message?) {
            recycler_view_message.post {
                //如果为发送正在输入状态消息，则不处理
                if (message != null) {
                    if (isTargetsMessage(message)) {
                        updateList(message)
                    }
                }
            }
        }

        override fun onSuccess() {
            QLog.i(TAG, "发送消息成功 threadId = " + Thread.currentThread().id)
        }

        override fun onError(error: QXError, message: Message?) {
            if (message != null) {
                refreshListForUpdate(arrayOf(message))
            }
            var errorMsg =
                ErrorMessageUtil.getErrorMessage(conversationType!!, error, this@BaseChatActivity)
            if (!TextUtils.isEmpty(errorMsg)) {
                ToastUtil.toast(this@BaseChatActivity, errorMsg)
            }
            QLog.e(TAG, "发送消息失败，错误码：${error.code} 错误信息：${error.msg}")
        }

    }


    private val sendMediaMessageCallback = object : MediaMessageEmitter.SendMediaMessageCallback {
        override fun onProgress(progress: Int) {
            QLog.i(TAG, " sendMediaMessageCallback > progress:$progress")
        }

        override fun onUploadCompleted(message: Message?) {
            runOnUiThread {
                if (message != null) {
                    refreshListForUpdate(arrayOf(message!!))
                }
                QLog.i(TAG, " sendMediaMessageCallback > onUploadCompleted")
            }
        }

        override fun onUploadFailed(errorCode: Int, msg: String, message: Message?) {
            runOnUiThread {
                mMessageAdapter.notifyDataSetChanged()
            }
            QLog.i(TAG, " sendMediaMessageCallback > onUploadFailed")
        }

        override fun onAttached(message: Message?) {
            recycler_view_message.post {
                //如果为发送正在输入消息状态，则不处理
                if (message != null) {
                    if (isTargetsMessage(message)) {
                        updateList(message)
                    }
                }
            }
        }

        override fun onSuccess() {

        }

        override fun onError(error: QXError, message: Message?) {
            runOnUiThread {
                QLog.e(TAG, "发送消息失败，错误码：${error.code} 错误信息：${error.msg}")
                if (message != null) {
                    refreshListForUpdate(arrayOf(message))
                }
                var errorMsg = ErrorMessageUtil.getErrorMessage(
                    conversationType!!,
                    error,
                    this@BaseChatActivity
                )
                if (!TextUtils.isEmpty(errorMsg)) {
                    ToastUtil.toast(this@BaseChatActivity, errorMsg)
                }
            }
            QLog.i(TAG, " sendMediaMessageCallback > onError")

        }

    }

    private fun updateList(message: Message) {
        if (!mMessageList.contains(message)) {
            mMessageList.add(message)
        }
        mMessageAdapter.notifyDataSetChanged()
        //做时间排序
        if (mMessageList.size > 0) {
            recycler_view_message.smoothScrollToPosition(mMessageList.size - 1)
        }
    }

    private fun handleForward() {
        QXIMKit.setQXFowardDataCallBack { list, forwardEndContinue ->
            mForwardList.clear()
            for (data in list) {
                when (mCurrForwordType) {
                    FORWARD_TYPE_SINGLE -> {
                        var msg = checkMsgList.first().clone() as Message
                        msg.targetId = data.targetId
                        if (checkType(data.type).isNotEmpty()) {
                            msg.conversationType = checkType(data.type)
                            sendMessage(msg)
                        } else {
                            ToastUtil.toast(
                                this,
                                getString(R.string.qx_error_param, "${data.type}")
                            )
                        }
                    }
                    FORWARD_TYPE_MULTI_ONE_BY_ONE -> {
                        mMessageAdapter.setMultipleCheckable(false)
                        for (index in 0 until checkMsgList.size) {
                            var msg = removeReplyMsg(checkMsgList[index].clone() as Message)
                            msg.targetId = data.targetId
                            if (checkType(data.type).isNotEmpty()) {
                                msg.conversationType = checkType(data.type)
                                sendMessage(msg)
                            } else {
                                ToastUtil.toast(
                                    this,
                                    getString(R.string.qx_error_param, "${data.type}")
                                )
                            }
                        }
                        hideMultipleView()
                    }
                    FORWARD_TYPE_MULTI_COMBINE -> {
                        mMessageAdapter.setMultipleCheckable(false)
                        var userId = arrayListOf<String>()
                        if (conversationType == Conversation.Type.TYPE_PRIVATE) {
                            userId.add(QXIMKit.getInstance().curUserId)
                            userId.add(targetId!!)
                        }
                        //传入扩展字段
                        var extra = Gson().toJson(RecordExtra(conversationType!!, userId))
                        var retransmissionMessage = MessageCreator.instance.createRecordMessage(
                            conversationType!!, QXIMClient.instance.getCurUserId()!!,
                            data.targetId!!, removeReplyMsgs(checkMsgList), extra
                        )

                        if (checkType(data.type).isNotEmpty()) {
                            retransmissionMessage.conversationType = checkType(data.type)
                            sendMessage(retransmissionMessage)

                        } else {
                            ToastUtil.toast(
                                this,
                                getString(R.string.qx_error_param, "${data.type}")
                            )
                        }
                        hideMultipleView()
                    }
                }
            }
            if (!forwardEndContinue) {
                checkMsgList.clear()
            }
        }
    }

    private fun getHistoryMessagesFromLocal() {
        QXIMClient.instance.getHistoryMessagesFromLocal(conversationId,
            conversationType!!,
            targetId!!,
            offset,
            pageSize,
            object : QXIMClient.ResultCallback<List<Message>>() {
                override fun onSuccess(data: List<Message>) {
                    if (data.isNotEmpty()) {
                        var isFirstTimeLoad = false
                        if (mMessageList.isNullOrEmpty()) {
                            //如果第一次加载数据
                            isFirstTimeLoad = true
                        }

                        if (isFirstTimeLoad || offset == 0) {
                            mMessageList.clear()
                            mMessageList.addAll(data)
                        } else {
                            mMessageList.addAll(data)
                        }
                        mMessageList.sort()
                        mMessageAdapter.notifyDataSetChanged()
                        if (isFirstTimeLoad || offset == 0) {
                            recycler_view_message.scrollToPosition(mMessageList.size - 1)
                        } else {
                            recycler_view_message.scrollToPosition(data.size - 1)
                        }
                    }
                }

                override fun onFailed(error: QXError) {
                    Log.e(TAG, "onFailed: ========= 加载本地数据失败：" + error.msg)
                }
            })
    }

    private fun hideMultipleView() {
        for (msg in checkMsgList) {
            msg.isChecked = false
        }
        isMultiple = false
        mMessageAdapter.setMultipleCheckable(false)
        updateMultipleButton(false)
        updateMultipleUI()
    }

    private fun updateMultipleUI() {
        if (isMultiple) {
            layout_input_panel.visibility = View.GONE
            bottom_layout.visibility = View.GONE
            layout_multiple_operation.visibility = View.VISIBLE
        } else {
            layout_input_panel.visibility = View.VISIBLE
            layout_multiple_operation.visibility = View.GONE
        }
    }

    private fun updateMultipleButton(isEnabled: Boolean) {
        btn_retransmission.isEnabled = isEnabled
        btn_favorite.isEnabled = isEnabled
        btn_delete.isEnabled = isEnabled
    }

    //检测禁言缓存
    fun updateInputViewByMute(tips: String, isMute: Boolean) {
        if (tips.isNotEmpty()) {
            tv_mute.text = tips
        }
        tv_mute.visibility = if (isMute) {
            View.VISIBLE
        } else {
            View.GONE
        }
        iv_audio.isEnabled = !isMute
        iv_emoj.isEnabled = !isMute
        iv_add.isEnabled = !isMute
        btn_record.isEnabled = !isMute
        edt_content.isEnabled = !isMute
        btn_send.isEnabled = !isMute

        if (isMute) {
            layout_reply.visibility = View.GONE
        }
    }
    private fun refreshListForUpdate(messages: Array<Message>) {
        mMessageList.removeAll(messages)
        mMessageList.addAll(messages)
        mMessageList.sort()
        mMessageAdapter.notifyDataSetChanged()
    }

    fun isTargetsMessage(message: Message): Boolean {
        //如果为发送正在输入消息状态，则不处理
        if (message!!.messageType != MessageType.TYPE_STATUS) {
            if (message?.conversationType == conversationType && message?.targetId == targetId) {
                return true
            }
        }
        return false
    }

    fun checkType(type: String): String {
        if (type == Conversation.Type.TYPE_PRIVATE) {
            return Conversation.Type.TYPE_PRIVATE
        }
        if (type == Conversation.Type.TYPE_GROUP) {
            return Conversation.Type.TYPE_GROUP
        }
        return ""
    }

    //过滤回复消息
    private fun removeReplyMsgs(messages: ArrayList<Message>): ArrayList<Message> {
        var list = ArrayList<Message>()
        for (msg in messages) {
            list.add(removeReplyMsg(msg))
        }
        return list
    }

    private fun removeReplyMsg(message: Message): Message {
        if (message.messageType == MessageType.TYPE_REPLY) {
            var reply = message.messageContent as ReplyMessage
            message.messageType = reply.answer.messageType
            message.messageContent = reply.answer.messageContent
        }
        return message
    }


    // 异步返回用户相关信息
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventUserUpdate(userinfo: QXUserInfo) {
        if (userinfo != null) {
            recycler_view_message.post {
                mMessageAdapter.notifyDataSetChanged()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventGroupUserUpdate(userinfo: QXGroupUserInfo) {
        if (userinfo != null) {
            recycler_view_message.post {
                mMessageAdapter.notifyDataSetChanged()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMessage(message: Message) {
        refreshListForInsert(arrayOf(message), FLAG_NEW_MESSAGE)
    }
}