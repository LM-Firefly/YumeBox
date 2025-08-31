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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.presentation.util.decodeObjectFields
import com.github.yumelira.yumebox.presentation.util.encodeObjectFields
import com.github.yumelira.yumebox.presentation.util.jsonElementToEditorValue
import com.github.yumelira.yumebox.presentation.util.toOrderedJsonElementMap
import kotlinx.serialization.json.JsonElement
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun StringListEditorDialog(
    show: MutableState<Boolean>,
    title: String,
    placeholder: String,
    value: List<String>?,
    onValueChange: (List<String>?) -> Unit,
) {
    var editText by remember(title, value) { mutableStateOf(value?.joinToString("\n") ?: "") }

    AppDialog(
        show = show.value,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = "每行一个条目",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = editText,
                onValueChange = { editText = it },
                label = placeholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 20,
            )
            Spacer(modifier = Modifier.height(24.dp))
            DialogButtonRow(
                onCancel = { show.value = false },
                onConfirm = {
                    val lines = editText.lines().filter { it.isNotBlank() }
                    onValueChange(lines.ifEmpty { null })
                    show.value = false
                },
                cancelText = "取消",
                confirmText = "确定",
            )
        }
    }
}

@Composable
fun JsonTextEditorDialog(
    show: MutableState<Boolean>,
    title: String,
    placeholder: String,
    value: String?,
    onValueChange: (String?) -> Unit,
) {

    com.github.yumelira.yumebox.feature.editor.component.JsonEditorDialog(
        show = show.value,
        title = title,
        subtitle = "使用 JSON 格式编辑该配置块",
        value = value,
        onValueChange = onValueChange,
        onDismiss = { show.value = false },
    )
}

@Composable
fun StringMapEditorDialog(
    show: MutableState<Boolean>,
    title: String,
    keyPlaceholder: String,
    valuePlaceholder: String,
    value: Map<String, String>?,
    onValueChange: (Map<String, String>?) -> Unit,
) {
    val entries = remember { mutableStateListOf<Pair<String, String>>() }

    LaunchedEffect(value) {
        entries.clear()
        value?.forEach { (key, mapValue) -> entries.add(key to mapValue) }
        if (entries.isEmpty()) {
            entries.add("" to "")
        }
    }

    AppDialog(
        show = show.value,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                itemsIndexed(entries) { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextField(
                            value = entry.first,
                            onValueChange = { updatedKey -> entries[index] = updatedKey to entry.second },
                            label = keyPlaceholder,
                            modifier = Modifier.weight(1f),
                        )
                        TextField(
                            value = entry.second,
                            onValueChange = { updatedValue -> entries[index] = entry.first to updatedValue },
                            label = valuePlaceholder,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { entries.add("" to "") },
                ) {
                    Text("新增一项")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (entries.size > 1) {
                            entries.removeLast()
                        } else {
                            entries[0] = "" to ""
                        }
                    },
                ) {
                    Text("删除最后一项")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            DialogButtonRow(
                onCancel = { show.value = false },
                onConfirm = {
                    val map = entries
                        .filter { it.first.isNotBlank() }
                        .associate { it.first to it.second }
                    onValueChange(map.ifEmpty { null })
                    show.value = false
                },
                cancelText = "取消",
                confirmText = "确定",
            )
        }
    }
}

