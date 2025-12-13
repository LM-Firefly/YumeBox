/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.clash.manager

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import com.github.yumelira.yumebox.clash.cache.GlobalDelayCache
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.repository.SelectionDao
import android.content.Context
import timber.log.Timber
import com.github.yumelira.yumebox.data.model.Selection
import java.util.concurrent.ConcurrentHashMap

class ProxyGroupManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val delayCache: GlobalDelayCache
) {
    private val selectionDao = SelectionDao(context)
    private val proxyGroupStates = ConcurrentHashMap<String, ProxyGroupState>()

    private val _proxyGroupsShared = MutableSharedFlow<List<ProxyGroupInfo>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = _proxyGroupsShared
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    data class ProxyGroupState(var now: String = "", var fixed: String = "", var lastUpdate: Long = 0)

    private fun resolveRecursiveDelay(
        name: String,
        delayMap: Map<String, Int>,
        visited: MutableSet<String> = mutableSetOf()
    ): Int? {
        if (name in visited) return null
        visited.add(name)
        val state = proxyGroupStates[name]
        if (state != null && state.now.isNotEmpty()) {
            val childDelay = resolveRecursiveDelay(state.now, delayMap, visited)
            if (childDelay != null && childDelay > 0) {
                return childDelay
            }
        }
        return delayMap[name]?.takeIf { it > 0 }
    }

    suspend fun refreshProxyGroups(skipCacheClear: Boolean = false, currentProfile: Profile? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val groupNames = Clash.queryGroupNames(excludeNotSelectable = false)

            val rawGroups = groupNames.associateWith { name ->
                val group = Clash.queryGroup(name, ProxySort.Default)
                val state = proxyGroupStates.getOrPut(name) { ProxyGroupState() }
                val previousNow = state.now
                val previousFixed = state.fixed
                val currentNow = group.now
                val currentFixed = group.fixed

                if (currentProfile != null && currentNow.isNotBlank() && currentNow != previousNow) {
                    if (group.type == Proxy.Type.Selector) {
                        selectionDao.setSelected(
                            Selection(currentProfile.id, name, currentNow)
                        )
                    }
                }
                // persist pin state changes
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
                group
            }

            rawGroups.values.flatMap { it.proxies }.forEach { proxy ->
                if (proxy.delay > 0) {
                    delayCache.updateDelay(proxy.name, proxy.delay)
                }
            }

            val cachedDelays = delayCache.getAllValidDelays()
            val mergedDelayMap = cachedDelays.toMutableMap()

            val finalGroups = groupNames.map { name ->
                val group = rawGroups[name]!!
                val enrichedProxies = group.proxies.map { proxy ->
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

                val chainPath = if (group.type.group && group.now.isNotBlank()) {
                    buildProxyChain(name, group.now, rawGroups)
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

            _proxyGroupsShared.emit(finalGroups)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "刷新代理组失败")
            Result.failure(e)
        }
    }

    suspend fun refreshSingleGroupSelection(groupName: String, proxyName: String, fixedProxyName: String? = null) {
        runCatching {
            val currentGroups = proxyGroups.value.toMutableList()
            val groupIndex = currentGroups.indexOfFirst { it.name == groupName }

            if (groupIndex != -1) {
                val preservedProxies = currentGroups[groupIndex].proxies.map { proxy ->
                    val cachedDelay = delayCache.getDelay(proxy.name)
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
                                fixed = newFixed
                            )
                        }
                        buildProxyChain(groupName, proxyName, groupsMap)
                    } else {
                        currentGroup.chainPath
                    }
                )

                currentGroups[groupIndex] = updatedGroup
                _proxyGroupsShared.emit(currentGroups)
            }
        }
    }

    private fun buildProxyChain(
        groupName: String,
        currentNode: String,
        groups: Map<String, ProxyGroup>,
        visited: MutableSet<String> = mutableSetOf()
    ): List<String> {
        if (groupName in visited) return listOf(groupName)
        visited.add(groupName)

        val nextGroup = groups[currentNode] ?: return listOf(groupName, currentNode)
        val groupNow = nextGroup.now.ifBlank { proxyGroupStates[currentNode]?.now }

        return if (!groupNow.isNullOrBlank()) {
            listOf(groupName) + buildProxyChain(currentNode, groupNow, groups, visited)
        } else {
            listOf(groupName, currentNode)
        }
    }

    suspend fun selectProxy(groupName: String, proxyName: String, currentProfile: Profile?): Boolean {
        return try {
            val result = Clash.patchSelector(groupName, proxyName)
            if (result) {
                if (currentProfile != null) {
                    selectionDao.setSelected(
                        Selection(currentProfile.id, groupName, proxyName)
                    )
                }

                proxyGroupStates[groupName]?.now = proxyName

                delay(50)
                refreshSingleGroupSelection(groupName, proxyName)
            }
            result
        } catch (e: Exception) {
            false
        }
    }

    suspend fun refreshDelaysOnly() {
        runCatching {
            val currentGroups = _proxyGroupsShared.replayCache.firstOrNull() ?: return
            val cachedDelays = delayCache.getAllValidDelays()
            val updatedGroups = currentGroups.map { group ->
                val enrichedProxies = group.proxies.map { proxy ->
                    var updatedProxy = cachedDelays[proxy.name]?.takeIf { it > 0 }?.let { proxy.copy(delay = it) } ?: proxy
                    if (updatedProxy.type.group) {
                        proxyGroupStates[proxy.name]?.now?.takeIf { it.isNotEmpty() }?.let { now ->
                            resolveRecursiveDelay(now, cachedDelays)?.let { childDelay ->
                                updatedProxy = updatedProxy.copy(delay = childDelay)
                            }
                        }
                    }
                    updatedProxy
                }
                group.copy(proxies = enrichedProxies)
            }
            _proxyGroupsShared.emit(updatedGroups)
        }.onFailure { e ->
            Timber.e(e, "刷新延迟失败")
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
                }
            }
            // restore pinned (fixed) selections if any
            val pins = selectionDao.getAllPins(profileId)
            if (pins.isNotEmpty()) {
                pins.forEach { (groupName, proxyName) ->
                    runCatching {
                        if (Clash.patchForceSelector(groupName, proxyName)) {
                            proxyGroupStates[groupName]?.now = proxyName
                        }
                    }
                }
            }
            delay(300)
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
                delay(50)
                refreshSingleGroupSelection(groupName, newNow, proxyName)
            }
            result
        } catch (e: Exception) {
            false
        }
    }

    fun getGroupState(groupName: String): ProxyGroupState? = proxyGroupStates[groupName]

    fun clearGroupStates() = proxyGroupStates.clear()
}