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
import com.github.yumelira.yumebox.presentation.util.OverrideRuleDraft
import com.github.yumelira.yumebox.presentation.util.OverrideRuleTypePresets
import com.github.yumelira.yumebox.presentation.util.OverrideStructuredEditorStore
import com.github.yumelira.yumebox.presentation.util.supportsRuleExtra
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.extra.WindowDropdown
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OverrideRuleDraftEditorScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val title = remember { OverrideStructuredEditorStore.ruleDraftEditorTitle.ifBlank { "规则编辑" } }
    val initialValue = remember { OverrideStructuredEditorStore.ruleDraftEditorValue }
    val saveFabController = rememberOverrideFabController()

    var ruleType by remember {
        mutableStateOf(initialValue?.type?.ifBlank { "DOMAIN-SUFFIX" } ?: "DOMAIN-SUFFIX")
    }
    var payload by remember { mutableStateOf(initialValue?.payload.orEmpty()) }
    var target by remember { mutableStateOf(initialValue?.target.orEmpty()) }
    var useSrc by remember {
        mutableStateOf(initialValue?.extras.orEmpty().any { it.equals("src", ignoreCase = true) })
    }
    var useNoResolve by remember {
        mutableStateOf(initialValue?.extras.orEmpty().any { it.equals("no-resolve", ignoreCase = true) })
    }
    var extraText by remember {
        mutableStateOf(
            initialValue
                ?.extras
                .orEmpty()
                .filterNot { it.equals("src", ignoreCase = true) || it.equals("no-resolve", ignoreCase = true) }
                .joinToString(","),
        )
    }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showTargetSelector by remember { mutableStateOf(false) }
    var showRuleProviderSelector by remember { mutableStateOf(false) }
    val selectedPresetIndex = OverrideRuleTypePresets.indexOfFirst {
        it.equals(ruleType, ignoreCase = true)
    }.coerceAtLeast(0)
    val canUseExtraSwitches = supportsRuleExtra(ruleType)
    val referenceCatalog = OverrideStructuredEditorStore.currentReferenceCatalog()
    val isSubRuleTarget = ruleType.equals("SUB-RULE", ignoreCase = true)
    val isRuleSetType = ruleType.equals("RULE-SET", ignoreCase = true)
    val targetCandidates = if (isSubRuleTarget) {
        referenceCatalog.subRuleNames
    } else {
        referenceCatalog.proxyGroupNames
    }
    val ruleProviderCandidates = referenceCatalog.ruleProviderNames
    var selectedRuleProvider by remember(initialValue?.payload, ruleProviderCandidates) {
        mutableStateOf(
            initialValue?.payload
                ?.takeIf { candidate -> candidate.isNotBlank() && candidate in ruleProviderCandidates }
                .orEmpty(),
        )
    }
    val normalizedPayloadInput = payload.trim()
    val selectedRuleProviderValue = normalizedPayloadInput
        .takeIf { candidate -> candidate.isNotBlank() && candidate in ruleProviderCandidates }
        ?: selectedRuleProvider.trim()
    val targetLabel = if (isSubRuleTarget) "子规则目标" else "策略组目标"
    val typeErrorText = errorText?.takeIf { it.contains("类型") }
    val payloadErrorText = errorText?.takeIf { it.contains("匹配内容") }
    val targetErrorText = errorText?.takeIf { it.contains("目标") || it.contains("结果") }

    DisposableEffect(Unit) {
        onDispose {
            OverrideStructuredEditorStore.clearRuleDraftEditor()
        }
    }

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = saveFabController,
                visible = true,
                imageVector = Yume.Save,
                contentDescription = "保存规则",
                onClick = {
                    val normalizedType = ruleType.trim().uppercase()
                    val normalizedPayload = payload.trim()
                    val resolvedPayload = if (normalizedPayload.isNotBlank()) {
                        normalizedPayload
                    } else if (normalizedType == "RULE-SET") {
                        selectedRuleProvider.trim()
                    } else {
                        normalizedPayload
                    }
                    val normalizedTarget = target.trim()
                    val extraValues = extraText
                        .split(',')
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .toMutableList()

                    if (normalizedType.isBlank()) {
                        errorText = "规则类型不能为空"
                        return@OverrideAnimatedFab
                    }
                    if (!normalizedType.equals("MATCH", ignoreCase = true) && resolvedPayload.isBlank()) {
                        errorText = "匹配内容不能为空"
                        return@OverrideAnimatedFab
                    }
                    if (normalizedTarget.isBlank()) {
                        errorText = "目标不能为空"
                        return@OverrideAnimatedFab
                    }
                    if (canUseExtraSwitches) {
                        if (useSrc) {
                            extraValues += "src"
                        }
                        if (useNoResolve) {
                            extraValues += "no-resolve"
                        }
                    }
                    OverrideStructuredEditorStore.submitRuleDraft(
                        OverrideRuleDraft(
                            type = normalizedType,
                            payload = resolvedPayload,
                            target = normalizedTarget,
                            extras = extraValues.distinct(),
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
            lazyListState = listState,
            onScrollDirectionChanged = saveFabController::onScrollDirectionChanged,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
                ) {
                    OverrideSection("规则主体") {
                        OverrideSelectorCard {
                            WindowDropdown(
                                title = "类型",
                                items = OverrideRuleTypePresets,
                                selectedIndex = selectedPresetIndex,
                                onSelectedIndexChange = { index ->
                                    if (index in OverrideRuleTypePresets.indices) {
                                        ruleType = OverrideRuleTypePresets[index]
                                        errorText = null
                                    }
                                },
                            )
                        }
                        if (isRuleSetType) {
                            OverrideSelectorCard {
                                SuperArrow(
                                    title = "规则提供者",
                                    onClick = {
                                        showRuleProviderSelector = true
                                        errorText = null
                                    },
                                    holdDownState = showRuleProviderSelector,
                                )
                            }
                        }
                        OverrideFormFieldColumn {
                            if (!ruleType.equals("MATCH", ignoreCase = true)) {
                                OverrideFormField(
                                    value = payload,
                                    onValueChange = {
                                        payload = it
                                        errorText = null
                                    },
                                    label = "匹配内容",
                                    supportText = if (isRuleSetType) {
                                        "输入框是自定义内容；留空时使用下面选中的规则提供者"
                                    } else {
                                        "逻辑规则可直接填写完整 payload"
                                    },
                                    errorText = payloadErrorText,
                                )
                            }
                        }
                        OverrideSelectorCard {
                            SuperArrow(
                                title = if (ruleType.equals("MATCH", ignoreCase = true)) "匹配结果" else targetLabel,
                                onClick = {
                                    showTargetSelector = true
                                    errorText = null
                                },
                                holdDownState = showTargetSelector,
                            )
                        }
                        targetErrorText?.let { message ->
                            OverrideFieldAssistText(
                                text = message,
                                color = MiuixTheme.colorScheme.error,
                            )
                        }
                    }
                    if (canUseExtraSwitches) {
                        OverrideCardSection("选项") {
                            RuleExtraSwitchRow(
                                title = "src",
                                checked = useSrc,
                                onCheckedChange = { useSrc = it },
                            )
                            RuleExtraSwitchRow(
                                title = "no-resolve",
                                checked = useNoResolve,
                                onCheckedChange = { useNoResolve = it },
                            )
                        }
                    }
                    OverridePlainFormSection("附加参数") {
                        OverrideFormField(
                            value = extraText,
                            onValueChange = {
                                extraText = it
                                errorText = null
                            },
                            label = "其他附加参数，多个值用逗号分隔",
                            supportText = "例如 src,no-resolve 之外的额外参数\n逻辑规则请直接填写完整 payload，例如 ((DOMAIN,google.com),(NETWORK,udp))。",
                        )
                    }
                    Spacer(modifier = Modifier.height(OverrideSectionBottomSpacing))
                }
            }
        }
        OverrideSingleValueSelectionSheet(
            show = showTargetSelector,
            title = if (ruleType.equals("MATCH", ignoreCase = true)) "选择匹配结果" else "选择${targetLabel}",
            value = target,
            groups = listOf(
                OverrideSelectionGroup(
                    title = if (isSubRuleTarget) "子规则组" else "策略组",
                    items = targetCandidates,
                ),
            ),
            customInputLabel = if (ruleType.equals("MATCH", ignoreCase = true)) "自定义匹配结果" else "自定义${targetLabel}",
            onDismiss = { showTargetSelector = false },
            onConfirm = { selectedValue ->
                target = selectedValue
                errorText = null
                showTargetSelector = false
            },
        )
        OverrideSingleValueSelectionSheet(
            show = showRuleProviderSelector,
            title = "选择规则提供者",
            value = selectedRuleProviderValue,
            groups = listOf(
                OverrideSelectionGroup(
                    title = "规则提供者",
                    items = ruleProviderCandidates,
                ),
            ),
            customInputLabel = "",
            allowCustomValue = false,
            onDismiss = { showRuleProviderSelector = false },
            onConfirm = { selectedValue ->
                selectedRuleProvider = selectedValue.trim()
                errorText = null
                showRuleProviderSelector = false
            },
        )
    }
}

@Composable
private fun RuleExtraSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SuperSwitch(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}
