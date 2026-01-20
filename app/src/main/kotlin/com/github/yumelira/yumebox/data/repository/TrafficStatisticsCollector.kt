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

package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStorage
import com.github.yumelira.yumebox.domain.model.TrafficData
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

class TrafficStatisticsCollector(
    private val clashManager: ClashManager,
    private val trafficStatisticsStore: TrafficStatisticsStorage,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "TrafficStatisticsCollector"
        private const val FOREGROUND_INTERVAL_MS = 900000L // 15 minutes
        private const val BACKGROUND_INTERVAL_MS = 1800000L // 30 minutes
    }

    private var collectionJob: Job? = null
    private var lastTotalUpload: Long = 0L
    private var lastTotalDownload: Long = 0L
    private var lastProfileId: String? = null
    private val client by lazy { OkHttpClient.Builder().build() }
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ConnectionsResponse(val downloadTotal: Long = 0, val uploadTotal: Long = 0)

    init {
        startCollection()
    }

    private fun startCollection() {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            combine(
                clashManager.isRunning,
                clashManager.screenStateFlow
            ) { isRunning, isScreenOn ->
                isRunning to isScreenOn
            }.collectLatest { (isRunning, isScreenOn) ->
                if (isRunning) {
                    startTrafficMonitoring(isScreenOn)
                } else {
                    resetLastValues()
                }
            }
        }
    }

    private suspend fun startTrafficMonitoring(isScreenOn: Boolean) {
        lastTotalUpload = trafficStatisticsStore.getLastTrafficUpload()
        lastTotalDownload = trafficStatisticsStore.getLastTrafficDownload()
        lastProfileId = trafficStatisticsStore.getLastProfileId()
        val interval = if (isScreenOn) FOREGROUND_INTERVAL_MS else BACKGROUND_INTERVAL_MS
        while (currentCoroutineContext().isActive) {
            runCatching {
                collectTrafficData()
            }.onFailure { e ->
                if (e is CancellationException) throw e
                Timber.tag(TAG).e(e, MLang.TrafficStatistics.Message.CollectionFailed.format(e.message ?: ""))
            }
            delay(interval)
        }
    }

    private suspend fun getInternalTrafficTotal(): Pair<Long, Long> {
        return try {
            val total = TrafficData.from(Clash.queryTrafficTotal())
            total.upload to total.download
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "Failed to query traffic via core")
            throw e
        }
    }

    private suspend fun collectTrafficData() {
        val (currentUpload, currentDownload) = getInternalTrafficTotal()
        val currentProfile = clashManager.currentProfile.value
        val currentProfileId = currentProfile?.id
        val currentProfileName = currentProfile?.name

        if (lastTotalUpload == 0L && lastTotalDownload == 0L) {
            lastTotalUpload = currentUpload
            lastTotalDownload = currentDownload
            lastProfileId = currentProfileId
            trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
            return
        }

        if (currentProfileId != lastProfileId) {
            lastTotalUpload = currentUpload
            lastTotalDownload = currentDownload
            lastProfileId = currentProfileId
            trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
            return
        }

        if (currentUpload < lastTotalUpload || currentDownload < lastTotalDownload) {
            lastTotalUpload = currentUpload
            lastTotalDownload = currentDownload
            trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
            return
        }

        val uploadDelta = currentUpload - lastTotalUpload
        val downloadDelta = currentDownload - lastTotalDownload

        if (uploadDelta > 0 || downloadDelta > 0) {
            trafficStatisticsStore.recordTraffic(
                uploadDelta,
                downloadDelta,
                currentProfileId,
                currentProfileName
            )
            Timber.tag(TAG)
                .v(MLang.TrafficStatistics.Message.RecordTraffic.format(uploadDelta, downloadDelta, currentProfileName))
        }

        lastTotalUpload = currentUpload
        lastTotalDownload = currentDownload
        trafficStatisticsStore.setLastTraffic(currentUpload, currentDownload, currentProfileId)
    }

    private fun resetLastValues() {
        lastTotalUpload = 0L
        lastTotalDownload = 0L
        lastProfileId = null
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }
}
