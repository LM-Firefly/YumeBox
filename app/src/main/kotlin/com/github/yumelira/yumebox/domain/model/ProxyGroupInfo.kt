package com.github.yumelira.yumebox.domain.model

import com.github.yumelira.yumebox.core.model.Proxy

data class ProxyGroupInfo(
    val name: String,
    val type: Proxy.Type,
    val proxies: List<Proxy>,
    val now: String,
    val fixed: String = "",
    val chainPath: List<String> = emptyList()
)
