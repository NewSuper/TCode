package com.qx.imui.util.language

import com.qx.imui.R
import java.util.*

enum class LanguageType(
    val code: String,
    val locale: Locale,
    val displayName: Int
) {
    SIMPLIFIED_CHINESE("1", Locale.SIMPLIFIED_CHINESE, R.string.jianti),
    TRADITIONAL_CHINESE("2", Locale.TRADITIONAL_CHINESE, R.string.panti),
    US("3", Locale.US, R.string.languang_english),
    KOREA("4", Locale.KOREA, R.string.languang_hanyu),
    JAPAN("5", Locale.JAPAN, R.string.languang_riyu)
}