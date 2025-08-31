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

import com.github.yumelira.yumebox.core.model.ConfigurationOverride

internal object ConfigurationOverrideMerger {

    fun merge(base: ConfigurationOverride, incoming: ConfigurationOverride): ConfigurationOverride {
        return base.copy(
            httpPort = incoming.httpPort ?: base.httpPort,
            socksPort = incoming.socksPort ?: base.socksPort,
            redirectPort = incoming.redirectPort ?: base.redirectPort,
            tproxyPort = incoming.tproxyPort ?: base.tproxyPort,
            mixedPort = incoming.mixedPort ?: base.mixedPort,

            authentication = MergeHelper.mergeList(
                base = base.authentication,
                replace = incoming.authentication,
                start = null,
                end = null,
            ),
            authenticationStart = MergeHelper.mergeList(
                base = base.authenticationStart,
                replace = incoming.authenticationStart,
                start = null,
                end = null,
            ),
            authenticationEnd = MergeHelper.mergeList(
                base = base.authenticationEnd,
                replace = incoming.authenticationEnd,
                start = null,
                end = null,
            ),
            skipAuthPrefixes = MergeHelper.mergeList(
                base = base.skipAuthPrefixes,
                replace = incoming.skipAuthPrefixes,
                start = null,
                end = null,
            ),
            skipAuthPrefixesStart = MergeHelper.mergeList(
                base = base.skipAuthPrefixesStart,
                replace = incoming.skipAuthPrefixesStart,
                start = null,
                end = null,
            ),
            skipAuthPrefixesEnd = MergeHelper.mergeList(
                base = base.skipAuthPrefixesEnd,
                replace = incoming.skipAuthPrefixesEnd,
                start = null,
                end = null,
            ),
            lanAllowedIps = MergeHelper.mergeList(
                base = base.lanAllowedIps,
                replace = incoming.lanAllowedIps,
                start = null,
                end = null,
            ),
            lanAllowedIpsStart = MergeHelper.mergeList(
                base = base.lanAllowedIpsStart,
                replace = incoming.lanAllowedIpsStart,
                start = null,
                end = null,
            ),
            lanAllowedIpsEnd = MergeHelper.mergeList(
                base = base.lanAllowedIpsEnd,
                replace = incoming.lanAllowedIpsEnd,
                start = null,
                end = null,
            ),
            lanDisallowedIps = MergeHelper.mergeList(
                base = base.lanDisallowedIps,
                replace = incoming.lanDisallowedIps,
                start = null,
                end = null,
            ),
            lanDisallowedIpsStart = MergeHelper.mergeList(
                base = base.lanDisallowedIpsStart,
                replace = incoming.lanDisallowedIpsStart,
                start = null,
                end = null,
            ),
            lanDisallowedIpsEnd = MergeHelper.mergeList(
                base = base.lanDisallowedIpsEnd,
                replace = incoming.lanDisallowedIpsEnd,
                start = null,
                end = null,
            ),
            allowLan = incoming.allowLan ?: base.allowLan,
            bindAddress = incoming.bindAddress ?: base.bindAddress,
            mode = incoming.mode ?: base.mode,
            logLevel = incoming.logLevel ?: base.logLevel,
            ipv6 = incoming.ipv6 ?: base.ipv6,
            externalController = incoming.externalController ?: base.externalController,
            externalControllerTLS = incoming.externalControllerTLS ?: base.externalControllerTLS,
            externalDohServer = incoming.externalDohServer ?: base.externalDohServer,
            externalControllerCors = mergeCors(base.externalControllerCors, incoming),
            externalControllerCorsForce = incoming.externalControllerCorsForce ?: base.externalControllerCorsForce,
            secret = incoming.secret ?: base.secret,
            hosts = MergeHelper.mergeMap(
                base = base.hosts,
                replace = incoming.hosts,
                merge = incoming.hostsMerge,
            ),

            hostsMerge = MergeHelper.mergeMap(
                base = base.hostsMerge,
                replace = incoming.hostsMerge,
                merge = null,
            ),
            unifiedDelay = incoming.unifiedDelay ?: base.unifiedDelay,
            geodataMode = incoming.geodataMode ?: base.geodataMode,
            tcpConcurrent = incoming.tcpConcurrent ?: base.tcpConcurrent,
            findProcessMode = incoming.findProcessMode ?: base.findProcessMode,
            keepAliveInterval = incoming.keepAliveInterval ?: base.keepAliveInterval,
            keepAliveIdle = incoming.keepAliveIdle ?: base.keepAliveIdle,
            interfaceName = incoming.interfaceName ?: base.interfaceName,
            routingMark = incoming.routingMark ?: base.routingMark,
            geositeMatcher = incoming.geositeMatcher ?: base.geositeMatcher,
            globalClientFingerprint = incoming.globalClientFingerprint ?: base.globalClientFingerprint,
            geoAutoUpdate = incoming.geoAutoUpdate ?: base.geoAutoUpdate,
            geoUpdateInterval = incoming.geoUpdateInterval ?: base.geoUpdateInterval,
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

            proxyGroups = MergeHelper.mergeProxyGroupList(
                base = base.proxyGroups,
                start = null,
                replace = incoming.proxyGroups,
                end = null,
            ),
            proxyGroupsStart = MergeHelper.mergeProxyGroupList(
                base = base.proxyGroupsStart,
                start = null,
                replace = incoming.proxyGroupsStart,
                end = null,
            ),
            proxyGroupsEnd = MergeHelper.mergeProxyGroupList(
                base = base.proxyGroupsEnd,
                start = null,
                replace = incoming.proxyGroupsEnd,
                end = null,
            ),

            rules = MergeHelper.mergeList(
                base = base.rules,
                replace = incoming.rules,
                start = null,
                end = null,
            ),
            rulesStart = MergeHelper.mergeList(
                base = base.rulesStart,
                replace = incoming.rulesStart,
                start = null,
                end = null,
            ),
            rulesEnd = MergeHelper.mergeList(
                base = base.rulesEnd,
                replace = incoming.rulesEnd,
                start = null,
                end = null,
            ),
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

            proxies = MergeHelper.mergeProxyList(
                base = base.proxies,
                start = null,
                replace = incoming.proxies,
                end = null,
            ),
            proxiesStart = MergeHelper.mergeProxyList(
                base = base.proxiesStart,
                start = null,
                replace = incoming.proxiesStart,
                end = null,
            ),
            proxiesEnd = MergeHelper.mergeProxyList(
                base = base.proxiesEnd,
                start = null,
                replace = incoming.proxiesEnd,
                end = null,
            ),
            proxyProviders = MergeHelper.mergeProviderMap(
                base = base.proxyProviders,
                replace = incoming.proxyProviders,
                merge = incoming.proxyProvidersMerge,
            ),
            proxyProvidersMerge = MergeHelper.mergeProviderMap(
                base = base.proxyProvidersMerge,
                replace = incoming.proxyProvidersMerge,
                merge = null,
            ),
            dns = mergeDns(base.dns, incoming),
            dnsForce = incoming.dnsForce ?: base.dnsForce,
            app = mergeApp(base.app, incoming.app),
            profile = mergeProfile(base.profile, incoming.profile),
            tun = mergeTun(base.tun, incoming.tun),
            sniffer = mergeSniffer(base.sniffer, incoming),
            snifferForce = incoming.snifferForce ?: base.snifferForce,
            geoxurl = mergeGeoX(base.geoxurl, incoming),
            geoxurlForce = incoming.geoxurlForce ?: base.geoxurlForce,
        )
    }

