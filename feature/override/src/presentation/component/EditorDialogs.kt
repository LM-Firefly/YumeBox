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
import com.github.yumelira.yumebox.presentation.theme.UiDp
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
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.serialization.json.JsonElement
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.UUID

@Composable
fun StringListEditorDialog(
    show: MutableState<Boolean>,
    title: String,
    placeholder: String,
    value: List<String>?,
    onValueChange: (List<String>?) -> Unit,
) {
    var editText by remember(title, value) { mutableStateOf(value?.joinToString("\n") ?: "") }

    AppTextFieldDialog(
        show = show.value,
        title = title,
        summary = MLang.Override.Editor.OneItemPerLine,
        value = editText,
        onValueChange = { editText = it },
        label = placeholder,
        singleLine = false,
        maxLines = 20,
        onDismissRequest = { show.value = false },
        onConfirm = {
            val lines = editText.lines().filter { it.isNotBlank() }
            onValueChange(lines.ifEmpty { null })
            show.value = false
        },
    )
}

@Composable
fun JsonTextEditorDialog(
    show: Boolean,
    title: String,
    placeholder: String,
    value: String?,
    onValueChange: (String?) -> Unit,
    onDismiss: () -> Unit,
) {

    com.github.yumelira.yumebox.feature.editor.api.component.JsonEditorDialog(
        show = show,
        title = title,
        subtitle = MLang.Override.Editor.JsonBlockSubtitle,
        value = value,
        onValueChange = onValueChange,
        onDismiss = onDismiss,
    )
}

