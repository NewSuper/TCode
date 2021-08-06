package com.qx.imui.adapter

import android.view.View
import android.view.ViewGroup
import com.qx.message.Message
import com.qx.message.MessageType

abstract class BaseContentViewDispatcher {
    abstract fun createTextHandler(): BaseMessageHandler?
    abstract fun createImageHandler(): BaseMessageHandler?
    abstract fun createImageTextHandler(): BaseMessageHandler?
    abstract fun createAudioHandler(): BaseMessageHandler?
    abstract fun createVideoHandler(): BaseMessageHandler?
    abstract fun createCallHandler(): BaseMessageHandler?
    abstract fun createFileHandler(): BaseMessageHandler?
    abstract fun createGeoHandler(): BaseMessageHandler?
    abstract fun createRecordHandler(): BaseMessageHandler?
    abstract fun createCustomHandler(): BaseMessageHandler?

    var mHandlerMap = HashMap<String, BaseMessageHandler>()
    open fun dispatch(itemView: View, contentView: ViewGroup, currMsg: Message) {
        contentView.removeAllViews()
        var handler = mHandlerMap[currMsg.messageType]
        if (handler == null) {
            handler = createWhenHandleNull(handler, currMsg)
        }
        contentView.tag = handler
        handler?.setContentView(itemView, contentView, currMsg)
    }

    open fun createWhenHandleNull(
        handler: ChatBaseMessageHandler?,
        currMsg: Message
    ): BaseMessageHandler? {
        //处理布局
        var messageType = getMessageType(currMsg)

        var h = createHandler(messageType)
        if (h != null) {
            mHandlerMap[messageType] = h
        }
        return h
    }

    open fun getMessageType(currMsg: Message): String {
        return currMsg.messageType
    }

    private fun createHandler(messageType: String?): BaseMessageHandler? {
        when (messageType) {
            MessageType.TYPE_TEXT -> {
                return createTextHandler()
            }
            MessageType.TYPE_IMAGE -> {
                return createImageHandler()
            }
            MessageType.TYPE_IMAGE_AND_TEXT -> {
                return createImageTextHandler()
            }
            MessageType.TYPE_AUDIO -> {
                return createAudioHandler()
            }
            MessageType.TYPE_VIDEO -> {
                return createVideoHandler()
            }
            MessageType.TYPE_AUDIO_CALL, MessageType.TYPE_VIDEO_CALL -> {
                return createCallHandler()
            }
            MessageType.TYPE_FILE -> {
                return createFileHandler()
            }
            MessageType.TYPE_GEO -> {
                return createGeoHandler()
            }
            MessageType.TYPE_RECORD -> {
                //转发
                return createRecordHandler()
            }
            else -> {
                return createCustomHandler()
            }
        }
    }
}