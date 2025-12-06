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

internal object ConfigurationOverrideCoreMerger {

    fun merge(base: ConfigurationOverride, incoming: ConfigurationOverride): ConfigurationOverride {
        return base.copy(
            httpPort = incoming.httpPort ?: base.httpPort,
            socksPort = incoming.socksPort ?: base.socksPort,
            redirectPort = incoming.redirectPort ?: base.redirectPort,
            tproxyPort = incoming.tproxyPort ?: base.tproxyPort,
            mixedPort = incoming.mixedPort ?: base.mixedPort,
            authentication = MergeHelper.mergeList(base.authentication, incoming.authentication),
            authenticationStart = MergeHelper.mergeList(base.authenticationStart, incoming.authenticationStart),
            authenticationEnd = MergeHelper.mergeList(base.authenticationEnd, incoming.authenticationEnd),
            skipAuthPrefixes = MergeHelper.mergeList(base.skipAuthPrefixes, incoming.skipAuthPrefixes),
            skipAuthPrefixesStart = MergeHelper.mergeList(base.skipAuthPrefixesStart, incoming.skipAuthPrefixesStart),
            skipAuthPrefixesEnd = MergeHelper.mergeList(base.skipAuthPrefixesEnd, incoming.skipAuthPrefixesEnd),
            lanAllowedIps = MergeHelper.mergeList(base.lanAllowedIps, incoming.lanAllowedIps),
            lanAllowedIpsStart = MergeHelper.mergeList(base.lanAllowedIpsStart, incoming.lanAllowedIpsStart),
            lanAllowedIpsEnd = MergeHelper.mergeList(base.lanAllowedIpsEnd, incoming.lanAllowedIpsEnd),
            lanDisallowedIps = MergeHelper.mergeList(base.lanDisallowedIps, incoming.lanDisallowedIps),
            lanDisallowedIpsStart = MergeHelper.mergeList(base.lanDisallowedIpsStart, incoming.lanDisallowedIpsStart),
            lanDisallowedIpsEnd = MergeHelper.mergeList(base.lanDisallowedIpsEnd, incoming.lanDisallowedIpsEnd),
            allowLan = incoming.allowLan ?: base.allowLan,
            bindAddress = incoming.bindAddress ?: base.bindAddress,
            mode = incoming.mode ?: base.mode,
            logLevel = incoming.logLevel ?: base.logLevel,
            ipv6 = incoming.ipv6 ?: base.ipv6,
            externalController = incoming.externalController ?: base.externalController,
            externalControllerTLS = incoming.externalControllerTLS ?: base.externalControllerTLS,
            externalDohServer = incoming.externalDohServer ?: base.externalDohServer,
            externalControllerCors = mergeCors(
                base = base.externalControllerCors,
                incoming = incoming.externalControllerCors,
                force = incoming.externalControllerCorsForce,
            ),
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
        )
    }

    private fun mergeCors(
        base: ConfigurationOverride.ExternalControllerCors,
        incoming: ConfigurationOverride.ExternalControllerCors,
        force: ConfigurationOverride.ExternalControllerCors?,
    ): ConfigurationOverride.ExternalControllerCors {
        force?.let { return it }
        return base.copy(
            allowOrigins = MergeHelper.mergeList(base.allowOrigins, incoming.allowOrigins),
            allowOriginsStart = MergeHelper.mergeList(base.allowOriginsStart, incoming.allowOriginsStart),
            allowOriginsEnd = MergeHelper.mergeList(base.allowOriginsEnd, incoming.allowOriginsEnd),
            allowPrivateNetwork = incoming.allowPrivateNetwork ?: base.allowPrivateNetwork,
        )
    }
}
