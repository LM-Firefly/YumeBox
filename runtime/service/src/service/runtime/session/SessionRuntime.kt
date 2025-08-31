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



package com.github.yumelira.yumebox.service.runtime.session

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.service.ServiceNetworkObserver
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.runtime.records.SelectionDao
import com.github.yumelira.yumebox.service.runtime.records.SelectionRestoreExecutor
import com.github.yumelira.yumebox.service.runtime.records.SelectionRestoreScope
import com.github.yumelira.yumebox.service.runtime.state.RuntimeOwner
import com.github.yumelira.yumebox.service.runtime.state.RuntimePhase
import com.github.yumelira.yumebox.service.runtime.state.RuntimeSnapshot
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.serializer
import timber.log.Timber
import java.io.File
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayDeque
import kotlin.math.min
import com.github.yumelira.yumebox.core.domain.ConnectionHistoryManager
import java.security.MessageDigest

class SessionRuntime(
    private val host: RuntimeHost,
    private val transport: RuntimeTransport,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val compiledConfigPipeline = CompiledConfigPipeline(host.context.appContextOrSelf)
    private val lock = Any()
    private var currentSpec: RuntimeSpec? = null
    private var currentSnapshot: RuntimeSnapshot = RuntimeSnapshot(targetMode = host.mode)
    private var networkObserver: ServiceNetworkObserver? = null
    private var logJob: Job? = null
    private var connectionTrackingJob: Job? = null
    private var runtimeSnapshot: RuntimeQuerySnapshot = RuntimeQuerySnapshot()
    private val logSeq = AtomicLong(0L)
    private val recentLogs = ArrayDeque<Pair<Long, String>>()
    private var localLogObserver: ((LogMessage) -> Unit)? = null

    fun start(spec: RuntimeSpec): RuntimeOperationResult {
        return synchronized(lock) {
            runCatching {
                stopInternal(reason = null, notifyHost = false)
                startupLog(spec, "session: start begin")
                startInternal(spec)
                RuntimeOperationResult(success = true)
            }.getOrElse { error ->
                rollback(spec, error.message ?: "start runtime failed")
                RuntimeOperationResult(success = false, error = error.message ?: "start runtime failed")
            }
        }
    }

    fun reload(spec: RuntimeSpec): RuntimeOperationResult {
        return synchronized(lock) {
            runCatching {
                startupLog(spec, "session: reload begin")
                reloadInternal(spec)
                RuntimeOperationResult(success = true)
            }.getOrElse { error ->
                startupLog(spec, "failed=${error.message ?: "reload runtime failed"}")
                RuntimeOperationResult(success = false, error = error.message ?: "reload runtime failed")
            }
        }
    }

    fun restart(spec: RuntimeSpec): RuntimeOperationResult {
        return synchronized(lock) {
            runCatching {
                stopInternal(reason = null, notifyHost = false)
                startupLog(spec, "session: restart begin")
                startInternal(spec)
                RuntimeOperationResult(success = true)
            }.getOrElse { error ->
                rollback(spec, error.message ?: "restart runtime failed")
                RuntimeOperationResult(success = false, error = error.message ?: "restart runtime failed")
            }
        }
    }

    fun stop(reason: String? = null): RuntimeOperationResult {
        return synchronized(lock) {
            runCatching {
                stopInternal(reason = reason, notifyHost = true)
                RuntimeOperationResult(success = true)
            }.getOrElse { error ->
                RuntimeOperationResult(success = false, error = error.message ?: "stop runtime failed")
            }
        }
    }

    fun destroy() {
        synchronized(lock) {
            runCatching { stopInternal(reason = "runtime destroyed", notifyHost = false) }
        }
        scope.cancel()
    }

    fun snapshot(): RuntimeSnapshot = currentSnapshot

    fun queryTunnelState(): TunnelState {
        return if (currentSnapshot.phase == RuntimePhase.Running) Clash.queryTunnelState() else TunnelState(TunnelState.Mode.Rule)
    }

    fun queryTrafficNow(): Long {
        return if (currentSnapshot.phase == RuntimePhase.Running) {
            Clash.queryTrafficNow().also {
                runtimeSnapshot = runtimeSnapshot.copy(trafficNow = it)
                publishSnapshot(currentSnapshot.copy(trafficReady = true))
            }
        } else {
            0L
        }
    }

    fun queryTrafficTotal(): Long {
        return if (currentSnapshot.phase == RuntimePhase.Running) {
            Clash.queryTrafficTotal().also {
                runtimeSnapshot = runtimeSnapshot.copy(trafficTotal = it)
                publishSnapshot(currentSnapshot.copy(trafficReady = true))
            }
        } else {
            0L
        }
    }

    fun queryConnections(): ConnectionSnapshot {
        if (currentSnapshot.phase != RuntimePhase.Running) return ConnectionSnapshot()
        return Clash.queryConnections()
    }

    fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> {
        if (currentSnapshot.phase != RuntimePhase.Running) return emptyList()
        val groups = runCatching {
            Clash.queryGroupNames(excludeNotSelectable).map { Clash.queryGroup(it, ProxySort.Default) }
        }.getOrElse {
            if (excludeNotSelectable) {
                val selectable = Clash.queryGroupNames(true).toSet()
                ensureRuntimeSnapshot().proxyGroups.filter { selectable.contains(it.name) }
            } else {
                ensureRuntimeSnapshot().proxyGroups
            }
        }
        runtimeSnapshot = runtimeSnapshot.copy(proxyGroups = groups)
        publishSnapshot(currentSnapshot.copy(groupsReady = groups.isNotEmpty()))
        return groups
    }

    fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> {
        if (currentSnapshot.phase != RuntimePhase.Running) return emptyList()
        return runCatching { Clash.queryGroupNames(excludeNotSelectable) }
            .getOrElse { queryAllProxyGroups(excludeNotSelectable).map { it.name } }
    }

    fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup {
        if (currentSnapshot.phase != RuntimePhase.Running) {
            error("runtime not running")
        }
        val group = Clash.queryGroup(name, proxySort)
        if (proxySort == ProxySort.Default && group.name.isNotBlank()) {
            val groups = runtimeSnapshot.proxyGroups
            runtimeSnapshot = runtimeSnapshot.copy(
                proxyGroups = if (groups.any { it.name == name }) {
                    groups.map { if (it.name == name) group else it }
                } else {
                    groups + group
                },
            )
        }
        return group
    }

    fun queryConfiguration(): UiConfiguration {
        if (currentSnapshot.phase != RuntimePhase.Running) return UiConfiguration()
        return ensureRuntimeSnapshot().configuration
    }

    fun queryProviders(): List<Provider> {
        if (currentSnapshot.phase != RuntimePhase.Running) return emptyList()
        return ensureRuntimeSnapshot().providers
    }

    fun patchSelector(group: String, name: String): Boolean {
        return Clash.patchSelector(group, name).also { patched ->
            if (patched && currentSnapshot.phase == RuntimePhase.Running) {
                refreshRuntimeSnapshot()
            }
        }
    }

    fun closeConnection(id: String): Boolean {
        if (currentSnapshot.phase != RuntimePhase.Running) return false
        return Clash.closeConnection(id)
    }

    fun closeAllConnections() {
        if (currentSnapshot.phase != RuntimePhase.Running) return
        Clash.closeAllConnections()
    }

    fun healthCheck(group: String): String? {
        Timber.d("SessionRuntime healthCheck: group=%s phase=%s owner=%s", group, currentSnapshot.phase, currentSnapshot.owner)
        return runCatching {
            runBlocking { Clash.healthCheck(group).await() }
            refreshRuntimeSnapshot()
            null
        }.getOrElse { it.message ?: "health check failed" }
    }

    fun healthCheckProxy(proxyName: String): String {
        Timber.d("SessionRuntime healthCheckProxy: proxy=%s phase=%s owner=%s", proxyName, currentSnapshot.phase, currentSnapshot.owner)
        return runCatching {
            runBlocking { Clash.healthCheckProxy(proxyName).await() }.also {
                refreshRuntimeSnapshot()
            }
        }.getOrElse {
            """{"delay":-1,"error":${com.github.yumelira.yumebox.service.root.RootTunJson.Default.encodeToString(String.serializer(), it.message ?: "health check proxy failed")}}"""
        }
    }

    fun updateProvider(type: String, name: String): String? {
        val providerType = runCatching { Provider.Type.valueOf(type) }.getOrElse {
            return "invalid provider type: $type"
        }
        return runCatching {
            runBlocking { Clash.updateProvider(providerType, name).await() }
            refreshRuntimeSnapshot()
            null
        }.getOrElse { it.message ?: "update provider failed" }
    }

    fun setLogObserver(observer: ((LogMessage) -> Unit)?) {
        localLogObserver = observer
    }

    fun queryRecentLogsJson(sinceSeq: Long): RuntimeLogChunk {
        synchronized(recentLogs) {
            val items = recentLogs
                .filter { it.first > sinceSeq }
                .map { it.second }
            return RuntimeLogChunk(
                nextSeq = logSeq.get(),
                items = items,
            )
        }
    }

    private fun startInternal(spec: RuntimeSpec) {
        val startedAt = System.currentTimeMillis()
        currentSpec = spec
        publishSnapshot(
            RuntimeSnapshot(
                owner = spec.owner,
                phase = RuntimePhase.Starting,
                targetMode = host.mode,
                profileUuid = spec.profileUuid,
                profileName = spec.profileName,
                profileReady = true,
                startedAt = startedAt,
                effectiveFingerprint = spec.effectiveFingerprint,
            ),
        )
        host.onStarting(spec)

        teardownCore()
        compileAndLoad(spec)
        startObservers()
        notifyCurrentTimeZone()
        startConnectionTracking()

        transport.prepare(spec)
        transport.start(spec)
        awaitProxyGroupsReady(spec)
        restoreSelections(spec)
        startLogStream()
        startupLog(spec, "snapshot refresh: begin")
        refreshRuntimeSnapshot()
        startupLog(spec, "snapshot refresh: done")

        publishSnapshot(
            currentSnapshot.copy(
                phase = RuntimePhase.Running,
                profileReady = true,
                groupsReady = runtimeSnapshot.proxyGroups.isNotEmpty(),
                trafficReady = true,
                configReady = true,
                transportReady = true,
                logReady = logJob?.isActive == true,
                startedAt = startedAt,
                effectiveFingerprint = spec.effectiveFingerprint,
            ),
        )
        host.onProfileLoaded(spec.profileUuid)
        host.onStarted(spec)
        startupLog(spec, "started")
    }

    private fun reloadInternal(spec: RuntimeSpec) {
        check(currentSpec != null) { "runtime not started" }
        publishSnapshot(
            currentSnapshot.copy(
                phase = RuntimePhase.Starting,
                profileUuid = spec.profileUuid,
                profileName = spec.profileName,
                effectiveFingerprint = spec.effectiveFingerprint,
                groupsReady = false,
                trafficReady = false,
            ),
        )

        compileAndLoad(spec)
        awaitProxyGroupsReady(spec)
        restoreSelections(spec)
        currentSpec = spec
        startupLog(spec, "snapshot refresh: begin")
        refreshRuntimeSnapshot()
        startupLog(spec, "snapshot refresh: done")
        publishSnapshot(
            currentSnapshot.copy(
                phase = RuntimePhase.Running,
                profileReady = true,
                groupsReady = runtimeSnapshot.proxyGroups.isNotEmpty(),
                trafficReady = true,
                configReady = true,
                transportReady = true,
                logReady = logJob?.isActive == true,
                effectiveFingerprint = spec.effectiveFingerprint,
                lastError = null,
            ),
        )
        host.onProfileLoaded(spec.profileUuid)
        startupLog(spec, "reload done")
    }

    private fun stopInternal(reason: String?, notifyHost: Boolean) {
        if (currentSnapshot.phase == RuntimePhase.Idle && currentSpec == null) {
            return
        }

        publishSnapshot(
            currentSnapshot.copy(
                phase = if (currentSnapshot.phase == RuntimePhase.Idle) RuntimePhase.Idle else RuntimePhase.Stopping,
                transportReady = false,
                groupsReady = false,
                trafficReady = false,
                configReady = false,
                logReady = false,
                lastError = reason,
            ),
        )
        stopLogStream()
        stopConnectionTracking()
        stopObservers()
        runCatching { transport.stop() }
        teardownCore()
        currentSpec = null
        runtimeSnapshot = RuntimeQuerySnapshot()
        publishSnapshot(
            RuntimeSnapshot(
                owner = RuntimeOwner.None,
                phase = if (reason.isNullOrBlank()) RuntimePhase.Idle else RuntimePhase.Failed,
                targetMode = host.mode,
                lastError = reason,
            ),
        )
        if (notifyHost) {
            host.onStopped(reason)
        }
    }

    private fun rollback(spec: RuntimeSpec, reason: String) {
        stopLogStream()
        stopObservers()
        runCatching { transport.stop() }
        teardownCore()
        currentSpec = null
        runtimeSnapshot = RuntimeQuerySnapshot()
        publishSnapshot(
            RuntimeSnapshot(
                owner = spec.owner,
                phase = RuntimePhase.Failed,
                targetMode = host.mode,
                profileUuid = spec.profileUuid,
                profileName = spec.profileName,
                profileReady = false,
                lastError = reason,
                effectiveFingerprint = spec.effectiveFingerprint,
            ),
        )
        startupLog(spec, "failed=$reason")
        host.reportFailure(reason)
    }

    private fun compileAndLoad(spec: RuntimeSpec) {
        startupLog(spec, "runtime override: begin apply overrides -> runtime.yaml path=${spec.runtimeConfigPath}")
        startupLog(
            spec,
            "runtime override: overridePaths=${spec.overridePaths.size} " +
                spec.overridePaths.joinToString(prefix = "[", postfix = "]"),
        )
        runBlocking { compiledConfigPipeline.applyOverrideToRuntimeFile(spec) }
        startupLog(spec, "runtime override: done ${describeFile(File(spec.runtimeConfigPath))}")
        startupLog(spec, "runtime load: loadCompiledConfig(${spec.runtimeConfigPath}) begin")
        runBlocking { Clash.loadCompiledConfig(File(spec.runtimeConfigPath)).await() }
        startupLog(spec, "runtime load: loadCompiledConfig done")
    }

    private fun awaitProxyGroupsReady(spec: RuntimeSpec) {
        val expectedGroups = readExpectedGroupNames(spec)
        startupLog(
            spec,
            "runtime verify: expectedGroups=${expectedGroups.size}" +
                expectedGroups.takeIf { it.isNotEmpty() }
                    ?.let { " sample=${it.take(5)}" }
                    .orEmpty(),
        )
        if (expectedGroups.isEmpty()) {
            return
        }

        repeat(PROXY_GROUP_READY_RETRY_COUNT) { attempt ->
            val names = runCatching { Clash.queryGroupNames(false) }.getOrDefault(emptyList())
            if (names.isNotEmpty()) {
                startupLog(spec, "runtime verify: actualGroups=${names.size} sample=${names.take(5)}")
                return
            }
            if (attempt < PROXY_GROUP_READY_RETRY_COUNT - 1) {
                startupLog(spec, "runtime verify: actualGroups=0 retry=${attempt + 1}")
                runBlocking {
                    delay(PROXY_GROUP_READY_RETRY_DELAY_MS)
                }
            }
        }

        error(
            "runtime loaded but exposed 0 proxy groups; expected=${expectedGroups.size} " +
                "sample=${expectedGroups.take(min(5, expectedGroups.size))}",
        )
    }

    private fun readExpectedGroupNames(spec: RuntimeSpec): List<String> {
        val runtimeFile = File(spec.runtimeConfigPath)
        if (!runtimeFile.exists()) {
            startupLog(spec, "runtime verify: runtime.yaml missing path=${runtimeFile.absolutePath}")
            return emptyList()
        }
        val yamlText = runtimeFile.readText()
        if (yamlText.isBlank()) {
            startupLog(spec, "runtime verify: runtime.yaml empty")
            return emptyList()
        }
        return runCatching {
            Clash.inspectCompiledGroups(yamlText, File(spec.profileDir), excludeNotSelectable = false)
                .map { it.name }
                .filter { it.isNotBlank() }
        }.getOrElse { error ->
            startupLog(spec, "runtime verify: inspect failed=${error.message}")
            emptyList()
        }
    }

    private fun restoreSelections(spec: RuntimeSpec) {
        val profileUuid = UUID.fromString(spec.profileUuid)
        val scopeKey = when (spec.owner) {
            RuntimeOwner.RootTun -> SelectionRestoreScope.rootScopeKey(spec.profileUuid)
            else -> SelectionRestoreScope.localScopeKey(profileUuid)
        }
        val restoreResult = SelectionDao.querySelectionsForRestore(
            profileUUID = profileUuid,
            currentScopeKey = scopeKey,
        )
        SelectionRestoreExecutor.restore(
            profileUuid = profileUuid,
            selections = restoreResult.selections,
            tag = spec.owner.name,
        )
    }

    private fun startObservers() {
        if (networkObserver == null) {
            networkObserver = ServiceNetworkObserver(host.context.appContextOrSelf) {
                transport.onNetworkChanged()
            }.also { it.start() }
        }
    }

    private fun stopObservers() {
        runCatching { networkObserver?.stop() }
        networkObserver = null
    }

    private fun notifyCurrentTimeZone() {
        val timeZone = TimeZone.getDefault()
        Clash.notifyTimeZoneChanged(timeZone.id, timeZone.rawOffset / 1000)
    }

    private fun teardownCore() {
        runCatching { Clash.stopRootTun() }
        runCatching { Clash.stopTun() }
        runCatching { Clash.stopHttp() }
        runCatching { Clash.reset() }
    }

    private fun refreshRuntimeSnapshot() {
        if (currentSnapshot.phase != RuntimePhase.Running && currentSnapshot.phase != RuntimePhase.Starting) {
            runtimeSnapshot = RuntimeQuerySnapshot()
            return
        }

        val configuration = runCatching { Clash.queryConfiguration() }.getOrDefault(UiConfiguration())
        val providers = runCatching { Clash.queryProviders() }.getOrDefault(emptyList())
        val proxyGroups = runCatching {
            Clash.queryGroupNames(false).map { Clash.queryGroup(it, ProxySort.Default) }
        }.getOrDefault(emptyList())
        val trafficNow = runCatching { Clash.queryTrafficNow() }.getOrDefault(0L)
        val trafficTotal = runCatching { Clash.queryTrafficTotal() }.getOrDefault(0L)
        runtimeSnapshot = RuntimeQuerySnapshot(
            configuration = configuration,
            providers = providers,
            proxyGroups = proxyGroups,
            trafficNow = trafficNow,
            trafficTotal = trafficTotal,
        )
    }

    private fun ensureRuntimeSnapshot(): RuntimeQuerySnapshot {
        if (runtimeSnapshot.proxyGroups.isNotEmpty()) {
            return runtimeSnapshot
        }
        refreshRuntimeSnapshot()
        return runtimeSnapshot
    }

    private fun startLogStream() {
        stopLogStream()
        host.onLogReady(false)
        logJob = scope.launch(Dispatchers.IO) {
            val receiver = Clash.subscribeLogcat()
            host.onLogReady(true)
            publishSnapshot(currentSnapshot.copy(logReady = true))
            try {
                while (isActive) {
                    val item = receiver.receive()
                    localLogObserver?.invoke(item)
                    host.onLogItem(item)
                    val encoded = com.github.yumelira.yumebox.service.root.RootTunJson.Default.encodeToString(LogMessage.serializer(), item)
                    val seq = logSeq.incrementAndGet()
                    synchronized(recentLogs) {
                        recentLogs.addLast(seq to encoded)
                        while (recentLogs.size > MAX_BUFFERED_LOGS) {
                            recentLogs.removeFirst()
                        }
                    }
                }
            } finally {
                receiver.cancel()
                host.onLogReady(false)
                publishSnapshot(currentSnapshot.copy(logReady = false))
            }
        }
    }

    private fun stopLogStream() {
        logJob?.cancel()
        logJob = null
        synchronized(recentLogs) {
            recentLogs.clear()
        }
        host.onLogReady(false)
    }

    private fun startConnectionTracking() {
        stopConnectionTracking()
        connectionTrackingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching {
                    val snapshot = Clash.queryConnections()
                    ConnectionHistoryManager.updateConnections(snapshot.connections)
                }
                delay(CONNECTION_TRACKING_INTERVAL_MS)
            }
        }
    }

    private fun stopConnectionTracking() {
        connectionTrackingJob?.cancel()
        connectionTrackingJob = null
    }

    private fun publishSnapshot(snapshot: RuntimeSnapshot) {
        currentSnapshot = snapshot.copy(running = snapshot.phase.running)
        host.onSnapshotChanged(currentSnapshot)
    }

    private fun startupLog(spec: RuntimeSpec, message: String) {
        val scope = when (spec.owner) {
            RuntimeOwner.LocalTun -> RuntimeStartupLogStore.Scope.LOCAL_TUN
            RuntimeOwner.LocalHttp -> RuntimeStartupLogStore.Scope.LOCAL_HTTP
            RuntimeOwner.RootTun -> RuntimeStartupLogStore.Scope.ROOT_TUN
            RuntimeOwner.None -> return
        }
        RuntimeStartupLogStore(host.context.appContextOrSelf, scope)
            .append("${scope.tag} session: $message")
    }

    private fun describeFile(file: File): String {
        if (!file.exists()) {
            return "path=${file.absolutePath} exists=false"
        }
        val content = file.readText()
        return buildString {
            append("path=")
            append(file.absolutePath)
            append(" exists=true size=")
            append(content.length)
            append(" sha=")
            append(content.sha256Short())
            content.lineSequence()
                .map(String::trim)
                .firstOrNull { it.isNotEmpty() }
                ?.let {
                    append(" firstLine=")
                    append(it.take(160))
                }
        }
    }

    private fun String.sha256Short(): String {
        if (isBlank()) return "empty"
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private data class RuntimeQuerySnapshot(
        val proxyGroups: List<ProxyGroup> = emptyList(),
        val configuration: UiConfiguration = UiConfiguration(),
        val providers: List<Provider> = emptyList(),
        val trafficNow: Long = 0L,
        val trafficTotal: Long = 0L,
    )

    private companion object {
        private const val MAX_BUFFERED_LOGS = 256
        private const val PROXY_GROUP_READY_RETRY_COUNT = 10
        private const val PROXY_GROUP_READY_RETRY_DELAY_MS = 200L
        private const val CONNECTION_TRACKING_INTERVAL_MS = 2000L
    }
}
