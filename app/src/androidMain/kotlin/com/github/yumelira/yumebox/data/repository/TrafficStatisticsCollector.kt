package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.store.TrafficStatisticsStore
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import timber.log.Timber

class TrafficStatisticsCollector(
    private val clashManager: ClashManager,
    private val trafficStatisticsStore: TrafficStatisticsStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "TrafficStatisticsCollector"
        private const val COLLECTION_INTERVAL_MS = 5000L
    }

    private var collectionJob: Job? = null
    private var lastTotalUpload: Long = 0L
    private var lastTotalDownload: Long = 0L
    private var lastProfileId: String? = null

    init {
        startCollection()
    }

    private fun startCollection() {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            clashManager.isRunning.collect { isRunning ->
                if (isRunning) {
                    startTrafficMonitoring()
                } else {
                    resetLastValues()
                }
            }
        }
    }

    private fun CoroutineScope.startTrafficMonitoring() {
        launch {
            lastTotalUpload = trafficStatisticsStore.getLastTrafficUpload()
            lastTotalDownload = trafficStatisticsStore.getLastTrafficDownload()
            lastProfileId = trafficStatisticsStore.getLastProfileId()

            while (isActive && clashManager.isRunning.value) {
                runCatching {
                    collectTrafficData()
                    delay(COLLECTION_INTERVAL_MS)
                }.onFailure { e ->
                    if (e is CancellationException) throw e
                    Timber.tag("TrafficStatisticsCollec").e(e, MLang.TrafficStatistics.Message.CollectionFailed.format(e.message ?: ""))
                    delay(COLLECTION_INTERVAL_MS)
                }
            }
        }
    }

    private fun collectTrafficData() {
        val trafficTotal = clashManager.trafficTotal.value
        val currentUpload = trafficTotal.upload
        val currentDownload = trafficTotal.download
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
            Timber.tag("TrafficStatisticsCollec")
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
