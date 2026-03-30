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
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val RuleListSectionGap = 12.dp
private const val RuleListReorderHeaderCount = 2

@Composable
fun OverrideRuleListEditorScreen(
    navigator: DestinationsNavigator,
    onOpenRuleDraftEditor: (
        title: String,
        initialValue: OverrideRuleDraft?,
        onConfirm: (OverrideRuleDraft) -> Unit,
    ) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val title = OverrideStructuredEditorStore.ruleEditorTitle.ifBlank { MLang.Override.Editor.Rules }
    val availableModes = OverrideStructuredEditorStore.ruleEditorAvailableModes
    var showResetDialog by remember { mutableStateOf(false) }
    val addFabController = rememberOverrideFabController()
    var isDeleteMode by rememberSaveable { mutableStateOf(false) }
    var selectedUiIds by remember { mutableStateOf(emptySet<String>()) }
    val selectedMode = OverrideStructuredEditorStore.ruleEditorSelectedMode
    val editorValues = OverrideStructuredEditorStore.ruleEditorDraftValues
    val currentRules = editorValues.valueFor(selectedMode).orEmpty()
    val selectedModeIndex = availableModes.indexOf(selectedMode).coerceAtLeast(0)

    fun applyRuleValues(values: OverrideListModeValues<List<OverrideRuleDraft>>) {
        OverrideStructuredEditorStore.applyRuleDraftValues(values)
    }

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = (from.index - RuleListReorderHeaderCount).coerceAtLeast(0)
        val toIndex = (to.index - RuleListReorderHeaderCount).coerceAtLeast(0)
        val latestValues = OverrideStructuredEditorStore.ruleEditorDraftValues
        val mode = OverrideStructuredEditorStore.ruleEditorSelectedMode
        val updatedValues = latestValues.update(
            mode,
            reorderDraftList(latestValues.valueFor(mode).orEmpty(), fromIndex, toIndex),
        )
        selectedUiIds = emptySet()
        applyRuleValues(updatedValues)
    }
    val showAddFab = !isDeleteMode && !showResetDialog

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = addFabController,
                visible = showAddFab,
                imageVector = Yume.`Badge-plus`,
                contentDescription = MLang.Override.Editor.NewRule,
                onClick = {
                    onOpenRuleDraftEditor(MLang.Override.Editor.NewRule, null) { createdDraft ->
                        val latestValues = OverrideStructuredEditorStore.ruleEditorDraftValues
                        val mode = OverrideStructuredEditorStore.ruleEditorSelectedMode
                        val updatedValues = latestValues.update(
                            mode,
                            latestValues.valueFor(mode).orEmpty().toMutableList().also { it.add(createdDraft) },
                        )
                        applyRuleValues(updatedValues)
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
                                selectedUiIds = emptySet()
                            },
                        )
                        OverrideTopBarAction(
                            icon = Yume.Delete,
                            contentDescription = MLang.Override.Editor.DeleteSelectedRules,
                            onClick = {
                                if (selectedUiIds.isNotEmpty()) {
                                    val latestValues = OverrideStructuredEditorStore.ruleEditorDraftValues
                                    val mode = OverrideStructuredEditorStore.ruleEditorSelectedMode
                                    val updatedValues = latestValues.update(
                                        mode,
                                        latestValues.valueFor(mode).orEmpty().filterNot { it.uiId in selectedUiIds },
                                    )
                                    selectedUiIds = emptySet()
                                    isDeleteMode = false
                                    applyRuleValues(updatedValues)
                                }
                            },
                        )
                    } else {
                        OverrideTopBarAction(
                            icon = Yume.Undo,
                            contentDescription = MLang.Override.Editor.ClearMode,
                            spacedFromNext = true,
                            onClick = { showResetDialog = true },
                        )
                        OverrideTopBarAction(
                            icon = Yume.Delete,
                            contentDescription = MLang.Override.Editor.EnterDeleteMode,
                            onClick = {
                                isDeleteMode = true
                                selectedUiIds = emptySet()
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
                        items = availableModes.map(OverrideListEditorMode::label),
                        selectedIndex = selectedModeIndex,
                        onSelectedIndexChange = { index ->
                            val newMode = availableModes.getOrElse(index) { selectedMode }
                            OverrideStructuredEditorStore.updateRuleEditorSession(selectedMode = newMode)
                            isDeleteMode = false
                            selectedUiIds = emptySet()
                        },
                    )
                }
            }

            item(key = "modifier-card-gap") {
                Spacer(modifier = Modifier.height(RuleListSectionGap))
            }

            if (currentRules.isNotEmpty()) {
                items(
                    count = currentRules.size,
                    key = { index -> currentRules[index].uiId },
                ) { index ->
                    val ruleDraft = currentRules[index]
                    val ruleTitle = formatRuleDraft(ruleDraft).ifBlank {
                        ruleDraft.type.ifBlank { MLang.Override.Editor.UnnamedRule }
                    }
                    ReorderableItem(
                        state = reorderState,
                        key = ruleDraft.uiId,
                    ) { isDragging ->
                        RuleListCard(
                            title = ruleTitle,
                            isDragging = isDragging,
                            isDeleteMode = isDeleteMode,
                            isSelected = ruleDraft.uiId in selectedUiIds,
                            onClick = {
                                if (isDeleteMode) {
                                    selectedUiIds = selectedUiIds.toggle(ruleDraft.uiId)
                                } else {
                                    val ruleUiId = ruleDraft.uiId
                                    val editMode = selectedMode
                                    onOpenRuleDraftEditor(MLang.Override.Editor.EditRule, ruleDraft) { updatedDraft ->
                                        val latestValues = OverrideStructuredEditorStore.ruleEditorDraftValues
                                        val updatedValues = latestValues.update(
                                            editMode,
                                            latestValues.valueFor(editMode).orEmpty().map { draft ->
                                                if (draft.uiId == ruleUiId) {
                                                    updatedDraft.copy(uiId = ruleUiId)
                                                } else {
                                                    draft
                                                }
                                            },
                                        )
                                        applyRuleValues(updatedValues)
                                    }
                                }
                            },
                            onSelectedChange = { checked ->
                                selectedUiIds = if (checked) {
                                    selectedUiIds + ruleDraft.uiId
                                } else {
                                    selectedUiIds - ruleDraft.uiId
                                }
                            },
                        )
                    }
                }
            }

            item(key = "rule-list-bottom-spacer") {
                Spacer(modifier = Modifier.height(OverrideSectionBottomSpacing))
            }
        }

    AppDialog(
            show = showResetDialog,
            title = MLang.Override.Editor.ClearDialog.Title.format(MLang.Override.Editor.Rules),
            summary = MLang.Override.Editor.ClearDialog.Summary.format(MLang.Override.Editor.Rules),
            onDismissRequest = { showResetDialog = false },
        ) {
            DialogButtonRow(
                onCancel = { showResetDialog = false },
                onConfirm = {
                    showResetDialog = false
                    isDeleteMode = false
                    selectedUiIds = emptySet()
                    val mode = OverrideStructuredEditorStore.ruleEditorSelectedMode
                    applyRuleValues(OverrideStructuredEditorStore.ruleEditorDraftValues.update(mode, emptyList()))
                },
                cancelText = MLang.Override.Dialog.Button.Cancel,
                confirmText = MLang.Override.Editor.Clear,
            )
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.RuleListCard(
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
                            contentDescription = MLang.Override.Editor.EditRule,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(RuleListSectionGap))
    }
}

private fun Set<String>.toggle(uiId: String): Set<String> {
    return if (uiId in this) this - uiId else this + uiId
}
