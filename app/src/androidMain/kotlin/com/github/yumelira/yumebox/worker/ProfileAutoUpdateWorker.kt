package com.github.yumelira.yumebox.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.workDataOf
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ProfileAutoUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PROFILE_ID = "profileId"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val profileId = inputData.getString(KEY_PROFILE_ID)
        if (profileId.isNullOrBlank()) return@withContext Result.success()
        val result = com.github.yumelira.yumebox.clash.performUpdate(profileId)
        if (result.isSuccess) {
            Result.success()
        } else {
            // Let WorkManager retry according to its backoff policy
            Result.retry()
        }
    }
}
