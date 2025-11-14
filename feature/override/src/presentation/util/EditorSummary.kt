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
import com.github.yumelira.yumebox.presentation.viewmodel.RuleValidator

object OverrideEditorSummaryBuilder {

    fun build(config: ConfigurationOverride): OverrideEditorOverview {
        val sectionSummaries = linkedMapOf(
            OverrideEditorSection.General to buildGeneralSummary(config),
            OverrideEditorSection.Dns to buildDnsSummary(config),
            OverrideEditorSection.Sniffer to buildSnifferSummary(config),
            OverrideEditorSection.Inbound to buildInboundSummary(config),
            OverrideEditorSection.Proxies to buildProxiesSummary(config),
            OverrideEditorSection.ProxyProviders to buildProxyProvidersSummary(config),
            OverrideEditorSection.ProxyGroups to buildProxyGroupsSummary(config),
            OverrideEditorSection.Rules to buildRulesSummary(config),
            OverrideEditorSection.RuleProviders to buildRuleProvidersSummary(config),
            OverrideEditorSection.SubRules to buildSubRulesSummary(config),
        )

        val modifierGroups = sectionSummaries.values.flatMap { it.visualModes }
        return OverrideEditorOverview(
            changedFieldCount = sectionSummaries.values.sumOf { it.modifiedCount },
            activeSectionCount = sectionSummaries.values.count { it.modifiedCount > 0 },
            replaceCount = modifierGroups.count { it == OverrideModifierVisualMode.Replace },
            appendCount = modifierGroups.count {
                it == OverrideModifierVisualMode.Start || it == OverrideModifierVisualMode.End
            },
            mergeCount = modifierGroups.count { it == OverrideModifierVisualMode.Merge },
            forceCount = modifierGroups.count { it == OverrideModifierVisualMode.Force },
            sectionSummaries = sectionSummaries,
            warnings = RuleValidator.validate(config),
        )
    }

    private fun buildGeneralSummary(config: ConfigurationOverride): OverrideSectionSummary {
        val modifiedCount = countPresent(
            config.allowLan,
            config.bindAddress,
            config.mode,
            config.logLevel,
            config.findProcessMode,
            config.keepAliveInterval,
            config.keepAliveIdle,
            config.unifiedDelay,
            config.tcpConcurrent,
            config.geodataMode,
            config.ipv6,
            config.interfaceName,
            config.routingMark,
            config.geositeMatcher,
            config.globalClientFingerprint,
            config.externalController,
            config.externalControllerTLS,
            config.externalDohServer,
            config.secret,
            config.profile.storeSelected,
            config.profile.storeFakeIp,
            config.geoAutoUpdate,
            config.geoUpdateInterval,
            config.geoxurl.geoip,
            config.geoxurl.geosite,
            config.geoxurl.mmdb,
        ) + countListGroup(
            replaceValue = config.authentication,
            startValue = config.authenticationStart,
            endValue = config.authenticationEnd,
        ) + countListGroup(
            replaceValue = config.skipAuthPrefixes,
            startValue = config.skipAuthPrefixesStart,
            endValue = config.skipAuthPrefixesEnd,
        ) + countListGroup(
            replaceValue = config.lanAllowedIps,
            startValue = config.lanAllowedIpsStart,
            endValue = config.lanAllowedIpsEnd,
        ) + countListGroup(
            replaceValue = config.lanDisallowedIps,
            startValue = config.lanDisallowedIpsStart,
            endValue = config.lanDisallowedIpsEnd,
        ) + countListGroup(
            replaceValue = config.externalControllerCors.allowOrigins,
            startValue = config.externalControllerCors.allowOriginsStart,
            endValue = config.externalControllerCors.allowOriginsEnd,
        )

        return OverrideSectionSummary(
            modifiedCount = modifiedCount,
            visualModes = buildSet {
                addIfValue(config.allowLan)
                addIfValue(config.bindAddress)
                addIfValue(config.mode)
                addIfValue(config.logLevel)
                addIfValue(config.findProcessMode)
                addIfValue(config.keepAliveInterval)
                addIfValue(config.keepAliveIdle)
                addIfValue(config.unifiedDelay)
                addIfValue(config.tcpConcurrent)
                addIfValue(config.geodataMode)
                addIfValue(config.ipv6)
                addIfValue(config.interfaceName)
                addIfValue(config.routingMark)
                addIfValue(config.geositeMatcher)
                addIfValue(config.globalClientFingerprint)
                addIfValue(config.externalController)
                addIfValue(config.externalControllerTLS)
                addIfValue(config.externalDohServer)
                addIfValue(config.secret)
                addIfValue(config.profile.storeSelected)
                addIfValue(config.profile.storeFakeIp)
                addIfValue(config.geoAutoUpdate)
                addIfValue(config.geoUpdateInterval)
                addIfValue(config.geoxurl.geoip)
                addIfValue(config.geoxurl.geosite)
                addIfValue(config.geoxurl.mmdb)
                addListModes(config.authentication, config.authenticationStart, config.authenticationEnd)
                addListModes(
                    config.skipAuthPrefixes,
                    config.skipAuthPrefixesStart,
                    config.skipAuthPrefixesEnd,
                )
                addListModes(config.lanAllowedIps, config.lanAllowedIpsStart, config.lanAllowedIpsEnd)
                addListModes(
                    config.lanDisallowedIps,
                    config.lanDisallowedIpsStart,
                    config.lanDisallowedIpsEnd,
                )
                addListModes(
                    replaceValue = config.externalControllerCors.allowOrigins,
                    startValue = config.externalControllerCors.allowOriginsStart,
                    endValue = config.externalControllerCors.allowOriginsEnd,
                )
                addIfValue(config.externalControllerCors.allowPrivateNetwork)
                addIfForce(config.externalControllerCorsForce)
                addIfForce(config.geoxurlForce)
            },
        )
    }

