package com.qx.imlib.db.entity

import androidx.room.*

/**
 *    author : zhangjingming
 *    e-mail : jimstin@126.com
 *    date   : 2021/1/18 16:11
 *    desc   : 记录不可信用时间区域，用于记录本地消息连续性
 *    version: 1.0
 */

@Entity(tableName = "un_trust_time")
class TBUnTrustTime {

    @PrimaryKey(autoGenerate = true)
    var id : Long = 0

    @ColumnInfo(name = "owner_id")
    var ownerId: String = ""

    @ColumnInfo(name = "conversation_type")
    var conversationType: String = ""

    @ColumnInfo(name = "target_id")
    var targetId: String = ""

    @ColumnInfo(name = "start_time")
    var startTime: Long = 0

    @ColumnInfo(name = "end_time")
    var endTime: Long = 0

    companion object {
        @JvmStatic
        fun obtain(ownerId : String, conversationType : String, targetId : String,
                   startTime : Long, endTime : Long) : TBUnTrustTime {
            var unTrustTime = TBUnTrustTime()
            unTrustTime.ownerId = ownerId
            unTrustTime.conversationType = conversationType
            unTrustTime.targetId = targetId
            unTrustTime.startTime = startTime
            unTrustTime.endTime = endTime
            return unTrustTime
        }
    }

    override fun toString(): String {
        return "TBUnTrustTime(id=$id, ownerId='$ownerId', conversationType='$conversationType', targetId='$targetId', startTime=$startTime, endTime=$endTime)"
    }


}