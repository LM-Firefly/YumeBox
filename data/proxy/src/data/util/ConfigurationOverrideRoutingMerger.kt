package com.github.yumelira.yumebox.data.util

import com.github.yumelira.yumebox.core.model.ConfigurationOverrideRoutingSection

internal object ConfigurationOverrideRoutingMerger {

    fun merge(
        base: ConfigurationOverrideRoutingSection,
        incoming: ConfigurationOverrideRoutingSection,
    ): ConfigurationOverrideRoutingSection {
        return base.copy(
            ruleProviders = MergeHelper.mergeProviderMap(
                base = base.ruleProviders,
                replace = incoming.ruleProviders,
                merge = incoming.ruleProvidersMerge,
            ),
            ruleProvidersMerge = MergeHelper.mergeProviderMap(
                base = base.ruleProvidersMerge,
                replace = incoming.ruleProvidersMerge,
                merge = null,
            ),
            proxyGroups = MergeHelper.mergeProxyGroupList(base.proxyGroups, incoming.proxyGroups),
            proxyGroupsStart = MergeHelper.mergeProxyGroupList(base.proxyGroupsStart, incoming.proxyGroupsStart),
            proxyGroupsEnd = MergeHelper.mergeProxyGroupList(base.proxyGroupsEnd, incoming.proxyGroupsEnd),
            rules = MergeHelper.mergeList(base.rules, incoming.rules),
            rulesStart = MergeHelper.mergeList(base.rulesStart, incoming.rulesStart),
            rulesEnd = MergeHelper.mergeList(base.rulesEnd, incoming.rulesEnd),
            subRules = MergeHelper.mergeMap(
                base = base.subRules,
                replace = incoming.subRules,
                merge = incoming.subRulesMerge,
            ),
            subRulesMerge = MergeHelper.mergeMap(
                base = base.subRulesMerge,
                replace = incoming.subRulesMerge,
                merge = null,
            ),
        )
    }
}
