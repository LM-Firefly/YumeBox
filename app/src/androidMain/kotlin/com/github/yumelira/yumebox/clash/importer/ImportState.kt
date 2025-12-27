package com.github.yumelira.yumebox.clash.importer

import dev.oom_wg.purejoy.mlang.MLang

sealed class ImportState {
    object Idle : ImportState()

    data class Preparing(
        val message: String = MLang.ProfilesPage.Import.Preparing
    ) : ImportState()

    data class Downloading(
        val progress: Int,
        val currentBytes: Long,
        val totalBytes: Long,
        val message: String = MLang.ProfilesPage.Import.Downloading
    ) : ImportState() {
        val progressPercent: Int
            get() = if (totalBytes > 0) {
                ((currentBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            } else progress
    }

    data class Copying(
        val progress: Int,
        val message: String = MLang.ProfilesPage.Import.Copying
    ) : ImportState()

    data class Validating(
        val progress: Int,
        val message: String = MLang.ProfilesPage.Import.Validating
    ) : ImportState()

    data class LoadingProviders(
        val progress: Int,
        val currentProvider: String?,
        val totalProviders: Int,
        val completedProviders: Int,
        val message: String = MLang.ProfilesPage.Import.DownloadingExternal
    ) : ImportState() {
        val providerProgress: String
            get() = "$completedProviders/$totalProviders"
    }

    data class Retrying(
        val attempt: Int,
        val maxAttempts: Int,
        val reason: String,
        val nextRetryInSeconds: Int
    ) : ImportState() {
        val retryMessage: String
            get() = MLang.ProfilesPage.Import.Message.Retrying.format(attempt, maxAttempts, reason)
    }

    data class Cleaning(
        val message: String = MLang.ProfilesPage.Import.Message.FileAccessError
    ) : ImportState()

    data class Success(
        val configPath: String,
        val message: String = MLang.ProfilesPage.Import.Success
    ) : ImportState()

    data class Failed(
        val error: String,
        val exception: Throwable?,
        val isRetryable: Boolean = false
    ) : ImportState() {
        val shortError: String
            get() = error.take(200)
    }

    data class Cancelled(
        val message: String = MLang.ProfilesPage.Import.Canceled
    ) : ImportState()

}

