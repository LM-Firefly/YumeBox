/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

package com.github.yumelira.yumebox.presentation.util

data class FlaggedName(
    val countryCode: String?,
    val displayName: String,
)

private const val REGIONAL_INDICATOR_BASE = 0x1F1E6
private const val REGIONAL_INDICATOR_END = 0x1F1FF

private fun isRegionalIndicator(codePoint: Int): Boolean {
    return codePoint in REGIONAL_INDICATOR_BASE..REGIONAL_INDICATOR_END
}

private fun Char.isNameSeparator(): Boolean {
    return this.isWhitespace() || this == '-' || this == '|' || this == '·' || this == '•' || this == '—' || this == ':'
}

fun extractFlaggedName(rawName: String): FlaggedName {
    val trimmed = rawName.trim()
    if (trimmed.isEmpty()) return FlaggedName(countryCode = null, displayName = rawName)

    val first = trimmed.codePointAt(0)
    val firstChars = Character.charCount(first)
    if (!isRegionalIndicator(first) || trimmed.length <= firstChars) {
        return FlaggedName(countryCode = null, displayName = trimmed)
    }

    val second = trimmed.codePointAt(firstChars)
    if (!isRegionalIndicator(second)) {
        return FlaggedName(countryCode = null, displayName = trimmed)
    }

    val countryCode = buildString(2) {
        append(('A'.code + (first - REGIONAL_INDICATOR_BASE)).toChar())
        append(('A'.code + (second - REGIONAL_INDICATOR_BASE)).toChar())
    }

    val afterSecond = firstChars + Character.charCount(second)
    val rest = trimmed.substring(afterSecond).trimStart { it.isNameSeparator() }
    return FlaggedName(countryCode = countryCode, displayName = rest.ifEmpty { trimmed })
}

