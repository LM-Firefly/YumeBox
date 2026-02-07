package com.github.yumelira.yumebox.clash

import com.github.yumelira.yumebox.App
import com.github.yumelira.yumebox.common.util.DownloadUtil
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.data.store.ProfilesStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

object ProfileUpdateManager : KoinComponent {
    private val profilesStore: ProfilesStore by inject()
    suspend fun updateProfile(
        profile: Profile,
        saveToDb: Boolean = true,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<Profile> = withContext(Dispatchers.IO) {
        runCatching {
            if (profile.type != ProfileType.URL) {
                return@runCatching updateLocalProfile(profile, saveToDb, onProgress)
            }
            val remoteUrl = profile.remoteUrl ?: profile.config
            if (remoteUrl.isBlank()) {
                throw IllegalArgumentException("Profile URL is empty")
            }
            onProgress?.invoke("获取订阅信息...", 0)
            // 1. Download to temp file and get subscription info
            val tempFile = File.createTempFile("update_${profile.id}", ".yaml")
            val (downloadSuccess, subscriptionInfo) = DownloadUtil.downloadWithSubscriptionInfo(
                url = remoteUrl,
                targetFile = tempFile,
                onProgress = { progress ->
                    // Map download progress 0-100 to overall progress 0-50
                    onProgress?.invoke("正在下载: ${progress.progress}%", progress.progress / 2)
                }
            )
            if (!downloadSuccess) {
                tempFile.delete()
                throw Exception("配置文件下载失败")
            }
            // 2. Import using the downloaded file
            val workDir = App.instance.filesDir.resolve("clash")
            val importResult = downloadProfile(
                profile = profile,
                workDir = workDir,
                sourceFile = tempFile, // Use downloaded file
                force = true,
                onProgress = { msg, p ->
                    // Map import progress 0-100 to overall progress 50-100
                    onProgress?.invoke(msg, 50 + p / 2)
                }
            )
            tempFile.delete()
            val configPath = importResult.getOrThrow()
            // 3. Update Profile Data
            var updated = profile.copy(
                config = configPath,
                updatedAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis()
            )
            // Update metadata from subscription info
            subscriptionInfo?.let { info ->
                updated = updated.copy(
                    provider = info.title ?: updated.provider,
                    expireAt = info.expire ?: updated.expireAt,
                    usedBytes = info.upload + info.download,
                    totalBytes = if (info.total > 0) info.total else updated.totalBytes
                )
                // Update file name if it's default or temp
                 info.filename?.let { fileName ->
                     val nameWithoutExt = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
                     val defaultName = "Unknown" // Or check against specific defaults if needed
                     // Simple heuristic: if name is empty, use filename
                     if (updated.name.isBlank()) {
                         updated = updated.copy(name = nameWithoutExt)
                     }
                 }
            }
            if (saveToDb) {
                profilesStore.updateProfile(updated)
                // Schedule next update if auto-update is enabled
                if (updated.autoUpdateMinutes > 0) {
                    scheduleNext(updated)
                }
            }
            updated
        }
    }
    private suspend fun updateLocalProfile(
        profile: Profile,
        saveToDb: Boolean,
        onProgress: ((String, Int) -> Unit)?
    ): Profile {
        val workDir = App.instance.filesDir.resolve("clash")
        val importResult = downloadProfile(
            profile = profile,
            workDir = workDir,
            force = true,
            onProgress = onProgress
        )
        val configPath = importResult.getOrThrow()
        val updated = profile.copy(
            config = configPath,
            updatedAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis()
        )
        if (saveToDb) {
            profilesStore.updateProfile(updated)
        }
        return updated
    }
}
