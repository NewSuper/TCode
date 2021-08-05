package com.qx.imui.emotion

import android.content.Context

interface IEmotionClickLisntener {
    fun emojiClick(emojiBean: EmojiBean)
    fun emojiDelClick()
    fun emojiSendClick()
    fun stickerClick(context: Context, sticker:StickerItem)
    fun stickerManager(context: Context)
    fun stickerDel(context: Context, sticker: StickerItem)
}