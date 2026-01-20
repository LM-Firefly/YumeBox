package com.github.yumelira.yumebox.clash

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.store.ProfilesStorage
import com.github.yumelira.yumebox.service.ProfileAutoUpdateService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import timber.log.Timber

private const val TAG = "AutoUpdateScheduler"

fun scheduleNext(profile: Profile) {
    try {
        val context = com.github.yumelira.yumebox.core.Global.application
        com.github.yumelira.yumebox.service.ProfileReceiver.scheduleNext(context, profile)
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "scheduleNext delegate failed for %s", profile.id)
    }
}

fun cancel(profileId: String) {
    try {
        val context = com.github.yumelira.yumebox.core.Global.application
        val profile = com.github.yumelira.yumebox.data.model.Profile(id = profileId, name = "", type = com.github.yumelira.yumebox.data.model.ProfileType.URL)
        com.github.yumelira.yumebox.service.ProfileReceiver.cancelNext(context, profile)
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "cancel delegate failed for %s", profileId)
    }
}

suspend fun restoreAll() {
    withContext(Dispatchers.IO) {
        try {
            val context = com.github.yumelira.yumebox.core.Global.application
            com.github.yumelira.yumebox.service.ProfileReceiver.rescheduleAll(context)
            Timber.tag(TAG).d("Restored all auto-update schedules")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "restoreAll failed")
        }
    }
}

suspend fun performUpdate(profileId: String, retryCount: Int = 0): Result<Boolean> = withContext(Dispatchers.IO) {
    if (retryCount >= 5) {
        Timber.tag(TAG).e("Max retries (%d) reached for profile %s, aborting.", retryCount, profileId)
        return@withContext Result.failure(IllegalStateException("Max retries reached"))
    }
    val maxAttempts = 3
    val retryDelayMs = 5000L
    try {
        val profilesStore = try { GlobalContext.get().get<ProfilesStorage>() } catch (_: Exception) { null }
            ?: return@withContext Result.failure<Boolean>(IllegalStateException("ProfilesStorage not available"))
        val profile = profilesStore.getAllProfiles().firstOrNull { it.id == profileId }
            ?: return@withContext Result.failure<Boolean>(IllegalArgumentException("Profile not found: $profileId"))
        if (profile.type != com.github.yumelira.yumebox.data.model.ProfileType.URL) {
            return@withContext Result.failure<Boolean>(IllegalArgumentException("Profile is not URL type: $profileId"))
        }
        val workDir = com.github.yumelira.yumebox.core.Global.application.filesDir.resolve("clash")
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            if (attempt > 0) {
                kotlinx.coroutines.delay(retryDelayMs * attempt)
            }
            val result = runCatching {
                downloadProfile(profile, workDir, force = true)
            }.getOrElse { e ->
                if (e is kotlinx.coroutines.CancellationException) {
                    Timber.tag(TAG).d("performUpdate cancelled for %s (attempt %d)", profileId, attempt + 1)
                    return@withContext Result.failure(e)
                } else {
                    Result.failure(e)
                }
            }
            if (result.isSuccess) {
                val configPath = result.getOrThrow()
                val updated = profile.copy(config = configPath, lastUpdatedAt = System.currentTimeMillis())
                profilesStore.updateProfile(updated)
                scheduleNext(updated)
                Timber.tag(TAG).d("performUpdate succeeded for %s", profileId)
                return@withContext Result.success(true)
            } else {
                lastError = result.exceptionOrNull()
                Timber.tag(TAG).w(lastError, "performUpdate attempt %d failed for %s", attempt + 1, profileId)
            }
        }
        val ctx = com.github.yumelira.yumebox.core.Global.application
        com.github.yumelira.yumebox.service.ProfileReceiver.scheduleRetry(ctx, profileId, 15, retryCount + 1)
        Timber.tag(TAG).e(lastError, "performUpdate all %d attempts failed for %s; scheduled retry #%d in 15 minutes", maxAttempts, profileId, retryCount + 1)
        Result.failure(lastError ?: IllegalStateException("Unknown update error"))
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "performUpdate unexpected exception for %s", profileId)
        Result.failure(e)
    }
}
