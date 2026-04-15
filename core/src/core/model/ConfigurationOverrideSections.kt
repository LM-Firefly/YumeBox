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