@Composable
fun JsonObjectListEditorDialog(
    show: MutableState<Boolean>,
    title: String,
    value: List<Map<String, JsonElement>>?,
    onValueChange: (List<Map<String, JsonElement>>?) -> Unit,
) {
    val drafts = remember { mutableStateListOf<Map<String, JsonElement>>() }
    var editingIndex by remember { mutableIntStateOf(-1) }
    val showItemEditor = remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        drafts.clear()
        value?.forEach { drafts.add(it) }
    }

    AppDialog(
        show = show.value,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = "结构化编辑对象列表，字段值支持字符串、数字、布尔和 JSON 片段。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(drafts) { index, fields ->
                    Card(
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    ) {
                        Text(
                            text = objectCardTitle(fields, "对象 ${index + 1}"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = objectCardSubtitle(fields),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    editingIndex = index
                                    showItemEditor.value = true
                                },
                            ) {
                                Text("编辑")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    drafts.add(index + 1, toOrderedJsonElementMap(fields))
                                },
                            ) {
                                Text("复制")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    drafts.removeAt(index)
                                },
                            ) {
                                Text("删除")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = index > 0,
                                onClick = {
                                    moveItem(drafts, index, index - 1)
                                },
                            ) {
                                Text("上移")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = index < drafts.lastIndex,
                                onClick = {
                                    moveItem(drafts, index, index + 1)
                                },
                            ) {
                                Text("下移")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    drafts.add(emptyMap())
                    editingIndex = drafts.lastIndex
                    showItemEditor.value = true
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = "新增对象",
                        color = MiuixTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            DialogButtonRow(
                onCancel = { show.value = false },
                onConfirm = {
                    onValueChange(drafts.toList().ifEmpty { null })
                    show.value = false
                },
                cancelText = "取消",
                confirmText = "确定",
            )
        }
    }

    JsonObjectFieldsDialog(
        show = showItemEditor,
        title = title,
        initialKey = null,
        initialFields = drafts.getOrNull(editingIndex),
        onConfirm = { _, fields ->
            if (editingIndex in drafts.indices) {
                drafts[editingIndex] = fields
            }
        },
    )
}

@Composable
fun JsonObjectMapEditorDialog(
    show: MutableState<Boolean>,
    title: String,
    value: Map<String, Map<String, JsonElement>>?,
    onValueChange: (Map<String, Map<String, JsonElement>>?) -> Unit,
) {
    val drafts = remember { mutableStateListOf<Pair<String, Map<String, JsonElement>>>() }
    var editingIndex by remember { mutableIntStateOf(-1) }
    val showItemEditor = remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        drafts.clear()
        value?.forEach { (key, fields) -> drafts.add(key to fields) }
    }

    AppDialog(
        show = show.value,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = "结构化编辑 Provider 字典，同名键会覆盖旧值。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(drafts) { index, draft ->
                    Card(
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    ) {
                        Text(
                            text = draft.first.ifBlank { "未命名 Provider" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = objectCardSubtitle(draft.second),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    editingIndex = index
                                    showItemEditor.value = true
                                },
                            ) {
                                Text("编辑")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { drafts.removeAt(index) },
                            ) {
                                Text("删除")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    drafts.add("" to emptyMap())
                    editingIndex = drafts.lastIndex
                    showItemEditor.value = true
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = "新增 Provider",
                        color = MiuixTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            DialogButtonRow(
                onCancel = { show.value = false },
                onConfirm = {
                    val mappedValue = drafts
                        .filter { it.first.isNotBlank() }
                        .associate { it.first to it.second }
                    onValueChange(mappedValue.ifEmpty { null })
                    show.value = false
                },
                cancelText = "取消",
                confirmText = "确定",
            )
        }
    }

    JsonObjectFieldsDialog(
        show = showItemEditor,
        title = title,
        initialKey = drafts.getOrNull(editingIndex)?.first,
        initialFields = drafts.getOrNull(editingIndex)?.second,
        onConfirm = { key, fields ->
            if (editingIndex in drafts.indices && key != null) {
                drafts[editingIndex] = key to fields
            }
        },
    )
}

