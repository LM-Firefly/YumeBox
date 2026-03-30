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
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.service

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ConnectionSnapshot
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.core.model.ProviderList
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.core.model.UiConfiguration
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.service.remote.IClashManager
import com.github.yumelira.yumebox.service.remote.ILogObserver
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.records.SelectionDao
import com.github.yumelira.yumebox.service.runtime.session.CompiledConfigPipeline
import com.github.yumelira.yumebox.service.runtime.session.SessionRuntimeSpecFactory
import com.github.yumelira.yumebox.service.runtime.util.sendBroadcastSelf
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

class ClashManager(private val context: Context) : IClashManager,
    CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val store = ServiceStore()
    private val compiledConfigPipeline = CompiledConfigPipeline(context)
    private val runtimeSpecFactory = SessionRuntimeSpecFactory(context)
    private val networkSettings = MMKV.mmkvWithID("network_settings", MMKV.MULTI_PROCESS_MODE)
    private var logReceiver: ReceiveChannel<LogMessage>? = null

    override fun queryTunnelState(): TunnelState {
        return Clash.queryTunnelState()
    }

    override fun queryTrafficNow(): Long {
        if (!StatusProvider.serviceRunning) return 0L
        return Clash.queryTrafficNow()
    }

    override fun queryTrafficTotal(): Long {
        if (!StatusProvider.serviceRunning) return 0L
        return Clash.queryTrafficTotal()
    }

    override fun queryConnections(): ConnectionSnapshot {
        return Clash.queryConnections()
    }

    override fun queryProfileProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        return queryProfileProxyGroups(excludeNotSelectable).map(ProxyGroup::name)
    }

    override fun queryProfileProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        if (store.activeProfile == null) return emptyList()
        val spec = when (configuredProxyMode()) {
            ProxyMode.RootTun -> runtimeSpecFactory.createRootTunSpec()
            ProxyMode.Http -> runtimeSpecFactory.createHttpSpec()
            ProxyMode.Tun -> runtimeSpecFactory.createTunSpec()
        }
        return runBlocking(Dispatchers.Default) {
            compiledConfigPipeline.previewGroups(spec, excludeNotSelectable)
        }
    }

    override fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        val groupNames = Clash.queryGroupNames(excludeNotSelectable)
        return groupNames.map { groupName -> Clash.queryGroup(groupName, ProxySort.Default) }
    }

    override fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        return Clash.queryGroupNames(excludeNotSelectable)
    }

    override fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup {
        return Clash.queryGroup(name, proxySort)
    }

    override fun queryConfiguration(): UiConfiguration {
        return Clash.queryConfiguration()
    }

    override fun queryProviders(): ProviderList {
        return ProviderList(Clash.queryProviders())
    }

    override fun patchSelector(group: String, name: String): Boolean {
        return Clash.patchSelector(group, name).also { patched ->
            val current = store.activeProfile ?: return@also

            if (!patched) {
                SelectionDao.remove(current, group)
                return@also
            }

            val patchedGroup = runCatching { Clash.queryGroup(group, ProxySort.Default) }.getOrNull()
            if (patchedGroup?.type == Proxy.Type.Selector) {
                SelectionDao.upsertManualSelection(current, group, name)
            } else {
                SelectionDao.remove(current, group)
            }
        }
    }

    override fun closeConnection(id: String): Boolean {
        return Clash.closeConnection(id)
    }

    override fun closeAllConnections() {
        Clash.closeAllConnections()
    }

    override fun requestStop() {
        runCatching {
            context.sendBroadcastSelf(Intent(Intents.ACTION_CLASH_REQUEST_STOP))
        }

        runCatching {
            context.stopService(Intent(context, TunService::class.java))
            context.stopService(Intent(context, ClashService::class.java))
        }

        runCatching {
            Clash.stopHttp()
            Clash.stopTun()
            Clash.reset()
        }
    }

    override suspend fun healthCheck(group: String) {
        Timber.d("ClashManager healthCheck: group=%s", group)
        return Clash.healthCheck(group).await()
    }

    override suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        Timber.d("ClashManager healthCheckProxy: group=%s proxy=%s", group, proxyName)
        val json = Clash.healthCheckProxy(proxyName).await()
        val obj = kotlinx.serialization.json.Json.parseToJsonElement(json)
        return obj.jsonObject["delay"]?.jsonPrimitive?.int ?: -1
    }

    override suspend fun updateProvider(type: Provider.Type, name: String) {
        return Clash.updateProvider(type, name).await()
    }

    private fun configuredProxyMode(): ProxyMode {
        val raw = networkSettings.decodeString("proxyMode", ProxyMode.Tun.name) ?: ProxyMode.Tun.name
        return runCatching { ProxyMode.valueOf(raw) }.getOrDefault(ProxyMode.Tun)
    }

    override fun setLogObserver(observer: ILogObserver?) {
        synchronized(this) {
            logReceiver?.apply {
                cancel()

                Clash.forceGc()
            }

            if (observer != null) {
                logReceiver = Clash.subscribeLogcat().also { c ->
                    launch {
                        try {
                            while (isActive) {
                                observer.newItem(c.receive())
                            }
                        } catch (_: CancellationException) {

                        } catch (e: Exception) {
                            Log.w("UI crashed", e)
                        } finally {
                            withContext(NonCancellable) {
                                c.cancel()

                                Clash.forceGc()
                            }
                        }
                    }
                }
            }
        }
    }
}
