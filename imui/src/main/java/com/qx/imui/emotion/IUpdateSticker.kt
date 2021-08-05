package com.qx.imui.emotion

interface IUpdateSticker {
    fun addSticker(stickerItem: StickerItem)
    fun removeSticker(stickerItem: StickerItem)
    fun removeSticker(stickerItemList: List<StickerItem>)
    fun notifyStickerPosition(from:Int,to:Int)
}