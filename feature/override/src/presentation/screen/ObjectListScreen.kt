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
import androidx.compose.foundation.lazy.items
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
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val selectedUiIds = remember { mutableStateMapOf<String, Boolean>() }
    val selectedMode = OverrideStructuredEditorStore.objectEditorSelectedMode

    val proxyModeValues = OverrideStructuredEditorStore.objectEditorProxyDraftValues
    val proxyGroupModeValues = OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues

    val modeLabels = remember(availableModes) { availableModes.map(OverrideListEditorMode::label) }
    val selectedModeIndex = remember(availableModes, selectedMode) {
        availableModes.indexOf(selectedMode).coerceAtLeast(0)
    }
    val currentProxyDrafts = remember(proxyModeValues, selectedMode) {
        proxyModeValues.valueFor(selectedMode).orEmpty()
    }
    val currentProxyGroupDrafts = remember(proxyGroupModeValues, selectedMode) {
        proxyGroupModeValues.valueFor(selectedMode).orEmpty()
    }
    val currentItemCount = remember(editorType, currentProxyDrafts, currentProxyGroupDrafts) {
        when (editorType) {
            OverrideStructuredObjectType.Proxies -> currentProxyDrafts.size
            OverrideStructuredObjectType.ProxyGroups -> currentProxyGroupDrafts.size
        }
    }

    fun applyProxyModeValue(
        mode: OverrideListEditorMode,
        values: List<OverrideProxyDraft>,
    ) {
        OverrideStructuredEditorStore.applyProxyDraftModeValue(mode, values)
    }

    fun applyProxyGroupModeValue(
        mode: OverrideListEditorMode,
        values: List<OverrideProxyGroupDraft>,
    ) {
        OverrideStructuredEditorStore.applyProxyGroupDraftModeValue(mode, values)
    }

    fun clearSelection() {
        selectedUiIds.clear()
    }

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = (from.index - ObjectListReorderHeaderCount).coerceAtLeast(0)
        val toIndex = (to.index - ObjectListReorderHeaderCount).coerceAtLeast(0)
        when (editorType) {
            OverrideStructuredObjectType.Proxies -> {
                val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                applyProxyModeValue(
                    mode,
                    reorderDraftList(
                        OverrideStructuredEditorStore.objectEditorProxyDraftValues.valueFor(mode).orEmpty(),
                        fromIndex,
                        toIndex,
                    ),
                )
            }

            OverrideStructuredObjectType.ProxyGroups -> {
                val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                applyProxyGroupModeValue(
                    mode,
                    reorderDraftList(
                        OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues.valueFor(mode).orEmpty(),
                        fromIndex,
                        toIndex,
                    ),
                )
            }
        }
        clearSelection()
    }
    val showAddFab = !isDeleteMode && !showResetDialog

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = addFabController,
                visible = showAddFab,
                imageVector = Yume.`Badge-plus`,
                contentDescription = MLang.Override.Editor.New + editorType.itemLabel,
                onClick = {
                    when (editorType) {
                        OverrideStructuredObjectType.Proxies -> {
                            onOpenProxyDraftEditor(MLang.Override.Editor.NewProxyNode, null) { createdDraft ->
                                val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                                applyProxyModeValue(
                                    mode,
                                    OverrideStructuredEditorStore.objectEditorProxyDraftValues.valueFor(mode)
                                        .orEmpty()
                                        .toMutableList()
                                        .also { it.add(createdDraft) },
                                )
                            }
                        }

                        OverrideStructuredObjectType.ProxyGroups -> {
                            onOpenProxyGroupDraftEditor(MLang.Override.Editor.NewProxyGroup, null) { createdDraft ->
                                val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                                applyProxyGroupModeValue(
                                    mode,
                                    OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues.valueFor(mode)
                                        .orEmpty()
                                        .toMutableList()
                                        .also { it.add(createdDraft) },
                                )
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
                        OverrideTopBarAction(
                            icon = Yume.Cancel,
                            contentDescription = MLang.Override.Editor.CancelDelete,
                            spacedFromNext = true,
                            onClick = {
                                isDeleteMode = false
                                clearSelection()
                            },
                        )
                        OverrideTopBarAction(
                            icon = Yume.Delete,
                            contentDescription = MLang.Override.Editor.DeleteSelected,
                            onClick = {
                                if (selectedUiIds.isNotEmpty()) {
                                    when (editorType) {
                                        OverrideStructuredObjectType.Proxies -> {
                                            val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                                            applyProxyModeValue(
                                                mode,
                                                OverrideStructuredEditorStore.objectEditorProxyDraftValues.valueFor(mode)
                                                    .orEmpty()
                                                    .filterNot { selectedUiIds.containsKey(it.uiId) },
                                            )
                                        }

                                        OverrideStructuredObjectType.ProxyGroups -> {
                                            val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                                            applyProxyGroupModeValue(
                                                mode,
                                                OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues.valueFor(mode)
                                                    .orEmpty()
                                                    .filterNot { selectedUiIds.containsKey(it.uiId) },
                                            )
                                        }
                                    }
                                    clearSelection()
                                    isDeleteMode = false
                                }
                            },
                        )
                    } else {
                        OverrideTopBarAction(
                            icon = Yume.Undo,
                            contentDescription = MLang.Override.Editor.ClearCurrentMode,
                            spacedFromNext = true,
                            onClick = { showResetDialog = true },
                        )
                        OverrideTopBarAction(
                            icon = Yume.Delete,
                            contentDescription = MLang.Override.Editor.EnterDeleteMode,
                            onClick = {
                                isDeleteMode = true
                                clearSelection()
                            },
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
            modifier = Modifier.fillMaxWidth(),
            lazyListState = listState,
            onScrollDirectionChanged = addFabController::onScrollDirectionChanged,
        ) {
            item(key = "modifier-card") {
                Card {
                    WindowDropdownPreference(
                        title = MLang.Override.Editor.Mode.Title,
                        items = modeLabels,
                        selectedIndex = selectedModeIndex,
                        onSelectedIndexChange = { index ->
                            val newMode = availableModes.getOrElse(index) { selectedMode }
                            OverrideStructuredEditorStore.updateObjectEditorSession(selectedMode = newMode)
                            isDeleteMode = false
                            clearSelection()
                        },
                    )
                }
            }

            item(key = "modifier-card-gap") {
    Spacer(modifier = Modifier.height(UiDp.dp12))
            }

            if (currentItemCount > 0) {
                when (editorType) {
                    OverrideStructuredObjectType.Proxies -> {
                        items(
                            items = currentProxyDrafts,
                            key = OverrideProxyDraft::uiId,
                            contentType = { "proxy-draft-card" },
                        ) { draft ->
                            ReorderableItem(
                                state = reorderState,
                                key = draft.uiId,
                            ) { isDragging ->
                                StructuredObjectCard(
                                    title = draft.name.ifBlank { MLang.Override.Editor.UnnamedProxyNode },
                                    isDragging = isDragging,
                                    isDeleteMode = isDeleteMode,
                                    isSelected = selectedUiIds[draft.uiId] == true,
                                    onClick = {
                                        if (isDeleteMode) {
                                            if (selectedUiIds[draft.uiId] == true) {
                                                selectedUiIds.remove(draft.uiId)
                                            } else {
                                                selectedUiIds[draft.uiId] = true
                                            }
                                        } else {
                                            val draftUiId = draft.uiId
                                            val editMode = selectedMode
                                            onOpenProxyDraftEditor(MLang.Override.Editor.EditProxyNode, draft) { updatedDraft ->
                                                applyProxyModeValue(
                                                    editMode,
                                                    OverrideStructuredEditorStore.objectEditorProxyDraftValues
                                                        .valueFor(editMode)
                                                        .orEmpty()
                                                        .map { currentDraft ->
                                                        if (currentDraft.uiId == draftUiId) {
                                                            updatedDraft.copy(uiId = draftUiId)
                                                        } else {
                                                            currentDraft
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                    },
                                    onSelectedChange = { checked ->
                                        if (checked) {
                                            selectedUiIds[draft.uiId] = true
                                        } else {
                                            selectedUiIds.remove(draft.uiId)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    OverrideStructuredObjectType.ProxyGroups -> {
                        items(
                            items = currentProxyGroupDrafts,
                            key = OverrideProxyGroupDraft::uiId,
                            contentType = { "proxy-group-draft-card" },
                        ) { draft ->
                            ReorderableItem(
                                state = reorderState,
                                key = draft.uiId,
                            ) { isDragging ->
                                StructuredObjectCard(
                                    title = draft.name.ifBlank { MLang.Override.Editor.UnnamedProxyGroup },
                                    isDragging = isDragging,
                                    isDeleteMode = isDeleteMode,
                                    isSelected = selectedUiIds[draft.uiId] == true,
                                    onClick = {
                                        if (isDeleteMode) {
                                            if (selectedUiIds[draft.uiId] == true) {
                                                selectedUiIds.remove(draft.uiId)
                                            } else {
                                                selectedUiIds[draft.uiId] = true
                                            }
                                        } else {
                                            val draftUiId = draft.uiId
                                            val editMode = selectedMode
                                            onOpenProxyGroupDraftEditor(MLang.Override.Editor.EditProxyGroup, draft) { updatedDraft ->
                                                applyProxyGroupModeValue(
                                                    editMode,
                                                    OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues
                                                        .valueFor(editMode)
                                                        .orEmpty()
                                                        .map { currentDraft ->
                                                        if (currentDraft.uiId == draftUiId) {
                                                            updatedDraft.copy(uiId = draftUiId)
                                                        } else {
                                                            currentDraft
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                    },
                                    onSelectedChange = { checked ->
                                        if (checked) {
                                            selectedUiIds[draft.uiId] = true
                                        } else {
                                            selectedUiIds.remove(draft.uiId)
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
            title = MLang.Override.Editor.ClearDialog.Title.format(editorType.itemLabel),
            summary = MLang.Override.Editor.ClearDialog.Summary.format(editorType.itemLabel),
            onDismissRequest = { showResetDialog = false },
        ) {
            DialogButtonRow(
                onCancel = { showResetDialog = false },
                onConfirm = {
                    showResetDialog = false
                    isDeleteMode = false
                    clearSelection()
                    when (editorType) {
                        OverrideStructuredObjectType.Proxies -> {
                            val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                            applyProxyModeValue(mode, emptyList())
                        }

                        OverrideStructuredObjectType.ProxyGroups -> {
                            val mode = OverrideStructuredEditorStore.objectEditorSelectedMode
                            applyProxyGroupModeValue(mode, emptyList())
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
                    .padding(horizontal = UiDp.dp14, vertical = UiDp.dp14),
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
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
                    modifier = Modifier.height(UiDp.dp32),
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
    Spacer(modifier = Modifier.height(UiDp.dp12))
    }
}
