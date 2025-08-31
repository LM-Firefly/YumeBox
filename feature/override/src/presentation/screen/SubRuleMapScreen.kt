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

private val SubRuleSectionGap = 12.dp
private const val SubRuleReorderHeaderCount = 2

@Composable
fun OverrideSubRuleMapEditorScreen(
    navigator: DestinationsNavigator,
    onOpenDraftEditor: (
        title: String,
        initialValue: OverrideSubRuleGroupDraft?,
        onConfirm: (OverrideSubRuleGroupDraft) -> Unit,
    ) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val title = OverrideStructuredEditorStore.subRuleGroupEditorTitle.ifBlank { "子规则" }
    val availableModes = OverrideStructuredEditorStore.subRuleGroupEditorAvailableModes
    var showResetDialog by remember { mutableStateOf(false) }
    val addFabController = rememberOverrideFabController()
    var isDeleteMode by rememberSaveable { mutableStateOf(false) }
    var selectedUiIds by remember { mutableStateOf(emptySet<String>()) }
    val selectedMode = OverrideStructuredEditorStore.subRuleGroupEditorSelectedMode
    val editorValues = OverrideStructuredEditorStore.subRuleGroupEditorDraftValues

    val selectedModeIndex = availableModes.indexOf(selectedMode).coerceAtLeast(0)
    val currentDrafts = editorValues.valueFor(selectedMode).orEmpty()

    fun applySubRuleValues(values: OverrideListModeValues<List<OverrideSubRuleGroupDraft>>) {
        OverrideStructuredEditorStore.applySubRuleDraftValues(values)
    }

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = (from.index - SubRuleReorderHeaderCount).coerceAtLeast(0)
        val toIndex = (to.index - SubRuleReorderHeaderCount).coerceAtLeast(0)
        val mode = OverrideStructuredEditorStore.subRuleGroupEditorSelectedMode
        val latestValues = OverrideStructuredEditorStore.subRuleGroupEditorDraftValues
        val updatedValues = latestValues.update(
            mode,
            reorderDraftList(latestValues.valueFor(mode).orEmpty(), fromIndex, toIndex),
        )
        selectedUiIds = emptySet()
        applySubRuleValues(updatedValues)
    }
    val showAddFab = !isDeleteMode && !showResetDialog

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = addFabController,
                visible = showAddFab,
                imageVector = Yume.`Badge-plus`,
                contentDescription = "新增子规则组",
                onClick = {
                    onOpenDraftEditor("新增子规则组", null) { createdDraft ->
                        val mode = OverrideStructuredEditorStore.subRuleGroupEditorSelectedMode
                        val latestValues = OverrideStructuredEditorStore.subRuleGroupEditorDraftValues
                        val updatedValues = latestValues.update(
                            mode,
                            latestValues.valueFor(mode).orEmpty().toMutableList().also { it.add(createdDraft) },
                        )
                        applySubRuleValues(updatedValues)
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
                                contentDescription = "取消删除",
                            )
                        }
                        IconButton(
                            onClick = {
                                if (selectedUiIds.isNotEmpty()) {
                                    val mode = OverrideStructuredEditorStore.subRuleGroupEditorSelectedMode
                                    val latestValues = OverrideStructuredEditorStore.subRuleGroupEditorDraftValues
                                    val updatedValues = latestValues.update(
                                        mode,
                                        latestValues.valueFor(mode).orEmpty().filterNot { it.uiId in selectedUiIds },
                                    )
                                    selectedUiIds = emptySet()
                                    isDeleteMode = false
                                    applySubRuleValues(updatedValues)
                                }
                            },
                            modifier = Modifier.padding(end = 24.dp),
                        ) {
                            Icon(
                                imageVector = Yume.Delete,
                                contentDescription = "删除已选子规则组",
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Icon(
                                imageVector = Yume.Undo,
                                contentDescription = "清空当前模式",
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
                                contentDescription = "进入删除模式",
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
                        title = "修饰符模式",
                        items = availableModes.map(OverrideListEditorMode::label),
                        selectedIndex = selectedModeIndex,
                        onSelectedIndexChange = { index ->
                            val newMode = availableModes.getOrElse(index) { selectedMode }
                            OverrideStructuredEditorStore.updateSubRuleGroupEditorSession(selectedMode = newMode)
                            isDeleteMode = false
                            selectedUiIds = emptySet()
                        },
                    )
                }
            }

            item(key = "modifier-card-gap") {
                Spacer(modifier = Modifier.height(SubRuleSectionGap))
            }

            if (currentDrafts.isNotEmpty()) {
                items(
                    count = currentDrafts.size,
                    key = { index -> currentDrafts[index].uiId },
                ) { index ->
                    val draft = currentDrafts[index]
                    ReorderableItem(
                        state = reorderState,
                        key = draft.uiId,
                    ) { isDragging ->
                        SubRuleGroupCard(
                            title = draft.name.ifBlank { "未命名子规则组" },
                            isDragging = isDragging,
                            isDeleteMode = isDeleteMode,
                            isSelected = draft.uiId in selectedUiIds,
                            onClick = {
                                if (isDeleteMode) {
                                    selectedUiIds = selectedUiIds.toggle(draft.uiId)
                                } else {
                                    val draftUiId = draft.uiId
                                    val editMode = selectedMode
                                    onOpenDraftEditor("编辑子规则组", draft) { updatedDraft ->
                                        val latestValues = OverrideStructuredEditorStore.subRuleGroupEditorDraftValues
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
                                        applySubRuleValues(updatedValues)
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

            item(key = "sub-rule-bottom-spacer") {
                Spacer(modifier = Modifier.height(OverrideSectionBottomSpacing))
            }
        }

    AppDialog(
            show = showResetDialog,
            title = "清空子规则",
            summary = "清空后将移除当前模式里的所有子规则组。",
            onDismissRequest = { showResetDialog = false },
        ) {
            DialogButtonRow(
                onCancel = { showResetDialog = false },
                onConfirm = {
                    showResetDialog = false
                    isDeleteMode = false
                    selectedUiIds = emptySet()
                    val mode = OverrideStructuredEditorStore.subRuleGroupEditorSelectedMode
                    applySubRuleValues(OverrideStructuredEditorStore.subRuleGroupEditorDraftValues.update(mode, emptyList()))
                },
                cancelText = "取消",
                confirmText = "清空",
            )
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.SubRuleGroupCard(
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
                    contentDescription = "拖拽排序",
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
                            contentDescription = "编辑",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(SubRuleSectionGap))
    }
}

private fun Set<String>.toggle(uiId: String): Set<String> {
    return if (uiId in this) this - uiId else this + uiId
}
