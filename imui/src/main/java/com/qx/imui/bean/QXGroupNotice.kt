package com.qx.imui.bean

import com.qx.imui.QXIMKit
import java.util.*

class QXGroupNotice(var groupId: String, var groupNotice: String) {
    var isRead: Boolean = false
    var ownerId: String = QXIMKit.getInstance().curUserId

    override fun hashCode(): Int {
        return Objects.hashCode(groupId + ownerId)
    }

    override fun equals(other: Any?): Boolean {
        return (groupId + ownerId) == ((other as QXGroupNotice).groupId + other.ownerId)
    }

}