package com.qx.imui.emotion

class StickerItem(var category: String? = null,
                  var name:String? = null,
                  var localPath:String? = null,
                  var originUrl:String? = null,
                  var width:Int? = null,
                  var height:Int? = null,
                  var index:Int? = null) {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StickerItem
        if (category != other.category) return false
        if (originUrl != other.originUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = category.hashCode()
        result = 31 * result + originUrl.hashCode()
        return result
    }

}