@Composable
fun StringMapEditorDialog(
    show: Boolean,
    title: String,
    keyPlaceholder: String,
    valuePlaceholder: String,
    value: Map<String, String>?,
    onValueChange: (Map<String, String>?) -> Unit,
    onDismiss: () -> Unit,
) {
    val entries = remember { mutableStateListOf<Pair<String, String>>() }
    val itemKeys = remember { mutableStateListOf<String>() }

    LaunchedEffect(value) {
        entries.clear()
        itemKeys.clear()
        value?.forEach { (key, mapValue) ->
            entries.add(key to mapValue)
            itemKeys.add(newEditorItemKey())
        }
        if (entries.isEmpty()) {
            entries.add("" to "")
            itemKeys.add(newEditorItemKey())
        }
    }

    AppFormDialog(
        show = show,
        title = title,
        onConfirm = {
            val map = entries
                .filter { it.first.isNotBlank() }
                .associate { it.first to it.second }
            onValueChange(map.ifEmpty { null })
            onDismiss()
        },
        onDismissRequest = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = UiDp.dp320),
        ) {
            itemsIndexed(entries, key = { index, _ -> itemKeys[index] }) { index, entry ->
                StringMapEntryItem(
                    key = entry.first,
                    value = entry.second,
                    keyPlaceholder = keyPlaceholder,
                    valuePlaceholder = valuePlaceholder,
                    onKeyChange = { updatedKey ->
                        entries[index] = updatedKey to entry.second
                    },
                    onValueChange = { updatedValue ->
                        entries[index] = entry.first to updatedValue
                    },
                )
                Spacer(modifier = Modifier.height(UiDp.dp8))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    entries.add("" to "")
                    itemKeys.add(newEditorItemKey())
                },
            ) {
                Text(MLang.Override.Editor.AddItem)
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (entries.size > 1) {
                        entries.removeLast()
                        itemKeys.removeLast()
                    } else {
                        entries[0] = "" to ""
                    }
                },
            ) {
                Text(MLang.Override.Editor.DeleteLastItem)
            }
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
    val itemKeys = remember { mutableStateListOf<String>() }
    var editingIndex by remember { mutableIntStateOf(-1) }
    val showItemEditor = remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        drafts.clear()
        itemKeys.clear()
        value?.forEach { drafts.add(it) }
        repeat(drafts.size) { itemKeys.add(newEditorItemKey()) }
    }

    AppDialog(
        show = show.value,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier = Modifier.padding(UiDp.dp20),
        ) {
            Text(
                text = MLang.Override.Editor.ObjectListHint,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(UiDp.dp12))
            LazyColumn(
                modifier = Modifier.heightIn(max = UiDp.dp360),
                verticalArrangement = Arrangement.spacedBy(UiDp.dp8),
            ) {
                itemsIndexed(drafts, key = { index, _ -> itemKeys[index] }) { index, fields ->
                    Card(
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(UiDp.dp12),
                    ) {
                        Text(
                            text = objectCardTitle(fields, MLang.Override.Editor.ObjectFallbackTitle.format(index + 1)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = objectCardSubtitle(fields),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(top = UiDp.dp4),
                        )
                        Spacer(modifier = Modifier.height(UiDp.dp10))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    editingIndex = index
                                    showItemEditor.value = true
                                },
                            ) {
                                Text(MLang.Override.Editor.Edit)
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    drafts.add(index + 1, toOrderedJsonElementMap(fields))
                                    itemKeys.add(index + 1, newEditorItemKey())
                                },
                            ) {
                                Text(MLang.Override.Editor.Copy)
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    drafts.removeAt(index)
                                    itemKeys.removeAt(index)
                                },
                            ) {
                                Text(MLang.Override.Card.Delete)
                            }
                        }
                        Spacer(modifier = Modifier.height(UiDp.dp8))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = index > 0,
                                onClick = {
                                    moveItem(drafts, index, index - 1)
                                    moveStringItem(itemKeys, index, index - 1)
                                },
                            ) {
                                Text(MLang.Override.Editor.MoveUp)
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                enabled = index < drafts.lastIndex,
                                onClick = {
                                    moveItem(drafts, index, index + 1)
                                    moveStringItem(itemKeys, index, index + 1)
                                },
                            ) {
                                Text(MLang.Override.Editor.MoveDown)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(UiDp.dp12))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    drafts.add(emptyMap())
                    itemKeys.add(newEditorItemKey())
                    editingIndex = drafts.lastIndex
                    showItemEditor.value = true
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = MLang.Override.Editor.AddObject,
                        color = MiuixTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.height(UiDp.dp24))
            DialogButtonRow(
                onCancel = { show.value = false },
                onConfirm = {
                    onValueChange(drafts.toList().ifEmpty { null })
                    show.value = false
                },
                cancelText = MLang.Override.Dialog.Button.Cancel,
                confirmText = MLang.Override.Editor.Confirm,
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
    val itemKeys = remember { mutableStateListOf<String>() }
    var editingIndex by remember { mutableIntStateOf(-1) }
    val showItemEditor = remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        drafts.clear()
        itemKeys.clear()
        value?.forEach { (key, fields) -> drafts.add(key to fields) }
        repeat(drafts.size) { itemKeys.add(newEditorItemKey()) }
    }

    AppDialog(
        show = show.value,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier = Modifier.padding(UiDp.dp20),
        ) {
            Text(
                text = MLang.Override.Editor.ProviderMapHint,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(UiDp.dp12))
            LazyColumn(
                modifier = Modifier.heightIn(max = UiDp.dp360),
                verticalArrangement = Arrangement.spacedBy(UiDp.dp8),
            ) {
                itemsIndexed(drafts, key = { index, _ -> itemKeys[index] }) { index, draft ->
                    Card(
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(UiDp.dp12),
                    ) {
                        Text(
                            text = draft.first.ifBlank { MLang.Override.Editor.UnnamedProvider },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = objectCardSubtitle(draft.second),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(top = UiDp.dp4),
                        )
                        Spacer(modifier = Modifier.height(UiDp.dp10))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    editingIndex = index
                                    showItemEditor.value = true
                                },
                            ) {
                                Text(MLang.Override.Editor.Edit)
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    drafts.removeAt(index)
                                    itemKeys.removeAt(index)
                                },
                            ) {
                                Text(MLang.Override.Card.Delete)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(UiDp.dp12))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    drafts.add("" to emptyMap())
                    itemKeys.add(newEditorItemKey())
                    editingIndex = drafts.lastIndex
                    showItemEditor.value = true
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = MLang.Override.Editor.NewProvider,
                        color = MiuixTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.height(UiDp.dp24))
            DialogButtonRow(
                onCancel = { show.value = false },
                onConfirm = {
                    val mappedValue = drafts
                        .filter { it.first.isNotBlank() }
                        .associate { it.first to it.second }
                    onValueChange(mappedValue.ifEmpty { null })
                    show.value = false
                },
                cancelText = MLang.Override.Dialog.Button.Cancel,
                confirmText = MLang.Override.Editor.Confirm,
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
    val itemKeys = remember { mutableStateListOf<String>() }
    val showRulesEditor = remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(value) {
        drafts.clear()
        itemKeys.clear()
        value?.forEach { (key, rules) ->
            drafts.add(key to rules)
            itemKeys.add(newEditorItemKey())
        }
        if (drafts.isEmpty()) {
            drafts.add("" to emptyList())
            itemKeys.add(newEditorItemKey())
        }
    }

    AppDialog(
        show = show.value,
        title = title,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier = Modifier.padding(UiDp.dp20),
        ) {
            Text(
                text = MLang.Override.Editor.SubRuleGroupHint,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(UiDp.dp12))
            LazyColumn(
                modifier = Modifier.heightIn(max = UiDp.dp360),
                verticalArrangement = Arrangement.spacedBy(UiDp.dp8),
            ) {
                itemsIndexed(drafts, key = { index, _ -> itemKeys[index] }) { index, draft ->
                    SubRuleEntryCard(
                        name = draft.first,
                        rulesCount = draft.second.size,
                        onNameChange = { updatedKey -> drafts[index] = updatedKey to draft.second },
                        onEditRules = {
                            editingIndex = index
                            showRulesEditor.value = true
                        },
                        onDelete = {
                            if (drafts.size > 1) {
                                drafts.removeAt(index)
                                itemKeys.removeAt(index)
                            } else {
                                drafts[0] = "" to emptyList()
                            }
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiDp.dp12))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    drafts.add("" to emptyList())
                    itemKeys.add(newEditorItemKey())
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = MLang.Override.Editor.AddSubRuleGroup,
                        color = MiuixTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.height(UiDp.dp24))
            DialogButtonRow(
                onCancel = { show.value = false },
                onConfirm = {
                    val mappedValue = drafts
                        .filter { it.first.isNotBlank() }
                        .associate { it.first to it.second }
                    onValueChange(mappedValue.ifEmpty { null })
                    show.value = false
                },
                cancelText = MLang.Override.Dialog.Button.Cancel,
                confirmText = MLang.Override.Editor.Confirm,
            )
        }
    }

    StringListEditorDialog(
        show = showRulesEditor,
        title = drafts.getOrNull(editingIndex)?.first?.ifBlank { MLang.Override.Editor.EditSubRule } ?: MLang.Override.Editor.EditSubRule,
        placeholder = MLang.Override.Editor.RulePlaceholder,
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

    AppFormDialog(
        show = show.value,
        title = title,
        summary = MLang.Override.Editor.ObjectFieldHint,
        onConfirm = {
            val fields = decodeObjectFields(rawJson).orEmpty()
            onConfirm(
                initialKey?.let { keyText.takeIf(String::isNotBlank) ?: "" },
                fields,
            )
            show.value = false
        },
        onDismissRequest = { show.value = false },
    ) {
        if (initialKey != null) {
            TextField(
                value = keyText,
                onValueChange = { keyText = it },
                label = MLang.Override.Editor.KeyName,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextField(
            value = rawJson,
            onValueChange = { rawJson = it },
            label = MLang.Override.Editor.ObjectJsonPlaceholder,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = UiDp.dp220),
            maxLines = 30,
        )
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
    val keyCountText = MLang.Override.Editor.ObjectFieldCount.format(fields.size)
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

private fun moveStringItem(
    drafts: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    fromIndex: Int,
    toIndex: Int,
) {
    val item = drafts.removeAt(fromIndex)
    drafts.add(toIndex, item)
}

private fun newEditorItemKey(): String = UUID.randomUUID().toString()

@Composable
private fun StringMapEntryItem(
    key: String,
    value: String,
    keyPlaceholder: String,
    valuePlaceholder: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
) {
    var keyText by remember(key) { mutableStateOf(key) }
    var valueText by remember(value) { mutableStateOf(value) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
    ) {
        TextField(
            value = keyText,
            onValueChange = {
                keyText = it
                onKeyChange(it)
            },
            label = keyPlaceholder,
            modifier = Modifier.weight(1f),
        )
        TextField(
            value = valueText,
            onValueChange = {
                valueText = it
                onValueChange(it)
            },
            label = valuePlaceholder,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SubRuleEntryCard(
    name: String,
    rulesCount: Int,
    onNameChange: (String) -> Unit,
    onEditRules: () -> Unit,
    onDelete: () -> Unit,
) {
    var nameText by remember(name) { mutableStateOf(name) }

    Card(
        insideMargin = androidx.compose.foundation.layout.PaddingValues(UiDp.dp12),
    ) {
        TextField(
            value = nameText,
            onValueChange = {
                nameText = it
                onNameChange(it)
            },
            label = MLang.Override.Editor.SubRuleName,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(UiDp.dp8))
        Text(
            text = if (rulesCount == 0) MLang.Override.Draft.NoRules else MLang.Override.Editor.RulesConfiguredInline.format(rulesCount),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.height(UiDp.dp8))
        Row(
            horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onEditRules,
            ) {
                Text(MLang.Override.Editor.EditRule)
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onDelete,
            ) {
                Text(MLang.Override.Card.Delete)
            }
        }
    }
}
