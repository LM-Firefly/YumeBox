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

package com.github.yumelira.yumebox.core.model

import kotlinx.serialization.json.JsonElement

data class ConfigurationOverrideRoutingSection(
    val ruleProviders: Map<String, Map<String, JsonElement>>?,
    val ruleProvidersMerge: Map<String, Map<String, JsonElement>>?,
    val proxyGroups: List<Map<String, JsonElement>>?,
    val proxyGroupsStart: List<Map<String, JsonElement>>?,
    val proxyGroupsEnd: List<Map<String, JsonElement>>?,
    val rules: List<String>?,
    val rulesStart: List<String>?,
    val rulesEnd: List<String>?,
    val subRules: Map<String, List<String>>?,
    val subRulesMerge: Map<String, List<String>>?,
)

data class ConfigurationOverrideProxyResourceSection(
    val proxies: List<Map<String, JsonElement>>?,
    val proxiesStart: List<Map<String, JsonElement>>?,
    val proxiesEnd: List<Map<String, JsonElement>>?,
    val proxyProviders: Map<String, Map<String, JsonElement>>?,
    val proxyProvidersMerge: Map<String, Map<String, JsonElement>>?,
)

data class ConfigurationOverrideDnsSection(
    val dns: ConfigurationOverride.Dns,
    val dnsForce: ConfigurationOverride.Dns?,
)

data class ConfigurationOverrideSnifferSection(
    val sniffer: ConfigurationOverride.Sniffer,
    val snifferForce: ConfigurationOverride.Sniffer?,
)

data class ConfigurationOverrideSupportSection(
    val app: ConfigurationOverride.App,
    val profile: ConfigurationOverride.Profile,
    val geoxurl: ConfigurationOverride.GeoXUrl,
    val geoxurlForce: ConfigurationOverride.GeoXUrl?,
)

fun ConfigurationOverride.routingSection(): ConfigurationOverrideRoutingSection =
    ConfigurationOverrideRoutingSection(
        ruleProviders = ruleProviders,
        ruleProvidersMerge = ruleProvidersMerge,
        proxyGroups = proxyGroups,
        proxyGroupsStart = proxyGroupsStart,
        proxyGroupsEnd = proxyGroupsEnd,
        rules = rules,
        rulesStart = rulesStart,
        rulesEnd = rulesEnd,
        subRules = subRules,
        subRulesMerge = subRulesMerge,
    )

fun ConfigurationOverride.withRoutingSection(
    section: ConfigurationOverrideRoutingSection,
): ConfigurationOverride = copy(
    ruleProviders = section.ruleProviders,
    ruleProvidersMerge = section.ruleProvidersMerge,
    proxyGroups = section.proxyGroups,
    proxyGroupsStart = section.proxyGroupsStart,
    proxyGroupsEnd = section.proxyGroupsEnd,
    rules = section.rules,
    rulesStart = section.rulesStart,
    rulesEnd = section.rulesEnd,
    subRules = section.subRules,
    subRulesMerge = section.subRulesMerge,
)

fun ConfigurationOverride.proxyResourceSection(): ConfigurationOverrideProxyResourceSection =
    ConfigurationOverrideProxyResourceSection(
        proxies = proxies,
        proxiesStart = proxiesStart,
        proxiesEnd = proxiesEnd,
        proxyProviders = proxyProviders,
        proxyProvidersMerge = proxyProvidersMerge,
    )

fun ConfigurationOverride.withProxyResourceSection(
    section: ConfigurationOverrideProxyResourceSection,
): ConfigurationOverride = copy(
    proxies = section.proxies,
    proxiesStart = section.proxiesStart,
    proxiesEnd = section.proxiesEnd,
    proxyProviders = section.proxyProviders,
    proxyProvidersMerge = section.proxyProvidersMerge,
)

fun ConfigurationOverride.dnsSection(): ConfigurationOverrideDnsSection =
    ConfigurationOverrideDnsSection(
        dns = dns,
        dnsForce = dnsForce,
    )

fun ConfigurationOverride.withDnsSection(
    section: ConfigurationOverrideDnsSection,
): ConfigurationOverride = copy(
    dns = section.dns,
    dnsForce = section.dnsForce,
)

fun ConfigurationOverride.snifferSection(): ConfigurationOverrideSnifferSection =
    ConfigurationOverrideSnifferSection(
        sniffer = sniffer,
        snifferForce = snifferForce,
    )

fun ConfigurationOverride.withSnifferSection(
    section: ConfigurationOverrideSnifferSection,
): ConfigurationOverride = copy(
    sniffer = section.sniffer,
    snifferForce = section.snifferForce,
)

fun ConfigurationOverride.supportSection(): ConfigurationOverrideSupportSection =
    ConfigurationOverrideSupportSection(
        app = app,
        profile = profile,
        geoxurl = geoxurl,
        geoxurlForce = geoxurlForce,
    )

fun ConfigurationOverride.withSupportSection(
    section: ConfigurationOverrideSupportSection,
): ConfigurationOverride = copy(
    app = section.app,
    profile = section.profile,
    geoxurl = section.geoxurl,
    geoxurlForce = section.geoxurlForce,
)
