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
import androidx.compose.ui.unit.Dp
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.presentation.util.*
import dev.oom_wg.purejoy.mlang.MLang

private val OverrideEditorSections = OverrideEditorSection.entries.toList()

fun LazyListScope.OverrideEditContent(
    name: String,
    description: String,
    config: ConfigurationOverride,
    referenceCatalog: OverrideReferenceCatalog,
    currentConfigProvider: () -> ConfigurationOverride,
    expandedSectionNames: Set<String>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onSectionToggle: (OverrideEditorSection) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
    onEditRuleList: OpenRuleListEditor,
    onEditStringMap: OpenStringMapEditor,
    onEditJson: OpenJsonEditor,
    onEditObjectList: OpenStructuredObjectListEditor,
    onEditObjectMap: OpenObjectMapEditor,
    onEditSubRules: OpenSubRulesEditor,
) {
    item(
        key = "override-basic-info-section",
        contentType = "override-basic-info",
    ) {
        OverrideEditorListItem(
            bottomSpacing = OverrideSectionBottomSpacing,
        ) {
            OverrideCardSection(
                title = MLang.Override.Draft.BasicInfo,
            ) {
                StringInputContent(
                    title = MLang.Override.Draft.ConfigName,
                    value = name,
                    placeholder = MLang.Override.Draft.ConfigName,
                    onValueChange = { onNameChange(it.orEmpty()) },
                )
                StringInputContent(
                    title = MLang.Override.Draft.ConfigDescription,
                    value = description,
                    placeholder = MLang.Override.Draft.ConfigDescription,
                    onValueChange = { onDescriptionChange(it.orEmpty()) },
                )
            }
        }
    }

    item(
        key = "override-section-title",
        contentType = "override-section-title",
    ) {
        OverrideEditorListItem {
            Title(MLang.Override.Draft.ConfigSections)
        }
    }

    OverrideEditorSections.forEach { section ->
        item(
            key = "override-section-${section.name}",
            contentType = "override-section-card",
        ) {
            OverrideEditorListItem {
                OverrideSectionEntry(
                    section = section,
                    config = config,
                    referenceCatalog = referenceCatalog,
                    currentConfigProvider = currentConfigProvider,
                    expandedSectionNames = expandedSectionNames,
                    onConfigChange = onConfigChange,
                    onSectionToggle = onSectionToggle,
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

    item(
        key = "override-bottom-spacer",
        contentType = "override-bottom-spacer",
    ) {
        Spacer(modifier = Modifier.height(OverrideSectionBottomSpacing))
    }
}

@Composable
private fun OverrideEditorListItem(
    modifier: Modifier = Modifier,
    bottomSpacing: Dp = OverrideSectionSpacing,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomSpacing),
    ) {
        content()
    }
}

@Composable
private fun OverrideSectionEntry(
    section: OverrideEditorSection,
    config: ConfigurationOverride,
    referenceCatalog: OverrideReferenceCatalog,
    currentConfigProvider: () -> ConfigurationOverride,
    expandedSectionNames: Set<String>,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onSectionToggle: (OverrideEditorSection) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
    onEditRuleList: OpenRuleListEditor,
    onEditStringMap: OpenStringMapEditor,
    onEditJson: OpenJsonEditor,
    onEditObjectList: OpenStructuredObjectListEditor,
    onEditObjectMap: OpenObjectMapEditor,
    onEditSubRules: OpenSubRulesEditor,
) {
    val directEntry = section.isDirectEntry()
    val expanded = section.name in expandedSectionNames

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OverrideSectionTitleSpacing),
    ) {
        OverrideSelectorCard {
            OverrideSectionCardHeader(
                title = section.title,
                summary = section.summary,
                expanded = !directEntry && expanded,
                onClick = {
                    if (directEntry) {
                        openDirectSectionEditor(
                            section = section,
                            referenceCatalog = referenceCatalog,
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
            OverrideSectionVisibility(visible = expanded) {
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
    referenceCatalog: OverrideReferenceCatalog,
    currentConfigProvider: () -> ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditRuleList: OpenRuleListEditor,
    onEditObjectList: OpenStructuredObjectListEditor,
    onEditObjectMap: OpenObjectMapEditor,
    onEditSubRules: OpenSubRulesEditor,
) {
    val config = currentConfigProvider()
    when (section) {
        OverrideEditorSection.Rules -> {
            val values = OverrideListModeValues(
                replaceValue = config.rules,
                startValue = config.rulesStart,
                endValue = config.rulesEnd,
            )
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
            val availableModes = listOf(
                OverrideListEditorMode.Replace,
                OverrideListEditorMode.Start,
                OverrideListEditorMode.End,
            )
            onEditObjectList(
                OverrideStructuredObjectType.Proxies,
                MLang.Override.Form.ProxyNodes,
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
            val availableModes = listOf(
                OverrideListEditorMode.Replace,
                OverrideListEditorMode.Start,
                OverrideListEditorMode.End,
            )
            onEditObjectList(
                OverrideStructuredObjectType.ProxyGroups,
                MLang.Override.Form.ProxyGroups,
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
                MLang.Override.Form.ProxyProviders,
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
                MLang.Override.Form.RuleProviders,
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
            val availableModes = listOf(
                OverrideListEditorMode.Replace,
                OverrideListEditorMode.Merge,
            )
            onEditSubRules(
                MLang.Override.Form.SubRules,
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
            -> Unit
    }
}
