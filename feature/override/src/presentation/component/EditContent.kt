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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.presentation.util.*
import top.yukonga.miuix.kmp.basic.BasicComponent

fun LazyListScope.OverrideEditContent(
    name: String,
    description: String,
    config: ConfigurationOverride,
    currentConfigProvider: () -> ConfigurationOverride,
    expandedSections: Set<OverrideEditorSection>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onSectionToggle: (OverrideEditorSection) -> Unit,
    onOpenPresetTemplate: () -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
    onEditRuleList: OpenRuleListEditor,
    onEditStringMap: OpenStringMapEditor,
    onEditJson: OpenJsonEditor,
    onEditObjectList: OpenStructuredObjectListEditor,
    onEditObjectMap: OpenObjectMapEditor,
    onEditSubRules: OpenSubRulesEditor,
) {
    item(key = "override-basic-info-section") {
        OverrideCardSection(
            title = "基本信息",
        ) {
            StringInputContent(
                title = "配置名称",
                value = name,
                placeholder = "配置名称",
                onValueChange = { onNameChange(it.orEmpty()) },
            )
            StringInputContent(
                title = "配置说明",
                value = description,
                placeholder = "配置说明",
                onValueChange = { onDescriptionChange(it.orEmpty()) },
            )
        }
    }

    item(key = "override-preset-template-section") {
        OverrideCardSection(
            title = "预设分流模板",
        ) {
            BasicComponent(
                title = "官方 MRS 常用分流",
                summary = "顶部模板编辑器，支持地区自动组和每个分流项单独开关；应用时会重建当前覆写里的规则三块。",
                onClick = onOpenPresetTemplate,
            )
        }
    }

    item(key = "override-section-list") {
        OverrideSection(title = "配置分区") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
            ) {
                OverrideEditorSection.entries.forEach { section ->
                    val directEntry = section.isDirectEntry()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(OverrideSectionTitleSpacing),
                    ) {
                        OverrideSelectorCard {
                            OverrideSectionCardHeader(
                                title = section.title,
                                summary = section.summary,
                                expanded = !directEntry && section in expandedSections,
                                onClick = {
                                    if (directEntry) {
                                        openDirectSectionEditor(
                                            section = section,
                                            config = config,
                                            currentConfigProvider = currentConfigProvider,
                                            onConfigChange = onConfigChange,
                                            onEditRuleList = onEditRuleList,
                                            onEditObjectList = onEditObjectList,
                                            onEditObjectMap = onEditObjectMap,
                                            onEditSubRules = onEditSubRules,
                                        )
                                    } else {
                                        onSectionToggle(section)
                                    }
                                },
                                showIndicator = true,
                            )
                        }
                        if (!directEntry) {
                            OverrideSectionVisibility(visible = section in expandedSections) {
                                OverrideSectionContent(
                                    section = section,
                                    config = config,
                                    onConfigChange = onConfigChange,
                                    onEditStringList = onEditStringList,
                                    onEditRuleList = onEditRuleList,
                                    onEditStringMap = onEditStringMap,
                                    onEditJson = onEditJson,
                                    onEditObjectList = onEditObjectList,
                                    onEditObjectMap = onEditObjectMap,
                                    onEditSubRules = onEditSubRules,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    item(key = "override-bottom-spacer") {
        Spacer(modifier = Modifier.height(OverrideSectionBottomSpacing))
    }
}

@Composable
private fun OverrideSectionContent(
    section: OverrideEditorSection,
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
    onEditRuleList: OpenRuleListEditor,
    onEditStringMap: OpenStringMapEditor,
    onEditJson: OpenJsonEditor,
    onEditObjectList: OpenStructuredObjectListEditor,
    onEditObjectMap: OpenObjectMapEditor,
    onEditSubRules: OpenSubRulesEditor,
) {
    when (section) {
        OverrideEditorSection.General -> GeneralEditor(config, onConfigChange, onEditStringList)
        OverrideEditorSection.Dns -> DnsEditor(
            config = config,
            onConfigChange = onConfigChange,
            onEditStringList = onEditStringList,
            onEditStringMap = onEditStringMap,
        )
        OverrideEditorSection.Sniffer -> SnifferEditor(config, onConfigChange, onEditStringList)
        OverrideEditorSection.Inbound -> InboundEditor(config, onConfigChange, onEditStringList)
        OverrideEditorSection.Tun -> TunEditor(config, onConfigChange, onEditStringList)
        OverrideEditorSection.Proxies,
        OverrideEditorSection.ProxyProviders,
        OverrideEditorSection.ProxyGroups,
        OverrideEditorSection.Rules,
        OverrideEditorSection.RuleProviders,
        OverrideEditorSection.SubRules,
        -> Unit
    }
}

private fun OverrideEditorSection.isDirectEntry(): Boolean {
    return when (this) {
        OverrideEditorSection.General,
        OverrideEditorSection.Dns,
        OverrideEditorSection.Sniffer,
        OverrideEditorSection.Inbound,
        OverrideEditorSection.Tun,
        -> false

        OverrideEditorSection.Rules,
        OverrideEditorSection.Proxies,
        OverrideEditorSection.ProxyProviders,
        OverrideEditorSection.ProxyGroups,
        OverrideEditorSection.RuleProviders,
        OverrideEditorSection.SubRules,
        -> true
    }
}

private fun openDirectSectionEditor(
    section: OverrideEditorSection,
    config: ConfigurationOverride,
    currentConfigProvider: () -> ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditRuleList: OpenRuleListEditor,
    onEditObjectList: OpenStructuredObjectListEditor,
    onEditObjectMap: OpenObjectMapEditor,
    onEditSubRules: OpenSubRulesEditor,
) {
    when (section) {
        OverrideEditorSection.Rules -> {
            val values = OverrideListModeValues(
                replaceValue = config.rules,
                startValue = config.rulesStart,
                endValue = config.rulesEnd,
            )
            val referenceCatalog = buildOverrideReferenceCatalog(currentConfigProvider())
            val availableModes = listOf(
                OverrideListEditorMode.Replace,
                OverrideListEditorMode.Start,
                OverrideListEditorMode.End,
            )
            onEditRuleList(
                "Rules",
                values,
                availableModes,
                resolveInitialEditorMode(availableModes, values),
                referenceCatalog,
            ) { updatedValues ->
                onConfigChange(
                    currentConfigProvider().copy(
                        rules = updatedValues.replaceValue,
                        rulesStart = updatedValues.startValue,
                        rulesEnd = updatedValues.endValue,
                    ),
                )
            }
        }

        OverrideEditorSection.Proxies -> {
            val values = OverrideListModeValues(
                replaceValue = config.proxies,
                startValue = config.proxiesStart,
                endValue = config.proxiesEnd,
            )
            val referenceCatalog = buildOverrideReferenceCatalog(currentConfigProvider())
            val availableModes = listOf(
                OverrideListEditorMode.Replace,
                OverrideListEditorMode.Start,
                OverrideListEditorMode.End,
            )
            onEditObjectList(
                OverrideStructuredObjectType.Proxies,
                "代理节点",
                values,
                availableModes,
                resolveInitialEditorMode(availableModes, values),
                referenceCatalog,
            ) { updatedValues ->
                onConfigChange(
                    currentConfigProvider().copy(
                        proxies = updatedValues.replaceValue,
                        proxiesStart = updatedValues.startValue,
                        proxiesEnd = updatedValues.endValue,
                    ),
                )
            }
        }

        OverrideEditorSection.ProxyGroups -> {
            val values = OverrideListModeValues(
                replaceValue = config.proxyGroups,
                startValue = config.proxyGroupsStart,
                endValue = config.proxyGroupsEnd,
            )
            val referenceCatalog = buildOverrideReferenceCatalog(currentConfigProvider())
            val availableModes = listOf(
                OverrideListEditorMode.Replace,
                OverrideListEditorMode.Start,
                OverrideListEditorMode.End,
            )
            onEditObjectList(
                OverrideStructuredObjectType.ProxyGroups,
                "策略组",
                values,
                availableModes,
                resolveInitialEditorMode(availableModes, values),
                referenceCatalog,
            ) { updatedValues ->
                onConfigChange(
                    currentConfigProvider().copy(
                        proxyGroups = updatedValues.replaceValue,
                        proxyGroupsStart = updatedValues.startValue,
                        proxyGroupsEnd = updatedValues.endValue,
                    ),
                )
            }
        }

        OverrideEditorSection.ProxyProviders -> {
            val values = OverrideListModeValues(
                replaceValue = config.proxyProviders,
                mergeValue = config.proxyProvidersMerge,
            )
            val availableModes = listOf(
                OverrideListEditorMode.Replace,
                OverrideListEditorMode.Merge,
            )
            onEditObjectMap(
                OverrideStructuredMapType.ProxyProviders,
                "代理提供者",
                values,
                availableModes,
                resolveInitialEditorMode(availableModes, values),
            ) { updatedValues ->
                onConfigChange(
                    currentConfigProvider().copy(
                        proxyProviders = updatedValues.replaceValue,
                        proxyProvidersMerge = updatedValues.mergeValue,
                    ),
                )
            }
        }

        OverrideEditorSection.RuleProviders -> {
            val values = OverrideListModeValues(
                replaceValue = config.ruleProviders,
                mergeValue = config.ruleProvidersMerge,
            )
            val availableModes = listOf(
                OverrideListEditorMode.Replace,
                OverrideListEditorMode.Merge,
            )
            onEditObjectMap(
                OverrideStructuredMapType.RuleProviders,
                "规则提供者",
                values,
                availableModes,
                resolveInitialEditorMode(availableModes, values),
            ) { updatedValues ->
                onConfigChange(
                    currentConfigProvider().copy(
                        ruleProviders = updatedValues.replaceValue,
                        ruleProvidersMerge = updatedValues.mergeValue,
                    ),
                )
            }
        }

        OverrideEditorSection.SubRules -> {
            val values = OverrideListModeValues(
                replaceValue = config.subRules,
                mergeValue = config.subRulesMerge,
            )
            val referenceCatalog = buildOverrideReferenceCatalog(currentConfigProvider())
            val availableModes = listOf(
                OverrideListEditorMode.Replace,
                OverrideListEditorMode.Merge,
            )
            onEditSubRules(
                "子规则",
                values,
                availableModes,
                resolveInitialEditorMode(availableModes, values),
                referenceCatalog,
            ) { updatedValues ->
                onConfigChange(
                    currentConfigProvider().copy(
                        subRules = updatedValues.replaceValue,
                        subRulesMerge = updatedValues.mergeValue,
                    ),
                )
            }
        }

        OverrideEditorSection.General,
        OverrideEditorSection.Dns,
        OverrideEditorSection.Sniffer,
        OverrideEditorSection.Inbound,
        OverrideEditorSection.Tun,
        -> Unit
    }
}
