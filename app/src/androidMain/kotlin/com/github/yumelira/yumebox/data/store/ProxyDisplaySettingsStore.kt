package com.github.yumelira.yumebox.data.store

import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import com.tencent.mmkv.MMKV

class ProxyDisplaySettingsStore(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    val sortMode by enumFlow(ProxySortMode.DEFAULT)
    val displayMode by enumFlow(ProxyDisplayMode.DOUBLE_SIMPLE)
    val proxyMode by enumFlow(TunnelState.Mode.Rule)
}
