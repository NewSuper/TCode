package com.qx.imui.plugin

import android.content.Context

class QXExtensionManager {
    var context: Context? = null
    var appKey:String? = null


    fun registerExtensionModule(extensionModule:IExtensionModule) {
        if (mExtModules != null) {
            mExtModules.add(extensionModule)
            extensionModule.onInit(appKey!!)
        }

    }

    fun unregisterExtensionModule(extensionModule: IExtensionModule) {
        if (mExtModules != null) {
            val iterator = mExtModules.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() == extensionModule) {
                    iterator.remove()
                }
            }
        }
    }

    fun getExtensionModules() : List<IExtensionModule> {
        return mExtModules
    }

    companion object {

        private  var mExtModules = mutableListOf<IExtensionModule>()

        @JvmStatic
        fun init(context: Context, appKey:String) {
            instance.context = context
            instance.appKey = appKey
        }

        @JvmStatic
        val instance: QXExtensionManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            QXExtensionManager()
        }
    }
}