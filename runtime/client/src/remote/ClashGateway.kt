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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.remote

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ConnectionSnapshot
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.core.model.ProviderList
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.core.model.UiConfiguration
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.runtime.client.root.RootTunController
import com.github.yumelira.yumebox.service.ClashService
import com.github.yumelira.yumebox.service.StatusProvider
import com.github.yumelira.yumebox.service.TunService
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.remote.IClashManager
import com.github.yumelira.yumebox.service.remote.ILogObserver
import com.github.yumelira.yumebox.service.root.RootTunRuntimeRecovery
import com.github.yumelira.yumebox.service.root.RootTunStatusFlow
import com.github.yumelira.yumebox.service.root.rootTunDecode
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.session.CompiledConfigPipeline
import com.github.yumelira.yumebox.service.runtime.session.RuntimeProxyGroupResolver
import com.github.yumelira.yumebox.service.runtime.session.RuntimeSpec
import com.github.yumelira.yumebox.service.runtime.session.SessionRuntimeSpecFactory
import com.github.yumelira.yumebox.service.runtime.util.sendBroadcastSelf
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/**
 * Single gateway for the mihomo control surface. Dispatches between the remote External Controller,
 * the root runtime, and the in-process local core, with the local branch driving [Clash] (and the
 * proxy-group resolver) directly — there is no separate `ClashManager` delegation layer.
 */
