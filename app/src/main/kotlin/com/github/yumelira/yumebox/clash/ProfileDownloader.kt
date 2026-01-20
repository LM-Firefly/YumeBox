package com.github.yumelira.yumebox.clash

import com.github.yumelira.yumebox.clash.exception.ConfigImportException
import com.github.yumelira.yumebox.clash.exception.UnknownException
import com.github.yumelira.yumebox.clash.importer.ImportService
import com.github.yumelira.yumebox.clash.importer.ImportState
import com.github.yumelira.yumebox.data.model.Profile
import dev.oom_wg.purejoy.mlang.MLang
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber

// 缓存机制：避免频繁反射查询和文件 I/O
private object ProfileDownloaderCache {
    private var cachedProfilesStore: com.github.yumelira.yumebox.data.store.ProfilesStore? = null
    private val fileCheckCache = mutableMapOf<String, Pair<Long, Boolean>>() // path -> (timestamp, exists)
    private const val FILE_CHECK_CACHE_TTL_MS = 5000L

    fun getProfilesStore(): com.github.yumelira.yumebox.data.store.ProfilesStore {
        return cachedProfilesStore ?: run {
            val store = org.koin.core.context.GlobalContext.get().get<com.github.yumelira.yumebox.data.store.ProfilesStore>()
            cachedProfilesStore = store
            store
        }
    }

    fun isConfigExists(configFile: File): Boolean {
        val key = configFile.absolutePath
        val now = System.currentTimeMillis()
        val cached = fileCheckCache[key]
        return if (cached != null && (now - cached.first) < FILE_CHECK_CACHE_TTL_MS) {
            cached.second
        } else {
            val exists = configFile.exists() && configFile.length() > 0
            fileCheckCache[key] = now to exists
            // 清理过期缓存
            fileCheckCache.entries.removeAll { (now - it.value.first) >= FILE_CHECK_CACHE_TTL_MS }
            exists
        }
    }

    fun clearCache() {
        cachedProfilesStore = null
        fileCheckCache.clear()
    }
}

suspend fun downloadProfile(
    profile: Profile,
    workDir: File,
    force: Boolean = true,
    onProgress: ((String, Int) -> Unit)? = null
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val importService = ImportService(workDir)

        val result = importService.importProfile(
            profile = profile,
            force = force,
            onProgress = { state ->
                val (message, progress) = mapStateToProgress(state)
                onProgress?.invoke(message, progress)
            }
        )

        if (result.isSuccess) {
            val configPath = result.getOrThrow()
            Result.success(configPath)
        } else {
            val error = result.exceptionOrNull()
            Timber.e(error, "配置下载失败: ${profile.name}")

            val friendlyError = when (error) {
                is ConfigImportException -> error.userFriendlyMessage
                else -> error?.message ?: "未知错误"
            }

            Result.failure(UnknownException(friendlyError, error))
        }
    }.getOrElse { e ->
        Timber.e(e, "下载配置异常: ${profile.name}")
        Result.failure(e)
    }
}

private fun mapStateToProgress(state: ImportState): Pair<String, Int> {
    return when (state) {
        is ImportState.Idle -> "空闲" to 0
        is ImportState.Preparing -> state.message to 5
        is ImportState.Downloading -> state.message to (10 + (state.progressPercent * 0.4).toInt())
        is ImportState.Copying -> state.message to (10 + (state.progress * 0.4).toInt())
        is ImportState.Validating -> state.message to (50 + (state.progress * 0.2).toInt())
        is ImportState.LoadingProviders -> state.message to (70 + (state.progress * 0.2).toInt())
        is ImportState.Retrying -> state.retryMessage to 5
        is ImportState.Cleaning -> state.message to 95
        is ImportState.Success -> state.message to 100
        is ImportState.Failed -> state.shortError to 0
        is ImportState.Cancelled -> state.message to 0
    }
}

fun cleanupOrphanedConfigs(
    workDir: File,
    validProfiles: List<Profile>
) {
    runCatching {
        val importService = ImportService(workDir)
        val validIds = validProfiles.map { it.id }.toSet()
        importService.cleanupOrphanedConfigs(validIds)
    }.onFailure { e ->
        Timber.e(e, "孤儿配置清理失败")
    }
}

