package com.github.yumelira.yumebox.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        if (!isNetworkAvailable()) {
            return@withContext Result.retry()
        }
        val result = com.github.yumelira.yumebox.clash.performUpdate(profileId)
        if (result.isSuccess) {
            Result.success()
        } else {
            // Let WorkManager retry according to its backoff policy
            Result.retry()
        }
    }
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
