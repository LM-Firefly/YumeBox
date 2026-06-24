/*
 * This file is part of FlyCat.
 *
 * FlyCat is free software: you can redistribute it and/or modify
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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.runtime.client.internal

import android.content.Context
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimePhase
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeSnapshot
import com.github.yumelira.yumebox.runtime.client.remote.ServiceClient
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles proxy group query, selection, and health check operations.
 * Extracted from ProxyFacade to separate proxy group interaction logic from runtime lifecycle management.
 */
internal class ProxyGroupInteraction(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val snapshotProvider: () -> RuntimeSnapshot,
    private val connectCurrentBackend: suspend () -> Unit,
    private val proxyGroupManager: ProxyGroupManager,
    private val scheduleRefresh: (Long) -> Unit,
    private val refreshGroup: suspend (String, ProxySort) -> Unit,
) {
    private companion object {
        const val PROXY_SELECT_FULL_REFRESH_DELAY_MS = 400L
    }

    suspend fun queryProxyGroupNames(excludeNotSelectable: Boolean = false): List<String> {
        if (snapshotProvider().owner == RuntimeOwner.RootTun) {
            return RootTunController.queryProxyGroupNames(appContext, excludeNotSelectable)
        }
        connectCurrentBackend()
        return ServiceClient.clash().queryProxyGroupNames(excludeNotSelectable)
    }

    suspend fun queryProfileProxyGroups(excludeNotSelectable: Boolean = false): List<ProxyGroup> {
        connectCurrentBackend()
        return ServiceClient.clash().queryProfileProxyGroups(excludeNotSelectable)
    }

    suspend fun queryProxyGroup(name: String, sort: ProxySort = ProxySort.Default): ProxyGroup {
        if (snapshotProvider().owner == RuntimeOwner.RootTun) {
            return RootTunController.queryProxyGroup(appContext, name, sort)
        }
        connectCurrentBackend()
        return ServiceClient.clash().queryProxyGroup(name, sort)
    }

    suspend fun selectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Select proxy: group=$group proxy=$proxyName")
        val ok =
            if (snapshotProvider().owner == RuntimeOwner.RootTun) {
                RootTunController.patchSelector(appContext, group, proxyName)
            } else {
                connectCurrentBackend()
                ServiceClient.clash().patchSelector(group, proxyName)
            }
        if (ok) {
            delay(200L)
            refreshGroup(group, ProxySort.Default)
            scheduleRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
        }
        return ok
    }

    suspend fun forceSelectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Force select proxy: group=$group proxy=$proxyName")
        val ok = if (snapshotProvider().owner == RuntimeOwner.RootTun) {
            RootTunController.patchForceSelector(appContext, group, proxyName)
        } else {
            connectCurrentBackend()
            ServiceClient.clash().patchForceSelector(group, proxyName)
        }
        if (ok) {
            applyLocalForceSelection(group = group, proxyName = proxyName)
            scope.launch {
                runCatching {
                    refreshGroup(group, ProxySort.Default)
                    scheduleRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
                }
            }
        }
        return ok
    }

    suspend fun healthCheck(group: String) {
        Timber.d("Health check request: group=%s", group)
        if (snapshotProvider().owner == RuntimeOwner.RootTun) {
            RootTunController.healthCheck(appContext, group)
        } else {
            connectCurrentBackend()
            ServiceClient.clash().healthCheck(group)
        }
        Timber.d("Health check dispatched: group=%s", group)
        proxyGroupManager.scheduleRuntimeGroupRefresh(scope, group, PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis) { refreshGroup(it, ProxySort.Default) }
        scheduleRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckAll() {
        Timber.d("Health check all request")
        val snapshot = snapshotProvider()
        if (snapshot.owner == RuntimeOwner.RootTun) {
            RootTunController.queryAllProxyGroups(appContext, excludeNotSelectable = false)
                .map { it.name }
                .forEach { groupName ->
                    RootTunController.healthCheck(appContext, groupName)
                    proxyGroupManager.scheduleRuntimeGroupRefresh(
                        scope,
                        groupName,
                        PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis,
                    ) { refreshGroup(it, ProxySort.Default) }
                }
        } else if (snapshot.owner == RuntimeOwner.RemoteController) {
            proxyGroupManager.proxyGroups.value
                .map { it.name }
                .forEach { groupName ->
                    connectCurrentBackend()
                    ServiceClient.clash().healthCheck(groupName)
                    proxyGroupManager.scheduleRuntimeGroupRefresh(
                        scope,
                        groupName,
                        PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis,
                    ) { refreshGroup(it, ProxySort.Default) }
                }
        } else {
            connectCurrentBackend()
            Clash.healthCheckAll()
        }
        scheduleRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        Timber.d("Health check proxy request: group=%s proxy=%s", group, proxyName)
        val delay =
            if (snapshotProvider().owner == RuntimeOwner.RootTun) {
                RootTunController.healthCheckProxy(appContext, group, proxyName).toIntOrNull() ?: 0
            } else {
                connectCurrentBackend()
                ServiceClient.clash().healthCheckProxy(group, proxyName)
            }
        Timber.d("Health check proxy done: group=%s proxy=%s delay=%s", group, proxyName, delay)
        refreshGroup(group, ProxySort.Default)
        scheduleRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
        return delay
    }

    private suspend fun applyLocalForceSelection(group: String, proxyName: String) {
        val desired = proxyName.trim()
        val currentGroups = proxyGroupManager.proxyGroups.value
        if (currentGroups.isEmpty()) return
        val updatedGroups = currentGroups.map { info ->
            if (info.name != group) return@map info
            val nextNow = if (desired.isNotEmpty()) desired else info.now.trim()
            info.copy(now = nextNow, fixed = desired)
        }
        proxyGroupManager.publishProxyGroups(updatedGroups, cacheForPreview = true)
    }
}