fun Profile.isConfigSaved(workDir: File): Boolean {
    val importedDir = workDir.parentFile?.resolve("imported") ?: return false
    val configFile = File(importedDir, "$id/config.yaml")
    return ProfileDownloaderCache.isConfigExists(configFile)
}

// ---- Auto-update / scheduling helpers moved here ----
private fun uniqueWorkName(profileId: String) = "profile_auto_update_$profileId"

fun scheduleNext(profile: Profile) {
    try {
        if (profile.autoUpdateMinutes <= 0) {
            cancel(profile.id)
            return
        }
        val effectiveMinutes = when {
            profile.autoUpdateMinutes <= 0 -> { cancel(profile.id); return }
            profile.autoUpdateMinutes in 1..14 -> 15
            else -> profile.autoUpdateMinutes
        }
        val now = System.currentTimeMillis()
        val last = profile.lastUpdatedAt ?: profile.updatedAt
        val delayMs = (effectiveMinutes * 60_000L).let { interval ->
            val target = last + interval
            val d = target - now
            if (d <= 0) 0L else d
        }
        val context = com.github.yumelira.yumebox.App.instance
        val intent = Intent(context, com.github.yumelira.yumebox.service.ProfileAutoUpdateService::class.java).apply {
            putExtra(com.github.yumelira.yumebox.service.ProfileAutoUpdateService.EXTRA_PROFILE_ID, profile.id)
        }
        val requestCode = profile.id.hashCode()
        val pending = PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + delayMs
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            else -> alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
        Timber.v("Scheduled auto-update for %s in %d ms (alarm)", profile.id, delayMs)
    } catch (e: Exception) {
        Timber.e(e, "Failed to schedule auto-update for %s", profile.id)
    }
}

fun cancel(profileId: String) {
    try {
        val context = com.github.yumelira.yumebox.App.instance
        val intent = Intent(context, com.github.yumelira.yumebox.service.ProfileAutoUpdateService::class.java).apply {
            putExtra(com.github.yumelira.yumebox.service.ProfileAutoUpdateService.EXTRA_PROFILE_ID, profileId)
        }
        val pending = PendingIntent.getService(context, profileId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
        Timber.v("Canceled auto-update for %s (alarm)", profileId)
    } catch (e: Exception) {
        Timber.e(e, "Failed to cancel auto-update for %s", profileId)
    }
}

fun restoreAll() {
    try {
        val profilesStore = ProfileDownloaderCache.getProfilesStore()
        val profiles = profilesStore.getAllProfiles()
        val updateableProfiles = profiles.filter { it.type == com.github.yumelira.yumebox.data.model.ProfileType.URL && it.autoUpdateMinutes > 0 }
        updateableProfiles.forEach { scheduleNext(it) }
        Timber.d("Restored auto-update schedules for %d profiles", updateableProfiles.size)
    } catch (e: Exception) {
        Timber.e(e, "Failed to restore auto-update schedules")
    }
}

suspend fun performUpdate(profileId: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val profilesStore = ProfileDownloaderCache.getProfilesStore()
        val profile = profilesStore.getAllProfiles().firstOrNull { it.id == profileId }
        if (profile == null) {
            Timber.d("Profile $profileId not found for auto-update")
            return@withContext Result.success(Unit)
        }
        if (profile.type != com.github.yumelira.yumebox.data.model.ProfileType.URL || profile.autoUpdateMinutes <= 0) {
            Timber.d("Profile ${profile.id} auto-update disabled or not remote")
            return@withContext Result.success(Unit)
        }

        val workDir = File(com.github.yumelira.yumebox.App.instance.filesDir, "clash")
        val result = downloadProfile(profile, workDir, force = true)
        if (result.isSuccess) {
            val configPath = result.getOrThrow()
            val now = System.currentTimeMillis()
            val updated = profile.copy(
                config = configPath,
                updatedAt = now,
                lastUpdatedAt = now
            )
            profilesStore.updateProfile(updated)
            // schedule next
            scheduleNext(updated)
            Result.success(Unit)
        } else {
            val ex = result.exceptionOrNull()
            Timber.e(ex, "Auto-update failed for %s", profile.id)
            Result.failure(ex ?: Exception("Auto-update failed"))
        }
    } catch (e: Exception) {
        Timber.e(e, "Auto-update worker exception for %s", profileId)
        Result.failure(e)
    }
}
