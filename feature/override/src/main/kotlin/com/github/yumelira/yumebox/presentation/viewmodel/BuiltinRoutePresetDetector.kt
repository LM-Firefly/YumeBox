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

package com.github.yumelira.yumebox.presentation.viewmodel

import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import timber.log.Timber

internal class BuiltinRoutePresetDetector(private val json: Json) {

    private data class RouteSectionSnapshot(
        val ruleProviders: Map<String, Map<String, JsonElement>>?,
        val proxyGroups: List<Map<String, JsonElement>>?,
        val prependRules: List<String>?,
        val rules: List<String>?,
        val subRules: Map<String, List<String>>?,
    )

    private val cache = mutableMapOf<BuiltinRoutePreset, RouteSectionSnapshot?>()

    fun detect(config: ConfigurationOverride): BuiltinRoutePreset {
        val current = config.toRouteSnapshot()

        val presetA = loadPresetSnapshot(BuiltinRoutePreset.PresetA)
        if (presetA != null && current == presetA) return BuiltinRoutePreset.PresetA

        val presetB = loadPresetSnapshot(BuiltinRoutePreset.PresetB)
        if (presetB != null && current == presetB) return BuiltinRoutePreset.PresetB

        return BuiltinRoutePreset.None
    }

    private fun loadPresetSnapshot(preset: BuiltinRoutePreset): RouteSectionSnapshot? {
        if (preset == BuiltinRoutePreset.None) return null
        if (cache.containsKey(preset)) return cache[preset]

        val fileName = when (preset) {
            BuiltinRoutePreset.PresetA -> "override.json"
            BuiltinRoutePreset.PresetB -> "override2.json"
            BuiltinRoutePreset.None -> return null
        }

        val snapshot = runCatching {
            val text = loadResourceText(fileName) ?: return@runCatching null
            val config = try {
                json.decodeFromString(ConfigurationOverride.serializer(), text)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse preset snapshot: $fileName")
                return@runCatching null
            }
            config.toRouteSnapshot()
        }.getOrNull()

        cache[preset] = snapshot
        return snapshot
    }

    private fun ConfigurationOverride.toRouteSnapshot(): RouteSectionSnapshot {
        return RouteSectionSnapshot(
            ruleProviders = ruleProviders,
            proxyGroups = proxyGroups,
            prependRules = prependRules,
            rules = rules,
            subRules = subRules,
        )
    }

    private fun loadResourceText(fileName: String): String? {
        val loader = javaClass.classLoader ?: Thread.currentThread().contextClassLoader ?: return null
        return loader.getResourceAsStream(fileName)
            ?.bufferedReader()
            ?.use { it.readText() }
    }
}
