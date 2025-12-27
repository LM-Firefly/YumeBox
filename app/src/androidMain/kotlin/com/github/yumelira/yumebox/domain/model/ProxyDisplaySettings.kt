package com.github.yumelira.yumebox.domain.model

import dev.oom_wg.purejoy.mlang.MLang

enum class ProxyDisplayMode {
    SINGLE_DETAILED,
    SINGLE_SIMPLE,
    DOUBLE_DETAILED,
    DOUBLE_SIMPLE;

    val displayName: String
        get() = when (this) {
            SINGLE_DETAILED -> MLang.Proxy.DisplayMode.SingleDetailed
            SINGLE_SIMPLE -> MLang.Proxy.DisplayMode.SingleSimple
            DOUBLE_DETAILED -> MLang.Proxy.DisplayMode.DoubleDetailed
            DOUBLE_SIMPLE -> MLang.Proxy.DisplayMode.DoubleSimple
        }

    val isSingleColumn: Boolean
        get() = this == SINGLE_DETAILED || this == SINGLE_SIMPLE

    val showDetail: Boolean
        get() = this == SINGLE_DETAILED || this == DOUBLE_DETAILED
}

enum class ProxySortMode {
    DEFAULT,
    BY_NAME,
    BY_LATENCY;

    val displayName: String
        get() = when (this) {
            DEFAULT -> MLang.Proxy.SortMode.Default
            BY_NAME -> MLang.Proxy.SortMode.ByName
            BY_LATENCY -> MLang.Proxy.SortMode.ByLatency
        }
}
