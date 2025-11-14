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
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.serialization.json.JsonElement
import top.yukonga.miuix.kmp.preference.ArrowPreference

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
        Title(MLang.Override.Form.RuleChain)
        OverrideSelectorCard {
            StructuredEditorEntry(
                title = MLang.Override.Editor.Rules,
                summary = buildModifierSummary(
                    replaceCount = config.rules?.size ?: 0,
                    startCount = config.rulesStart?.size ?: 0,
                    endCount = config.rulesEnd?.size ?: 0,
                    emptyHint = MLang.Override.Form.RuleChainNotSet,
                ),
                onClick = {
                    val values = OverrideListModeValues(
                        replaceValue = config.rules,
                        startValue = config.rulesStart,
                        endValue = config.rulesEnd,
                    )
                    onEditRuleList(
                        MLang.Override.Editor.Rules,
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
        Title(MLang.Override.Form.SubRules)
        StructuredInputContent(
            title = MLang.Override.Form.SubRules,
            summary = buildMergeModifierSummary(
                replaceCount = config.subRules?.size ?: 0,
                mergeCount = config.subRulesMerge?.size ?: 0,
                emptyHint = MLang.Override.Form.SubRulesHint,
            ),
            advancedSummary = MLang.Override.Form.SubRulesAdvanced,
            onStructuredClick = {
                val values = OverrideListModeValues(
                    replaceValue = config.subRules,
                    mergeValue = config.subRulesMerge,
                )
                    onEditSubRules(
                        MLang.Override.Form.SubRules,
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
                    MLang.Override.Form.SubRules,
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
        Title(MLang.Override.Form.RuleProviders)
        StructuredInputContent(
            title = MLang.Override.Form.RuleProviders,
            summary = buildMergeModifierSummary(
                replaceCount = config.ruleProviders?.size ?: 0,
                mergeCount = config.ruleProvidersMerge?.size ?: 0,
                emptyHint = MLang.Override.Form.RuleProvidersHint,
            ),
            advancedSummary = MLang.Override.Form.RuleProvidersAdvanced,
            onStructuredClick = {
                val values = OverrideListModeValues(
                    replaceValue = config.ruleProviders,
                    mergeValue = config.ruleProvidersMerge,
                )
                onEditObjectMap(
                    OverrideStructuredMapType.RuleProviders,
                    MLang.Override.Form.RuleProviders,
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
                    MLang.Override.Form.RuleProviders,
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
        Title(MLang.Override.Form.ProxyNodes)
        OverrideSelectorCard {
            StructuredEditorEntry(
                title = MLang.Override.Form.ProxyNodes,
                summary = buildModifierSummary(
                    replaceCount = config.proxies?.size ?: 0,
                    startCount = config.proxiesStart?.size ?: 0,
                    endCount = config.proxiesEnd?.size ?: 0,
                    emptyHint = MLang.Override.Form.ProxyNodesHint,
                ),
                onClick = {
                    val values = OverrideListModeValues(
                        replaceValue = config.proxies,
                        startValue = config.proxiesStart,
                        endValue = config.proxiesEnd,
                    )
                    onEditObjectList(
                        OverrideStructuredObjectType.Proxies,
                        MLang.Override.Form.ProxyNodes,
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
        Title(MLang.Override.Form.ProxyProviders)
        StructuredInputContent(
            title = MLang.Override.Form.ProxyProviders,
            summary = buildMergeModifierSummary(
                replaceCount = config.proxyProviders?.size ?: 0,
                mergeCount = config.proxyProvidersMerge?.size ?: 0,
                emptyHint = MLang.Override.Form.ProxyProvidersHint,
            ),
            advancedSummary = MLang.Override.Form.ProxyProvidersAdvanced,
            onStructuredClick = {
                val values = OverrideListModeValues(
                    replaceValue = config.proxyProviders,
                    mergeValue = config.proxyProvidersMerge,
                )
                onEditObjectMap(
                    OverrideStructuredMapType.ProxyProviders,
                    MLang.Override.Form.ProxyProviders,
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
                    MLang.Override.Form.ProxyProviders,
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
        Title(MLang.Override.Form.ProxyGroups)
        OverrideSelectorCard {
            StructuredEditorEntry(
                title = MLang.Override.Form.ProxyGroups,
                summary = buildModifierSummary(
                    replaceCount = config.proxyGroups?.size ?: 0,
                    startCount = config.proxyGroupsStart?.size ?: 0,
                    endCount = config.proxyGroupsEnd?.size ?: 0,
                    emptyHint = MLang.Override.Form.ProxyGroupsHint,
                ),
                onClick = {
                    val values = OverrideListModeValues(
                        replaceValue = config.proxyGroups,
                        startValue = config.proxyGroupsStart,
                        endValue = config.proxyGroupsEnd,
                    )
                    onEditObjectList(
                        OverrideStructuredObjectType.ProxyGroups,
                        MLang.Override.Form.ProxyGroups,
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
            ArrowPreference(
                title = MLang.Override.Form.StructuredEdit.format(title),
                summary = summary,
                onClick = onStructuredClick,
            )
        }
        OverrideAdvancedCard(
            title = MLang.Override.Form.AdvancedJson.format(title),
            summary = advancedSummary,
            expanded = advancedExpanded,
            onExpandedChange = { advancedExpanded = it },
        ) {
            ArrowPreference(
                title = MLang.Override.Form.OpenAdvancedEdit,
                summary = MLang.Override.Form.OpenAdvancedEditSummary,
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
    ArrowPreference(
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
        MLang.Override.Form.ItemsConfigured.format(count)
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
            add(MLang.Override.Modifier.ItemsCount.format(replaceCount))
        }
        if (startCount > 0) {
            add(MLang.Override.Modifier.Start)
        }
        if (endCount > 0) {
            add(MLang.Override.Modifier.End)
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
            add(MLang.Override.Modifier.ItemsCount.format(replaceCount))
        }
        if (mergeCount > 0) {
            add(MLang.Override.Modifier.Merge)
        }
    }.joinToString(" · ").ifEmpty { emptyHint }
}
