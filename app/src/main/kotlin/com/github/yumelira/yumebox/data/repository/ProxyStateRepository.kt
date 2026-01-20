package com.github.yumelira.yumebox.data.repository

import android.content.Context
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.Selection
import com.github.yumelira.yumebox.data.repository.SelectionDao
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import dev.oom_wg.purejoy.mlang.MLang
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class ProxyStateRepository(
    private val context: Context,
    private val selectionDao: SelectionDao,
    private val profileIdProvider: () -> String? = { null },
    private val proxyChainResolver: ProxyChainResolver = ProxyChainResolver()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val proxyGroupStates = ConcurrentHashMap<String, ProxyGroupState>()
    private val delayMap = ConcurrentHashMap<String, DelayEntry>()
    private val stateMutex = Mutex()
    private val activeFlag = AtomicBoolean(false)
    private var autoSyncJob: Job? = null
    private val DELAY_TTL_MS = 5 * 60 * 1000L // 5 minutes
    @Volatile
    private var _currentInterval: Long = 5000L // 默认 5 秒
    private var cleanupJob: Job? = null
    private val CLEANUP_INTERVAL_MS = 5 * 60 * 1000L // 5 分钟清理一次过期数据
    data class ProxyGroupState(var now: String = "", var fixed: String = "", var lastUpdate: Long = 0)
    private data class DelayEntry(val delay: Int, val updatedAt: Long)

    fun start(intervalMs: Long = 5000L) {
        if (activeFlag.getAndSet(true)) {
            Timber.d("ProxyStateRepository already started")
            return
        }
        _currentInterval = intervalMs
        Timber.d("ProxyStateRepository starting with interval ${intervalMs}ms")
        autoSyncJob = scope.launch {
            var currentDelay = intervalMs
            while (isActive && activeFlag.get()) {
                delay(currentDelay)
                currentDelay = _currentInterval
                val result = syncFromCore()
                if (result.isFailure) {
                    Timber.w("Auto sync failed: ${result.exceptionOrNull()?.message}")
                }
            }
            Timber.d("ProxyStateRepository auto-sync loop ended")
        }
        startCleanupTask()
    }

    fun adjustInterval(intervalMs: Long) {
        require(intervalMs > 0) { "Interval must be positive" }
        if (_currentInterval == intervalMs) return
        
        val oldInterval = _currentInterval
        _currentInterval = intervalMs
        Timber.i("ProxyStateRepository interval adjusted: ${oldInterval}ms → ${intervalMs}ms")
    }

    private fun startCleanupTask() {
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive && activeFlag.get()) {
                delay(CLEANUP_INTERVAL_MS)
                try {
                    cleanupExpiredDelays()
                } catch (e: Exception) {
                    Timber.w(e, "Failed to cleanup expired delays")
                }
            }
        }
    }

    private suspend fun cleanupExpiredDelays() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expired = delayMap.filter { (_, entry) ->
            now - entry.updatedAt > DELAY_TTL_MS
        }
        expired.forEach { (key, _) ->
            delayMap.remove(key)
        }
        if (expired.isNotEmpty()) {
            Timber.d("Cleaned up ${expired.size} expired delay entries")
        }
    }

    fun stop() {
        if (!activeFlag.getAndSet(false)) return
        autoSyncJob?.cancel()
        autoSyncJob = null
        cleanupJob?.cancel()
        cleanupJob = null
        _isSyncing.value = false
    }

    fun close() {
        stop()
        scope.cancel()
        proxyGroupStates.clear()
        delayMap.clear()
    }

    suspend fun syncOnce() = syncFromCore()
    suspend fun syncFromCore(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!activeFlag.get()) {
            return@withContext Result.failure(IllegalStateException("Not started"))
        }
        runWithSyncing {
            try {
                val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)
                if (groupNames.isEmpty()) {
                    _proxyGroups.value = emptyList()
                    return@runWithSyncing Result.success(Unit)
                }
                val currentProfileId = profileIdProvider()
                val rawGroupsMap = groupNames.mapNotNull { name ->
                    try {
                        val group = Clash.queryGroup(name, ProxySort.Default)
                        updateAndPersistGroupState(name, group, currentProfileId)
                        name to group
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to query group: $name")
                        null
                    }
                }.toMap()
                val allProxies = rawGroupsMap.values.flatMap { it.proxies }
                updateDelaysFromProxies(allProxies.asSequence())
                val currentDelays = getAllValidDelays()
                val contextGroupsMap: MutableMap<String, ProxyGroup> = rawGroupsMap.toMutableMap()
                val finalGroups = groupNames.mapNotNull { name ->
                    val group = rawGroupsMap[name] ?: return@mapNotNull null
                    buildProxyGroupInfo(name, group, currentDelays, contextGroupsMap)
                }
                _proxyGroups.value = finalGroups
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Sync from core failed")
                Result.failure(e)
            }
        }
    }

    suspend fun refreshProxyGroups(currentProfile: Profile? = null): Result<Unit> = withContext(Dispatchers.IO) {
        runWithSyncing {
            try {
                val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)
                val profileId = currentProfile?.id ?: profileIdProvider()
                val rawGroupsMap = groupNames.associateWith { name ->
                    val group = Clash.queryGroup(name, ProxySort.Default)
                    updateAndPersistGroupState(name, group, profileId)
                    group
                }
                val allProxies = rawGroupsMap.values.flatMap { it.proxies }
                updateDelaysFromProxies(allProxies.asSequence())
                val currentDelays = getAllValidDelays()
                val finalGroups = groupNames.map { name ->
                    val group = rawGroupsMap[name]!!
                    buildProxyGroupInfo(name, group, currentDelays, rawGroupsMap)
                }
                _proxyGroups.value = finalGroups
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, MLang.Proxy.Message.SyncGroupFailed.format(e.message ?: ""))
                Result.failure(e)
            }
        }
    }

    suspend fun refreshGroup(groupName: String, currentProfile: Profile? = null): Result<Unit> = withContext(Dispatchers.IO) {
        runWithSyncing {
            try {
                val group = Clash.queryGroup(groupName, ProxySort.Default)
                val profileId = currentProfile?.id ?: profileIdProvider()
                updateAndPersistGroupState(groupName, group, profileId)
                updateDelaysFromProxies(group.proxies.asSequence())
                val currentDelays = getAllValidDelays()
                val currentInfos = _proxyGroups.value
                val newInfos = currentInfos.map { info ->
                    if (info.name == groupName) {
                        val contextMap = currentInfos.associate { 
                             it.name to ProxyGroup(
                                type = it.type, 
                                proxies = it.proxies, 
                                now = it.now, 
                                fixed = it.fixed,
                                icon = it.icon
                             ) 
                        }.toMutableMap()
                        contextMap[groupName] = group
                        buildProxyGroupInfo(groupName, group, currentDelays, contextMap)
                    } else {
                        info.copy(proxies = enrichProxies(info.proxies, currentDelays))
                    }
                }
                _proxyGroups.value = newInfos
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, MLang.Proxy.Message.QueryGroupFailed.format(groupName))
                Result.failure(e)
            }
        }
    }

    private suspend fun refreshSingleGroupSelectionLocal(groupName: String, proxyName: String, fixedProxyName: String?) {
        val currentInfos = _proxyGroups.value
        val currentDelays = getAllValidDelays()
        val contextMap: MutableMap<String, ProxyGroup> = currentInfos.associate { 
            it.name to ProxyGroup(
                type = it.type, 
                proxies = it.proxies, 
                now = it.now, 
                fixed = it.fixed,
                icon = it.icon
            ) 
        }.toMutableMap()
        contextMap[groupName]?.let {
            contextMap[groupName] = it.copy(now = proxyName, fixed = fixedProxyName ?: it.fixed)
        }
        proxyGroupStates[groupName]?.let {
            it.now = proxyName
            it.fixed = fixedProxyName ?: it.fixed
            it.lastUpdate = System.currentTimeMillis()
        }
        val newInfos = currentInfos.map { info ->
            var now = info.now
            var fixed = info.fixed
            if (info.name == groupName) {
                now = proxyName
                fixed = fixedProxyName ?: info.fixed
            }
            val enrichedProxies = enrichProxies(info.proxies, currentDelays)
            val chainPath = if (info.type.group && now.isNotBlank()) {
                proxyChainResolver.buildChainPathFromMap(info.name, now, contextMap)
            } else emptyList()
            info.copy(
                proxies = enrichedProxies,
                now = now,
                fixed = fixed,
                chainPath = chainPath
            )
        }
        _proxyGroups.value = newInfos
    }

    private fun updateAndPersistGroupState(name: String, group: ProxyGroup, profileId: String?) {
        val state = proxyGroupStates.getOrPut(name) { ProxyGroupState() }
        val nowChanged = group.now.isNotBlank() && group.now != state.now
        val fixedChanged = group.fixed != state.fixed
        if (!nowChanged && !fixedChanged) {
            state.now = group.now
            state.fixed = group.fixed
            return
        }
        state.now = group.now
        state.fixed = group.fixed
        state.lastUpdate = System.currentTimeMillis()
        if (profileId != null) {
            if (nowChanged && group.type == Proxy.Type.Selector) {
                 selectionDao.setSelected(Selection(profileId, name, group.now))
            }
            if (fixedChanged) {
                if (group.fixed.isNotBlank()) {
                    selectionDao.setPinned(profileId, name, group.fixed)
                } else {
                    selectionDao.removePinned(profileId, name)
                }
            }
        }
    }

    suspend fun selectProxy(groupName: String, proxyName: String, currentProfile: Profile?): Result<Boolean> {
        return try {
            val success = Clash.patchSelector(groupName, proxyName)
            if (success) {
                val profileId = currentProfile?.id ?: profileIdProvider()
                if (profileId != null) {
                    withContext(Dispatchers.IO) {
                        selectionDao.setSelected(Selection(profileId, groupName, proxyName))
                    }
                }
                refreshSingleGroupSelectionLocal(groupName, proxyName, null)
            }
            Result.success(success)
        } catch (e: Exception) {
            Timber.e(e, MLang.Proxy.Message.SelectProxyFailed.format(groupName, proxyName))
            Result.failure(e)
        }
    }

    suspend fun forceSelectProxy(groupName: String, proxyName: String, currentProfile: Profile?): Boolean {
        return try {
            val success = Clash.patchForceSelector(groupName, proxyName)
            if (success) {
                val profileId = currentProfile?.id ?: profileIdProvider()
                if (profileId != null) {
                    withContext(Dispatchers.IO) {
                        if (proxyName.isNotBlank()) {
                            selectionDao.setPinned(profileId, groupName, proxyName)
                        } else {
                            selectionDao.removePinned(profileId, groupName)
                        }
                    }
                }
                var newNow = proxyName
                if (proxyName.isBlank()) {
                    val group = Clash.queryGroup(groupName, ProxySort.Default)
                    newNow = group.now
                }
                refreshSingleGroupSelectionLocal(groupName, newNow, proxyName)
            }
            success
        } catch (e: Exception) {
            Timber.w(e, "forceSelectProxy failed: $groupName -> $proxyName")
            false
        }
    }

    suspend fun restoreSelections(profileId: String) {
        withContext(Dispatchers.IO) {
            stateMutex.withLock {
                runCatching {
                    val failedSelectors = mutableListOf<String>()
                    val failedPins = mutableListOf<String>()
                    selectionDao.getAllSelections(profileId).forEach { (groupName, proxyName) ->
                        val restored = runCatching {
                            var success = false
                            for (attempt in 0 until 3) {
                                val group = Clash.queryGroup(groupName, ProxySort.Default)
                                if (group.type != Proxy.Type.Selector) {
                                    success = true
                                    break
                                }
                                if (Clash.patchSelector(groupName, proxyName)) {
                                    val state = proxyGroupStates.getOrPut(groupName) { ProxyGroupState() }
                                    state.now = proxyName
                                    state.lastUpdate = System.currentTimeMillis()
                                    success = true
                                    break
                                }
                                if (attempt < 2) {
                                    delay(120L * (attempt + 1))
                                }
                            }
                            success
                        }.getOrDefault(false)
                        if (!restored) {
                            failedSelectors += "$groupName->$proxyName"
                        }
                    }
                    selectionDao.getAllPins(profileId).forEach { (groupName, proxyName) ->
                        val restored = runCatching {
                            var success = false
                            for (attempt in 0 until 3) {
                                if (Clash.patchForceSelector(groupName, proxyName)) {
                                    val state = proxyGroupStates.getOrPut(groupName) { ProxyGroupState() }
                                    state.fixed = proxyName
                                    state.now = proxyName
                                    state.lastUpdate = System.currentTimeMillis()
                                    success = true
                                    break
                                }
                                if (attempt < 2) {
                                    delay(120L * (attempt + 1))
                                }
                            }
                            success
                        }.getOrDefault(false)
                        if (!restored) {
                            failedPins += "$groupName->$proxyName"
                        }
                    }
                    if (failedSelectors.isNotEmpty() || failedPins.isNotEmpty()) {
                        Timber.w(
                            "restoreSelections partial failure for profileId=%s, selectors=%s, pins=%s",
                            profileId,
                            failedSelectors.joinToString(),
                            failedPins.joinToString()
                        )
                    }
                }.onFailure { e ->
                    Timber.w(e, "restoreSelections failed for profileId=$profileId")
                }
            }
        }
    }

    suspend fun refreshDelaysOnly() {
        runWithSyncing {
            try {
                val currentGroups = _proxyGroups.value
                val cachedDelays = getAllValidDelays()
                val updatedGroups = currentGroups.map { group ->
                    group.copy(proxies = enrichProxies(group.proxies, cachedDelays))
                }
                _proxyGroups.value = updatedGroups
            } catch (e: Exception) {
                Timber.e(e, "refreshDelaysOnly failed")
            }
        }
    }

    suspend fun testGroupDelay(groupName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runWithSyncing {
             try {
                Clash.healthCheck(groupName).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, MLang.Proxy.Message.TestGroupDelayFailed.format(e.message ?: ""))
                Result.failure(e)
            }
        }
    }

    suspend fun testAllDelay(): Result<Unit> = withContext(Dispatchers.IO) {
         runWithSyncing {
            try {
                Clash.healthCheckAll()
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, MLang.Proxy.Message.TestAllGroupDelayFailed.format(e.message ?: ""))
                Result.failure(e)
            }
        }
    }

    private fun buildProxyGroupInfo(
        name: String, 
        group: ProxyGroup, 
        delayMap: Map<String, Int>, 
        allGroups: Map<String, ProxyGroup>
    ): ProxyGroupInfo {
        val enrichedProxies = enrichProxies(group.proxies, delayMap)
        val chainPath = if (group.type.group && group.now.isNotBlank()) {
             computeChainPath(name, group.now, allGroups)
        } else {
            emptyList()
        }
        return ProxyGroupInfo(
            name = name,
            type = group.type,
            proxies = enrichedProxies,
            now = group.now,
            fixed = group.fixed,
            chainPath = chainPath,
            icon = group.icon
        )
    }

    private fun computeChainPath(name: String, now: String, groups: Map<String, ProxyGroup>): List<String> {
        val prepared = groups.mapValues { (k, v) ->
            if (v.now.isBlank()) {
                val fallback = proxyGroupStates[k]?.now ?: v.now
                v.copy(now = fallback)
            } else v
        }
        return proxyChainResolver.buildChainPathFromMap(name, now, prepared)
    }

    private fun enrichProxies(proxies: List<Proxy>, mergedDelayMap: Map<String, Int>): List<Proxy> {
        val statesSnapshot = proxyGroupStates.toMap()
        return proxies.map { proxy ->
            val effectiveDelay = resolveEffectiveDelay(proxy.name, mergedDelayMap, statesSnapshot, mutableSetOf())
            var p = proxy
            if (effectiveDelay != null) {
                p = p.copy(delay = effectiveDelay)
            }
            val isGroup = proxy.type.group || statesSnapshot.containsKey(proxy.name)
            if (isGroup) {
                statesSnapshot[proxy.name]?.now?.takeIf { it.isNotEmpty() }?.let { now ->
                    p = p.copy(subtitle = "${proxy.type}($now)")
                }
            }
            p
        }
    }

    private fun resolveEffectiveDelay(
        name: String,
        delayMap: Map<String, Int>,
        groupStates: Map<String, ProxyGroupState>,
        visited: MutableSet<String>
    ): Int? {
        if (!visited.add(name)) return null
        val groupState = groupStates[name]
        if (groupState != null && groupState.now.isNotEmpty()) {
            val childDelay = resolveEffectiveDelay(groupState.now, delayMap, groupStates, visited)
            if (childDelay != null && childDelay > 0) return childDelay
        }
        return delayMap[name]?.takeIf { it > 0 }
    }

    private fun updateDelay(name: String, delay: Int, updatedAt: Long = System.currentTimeMillis()) {
        if (delay != 0 || delay < 0) {
            delayMap[name] = DelayEntry(delay, updatedAt)
        }
    }

    private fun getAllValidDelays(): Map<String, Int> {
        val now = System.currentTimeMillis()
        return delayMap.entries.filter { now - it.value.updatedAt <= DELAY_TTL_MS }
            .associate { it.key to it.value.delay }
    }

    private fun updateDelaysFromProxies(proxies: Sequence<Proxy>) {
        val now = System.currentTimeMillis()
        proxies.forEach { proxy ->
            val entry = extractLatestDelayEntryFromProxy(proxy)
            if (entry != null) {
                updateDelay(proxy.name, entry.delay, entry.updatedAt)
            } else if (proxy.delay != 0 && proxy.history.isEmpty()) {
                updateDelay(proxy.name, proxy.delay, now)
            }
        }
    }

    private fun extractLatestDelayEntryFromProxy(proxy: Proxy): DelayEntry? {
        var latest: DelayEntry? = null
        val updateLatest = { d: Int, t: Long ->
             val curr = latest
             if (curr == null || t > curr.updatedAt) {
                 latest = DelayEntry(if (d == 0) -1 else d, t)
             }
        }
        proxy.history.forEach { h -> 
            parseTimeToMillis(h.time)?.let { t ->
                updateLatest(h.delay, t)
            }
        }
        proxy.extra.values.forEach { state ->
            state.history.forEach { h ->
                parseTimeToMillis(h.time)?.let { t ->
                    updateLatest(h.delay, t)
                }
            }
             if (!state.alive && state.history.isEmpty()) {
                 updateLatest(-1, System.currentTimeMillis())
             }
        }
        if (latest == null) {
            if (proxy.delay != 0 && proxy.history.isEmpty()) {
                 return DelayEntry(proxy.delay, System.currentTimeMillis())
            }
            return null
        }
        return latest
    }

    private fun parseTimeToMillis(time: String): Long? {
        return try {
            java.time.Instant.parse(time).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun <T> runWithSyncing(action: suspend () -> T): T {
        return stateMutex.withLock {
            _isSyncing.value = true
            try {
                action()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun getGroupState(groupName: String): ProxyGroupState? = proxyGroupStates[groupName]
    fun getCachedDelay(nodeName: String): Int? {
        return _proxyGroups.value.asSequence()
            .flatMap { it.proxies.asSequence() }
            .firstOrNull { it.name == nodeName }?.delay
    }

    fun getResolvedDelay(nodeName: String): Int? {
        return resolveEffectiveDelay(
            name = nodeName,
            delayMap = getAllValidDelays(),
            groupStates = proxyGroupStates.toMap(),
            visited = mutableSetOf()
        )
    }

    fun findGroup(groupName: String): ProxyGroupInfo? = _proxyGroups.value.find { it.name == groupName }
    fun findProxy(nodeName: String): Proxy? {
        return _proxyGroups.value.asSequence()
            .flatMap { it.proxies.asSequence() }
            .firstOrNull { it.name == nodeName }
    }

    fun getCurrentSelection(groupName: String): String? = findGroup(groupName)?.now
}
