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



package com.github.yumelira.yumebox.presentation.util

import com.github.yumelira.yumebox.core.model.ConfigurationOverride

data class OverrideReferenceCatalog(
    val proxyNames: List<String> = emptyList(),
    val proxyGroupNames: List<String> = emptyList(),
    val subRuleNames: List<String> = emptyList(),
    val ruleProviderNames: List<String> = emptyList(),
)

fun buildOverrideReferenceCatalog(config: ConfigurationOverride): OverrideReferenceCatalog {
    return OverrideReferenceCatalog(
        proxyNames = collectProxyNames(
            OverrideListModeValues(
                replaceValue = parseProxyDrafts(config.proxies),
                startValue = parseProxyDrafts(config.proxiesStart),
                endValue = parseProxyDrafts(config.proxiesEnd),
            ),
        ),
        proxyGroupNames = collectProxyGroupNames(
            OverrideListModeValues(
                replaceValue = parseProxyGroupDrafts(config.proxyGroups),
                startValue = parseProxyGroupDrafts(config.proxyGroupsStart),
                endValue = parseProxyGroupDrafts(config.proxyGroupsEnd),
            ),
        ),
        subRuleNames = collectSubRuleNames(
            OverrideListModeValues(
                replaceValue = parseSubRuleGroupDrafts(config.subRules),
                mergeValue = parseSubRuleGroupDrafts(config.subRulesMerge),
            ),
        ),
        ruleProviderNames = collectRuleProviderNames(
            OverrideListModeValues(
                replaceValue = parseKeyedObjectDrafts(config.ruleProviders),
                mergeValue = parseKeyedObjectDrafts(config.ruleProvidersMerge),
            ),
        ),
    )
}

fun collectProxyNames(
    values: OverrideListModeValues<List<OverrideProxyDraft>>,
): List<String> {
    return collectOrderedNames(
        values.replaceValue.orEmpty().map(OverrideProxyDraft::name),
        values.startValue.orEmpty().map(OverrideProxyDraft::name),
        values.endValue.orEmpty().map(OverrideProxyDraft::name),
    )
}

fun collectProxyGroupNames(
    values: OverrideListModeValues<List<OverrideProxyGroupDraft>>,
): List<String> {
    return collectOrderedNames(
        values.replaceValue.orEmpty().map(OverrideProxyGroupDraft::name),
        values.startValue.orEmpty().map(OverrideProxyGroupDraft::name),
        values.endValue.orEmpty().map(OverrideProxyGroupDraft::name),
    )
}

fun collectSubRuleNames(
    values: OverrideListModeValues<List<OverrideSubRuleGroupDraft>>,
): List<String> {
    return collectOrderedNames(
        values.replaceValue.orEmpty().map(OverrideSubRuleGroupDraft::name),
        values.mergeValue.orEmpty().map(OverrideSubRuleGroupDraft::name),
    )
}

fun collectRuleProviderNames(
    values: OverrideListModeValues<List<OverrideKeyedObjectDraft>>,
): List<String> {
    return collectOrderedNames(
        values.replaceValue.orEmpty().map(OverrideKeyedObjectDraft::key),
        values.mergeValue.orEmpty().map(OverrideKeyedObjectDraft::key),
    )
}

fun buildOrderedProxyGroupMembers(
    catalog: OverrideReferenceCatalog,
    selectedKnownValues: Collection<String>,
    customValues: List<String>,
): List<String> {
    val orderedValues = LinkedHashSet<String>()
    catalog.proxyNames.forEach { name ->
        if (name in selectedKnownValues) {
            orderedValues += name
        }
    }
    catalog.proxyGroupNames.forEach { name ->
        if (name in selectedKnownValues) {
            orderedValues += name
        }
    }
    customValues.forEach { name ->
        val normalizedName = name.trim()
        if (normalizedName.isNotBlank() && normalizedName !in orderedValues) {
            orderedValues += normalizedName
        }
    }
    return orderedValues.toList()
}

private fun collectOrderedNames(vararg groups: List<String>): List<String> {
    val orderedNames = LinkedHashSet<String>()
    groups.forEach { values ->
        values.forEach { rawName ->
            val normalizedName = rawName.trim()
            if (normalizedName.isNotBlank()) {
                orderedNames += normalizedName
            }
        }
    }
    return orderedNames.toList()
}