class ClashGateway(
    context: Context,
    private val remote: IClashManager,
    private val isRemoteControllerActive: () -> Boolean,
) : IClashManager {
    private val appContext = context.appContextOrSelf
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var rootLogJob: Job? = null
    private var rootLogSeq: Long = 0L

    // Local-core collaborators (formerly ClashManager's fields).
    private val store = ServiceStore()
    private val compiledConfigPipeline = CompiledConfigPipeline(appContext)
    private val proxyGroupResolver = RuntimeProxyGroupResolver(compiledConfigPipeline)
    private val runtimeSpecFactory = SessionRuntimeSpecFactory(appContext)
    private val networkSettings = MMKV.mmkvWithID("network_settings", MMKV.MULTI_PROCESS_MODE)
    private var logReceiver: ReceiveChannel<LogMessage>? = null

    private fun useRemote(): Boolean = isRemoteControllerActive()

    override fun queryTunnelState(): TunnelState =
        dispatch(
            remoteCall = { remote.queryTunnelState() },
            rootCall = { runBlocking { RootTunController.queryTunnelState(appContext) } },
            localCall = { Clash.queryTunnelState() },
        )

    override fun queryTrafficNow(): Long =
        dispatch(
            remoteCall = { remote.queryTrafficNow() },
            rootCall = { runBlocking { RootTunController.queryTrafficNow(appContext) } },
            localCall = {
                if (!StatusProvider.serviceRunning) 0L else Clash.queryTrafficNow()
            },
        )

    override fun queryTrafficTotal(): Long =
        dispatch(
            remoteCall = { remote.queryTrafficTotal() },
            rootCall = { runBlocking { RootTunController.queryTrafficTotal(appContext) } },
            localCall = {
                if (!StatusProvider.serviceRunning) 0L else Clash.queryTrafficTotal()
            },
        )

    override fun queryConnections(): ConnectionSnapshot =
        dispatch(
            remoteCall = { remote.queryConnections() },
            rootCall = { runBlocking { RootTunController.queryConnections(appContext) } },
            localCall = { Clash.queryConnections() },
        )

    override fun queryProfileProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        if (useRemote()) return remote.queryProfileProxyGroupNames(excludeNotSelectable)
        return localQueryProfileProxyGroups(excludeNotSelectable).map(ProxyGroup::name)
    }

    override fun queryProfileProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        if (useRemote()) return remote.queryProfileProxyGroups(excludeNotSelectable)
        return localQueryProfileProxyGroups(excludeNotSelectable)
    }

    override fun queryActiveProfileTunRouteExcludeAddress(): List<String> {
        if (useRemote()) return remote.queryActiveProfileTunRouteExcludeAddress()
        if (store.activeProfile == null) return emptyList()
        val spec = runtimeSpecFactory.createTunSpec()
        return runBlocking(Dispatchers.Default) {
            compiledConfigPipeline.previewTunRouteExcludeAddress(spec)
        }
    }

    override fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> =
        dispatch(
            remoteCall = { remote.queryAllProxyGroups(excludeNotSelectable) },
            rootCall = {
                runBlocking {
                    RootTunController.queryAllProxyGroups(appContext, excludeNotSelectable)
                }
            },
            localCall = {
                runBlocking(Dispatchers.Default) {
                    proxyGroupResolver.resolvedGroups(activeRuntimeSpec(), excludeNotSelectable)
                }
            },
        )

    override fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> =
        dispatch(
            remoteCall = { remote.queryProxyGroupNames(excludeNotSelectable) },
            rootCall = {
                runBlocking {
                    RootTunController.queryProxyGroupNames(appContext, excludeNotSelectable)
                }
            },
            localCall = {
                runBlocking(Dispatchers.Default) {
                    proxyGroupResolver.resolvedGroupNames(activeRuntimeSpec(), excludeNotSelectable)
                }
            },
        )

    override fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup =
        dispatch(
            remoteCall = { remote.queryProxyGroup(name, proxySort) },
            rootCall = {
                runBlocking { RootTunController.queryProxyGroup(appContext, name, proxySort) }
            },
            localCall = { Clash.queryGroup(name, proxySort) },
        )

    override fun queryConfiguration(): UiConfiguration =
        dispatch(
            remoteCall = { remote.queryConfiguration() },
            rootCall = { runBlocking { RootTunController.queryConfiguration(appContext) } },
            localCall = { Clash.queryConfiguration() },
        )

    override fun queryProviders(): ProviderList {
        if (useRemote()) return remote.queryProviders()
        val providers =
            queryWithRuntime(
                rootCall = { runBlocking { RootTunController.queryProviders(appContext) } },
                localCall = { ProviderList(Clash.queryProviders()).toList() },
                fallbackOnRootFailure = false,
            )
        return ProviderList(providers)
    }

    override fun patchSelector(group: String, name: String): Boolean =
        dispatch(
            remoteCall = { remote.patchSelector(group, name) },
            rootCall = { runBlocking { RootTunController.patchSelector(appContext, group, name) } },
            localCall = { Clash.patchSelector(group, name) },
        )

    override fun closeConnection(id: String): Boolean =
        dispatch(
            remoteCall = { remote.closeConnection(id) },
            rootCall = { runBlocking { RootTunController.closeConnection(appContext, id) } },
            localCall = { Clash.closeConnection(id) },
        )

    override fun closeAllConnections() =
        dispatch(
            remoteCall = { remote.closeAllConnections() },
            rootCall = { runBlocking { RootTunController.closeAllConnections(appContext) } },
            localCall = { Clash.closeAllConnections() },
        )

    override suspend fun healthCheck(group: String) =
        dispatchSuspend(
            remoteCall = { remote.healthCheck(group) },
            rootCall = { RootTunController.healthCheck(appContext, group) },
            localCall = {
                Timber.d("ClashManager healthCheck: group=%s", group)
                Clash.healthCheck(group).await()
            },
        )

    override suspend fun healthCheckProxy(group: String, proxyName: String): Int =
        dispatchSuspend(
            remoteCall = { remote.healthCheckProxy(group, proxyName) },
            rootCall = {
                val payload = RootTunController.healthCheckProxy(appContext, group, proxyName)
                val json = kotlinx.serialization.json.Json.parseToJsonElement(payload)
                json.jsonObject["delay"]?.jsonPrimitive?.int ?: -1
            },
            localCall = {
                Timber.d("ClashManager healthCheckProxy: group=%s proxy=%s", group, proxyName)
                val json = Clash.healthCheckProxy(proxyName).await()
                val jsonElement = kotlinx.serialization.json.Json.parseToJsonElement(json)
                jsonElement.jsonObject["delay"]?.jsonPrimitive?.int ?: -1
            },
        )

    override suspend fun updateProvider(type: Provider.Type, name: String) =
        dispatchSuspend(
            remoteCall = { remote.updateProvider(type, name) },
            rootCall = { RootTunController.updateProvider(appContext, type, name) },
            localCall = { Clash.updateProvider(type, name).await() },
        )

    override fun requestStop() =
        dispatch(
            remoteCall = { remote.requestStop() },
            rootCall = { runBlocking { RootTunController.requestStop(appContext) } },
            localCall = {
                runCatching {
                    appContext.sendBroadcastSelf(Intent(Intents.ACTION_CLASH_REQUEST_STOP))
                }

                runCatching {
                    appContext.stopService(Intent(appContext, TunService::class.java))
                    appContext.stopService(Intent(appContext, ClashService::class.java))
                }

                runCatching {
                    Clash.stopHttp()
                    Clash.stopTun()
                    Clash.reset()
                }
                Unit
            },
        )

    override fun setLogObserver(observer: ILogObserver?) {
        if (useRemote()) {
            remote.setLogObserver(observer)
            return
        }
        if (useRootRuntime()) {
            setLocalLogObserver(null)
            rootLogJob?.cancel()
            if (observer == null) {
                rootLogSeq = 0L
                return
            }
            rootLogJob = scope.launch {
                PollingTimers.ticks(PollingTimerSpecs.RuntimeRootLogPolling).collect {
                    runCatching {
                            val chunk = RootTunController.queryRecentLogs(appContext, rootLogSeq)
                            if (chunk.items.isNotEmpty()) {
                                chunk.items.forEach { raw ->
                                    observer.newItem(rootTunDecode<LogMessage>(raw))
                                }
                            }
                            rootLogSeq = chunk.nextSeq
                        }
                        .onFailure { error -> Timber.d(error, "Root runtime log polling skipped") }
                }
            }
        } else {
            rootLogJob?.cancel()
            rootLogSeq = 0L
            setLocalLogObserver(observer)
        }
    }

    /** In-process logcat subscription (formerly `ClashManager.setLogObserver`). */
    private fun setLocalLogObserver(observer: ILogObserver?) {
        synchronized(this) {
            logReceiver?.apply {
                cancel()

                Clash.forceGc()
            }

            if (observer != null) {
                logReceiver =
                    Clash.subscribeLogcat().also { receiver ->
                        scope.launch(Dispatchers.IO) {
                            try {
                                while (isActive) {
                                    observer.newItem(receiver.receive())
                                }
                            } catch (_: CancellationException) {} catch (error: Exception) {
                                Log.w("UI crashed", error)
                            } finally {
                                withContext(NonCancellable) {
                                    receiver.cancel()

                                    Clash.forceGc()
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun localQueryProfileProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        if (store.activeProfile == null) return emptyList()
        val spec =
            when (configuredProxyMode()) {
                ProxyMode.RootTun -> runtimeSpecFactory.createRootTunSpec()
                ProxyMode.Http -> runtimeSpecFactory.createHttpSpec()
                ProxyMode.Tun -> runtimeSpecFactory.createTunSpec()
            }
        return runBlocking(Dispatchers.Default) {
            proxyGroupResolver.resolvedGroups(spec, excludeNotSelectable, enrichLive = false)
        }
    }

    private fun configuredProxyMode(): ProxyMode {
        val raw =
            networkSettings.decodeString("proxyMode", ProxyMode.Tun.name) ?: ProxyMode.Tun.name
        return runCatching { ProxyMode.valueOf(raw) }.getOrDefault(ProxyMode.Tun)
    }

    private fun activeRuntimeSpec(): RuntimeSpec? {
        val activeProfile = store.activeProfile ?: return null
        val spec =
            when (configuredProxyMode()) {
                ProxyMode.RootTun -> runtimeSpecFactory.createRootTunSpec()
                ProxyMode.Http -> runtimeSpecFactory.createHttpSpec()
                ProxyMode.Tun -> runtimeSpecFactory.createTunSpec()
            }
        return spec.takeIf { it.profileUuid == activeProfile.toString() }
    }

    private fun useRootRuntime(): Boolean {
        val status = RootTunStatusFlow.current(appContext)
        return status.state.isActiveOrStopping || status.runtimeReady
    }

    /**
     * Non-suspend dispatch: remote controller wins when active, otherwise route between the root
     * runtime and the local service via [queryWithRuntime]. Mirrors the per-method
     * `if (useRemote()) ... else queryWithRuntime(...)` shape so the public overrides stay terse.
     */
    private inline fun <T> dispatch(
        remoteCall: () -> T,
        rootCall: () -> T,
        localCall: () -> T,
        fallbackOnRootFailure: Boolean = false,
    ): T {
        if (useRemote()) return remoteCall()
        return queryWithRuntime(rootCall, localCall, fallbackOnRootFailure)
    }

    /** Suspend counterpart of [dispatch]; see [queryWithRuntimeSuspend]. */
    private suspend inline fun <T> dispatchSuspend(
        crossinline remoteCall: suspend () -> T,
        crossinline rootCall: suspend () -> T,
        crossinline localCall: suspend () -> T,
        fallbackOnRootFailure: Boolean = false,
    ): T {
        if (useRemote()) return remoteCall()
        return queryWithRuntimeSuspend({ rootCall() }, { localCall() }, fallbackOnRootFailure)
    }

    private inline fun <T> queryWithRuntime(
        rootCall: () -> T,
        localCall: () -> T,
        fallbackOnRootFailure: Boolean = true,
    ): T {
        if (!useRootRuntime()) {
            return localCall()
        }
        return try {
            rootCall()
        } catch (error: Throwable) {
            handleRootRuntimeFailure(error)
            if (fallbackOnRootFailure) localCall() else throw error
        }
    }

    private suspend inline fun <T> queryWithRuntimeSuspend(
        crossinline rootCall: suspend () -> T,
        crossinline localCall: suspend () -> T,
        fallbackOnRootFailure: Boolean = true,
    ): T {
        if (!useRootRuntime()) {
            return localCall()
        }
        return try {
            rootCall()
        } catch (error: Throwable) {
            handleRootRuntimeFailure(error)
            if (fallbackOnRootFailure) localCall() else throw error
        }
    }

    private fun handleRootRuntimeFailure(error: Throwable) {
        if (RootTunRuntimeRecovery.isBinderConnectionFailure(error)) {
            rootLogJob?.cancel()
            rootLogJob = null
            rootLogSeq = 0L
            RootTunRuntimeRecovery.handleBinderGone(
                appContext,
                RootTunRuntimeRecovery.binderFailureReason(error),
            )
            Timber.w(error, "Root runtime binder died")
            return
        }
        Timber.w(error, "Root runtime query failed")
    }
}
