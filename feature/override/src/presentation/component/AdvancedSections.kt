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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.presentation.util.*
import kotlinx.serialization.json.JsonElement
import top.yukonga.miuix.kmp.extra.SuperArrow

typealias OpenRuleListEditor = (
    title: String,
    values: OverrideListModeValues<List<String>>,
    availableModes: List<OverrideListEditorMode>,
    selectedMode: OverrideListEditorMode,
    referenceCatalog: OverrideReferenceCatalog,
    onValueChange: (OverrideListModeValues<List<String>>) -> Unit,
) -> Unit

typealias OpenStructuredObjectListEditor = (
    type: OverrideStructuredObjectType,
    title: String,
    values: OverrideListModeValues<List<Map<String, JsonElement>>>,
    availableModes: List<OverrideListEditorMode>,
    selectedMode: OverrideListEditorMode,
    referenceCatalog: OverrideReferenceCatalog,
    onValueChange: (OverrideListModeValues<List<Map<String, JsonElement>>>) -> Unit,
) -> Unit

typealias OpenObjectMapEditor = (
    type: OverrideStructuredMapType,
    title: String,
    values: OverrideListModeValues<Map<String, Map<String, JsonElement>>>,
    availableModes: List<OverrideListEditorMode>,
    selectedMode: OverrideListEditorMode,
    onValueChange: (OverrideListModeValues<Map<String, Map<String, JsonElement>>>) -> Unit,
) -> Unit

typealias OpenSubRulesEditor = (
    title: String,
    values: OverrideListModeValues<Map<String, List<String>>>,
    availableModes: List<OverrideListEditorMode>,
    selectedMode: OverrideListEditorMode,
    referenceCatalog: OverrideReferenceCatalog,
    onValueChange: (OverrideListModeValues<Map<String, List<String>>>) -> Unit,
) -> Unit

