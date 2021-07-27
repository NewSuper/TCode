package com.qx.imlib.handler

import com.qx.imlib.netty.S2CRecMessage
import com.qx.imlib.qlog.QLog
import io.netty.channel.ChannelHandlerContext

class SystemMaintenanceHandler :BaseCmdHandler() {
    override fun handle(ctx: ChannelHandlerContext?, recMessage: S2CRecMessage?) {
        QLog.i("SystemMaintenanceHandler",  "cmd=" + recMessage!!.cmd + "系统维护")
    }
}