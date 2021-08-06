package com.qx.imui.plugin

import com.qx.imui.QXExtension
import com.qx.message.Message

interface IExtensionModule {

    fun onInit(appKey: String)

    fun onConnect(token: String)

    fun onAttachedToExtension(extension: QXExtension)

    fun onDetachedFromExtension()

    fun onReceivedMessage(message: Message)

    /**
     * 单聊，群聊部分插件功能不一样
     */
    fun getPluginModules(conversationType: String): List<IPluginModule>

    fun onDisconnect()
}