    private fun buildInboundSummary(config: ConfigurationOverride): OverrideSectionSummary {
        return OverrideSectionSummary(
            modifiedCount = countPresent(
                config.httpPort,
                config.socksPort,
                config.redirectPort,
                config.tproxyPort,
                config.mixedPort,
            ),
            visualModes = buildSet {
                addIfValue(config.httpPort)
                addIfValue(config.socksPort)
                addIfValue(config.redirectPort)
                addIfValue(config.tproxyPort)
                addIfValue(config.mixedPort)
            },
        )
    }

    private fun buildDnsSummary(config: ConfigurationOverride): OverrideSectionSummary {
        return OverrideSectionSummary(
            modifiedCount = countPresent(
                config.dns.enable,
                config.dns.listen,
                config.dns.ipv6,
                config.dns.useHosts,
                config.dns.cacheAlgorithm,
                config.dns.enhancedMode,
                config.dns.fakeIpRange,
                config.dns.fakeIpRange6,
                config.dns.fakeIPFilterMode,
                config.dns.fakeIpTtl,
                config.app.appendSystemDns,
            ) +
                countListGroup(config.dns.nameServer, config.dns.nameServerStart, config.dns.nameServerEnd) +
                countListGroup(config.dns.fallback, config.dns.fallbackStart, config.dns.fallbackEnd) +
                countListGroup(
                    config.dns.defaultServer,
                    config.dns.defaultServerStart,
                    config.dns.defaultServerEnd,
                ) +
                countListGroup(
                    config.dns.proxyServerNameserver,
                    config.dns.proxyServerNameserverStart,
                    config.dns.proxyServerNameserverEnd,
                ) +
                countListGroup(
                    config.dns.directNameserver,
                    config.dns.directNameserverStart,
                    config.dns.directNameserverEnd,
                ) +
                countListGroup(
                    config.dns.fakeIpFilter,
                    config.dns.fakeIpFilterStart,
                    config.dns.fakeIpFilterEnd,
                ) +
                countMapGroup(config.dns.nameserverPolicy, config.dns.nameserverPolicyMerge) +
                countMapGroup(
                    config.dns.proxyServerNameserverPolicy,
                    config.dns.proxyServerNameserverPolicyMerge,
                ) +
                countMapGroup(config.hosts, config.hostsMerge),
            visualModes = buildSet {
                addIfValue(config.dns.enable)
                addIfValue(config.dns.listen)
                addIfValue(config.dns.ipv6)
                addIfValue(config.dns.useHosts)
                addIfValue(config.dns.cacheAlgorithm)
                addIfValue(config.dns.enhancedMode)
                addIfValue(config.dns.fakeIpRange)
                addIfValue(config.dns.fakeIpRange6)
                addIfValue(config.dns.fakeIPFilterMode)
                addIfValue(config.dns.fakeIpTtl)
                addIfValue(config.app.appendSystemDns)
                addListModes(
                    config.dns.nameServer,
                    config.dns.nameServerStart,
                    config.dns.nameServerEnd,
                )
                addListModes(config.dns.fallback, config.dns.fallbackStart, config.dns.fallbackEnd)
                addListModes(
                    config.dns.defaultServer,
                    config.dns.defaultServerStart,
                    config.dns.defaultServerEnd,
                )
                addListModes(
                    config.dns.proxyServerNameserver,
                    config.dns.proxyServerNameserverStart,
                    config.dns.proxyServerNameserverEnd,
                )
                addListModes(
                    config.dns.directNameserver,
                    config.dns.directNameserverStart,
                    config.dns.directNameserverEnd,
                )
                addListModes(
                    config.dns.fakeIpFilter,
                    config.dns.fakeIpFilterStart,
                    config.dns.fakeIpFilterEnd,
                )
                addMapModes(config.dns.nameserverPolicy, config.dns.nameserverPolicyMerge)
                addMapModes(
                    config.dns.proxyServerNameserverPolicy,
                    config.dns.proxyServerNameserverPolicyMerge,
                )
                addMapModes(config.hosts, config.hostsMerge)
                addIfForce(config.dnsForce)
            },
        )
    }

