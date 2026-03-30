package com.github.yumelira.yumebox.data.util

import com.github.yumelira.yumebox.core.model.ConfigurationOverrideProxyResourceSection

internal object ConfigurationOverrideProxyResourceMerger {

    fun merge(
        base: ConfigurationOverrideProxyResourceSection,
        incoming: ConfigurationOverrideProxyResourceSection,
    ): ConfigurationOverrideProxyResourceSection {
        return base.copy(
            proxies = MergeHelper.mergeProxyList(base.proxies, incoming.proxies),
            proxiesStart = MergeHelper.mergeProxyList(base.proxiesStart, incoming.proxiesStart),
            proxiesEnd = MergeHelper.mergeProxyList(base.proxiesEnd, incoming.proxiesEnd),
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
        )
    }
}
