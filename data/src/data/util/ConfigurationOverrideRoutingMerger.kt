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
