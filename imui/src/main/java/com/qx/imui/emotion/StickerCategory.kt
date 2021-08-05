package com.qx.imui.emotion

class StickerCategory (var title:String,var system:Boolean = false, var order:Int = 1,
                       var stickers:MutableList<StickerItem> = mutableListOf<StickerItem>()) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StickerCategory

        if (title != other.title) return false
        return true
    }

    override fun hashCode(): Int {
        return title.hashCode()
    }
}