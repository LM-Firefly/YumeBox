package com.github.yumelira.yumebox.runtime.client.internal

import com.github.yumelira.yumebox.core.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.core.util.ProxyChainResolver
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimePhase
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeSnapshot
import com.github.yumelira.yumebox.runtime.client.ProxyGroupPreviewCache
import com.github.yumelira.yumebox.runtime.client.remote.ServiceClient
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class ProxyGroupManager {
    private data class DelayCacheEntry(
        val delay: Int,
        val updatedAt: Long,
    )
    private companion object {
        const val PROXY_SELECT_FULL_REFRESH_DELAY_MS = 400L
        const val PROXY_DELAY_CACHE_TTL_MS = 5 * 60 * 1000L
    }
    private val previewCache = ProxyGroupPreviewCache()
    private var previewProfile: Profile? = null
    private val proxyChainResolver = ProxyChainResolver()

    fun setPreviewProfile(profile: Profile?) {
        previewProfile = profile
    }
    private val refreshProxyGroupsMutex = Mutex()
    private val proxyDelayCache = java.util.concurrent.ConcurrentHashMap<String, DelayCacheEntry>()
    private var pendingGroupsRefreshJob: Job? = null
    private val pendingGroupRefreshJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private var lastProxyGroupsSummary: String? = null
    private val _proxyGroups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroups.asStateFlow()
    private val _resolvedPrimaryNode = MutableStateFlow<Proxy?>(null)
    val resolvedPrimaryNode: StateFlow<Proxy?> = _resolvedPrimaryNode.asStateFlow()
    suspend fun refreshProxyGroups(
        appContext: android.content.Context,
        snapshot: RuntimeSnapshot,
        isRootSessionActive: () -> Boolean,
        connectCurrentBackend: suspend () -> Unit,
    ) {
        refreshProxyGroupsMutex.withLock {
            var missingLocalRuntime = false
            val groups =
                withContext(Dispatchers.IO) {
                    runCatching {
                            if (!snapshot.running) {
                                return@runCatching queryPreviewProxyGroups(appContext, connectCurrentBackend)
                            }
                            if (snapshot.owner == com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner.RootTun && !isRootSessionActive()) {
                                error("RootTun runtime not ready")
                            }
                            if (snapshot.owner == com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner.RootTun) {
                                RootTunController.queryAllProxyGroups(
                                        context = appContext,
                                        excludeNotSelectable = false,
                                    )
                                    .let(::toProxyGroupInfos)
                            } else {
                                connectCurrentBackend()
                                ServiceClient.clash()
                                    .queryAllProxyGroups(excludeNotSelectable = false)
                                    .let(::toProxyGroupInfos)
                            }
                        }
                        .getOrElse { error ->
                            Timber.e(error, "Failed to refresh proxy groups")
                            missingLocalRuntime = snapshot.owner != com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner.RootTun &&
                                snapshot.owner != com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner.None &&
                                snapshot.owner != com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner.RemoteController
                            null
                        }
                }
            if (groups != null && (groups.isNotEmpty() || snapshot.running)) {
                publishProxyGroups(groups, cacheForPreview = true)
            } else if (!snapshot.running) {
                val cached = previewCache.fallback(
                    phase = snapshot.phase,
                    profile = previewProfile,
                    excludeNotSelectable = false,
                    overrideSignature = "",
                )
                if (!cached.isNullOrEmpty()) {
                    publishProxyGroups(cached, cacheForPreview = false)
                }
            } else if (missingLocalRuntime) {
                _proxyGroups.value = emptyList()
            }
        }
    }
    suspend fun refreshProxyGroup(
        appContext: android.content.Context,
        name: String,
        sort: ProxySort = ProxySort.Default,
        snapshot: RuntimeSnapshot,
        isRootSessionActive: () -> Boolean,
        connectCurrentBackend: suspend () -> Unit,
    ) {
        if (!snapshot.running) {
            if (_proxyGroups.value.isEmpty()) {
                refreshProxyGroups(appContext, snapshot, isRootSessionActive, connectCurrentBackend)
            }
            return
        }
        refreshProxyGroupsMutex.withLock {
            val updatedGroup =
                withContext(Dispatchers.IO) {
                    runCatching {
                            if (snapshot.owner == com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner.RootTun && !isRootSessionActive()) {
                                error("RootTun runtime not ready")
                            }
                            if (snapshot.owner == com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner.RootTun) {
                                toProxyGroupInfo(
                                    RootTunController.queryProxyGroup(appContext, name, sort)
                                )
                            } else {
                                connectCurrentBackend()
                                toProxyGroupInfo(ServiceClient.clash().queryProxyGroup(name, sort))
                            }
                        }
                        .getOrElse { error ->
                            Timber.e(error, "Failed to refresh proxy group: %s", name)
                            null
                        }
                } ?: return
            val updatedGroups = attachChainPaths(updateCachedProxyGroup(updatedGroup))
            publishProxyGroups(updatedGroups, cacheForPreview = true)
        }
    }
    fun publishProxyGroups(groups: List<ProxyGroupInfo>, cacheForPreview: Boolean) {
        val normalizedGroups = enrichProxyGroupDelays(groups)
        val summary = summarizeProxyGroups(normalizedGroups)
        if (summary != lastProxyGroupsSummary) {
            _proxyGroups.value = normalizedGroups
            lastProxyGroupsSummary = summary
        }
        if (cacheForPreview && normalizedGroups.isNotEmpty()) {
            previewProfile?.let { profile ->
                previewCache.store(
                    profile = profile,
                    excludeNotSelectable = false,
                    overrideSignature = "",
                    groups = normalizedGroups,
                )
            }
        }
    }
    fun updateResolvedPrimaryNode(snapshot: RuntimeSnapshot, groups: List<ProxyGroupInfo>) {
        if (snapshot.phase != RuntimePhase.Running || groups.isEmpty()) {
            _resolvedPrimaryNode.value = null
            return
        }
        val mainGroup =
            groups.find { it.name.equals("Proxy", ignoreCase = true) } ?: groups.firstOrNull()
        val targetNode = mainGroup?.now?.trim().orEmpty()
        _resolvedPrimaryNode.value =
            targetNode.takeIf(String::isNotEmpty)?.let { resolveProxyNode(it, groups) }
    }
    fun clearGroups() {
        _proxyGroups.value = emptyList()
        lastProxyGroupsSummary = null
        _resolvedPrimaryNode.value = null
    }
    fun scheduleRuntimeGroupRefresh(
        scope: CoroutineScope,
        groupName: String,
        delayMillis: Long = 0L,
        refreshAction: suspend (String) -> Unit,
    ) {
        if (groupName.isBlank()) return
        pendingGroupRefreshJobs[groupName]?.cancel()
        pendingGroupRefreshJobs[groupName] = scope.launch {
            if (delayMillis > 0L) delay(delayMillis)
            runCatching { refreshAction(groupName) }
                .onFailure { error ->
                    Timber.d(error, "Deferred proxy group refresh skipped: %s", groupName)
                }
            pendingGroupRefreshJobs.remove(groupName)
        }
    }
    fun scheduleRuntimeProxyGroupsRefresh(
        scope: CoroutineScope,
        delayMillis: Long = 0L,
        refreshAction: suspend () -> Unit,
    ) {
        pendingGroupsRefreshJob?.cancel()
        pendingGroupsRefreshJob = scope.launch {
            if (delayMillis > 0L) delay(delayMillis)
            refreshAction()
        }
    }
    private suspend fun queryPreviewProxyGroups(
        appContext: android.content.Context,
        connectCurrentBackend: suspend () -> Unit,
    ): List<ProxyGroupInfo> {
        connectCurrentBackend()
        val groups =
            ServiceClient.clash()
                .queryProfileProxyGroups(excludeNotSelectable = false)
                .let(::toProxyGroupInfos)
        return groups
    }
    private fun enrichProxyGroupDelays(groups: List<ProxyGroupInfo>): List<ProxyGroupInfo> {
        if (groups.isEmpty()) {
            proxyDelayCache.clear()
            return groups
        }
        val now = System.currentTimeMillis()
        groups.asSequence()
            .flatMap { group -> group.proxies.asSequence() }
            .forEach { proxy ->
                if (proxy.delay != 0) {
                    proxyDelayCache[proxy.name] = DelayCacheEntry(delay = proxy.delay, updatedAt = now)
                }
            }
        val validDelayMap = proxyDelayCache.entries
            .filter { (_, entry) -> now - entry.updatedAt <= PROXY_DELAY_CACHE_TTL_MS }
            .associate { (name, entry) -> name to entry.delay }
        if (validDelayMap.isEmpty()) {
            proxyDelayCache.clear()
            return groups
        }
        proxyDelayCache.keys.removeAll { name -> name !in validDelayMap }
        val groupNowMap = groups.associate { group -> group.name to group.now.trim() }
        return groups.map { group ->
            val enrichedProxies = group.proxies.map { proxy ->
                val effectiveDelay = resolveEffectiveDelay(
                    name = proxy.name,
                    delayMap = validDelayMap,
                    groupNowMap = groupNowMap,
                    visited = mutableSetOf(),
                )
                if (effectiveDelay != null && effectiveDelay != proxy.delay) {
                    proxy.copy(delay = effectiveDelay)
                } else {
                    proxy
                }
            }
            group.copy(proxies = enrichedProxies)
        }
    }
    private fun resolveEffectiveDelay(
        name: String,
        delayMap: Map<String, Int>,
        groupNowMap: Map<String, String>,
        visited: MutableSet<String>,
    ): Int? {
        if (!visited.add(name)) return null
        val selectedChild = groupNowMap[name].orEmpty()
        if (selectedChild.isNotEmpty()) {
            val childDelay = resolveEffectiveDelay(
                name = selectedChild,
                delayMap = delayMap,
                groupNowMap = groupNowMap,
                visited = visited,
            )
            if (childDelay != null && childDelay != 0) {
                return childDelay
            }
        }
        return delayMap[name]?.takeIf { it != 0 }
    }
    private fun toProxyGroupInfo(group: ProxyGroup): ProxyGroupInfo {
        return ProxyGroupInfo(
            name = group.name,
            type = group.type,
            proxies = group.proxies,
            now = group.now.trim(),
            icon = group.icon,
            hidden = group.hidden,
            fixed = group.fixed.trim(),
            chainPath = emptyList(),
        )
    }
    private fun toProxyGroupInfos(groups: List<ProxyGroup>): List<ProxyGroupInfo> {
        return attachChainPaths(groups.map(::toProxyGroupInfo))
    }
    private fun attachChainPaths(groups: List<ProxyGroupInfo>): List<ProxyGroupInfo> {
        if (groups.isEmpty()) return groups
        val groupMap = groups.associateBy { it.name }
        return groups.map { group ->
            if (group.type !in Proxy.Type.GROUP_TYPES || group.now.isBlank()) {
                group.copy(chainPath = emptyList())
            } else {
                group.copy(
                    chainPath = proxyChainResolver.buildChainPathFromMap(
                        groupName = group.name,
                        currentNode = group.now,
                        groups = groupMap,
                    ),
                )
            }
        }
    }
    private fun updateCachedProxyGroup(updated: ProxyGroupInfo): List<ProxyGroupInfo> {
        val currentGroups = _proxyGroups.value
        if (currentGroups.isEmpty()) return listOf(updated)
        if (currentGroups.none { it.name == updated.name }) {
            return currentGroups + updated
        }
        return currentGroups.map { group -> if (group.name == updated.name) updated else group }
    }
    private fun summarizeProxyGroups(groups: List<ProxyGroupInfo>): String {
        return groups.joinToString(separator = "\n") { group ->
            buildString {
                append(group.name)
                append('|')
                append(group.type)
                append('|')
                append(group.now)
                append('|')
                append(group.hidden)
                append('|')
                append(group.proxies.size)
                group.proxies.forEach { proxy ->
                    append('|')
                    append(proxy.name)
                    append(':')
                    append(proxy.type)
                    append(':')
                    append(proxy.delay)
                }
            }
        }
    }
    private fun resolveProxyNode(
        nodeName: String,
        groups: List<ProxyGroupInfo>,
        visited: MutableSet<String> = linkedSetOf(),
    ): Proxy? {
        if (!visited.add(nodeName)) {
            return null
        }
        val group = groups.firstOrNull { it.name == nodeName }
        if (group != null) {
            val groupNow = group.now.trim()
            return groupNow
                .takeIf { it.isNotEmpty() }
                ?.let { resolveProxyNode(it, groups, visited) }
        }
        groups.forEach { proxyGroup ->
            val proxy = proxyGroup.proxies.firstOrNull { it.name == nodeName } ?: return@forEach
            if (proxy.type in Proxy.Type.GROUP_TYPES) {
                val nextGroup = groups.firstOrNull { it.name == proxy.name } ?: return null
                val nextNode = nextGroup.now.trim()
                return nextNode
                    .takeIf { it.isNotEmpty() }
                    ?.let { resolveProxyNode(it, groups, visited) }
            }
            return proxy
        }
        return null
    }
}
