package com.github.yumelira.yumebox.data.store

import com.tencent.mmkv.MMKV

class ExtensionStore(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {
    val isInstallSubStoreDist by bool(false)
    val isInsatllExtenApk by bool(false)
    val isInstallZashboard by bool(false)
    val isInstallMetaCibeXD by bool(false)
    val isLoadNativeLibs by bool(false)
}