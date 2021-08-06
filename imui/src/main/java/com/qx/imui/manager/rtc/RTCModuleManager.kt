package com.qx.imui.manager.rtc

import android.content.Context
import com.qx.im.model.ConversationType
import com.qx.imlib.qlog.QLog
import com.qx.imui.plugin.IPluginModule

//InternalModuleManager
class RTCModuleManager {

    fun getInternalPlugins(conversationType: String): List<IPluginModule> {
        val pluginModules = mutableListOf<IPluginModule>()
        if (callModule != null && (conversationType == ConversationType.TYPE_PRIVATE )) {
            pluginModules.addAll(callModule!!.getPlugins(conversationType))
        }
        return pluginModules
    }

    fun onConnected(token: String,host:String) {
        QLog.d(TAG,"RTCModuleManager onConnected token:$token,host:$host")
        callModule?.onConnected(token,host)
    }

    fun onClick(context: Context, conversationType:String, target:String, mediayType:Int) {
        callModule?.onClick(context,conversationType,target,mediayType)
    }

    fun onInitialized(appKey: String?) {
        if (callModule != null) {
            callModule!!.onInitialized(appKey!!)
        }
    }

    companion object {

        private val TAG = "RTCModuleManager"

        private var callModule: IExternalModule? = null

        @JvmStatic
        fun init(context: Context, appKey:String) {

            try {
                val moduleName = "com.qx.rtc.call_lib.QXCallClient"
                val cls = Class.forName(moduleName)
                val constructor = cls.getConstructor(Context::class.java)
                val client = constructor.newInstance(context)
                QLog.d(TAG,"RTCModuleManager init getDeclaredConstructor2 QXCallClient")
            }catch (ex:Exception) {
                ex.printStackTrace()
                QLog.e(TAG,"RTCModuleManager init getDeclaredConstructor2 QXCallClient exception:${ex}")
            }

            try {
                val moduleName = "com.qx.rtc.call_kit.QXCallModule"
                val cls = Class.forName(moduleName)
                val constructor = cls.getDeclaredConstructor()
                callModule = constructor.newInstance() as IExternalModule
                callModule?.onCreate(context)
                QLog.d(TAG,"InternalModuleManager init getDeclaredConstructor2 QXCallModule")
            }catch (ex:Exception) {
                ex.printStackTrace()
                QLog.e(TAG,"InternalModuleManager init getDeclaredConstructor2 QXCallModule exception:${ex}")
            }
        }

        @JvmStatic
        val instance: RTCModuleManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            RTCModuleManager()
        }

    }

}