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

package com.github.yumelira.yumebox.core.util

fun decodeTrafficValue(value: Long): Long {
    val type = (value ushr 30) and 0x3
    val payload = value and 0x3FFFFFFFL

    return when (type) {
        0L -> payload
        1L -> (payload * 1024L) / 100L
        2L -> (payload * 1024L * 1024L) / 100L
        3L -> (payload * 1024L * 1024L * 1024L) / 100L
        else -> 0L
    }
}
