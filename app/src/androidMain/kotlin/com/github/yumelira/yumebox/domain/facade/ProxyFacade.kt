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

package com.github.yumelira.yumebox.domain.facade

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxyState
import com.github.yumelira.yumebox.domain.model.RunningMode
import com.github.yumelira.yumebox.domain.model.TrafficData

class ProxyFacade(
    private val clashManager: ClashManager,
    private val proxyConnectionService: ProxyConnectionService
) {
    val proxyState: StateFlow<ProxyState> = clashManager.proxyState
    val isRunning: StateFlow<Boolean> = clashManager.isRunning
    val currentProfile: StateFlow<Profile?> = clashManager.currentProfile
    val trafficNow: StateFlow<TrafficData> = clashManager.trafficNow
    val trafficTotal: StateFlow<TrafficData> = clashManager.trafficTotal
    val tunnelState: StateFlow<TunnelState?> = clashManager.tunnelState
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = clashManager.proxyGroups
    val runningMode: StateFlow<RunningMode> = clashManager.runningMode
    val logs = clashManager.logs

    suspend fun startProxy(profileId: String, forceTunMode: Boolean? = null): Result<Intent?> {
        return proxyConnectionService.prepareAndStart(profileId, forceTunMode)
    }

    fun stopProxy() {
        proxyConnectionService.stop(runningMode.value)
    }

    suspend fun refreshProxyGroups(skipCacheClear: Boolean = false): Result<Unit> {
        return clashManager.refreshProxyGroups(skipCacheClear)
    }

    suspend fun selectProxy(groupName: String, proxyName: String): Boolean {
        return clashManager.selectProxy(groupName, proxyName)
    }

    fun testProxyDelay(groupName: String) {
        clashManager.testProxyDelay(groupName)
    }

    fun getCachedDelay(nodeName: String): Int? {
        return clashManager.getCachedDelay(nodeName)
    }
}
