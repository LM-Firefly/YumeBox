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

package com.github.yumelira.yumebox.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

private val configurationOverrideJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

fun encodeConfigurationOverride(config: ConfigurationOverride): String {
    val element = configurationOverrideJson.encodeToJsonElement(ConfigurationOverride.serializer(), config)
    val cleaned = pruneConfigurationOverrideJson(element) ?: JsonObject(emptyMap())
    return configurationOverrideJson.encodeToString(JsonElement.serializer(), cleaned)
}

private fun pruneConfigurationOverrideJson(element: JsonElement): JsonElement? {
    return when (element) {
        JsonNull -> null
        is JsonObject -> {
            val cleaned = element.entries
                .mapNotNull { (key, value) -> pruneConfigurationOverrideJson(value)?.let { key to it } }
                .toMap()
            if (cleaned.isEmpty()) null else JsonObject(cleaned)
        }

        is JsonArray -> JsonArray(element.mapNotNull(::pruneConfigurationOverrideJson))
        else -> element
    }
}
