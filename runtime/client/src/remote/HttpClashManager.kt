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

package com.github.yumelira.yumebox.runtime.client.remote

import com.github.yumelira.yumebox.core.model.ConnectionSnapshot
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.core.model.ProviderList
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.core.model.RemoteBackend
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.core.model.UiConfiguration
import com.github.yumelira.yumebox.core.util.encodeTrafficValue
import com.github.yumelira.yumebox.runtime.api.service.remote.IClashManager
import com.github.yumelira.yumebox.runtime.api.service.remote.ILogObserver
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * [IClashManager] implementation that steers a remote mihomo backend through its
 * RESTful API (https://wiki.metacubex.one/api/) instead of a local core.
 *
 * The active backend (baseUrl + secret) is read fresh from [backendProvider] on
 * every call, so switching the active backend takes effect immediately. Blocking
 * interface methods bridge to suspend Ktor calls via [runBlocking] on the IO
 * dispatcher, mirroring how `RuntimeClashManager` wraps `RootTunController`.
 */
class HttpClashManager(
    private val backendProvider: () -> RemoteBackend?,
) : IClashManager {

    private val json = Json { ignoreUnknownKeys = true }

    private val client: HttpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    private fun requireBackend(): RemoteBackend =
        backendProvider() ?: error("No active remote controller backend")

    /** Builds an absolute URL to [pathSegments] under the active backend's base URL. */
    private fun buildUrl(vararg pathSegments: String, query: Map<String, String> = emptyMap()): String {
        val backend = requireBackend()
        return URLBuilder(backend.normalizedBaseUrl).apply {
            appendPathSegments(*pathSegments)
            query.forEach { (key, value) -> parameters.append(key, value) }
        }.buildString()
    }

    private suspend fun request(
        method: HttpMethod,
        vararg pathSegments: String,
        query: Map<String, String> = emptyMap(),
        body: Any? = null,
    ): HttpResponse {
        val backend = requireBackend()
        val url = buildUrl(*pathSegments, query = query)
        return when (method) {
            HttpMethod.Get ->
                client.get(url) { applyAuth(backend) }
            HttpMethod.Delete ->
                client.delete(url) { applyAuth(backend) }
            HttpMethod.Put ->
                client.put(url) {
                    applyAuth(backend)
                    if (body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }
            else -> error("Unsupported HTTP method: $method")
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuth(backend: RemoteBackend) {
        if (backend.secret.isNotBlank()) {
            header(HttpHeaders.Authorization, "Bearer ${backend.secret}")
        }
    }

    // ---- Tunnel / traffic ------------------------------------------------

    override suspend fun queryTunnelState(): TunnelState = withContext(Dispatchers.IO) {
        val raw = request(HttpMethod.Get, "configs").bodyAsText()
        val configs = json.decodeFromString<RawConfigs>(raw)
        TunnelState(configs.mode)
    }

    override suspend fun queryTrafficNow(): Long = withContext(Dispatchers.IO) {
        val sample = readTrafficSample() ?: return@withContext 0L
        (encodeTrafficValue(sample.up) shl 32) or encodeTrafficValue(sample.down)
    }

    override suspend fun queryTrafficTotal(): Long = withContext(Dispatchers.IO) {
        val sample = readTrafficSample() ?: return@withContext 0L
        (encodeTrafficValue(sample.upTotal) shl 32) or encodeTrafficValue(sample.downTotal)
    }

    /**
     * Reads the FIRST line of the streaming `/traffic` endpoint (one JSON line per second) and
     * closes the stream. `up`/`down` are realtime bytes/second; `upTotal`/`downTotal` cumulative.
     */
    private suspend fun readTrafficSample(): RawTraffic? = runCatching {
        val backend = requireBackend()
        client.prepareGet(buildUrl("traffic")) { applyAuth(backend) }.execute { response ->
            val line = response.bodyAsChannel().readLine()
            line?.let { json.decodeFromString<RawTraffic>(it) }
        }
    }.getOrNull()

    override suspend fun queryConnections(): ConnectionSnapshot = withContext(Dispatchers.IO) {
        fetchConnections()
    }

    private suspend fun fetchConnections(): ConnectionSnapshot {
        val raw = request(HttpMethod.Get, "connections").bodyAsText()
        return json.decodeFromString<ConnectionSnapshot>(raw)
    }

    // ---- Local-profile-only (irrelevant in pure-remote mode) -------------

    override suspend fun queryProfileProxyGroupNames(excludeNotSelectable: Boolean): List<String> = emptyList()

    override suspend fun queryProfileProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> = emptyList()

    override suspend fun queryActiveProfileTunRouteExcludeAddress(): List<String> = emptyList()

    // ---- Proxy groups ----------------------------------------------------

    override suspend fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> =
        withContext(Dispatchers.IO) {
            val nodes = fetchProxies()
            val groups = orderGroups(fetchGroups(), nodes)
            groups
                .filter { !excludeNotSelectable || it.type in Proxy.Type.MANUALLY_SELECTABLE }
                .map { buildGroup(it, nodes, ProxySort.Default) }
        }

    override suspend fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> =
        withContext(Dispatchers.IO) {
            val nodes = fetchProxies()
            orderGroups(fetchGroups(), nodes)
                .filter { !excludeNotSelectable || it.type in Proxy.Type.MANUALLY_SELECTABLE }
                .map { it.name }
        }

    /** Stable group order: index in GLOBAL.all (config order); groups absent from it sort last by name. */
    private fun orderGroups(groups: List<RawProxy>, nodes: Map<String, RawProxy>): List<RawProxy> {
        val canonical = nodes["GLOBAL"]?.all ?: emptyList()
        val indexOf = canonical.withIndex().associate { (i, name) -> name to i }
        return groups.sortedWith(
            compareBy({ indexOf[it.name] ?: Int.MAX_VALUE }, { it.name })
        )
    }

    override suspend fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup =
        withContext(Dispatchers.IO) {
            val nodes = fetchProxies()
            val group = nodes[name]
                ?: return@withContext ProxyGroup(
                    name = name,
                    type = Proxy.Type.Unknown,
                    proxies = emptyList(),
                    now = "",
                )
            buildGroup(group, nodes, proxySort)
        }

    private suspend fun fetchProxies(): Map<String, RawProxy> {
        val raw = request(HttpMethod.Get, "proxies").bodyAsText()
        return json.decodeFromString<RawProxiesResponse>(raw).proxies
    }

    private suspend fun fetchGroups(): List<RawProxy> {
        val raw = request(HttpMethod.Get, "group").bodyAsText()
        return json.decodeFromString<RawGroupResponse>(raw).proxies
    }

    // ---- Selection / connections mutation --------------------------------

    override suspend fun patchSelector(group: String, name: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = request(
                    HttpMethod.Put,
                    "proxies",
                    group,
                    body = SelectBody(name),
                )
                response.status.isSuccess()
            } catch (error: Throwable) {
                false
            }
        }

    override suspend fun patchForceSelector(group: String, name: String): Boolean =
        patchSelector(group, name)

    override suspend fun patchTunnelMode(mode: TunnelState.Mode): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = request(
                    HttpMethod.Put,
                    "configs",
                    body = mapOf("mode" to mode.name.lowercase()),
                )
                response.status.isSuccess()
            } catch (error: Throwable) {
                false
            }
        }

    override suspend fun closeConnection(id: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                request(HttpMethod.Delete, "connections", id).status.isSuccess()
            } catch (error: Throwable) {
                false
            }
        }

    override suspend fun closeAllConnections() {
        withContext(Dispatchers.IO) {
            runCatching { request(HttpMethod.Delete, "connections") }
        }
    }

    // ---- Health checks ---------------------------------------------------

    override suspend fun healthCheck(group: String) {
        // Triggers a group delay test on the backend; the result body is ignored.
        runCatching {
            request(
                HttpMethod.Get,
                "group",
                group,
                "delay",
                query = DELAY_QUERY,
            )
        }
    }

    override suspend fun healthCheckProxy(group: String, proxyName: String): Int =
        try {
            val response = request(
                HttpMethod.Get,
                "proxies",
                proxyName,
                "delay",
                query = DELAY_QUERY,
            )
            if (response.status.isSuccess()) {
                json.decodeFromString<RawDelayResult>(response.bodyAsText()).delay
            } else {
                -1
            }
        } catch (error: Throwable) {
            -1
        }

    // ---- Providers -------------------------------------------------------

    override suspend fun queryProviders(): ProviderList {
        // TODO(M7): map /providers/proxies + /providers/rules → ProviderList.
        return ProviderList(emptyList())
    }

    override suspend fun updateProvider(type: Provider.Type, name: String) {
        val category = if (type == Provider.Type.Proxy) "proxies" else "rules"
        runCatching { request(HttpMethod.Put, "providers", category, name) }
    }

    // ---- Configuration ---------------------------------------------------

    override suspend fun queryConfiguration(): UiConfiguration = UiConfiguration()

    // ---- Lifecycle (no-ops in pure-remote mode) --------------------------

    override fun requestStop() {
        // No-op: we don't own the remote core, so there is nothing to stop.
    }

    override fun setLogObserver(observer: ILogObserver?) {
        // TODO(M7): stream WS /logs. MVP no-op.
    }

    // ---- Adapters --------------------------------------------------------

    private fun RawProxy.toProxy(): Proxy =
        Proxy(
            name = name,
            title = name,
            subtitle = "",
            type = type,
            delay = history.lastOrNull()?.delay ?: 0,
        )

    private fun buildGroup(group: RawProxy, nodes: Map<String, RawProxy>, sort: ProxySort): ProxyGroup {
        val members = group.all.map { memberName ->
            nodes[memberName]?.toProxy()
                ?: Proxy(
                    name = memberName,
                    title = memberName,
                    subtitle = "",
                    type = Proxy.Type.Unknown,
                    delay = 0,
                )
        }
        val sorted = when (sort) {
            ProxySort.Default -> members
            ProxySort.Title -> members.sortedBy { it.name }
            ProxySort.Delay ->
                members.sortedWith(
                    compareBy(
                        { if (it.delay > 0) 0 else 1 },
                        { if (it.delay > 0) it.delay else Int.MAX_VALUE },
                    )
                )
        }
        return ProxyGroup(
            name = group.name,
            type = group.type,
            proxies = sorted,
            now = group.now,
            icon = group.icon,
            hidden = group.hidden,
        )
    }

    // ---- DTOs ------------------------------------------------------------

    @Serializable
    private data class RawProxiesResponse(val proxies: Map<String, RawProxy> = emptyMap())

    @Serializable
    private data class RawGroupResponse(val proxies: List<RawProxy> = emptyList())

    @Serializable
    private data class RawProxy(
        val name: String,
        val type: String,
        val now: String = "",
        val all: List<String> = emptyList(),
        val history: List<RawDelay> = emptyList(),
        val hidden: Boolean = false,
        val icon: String? = null,
        val udp: Boolean = false,
    )

    @Serializable
    private data class RawDelay(val delay: Int = 0)

    @Serializable
    private data class RawDelayResult(val delay: Int = 0)

    @Serializable
    private data class RawConfigs(val mode: TunnelState.Mode = TunnelState.Mode.Rule)

    @Serializable
    private data class RawTraffic(
        val up: Long = 0,
        val down: Long = 0,
        val upTotal: Long = 0,
        val downTotal: Long = 0,
    )

    @Serializable
    private data class SelectBody(val name: String)

    private companion object {
        val DELAY_QUERY = mapOf(
            "url" to "http://www.gstatic.com/generate_204",
            "timeout" to "5000",
        )
    }
}