    private fun buildSnifferSummary(config: ConfigurationOverride): OverrideSectionSummary {
        return OverrideSectionSummary(
            modifiedCount = countPresent(
                config.sniffer.enable,
                config.sniffer.forceDnsMapping,
                config.sniffer.parsePureIp,
                config.sniffer.overrideDestination,
                config.sniffer.sniff.http.overrideDestination,
                config.sniffer.sniff.tls.overrideDestination,
                config.sniffer.sniff.quic.overrideDestination,
            ) +
                countListGroup(
                    config.sniffer.sniff.http.ports,
                    config.sniffer.sniff.http.portsStart,
                    config.sniffer.sniff.http.portsEnd,
                ) +
                countListGroup(
                    config.sniffer.sniff.tls.ports,
                    config.sniffer.sniff.tls.portsStart,
                    config.sniffer.sniff.tls.portsEnd,
                ) +
                countListGroup(
                    config.sniffer.sniff.quic.ports,
                    config.sniffer.sniff.quic.portsStart,
                    config.sniffer.sniff.quic.portsEnd,
                ) +
                countListGroup(
                    config.sniffer.forceDomain,
                    config.sniffer.forceDomainStart,
                    config.sniffer.forceDomainEnd,
                ) +
                countListGroup(
                    config.sniffer.skipDomain,
                    config.sniffer.skipDomainStart,
                    config.sniffer.skipDomainEnd,
                ) +
                countListGroup(
                    config.sniffer.skipSrcAddress,
                    config.sniffer.skipSrcAddressStart,
                    config.sniffer.skipSrcAddressEnd,
                ) +
                countListGroup(
                    config.sniffer.skipDstAddress,
                    config.sniffer.skipDstAddressStart,
                    config.sniffer.skipDstAddressEnd,
                ),
            visualModes = buildSet {
                addIfValue(config.sniffer.enable)
                addIfValue(config.sniffer.forceDnsMapping)
                addIfValue(config.sniffer.parsePureIp)
                addIfValue(config.sniffer.overrideDestination)
                addIfValue(config.sniffer.sniff.http.overrideDestination)
                addIfValue(config.sniffer.sniff.tls.overrideDestination)
                addIfValue(config.sniffer.sniff.quic.overrideDestination)
                addListModes(
                    config.sniffer.sniff.http.ports,
                    config.sniffer.sniff.http.portsStart,
                    config.sniffer.sniff.http.portsEnd,
                )
                addListModes(
                    config.sniffer.sniff.tls.ports,
                    config.sniffer.sniff.tls.portsStart,
                    config.sniffer.sniff.tls.portsEnd,
                )
                addListModes(
                    config.sniffer.sniff.quic.ports,
                    config.sniffer.sniff.quic.portsStart,
                    config.sniffer.sniff.quic.portsEnd,
                )
                addListModes(
                    config.sniffer.forceDomain,
                    config.sniffer.forceDomainStart,
                    config.sniffer.forceDomainEnd,
                )
                addListModes(
                    config.sniffer.skipDomain,
                    config.sniffer.skipDomainStart,
                    config.sniffer.skipDomainEnd,
                )
                addListModes(
                    config.sniffer.skipSrcAddress,
                    config.sniffer.skipSrcAddressStart,
                    config.sniffer.skipSrcAddressEnd,
                )
                addListModes(
                    config.sniffer.skipDstAddress,
                    config.sniffer.skipDstAddressStart,
                    config.sniffer.skipDstAddressEnd,
                )
                addIfForce(config.snifferForce)
            },
        )
    }

