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
import com.github.yumelira.yumebox.core.model.ConfigurationOverrideSupportSection

internal object ConfigurationOverrideSupportMerger {

    fun merge(
        base: ConfigurationOverrideSupportSection,
        incoming: ConfigurationOverrideSupportSection,
    ): ConfigurationOverrideSupportSection {
        return ConfigurationOverrideSupportSection(
            app = mergeApp(base.app, incoming.app),
            profile = mergeProfile(base.profile, incoming.profile),
            geoxurl = mergeGeoX(base.geoxurl, incoming),
            geoxurlForce = incoming.geoxurlForce ?: base.geoxurlForce,
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

    private fun mergeGeoX(
        base: ConfigurationOverride.GeoXUrl,
        incoming: ConfigurationOverrideSupportSection,
    ): ConfigurationOverride.GeoXUrl {
        incoming.geoxurlForce?.let { return it }
        val incomingGeoX = incoming.geoxurl
        return base.copy(
            geoip = incomingGeoX.geoip ?: base.geoip,
            mmdb = incomingGeoX.mmdb ?: base.mmdb,
            geosite = incomingGeoX.geosite ?: base.geosite,
        )
    }
}
