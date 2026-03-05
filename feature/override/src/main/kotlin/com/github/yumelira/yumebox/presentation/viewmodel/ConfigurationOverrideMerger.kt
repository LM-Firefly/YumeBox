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

package com.github.yumelira.yumebox.presentation.viewmodel

import com.github.yumelira.yumebox.core.model.ConfigurationOverride

internal object ConfigurationOverrideMerger {

    fun merge(base: ConfigurationOverride, incoming: ConfigurationOverride): ConfigurationOverride {
        return base.copy(
            httpPort = incoming.httpPort ?: base.httpPort,
            socksPort = incoming.socksPort ?: base.socksPort,
            redirectPort = incoming.redirectPort ?: base.redirectPort,
            tproxyPort = incoming.tproxyPort ?: base.tproxyPort,
            mixedPort = incoming.mixedPort ?: base.mixedPort,
            authentication = incoming.authentication ?: base.authentication,
            allowLan = incoming.allowLan ?: base.allowLan,
            bindAddress = incoming.bindAddress ?: base.bindAddress,
            mode = incoming.mode ?: base.mode,
            logLevel = incoming.logLevel ?: base.logLevel,
            ipv6 = incoming.ipv6 ?: base.ipv6,
            externalController = incoming.externalController ?: base.externalController,
            externalControllerTLS = incoming.externalControllerTLS ?: base.externalControllerTLS,
            externalControllerCors = mergeCors(base.externalControllerCors, incoming.externalControllerCors),
            secret = incoming.secret ?: base.secret,
            hosts = incoming.hosts ?: base.hosts,
            unifiedDelay = incoming.unifiedDelay ?: base.unifiedDelay,
            geodataMode = incoming.geodataMode ?: base.geodataMode,
            tcpConcurrent = incoming.tcpConcurrent ?: base.tcpConcurrent,
            findProcessMode = incoming.findProcessMode ?: base.findProcessMode,
            keepAliveInterval = incoming.keepAliveInterval ?: base.keepAliveInterval,
            keepAliveIdle = incoming.keepAliveIdle ?: base.keepAliveIdle,
            dns = mergeDns(base.dns, incoming.dns),
            app = mergeApp(base.app, incoming.app),
            sniffer = mergeSniffer(base.sniffer, incoming.sniffer),
            geoxurl = mergeGeoX(base.geoxurl, incoming.geoxurl),
            ruleProviders = incoming.ruleProviders ?: base.ruleProviders,
            proxyGroups = incoming.proxyGroups ?: base.proxyGroups,
            prependRules = incoming.prependRules ?: base.prependRules,
            rules = incoming.rules ?: base.rules,
            subRules = incoming.subRules ?: base.subRules,
        )
    }

    private fun mergeDns(
        base: ConfigurationOverride.Dns,
        incoming: ConfigurationOverride.Dns,
    ): ConfigurationOverride.Dns {
        return base.copy(
            enable = incoming.enable ?: base.enable,
            cacheAlgorithm = incoming.cacheAlgorithm ?: base.cacheAlgorithm,
            preferH3 = incoming.preferH3 ?: base.preferH3,
            listen = incoming.listen ?: base.listen,
            ipv6 = incoming.ipv6 ?: base.ipv6,
            useHosts = incoming.useHosts ?: base.useHosts,
            useSystemHosts = incoming.useSystemHosts ?: base.useSystemHosts,
            respectRules = incoming.respectRules ?: base.respectRules,
            enhancedMode = incoming.enhancedMode ?: base.enhancedMode,
            nameServer = incoming.nameServer ?: base.nameServer,
            fallback = incoming.fallback ?: base.fallback,
            defaultServer = incoming.defaultServer ?: base.defaultServer,
            fakeIpFilter = incoming.fakeIpFilter ?: base.fakeIpFilter,
            fakeIpRange = incoming.fakeIpRange ?: base.fakeIpRange,
            fakeIPFilterMode = incoming.fakeIPFilterMode ?: base.fakeIPFilterMode,
            fallbackFilter = mergeFallbackFilter(base.fallbackFilter, incoming.fallbackFilter),
            proxyServerNameserver = incoming.proxyServerNameserver ?: base.proxyServerNameserver,
            nameserverPolicy = incoming.nameserverPolicy ?: base.nameserverPolicy,
        )
    }

    private fun mergeFallbackFilter(
        base: ConfigurationOverride.DnsFallbackFilter,
        incoming: ConfigurationOverride.DnsFallbackFilter,
    ): ConfigurationOverride.DnsFallbackFilter {
        return base.copy(
            geoIp = incoming.geoIp ?: base.geoIp,
            geoIpCode = incoming.geoIpCode ?: base.geoIpCode,
            ipcidr = incoming.ipcidr ?: base.ipcidr,
            geosite = incoming.geosite ?: base.geosite,
            domain = incoming.domain ?: base.domain,
        )
    }

    private fun mergeApp(
        base: ConfigurationOverride.App,
        incoming: ConfigurationOverride.App,
    ): ConfigurationOverride.App {
        return base.copy(appendSystemDns = incoming.appendSystemDns ?: base.appendSystemDns)
    }

    private fun mergeCors(
        base: ConfigurationOverride.ExternalControllerCors,
        incoming: ConfigurationOverride.ExternalControllerCors,
    ): ConfigurationOverride.ExternalControllerCors {
        return base.copy(
            allowOrigins = incoming.allowOrigins ?: base.allowOrigins,
            allowPrivateNetwork = incoming.allowPrivateNetwork ?: base.allowPrivateNetwork,
        )
    }

    private fun mergeGeoX(
        base: ConfigurationOverride.GeoXUrl,
        incoming: ConfigurationOverride.GeoXUrl,
    ): ConfigurationOverride.GeoXUrl {
        return base.copy(
            geoip = incoming.geoip ?: base.geoip,
            mmdb = incoming.mmdb ?: base.mmdb,
            geosite = incoming.geosite ?: base.geosite,
        )
    }

    private fun mergeSniffer(
        base: ConfigurationOverride.Sniffer,
        incoming: ConfigurationOverride.Sniffer,
    ): ConfigurationOverride.Sniffer {
        return base.copy(
            enable = incoming.enable ?: base.enable,
            sniff = mergeSniff(base.sniff, incoming.sniff),
            forceDnsMapping = incoming.forceDnsMapping ?: base.forceDnsMapping,
            parsePureIp = incoming.parsePureIp ?: base.parsePureIp,
            overrideDestination = incoming.overrideDestination ?: base.overrideDestination,
            forceDomain = incoming.forceDomain ?: base.forceDomain,
            skipDomain = incoming.skipDomain ?: base.skipDomain,
            skipSrcAddress = incoming.skipSrcAddress ?: base.skipSrcAddress,
            skipDstAddress = incoming.skipDstAddress ?: base.skipDstAddress,
        )
    }

    private fun mergeSniff(
        base: ConfigurationOverride.Sniff,
        incoming: ConfigurationOverride.Sniff,
    ): ConfigurationOverride.Sniff {
        return base.copy(
            http = mergeProtocol(base.http, incoming.http),
            tls = mergeProtocol(base.tls, incoming.tls),
            quic = mergeProtocol(base.quic, incoming.quic),
        )
    }

    private fun mergeProtocol(
        base: ConfigurationOverride.ProtocolConfig,
        incoming: ConfigurationOverride.ProtocolConfig,
    ): ConfigurationOverride.ProtocolConfig {
        return base.copy(
            ports = incoming.ports ?: base.ports,
            overrideDestination = incoming.overrideDestination ?: base.overrideDestination,
        )
    }
}
