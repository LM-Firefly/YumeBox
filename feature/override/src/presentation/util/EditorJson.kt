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

import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.serialization.json.*

val OverrideEditorJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
}

fun encodeObjectList(value: List<Map<String, JsonElement>>?): String? {
    if (value.isNullOrEmpty()) return null
    return OverrideEditorJson.encodeToString(
        JsonElement.serializer(),
        JsonArray(
            value.map { fields ->
                JsonObject(toOrderedJsonElementMap(fields))
            },
        ),
    )
}

fun decodeObjectList(value: String?): List<Map<String, JsonElement>>? {
    if (value.isNullOrBlank()) return null
    return OverrideEditorJson.parseToJsonElement(value)
        .jsonArray
        .map { element -> orderedJsonObjectFields(element.jsonObject) }
        .ifEmpty { null }
}

fun encodeObjectMap(value: Map<String, Map<String, JsonElement>>?): String? {
    if (value.isNullOrEmpty()) return null
    return OverrideEditorJson.encodeToString(
        JsonElement.serializer(),
        JsonObject(
            LinkedHashMap<String, JsonElement>(value.size).apply {
                toOrderedObjectMap(value)?.forEach { (key, fields) ->
                    put(key, JsonObject(toOrderedJsonElementMap(fields)))
                }
            },
        ),
    )
}

fun decodeObjectMap(value: String?): Map<String, Map<String, JsonElement>>? {
    if (value.isNullOrBlank()) return null
    return OverrideEditorJson.parseToJsonElement(value)
        .jsonObject
        .let(::orderedObjectMapFromJson)
        .ifEmpty { null }
}

fun encodeSubRules(value: Map<String, List<String>>?): String? {
    if (value.isNullOrEmpty()) return null
    return OverrideEditorJson.encodeToString(
        JsonElement.serializer(),
        JsonObject(
            LinkedHashMap<String, JsonElement>(value.size).apply {
                toOrderedSubRuleMap(value)?.forEach { (key, rules) ->
                    put(key, JsonArray(rules.map(::JsonPrimitive)))
                }
            },
        ),
    )
}

fun decodeSubRules(value: String?): Map<String, List<String>>? {
    if (value.isNullOrBlank()) return null
    return OverrideEditorJson.parseToJsonElement(value)
        .jsonObject
        .let(::orderedSubRuleMapFromJson)
        .ifEmpty { null }
}

fun encodeObjectFields(value: Map<String, JsonElement>?): String? {
    if (value.isNullOrEmpty()) return null
    return OverrideEditorJson.encodeToString(
        JsonElement.serializer(),
        JsonObject(toOrderedJsonElementMap(value)),
    )
}

fun decodeObjectFields(value: String?): Map<String, JsonElement>? {
    if (value.isNullOrBlank()) return null
    return OverrideEditorJson.parseToJsonElement(value)
        .jsonObject
        .let(::orderedJsonObjectFields)
        .ifEmpty { null }
}

private fun orderedJsonObjectFields(value: JsonObject): LinkedHashMap<String, JsonElement> {
    val orderedMap = LinkedHashMap<String, JsonElement>(value.size)
    value.forEach { (key, element) ->
        orderedMap[key] = element
    }
    return orderedMap
}

private fun orderedObjectMapFromJson(
    value: JsonObject,
): LinkedHashMap<String, Map<String, JsonElement>> {
    val orderedMap = LinkedHashMap<String, Map<String, JsonElement>>(value.size)
    value.forEach { (key, element) ->
        orderedMap[key] = orderedJsonObjectFields(element.jsonObject)
    }
    return orderedMap
}

private fun orderedSubRuleMapFromJson(
    value: JsonObject,
): LinkedHashMap<String, List<String>> {
    val orderedMap = LinkedHashMap<String, List<String>>(value.size)
    value.forEach { (key, element) ->
        orderedMap[key] = element.jsonArray.map { it.jsonPrimitive.content }
    }
    return orderedMap
}

