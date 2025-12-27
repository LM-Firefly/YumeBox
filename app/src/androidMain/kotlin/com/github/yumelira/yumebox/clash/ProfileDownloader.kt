package com.github.yumelira.yumebox.clash

import com.github.yumelira.yumebox.clash.exception.ConfigImportException
import com.github.yumelira.yumebox.clash.exception.UnknownException
import com.github.yumelira.yumebox.clash.importer.ImportService
import com.github.yumelira.yumebox.clash.importer.ImportState
import com.github.yumelira.yumebox.data.model.Profile
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

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
            Timber.e(error, MLang.ProfilesPage.Message.DownloadFailedWithName.format(profile.name))

            val friendlyError = when (error) {
                is ConfigImportException -> error.userFriendlyMessage
                else -> error?.message ?: MLang.ProfilesPage.Import.Message.UnknownError
            }

            Result.failure(UnknownException(friendlyError, error))
        }
    }.getOrElse { e ->
        Timber.e(e, MLang.ProfilesPage.Message.DownloadFailedWithName.format(profile.name))
        Result.failure(e)
    }
}

private fun mapStateToProgress(state: ImportState): Pair<String, Int> {
    return when (state) {
        is ImportState.Idle -> MLang.ProfilesPage.Progress.Idle to 0
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
        Timber.e(e, MLang.ProfilesPage.Import.Message.CleanupOrphanedFailed.format(e.message ?: ""))
    }
}

fun Profile.isConfigSaved(workDir: File): Boolean {
    val importedDir = workDir.parentFile?.resolve("imported") ?: return false
    val configFile = File(importedDir, "$id/config.yaml")
    return configFile.exists() && configFile.length() > 0
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
        val data = androidx.work.workDataOf(com.github.yumelira.yumebox.worker.ProfileAutoUpdateWorker.KEY_PROFILE_ID to profile.id)
        val request = androidx.work.OneTimeWorkRequestBuilder<com.github.yumelira.yumebox.worker.ProfileAutoUpdateWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        androidx.work.WorkManager.getInstance(com.github.yumelira.yumebox.App.instance).enqueueUniqueWork(uniqueWorkName(profile.id), androidx.work.ExistingWorkPolicy.REPLACE, request)
        Timber.d("Scheduled auto-update for %s in %d ms", profile.id, delayMs)
    } catch (e: Exception) {
        Timber.e(e, "Failed to schedule auto-update for %s", profile.id)
    }
}

fun cancel(profileId: String) {
    try {
        androidx.work.WorkManager.getInstance(com.github.yumelira.yumebox.App.instance).cancelUniqueWork(uniqueWorkName(profileId))
        Timber.d("Canceled auto-update for %s", profileId)
    } catch (e: Exception) {
        Timber.e(e, "Failed to cancel auto-update for %s", profileId)
    }
}

fun restoreAll() {
    try {
        val profilesStore = org.koin.core.context.GlobalContext.get().get<com.github.yumelira.yumebox.data.store.ProfilesStore>()
        val profiles = profilesStore.getAllProfiles()
        profiles.filter { it.type == com.github.yumelira.yumebox.data.model.ProfileType.URL && it.autoUpdateMinutes > 0 }
            .forEach { scheduleNext(it) }
        Timber.d("Restored auto-update schedules for ${profiles.size} profiles")
    } catch (e: Exception) {
        Timber.e(e, "Failed to restore auto-update schedules")
    }
}

suspend fun performUpdate(profileId: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val profilesStore: com.github.yumelira.yumebox.data.store.ProfilesStore = org.koin.core.context.GlobalContext.get().get()
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
