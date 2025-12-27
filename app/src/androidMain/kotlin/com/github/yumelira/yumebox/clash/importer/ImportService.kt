package com.github.yumelira.yumebox.clash.importer

import com.github.yumelira.yumebox.clash.exception.*
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.bridge.ClashException
import com.github.yumelira.yumebox.core.model.FetchStatus
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ImportService(private val workDir: File) {

    companion object {
        private const val IMPORTED_DIR = "imported"
        private const val CONFIG_FILE = "config.yaml"
    }

    private val activeImports = ConcurrentHashMap<String, Job>()

    init {
        val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR)
        if (importedDir != null) {
            if (!importedDir.exists()) {
                val created = importedDir.mkdirs()
                if (!created && !importedDir.exists()) {
                    Timber.e(MLang.ProfilesPage.Import.Message.CannotCreateImportDir.format(importedDir.absolutePath))
                }
            }
        } else {
            Timber.e(MLang.ProfilesPage.Import.Message.CannotDetermineImportDirPath.format(workDir.absolutePath))
        }
    }

    suspend fun importProfile(
        profile: Profile,
        force: Boolean = false,
        onProgress: ((ImportState) -> Unit)? = null
    ): Result<String> = coroutineScope {
        try {
            if (activeImports.containsKey(profile.id)) {
                throw IllegalStateException(MLang.ProfilesPage.Import.Message.AlreadyImporting.format(profile.name))
            }

            activeImports[profile.id] = coroutineContext.job

            onProgress?.invoke(ImportState.Preparing(MLang.ProfilesPage.Import.Preparing))

            val configPath = when (profile.type) {
                ProfileType.URL -> importFromUrl(profile, force, onProgress)
                ProfileType.FILE -> importFromFile(profile, onProgress)
            }

            onProgress?.invoke(ImportState.Success(configPath))
            Result.success(configPath)

        } catch (e: CancellationException) {
            onProgress?.invoke(ImportState.Cancelled())
            cleanupFailedImport(profile.id)
            throw e
        } catch (e: Exception) {
            val importException = e.toConfigImportException()

            val errorMessage = importException.userFriendlyMessage

            Timber.e(MLang.ProfilesPage.Import.Message.ImportFailedLog.format(profile.name, errorMessage), e)
            onProgress?.invoke(ImportState.Failed(errorMessage, e))
            cleanupFailedImport(profile.id)
            Result.failure(importException)
        } finally {
            activeImports.remove(profile.id)
        }
    }

    fun cancelImport(profileId: String): Boolean {
        val job = activeImports[profileId]
        return if (job != null) {
            job.cancel()
            true
        } else {
            false
        }
    }

    private suspend fun importFromUrl(
        profile: Profile,
        force: Boolean,
        onProgress: ((ImportState) -> Unit)?
    ): String = withContext(Dispatchers.IO) {
        val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR)
            ?: throw FileAccessException(MLang.ProfilesPage.Import.Message.CannotDetermineImportDir, FileAccessException.Reason.INVALID_PATH)

        if (!importedDir.exists()) {
            val created = importedDir.mkdirs()
            if (!created && !importedDir.exists()) {
                throw FileAccessException(
                    MLang.ProfilesPage.Import.Message.CannotCreateImportDir.format(importedDir.absolutePath),
                    FileAccessException.Reason.NO_WRITE_PERMISSION
                )
            }
        }

        val targetDir = File(importedDir, profile.id)

        if (!targetDir.exists()) {
            val created = targetDir.mkdirs()
            if (!created && !targetDir.exists()) {
                throw FileAccessException(
                    MLang.ProfilesPage.Import.Message.CannotCreateConfigDir.format(targetDir.absolutePath),
                    FileAccessException.Reason.NO_WRITE_PERMISSION
                )
            }
        }

        val url = profile.remoteUrl ?: profile.config
        if (url.isBlank()) {
            throw NetworkException(MLang.ProfilesPage.Import.Message.URLEmpty)
        }

        onProgress?.invoke(ImportState.Downloading(0, 0L, 0L, MLang.ProfilesPage.Import.Downloading))

        try {
            Clash.fetchAndValid(
                path = targetDir,
                url = url,
                force = force,
                reportStatus = { status ->
                    ensureActive()

                    val progress = if (status.max > 0) {
                        (status.progress * 100 / status.max).coerceIn(0, 100)
                    } else 0

                    val state = when (status.action) {
                        FetchStatus.Action.FetchConfiguration -> {
                            ImportState.Downloading(progress, 0L, 0L, MLang.ProfilesPage.Import.DownloadingFile)
                        }

                        FetchStatus.Action.FetchProviders -> {
                            ImportState.LoadingProviders(
                                progress = progress,
                                currentProvider = status.args.firstOrNull(),
                                totalProviders = status.max,
                                completedProviders = status.progress,
                                message = MLang.ProfilesPage.Import.DownloadingExternal
                            )
                        }

                        FetchStatus.Action.Verifying -> {
                            ImportState.Validating(progress, MLang.ProfilesPage.Import.Validating)
                        }
                    }
                    onProgress?.invoke(state)
                }
            ).await()

            File(targetDir, CONFIG_FILE).absolutePath

        } catch (e: ClashException) {
            targetDir.deleteRecursively()
            throw ConfigValidationException(
                e.message ?: MLang.ProfilesPage.Import.Message.FormatValidationError.format(""),
                e
            )
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            throw NetworkException(MLang.ProfilesPage.Import.Message.NetworkConnFailed.format(e.message ?: ""), e)
        }
    }

    private suspend fun importFromFile(
        profile: Profile,
        onProgress: ((ImportState) -> Unit)?
    ): String = withContext(Dispatchers.IO) {
        val sourcePath = profile.config
        if (sourcePath.isBlank()) {
            throw FileAccessException(MLang.ProfilesPage.Import.Message.URLEmpty, FileAccessException.Reason.INVALID_PATH)
        }

        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) {
            throw FileAccessException(MLang.ProfilesPage.Import.Message.FileNotFound.format(sourcePath), FileAccessException.Reason.NOT_FOUND)
        }

        if (!sourceFile.canRead()) {
            throw FileAccessException(
                MLang.ProfilesPage.Import.Message.NoReadPermission.format(sourcePath),
                FileAccessException.Reason.NO_READ_PERMISSION
            )
        }

        val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR)
            ?: throw FileAccessException(MLang.ProfilesPage.Import.Message.CannotDetermineImportDir, FileAccessException.Reason.INVALID_PATH)

        if (!importedDir.exists()) {
            val created = importedDir.mkdirs()
            if (!created && !importedDir.exists()) {
                throw FileAccessException(
                    MLang.ProfilesPage.Import.Message.CannotCreateImportDir.format(importedDir.absolutePath),
                    FileAccessException.Reason.NO_WRITE_PERMISSION
                )
            }
        }

        val targetDir = File(importedDir, profile.id)

        if (!targetDir.exists()) {
            val created = targetDir.mkdirs()
            if (!created && !targetDir.exists()) {
                throw FileAccessException(
                    MLang.ProfilesPage.Import.Message.CannotCreateConfigDir.format(targetDir.absolutePath),
                    FileAccessException.Reason.NO_WRITE_PERMISSION
                )
            }
        }

        onProgress?.invoke(ImportState.Copying(0, MLang.ProfilesPage.Import.Copying))

        val targetFile = File(targetDir, CONFIG_FILE)

        try {
            val isSameFile = runCatching {
                sourceFile.canonicalPath == targetFile.canonicalPath
            }.getOrElse { e ->
                Timber.w(e, MLang.ProfilesPage.Import.Message.PathCompareFallback)
                sourceFile.absolutePath == targetFile.absolutePath
            }

            if (!isSameFile) {
                sourceFile.copyTo(targetFile, overwrite = true)
                onProgress?.invoke(ImportState.Copying(50, MLang.ProfilesPage.Import.CopyingDone))
            } else {
                onProgress?.invoke(ImportState.Copying(50, MLang.ProfilesPage.Import.FileReady))
            }

            onProgress?.invoke(ImportState.Validating(50, MLang.ProfilesPage.Import.Validating))

            Clash.fetchAndValid(
                path = targetDir,
                url = "",
                force = false,
                reportStatus = { status ->
                    ensureActive()

                    val progress = if (status.max > 0) {
                        50 + (status.progress * 50 / status.max).coerceIn(0, 50)
                    } else 50

                    val state = when (status.action) {
                        FetchStatus.Action.FetchProviders -> {
                            ImportState.LoadingProviders(
                                progress = progress,
                                currentProvider = status.args.firstOrNull(),
                                totalProviders = status.max,
                                completedProviders = status.progress,
                                message = MLang.ProfilesPage.Import.DownloadingExternal
                            )
                        }

                        FetchStatus.Action.Verifying -> {
                            ImportState.Validating(progress, MLang.ProfilesPage.Import.Validating)
                        }

                        else -> {
                            null
                        }
                    }
                    state?.let { onProgress?.invoke(it) }
                }
            ).await()

            targetFile.absolutePath

        } catch (e: ClashException) {
            targetDir.deleteRecursively()
            throw ConfigValidationException(
                e.message ?: MLang.ProfilesPage.Import.Message.FormatValidationError.format(""),
                e
            )
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            throw FileAccessException(MLang.ProfilesPage.Import.ImportFailed.format(e.message ?: ""), FileAccessException.Reason.UNKNOWN, e)
        }
    }

    private fun cleanupFailedImport(profileId: String) {
        runCatching {
            val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR) ?: return
            val profileDir = File(importedDir, profileId)
            if (profileDir.exists()) {
                profileDir.deleteRecursively()
            }
        }.onFailure { e ->
            Timber.e(e, MLang.ProfilesPage.Import.Message.CleanupFailed.format(profileId), e)
        }
    }

    fun cleanupOrphanedConfigs(validProfileIds: Set<String>) {
        runCatching {
            val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR)
            if (importedDir == null || !importedDir.exists()) {
                return
            }

            var cleanedCount = 0
            importedDir.listFiles()?.forEach { dir ->
                if (dir.isDirectory && dir.name !in validProfileIds) {
                    dir.deleteRecursively()
                    cleanedCount++
                }
            }

            if (cleanedCount > 0) {
                Timber.i(MLang.ProfilesPage.Import.Message.CleanupOrphanedSuccess.format(cleanedCount.toString()))
            }
        }.onFailure { e ->
            Timber.e(e, MLang.ProfilesPage.Import.Message.CleanupOrphanedFailed, e)
        }
    }
}
