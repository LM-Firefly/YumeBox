package com.github.yumelira.yumebox.runtime.client.internal

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.runtime.client.RuntimeBackendRouter
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
    private val scope: CoroutineScope,
    private val router: RuntimeBackendRouter,
    private val proxyGroupManager: ProxyGroupManager,
    private val scheduleRefresh: (Long) -> Unit,
    private val refreshGroup: suspend (String, ProxySort) -> Unit,
) {
    private companion object {
        const val PROXY_SELECT_FULL_REFRESH_DELAY_MS = 400L
    }

    suspend fun queryProxyGroupNames(excludeNotSelectable: Boolean = false): List<String> {
        return router.dispatch(
            onRoot = { RootTunController.queryProxyGroupNames(it, excludeNotSelectable) },
            onLocal = { ServiceClient.clash().queryProxyGroupNames(excludeNotSelectable) },
        )
    }

    suspend fun queryProfileProxyGroups(excludeNotSelectable: Boolean = false): List<ProxyGroup> {
        router.ensureLocalConnected()
        return ServiceClient.clash().queryProfileProxyGroups(excludeNotSelectable)
    }

    suspend fun queryProxyGroup(name: String, sort: ProxySort = ProxySort.Default): ProxyGroup {
        return router.dispatch(
            onRoot = { RootTunController.queryProxyGroup(it, name, sort) },
            onLocal = { ServiceClient.clash().queryProxyGroup(name, sort) },
        )
    }

    suspend fun selectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Select proxy: group=$group proxy=$proxyName")
        val ok = router.dispatch(
            onRoot = { RootTunController.patchSelector(it, group, proxyName) },
            onLocal = { ServiceClient.clash().patchSelector(group, proxyName) },
        )
        if (ok) {
            delay(200L)
            refreshGroup(group, ProxySort.Default)
            scheduleRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
        }
        return ok
    }

    suspend fun forceSelectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Force select proxy: group=$group proxy=$proxyName")
        val ok = router.dispatch(
            onRoot = { RootTunController.patchForceSelector(it, group, proxyName) },
            onLocal = { ServiceClient.clash().patchForceSelector(group, proxyName) },
        )
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
        router.dispatch(
            onRoot = { RootTunController.healthCheck(it, group) },
            onLocal = { ServiceClient.clash().healthCheck(group) },
        )
        Timber.d("Health check dispatched: group=%s", group)
        proxyGroupManager.scheduleRuntimeGroupRefresh(scope, group, PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis) { refreshGroup(it, ProxySort.Default) }
        scheduleRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckAll() {
        Timber.d("Health check all request")
        router.dispatch(
            onRoot = { ctx ->
                RootTunController.queryAllProxyGroups(ctx, excludeNotSelectable = false)
                    .map { it.name }
                    .forEach { groupName ->
                        RootTunController.healthCheck(ctx, groupName)
                        proxyGroupManager.scheduleRuntimeGroupRefresh(
                            scope,
                            groupName,
                            PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis,
                        ) { refreshGroup(it, ProxySort.Default) }
                    }
            },
            onRemote = {
                proxyGroupManager.proxyGroups.value
                    .map { it.name }
                    .forEach { groupName ->
                        ServiceClient.clash().healthCheck(groupName)
                        proxyGroupManager.scheduleRuntimeGroupRefresh(
                            scope,
                            groupName,
                            PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis,
                        ) { refreshGroup(it, ProxySort.Default) }
                    }
            },
            onLocal = { Clash.healthCheckAll() },
        )
        scheduleRefresh(PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis)
    }

    suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        Timber.d("Health check proxy request: group=%s proxy=%s", group, proxyName)
        val delay = router.dispatch(
            onRoot = { RootTunController.healthCheckProxy(it, group, proxyName).toIntOrNull() ?: 0 },
            onLocal = { ServiceClient.clash().healthCheckProxy(group, proxyName) },
        )
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
