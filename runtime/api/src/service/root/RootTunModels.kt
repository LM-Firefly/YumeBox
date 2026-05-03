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



package com.github.yumelira.yumebox.runtime.api.service.root

import android.content.Context
import android.os.IInterface
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.UiConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object RootTunJson {
    val Default = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

@Serializable
enum class RootTunState {
    Idle,
    Starting,
    Running,
    Stopping,
    Failed;

    val isActive: Boolean
        get() = this == Starting || this == Running || this == Stopping

    val isRecovering: Boolean
        get() = this == Starting || this == Stopping
}

@Serializable
data class RootTunStartRequest(
    val source: String = "",
)

@Serializable
data class RootTunOperationResult(
    val success: Boolean,
    val error: String? = null,
)

@Serializable
data class RootTunStatus(
    val state: RootTunState = RootTunState.Idle,
    val running: Boolean = state.isActive,
    val lastError: String? = null,
    val profileUuid: String? = null,
    val profileName: String? = null,
    val runtimeReady: Boolean = false,
    val controllerReady: Boolean = false,
    val startedAt: Long? = null,
    val staticPlanFingerprint: String? = null,
    val transportFingerprint: String? = null,
    val overrideFingerprint: String? = null,
    val profileFingerprint: String? = null,
)

@Serializable
data class RootTunRuntimeSnapshot(
    val status: RootTunStatus = RootTunStatus(),
    val profileUuid: String? = null,
    val profileName: String? = null,
    val proxyGroups: List<ProxyGroup> = emptyList(),
    val configuration: UiConfiguration = UiConfiguration(),
    val providers: List<Provider> = emptyList(),
    val trafficNow: Long = 0L,
    val trafficTotal: Long = 0L,
    val transportFingerprint: String = "",
    val overrideFingerprint: String = "",
    val profileFingerprint: String = "",
)

data class RootAccessStatus(
    val rootAccessGranted: Boolean,
    val blockedMessage: String = "Root access required",
) {
    val canStartRootTun: Boolean
        get() = rootAccessGranted
    fun rootTunBlockedMessage(): String = blockedMessage
}

interface RootAccessSupportContract {
    fun evaluate(context: Context): RootAccessStatus
    suspend fun evaluateAsync(context: Context): RootAccessStatus
    suspend fun requireRootTunAccess(context: Context): RootAccessStatus
}

interface RootTunRuntimeRecoveryContract {
    fun isBinderAlive(service: IInterface?): Boolean
    fun isBinderConnectionFailure(error: Throwable): Boolean
    fun binderFailureReason(error: Throwable): String
    fun handleBinderGone(context: Context, reason: String?)
}

interface RootTunForegroundServiceContract {
    fun start(context: Context)
    fun stop(context: Context)
}

@Serializable
data class RootTunLogChunk(
    val nextSeq: Long = 0L,
    val items: List<String> = emptyList(),
)

interface RootTunStateStoreContract {
    fun snapshot(): RootTunStatus
    fun isRunning(): Boolean
    fun updateStatus(status: RootTunStatus)
    fun markIdle(error: String? = null)
    fun clear()
}

interface RootTunStateStoreFactoryContract {
    fun create(context: Context): RootTunStateStoreContract
}

interface RootPackageQueryContract {
    fun hasRootAccess(): Boolean
    fun queryInstalledPackageNames(): Set<String>?
}
