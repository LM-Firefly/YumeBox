package com.github.yumelira.yumebox.service.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.R
import com.github.yumelira.yumebox.common.util.formatBytes
import com.github.yumelira.yumebox.common.util.formatSpeed
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.service.data.ImportedDao
import com.github.yumelira.yumebox.service.store.ServiceStore
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ServiceNotificationManager(
    private val service: Service,
    private val config: Config,
) {
    data class Config(
        val notificationId: Int,
        val channelId: String,
        val channelName: String,
    )

    private val serviceStore by lazy { ServiceStore(service) }
    private val settingsStore by lazy { MMKV.mmkvWithID("settings", MMKV.MULTI_PROCESS_MODE) }
    private val notificationManager by lazy { NotificationManagerCompat.from(service) }

    fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                config.channelId,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).setName(config.channelName).build()
        )
    }

    fun createLoadingNotification(): Notification {
        return buildNotification(service.getText(R.string.loading), service.getText(R.string.loading))
    }

    fun startTrafficUpdate(scope: CoroutineScope): Job {
        return scope.launch(Dispatchers.Default) {
            while (isActive) {
                notificationManager.notify(config.notificationId, buildRunningNotification())
                delay(1000L)
            }
        }
    }

    private fun buildRunningNotification(): Notification {
        val profileName = resolveProfileName()
        if (!shouldShowTrafficNotification()) {
            return buildNotification(profileName, service.getText(R.string.running))
        }

        val now = runCatching { Clash.queryTrafficNow() }.getOrDefault(0L)
        val total = runCatching { Clash.queryTrafficTotal() }.getOrDefault(0L)

        val upNow = decodeTrafficHalf(now ushr 32)
        val downNow = decodeTrafficHalf(now and 0xFFFFFFFFL)
        val upTotal = decodeTrafficHalf(total ushr 32)
        val downTotal = decodeTrafficHalf(total and 0xFFFFFFFFL)

        val speedStr = "↓ ${formatSpeed(downNow)} ↑ ${formatSpeed(upNow)}"
        val totalStr = formatBytes(upTotal + downTotal)
        return buildNotification(profileName, "$speedStr | $totalStr")
    }

    private fun buildNotification(title: CharSequence, content: CharSequence): Notification {
        val contentIntent = PendingIntent.getActivity(
            service,
            0,
            Intent(service, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(service, config.channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_logo_service)
            .setColor(service.getColor(R.color.color_clash))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun resolveProfileName(): String {
        val active = serviceStore.activeProfile ?: return service.getString(R.string.running)
        return ImportedDao.queryByUUID(active)?.name?.takeIf { it.isNotBlank() }
            ?: service.getString(R.string.running)
    }

    private fun shouldShowTrafficNotification(): Boolean {
        val settings = settingsStore
        if (settings.containsKey("showTrafficNotification")) {
            return settings.decodeBool("showTrafficNotification", true)
        }
        return serviceStore.showTrafficNotification
    }

    private fun decodeTrafficHalf(encoded: Long): Long {
        val type = (encoded ushr 30) and 0x3L
        val data = encoded and 0x3FFFFFFFL
        return when (type.toInt()) {
            0 -> data
            1 -> (data * 1024L) / 100L
            2 -> (data * 1024L * 1024L) / 100L
            3 -> (data * 1024L * 1024L * 1024L) / 100L
            else -> 0L
        }
    }

    companion object {
        val VPN_CONFIG = Config(
            notificationId = 1001,
            channelId = "clash_vpn_service",
            channelName = "Clash VPN Service",
        )

        val HTTP_CONFIG = Config(
            notificationId = 1002,
            channelId = "clash_http_service",
            channelName = "Clash HTTP Service",
        )
    }
}
