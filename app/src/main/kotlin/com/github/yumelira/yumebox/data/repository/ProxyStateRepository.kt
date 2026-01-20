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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import timber.log.Timber

class ProxyStateRepository(
    private val context: Context,
    private val profileIdProvider: () -> String? = { null },
    private val proxyChainResolver: ProxyChainResolver = ProxyChainResolver()
) {
    private val selectionDao = SelectionDao(context)
    private val lastSavedStates = ConcurrentHashMap<String, String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()
    init {
        scope.launch {
            proxyGroups.collect { groups ->
                val profileId = profileIdProvider() ?: return@collect
                for (i in groups.indices) {
                    val group = groups[i]
                    if (group.type == Proxy.Type.Selector && group.now.isNotEmpty()) {
                        val name = group.name
                        val last = lastSavedStates[name]
                        if (last != group.now) {
                            lastSavedStates[name] = group.now
                            selectionDao.setSelected(Selection(profileId, name, group.now))
                        }
                    }
                }
            }
        }
    }
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val running = AtomicBoolean(false)
    private var syncJob: Job? = null
    private val activeFlag = AtomicBoolean(false)
    private var autoSyncJob: Job? = null
    fun start(intervalMs: Long = 5000L) {
        if (activeFlag.getAndSet(true)) {
            Timber.d("ProxyStateRepository already started")
            return
        }
        Timber.d("ProxyStateRepository starting with interval ${intervalMs}ms")
        autoSyncJob = scope.launch {
            while (isActive && activeFlag.get()) {
                delay(intervalMs)
                val result = syncFromCore()
                if (result.isFailure) {
                    Timber.w("Auto sync failed: ${result.exceptionOrNull()?.message}")
                }
            }
            Timber.d("ProxyStateRepository auto-sync loop ended")
        }
    }
    suspend fun syncOnce() = syncFromCore()
    private val proxyGroupStates = java.util.concurrent.ConcurrentHashMap<String, ProxyGroupState>()
    private val delayMap = java.util.concurrent.ConcurrentHashMap<String, DelayEntry>()
    private val DELAY_TTL_MS = 5 * 60 * 1000L // 5 minutes
    private val SYNC_INTERVAL_MS = 5_000L // background sync interval (ms)
    private val MIN_SYNC_MS = SYNC_INTERVAL_MS
    private val MAX_SYNC_MS = 60_000L
    private var currentSyncInterval = MIN_SYNC_MS
    data class ProxyGroupState(var now: String = "", var fixed: String = "", var lastUpdate: Long = 0)
    private data class DelayEntry(val delay: Int, val updatedAt: Long)
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
    private fun getDelay(name: String): Int? = getAllValidDelays()[name]
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
        if (!visited.add(name)) return null // Cycle detected
        val groupState = groupStates[name]
        if (groupState != null && groupState.now.isNotEmpty()) {
            val childDelay = resolveEffectiveDelay(groupState.now, delayMap, groupStates, visited)
            if (childDelay != null && childDelay > 0) return childDelay
        }
        return delayMap[name]?.takeIf { it > 0 }
    }
    private suspend fun <T> runWithSyncing(action: suspend () -> T): T {
        _isSyncing.value = true
        try {
            return action()
        } finally {
            _isSyncing.value = false
        }
    }
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
    private fun parseTimeToMillis(time: String): Long? {
        return try {
            java.time.Instant.parse(time).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
    private fun extractLatestDelayEntryFromProxy(proxy: Proxy): DelayEntry? {
        var latest: DelayEntry? = null
        proxy.history.forEach { h ->
            val ts = parseTimeToMillis(h.time) ?: System.currentTimeMillis()
            val d = if (h.delay == 0) -1 else h.delay
            val curr = latest
            if (curr == null || ts > curr.updatedAt) latest = DelayEntry(d, ts)
        }
        proxy.extra.values.forEach { state ->
            state.history.forEach { h ->
                val ts = parseTimeToMillis(h.time) ?: System.currentTimeMillis()
                val d = if (h.delay == 0) -1 else h.delay
                val curr = latest
                if (curr == null || ts > curr.updatedAt) latest = DelayEntry(d, ts)
            }
            if (!state.alive && state.history.isEmpty()) {
                val ts = System.currentTimeMillis()
                val curr = latest
                if (curr == null || ts > curr.updatedAt) latest = DelayEntry(-1, ts)
            }
        }
        if (latest == null) {
            if (proxy.delay != 0) {
                return DelayEntry(proxy.delay, System.currentTimeMillis())
            }
            return null
        }
        Timber.d("Latest delay for ${proxy.name}: ${latest.delay} at ${latest.updatedAt}")
        return latest
    }
    private fun updateDelaysFromProxies(proxies: Sequence<Proxy>) {
        val now = System.currentTimeMillis()
        proxies.forEach { proxy ->
            val entry = extractLatestDelayEntryFromProxy(proxy)
            if (entry != null) {
                updateDelay(proxy.name, entry.delay, entry.updatedAt)
            } else if (proxy.delay != 0) {
                updateDelay(proxy.name, proxy.delay, now)
            }
        }
    }
    private fun collectAndMergeDelays(groups: Map<String, ProxyGroup>): Map<String, Int> {
        updateDelaysFromProxies(groups.values.asSequence().flatMap { it.proxies.asSequence() })
        return getAllValidDelays()
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
                updateDelaysFromProxies(group.proxies.asSequence())
                val mergedDelayMap = getAllValidDelays()
                val currentList = _proxyGroups.value
                val groupsMap = currentList.associate {
                    it.name to ProxyGroup(type = it.type, proxies = it.proxies, now = it.now, fixed = it.fixed)
                }.toMutableMap()
                groupsMap[groupName] = group
                val updatedList = currentList.map { info ->
                    if (info.name == groupName) {
                        val enrichedProxies = enrichProxies(group.proxies, mergedDelayMap)
                        val chainPath = if (info.type.group && group.now.isNotBlank()) {
                            proxyChainResolver.buildChainPathFromMap(info.name, group.now, groupsMap)
                        } else {
                            emptyList()
                        }
                        info.copy(
                            proxies = enrichedProxies,
                            now = group.now,
                            fixed = group.fixed,
                            chainPath = chainPath
                        )
                    } else {
                        info.copy(proxies = enrichProxies(info.proxies, mergedDelayMap))
                    }
                }
                _proxyGroups.value = updatedList
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
            val currentList = _proxyGroups.value
            val mergedDelayMap = getAllValidDelays()
            val groupsMap = currentList.associate {
                it.name to ProxyGroup(type = it.type, now = it.now, fixed = it.fixed, proxies = it.proxies)
            }.toMutableMap()
            groupsMap[groupName]?.let {
                val updated = it.copy(now = proxyName, fixed = fixedProxyName ?: it.fixed)
                groupsMap[groupName] = updated
            }
            proxyGroupStates[groupName]?.let {
                it.now = proxyName
                it.fixed = fixedProxyName ?: it.fixed
            }
            val updatedList = currentList.map { info ->
                val enrichedProxies = enrichProxies(info.proxies, mergedDelayMap)
                var newNow = info.now
                var newFixed = info.fixed
                if (info.name == groupName) {
                    newNow = proxyName
                    newFixed = fixedProxyName ?: info.fixed
                }
                val chainPath = if (info.type.group && newNow.isNotBlank()) {
                    proxyChainResolver.buildChainPathFromMap(info.name, newNow, groupsMap)
                } else emptyList()
                info.copy(
                    proxies = enrichedProxies,
                    now = newNow,
                    fixed = newFixed,
                    chainPath = chainPath
                )
            }
            _proxyGroups.value = updatedList
        }.onFailure { e ->
            Timber.w(e, "refreshSingleGroupSelection failed: $groupName -> $proxyName")
        }.also { _isSyncing.value = false }
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

    fun stopAutoSync() {
        if (!activeFlag.getAndSet(false)) return

        autoSyncJob?.cancel()
        autoSyncJob = null
        _proxyGroups.value = emptyList()
        _isSyncing.value = false
    }

    suspend fun syncFromCore(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!activeFlag.get()) {
            Timber.w("ProxyStateRepository syncFromCore called but not active")
            return@withContext Result.failure(IllegalStateException("未启动"))
        }

        _isSyncing.value = true
        try {
            val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)
            Timber.d("Syncing ${groupNames.size} proxy groups")
            
            if (groupNames.isEmpty()) {
                _proxyGroups.value = emptyList()
                return@withContext Result.success(Unit)
            }

            // Query all groups and keep fixed/now/proxies info, then enrich delays and compute chain paths
            val rawGroups = groupNames.associateWith { name ->
                try {
                    Clash.queryGroup(name, ProxySort.Default)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to query group: $name")
                    null
                }
            }
            val validGroups = rawGroups.filterValues { it != null }.mapValues { it.value!! }
            val mergedDelayMap = collectAndMergeDelays(validGroups)
            val finalGroups = groupNames.mapNotNull { name ->
                val group = validGroups[name] ?: return@mapNotNull null
                val enrichedProxies = enrichProxies(group.proxies, mergedDelayMap)
                val chainPath = if (group.type.group && group.now.isNotBlank()) {
                    // build a temporary map of current groups (using now/fixed from fetched groups)
                    val groupsMap = groupNames.associateWith { gName ->
                        val g = validGroups[gName] ?: ProxyGroup(type = Proxy.Type.Selector, proxies = emptyList(), now = "", fixed = "")
                        ProxyGroup(type = g.type, now = g.now, proxies = g.proxies, fixed = g.fixed)
                    }
                    computeChainPath(name, group.now, groupsMap)
                } else {
                    emptyList()
                }
                ProxyGroupInfo(
                    name = name,
                    type = group.type,
                    proxies = enrichedProxies,
                    now = group.now,
                    fixed = group.fixed,
                    chainPath = chainPath,
                    icon = group.icon,
                )
            }
            _proxyGroups.value = finalGroups
            Timber.d("Synced ${finalGroups.size} proxy groups successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sync from core failed")
            Result.failure(Exception("同步失败"))
        } finally {
            _isSyncing.value = false
        }
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

    suspend fun testGroupDelay(groupName: String): Result<Unit> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            com.github.yumelira.yumebox.core.Clash.healthCheck(groupName).await()
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
        return _proxyGroups.value.asSequence()
            .flatMap { it.proxies.asSequence() }
            .firstOrNull { it.name == nodeName }?.delay
    }

    private fun getAllValidDelayEntries(): Map<String, DelayEntry> {
        val now = System.currentTimeMillis()
        return delayMap.entries.filter { now - it.value.updatedAt <= DELAY_TTL_MS }
            .associate { it.key to it.value }
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

    fun stop() {
        running.set(false)
        syncJob?.cancel()
        syncJob = null
        scope.coroutineContext[Job]?.cancelChildren()
        _isSyncing.value = false
    }

    fun close() {
        stop()
        scope.cancel()
    }
}