@Composable
fun SubRulesEditorDialog(
    show: MutableState<Boolean>,
    title: String,
    value: Map<String, List<String>>?,
    onValueChange: (Map<String, List<String>>?) -> Unit,
) {
    val drafts = remember { mutableStateListOf<Pair<String, List<String>>>() }
    val showRulesEditor = remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(value) {
        drafts.clear()
        value?.forEach { (key, rules) -> drafts.add(key to rules) }
        if (drafts.isEmpty()) {
            drafts.add("" to emptyList())
        }
    }

    AppDialog(
        show = show.value,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = "每个子规则组包含一个名称和一组规则。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(drafts) { index, draft ->
                    Card(
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    ) {
                        TextField(
                            value = draft.first,
                            onValueChange = { updatedKey -> drafts[index] = updatedKey to draft.second },
                            label = "子规则名称",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (draft.second.isEmpty()) "暂无规则" else "已配置 ${draft.second.size} 条规则",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    editingIndex = index
                                    showRulesEditor.value = true
                                },
                            ) {
                                Text("编辑规则")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (drafts.size > 1) {
                                        drafts.removeAt(index)
                                    } else {
                                        drafts[0] = "" to emptyList()
                                    }
                                },
                            ) {
                                Text("删除")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { drafts.add("" to emptyList()) },
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = "新增子规则组",
                        color = MiuixTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            DialogButtonRow(
                onCancel = { show.value = false },
                onConfirm = {
                    val mappedValue = drafts
                        .filter { it.first.isNotBlank() }
                        .associate { it.first to it.second }
                    onValueChange(mappedValue.ifEmpty { null })
                    show.value = false
                },
                cancelText = "取消",
                confirmText = "确定",
            )
        }
    }

    StringListEditorDialog(
        show = showRulesEditor,
        title = drafts.getOrNull(editingIndex)?.first?.ifBlank { "编辑子规则" } ?: "编辑子规则",
        placeholder = "DOMAIN-SUFFIX,example.com,DIRECT",
        value = drafts.getOrNull(editingIndex)?.second,
        onValueChange = { updatedRules ->
            if (editingIndex in drafts.indices) {
                drafts[editingIndex] = drafts[editingIndex].first to updatedRules.orEmpty()
            }
        },
    )
}

@Composable
private fun JsonObjectFieldsDialog(
    show: MutableState<Boolean>,
    title: String,
    initialKey: String?,
    initialFields: Map<String, JsonElement>?,
    onConfirm: (String?, Map<String, JsonElement>) -> Unit,
) {
    var keyText by remember(initialKey, initialFields) { mutableStateOf(initialKey ?: "") }
    var rawJson by remember(initialKey, initialFields) {
        mutableStateOf(encodeObjectFields(initialFields) ?: "{}")
    }

    AppDialog(
        show = show.value,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            if (initialKey != null) {
                TextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = "键名",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = "键值内容支持简单值和 JSON 结构。",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = rawJson,
                onValueChange = { rawJson = it },
                label = "{ \"name\": \"proxy\", \"type\": \"ss\" }",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
                maxLines = 30,
            )
            Spacer(modifier = Modifier.height(24.dp))
            DialogButtonRow(
                onCancel = { show.value = false },
                onConfirm = {
                    val fields = decodeObjectFields(rawJson).orEmpty()
                    onConfirm(
                        initialKey?.let { keyText.takeIf(String::isNotBlank) ?: "" },
                        fields,
                    )
                    show.value = false
                },
                cancelText = "取消",
                confirmText = "确定",
            )
        }
    }
}

private fun objectCardTitle(
    fields: Map<String, JsonElement>,
    fallbackTitle: String,
): String {
    val nameField = fields["name"]?.let(::jsonElementToEditorValue)
    return nameField?.takeIf(String::isNotBlank) ?: fallbackTitle
}

private fun objectCardSubtitle(fields: Map<String, JsonElement>): String {
    val typeField = fields["type"]?.let(::jsonElementToEditorValue)?.takeIf(String::isNotBlank)
    val keyCountText = "${fields.size} 个字段"
    return if (typeField != null) {
        "$typeField · $keyCountText"
    } else {
        keyCountText
    }
}

private fun moveItem(
    drafts: androidx.compose.runtime.snapshots.SnapshotStateList<Map<String, JsonElement>>,
    fromIndex: Int,
    toIndex: Int,
) {
    val item = drafts.removeAt(fromIndex)
    drafts.add(toIndex, item)
}
