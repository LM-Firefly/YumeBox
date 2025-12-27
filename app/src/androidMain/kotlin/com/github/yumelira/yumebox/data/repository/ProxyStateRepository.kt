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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class ProxyStateRepository(
    private val context: Context,
    private val proxyChainResolver: ProxyChainResolver
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val selectionDao = SelectionDao(context)
    private val proxyGroupStates = java.util.concurrent.ConcurrentHashMap<String, ProxyGroupState>()
    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()
    private val delayMap = java.util.concurrent.ConcurrentHashMap<String, DelayEntry>()
    private val DELAY_TTL_MS = 5 * 60 * 1000L // 5 minutes
    private val SYNC_INTERVAL_MS = 5_000L // background sync interval (ms)
    private val MIN_SYNC_MS = SYNC_INTERVAL_MS
    private val MAX_SYNC_MS = 60_000L
    private var currentSyncInterval = MIN_SYNC_MS
    data class ProxyGroupState(var now: String = "", var fixed: String = "", var lastUpdate: Long = 0)

    private data class DelayEntry(val delay: Int, val updatedAt: Long)

    private fun updateDelay(name: String, delay: Int) {
        if (delay > 0) {
            delayMap[name] = DelayEntry(delay, System.currentTimeMillis())
        }
    }

    private fun getAllValidDelays(): Map<String, Int> {
        val now = System.currentTimeMillis()
        return delayMap.entries.filter { now - it.value.updatedAt <= DELAY_TTL_MS }
            .associate { it.key to it.value.delay }
    }

    private fun getDelay(name: String): Int? = getAllValidDelays()[name]

    private fun resolveRecursiveDelay(
        name: String,
        delayMap: Map<String, Int>,
        visited: MutableSet<String> = mutableSetOf()
    ): Int? {
        if (name in visited) return null
        visited.add(name)
        delayMap[name]?.takeIf { it > 0 }?.let { return it }
        val state = proxyGroupStates[name]
        if (state != null && state.now.isNotEmpty()) {
            val childDelay = resolveRecursiveDelay(state.now, delayMap, visited)
            if (childDelay != null && childDelay > 0) {
                return childDelay
            }
        }
        return null
    }

    private fun enrichProxies(proxies: List<Proxy>, mergedDelayMap: Map<String, Int>): List<Proxy> {
        return proxies.map { proxy ->
            val cachedDelay = mergedDelayMap[proxy.name]
            var updatedProxy = if (cachedDelay != null && cachedDelay > 0) {
                proxy.copy(delay = cachedDelay)
            } else {
                proxy
            }
            if (proxy.type.group) {
                proxyGroupStates[proxy.name]?.takeIf { it.now.isNotEmpty() }?.let { state ->
                    updatedProxy = updatedProxy.copy(subtitle = "${proxy.type}(${state.now})")
                    resolveRecursiveDelay(state.now, mergedDelayMap)?.let { childDelay ->
                        updatedProxy = updatedProxy.copy(delay = childDelay)
                    }
                }
            }
            updatedProxy
        }
    }

    // Helper: run a suspend action while setting _isSyncing flag
    private suspend fun <T> runWithSyncing(action: suspend () -> T): T {
        _isSyncing.value = true
        try {
            return action()
        } finally {
            _isSyncing.value = false
        }
    }

    // Helper: apply group state updates and persist selection/pin changes
    private fun applyGroupStateAndPersist(name: String, group: ProxyGroup, currentProfile: Profile?) {
        val state = proxyGroupStates.getOrPut(name) { ProxyGroupState() }
        val previousNow = state.now
        val previousFixed = state.fixed
        val currentNow = group.now
        val currentFixed = group.fixed

        if (currentProfile != null && currentNow.isNotBlank() && currentNow != previousNow && group.type == Proxy.Type.Selector) {
            selectionDao.setSelected(Selection(currentProfile.id, name, currentNow))
        }

        if (currentProfile != null && currentFixed != previousFixed) {
            if (currentFixed.isNotBlank()) {
                selectionDao.setPinned(currentProfile.id, name, currentFixed)
            } else {
                selectionDao.removePinned(currentProfile.id, name)
            }
        }

        state.now = currentNow
        state.fixed = currentFixed
        state.lastUpdate = System.currentTimeMillis()
    }

    // Helper: update delays from provided groups and return merged valid delay map
    private fun collectAndMergeDelays(groups: Map<String, ProxyGroup>): MutableMap<String, Int> {
        groups.values.flatMap { it.proxies }.forEach { proxy ->
            if (proxy.delay > 0) updateDelay(proxy.name, proxy.delay)
        }
        return getAllValidDelays().toMutableMap()
    }

    // Helper: prepare groups (fallback now) and compute chain path
    private fun computeChainPath(name: String, now: String, groups: Map<String, ProxyGroup>): List<String> {
        val prepared = groups.mapValues { (k, v) ->
            if (v.now.isBlank()) {
                val fallback = proxyGroupStates[k]?.now ?: v.now
                v.copy(now = fallback)
            } else v
        }
        return proxyChainResolver.buildChainPathFromMap(name, now, prepared)
    }

    suspend fun refreshProxyGroups(currentProfile: Profile? = null): Result<Unit> = withContext(Dispatchers.IO) {
        runWithSyncing {
            try {
                val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)
                val rawGroups = groupNames.associateWith { name ->
                    val group = Clash.queryGroup(name, ProxySort.Default)
                    applyGroupStateAndPersist(name, group, currentProfile)
                    group
                }

                val mergedDelayMap = collectAndMergeDelays(rawGroups)

                val finalGroups = groupNames.map { name ->
                    val group = rawGroups[name]!!
                    val enrichedProxies = enrichProxies(group.proxies, mergedDelayMap)
                    val chainPath = if (group.type.group && group.now.isNotBlank()) {
                        computeChainPath(name, group.now, rawGroups)
                    } else {
                        emptyList()
                    }
                    ProxyGroupInfo(
                        name = name,
                        type = group.type,
                        proxies = enrichedProxies,
                        now = group.now,
                        fixed = group.fixed,
                        chainPath = chainPath
                    )
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
                applyGroupStateAndPersist(groupName, group, currentProfile)

                val mergedDelayMap = collectAndMergeDelays(mapOf(groupName to group))

                val currentList = _proxyGroups.value
                val updatedList = currentList.map { info ->
                    if (info.name == groupName) {
                        val enrichedProxies = enrichProxies(group.proxies, mergedDelayMap)
                        info.copy(
                            proxies = enrichedProxies,
                            now = group.now,
                            fixed = group.fixed
                        )
                    } else {
                        info
                    }
                }

                val groupsMap = updatedList.associate { it.name to ProxyGroup(it.type, it.proxies, it.now, it.fixed) }
                val finalList = updatedList.map { info ->
                    if (info.name == groupName) {
                        val chainPath = if (info.type.group && info.now.isNotBlank()) {
                            computeChainPath(info.name, info.now, groupsMap)
                        } else {
                            emptyList()
                        }
                        info.copy(chainPath = chainPath)
                    } else {
                        info
                    }
                }
                _proxyGroups.value = finalList
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, MLang.Proxy.Message.QueryGroupFailed.format(groupName))
                Result.failure(e)
            }
        }
    }

    suspend fun refreshSingleGroupSelection(groupName: String, proxyName: String, fixedProxyName: String? = null) {
        _isSyncing.value = true
        runCatching {
            val currentGroups = proxyGroups.value.toMutableList()
            val groupIndex = currentGroups.indexOfFirst { it.name == groupName }
            if (groupIndex != -1) {
                val cachedDelays = getAllValidDelays()
                val preservedProxies = currentGroups[groupIndex].proxies.map { proxy ->
                    val cachedDelay = cachedDelays[proxy.name]
                    if (cachedDelay != null && cachedDelay > 0) {
                        proxy.copy(delay = cachedDelay)
                    } else {
                        proxy
                    }
                }
                val currentGroup = currentGroups[groupIndex]
                val newFixed = fixedProxyName ?: currentGroup.fixed
                val updatedGroup = currentGroup.copy(
                    now = proxyName,
                    fixed = newFixed,
                    proxies = preservedProxies,
                    chainPath = if (currentGroup.type.group) {
                        val groupsMap = currentGroups.associate { it.name to
                            ProxyGroup(
                                type = it.type,
                                now = if (it.name == groupName) proxyName else it.now,
                                proxies = it.proxies,
                                fixed = if (it.name == groupName) newFixed else it.fixed
                            )
                        }
                        computeChainPath(groupName, proxyName, groupsMap)
                    } else {
                        currentGroup.chainPath
                    }
                )
                currentGroups[groupIndex] = updatedGroup
                _proxyGroups.value = currentGroups
            }
        }.onFailure { e ->
            Timber.w(e, "refreshSingleGroupSelection failed: $groupName -> $proxyName")
        }.also { _isSyncing.value = false }
    }

    suspend fun selectProxy(groupName: String, proxyName: String, currentProfile: Profile?): Result<Boolean> {
        return try {
            val group = _proxyGroups.value.find { it.name == groupName }
            if (group == null) {
                Timber.w(MLang.Proxy.Message.GroupNotFound.format(groupName))
                return Result.failure(IllegalArgumentException(MLang.Proxy.Message.GroupNotFound.format(groupName)))
            }
            val success = Clash.patchSelector(groupName, proxyName)
            if (success) {
                if (currentProfile != null) {
                    selectionDao.setSelected(
                        Selection(currentProfile.id, groupName, proxyName)
                    )
                }
                proxyGroupStates[groupName]?.now = proxyName
                delay(50)
                refreshSingleGroupSelection(groupName, proxyName)
            }
            Result.success(success)
        } catch (e: Exception) {
            Timber.e(e, MLang.Proxy.Message.SelectProxyFailed.format(groupName, proxyName))
            Result.failure(e)
        }
    }

    suspend fun refreshDelaysOnly() {
        _isSyncing.value = true
        try {
            val currentGroups = _proxyGroups.value
            val cachedDelays = getAllValidDelays()
            val updatedGroups = currentGroups.map { group ->
                val enrichedProxies = enrichProxies(group.proxies, cachedDelays)
                group.copy(proxies = enrichedProxies)
            }
            _proxyGroups.value = updatedGroups
        } catch (e: Exception) {
            Timber.e(e, "刷新延迟失败")
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun restoreSelections(profileId: String) {
        runCatching {
            val selections = selectionDao.getAllSelections(profileId)
            if (selections.isEmpty()) return
            selections.forEach { (groupName, proxyName) ->
                runCatching {
                    val group = Clash.queryGroup(groupName, ProxySort.Default)
                    if (group.type == Proxy.Type.Selector) {
                        if (Clash.patchSelector(groupName, proxyName)) {
                            proxyGroupStates[groupName]?.now = proxyName
                        }
                    }
                }.onFailure { e ->
                    Timber.w(e, "Failed to restore selection $groupName -> $proxyName")
                }
            }
            // restore pinned (fixed) selections if any
            val pins = selectionDao.getAllPins(profileId)
            if (pins.isNotEmpty()) {
                pins.forEach { (groupName, proxyName) ->
                    runCatching {
                        if (Clash.patchForceSelector(groupName, proxyName)) {
                            proxyGroupStates[groupName]?.apply {
                                fixed = proxyName
                                now = proxyName
                                lastUpdate = System.currentTimeMillis()
                            }
                        }
                    }.onFailure { e ->
                        Timber.w(e, "Failed to restore pinned selection $groupName -> $proxyName")
                    }
                }
            }
            delay(300)
        }.onFailure { e ->
            Timber.w(e, "restoreSelections failed for profileId=$profileId")
        }
    }

    suspend fun forceSelectProxy(groupName: String, proxyName: String, currentProfile: Profile?): Boolean {
        return try {
            val result = Clash.patchForceSelector(groupName, proxyName)
            if (result) {
                if (currentProfile != null && proxyName.isNotBlank()) {
                    selectionDao.setPinned(currentProfile.id, groupName, proxyName)
                } else if (currentProfile != null && proxyName.isBlank()) {
                    selectionDao.removePinned(currentProfile.id, groupName)
                }
                val state = proxyGroupStates[groupName]
                state?.fixed = proxyName
                var newNow = proxyName
                if (proxyName.isBlank()) {
                    val group = Clash.queryGroup(groupName, ProxySort.Default)
                    newNow = group.now
                }
                state?.now = newNow
                state?.lastUpdate = System.currentTimeMillis()
                delay(50)
                refreshSingleGroupSelection(groupName, newNow, proxyName)
            }
            result
        } catch (e: Exception) {
            Timber.w(e, "forceSelectProxy failed: $groupName -> $proxyName")
            false
        }
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val running = AtomicBoolean(false)
    private var syncJob: Job? = null

    fun start() {
        if (running.compareAndSet(false, true)) {
            syncJob?.cancel()
            syncJob = scope.launch {
                var interval = MIN_SYNC_MS
                while (running.get()) {
                    try {
                        val before = _proxyGroups.value.map { "${it.name}:${it.now}" }
                        syncFromCore()
                        val after = _proxyGroups.value.map { "${it.name}:${it.now}" }
                        interval = if (before == after) {
                            (interval * 2).coerceAtMost(MAX_SYNC_MS)
                        } else {
                            MIN_SYNC_MS
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Background sync failed")
                        interval = (interval * 2).coerceAtMost(MAX_SYNC_MS)
                    }
                    delay(interval)
                }
            }
        }
    }

    suspend fun syncFromCore(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            refreshProxyGroups()
        } catch (e: Exception) {
            Timber.e(e, "syncFromCore failed")
            Result.failure(e)
        }
    }

    suspend fun testGroupDelay(groupName: String): Result<Unit> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            val group = findGroup(groupName) ?: return@withContext Result.failure(IllegalArgumentException(MLang.Proxy.Message.GroupNotFound.format(groupName)))
            val deferred = com.github.yumelira.yumebox.core.Clash.healthCheck(groupName)
            withTimeout(15_000) { deferred.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, MLang.Proxy.Message.TestGroupDelayFailed.format(e.message ?: ""))
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun testAllDelay(): Result<Unit> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            com.github.yumelira.yumebox.core.Clash.healthCheckAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, MLang.Proxy.Message.TestAllGroupDelayFailed.format(e.message ?: ""))
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    fun getGroupState(groupName: String): ProxyGroupState? = proxyGroupStates[groupName]

    fun clearGroupStates() = proxyGroupStates.clear()

    fun getCachedDelay(nodeName: String): Int? {
        return _proxyGroups.value
            .flatMap { it.proxies }
            .find { it.name == nodeName }
            ?.delay
    }

    fun findGroup(groupName: String): ProxyGroupInfo? {
        return _proxyGroups.value.find { it.name == groupName }
    }

    fun findProxy(nodeName: String): Proxy? {
        return _proxyGroups.value
            .flatMap { it.proxies }
            .find { it.name == nodeName }
    }

    fun getCurrentSelection(groupName: String): String? {
        return findGroup(groupName)?.now
    }

    fun stop() {
        running.set(false)
        syncJob?.cancel()
        syncJob = null
        scope.coroutineContext[Job]?.cancelChildren()
        _isSyncing.value = false
    }

    fun close() {
        stop()
        scope.cancel("ProxyStateRepository closed")
    }
}