fun jsonElementToEditorValue(element: JsonElement): String {
    return when (element) {
        is JsonPrimitive -> {
            if (element.isString) {
                element.content
            } else {
                element.toString()
            }
        }

        else -> OverrideEditorJson.encodeToString(JsonElement.serializer(), element)
    }
}

fun editorValueToJsonElement(rawValue: String): JsonElement {
    val trimmedValue = rawValue.trim()
    if (trimmedValue.isEmpty()) {
        return JsonPrimitive("")
    }
    if (trimmedValue == "null") {
        return JsonNull
    }
    val parsedElement = runCatching {
        OverrideEditorJson.parseToJsonElement(trimmedValue)
    }.getOrNull()
    if (parsedElement != null) {
        return parsedElement
    }

    val primitive = JsonPrimitive(trimmedValue)
    return when {
        primitive.booleanOrNull != null -> JsonPrimitive(primitive.booleanOrNull!!)
        primitive.intOrNull != null -> JsonPrimitive(primitive.intOrNull!!)
        primitive.doubleOrNull != null -> JsonPrimitive(primitive.doubleOrNull!!)
        else -> JsonPrimitive(trimmedValue)
    }
}

enum class OverrideExtraFieldValueType {
    String,
    Boolean,
    Int,
    Double,
    Null,
    JsonFragment,
}

data class OverrideExtraFieldDraft(
    val key: String = "",
    val valueType: OverrideExtraFieldValueType = OverrideExtraFieldValueType.String,
    val value: String = "",
)

fun jsonElementToExtraFieldDraft(
    key: String,
    value: JsonElement,
): OverrideExtraFieldDraft {
    return when (value) {
        JsonNull -> OverrideExtraFieldDraft(
            key = key,
            valueType = OverrideExtraFieldValueType.Null,
        )

        is JsonPrimitive -> when {
            value.isString -> OverrideExtraFieldDraft(
                key = key,
                valueType = OverrideExtraFieldValueType.String,
                value = value.content,
            )

            value.booleanOrNull != null -> OverrideExtraFieldDraft(
                key = key,
                valueType = OverrideExtraFieldValueType.Boolean,
                value = value.content,
            )

            value.intOrNull != null -> OverrideExtraFieldDraft(
                key = key,
                valueType = OverrideExtraFieldValueType.Int,
                value = value.content,
            )

            value.doubleOrNull != null -> OverrideExtraFieldDraft(
                key = key,
                valueType = OverrideExtraFieldValueType.Double,
                value = value.content,
            )

            else -> OverrideExtraFieldDraft(
                key = key,
                valueType = OverrideExtraFieldValueType.String,
                value = value.content,
            )
        }

        else -> OverrideExtraFieldDraft(
            key = key,
            valueType = OverrideExtraFieldValueType.JsonFragment,
            value = OverrideEditorJson.encodeToString(JsonElement.serializer(), value),
        )
    }
}

fun extraFieldDraftToJsonElement(draft: OverrideExtraFieldDraft): JsonElement? {
    return when (draft.valueType) {
        OverrideExtraFieldValueType.String -> JsonPrimitive(draft.value)
        OverrideExtraFieldValueType.Boolean -> draft.value.trim().toBooleanStrictOrNull()?.let(::JsonPrimitive)
        OverrideExtraFieldValueType.Int -> draft.value.trim().toIntOrNull()?.let(::JsonPrimitive)
        OverrideExtraFieldValueType.Double -> draft.value.trim().toDoubleOrNull()?.let(::JsonPrimitive)
        OverrideExtraFieldValueType.Null -> JsonNull
        OverrideExtraFieldValueType.JsonFragment -> runCatching {
            OverrideEditorJson.parseToJsonElement(draft.value.trim())
        }.getOrNull()
    }
}

fun summarizeExtraFieldValue(element: JsonElement): String {
    return when (element) {
        JsonNull -> "Null"
        is JsonPrimitive -> when {
            element.isString -> element.content.ifBlank { MLang.Override.Editor.EmptyString }
            else -> element.content
        }

        is JsonArray -> MLang.Override.Editor.ArrayItems.format(element.size)
        is JsonObject -> MLang.Override.Editor.ObjectFields.format(element.size)
    }
}