    private fun buildRulesSummary(config: ConfigurationOverride): OverrideSectionSummary {
        return OverrideSectionSummary(
            modifiedCount = countListGroup(config.rules, config.rulesStart, config.rulesEnd),
            visualModes = buildSet {
                addListModes(config.rules, config.rulesStart, config.rulesEnd)
            },
        )
    }

    private fun buildProxiesSummary(config: ConfigurationOverride): OverrideSectionSummary {
        return OverrideSectionSummary(
            modifiedCount = countListGroup(config.proxies, config.proxiesStart, config.proxiesEnd),
            visualModes = buildSet {
                addListModes(config.proxies, config.proxiesStart, config.proxiesEnd)
            },
        )
    }

    private fun buildProxyProvidersSummary(config: ConfigurationOverride): OverrideSectionSummary {
        return OverrideSectionSummary(
            modifiedCount = countMapGroup(config.proxyProviders, config.proxyProvidersMerge),
            visualModes = buildSet {
                addMapModes(config.proxyProviders, config.proxyProvidersMerge)
            },
        )
    }

    private fun buildProxyGroupsSummary(config: ConfigurationOverride): OverrideSectionSummary {
        return OverrideSectionSummary(
            modifiedCount = countListGroup(
                config.proxyGroups,
                config.proxyGroupsStart,
                config.proxyGroupsEnd,
            ),
            visualModes = buildSet {
                addListModes(config.proxyGroups, config.proxyGroupsStart, config.proxyGroupsEnd)
            },
        )
    }

    private fun buildRuleProvidersSummary(config: ConfigurationOverride): OverrideSectionSummary {
        return OverrideSectionSummary(
            modifiedCount = countMapGroup(config.ruleProviders, config.ruleProvidersMerge),
            visualModes = buildSet {
                addMapModes(config.ruleProviders, config.ruleProvidersMerge)
            },
        )
    }

    private fun buildSubRulesSummary(config: ConfigurationOverride): OverrideSectionSummary {
        return OverrideSectionSummary(
            modifiedCount = countMapGroup(config.subRules, config.subRulesMerge),
            visualModes = buildSet {
                addMapModes(config.subRules, config.subRulesMerge)
            },
        )
    }

    private fun countPresent(vararg values: Any?): Int {
        return values.count(::isConfigured)
    }

    private fun countListGroup(
        replaceValue: Any?,
        startValue: Any?,
        endValue: Any?,
    ): Int {
        return if (isConfigured(replaceValue) || isConfigured(startValue) || isConfigured(endValue)) 1 else 0
    }

    private fun countMapGroup(
        replaceValue: Any?,
        mergeValue: Any?,
    ): Int {
        return if (isConfigured(replaceValue) || isConfigured(mergeValue)) 1 else 0
    }

    private fun MutableSet<OverrideModifierVisualMode>.addListModes(
        replaceValue: Any?,
        startValue: Any?,
        endValue: Any?,
    ) {
        if (isConfigured(replaceValue)) {
            add(OverrideModifierVisualMode.Replace)
        }
        if (isConfigured(startValue)) {
            add(OverrideModifierVisualMode.Start)
        }
        if (isConfigured(endValue)) {
            add(OverrideModifierVisualMode.End)
        }
    }

    private fun MutableSet<OverrideModifierVisualMode>.addMapModes(
        replaceValue: Any?,
        mergeValue: Any?,
    ) {
        if (isConfigured(replaceValue)) {
            add(OverrideModifierVisualMode.Replace)
        }
        if (isConfigured(mergeValue)) {
            add(OverrideModifierVisualMode.Merge)
        }
    }

    private fun MutableSet<OverrideModifierVisualMode>.addIfValue(value: Any?) {
        if (isConfigured(value)) {
            add(OverrideModifierVisualMode.Replace)
        }
    }

    private fun MutableSet<OverrideModifierVisualMode>.addIfForce(value: Any?) {
        if (value != null) {
            add(OverrideModifierVisualMode.Force)
        }
    }

    private fun isConfigured(value: Any?): Boolean {
        return when (value) {
            null -> false
            is String -> value.isNotBlank()
            is Collection<*> -> value.isNotEmpty()
            is Map<*, *> -> value.isNotEmpty()
            else -> true
        }
    }
}
