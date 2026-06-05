package com.github.yumelira.yumebox.runtime.service

import android.net.VpnService
import com.github.yumelira.yumebox.core.appContextOrSelf
import com.github.yumelira.yumebox.core.Global
import com.github.yumelira.yumebox.runtime.service.runtime.util.cancelAndJoinBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

abstract class BaseVpnService : VpnService(), CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    override fun onCreate() {
        super.onCreate()

        Global.init(appContextOrSelf)
    }

    override fun onDestroy() {
        super.onDestroy()

        cancelAndJoinBlocking()
    }
}
