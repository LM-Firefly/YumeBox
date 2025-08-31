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



package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Badge-plus`
import com.github.yumelira.yumebox.presentation.icon.yume.Delete
import com.github.yumelira.yumebox.presentation.util.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.WindowDropdown
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OverrideExtraFieldsCard(
    title: String,
    fields: Map<String, JsonElement>,
    onAddClick: () -> Unit,
    onEditClick: (String, JsonElement) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicComponent(
                title = title,
                summary = if (fields.isEmpty()) {
                    "点击添加额外字段"
                } else {
                    "已配置 ${fields.size} 个额外字段"
                },
                endActions = {
                    OverrideCardActionIconButton(
                        imageVector = Yume.`Badge-plus`,
                        contentDescription = "新增额外字段",
                        onClick = onAddClick,
                        tone = OverrideActionTone.Primary,
                    )
                },
                onClick = onAddClick,
            )

            if (fields.isNotEmpty()) {
                fields.entries.forEach { entry ->
                    BasicComponent(
                        title = entry.key,
                        summary = "${resolveValueTypeLabel(entry.value)} · ${summarizeExtraFieldValue(entry.value)}",
                        onClick = { onEditClick(entry.key, entry.value) },
                        endActions = {
                            OverrideCardActionIconButton(
                                imageVector = Yume.Delete,
                                contentDescription = "删除额外字段",
                                onClick = { onDeleteClick(entry.key) },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun OverrideExtraFieldDialog(
    show: Boolean,
    title: String,
    initialValue: OverrideExtraFieldDraft?,
    onConfirm: (OverrideExtraFieldDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) {
        return
    }

    var keyText by remember(show, initialValue) { mutableStateOf(initialValue?.key.orEmpty()) }
    var valueText by remember(show, initialValue) { mutableStateOf(initialValue?.value.orEmpty()) }
    var selectedType by remember(show, initialValue) {
        mutableStateOf(initialValue?.valueType ?: OverrideExtraFieldValueType.String)
    }
    var errorText by remember(show, initialValue) { mutableStateOf<String?>(null) }
    val valueTypeItems = OverrideExtraFieldValueType.entries.map(::resolveValueTypeLabel)
    val selectedTypeIndex = OverrideExtraFieldValueType.entries.indexOf(selectedType).coerceAtLeast(0)

    AppDialog(
        show = show,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WindowDropdown(
                title = "值类型",
                items = valueTypeItems,
                selectedIndex = selectedTypeIndex,
                onSelectedIndexChange = { index ->
                    selectedType = OverrideExtraFieldValueType.entries.getOrElse(index) { selectedType }
                    errorText = null
                },
            )
            TextField(
                value = keyText,
                onValueChange = {
                    keyText = it
                    errorText = null
                },
                label = "键名",
                modifier = Modifier.fillMaxWidth(),
            )
            if (selectedType != OverrideExtraFieldValueType.Null) {
                TextField(
                    value = valueText,
                    onValueChange = {
                        valueText = it
                        errorText = null
                    },
                    label = when (selectedType) {
                        OverrideExtraFieldValueType.String -> "字符串值"
                        OverrideExtraFieldValueType.Boolean -> "true / false"
                        OverrideExtraFieldValueType.Int -> "整数值"
                        OverrideExtraFieldValueType.Double -> "浮点数值"
                        OverrideExtraFieldValueType.Null -> ""
                        OverrideExtraFieldValueType.JsonFragment -> "单个 JSON 片段"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (selectedType == OverrideExtraFieldValueType.JsonFragment) 140.dp else 0.dp),
                    maxLines = if (selectedType == OverrideExtraFieldValueType.JsonFragment) 12 else 1,
                )
            }
            errorText?.let { message ->
                OverrideFieldAssistText(
                    text = message,
                    color = MiuixTheme.colorScheme.error,
                )
            }
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = {
                    val normalizedDraft = OverrideExtraFieldDraft(
                        key = keyText.trim(),
                        valueType = selectedType,
                        value = valueText.trim(),
                    )
                    if (normalizedDraft.key.isBlank()) {
                        errorText = "键名不能为空"
                        return@DialogButtonRow
                    }
                    val parsedValue = extraFieldDraftToJsonElement(normalizedDraft)
                    if (parsedValue == null) {
                        errorText = "当前值与所选类型不匹配"
                        return@DialogButtonRow
                    }
                    onConfirm(normalizedDraft)
                },
                cancelText = "取消",
                confirmText = "确定",
            )
        }
    }
}

fun Map<String, JsonElement>.updateExtraField(
    originalKey: String?,
    draft: OverrideExtraFieldDraft,
): Map<String, JsonElement> {
    val parsedValue = extraFieldDraftToJsonElement(draft) ?: return this
    val updatedFields = LinkedHashMap(this)
    if (originalKey != null && originalKey != draft.key) {
        updatedFields.remove(originalKey)
    }
    updatedFields[draft.key] = parsedValue
    return updatedFields
}

fun Map<String, JsonElement>.toExtraFieldDraft(key: String): OverrideExtraFieldDraft? {
    val value = get(key) ?: return null
    return jsonElementToExtraFieldDraft(key, value)
}

private fun resolveValueTypeLabel(value: JsonElement): String {
    return when (value) {
        is kotlinx.serialization.json.JsonNull -> resolveValueTypeLabel(OverrideExtraFieldValueType.Null)
        is kotlinx.serialization.json.JsonPrimitive -> when {
            value.isString -> resolveValueTypeLabel(OverrideExtraFieldValueType.String)
            value.booleanOrNull != null -> resolveValueTypeLabel(OverrideExtraFieldValueType.Boolean)
            value.intOrNull != null -> resolveValueTypeLabel(OverrideExtraFieldValueType.Int)
            value.doubleOrNull != null -> resolveValueTypeLabel(OverrideExtraFieldValueType.Double)
            else -> resolveValueTypeLabel(OverrideExtraFieldValueType.String)
        }

        else -> resolveValueTypeLabel(OverrideExtraFieldValueType.JsonFragment)
    }
}

private fun resolveValueTypeLabel(valueType: OverrideExtraFieldValueType): String {
    return when (valueType) {
        OverrideExtraFieldValueType.String -> "String"
        OverrideExtraFieldValueType.Boolean -> "Boolean"
        OverrideExtraFieldValueType.Int -> "Int"
        OverrideExtraFieldValueType.Double -> "Double"
        OverrideExtraFieldValueType.Null -> "Null"
        OverrideExtraFieldValueType.JsonFragment -> "JsonFragment"
    }
}
