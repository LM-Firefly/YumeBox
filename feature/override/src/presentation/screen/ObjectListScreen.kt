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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.presentation.util.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.WindowDropdown
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val ObjectListSectionGap = 12.dp
private const val ObjectListReorderHeaderCount = 2

@Composable
fun OverrideObjectListEditorScreen(
    navigator: DestinationsNavigator,
    onOpenProxyDraftEditor: (
        title: String,
        initialValue: OverrideProxyDraft?,
        onConfirm: (OverrideProxyDraft) -> Unit,
    ) -> Unit,
    onOpenProxyGroupDraftEditor: (
        title: String,
        initialValue: OverrideProxyGroupDraft?,
        onConfirm: (OverrideProxyGroupDraft) -> Unit,
    ) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val editorType = OverrideStructuredEditorStore.objectEditorType
    val title = OverrideStructuredEditorStore.objectEditorTitle.ifBlank { editorType.title }
    val availableModes = OverrideStructuredEditorStore.objectEditorAvailableModes
    var showResetDialog by remember { mutableStateOf(false) }
    val addFabController = rememberOverrideFabController()
    var isDeleteMode by rememberSaveable { mutableStateOf(false) }
    var selectedUiIds by remember { mutableStateOf(emptySet<String>()) }
    val selectedMode = OverrideStructuredEditorStore.objectEditorSelectedMode

    val proxyModeValues = OverrideStructuredEditorStore.objectEditorProxyDraftValues
    val proxyGroupModeValues = OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues

    val selectedModeIndex = availableModes.indexOf(selectedMode).coerceAtLeast(0)
    val currentProxyDrafts: List<OverrideProxyDraft> = proxyModeValues.valueFor(selectedMode).orEmpty()
    val currentProxyGroupDrafts: List<OverrideProxyGroupDraft> = proxyGroupModeValues.valueFor(selectedMode).orEmpty()
    val currentItemCount = when (editorType) {
        OverrideStructuredObjectType.Proxies -> currentProxyDrafts.size
        OverrideStructuredObjectType.ProxyGroups -> currentProxyGroupDrafts.size
    }

    fun applyProxyValues(values: OverrideListModeValues<List<OverrideProxyDraft>>) {
        OverrideStructuredEditorStore.applyProxyDraftValues(values)
    }

    fun applyProxyGroupValues(values: OverrideListModeValues<List<OverrideProxyGroupDraft>>) {
        OverrideStructuredEditorStore.applyProxyGroupDraftValues(values)
    }

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = (from.index - ObjectListReorderHeaderCount).coerceAtLeast(0)
        val toIndex = (to.index - ObjectListReorderHeaderCount).coerceAtLeast(0)
        when (editorType) {
            OverrideStructuredObjectType.Proxies -> {
                val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                val latestValues = OverrideStructuredEditorStore.objectEditorProxyDraftValues
                val updatedValues = latestValues.update(
                    mode,
                    reorderDraftList(latestValues.valueFor(mode).orEmpty(), fromIndex, toIndex),
                )
                applyProxyValues(updatedValues)
            }

            OverrideStructuredObjectType.ProxyGroups -> {
                val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                val latestValues = OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues
                val updatedValues = latestValues.update(
                    mode,
                    reorderDraftList(latestValues.valueFor(mode).orEmpty(), fromIndex, toIndex),
                )
                applyProxyGroupValues(updatedValues)
            }
        }
        selectedUiIds = emptySet()
    }
    val showAddFab = !isDeleteMode && !showResetDialog

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = addFabController,
                visible = showAddFab,
                imageVector = Yume.`Badge-plus`,
                contentDescription = "新增${editorType.itemLabel}",
                onClick = {
                    when (editorType) {
                        OverrideStructuredObjectType.Proxies -> {
                            onOpenProxyDraftEditor(MLang.Override.Editor.NewProxyNode, null) { createdDraft ->
                                val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                                val latestValues = OverrideStructuredEditorStore.objectEditorProxyDraftValues
                                val updatedValues = latestValues.update(
                                    mode,
                                    latestValues.valueFor(mode).orEmpty().toMutableList().also { it.add(createdDraft) },
                                )
                                applyProxyValues(updatedValues)
                            }
                        }

                        OverrideStructuredObjectType.ProxyGroups -> {
                            onOpenProxyGroupDraftEditor(MLang.Override.Editor.NewProxyGroup, null) { createdDraft ->
                                val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                                val latestValues = OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues
                                val updatedValues = latestValues.update(
                                    mode,
                                    latestValues.valueFor(mode).orEmpty().toMutableList().also { it.add(createdDraft) },
                                )
                                applyProxyGroupValues(updatedValues)
                            }
                        }
                    }
                },
            )
        },
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (isDeleteMode) {
                        IconButton(
                            onClick = {
                                isDeleteMode = false
                                selectedUiIds = emptySet()
                            },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Icon(
                                imageVector = Yume.Cancel,
                                contentDescription = MLang.Override.Editor.CancelDelete,
                            )
                        }
                        IconButton(
                            onClick = {
                                if (selectedUiIds.isNotEmpty()) {
                                    when (editorType) {
                                        OverrideStructuredObjectType.Proxies -> {
                                            val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                                            val latestValues = OverrideStructuredEditorStore.objectEditorProxyDraftValues
                                            val updatedValues = latestValues.update(
                                                mode,
                                                latestValues.valueFor(mode).orEmpty().filterNot { it.uiId in selectedUiIds },
                                            )
                                            applyProxyValues(updatedValues)
                                        }

                                        OverrideStructuredObjectType.ProxyGroups -> {
                                            val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                                            val latestValues = OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues
                                            val updatedValues = latestValues.update(
                                                mode,
                                                latestValues.valueFor(mode).orEmpty().filterNot { it.uiId in selectedUiIds },
                                            )
                                            applyProxyGroupValues(updatedValues)
                                        }
                                    }
                                    selectedUiIds = emptySet()
                                    isDeleteMode = false
                                }
                            },
                            modifier = Modifier.padding(end = 24.dp),
                        ) {
                            Icon(
                                imageVector = Yume.Delete,
                                contentDescription = "删除已选${editorType.itemLabel}",
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Icon(
                                imageVector = Yume.Undo,
                                contentDescription = MLang.Override.Editor.ClearCurrentMode,
                            )
                        }
                        IconButton(
                            onClick = {
                                isDeleteMode = true
                                selectedUiIds = emptySet()
                            },
                            modifier = Modifier.padding(end = 24.dp),
                        ) {
                            Icon(
                                imageVector = Yume.Delete,
                                contentDescription = MLang.Override.Editor.EnterDeleteMode,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
            modifier = Modifier.fillMaxWidth(),
            topPadding = 20.dp,
            lazyListState = listState,
            onScrollDirectionChanged = addFabController::onScrollDirectionChanged,
        ) {
            item(key = "modifier-card") {
                Card {
                    WindowDropdown(
                        title = MLang.Override.Editor.Mode.Title,
                        items = availableModes.map(OverrideListEditorMode::label),
                        selectedIndex = selectedModeIndex,
                        onSelectedIndexChange = { index ->
                            val newMode = availableModes.getOrElse(index) { selectedMode }
                            OverrideStructuredEditorStore.updateObjectEditorSession(selectedMode = newMode)
                            isDeleteMode = false
                            selectedUiIds = emptySet()
                        },
                    )
                }
            }

            item(key = "modifier-card-gap") {
                Spacer(modifier = Modifier.height(ObjectListSectionGap))
            }

            if (currentItemCount > 0) {
                when (editorType) {
                    OverrideStructuredObjectType.Proxies -> {
                        items(
                            count = currentProxyDrafts.size,
                            key = { index -> currentProxyDrafts[index].uiId },
                        ) { index ->
                            val draft = currentProxyDrafts[index]
                            ReorderableItem(
                                state = reorderState,
                                key = draft.uiId,
                            ) { isDragging ->
                                StructuredObjectCard(
                                    title = draft.name.ifBlank { MLang.Override.Editor.UnnamedProxyNode },
                                    isDragging = isDragging,
                                    isDeleteMode = isDeleteMode,
                                    isSelected = draft.uiId in selectedUiIds,
                                    onClick = {
                                        if (isDeleteMode) {
                                            selectedUiIds = selectedUiIds.toggle(draft.uiId)
                                        } else {
                                            val draftUiId = draft.uiId
                                            val editMode = selectedMode
                                            onOpenProxyDraftEditor(MLang.Override.Editor.EditProxyNode, draft) { updatedDraft ->
                                                val latestValues = OverrideStructuredEditorStore.objectEditorProxyDraftValues
                                                val updatedValues = latestValues.update(
                                                    editMode,
                                                    latestValues.valueFor(editMode).orEmpty().map { currentDraft ->
                                                        if (currentDraft.uiId == draftUiId) {
                                                            updatedDraft.copy(uiId = draftUiId)
                                                        } else {
                                                            currentDraft
                                                        }
                                                    },
                                                )
                                                applyProxyValues(updatedValues)
                                            }
                                        }
                                    },
                                    onSelectedChange = { checked ->
                                        selectedUiIds = if (checked) {
                                            selectedUiIds + draft.uiId
                                        } else {
                                            selectedUiIds - draft.uiId
                                        }
                                    },
                                )
                            }
                        }
                    }

                    OverrideStructuredObjectType.ProxyGroups -> {
                        items(
                            count = currentProxyGroupDrafts.size,
                            key = { index -> currentProxyGroupDrafts[index].uiId },
                        ) { index ->
                            val draft = currentProxyGroupDrafts[index]
                            ReorderableItem(
                                state = reorderState,
                                key = draft.uiId,
                            ) { isDragging ->
                                StructuredObjectCard(
                                    title = draft.name.ifBlank { MLang.Override.Editor.UnnamedProxyGroup },
                                    isDragging = isDragging,
                                    isDeleteMode = isDeleteMode,
                                    isSelected = draft.uiId in selectedUiIds,
                                    onClick = {
                                        if (isDeleteMode) {
                                            selectedUiIds = selectedUiIds.toggle(draft.uiId)
                                        } else {
                                            val draftUiId = draft.uiId
                                            val editMode = selectedMode
                                            onOpenProxyGroupDraftEditor(MLang.Override.Editor.EditProxyGroup, draft) { updatedDraft ->
                                                val latestValues = OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues
                                                val updatedValues = latestValues.update(
                                                    editMode,
                                                    latestValues.valueFor(editMode).orEmpty().map { currentDraft ->
                                                        if (currentDraft.uiId == draftUiId) {
                                                            updatedDraft.copy(uiId = draftUiId)
                                                        } else {
                                                            currentDraft
                                                        }
                                                    },
                                                )
                                                applyProxyGroupValues(updatedValues)
                                            }
                                        }
                                    },
                                    onSelectedChange = { checked ->
                                        selectedUiIds = if (checked) {
                                            selectedUiIds + draft.uiId
                                        } else {
                                            selectedUiIds - draft.uiId
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item(key = "object-list-bottom-spacer") {
                Spacer(modifier = Modifier.height(OverrideSectionBottomSpacing))
            }
        }

    AppDialog(
            show = showResetDialog,
            title = "清空${editorType.itemLabel}",
            summary = "清空后将移除当前模式里的所有${editorType.itemLabel}。",
            onDismissRequest = { showResetDialog = false },
        ) {
            DialogButtonRow(
                onCancel = { showResetDialog = false },
                onConfirm = {
                    showResetDialog = false
                    isDeleteMode = false
                    selectedUiIds = emptySet()
                    when (editorType) {
                        OverrideStructuredObjectType.Proxies -> {
                            val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                            applyProxyValues(OverrideStructuredEditorStore.objectEditorProxyDraftValues.update(mode, emptyList()))
                        }

                        OverrideStructuredObjectType.ProxyGroups -> {
                            val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                            applyProxyGroupValues(OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues.update(mode, emptyList()))
                        }
                    }
                },
                cancelText = MLang.Override.Dialog.Button.Cancel,
                confirmText = MLang.Override.Editor.Clear,
            )
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.StructuredObjectCard(
    title: String,
    isDragging: Boolean,
    isDeleteMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectedChange: (Boolean) -> Unit,
) {
    Column {
        Card(
            modifier = Modifier
                .longPressDraggableHandle(enabled = !isDeleteMode)
                .alpha(if (isDragging) 0.92f else 1f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Yume.List,
                    contentDescription = MLang.Override.Editor.DragToSort,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.body1,
                    )
                }
                Box(
                    modifier = Modifier.height(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDeleteMode) {
                        Checkbox(
                            state = ToggleableState(isSelected),
                            onClick = { onSelectedChange(!isSelected) },
                        )
                    } else {
                        Icon(
                            imageVector = Yume.chevron,
                            contentDescription = MLang.Override.Editor.Edit,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(ObjectListSectionGap))
    }
}

private fun Set<String>.toggle(uiId: String): Set<String> {
    return if (uiId in this) this - uiId else this + uiId
}
