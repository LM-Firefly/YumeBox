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
import com.github.yumelira.yumebox.core.model.ConfigurationOverrideSnifferSection

internal object ConfigurationOverrideSnifferMerger {

    fun merge(
        base: ConfigurationOverrideSnifferSection,
        incoming: ConfigurationOverrideSnifferSection,
    ): ConfigurationOverrideSnifferSection {
        return ConfigurationOverrideSnifferSection(
            sniffer = mergeSniffer(base.sniffer, incoming),
            snifferForce = incoming.snifferForce ?: base.snifferForce,
        )
    }

    private fun mergeSniffer(
        base: ConfigurationOverride.Sniffer,
        incoming: ConfigurationOverrideSnifferSection,
    ): ConfigurationOverride.Sniffer {
        incoming.snifferForce?.let { return it }

        val incomingSniffer = incoming.sniffer
        return base.copy(
            enable = incomingSniffer.enable ?: base.enable,
            sniff = mergeSniff(base.sniff, incomingSniffer.sniff, incomingSniffer.sniffForce),
            forceDnsMapping = incomingSniffer.forceDnsMapping ?: base.forceDnsMapping,
            parsePureIp = incomingSniffer.parsePureIp ?: base.parsePureIp,
            overrideDestination = incomingSniffer.overrideDestination ?: base.overrideDestination,
            forceDomain = MergeHelper.mergeList(base.forceDomain, incomingSniffer.forceDomain),
            forceDomainStart = MergeHelper.mergeList(base.forceDomainStart, incomingSniffer.forceDomainStart),
            forceDomainEnd = MergeHelper.mergeList(base.forceDomainEnd, incomingSniffer.forceDomainEnd),
            skipDomain = MergeHelper.mergeList(base.skipDomain, incomingSniffer.skipDomain),
            skipDomainStart = MergeHelper.mergeList(base.skipDomainStart, incomingSniffer.skipDomainStart),
            skipDomainEnd = MergeHelper.mergeList(base.skipDomainEnd, incomingSniffer.skipDomainEnd),
            skipSrcAddress = MergeHelper.mergeList(base.skipSrcAddress, incomingSniffer.skipSrcAddress),
            skipSrcAddressStart = MergeHelper.mergeList(base.skipSrcAddressStart, incomingSniffer.skipSrcAddressStart),
            skipSrcAddressEnd = MergeHelper.mergeList(base.skipSrcAddressEnd, incomingSniffer.skipSrcAddressEnd),
            skipDstAddress = MergeHelper.mergeList(base.skipDstAddress, incomingSniffer.skipDstAddress),
            skipDstAddressStart = MergeHelper.mergeList(base.skipDstAddressStart, incomingSniffer.skipDstAddressStart),
            skipDstAddressEnd = MergeHelper.mergeList(base.skipDstAddressEnd, incomingSniffer.skipDstAddressEnd),
        )
    }

    private fun mergeSniff(
        base: ConfigurationOverride.Sniff,
        incoming: ConfigurationOverride.Sniff,
        force: ConfigurationOverride.Sniff?,
    ): ConfigurationOverride.Sniff {
        force?.let { return it }
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
            ports = MergeHelper.mergeList(base.ports, incoming.ports),
            portsStart = MergeHelper.mergeList(base.portsStart, incoming.portsStart),
            portsEnd = MergeHelper.mergeList(base.portsEnd, incoming.portsEnd),
            overrideDestination = incoming.overrideDestination ?: base.overrideDestination,
        )
    }
}
