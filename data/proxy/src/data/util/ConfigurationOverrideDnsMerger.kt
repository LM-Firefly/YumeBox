package com.github.yumelira.yumebox.data.util

import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.ConfigurationOverrideDnsSection

internal object ConfigurationOverrideDnsMerger {

    fun merge(
        base: ConfigurationOverrideDnsSection,
        incoming: ConfigurationOverrideDnsSection,
    ): ConfigurationOverrideDnsSection {
        return ConfigurationOverrideDnsSection(
            dns = mergeDns(base.dns, incoming),
            dnsForce = incoming.dnsForce ?: base.dnsForce,
        )
    }

    private fun mergeDns(
        base: ConfigurationOverride.Dns,
        incoming: ConfigurationOverrideDnsSection,
    ): ConfigurationOverride.Dns {
        incoming.dnsForce?.let { return it }

        val incomingDns = incoming.dns
        return base.copy(
            enable = incomingDns.enable ?: base.enable,
            cacheAlgorithm = incomingDns.cacheAlgorithm ?: base.cacheAlgorithm,
            preferH3 = incomingDns.preferH3 ?: base.preferH3,
            listen = incomingDns.listen ?: base.listen,
            ipv6 = incomingDns.ipv6 ?: base.ipv6,
            ipv6Timeout = incomingDns.ipv6Timeout ?: base.ipv6Timeout,
            useHosts = incomingDns.useHosts ?: base.useHosts,
            useSystemHosts = incomingDns.useSystemHosts ?: base.useSystemHosts,
            respectRules = incomingDns.respectRules ?: base.respectRules,
            enhancedMode = incomingDns.enhancedMode ?: base.enhancedMode,
            fakeIpRange = incomingDns.fakeIpRange ?: base.fakeIpRange,
            fakeIpRange6 = incomingDns.fakeIpRange6 ?: base.fakeIpRange6,
            fakeIPFilterMode = incomingDns.fakeIPFilterMode ?: base.fakeIPFilterMode,
            fakeIpTtl = incomingDns.fakeIpTtl ?: base.fakeIpTtl,
            cacheMaxSize = incomingDns.cacheMaxSize ?: base.cacheMaxSize,
            directFollowPolicy = incomingDns.directFollowPolicy ?: base.directFollowPolicy,
            nameServer = MergeHelper.mergeList(base.nameServer, incomingDns.nameServer),
            nameServerStart = MergeHelper.mergeList(base.nameServerStart, incomingDns.nameServerStart),
            nameServerEnd = MergeHelper.mergeList(base.nameServerEnd, incomingDns.nameServerEnd),
            fallback = MergeHelper.mergeList(base.fallback, incomingDns.fallback),
            fallbackStart = MergeHelper.mergeList(base.fallbackStart, incomingDns.fallbackStart),
            fallbackEnd = MergeHelper.mergeList(base.fallbackEnd, incomingDns.fallbackEnd),
            defaultServer = MergeHelper.mergeList(base.defaultServer, incomingDns.defaultServer),
            defaultServerStart = MergeHelper.mergeList(base.defaultServerStart, incomingDns.defaultServerStart),
            defaultServerEnd = MergeHelper.mergeList(base.defaultServerEnd, incomingDns.defaultServerEnd),
            fakeIpFilter = MergeHelper.mergeList(base.fakeIpFilter, incomingDns.fakeIpFilter),
            fakeIpFilterStart = MergeHelper.mergeList(base.fakeIpFilterStart, incomingDns.fakeIpFilterStart),
            fakeIpFilterEnd = MergeHelper.mergeList(base.fakeIpFilterEnd, incomingDns.fakeIpFilterEnd),
            proxyServerNameserver = MergeHelper.mergeList(
                base.proxyServerNameserver,
                incomingDns.proxyServerNameserver,
            ),
            proxyServerNameserverStart = MergeHelper.mergeList(
                base.proxyServerNameserverStart,
                incomingDns.proxyServerNameserverStart,
            ),
            proxyServerNameserverEnd = MergeHelper.mergeList(
                base.proxyServerNameserverEnd,
                incomingDns.proxyServerNameserverEnd,
            ),
            directNameserver = MergeHelper.mergeList(base.directNameserver, incomingDns.directNameserver),
            directNameserverStart = MergeHelper.mergeList(base.directNameserverStart, incomingDns.directNameserverStart),
            directNameserverEnd = MergeHelper.mergeList(base.directNameserverEnd, incomingDns.directNameserverEnd),
            nameserverPolicy = MergeHelper.mergeMap(
                base = base.nameserverPolicy,
                replace = incomingDns.nameserverPolicy,
                merge = incomingDns.nameserverPolicyMerge,
            ),
            nameserverPolicyMerge = MergeHelper.mergeMap(
                base = base.nameserverPolicyMerge,
                replace = incomingDns.nameserverPolicyMerge,
                merge = null,
            ),
            proxyServerNameserverPolicy = MergeHelper.mergeMap(
                base = base.proxyServerNameserverPolicy,
                replace = incomingDns.proxyServerNameserverPolicy,
                merge = incomingDns.proxyServerNameserverPolicyMerge,
            ),
            proxyServerNameserverPolicyMerge = MergeHelper.mergeMap(
                base = base.proxyServerNameserverPolicyMerge,
                replace = incomingDns.proxyServerNameserverPolicyMerge,
                merge = null,
            ),
            fallbackFilter = mergeFallbackFilter(base.fallbackFilter, incomingDns),
            fallbackFilterForce = incomingDns.fallbackFilterForce ?: base.fallbackFilterForce,
        )
    }

    private fun mergeFallbackFilter(
        base: ConfigurationOverride.DnsFallbackFilter,
        incoming: ConfigurationOverride.Dns,
    ): ConfigurationOverride.DnsFallbackFilter {
        incoming.fallbackFilterForce?.let { return it }

        val incomingFilter = incoming.fallbackFilter
        return base.copy(
            geoIp = incomingFilter.geoIp ?: base.geoIp,
            geoIpCode = incomingFilter.geoIpCode ?: base.geoIpCode,
            ipcidr = MergeHelper.mergeList(base.ipcidr, incomingFilter.ipcidr),
            ipcidrStart = MergeHelper.mergeList(base.ipcidrStart, incomingFilter.ipcidrStart),
            ipcidrEnd = MergeHelper.mergeList(base.ipcidrEnd, incomingFilter.ipcidrEnd),
            geosite = MergeHelper.mergeList(base.geosite, incomingFilter.geosite),
            geositeStart = MergeHelper.mergeList(base.geositeStart, incomingFilter.geositeStart),
            geositeEnd = MergeHelper.mergeList(base.geositeEnd, incomingFilter.geositeEnd),
            domain = MergeHelper.mergeList(base.domain, incomingFilter.domain),
            domainStart = MergeHelper.mergeList(base.domainStart, incomingFilter.domainStart),
            domainEnd = MergeHelper.mergeList(base.domainEnd, incomingFilter.domainEnd),
        )
    }
}
