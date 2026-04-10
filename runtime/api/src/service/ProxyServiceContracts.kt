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



package com.github.yumelira.yumebox.runtime.api.service

import android.content.Context
import com.github.yumelira.yumebox.runtime.api.service.common.constants.Intents
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeTargetMode

enum class LocalRuntimePhase {
    Idle,
    Starting,
    Running,
    Stopping,
    Failed;
    val isActive: Boolean
        get() = this != Idle
}

interface LocalRuntimeServiceContract {
    fun start(
        context: Context,
        mode: RuntimeTargetMode,
        source: String = ProxyServiceContracts.SOURCE_UNKNOWN,
    )
    fun stop(
        context: Context,
        clashRequestStopAction: String,
    )
}

interface LocalRuntimeStatusContract {
    val serviceRunning: Boolean
    fun reconcilePersistedRuntimeState()
    fun clearLegacyStateFiles()
    fun isRuntimeActive(mode: RuntimeTargetMode): Boolean
    fun queryRuntimePhase(mode: RuntimeTargetMode): LocalRuntimePhase
    fun queryRuntimeStartedAt(mode: RuntimeTargetMode): Long?
    fun isLocalRuntimeServiceAlive(mode: RuntimeTargetMode): Boolean
    fun markRuntimeIdle(mode: RuntimeTargetMode)
}

object RuntimeServiceContractRegistry {
    @Volatile
    var localRuntimeService: LocalRuntimeServiceContract? = null
    @Volatile
    var localRuntimeStatus: LocalRuntimeStatusContract? = null
    @Volatile
    var rootAccessSupport: com.github.yumelira.yumebox.runtime.api.service.root.RootAccessSupportContract? = null
    @Volatile
    var rootTunRuntimeRecovery: com.github.yumelira.yumebox.runtime.api.service.root.RootTunRuntimeRecoveryContract? = null
    @Volatile
    var rootTunForegroundService: com.github.yumelira.yumebox.runtime.api.service.root.RootTunForegroundServiceContract? = null
    @Volatile
    var rootTunStateStoreFactory: com.github.yumelira.yumebox.runtime.api.service.root.RootTunStateStoreFactoryContract? = null
    @Volatile
    var rootPackageQuery: com.github.yumelira.yumebox.runtime.api.service.root.RootPackageQueryContract? = null
}

object ProxyServiceContracts {
    const val SOURCE_UI = "ui"
    const val SOURCE_TILE = "tile"
    const val SOURCE_AUTO_RESTART = "auto_restart"
    const val SOURCE_AUTO_RESTART_BOOT = "auto_restart_boot"
    const val SOURCE_AUTO_RESTART_REPLACED = "auto_restart_replaced"
    const val SOURCE_UNKNOWN = "unknown"
    val ACTION_PROXY_STARTED: String
        get() = Intents.ACTION_CLASH_STARTED
    val ACTION_PROXY_STOPPED: String
        get() = Intents.ACTION_CLASH_STOPPED
    val ACTION_PROXY_GROUPS_UPDATED: String
        get() = Intents.ACTION_PROXY_GROUPS_UPDATED
    val ACTION_PROFILE_LOADED: String
        get() = Intents.ACTION_PROFILE_LOADED
    val ACTION_PROFILE_CHANGED: String
        get() = Intents.ACTION_PROFILE_CHANGED
    val ACTION_REQUEST_STOP: String
        get() = Intents.ACTION_CLASH_REQUEST_STOP

    val ACTION_PATCH_SELECTOR: String
        get() = Intents.ACTION_PATCH_SELECTOR
    const val EXTRA_GROUP_NAME = Intents.EXTRA_GROUP_NAME
    const val EXTRA_PROXY_NAME = Intents.EXTRA_PROXY_NAME
    const val EXTRA_PROFILE_ID = Intents.EXTRA_PROFILE_ID
    const val EXTRA_START_PROXY = Intents.EXTRA_START_PROXY

    val ACTION_PATCH_OVERRIDE: String
        get() = Intents.ACTION_PATCH_OVERRIDE
    val ACTION_CLEAR_OVERRIDE: String
        get() = Intents.ACTION_CLEAR_OVERRIDE
    const val EXTRA_OVERRIDE_SLOT = Intents.EXTRA_OVERRIDE_SLOT
    const val EXTRA_OVERRIDE_CONFIG = Intents.EXTRA_OVERRIDE_CONFIG

    val ACTION_HEALTH_CHECK: String
        get() = Intents.ACTION_HEALTH_CHECK
    val ACTION_HEALTH_CHECK_ALL: String
        get() = Intents.ACTION_HEALTH_CHECK_ALL
    const val EXTRA_HEALTH_CHECK_GROUP = Intents.EXTRA_HEALTH_CHECK_GROUP

    fun intentSelf(action: String, packageName: String? = null): android.content.Intent {
        return android.content.Intent(action).apply {
            if (!packageName.isNullOrBlank()) {
                setPackage(packageName)
            }
        }
    }
}
