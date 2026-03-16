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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Save
import com.github.yumelira.yumebox.presentation.util.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperArrow

@Composable
fun OverrideSubRuleDraftEditorScreen(
    navigator: DestinationsNavigator,
    onOpenRuleListEditor: (
        title: String,
        values: OverrideListModeValues<List<String>>,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        referenceCatalog: OverrideReferenceCatalog,
        callback: (OverrideListModeValues<List<String>>) -> Unit,
    ) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val title = OverrideStructuredEditorStore.subRuleDraftEditorTitle.ifBlank { "子规则组" }
    val storeDraft = OverrideStructuredEditorStore.subRuleDraftEditorValue
    val saveFabController = rememberOverrideFabController()
    val draftUiId = remember { storeDraft?.uiId ?: OverrideSubRuleGroupDraft().uiId }

    var name by remember { mutableStateOf(storeDraft?.name.orEmpty()) }
    var rules by remember { mutableStateOf(storeDraft?.rules.orEmpty()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun syncDraftSession(
        updatedName: String = name,
        updatedRules: List<String> = rules,
    ) {
        OverrideStructuredEditorStore.updateSubRuleDraftEditorSession(
            OverrideSubRuleGroupDraft(
                name = updatedName,
                rules = updatedRules,
                uiId = OverrideStructuredEditorStore.subRuleDraftEditorValue?.uiId ?: draftUiId,
            ),
        )
    }

    LaunchedEffect(storeDraft?.name, storeDraft?.rules) {
        val latestDraft = OverrideStructuredEditorStore.subRuleDraftEditorValue ?: return@LaunchedEffect
        if (name != latestDraft.name) {
            name = latestDraft.name
        }
        if (rules != latestDraft.rules) {
            rules = latestDraft.rules
        }
    }

    LaunchedEffect(draftUiId) {
        if (OverrideStructuredEditorStore.subRuleDraftEditorValue == null) {
            syncDraftSession()
        }
    }

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = saveFabController,
                visible = true,
                imageVector = Yume.Save,
                contentDescription = "保存子规则组",
                onClick = {
                    if (name.trim().isBlank()) {
                        errorText = "名称不能为空"
                        return@OverrideAnimatedFab
                    }
                    OverrideStructuredEditorStore.submitSubRuleDraft(
                        OverrideSubRuleGroupDraft(
                            name = name.trim(),
                            rules = rules,
                            uiId = OverrideStructuredEditorStore.subRuleDraftEditorValue?.uiId ?: draftUiId,
                        ),
                    )
                    navigator.navigateUp()
                },
            )
        },
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
            lazyListState = listState,
            onScrollDirectionChanged = saveFabController::onScrollDirectionChanged,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
                ) {
                    OverridePlainFormSection("基础信息") {
                        OverrideFormField(
                            value = name,
                            onValueChange = {
                                name = it
                                errorText = null
                                syncDraftSession(updatedName = it)
                            },
                            label = "名称",
                            errorText = errorText,
                        )
                    }
                    OverrideCardSection("规则列表") {
                        SuperArrow(
                            title = "规则列表",
                            summary = if (rules.isEmpty()) {
                                "未配置规则"
                            } else {
                                "已配置 ${rules.size} 条规则"
                            },
                            onClick = {
                                syncDraftSession()
                                onOpenRuleListEditor(
                                    "编辑子规则",
                                    OverrideListModeValues(replaceValue = rules),
                                    listOf(OverrideListEditorMode.Replace),
                                    OverrideListEditorMode.Replace,
                                    OverrideStructuredEditorStore.currentReferenceCatalog(),
                                ) { updatedValues ->
                                    syncDraftSession(
                                        updatedName = OverrideStructuredEditorStore.subRuleDraftEditorValue?.name.orEmpty(),
                                        updatedRules = updatedValues.replaceValue.orEmpty(),
                                    )
                                }
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(OverrideSectionBottomSpacing))
                }
            }
        }
    }
}
