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


package com.github.yumelira.yumebox.presentation.screen
import com.github.yumelira.yumebox.presentation.theme.UiDp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Badge-plus`
import com.github.yumelira.yumebox.presentation.icon.yume.Delete
import com.github.yumelira.yumebox.presentation.icon.yume.Undo
import com.github.yumelira.yumebox.presentation.util.OverrideListEditorMode
import com.github.yumelira.yumebox.presentation.util.OverrideListModeValues
import com.github.yumelira.yumebox.presentation.util.OverrideStructuredEditorStore
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OverrideStringListEditorScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val title = OverrideStructuredEditorStore.stringListEditorTitle.ifBlank { MLang.Override.Editor.List }
    val placeholder = OverrideStructuredEditorStore.stringListEditorPlaceholder
    val availableModes = OverrideStructuredEditorStore.stringListEditorAvailableModes
    var showItemDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var currentDraftValue by remember { mutableStateOf("") }
    val addFabController = rememberOverrideFabController()
    val selectedMode = OverrideStructuredEditorStore.stringListEditorSelectedMode
    val editorValues = remember(OverrideStructuredEditorStore.stringListEditorValues) {
        OverrideListModeValues(
            replaceValue = OverrideStructuredEditorStore.stringListEditorValues.replaceValue?.toList(),
            startValue = OverrideStructuredEditorStore.stringListEditorValues.startValue?.toList(),
            endValue = OverrideStructuredEditorStore.stringListEditorValues.endValue?.toList(),
        )
    }
    val currentItems = editorValues.valueFor(selectedMode).orEmpty()
    val selectedModeIndex = availableModes.indexOf(selectedMode).coerceAtLeast(0)
    val showAddFab = !showItemDialog && !showResetDialog

    fun currentStringListValues(): OverrideListModeValues<List<String>> {
        val latestValues = OverrideStructuredEditorStore.stringListEditorValues
        return OverrideListModeValues(
            replaceValue = latestValues.replaceValue?.toList(),
            startValue = latestValues.startValue?.toList(),
            endValue = latestValues.endValue?.toList(),
        )
    }

    fun applyStringListValues(values: OverrideListModeValues<List<String>>) {
        OverrideStructuredEditorStore.applyStringListValues(
            OverrideListModeValues(
                replaceValue = values.replaceValue?.toList(),
                startValue = values.startValue?.toList(),
                endValue = values.endValue?.toList(),
            ),
        )
    }

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = addFabController,
                visible = showAddFab,
                imageVector = Yume.`Badge-plus`,
                contentDescription = MLang.Override.Editor.AddItem,
                onClick = {
                    editingIndex = -1
                    currentDraftValue = ""
                    showItemDialog = true
                },
            )
        },
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIconPadding = 0.dp,
                navigationIcon = { NavigationBackIcon(navigator = navigator) },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                    ) {
                        Icon(
                            imageVector = Yume.Undo,
                            contentDescription = MLang.Override.Editor.ClearCurrentMode,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
            lazyListState = listState,
            onScrollDirectionChanged = addFabController::onScrollDirectionChanged,
        ) {
            item {
                Card {
                    WindowDropdownPreference(
                        title = MLang.Override.Editor.Mode.Title,
                        items = availableModes.map(OverrideListEditorMode::label),
                        selectedIndex = selectedModeIndex,
                        onSelectedIndexChange = { index ->
                            val newMode = availableModes.getOrElse(index) { selectedMode }
                            OverrideStructuredEditorStore.updateStringListEditorSession(selectedMode = newMode)
                        },
                    )
                }
            }

            if (currentItems.isNotEmpty()) {
                itemsIndexed(
                    items = currentItems,
                    key = { index, itemValue -> "$index:$itemValue" },
                ) { index, itemValue ->
                    StringListEntryCard(
                        index = index + 1,
                        value = itemValue,
                        onEdit = {
                            editingIndex = index
                            currentDraftValue = itemValue
                            showItemDialog = true
                        },
                        onDelete = {
                            val mode = OverrideStructuredEditorStore.stringListEditorSelectedMode
                            val latestValues = currentStringListValues()
                            val updatedValues = latestValues.update(
                                mode,
                                latestValues.valueFor(mode).orEmpty().toMutableList().also { items ->
                                    items.removeAt(index)
                                },
                            )
                            applyStringListValues(updatedValues)
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(OverrideSectionBottomSpacing))
            }
        }

        StringListEntryDialog(
            show = showItemDialog,
            title = if (editingIndex >= 0) MLang.Override.Editor.EditItem else MLang.Override.Editor.AddItem,
            placeholder = placeholder,
            initialValue = currentDraftValue,
            onConfirm = { updatedValue ->
                val normalizedValue = updatedValue.trim()
                if (normalizedValue.isBlank()) {
                    return@StringListEntryDialog
                }
                val updatedItems = currentItems.toMutableList().also { items ->
                    if (editingIndex in items.indices) {
                        items[editingIndex] = normalizedValue
                    } else {
                        items.add(normalizedValue)
                    }
                }
                val mode = OverrideStructuredEditorStore.stringListEditorSelectedMode
                val updatedValues = currentStringListValues().update(mode, updatedItems)
                applyStringListValues(updatedValues)
                editingIndex = -1
                currentDraftValue = ""
                showItemDialog = false
            },
            onDismiss = {
                editingIndex = -1
                currentDraftValue = ""
                showItemDialog = false
            },
        )

        AppDialog(
            show = showResetDialog,
            title = MLang.Override.Editor.ClearCurrentMode,
            summary = MLang.Override.Editor.ClearDialog.Summary.format(MLang.Override.Editor.List),
            onDismissRequest = { showResetDialog = false },
        ) {
            DialogButtonRow(
                onCancel = { showResetDialog = false },
                onConfirm = {
                    showResetDialog = false
                    val mode = OverrideStructuredEditorStore.stringListEditorSelectedMode
                    applyStringListValues(currentStringListValues().update(mode, emptyList()))
                },
                cancelText = MLang.Override.Dialog.Button.Cancel,
                confirmText = MLang.Override.Editor.Clear,
            )
        }
    }
}

@Composable
private fun StringListEntryCard(
    index: Int,
    value: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.padding(top = UiDp.dp12),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(horizontal = UiDp.dp16, vertical = UiDp.dp14),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$index.",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.width(UiDp.dp40),
            )
            Text(
                text = value,
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = UiDp.dp8),
            )
            OverrideCardActionIconButton(
                imageVector = Yume.Delete,
                contentDescription = MLang.Override.Card.Delete,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun StringListEntryDialog(
    show: Boolean,
    title: String,
    placeholder: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) {
        return
    }

    var draftValue by remember(show, initialValue) { mutableStateOf(initialValue) }

    AppDialog(
        show = show,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
        ) {
            TextField(
                value = draftValue,
                onValueChange = { draftValue = it },
                label = placeholder,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(MLang.Override.Dialog.Button.Cancel)
                }
                Button(
                    onClick = { onConfirm(draftValue) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(
                        MLang.Override.Editor.Confirm,
                    color = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}
