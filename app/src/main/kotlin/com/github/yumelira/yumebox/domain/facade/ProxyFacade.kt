package com.github.yumelira.yumebox.domain.facade

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.Selection
import com.github.yumelira.yumebox.data.repository.IpMonitoringState
import com.github.yumelira.yumebox.data.repository.NetworkInfoService
import com.github.yumelira.yumebox.data.repository.ProxyChainResolver
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.repository.SelectionDao
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.data.store.ProxyDisplaySettingsStorage
import com.github.yumelira.yumebox.domain.model.ConnectionsSnapshot
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import com.github.yumelira.yumebox.domain.model.ProxyState
import com.github.yumelira.yumebox.domain.model.RunningMode
import com.github.yumelira.yumebox.domain.model.TrafficData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class ProxyFacade(
    private val clashManager: ClashManager,
    private val proxyConnectionService: ProxyConnectionService,
    private val selectionDao: SelectionDao,
    private val proxyDisplaySettingsStore: ProxyDisplaySettingsStorage,
    private val networkInfoService: NetworkInfoService,
    private val proxyChainResolver: ProxyChainResolver
) {

    val proxyState: StateFlow<ProxyState> = clashManager.proxyState

    val isRunning: StateFlow<Boolean> = clashManager.isRunning

    val currentProfile: StateFlow<Profile?> = clashManager.currentProfile

    val trafficNow: StateFlow<TrafficData> = clashManager.trafficNow

    val trafficTotal: StateFlow<TrafficData> = clashManager.trafficTotal

    suspend fun getTunnelState(): TunnelState? = clashManager.queryTunnelState()

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = clashManager.proxyStateRepository.proxyGroups

    val isSyncing: StateFlow<Boolean> = clashManager.proxyStateRepository.isSyncing

    val runningMode: StateFlow<RunningMode> = clashManager.runningMode

    val logs: Flow<LogMessage> = clashManager.logs

    val connections: Flow<ConnectionsSnapshot> = clashManager.connections

    val sortMode: Preference<ProxySortMode> = proxyDisplaySettingsStore.sortMode
    val displayMode: Preference<ProxyDisplayMode> = proxyDisplaySettingsStore.displayMode
    val proxyMode: Preference<TunnelState.Mode> = proxyDisplaySettingsStore.proxyMode

    suspend fun startProxy(profileId: String, forceTunMode: Boolean? = null): Result<Intent?> {
        return proxyConnectionService.prepareAndStart(profileId, forceTunMode)
    }

    suspend fun startDirectProxy(profileId: String, mode: com.github.yumelira.yumebox.data.model.ProxyMode): Result<Unit> {
        return proxyConnectionService.startDirect(profileId, mode)
    }

    suspend fun stopProxy(): Result<Unit> {
        return proxyConnectionService.stop(runningMode.value)
    }

    suspend fun stopProxy(mode: RunningMode): Result<Unit> {
        return proxyConnectionService.stop(mode)
    }

    suspend fun loadProfile(profile: Profile): Result<Unit> {
        return runCatching { clashManager.loadProfile(profile) }
    }

    suspend fun reloadCurrentProfile(): Result<Unit> {
        return runCatching { clashManager.reloadCurrentProfile() }
    }

    suspend fun selectProxy(groupName: String, proxyName: String): Result<Boolean> {
        val result = clashManager.proxyStateRepository.selectProxy(groupName, proxyName, currentProfile.value)

        if (result.isSuccess && result.getOrNull() == true) {
            val profile = currentProfile.value
            if (profile != null) {
                try {
                    selectionDao.setSelected(Selection(profile.id, groupName, proxyName))
                } catch (e: Exception) {
                    Timber.e(e, "保存节点选择失败")
                }
            }
        }

        return result
    }

    suspend fun forceSelectProxy(groupName: String, proxyName: String): Boolean {
        return clashManager.forceSelectProxy(groupName, proxyName)
    }

    suspend fun healthCheck(groupName: String): Result<Unit> {
        return clashManager.healthCheck(groupName)
    }

    suspend fun healthCheckAll(): Result<Unit> {
        return clashManager.healthCheckAll()
    }

    suspend fun refreshProxyGroups(): Result<Unit> {
        return clashManager.refreshProxyGroups()
    }

    suspend fun testDelay(groupName: String): Result<Unit> {
        return clashManager.proxyStateRepository.testGroupDelay(groupName)
    }

    suspend fun testAllDelay(): Result<Unit> {
        return clashManager.proxyStateRepository.testAllDelay()
    }

    fun getCachedDelay(nodeName: String): Int? {
        return clashManager.proxyStateRepository.getCachedDelay(nodeName)
    }

    fun getResolvedDelay(nodeName: String): Int? {
        return clashManager.proxyStateRepository.getResolvedDelay(nodeName)
    }

    fun findGroup(groupName: String): ProxyGroupInfo? {
        return clashManager.proxyStateRepository.findGroup(groupName)
    }

    fun getCurrentSelection(groupName: String): String? {
        return clashManager.proxyStateRepository.getCurrentSelection(groupName)
    }

    // Connection management (merged from ConnectionsFacade)
    suspend fun closeConnection(id: String): Result<Boolean> {
        return runCatching {
            Clash.closeConnection(id)
        }
    }

    suspend fun closeAllConnections(): Result<Boolean> {
        return runCatching {
            Clash.closeAllConnections()
        }
    }

    // Clash core functions (merged from ClashCoreFacade)
    fun setCustomUserAgent(userAgent: String) {
        Clash.setCustomUserAgent(userAgent)
    }

    fun setSortMode(mode: ProxySortMode) = sortMode.set(mode)

    fun setDisplayMode(mode: ProxyDisplayMode) = displayMode.set(mode)

    // Network monitoring
    fun startIpMonitoring(isRunningFlow: Flow<Boolean>): Flow<IpMonitoringState> {
        return networkInfoService.startIpMonitoring(isRunningFlow)
    }

    fun triggerIpRefresh() {
        networkInfoService.triggerRefresh()
    }

    // Proxy chain resolution
    fun resolveProxyEndNode(startNodeName: String, groups: List<ProxyGroupInfo>): Proxy? {
        return proxyChainResolver.resolveEndNode(startNodeName, groups)
    }

    // Provider management
    suspend fun queryProviders(): Result<List<Provider>> {
        return try {
            Result.success(Clash.queryProviders())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProvider(provider: Provider): Result<Unit> {
        return updateProviderInternal(provider.type, provider.name)
    }

    suspend fun updateAllProviders(providers: List<Provider>): Result<UpdateProvidersResult> {
        if (providers.isEmpty()) return Result.success(UpdateProvidersResult(emptyList()))

        val failed = mutableListOf<String>()
        providers.forEach { provider ->
            val result = updateProviderInternal(provider.type, provider.name)
            if (result.isFailure) {
                failed.add(provider.name)
            }
        }
        return Result.success(UpdateProvidersResult(failed))
    }

    suspend fun uploadProviderFile(
        context: Context,
        provider: Provider,
        uri: Uri,
        maxBytes: Long = MAX_UPLOAD_SIZE_BYTES
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val targetFile = buildTargetFile(provider)
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IllegalStateException("无法读取文件: $uri"))

                inputStream.use { input ->
                    val size = input.available().toLong()
                    if (size > maxBytes) {
                        return@withContext Result.failure(IllegalStateException("文件超过 ${maxBytes / (1024 * 1024)}MB 限制"))
                    }

                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun updateProviderInternal(type: Provider.Type, name: String): Result<Unit> {
        return try {
            Clash.updateProvider(type, name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildTargetFile(provider: Provider): File {
        if (provider.path.isBlank()) {
            throw IllegalStateException("Provider path is empty")
        }
        val targetFile = File(provider.path)
        targetFile.parentFile?.mkdirs()
        return targetFile
    }

    data class UpdateProvidersResult(
        val failedProviders: List<String>
    )

    companion object {
        private const val MAX_UPLOAD_SIZE_BYTES = 50L * 1024 * 1024
    }

}
