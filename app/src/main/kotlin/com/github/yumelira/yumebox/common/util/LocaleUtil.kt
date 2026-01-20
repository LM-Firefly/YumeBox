package com.github.yumelira.yumebox.common.util

import java.util.*

object LocaleUtil {
    fun normalizeRegionCode(countryCode: String?): String? {
        return countryCode
    }
    fun getFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🌐"
        return countryCode.uppercase().map { char ->
            Character.toChars(char.code + 127397)
        }.joinToString("") { String(it) }
    }
}
