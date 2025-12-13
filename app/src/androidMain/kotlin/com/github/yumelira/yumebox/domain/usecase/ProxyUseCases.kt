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

package com.github.yumelira.yumebox.domain.usecase

import com.github.yumelira.yumebox.clash.config.ClashConfiguration
import com.github.yumelira.yumebox.clash.manager.ProfileManager
import com.github.yumelira.yumebox.clash.manager.ProxyGroupManager
import com.github.yumelira.yumebox.clash.manager.ProxyStateManager
import com.github.yumelira.yumebox.clash.manager.ServiceManager
import com.github.yumelira.yumebox.clash.testing.ProxyTestManager
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.data.model.Profile
import kotlinx.coroutines.delay
import java.net.InetSocketAddress

class LoadProfileUseCase(
    private val profileManager: ProfileManager,
    private val stateManager: ProxyStateManager,
    private val proxyGroupManager: ProxyGroupManager
) {
    suspend operator fun invoke(
        profile: Profile,
        forceDownload: Boolean = false,
        onProgress: ((String, Int) -> Unit)? = null,
        willUseTunMode: Boolean = false,
        quickStart: Boolean = false
    ): Result<String> {
        return profileManager.loadProfile(profile, forceDownload, onProgress, willUseTunMode, quickStart)
            .onSuccess {
                stateManager.updateCurrentProfile(profile)
                proxyGroupManager.restoreSelections(profile.id)
            }
    }
}

class DownloadProfileUseCase(
    private val profileManager: ProfileManager
) {
    suspend operator fun invoke(
        profile: Profile,
        forceDownload: Boolean = true,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<String> {
        return profileManager.downloadProfileOnly(profile, forceDownload, onProgress)
    }
}

class ReloadProfileUseCase(
    private val profileManager: ProfileManager,
    private val stateManager: ProxyStateManager
) {
    suspend operator fun invoke(): Result<Unit> {
        return profileManager.reloadCurrentProfile(stateManager.currentProfile.value)
    }
}

class StartTunModeUseCase(
    private val serviceManager: ServiceManager
) {
    suspend operator fun invoke(
        fd: Int,
        config: ClashConfiguration.TunConfig = ClashConfiguration.TunConfig(),
        markSocket: (Int) -> Boolean,
        querySocketUid: (protocol: Int, source: InetSocketAddress, target: InetSocketAddress) -> Int = { _, _, _ -> -1 }
    ): Result<Unit> {
        return serviceManager.startTunMode(fd, config, markSocket, querySocketUid)
    }
}

class StartHttpModeUseCase(
    private val serviceManager: ServiceManager
) {
    suspend operator fun invoke(
        config: ClashConfiguration.HttpConfig = ClashConfiguration.HttpConfig()
    ): Result<String?> {
        return serviceManager.startHttpMode(config)
    }
}

class StopProxyUseCase(
    private val serviceManager: ServiceManager,
    private val proxyGroupManager: ProxyGroupManager
) {
    operator fun invoke() {
        serviceManager.stop()
        proxyGroupManager.clearGroupStates()
    }
}

class SelectProxyUseCase(
    private val proxyGroupManager: ProxyGroupManager,
    private val stateManager: ProxyStateManager
) {
    suspend operator fun invoke(groupName: String, proxyName: String): Boolean {
        return proxyGroupManager.selectProxy(groupName, proxyName, stateManager.currentProfile.value)
    }
}

class RefreshProxyGroupsUseCase(
    private val proxyGroupManager: ProxyGroupManager,
    private val stateManager: ProxyStateManager
) {
    suspend operator fun invoke(skipCacheClear: Boolean = false): Result<Unit> {
        return proxyGroupManager.refreshProxyGroups(skipCacheClear, stateManager.currentProfile.value)
    }
    suspend fun refreshGroup(groupName: String): Result<Unit> {
        return proxyGroupManager.refreshGroup(groupName, stateManager.currentProfile.value)
    }
}

class TestProxyDelayUseCase(
    private val proxyTestManager: ProxyTestManager
) {
    operator fun invoke(
        groupName: String,
        priority: Int = ProxyTestManager.Priority.NORMAL,
        forceTest: Boolean = false
    ) {
        proxyTestManager.requestTest(groupName, priority, forceTest)
    }
}

class HealthCheckUseCase(
    private val refreshProxyGroupsUseCase: RefreshProxyGroupsUseCase
) {
    suspend operator fun invoke(groupName: String): Result<Unit> {
        return try {
            Clash.healthCheck(groupName).await()
            refreshProxyGroupsUseCase.refreshGroup(groupName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun checkAll(): Result<Unit> {
        return try {
            Clash.healthCheckAll()
            delay(3000)
            refreshProxyGroupsUseCase()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
