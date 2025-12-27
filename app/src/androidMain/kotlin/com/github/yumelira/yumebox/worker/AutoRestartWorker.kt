package com.github.yumelira.yumebox.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.common.util.ProxyAutoStartHelper
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber

class AutoRestartWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {
    private val appSettingsStorage: AppSettingsStorage by inject()
    private val networkSettingsStorage: NetworkSettingsStorage by inject()
    private val profilesStore: ProfilesStore by inject()
    private val clashManager: ClashManager by inject()
    private val proxyConnectionService: ProxyConnectionService by inject()
    override suspend fun doWork(): Result = withContext(Dispatchers.Main) {
        runCatching {
            ProxyAutoStartHelper.checkAndAutoStart(
                proxyConnectionService = proxyConnectionService,
                appSettingsStorage = appSettingsStorage,
                networkSettingsStorage = networkSettingsStorage,
                profilesStore = profilesStore,
                clashManager = clashManager,
                isBootCompleted = true
            )
        }.onFailure { e ->
            Timber.tag("AutoRestartWorker").e(e, MLang.Service.Message.AutoStartFailed.format(e.message ?: ""))
            return@withContext Result.failure()
        }
        Result.success()
    }
}
