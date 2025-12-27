package com.github.yumelira.yumebox.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import com.github.yumelira.yumebox.core.Clash
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class AppListCacheModule(private val service: Service, private val scope: CoroutineScope) {
    private var job: Job? = null
    private fun PackageInfo.uniqueUidName(): String =
        if (sharedUserId?.isNotBlank() == true) sharedUserId!! else packageName
    private fun reload() {
        try {
            val packages = service.packageManager.getInstalledPackages(0)
                .filter { it.applicationInfo != null }
                .groupBy { it.uniqueUidName() }
                .map { (_, v) ->
                    val info = v[0]
                    if (v.size == 1) {
                        info.applicationInfo!!.uid to info.packageName
                    } else {
                        info.applicationInfo!!.uid to info.uniqueUidName()
                    }
                }
            Clash.notifyInstalledAppsChanged(packages)
            Timber.d("Installed ${packages.size} packages cached")
        } catch (e: Exception) {
            Timber.e(e, "Failed to reload installed packages")
        }
    }
    fun start() {
        if (job != null) return
        job = scope.launch(Dispatchers.IO) {
            val packageChanged = Channel<Unit>(Channel.CONFLATED)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    packageChanged.trySend(Unit)
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
            service.registerReceiver(receiver, filter)
            try {
                while (true) {
                    reload()
                    packageChanged.receive()
                    delay(TimeUnit.SECONDS.toMillis(10))
                }
            } finally {
                try {
                    service.unregisterReceiver(receiver)
                } catch (ignored: Exception) {
                }
            }
        }
    }
    fun stop() {
        job?.cancel()
        job = null
    }
}
