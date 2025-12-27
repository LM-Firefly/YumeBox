package com.github.yumelira.yumebox.data.store

import com.github.yumelira.yumebox.data.model.AutoCloseMode
import com.tencent.mmkv.MMKV

class FeatureStore(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    val allowLanAccess by boolFlow(false)
    val backendPort by intFlow(8081)
    val frontendPort by intFlow(8080)
    val selectedPanelType by intFlow(0)
    val showWebControlInProxy by boolFlow(false)
    val autoCloseMode by enumFlow(AutoCloseMode.DISABLED)

    var isFirstOpen by bool(true)

    fun markFirstOpenHandled() {
        isFirstOpen = false
    }

    fun isFirstTimeOpen(): Boolean = isFirstOpen

    fun setLibraryCacheVersion(libraryName: String, version: Int) {
        mmkv.encode("library_version_" + libraryName, version)
    }

    fun getLibraryCacheVersion(libraryName: String): Int {
        return mmkv.decodeInt("library_version_" + libraryName, -1)
    }

    fun setAssetCacheVersion(assetPath: String, version: Int) {
        mmkv.encode("asset_version_" + assetPath, version)
    }

    fun getAssetCacheVersion(assetPath: String): Int {
        return mmkv.decodeInt("asset_version_" + assetPath, -1)
    }

    fun clearAll() {
        mmkv.edit().clear().apply()
    }
}