    private fun mergeDns(
        base: ConfigurationOverride.Dns,
        incoming: ConfigurationOverride,
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

            nameServer = MergeHelper.mergeList(
                base = base.nameServer,
                replace = incomingDns.nameServer,
                start = null,
                end = null,
            ),
            nameServerStart = MergeHelper.mergeList(
                base = base.nameServerStart,
                replace = incomingDns.nameServerStart,
                start = null,
                end = null,
            ),
            nameServerEnd = MergeHelper.mergeList(
                base = base.nameServerEnd,
                replace = incomingDns.nameServerEnd,
                start = null,
                end = null,
            ),

            fallback = MergeHelper.mergeList(
                base = base.fallback,
                replace = incomingDns.fallback,
                start = null,
                end = null,
            ),
            fallbackStart = MergeHelper.mergeList(
                base = base.fallbackStart,
                replace = incomingDns.fallbackStart,
                start = null,
                end = null,
            ),
            fallbackEnd = MergeHelper.mergeList(
                base = base.fallbackEnd,
                replace = incomingDns.fallbackEnd,
                start = null,
                end = null,
            ),

            defaultServer = MergeHelper.mergeList(
                base = base.defaultServer,
                replace = incomingDns.defaultServer,
                start = null,
                end = null,
            ),
            defaultServerStart = MergeHelper.mergeList(
                base = base.defaultServerStart,
                replace = incomingDns.defaultServerStart,
                start = null,
                end = null,
            ),
            defaultServerEnd = MergeHelper.mergeList(
                base = base.defaultServerEnd,
                replace = incomingDns.defaultServerEnd,
                start = null,
                end = null,
            ),

            fakeIpFilter = MergeHelper.mergeList(
                base = base.fakeIpFilter,
                replace = incomingDns.fakeIpFilter,
                start = null,
                end = null,
            ),
            fakeIpFilterStart = MergeHelper.mergeList(
                base = base.fakeIpFilterStart,
                replace = incomingDns.fakeIpFilterStart,
                start = null,
                end = null,
            ),
            fakeIpFilterEnd = MergeHelper.mergeList(
                base = base.fakeIpFilterEnd,
                replace = incomingDns.fakeIpFilterEnd,
                start = null,
                end = null,
            ),

            proxyServerNameserver = MergeHelper.mergeList(
                base = base.proxyServerNameserver,
                replace = incomingDns.proxyServerNameserver,
                start = null,
                end = null,
            ),
            proxyServerNameserverStart = MergeHelper.mergeList(
                base = base.proxyServerNameserverStart,
                replace = incomingDns.proxyServerNameserverStart,
                start = null,
                end = null,
            ),
            proxyServerNameserverEnd = MergeHelper.mergeList(
                base = base.proxyServerNameserverEnd,
                replace = incomingDns.proxyServerNameserverEnd,
                start = null,
                end = null,
            ),

            directNameserver = MergeHelper.mergeList(
                base = base.directNameserver,
                replace = incomingDns.directNameserver,
                start = null,
                end = null,
            ),
            directNameserverStart = MergeHelper.mergeList(
                base = base.directNameserverStart,
                replace = incomingDns.directNameserverStart,
                start = null,
                end = null,
            ),
            directNameserverEnd = MergeHelper.mergeList(
                base = base.directNameserverEnd,
                replace = incomingDns.directNameserverEnd,
                start = null,
                end = null,
            ),

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

            ipcidr = MergeHelper.mergeList(
                base = base.ipcidr,
                replace = incomingFilter.ipcidr,
                start = null,
                end = null,
            ),
            ipcidrStart = MergeHelper.mergeList(
                base = base.ipcidrStart,
                replace = incomingFilter.ipcidrStart,
                start = null,
                end = null,
            ),
            ipcidrEnd = MergeHelper.mergeList(
                base = base.ipcidrEnd,
                replace = incomingFilter.ipcidrEnd,
                start = null,
                end = null,
            ),

            geosite = MergeHelper.mergeList(
                base = base.geosite,
                replace = incomingFilter.geosite,
                start = null,
                end = null,
            ),
            geositeStart = MergeHelper.mergeList(
                base = base.geositeStart,
                replace = incomingFilter.geositeStart,
                start = null,
                end = null,
            ),
            geositeEnd = MergeHelper.mergeList(
                base = base.geositeEnd,
                replace = incomingFilter.geositeEnd,
                start = null,
                end = null,
            ),

            domain = MergeHelper.mergeList(
                base = base.domain,
                replace = incomingFilter.domain,
                start = null,
                end = null,
            ),
            domainStart = MergeHelper.mergeList(
                base = base.domainStart,
                replace = incomingFilter.domainStart,
                start = null,
                end = null,
            ),
            domainEnd = MergeHelper.mergeList(
                base = base.domainEnd,
                replace = incomingFilter.domainEnd,
                start = null,
                end = null,
            ),
        )
    }

    private fun mergeApp(
        base: ConfigurationOverride.App,
        incoming: ConfigurationOverride.App,
    ): ConfigurationOverride.App {
        return base.copy(
            appendSystemDns = incoming.appendSystemDns ?: base.appendSystemDns,
        )
    }

    private fun mergeProfile(
        base: ConfigurationOverride.Profile,
        incoming: ConfigurationOverride.Profile,
    ): ConfigurationOverride.Profile {
        return base.copy(
            storeSelected = incoming.storeSelected ?: base.storeSelected,
            storeFakeIp = incoming.storeFakeIp ?: base.storeFakeIp,
        )
    }

    private fun mergeCors(
        base: ConfigurationOverride.ExternalControllerCors,
        incoming: ConfigurationOverride,
    ): ConfigurationOverride.ExternalControllerCors {
        incoming.externalControllerCorsForce?.let { return it }

        val incomingCors = incoming.externalControllerCors
        return base.copy(

            allowOrigins = MergeHelper.mergeList(
                base = base.allowOrigins,
                replace = incomingCors.allowOrigins,
                start = null,
                end = null,
            ),
            allowOriginsStart = MergeHelper.mergeList(
                base = base.allowOriginsStart,
                replace = incomingCors.allowOriginsStart,
                start = null,
                end = null,
            ),
            allowOriginsEnd = MergeHelper.mergeList(
                base = base.allowOriginsEnd,
                replace = incomingCors.allowOriginsEnd,
                start = null,
                end = null,
            ),
            allowPrivateNetwork = incomingCors.allowPrivateNetwork ?: base.allowPrivateNetwork,
        )
    }

    private fun mergeGeoX(
        base: ConfigurationOverride.GeoXUrl,
        incoming: ConfigurationOverride,
    ): ConfigurationOverride.GeoXUrl {
        incoming.geoxurlForce?.let { return it }

        val incomingGeoX = incoming.geoxurl
        return base.copy(
            geoip = incomingGeoX.geoip ?: base.geoip,
            mmdb = incomingGeoX.mmdb ?: base.mmdb,
            geosite = incomingGeoX.geosite ?: base.geosite,
        )
    }

    private fun mergeTun(
        base: ConfigurationOverride.Tun,
        incoming: ConfigurationOverride.Tun,
    ): ConfigurationOverride.Tun {
        return base.copy(
            enable = incoming.enable ?: base.enable,
            stack = incoming.stack ?: base.stack,

            dnsHijack = MergeHelper.mergeList(
                base = base.dnsHijack,
                replace = incoming.dnsHijack,
                start = null,
                end = null,
            ),
            dnsHijackStart = MergeHelper.mergeList(
                base = base.dnsHijackStart,
                replace = incoming.dnsHijackStart,
                start = null,
                end = null,
            ),
            dnsHijackEnd = MergeHelper.mergeList(
                base = base.dnsHijackEnd,
                replace = incoming.dnsHijackEnd,
                start = null,
                end = null,
            ),
            autoRoute = incoming.autoRoute ?: base.autoRoute,
            autoDetectInterface = incoming.autoDetectInterface ?: base.autoDetectInterface,
            autoRedirect = incoming.autoRedirect ?: base.autoRedirect,
            mtu = incoming.mtu ?: base.mtu,
            gso = incoming.gso ?: base.gso,
            gsoMaxSize = incoming.gsoMaxSize ?: base.gsoMaxSize,
            strictRoute = incoming.strictRoute ?: base.strictRoute,
            disableIcmpForwarding = incoming.disableIcmpForwarding ?: base.disableIcmpForwarding,

            routeAddress = MergeHelper.mergeList(
                base = base.routeAddress,
                replace = incoming.routeAddress,
                start = null,
                end = null,
            ),
            routeAddressStart = MergeHelper.mergeList(
                base = base.routeAddressStart,
                replace = incoming.routeAddressStart,
                start = null,
                end = null,
            ),
            routeAddressEnd = MergeHelper.mergeList(
                base = base.routeAddressEnd,
                replace = incoming.routeAddressEnd,
                start = null,
                end = null,
            ),

            routeExcludeAddress = MergeHelper.mergeList(
                base = base.routeExcludeAddress,
                replace = incoming.routeExcludeAddress,
                start = null,
                end = null,
            ),
            routeExcludeAddressStart = MergeHelper.mergeList(
                base = base.routeExcludeAddressStart,
                replace = incoming.routeExcludeAddressStart,
                start = null,
                end = null,
            ),
            routeExcludeAddressEnd = MergeHelper.mergeList(
                base = base.routeExcludeAddressEnd,
                replace = incoming.routeExcludeAddressEnd,
                start = null,
                end = null,
            ),
            endpointIndependentNat = incoming.endpointIndependentNat ?: base.endpointIndependentNat,

            includePackage = MergeHelper.mergeList(
                base = base.includePackage,
                replace = incoming.includePackage,
                start = null,
                end = null,
            ),
            includePackageStart = MergeHelper.mergeList(
                base = base.includePackageStart,
                replace = incoming.includePackageStart,
                start = null,
                end = null,
            ),
            includePackageEnd = MergeHelper.mergeList(
                base = base.includePackageEnd,
                replace = incoming.includePackageEnd,
                start = null,
                end = null,
            ),

            excludePackage = MergeHelper.mergeList(
                base = base.excludePackage,
                replace = incoming.excludePackage,
                start = null,
                end = null,
            ),
            excludePackageStart = MergeHelper.mergeList(
                base = base.excludePackageStart,
                replace = incoming.excludePackageStart,
                start = null,
                end = null,
            ),
            excludePackageEnd = MergeHelper.mergeList(
                base = base.excludePackageEnd,
                replace = incoming.excludePackageEnd,
                start = null,
                end = null,
            ),
        )
    }

    private fun mergeSniffer(
        base: ConfigurationOverride.Sniffer,
        incoming: ConfigurationOverride,
    ): ConfigurationOverride.Sniffer {
        incoming.snifferForce?.let { return it }

        val incomingSniffer = incoming.sniffer
        return base.copy(
            enable = incomingSniffer.enable ?: base.enable,
            sniff = mergeSniff(base.sniff, incomingSniffer),
            forceDnsMapping = incomingSniffer.forceDnsMapping ?: base.forceDnsMapping,
            parsePureIp = incomingSniffer.parsePureIp ?: base.parsePureIp,
            overrideDestination = incomingSniffer.overrideDestination ?: base.overrideDestination,

            forceDomain = MergeHelper.mergeList(
                base = base.forceDomain,
                replace = incomingSniffer.forceDomain,
                start = null,
                end = null,
            ),
            forceDomainStart = MergeHelper.mergeList(
                base = base.forceDomainStart,
                replace = incomingSniffer.forceDomainStart,
                start = null,
                end = null,
            ),
            forceDomainEnd = MergeHelper.mergeList(
                base = base.forceDomainEnd,
                replace = incomingSniffer.forceDomainEnd,
                start = null,
                end = null,
            ),

            skipDomain = MergeHelper.mergeList(
                base = base.skipDomain,
                replace = incomingSniffer.skipDomain,
                start = null,
                end = null,
            ),
            skipDomainStart = MergeHelper.mergeList(
                base = base.skipDomainStart,
                replace = incomingSniffer.skipDomainStart,
                start = null,
                end = null,
            ),
            skipDomainEnd = MergeHelper.mergeList(
                base = base.skipDomainEnd,
                replace = incomingSniffer.skipDomainEnd,
                start = null,
                end = null,
            ),

            skipSrcAddress = MergeHelper.mergeList(
                base = base.skipSrcAddress,
                replace = incomingSniffer.skipSrcAddress,
                start = null,
                end = null,
            ),
            skipSrcAddressStart = MergeHelper.mergeList(
                base = base.skipSrcAddressStart,
                replace = incomingSniffer.skipSrcAddressStart,
                start = null,
                end = null,
            ),
            skipSrcAddressEnd = MergeHelper.mergeList(
                base = base.skipSrcAddressEnd,
                replace = incomingSniffer.skipSrcAddressEnd,
                start = null,
                end = null,
            ),

            skipDstAddress = MergeHelper.mergeList(
                base = base.skipDstAddress,
                replace = incomingSniffer.skipDstAddress,
                start = null,
                end = null,
            ),
            skipDstAddressStart = MergeHelper.mergeList(
                base = base.skipDstAddressStart,
                replace = incomingSniffer.skipDstAddressStart,
                start = null,
                end = null,
            ),
            skipDstAddressEnd = MergeHelper.mergeList(
                base = base.skipDstAddressEnd,
                replace = incomingSniffer.skipDstAddressEnd,
                start = null,
                end = null,
            ),
        )
    }

    private fun mergeSniff(
        base: ConfigurationOverride.Sniff,
        incoming: ConfigurationOverride.Sniffer,
    ): ConfigurationOverride.Sniff {
        incoming.sniffForce?.let { return it }

        val incomingSniff = incoming.sniff
        return base.copy(
            http = mergeProtocol(base.http, incomingSniff.http),
            tls = mergeProtocol(base.tls, incomingSniff.tls),
            quic = mergeProtocol(base.quic, incomingSniff.quic),
        )
    }

    private fun mergeProtocol(
        base: ConfigurationOverride.ProtocolConfig,
        incoming: ConfigurationOverride.ProtocolConfig,
    ): ConfigurationOverride.ProtocolConfig {
        return base.copy(

            ports = MergeHelper.mergeList(
                base = base.ports,
                replace = incoming.ports,
                start = null,
                end = null,
            ),
            portsStart = MergeHelper.mergeList(
                base = base.portsStart,
                replace = incoming.portsStart,
                start = null,
                end = null,
            ),
            portsEnd = MergeHelper.mergeList(
                base = base.portsEnd,
                replace = incoming.portsEnd,
                start = null,
                end = null,
            ),
            overrideDestination = incoming.overrideDestination ?: base.overrideDestination,
        )
    }
}
