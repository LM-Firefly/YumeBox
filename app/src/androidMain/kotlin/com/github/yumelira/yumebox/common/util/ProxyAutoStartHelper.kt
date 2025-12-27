package com.github.yumelira.yumebox.common.util

import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import timber.log.Timber

object ProxyAutoStartHelper {

    private const val TAG = "ProxyAutoStartHelper"

    suspend fun checkAndAutoStart(
        proxyConnectionService: ProxyConnectionService,
        appSettingsStorage: AppSettingsStorage,
        networkSettingsStorage: NetworkSettingsStorage,
        profilesStore: ProfilesStore,
        clashManager: ClashManager,
        isBootCompleted: Boolean = false
    ) {
        runCatching {
            val automaticRestart = appSettingsStorage.automaticRestart.value
            if (!automaticRestart) {
                return
            }

            if (clashManager.isRunning.value) {
                return
            }

            val profileId = getProfileToStart(profilesStore)
            if (profileId == null) {
                Timber.tag(TAG).e(MLang.Service.Message.AutoStartFailed.format(MLang.ProfilesPage.Message.UnknownProfile))
                return
            }

            if (isBootCompleted) {
                delay(3000)
            }

            val proxyMode = networkSettingsStorage.proxyMode.value

            val result = proxyConnectionService.startDirect(
                profileId = profileId, mode = proxyMode
            )

            if (result.isFailure) {
                Timber.tag(TAG).e(MLang.Service.Message.AutoStartFailed.format(result.exceptionOrNull()?.message ?: ""))
            }
        }.onFailure { e ->
            Timber.tag(TAG).e(e, MLang.Service.Message.AutoStartFailed.format(e.message ?: ""))
        }
    }

    private fun getProfileToStart(profilesStore: ProfilesStore): String? {
        val lastUsedId = profilesStore.lastUsedProfileId
        if (lastUsedId.isNotEmpty()) {
            val lastUsedProfile = profilesStore.getAllProfiles().find { it.id == lastUsedId }
            if (lastUsedProfile != null) {
                return lastUsedId
            }
        }

        val enabledProfile = profilesStore.getAllProfiles().find { it.enabled }
        if (enabledProfile != null) {
            return enabledProfile.id
        }

        val firstProfile = profilesStore.getAllProfiles().firstOrNull()
        if (firstProfile != null) {
            return firstProfile.id
        }

        return null
    }
}
