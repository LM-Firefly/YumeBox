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



package com.github.yumelira.yumebox.data.util

internal object MergeHelper {

    fun <T> mergeList(
        base: List<T>?,
        replace: List<T>?,
    ): List<T>? {
        return mergeList(
            base = base,
            replace = replace,
            start = null,
            end = null,
        )
    }

    fun <T> mergeList(
        base: List<T>?,
        replace: List<T>?,
        @Suppress("UNUSED_PARAMETER") start: List<T>?,
        @Suppress("UNUSED_PARAMETER") end: List<T>?,
    ): List<T>? {
        val merged = when {
            base == null -> replace
            replace == null -> base
            else -> base + replace
        }

        return merged?.takeIf { it.isNotEmpty() }
    }

    fun <K, V> mergeMap(
        base: Map<K, V>?,
        replace: Map<K, V>?,
        merge: Map<K, V>?,
    ): Map<K, V>? {
        val mergedMap = buildMap {
            base?.let(::putAll)
            replace?.let(::putAll)
            merge?.let(::putAll)
        }

        return mergedMap.takeIf { it.isNotEmpty() }
    }
}
