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
