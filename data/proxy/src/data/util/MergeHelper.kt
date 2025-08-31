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

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object MergeHelper {

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

    fun mergeProxyList(
        base: List<Map<String, JsonElement>>?,
        @Suppress("UNUSED_PARAMETER") start: List<Map<String, JsonElement>>?,
        replace: List<Map<String, JsonElement>>?,
        @Suppress("UNUSED_PARAMETER") end: List<Map<String, JsonElement>>?,
    ): List<Map<String, JsonElement>>? {
        val allProxies = buildList {
            base?.let(::addAll)
            replace?.let(::addAll)
        }

        return deduplicateByName(allProxies).takeIf { it.isNotEmpty() }
    }

    fun mergeProviderMap(
        base: Map<String, Map<String, JsonElement>>?,
        replace: Map<String, Map<String, JsonElement>>?,
        merge: Map<String, Map<String, JsonElement>>?,
    ): Map<String, Map<String, JsonElement>>? {
        val mergedMap = buildMap {
            base?.let(::putAll)
            replace?.let(::putAll)
            merge?.let(::putAll)
        }

        return mergedMap.takeIf { it.isNotEmpty() }
    }

    fun mergeProxyGroupList(
        base: List<Map<String, JsonElement>>?,
        @Suppress("UNUSED_PARAMETER") start: List<Map<String, JsonElement>>?,
        replace: List<Map<String, JsonElement>>?,
        @Suppress("UNUSED_PARAMETER") end: List<Map<String, JsonElement>>?,
    ): List<Map<String, JsonElement>>? {
        val allGroups = buildList {
            base?.let(::addAll)
            replace?.let(::addAll)
        }

        return deduplicateByName(allGroups).takeIf { it.isNotEmpty() }
    }

    private fun deduplicateByName(
        proxies: List<Map<String, JsonElement>>,
    ): List<Map<String, JsonElement>> {
        if (proxies.isEmpty()) {
            return emptyList()
        }

        val proxiesByName = linkedMapOf<String?, Map<String, JsonElement>>()
        var unnamedIndex = 0

        for (proxy in proxies) {
            val proxyName = extractName(proxy)
            if (proxyName == null) {
                proxiesByName["__unnamed_${unnamedIndex++}"] = proxy
            } else {
                proxiesByName[proxyName] = proxy
            }
        }

        return proxiesByName.values.toList()
    }

    private fun extractName(proxy: Map<String, JsonElement>): String? {
        val nameElement = proxy["name"] ?: return null
        return when (nameElement) {
            is JsonPrimitive -> nameElement.content
            is JsonObject -> null
            else -> null
        }
    }
}