@Composable
fun RulesEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditRuleList: OpenRuleListEditor,
) {
    Column {
        SmallTitle("规则链")
        OverrideSelectorCard {
            StructuredEditorEntry(
                title = "Rules",
                summary = buildModifierSummary(
                    replaceCount = config.rules?.size ?: 0,
                    startCount = config.rulesStart?.size ?: 0,
                    endCount = config.rulesEnd?.size ?: 0,
                    emptyHint = "未设置规则链",
                ),
                onClick = {
                    val values = OverrideListModeValues(
                        replaceValue = config.rules,
                        startValue = config.rulesStart,
                        endValue = config.rulesEnd,
                    )
                    onEditRuleList(
                        "Rules",
                        values,
                        listOf(
                            OverrideListEditorMode.Replace,
                            OverrideListEditorMode.Start,
                            OverrideListEditorMode.End,
                        ),
                        resolveInitialEditorMode(
                            availableModes = listOf(
                                OverrideListEditorMode.Replace,
                                OverrideListEditorMode.Start,
                                OverrideListEditorMode.End,
                            ),
                            values = values,
                        ),
                        buildOverrideReferenceCatalog(config),
                    ) { updatedValues ->
                        onConfigChange(
                            config.copy(
                                rules = updatedValues.replaceValue,
                                rulesStart = updatedValues.startValue,
                                rulesEnd = updatedValues.endValue,
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun SubRulesEditorSection(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditSubRules: OpenSubRulesEditor,
    onEditJson: OpenJsonEditor,
) {
    Column {
        SmallTitle("子规则")
        StructuredInputContent(
            title = "子规则",
            summary = buildMergeModifierSummary(
                replaceCount = config.subRules?.size ?: 0,
                mergeCount = config.subRulesMerge?.size ?: 0,
                emptyHint = "结构化规则组",
            ),
            advancedSummary = "复杂子规则结构统一收在高级 JSON 中",
            onStructuredClick = {
                val values = OverrideListModeValues(
                    replaceValue = config.subRules,
                    mergeValue = config.subRulesMerge,
                )
                    onEditSubRules(
                        "子规则",
                        values,
                        listOf(
                            OverrideListEditorMode.Replace,
                        OverrideListEditorMode.Merge,
                    ),
                        resolveInitialEditorMode(
                            availableModes = listOf(
                                OverrideListEditorMode.Replace,
                                OverrideListEditorMode.Merge,
                            ),
                            values = values,
                        ),
                        buildOverrideReferenceCatalog(config),
                    ) { updatedValues ->
                        onConfigChange(
                            config.copy(
                                subRules = updatedValues.replaceValue,
                            subRulesMerge = updatedValues.mergeValue,
                        ),
                    )
                }
            },
            onAdvancedClick = {
                onEditJson(
                    "子规则",
                    "{\n  \"sub-rule\": [\"DOMAIN,example.com,DIRECT\"]\n}",
                    encodeSubRules(config.subRules),
                ) {
                    onConfigChange(config.copy(subRules = decodeSubRules(it)))
                }
            },
        )
    }
}

@Composable
fun RuleProvidersEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditObjectMap: OpenObjectMapEditor,
    onEditJson: OpenJsonEditor,
) {
    Column {
        SmallTitle("规则提供者")
        StructuredInputContent(
            title = "规则提供者",
            summary = buildMergeModifierSummary(
                replaceCount = config.ruleProviders?.size ?: 0,
                mergeCount = config.ruleProvidersMerge?.size ?: 0,
                emptyHint = "结构化 Provider",
            ),
            advancedSummary = "需要复杂 Provider 字段时再进入高级 JSON",
            onStructuredClick = {
                val values = OverrideListModeValues(
                    replaceValue = config.ruleProviders,
                    mergeValue = config.ruleProvidersMerge,
                )
                onEditObjectMap(
                    OverrideStructuredMapType.RuleProviders,
                    "规则提供者",
                    values,
                    listOf(
                        OverrideListEditorMode.Replace,
                        OverrideListEditorMode.Merge,
                    ),
                    resolveInitialEditorMode(
                        availableModes = listOf(
                            OverrideListEditorMode.Replace,
                            OverrideListEditorMode.Merge,
                        ),
                        values = values,
                    ),
                ) { updatedValues ->
                    onConfigChange(
                        config.copy(
                            ruleProviders = updatedValues.replaceValue,
                            ruleProvidersMerge = updatedValues.mergeValue,
                        ),
                    )
                }
            },
            onAdvancedClick = {
                onEditJson(
                    "规则提供者",
                    "{\n  \"google\": {\n    \"type\": \"http\"\n  }\n}",
                    encodeObjectMap(config.ruleProviders),
                ) {
                    onConfigChange(config.copy(ruleProviders = decodeObjectMap(it)))
                }
            },
        )
    }
}

@Composable
fun ProxiesEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditObjectList: OpenStructuredObjectListEditor,
    onEditJson: OpenJsonEditor,
) {
    Column {
        SmallTitle("代理节点")
        OverrideSelectorCard {
            StructuredEditorEntry(
                title = "代理节点",
                summary = buildModifierSummary(
                    replaceCount = config.proxies?.size ?: 0,
                    startCount = config.proxiesStart?.size ?: 0,
                    endCount = config.proxiesEnd?.size ?: 0,
                    emptyHint = "结构化代理条目",
                ),
                onClick = {
                    val values = OverrideListModeValues(
                        replaceValue = config.proxies,
                        startValue = config.proxiesStart,
                        endValue = config.proxiesEnd,
                    )
                    onEditObjectList(
                        OverrideStructuredObjectType.Proxies,
                        "代理节点",
                        values,
                        listOf(
                            OverrideListEditorMode.Replace,
                            OverrideListEditorMode.Start,
                            OverrideListEditorMode.End,
                        ),
                        resolveInitialEditorMode(
                            availableModes = listOf(
                                OverrideListEditorMode.Replace,
                                OverrideListEditorMode.Start,
                                OverrideListEditorMode.End,
                            ),
                            values = values,
                        ),
                        buildOverrideReferenceCatalog(config),
                    ) { updatedValues ->
                        onConfigChange(
                            config.copy(
                                proxies = updatedValues.replaceValue,
                                proxiesStart = updatedValues.startValue,
                                proxiesEnd = updatedValues.endValue,
                            ),
                        )
                    }
                },
            )
        }

    }
}

@Composable
fun ProxyProvidersEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditObjectMap: OpenObjectMapEditor,
    onEditJson: OpenJsonEditor,
) {
    Column {
        SmallTitle("代理提供者")
        StructuredInputContent(
            title = "代理提供者",
            summary = buildMergeModifierSummary(
                replaceCount = config.proxyProviders?.size ?: 0,
                mergeCount = config.proxyProvidersMerge?.size ?: 0,
                emptyHint = "结构化 Provider",
            ),
            advancedSummary = "需要协议细节、校验或额外字段时再进入高级 JSON",
            onStructuredClick = {
                val values = OverrideListModeValues(
                    replaceValue = config.proxyProviders,
                    mergeValue = config.proxyProvidersMerge,
                )
                onEditObjectMap(
                    OverrideStructuredMapType.ProxyProviders,
                    "代理提供者",
                    values,
                    listOf(
                        OverrideListEditorMode.Replace,
                        OverrideListEditorMode.Merge,
                    ),
                    resolveInitialEditorMode(
                        availableModes = listOf(
                            OverrideListEditorMode.Replace,
                            OverrideListEditorMode.Merge,
                        ),
                        values = values,
                    ),
                ) { updatedValues ->
                    onConfigChange(
                        config.copy(
                            proxyProviders = updatedValues.replaceValue,
                            proxyProvidersMerge = updatedValues.mergeValue,
                        ),
                    )
                }
            },
            onAdvancedClick = {
                onEditJson(
                    "代理提供者",
                    "{\n  \"provider\": {\n    \"type\": \"http\"\n  }\n}",
                    encodeObjectMap(config.proxyProviders),
                ) {
                    onConfigChange(config.copy(proxyProviders = decodeObjectMap(it)))
                }
            },
        )
    }
}

@Composable
fun ProxyGroupsEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditObjectList: OpenStructuredObjectListEditor,
    onEditJson: OpenJsonEditor,
) {
    Column {
        SmallTitle("策略组")
        OverrideSelectorCard {
            StructuredEditorEntry(
                title = "策略组",
                summary = buildModifierSummary(
                    replaceCount = config.proxyGroups?.size ?: 0,
                    startCount = config.proxyGroupsStart?.size ?: 0,
                    endCount = config.proxyGroupsEnd?.size ?: 0,
                    emptyHint = "结构化策略组",
                ),
                onClick = {
                    val values = OverrideListModeValues(
                        replaceValue = config.proxyGroups,
                        startValue = config.proxyGroupsStart,
                        endValue = config.proxyGroupsEnd,
                    )
                    onEditObjectList(
                        OverrideStructuredObjectType.ProxyGroups,
                        "策略组",
                        values,
                        listOf(
                            OverrideListEditorMode.Replace,
                            OverrideListEditorMode.Start,
                            OverrideListEditorMode.End,
                        ),
                        resolveInitialEditorMode(
                            availableModes = listOf(
                                OverrideListEditorMode.Replace,
                                OverrideListEditorMode.Start,
                                OverrideListEditorMode.End,
                            ),
                            values = values,
                        ),
                        buildOverrideReferenceCatalog(config),
                    ) { updatedValues ->
                        onConfigChange(
                            config.copy(
                                proxyGroups = updatedValues.replaceValue,
                                proxyGroupsStart = updatedValues.startValue,
                                proxyGroupsEnd = updatedValues.endValue,
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun StructuredInputContent(
    title: String,
    summary: String,
    advancedSummary: String,
    onStructuredClick: () -> Unit,
    onAdvancedClick: () -> Unit,
) {
    var advancedExpanded by remember { mutableStateOf(false) }

    Column {
        OverrideSelectorCard {
            SuperArrow(
                title = "$title · 结构化编辑",
                summary = summary,
                onClick = onStructuredClick,
            )
        }
        OverrideAdvancedCard(
            title = "$title · 高级 JSON",
            summary = advancedSummary,
            expanded = advancedExpanded,
            onExpandedChange = { advancedExpanded = it },
        ) {
            SuperArrow(
                title = "打开高级编辑",
                summary = "直接编辑原始对象，用于补充结构化表单未覆盖的字段",
                onClick = onAdvancedClick,
            )
        }
    }
}

@Composable
private fun StructuredEditorEntry(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    SuperArrow(
        title = title,
        summary = summary,
        onClick = onClick,
    )
}

private fun buildStructuredSummary(
    count: Int,
    emptyHint: String,
): String {
    return if (count > 0) {
        "已配置 $count 项"
    } else {
        emptyHint
    }
}

private fun buildModifierSummary(
    replaceCount: Int,
    startCount: Int,
    endCount: Int,
    emptyHint: String,
): String {
    return buildList {
        if (replaceCount > 0) {
            add("覆盖 $replaceCount")
        }
        if (startCount > 0) {
            add("前置 $startCount")
        }
        if (endCount > 0) {
            add("后置 $endCount")
        }
    }.joinToString(" · ").ifEmpty { emptyHint }
}

private fun buildMergeModifierSummary(
    replaceCount: Int,
    mergeCount: Int,
    emptyHint: String,
): String {
    return buildList {
        if (replaceCount > 0) {
            add("覆盖 $replaceCount")
        }
        if (mergeCount > 0) {
            add("合并 $mergeCount")
        }
    }.joinToString(" · ").ifEmpty { emptyHint }
}
