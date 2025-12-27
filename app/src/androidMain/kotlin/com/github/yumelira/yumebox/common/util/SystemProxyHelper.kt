package com.github.yumelira.yumebox.common.util

import android.content.Context
import timber.log.Timber

object SystemProxyHelper {

    private const val TAG = "SystemProxyHelper"

    fun clearSystemProxy(context: Context) {
        runCatching {
            System.clearProperty("http.proxyHost")
            System.clearProperty("http.proxyPort")
            System.clearProperty("https.proxyHost")
            System.clearProperty("https.proxyPort")
            System.clearProperty("socksProxyHost")
            System.clearProperty("socksProxyPort")
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "清理系统代理失败: ${e.message}")
        }
    }
}