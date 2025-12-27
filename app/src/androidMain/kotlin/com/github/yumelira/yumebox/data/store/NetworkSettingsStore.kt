package com.github.yumelira.yumebox.data.store

import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.tencent.mmkv.MMKV

class NetworkSettingsStorage(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {


    val proxyMode by enumFlow(ProxyMode.Tun)


    val bypassPrivateNetwork by boolFlow(true)
    val dnsHijack by boolFlow(true)
    val allowBypass by boolFlow(true)
    val enableIPv6 by boolFlow(false)
    val systemProxy by boolFlow(true)


    val tunStack by enumFlow(TunStack.System)
    val accessControlMode by enumFlow(AccessControlMode.ALLOW_ALL)
    val accessControlPackages by stringSetFlow(emptySet())
}
