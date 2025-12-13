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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.common.util

import java.util.Locale

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
