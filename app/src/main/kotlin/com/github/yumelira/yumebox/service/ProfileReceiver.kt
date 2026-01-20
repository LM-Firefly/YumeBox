package com.github.yumelira.yumebox.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.service.ProfileAutoUpdateService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import timber.log.Timber

class ProfileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED, ACTION_RESCHEDULE_ALL -> {
                try {
                    val koin = GlobalContext.getOrNull()
                    if (koin != null) {
                        koin.get<ProfilesStore>().let { store ->
                            val profiles = store.getAllProfiles()
                            profiles.forEach { p ->
                                if (p.type == ProfileType.URL && p.autoUpdateMinutes > 0) {
                                    scheduleNext(context, p)
                                }
                            }
                        }
                    } else {
                        scheduleRescheduleRetry(context, 1)
                    }
                } catch (t: Throwable) {
                    Timber.tag(TAG).w(t, "Direct reschedule failed; scheduling retry")
                    scheduleRescheduleRetry(context, 1)
                }
            }
            ACTION_PROFILE_REQUEST_UPDATE -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                if (profileId != null && profileId.isNotBlank()) {
                    // Best-effort: start the existing ProfileAutoUpdateService
                    ProfileAutoUpdateService.start(context, profileId)
                }
            }
        }
    }
    companion object {
        private const val TAG = "ProfileReceiver"
        const val ACTION_PROFILE_REQUEST_UPDATE = "com.github.yumelira.yumebox.action.PROFILE_REQUEST_UPDATE"
        const val ACTION_RESCHEDULE_ALL = "com.github.yumelira.yumebox.action.RESCHEDULE_ALL"
        const val EXTRA_PROFILE_ID = "profile_id"
        private val lock = Mutex()
        private var initialized = false
        suspend fun rescheduleAll(context: Context) = withContext(Dispatchers.IO) {
            lock.withLock {
                if (initialized) return@withContext
                initialized = true
                Timber.tag(TAG).d("Reschedule all profile updates")
                val store = try { GlobalContext.get().get<ProfilesStore>() } catch (_: Exception) { null }
                val profiles = store?.getAllProfiles() ?: emptyList()
                profiles.forEach { p ->
                    if (p.type == ProfileType.URL && p.autoUpdateMinutes > 0) {
                        scheduleNext(context, p)
                    }
                }
            }
        }
        fun cancelNext(context: Context, profile: Profile) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val pi = pendingIntentOf(context, profile.id)
                am.cancel(pi)
                try { context.stopService(Intent(context, ProfileAutoUpdateService::class.java).apply { putExtra(EXTRA_PROFILE_ID, profile.id) }) } catch (_: Throwable) {}
                Timber.tag(TAG).d("Cancelled auto-update for %s", profile.id)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "cancelNext exception for %s", profile.id)
            }
        }
        fun schedule(context: Context, profile: Profile) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pi = pendingIntentOf(context, profile.id)
            am.cancel(pi)
            pi.send(context, 0, null)
        }
        fun scheduleNext(context: Context, profile: Profile) {
            try {
                if (profile.type != ProfileType.URL) return
                if (profile.autoUpdateMinutes <= 0) return
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val pi = pendingIntentOf(context, profile.id)
                am.cancel(pi)
                // Enforce minimum interval (echo CMFA behavior)
                val minIntervalMs = TimeUnit.MINUTES.toMillis(15)
                val intervalMs = TimeUnit.MINUTES.toMillis(profile.autoUpdateMinutes.toLong())
                if (intervalMs < minIntervalMs) return
                val now = System.currentTimeMillis()
                val importedCfg = context.filesDir.resolve("imported/${profile.id}/config.yaml")
                val last = if (importedCfg.exists()) importedCfg.lastModified() else -1L
                if (last < 0) return
                val remaining = (intervalMs - (now - last)).coerceAtLeast(0L)
                am.set(AlarmManager.RTC, now + remaining, pi)
                Timber.tag(TAG).d("Scheduled auto-update for %s in %d ms (interval=%dmin)", profile.id, remaining, profile.autoUpdateMinutes)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "scheduleNext exception for %s", profile.id)
            }
        }
        fun scheduleRetry(context: Context, profileId: String, minutesFromNow: Long) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(ACTION_PROFILE_REQUEST_UPDATE).setClass(context, ProfileReceiver::class.java).apply {
                    putExtra(EXTRA_PROFILE_ID, profileId)
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                val pi = PendingIntent.getBroadcast(context, profileId.hashCode(), intent, flags)
                am.cancel(pi)
                val now = System.currentTimeMillis()
                val triggerAt = now + TimeUnit.MINUTES.toMillis(minutesFromNow)
                am.set(AlarmManager.RTC, triggerAt, pi)
                Timber.tag(TAG).d("Scheduled retry update for %s in %d minutes", profileId, minutesFromNow)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "scheduleRetry exception for %s", profileId)
            }
        }
        fun scheduleRescheduleRetry(context: Context, minutesFromNow: Long) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(ACTION_RESCHEDULE_ALL).setClass(context, ProfileReceiver::class.java)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                val pi = PendingIntent.getBroadcast(context, 0, intent, flags)
                am.cancel(pi)
                val now = System.currentTimeMillis()
                val triggerAt = now + TimeUnit.MINUTES.toMillis(minutesFromNow)
                am.set(AlarmManager.RTC, triggerAt, pi)
                Timber.tag(TAG).d("Scheduled reschedule retry in %d minutes", minutesFromNow)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "scheduleRescheduleRetry exception")
            }
        }
        private fun pendingIntentOf(context: Context, profileId: String): PendingIntent {
            val intent = Intent(ACTION_PROFILE_REQUEST_UPDATE).setClass(context, ProfileReceiver::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getBroadcast(context, profileId.hashCode(), intent, flags)
        }
    }
}
