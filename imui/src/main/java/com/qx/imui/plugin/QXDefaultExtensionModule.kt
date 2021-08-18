package com.qx.imui.plugin

import android.content.Context
import com.qx.imui.QXExtension
import com.qx.imui.plugin.rtc.RTCModuleManager
import com.qx.imui.plugin.image.FilePlugin
import com.qx.imui.plugin.image.ImagePlugin
import com.qx.imui.plugin.image.LocationPlugin
import com.qx.imui.plugin.image.TakePhotoPlugin
import com.qx.message.Message

open class QXDefaultExtensionModule(context: Context) : IExtensionModule {

    override fun onInit(appKey: String) {

    }

    override fun onConnect(token: String) {

    }

    override fun onAttachedToExtension(extension: QXExtension) {
    }

    override fun onDetachedFromExtension() {
    }

    override fun onReceivedMessage(message: Message) {
    }

    override fun getPluginModules(conversationType: String): List<IPluginModule> {
        val pluginModuleList = mutableListOf<IPluginModule>()
        pluginModuleList.add(ImagePlugin())
        pluginModuleList.add(TakePhotoPlugin())
        pluginModuleList.addAll(RTCModuleManager.instance.getInternalPlugins(conversationType))
        pluginModuleList.add(LocationPlugin())
        pluginModuleList.add(FilePlugin())
        return pluginModuleList
    }

    override fun onDisconnect() {
    }


}