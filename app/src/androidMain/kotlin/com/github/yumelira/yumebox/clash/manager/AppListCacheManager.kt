package com.github.yumelira.yumebox.clash.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import com.github.yumelira.yumebox.core.Clash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AppListCacheManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val packageChangeChannel = Channel<Unit>(Channel.CONFLATED)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            packageChangeChannel.trySend(Unit)
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)

        scope.launch(Dispatchers.IO) {
            while (true) {
                reload()
                packageChangeChannel.receive()
                delay(TimeUnit.SECONDS.toMillis(10))
            }
        }
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // Ignore if not registered
        }
    }

    private fun PackageInfo.uniqueUidName(): String =
        if (sharedUserId?.isNotBlank() == true) sharedUserId!! else packageName

    private fun reload() {
        try {
            val packages = context.packageManager.getInstalledPackages(0)
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
}
