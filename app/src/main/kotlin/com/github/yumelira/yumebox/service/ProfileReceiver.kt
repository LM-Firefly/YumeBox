package com.github.yumelira.yumebox.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.data.store.ProfilesStorage
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
                Timber.tag(TAG).i("=== System event received: %s, rescheduling all profiles ===", intent.action)
                try {
                    val koin = GlobalContext.getOrNull()
                    if (koin != null) {
                        koin.get<ProfilesStorage>().let { store ->
                            val profiles = store.getAllProfiles()
                            val urlProfiles = profiles.filter { it.type == ProfileType.URL && it.autoUpdateMinutes > 0 }
                            Timber.tag(TAG).i("Found %d auto-update enabled profiles", urlProfiles.size)
                            urlProfiles.forEach { p ->
                                scheduleNext(context, p)
                            }
                        }
                        Timber.tag(TAG).i("Successfully rescheduled all profiles")
                    } else {
                        Timber.tag(TAG).w("Koin context not ready; scheduling reschedule retry in 1 minute")
                        scheduleRescheduleRetry(context, 1)
                    }
                } catch (t: Throwable) {
                    Timber.tag(TAG).e(t, "Failed to reschedule profiles; scheduling retry in 1 minute")
                    scheduleRescheduleRetry(context, 1)
                }
            }
            ACTION_PROFILE_REQUEST_UPDATE -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                val retryCount = intent.getIntExtra(ProfileAutoUpdateService.EXTRA_RETRY_COUNT, 0)
                if (profileId != null && profileId.isNotBlank()) {
                    Timber.tag(TAG).i("=== Triggered profile auto-update: %s (retry: %d) ===", profileId, retryCount)
                    ProfileAutoUpdateService.start(context, profileId, retryCount)
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
                if (initialized) {
                    Timber.tag(TAG).d("rescheduleAll already initialized, skipping")
                    return@withContext
                }
                initialized = true
                Timber.tag(TAG).i("=== Rescheduling all profile auto-updates (app initialization) ===")
                val store = try { GlobalContext.get().get<ProfilesStorage>() } catch (_: Exception) { null }
                val profiles = store?.getAllProfiles() ?: emptyList()
                val urlProfiles = profiles.filter { it.type == ProfileType.URL && it.autoUpdateMinutes > 0 }
                Timber.tag(TAG).i("Found %d profiles with auto-update enabled", urlProfiles.size)
                urlProfiles.forEach { p ->
                    try {
                        scheduleNext(context, p)
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Failed to schedule profile %s", p.id)
                    }
                }
                Timber.tag(TAG).i("Completed rescheduling all profiles")
            }
        }
        suspend fun resetSchedulingState() = lock.withLock {
            initialized = false
            Timber.tag(TAG).d("Reset profile scheduling state")
        }
        fun cancelNext(context: Context, profile: Profile) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val pi = pendingIntentOf(context, profile.id)
                am.cancel(pi)
                Timber.tag(TAG).d("Cancelled auto-update alarm for profile %s", profile.id)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to cancel update for profile %s", profile.id)
            }
        }
        fun schedule(context: Context, profile: Profile) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val pi = pendingIntentOf(context, profile.id)
                am.cancel(pi)
                pi.send(context, 0, null)
                Timber.tag(TAG).d("Triggered immediate update for profile %s", profile.id)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to trigger immediate update for profile %s", profile.id)
            }
        }
        fun scheduleNext(context: Context, profile: Profile) {
            try {
                if (profile.type != ProfileType.URL) return
                if (profile.autoUpdateMinutes <= 0) return
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val pi = pendingIntentOf(context, profile.id)
                am.cancel(pi)
                val minIntervalMs = TimeUnit.MINUTES.toMillis(15)
                val intervalMs = TimeUnit.MINUTES.toMillis(profile.autoUpdateMinutes.toLong())
                if (intervalMs < minIntervalMs) {
                    Timber.tag(TAG).d("Profile %s interval (%d min) less than minimum (15 min); skipping schedule", 
                        profile.id, profile.autoUpdateMinutes)
                    return
                }
                val now = System.currentTimeMillis()
                val lastUpdated = profile.lastUpdatedAt ?: profile.updatedAt
                val baseTime = if (now - lastUpdated < TimeUnit.HOURS.toMillis(24)) {
                    lastUpdated
                } else {
                    now
                }
                val triggerTime = baseTime + intervalMs
                val delay = triggerTime - now
                am.set(AlarmManager.RTC, triggerTime, pi)
                Timber.tag(TAG).i("Scheduled auto-update for profile: %s (interval=%d min, next in %.1f min, baseTime=%.1f min ago)", 
                    profile.id, profile.autoUpdateMinutes, delay / 60000.0, (now - baseTime) / 60000.0)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to schedule next update for profile %s", profile.id)
            }
        }
        fun scheduleRetry(context: Context, profileId: String, minutesFromNow: Long, retryCount: Int = 0) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(ACTION_PROFILE_REQUEST_UPDATE).setClass(context, ProfileReceiver::class.java).apply {
                    putExtra(EXTRA_PROFILE_ID, profileId)
                    putExtra(ProfileAutoUpdateService.EXTRA_RETRY_COUNT, retryCount)
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                val pi = PendingIntent.getBroadcast(context, profileId.hashCode(), intent, flags)
                am.cancel(pi)
                val now = System.currentTimeMillis()
                val triggerAt = now + TimeUnit.MINUTES.toMillis(minutesFromNow)
                am.set(AlarmManager.RTC, triggerAt, pi)
                Timber.tag(TAG).w("Scheduled RETRY for profile %s in %d minutes (retry #%d)", profileId, minutesFromNow, retryCount)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to schedule retry for profile %s", profileId)
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
                Timber.tag(TAG).w("Scheduled RESCHEDULE RETRY in %d minutes (will retry initializing profile updates)", minutesFromNow)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to schedule reschedule retry")
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
