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

package com.github.yumelira.yumebox.presentation.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.common.native.NativeLibraryManager
import com.github.yumelira.yumebox.common.util.DeviceUtil
import com.github.yumelira.yumebox.common.util.DownloadProgress
import com.github.yumelira.yumebox.common.util.DownloadUtil
import com.github.yumelira.yumebox.data.model.AutoCloseMode
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.domain.facade.SettingsFacade
import com.github.yumelira.yumebox.substore.SubStorePaths
import dev.oom_wg.purejoy.mlang.MLang
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class FeatureViewModel(
    private val settingsFacade: SettingsFacade,
    private val application: Application,
) : ViewModel() {

    val isServiceRunning: Boolean get() = settingsFacade.isSubStoreRunning()
    val allowLanAccess: Preference<Boolean> = settingsFacade.allowLanAccess
    val backendPort: Preference<Int> = settingsFacade.backendPort
    val frontendPort: Preference<Int> = settingsFacade.frontendPort
    val selectedPanelType: Preference<Int> = settingsFacade.selectedPanelType

    val autoCloseMode: Preference<AutoCloseMode> = settingsFacade.autoCloseMode

    private val _serviceRunningState = MutableStateFlow(settingsFacade.isSubStoreRunning())
    val serviceRunningState: StateFlow<Boolean> = _serviceRunningState.asStateFlow()

    private var autoCloseJob: Job? = null

    private val _panelPaths = MutableStateFlow<List<String>>(emptyList())

    private val _panelInstallStatus = MutableStateFlow(listOf(false, false))
    val panelInstallStatus: StateFlow<List<Boolean>> = _panelInstallStatus.asStateFlow()

    private val _isDownloadingPanel = MutableStateFlow(false)
    val isDownloadingPanel: StateFlow<Boolean> = _isDownloadingPanel.asStateFlow()

    private val _isDownloadingSubStoreFrontend = MutableStateFlow(false)
    val isDownloadingSubStoreFrontend: StateFlow<Boolean> = _isDownloadingSubStoreFrontend.asStateFlow()

    private val _isDownloadingSubStoreBackend = MutableStateFlow(false)
    val isDownloadingSubStoreBackend: StateFlow<Boolean> = _isDownloadingSubStoreBackend.asStateFlow()

    private val _subStoreFrontendDownloadProgress = MutableStateFlow<DownloadProgress?>(null)

    private val _subStoreBackendDownloadProgress = MutableStateFlow<DownloadProgress?>(null)


    private val _isSubStoreInitialized = MutableStateFlow(false)
    val isSubStoreInitialized: StateFlow<Boolean> = _isSubStoreInitialized.asStateFlow()

    private val _isExtensionInstalled = MutableStateFlow(false)
    val isExtensionInstalled: StateFlow<Boolean> = _isExtensionInstalled.asStateFlow()

    private val _isJavetLoaded = MutableStateFlow(false)
    val isJavetLoaded: StateFlow<Boolean> = _isJavetLoaded.asStateFlow()

    companion object {
        private const val EXTENSION_PACKAGE_NAME = "com.github.yumelira.yumebox.extension"
        private const val JAVET_LIB_NAME = "libjavet-node-android"
        private val PANEL_NAMES = listOf("zashboard", "metacubexd")
        private val PANEL_DISPLAY_NAMES = listOf("Sub-Store Zashboard", "Sub-Store 官方面板")
        private val PANEL_URLS = listOf(
            "https://github.com/Zephyruso/zashboard/releases/latest/download/dist.zip",
            "https://github.com/MetaCubeX/metacubexd/releases/latest/download/compressed-dist.tgz"
        )
        private val ENTRY_FILES = listOf("index.html", "main.html", "app.html")
    }

    fun startService() {
        if (DeviceUtil.is32BitDevice()) {
            showToast(MLang.Feature.SubStore.Not32Bit)
            return
        }
        if (!checkSubStoreReadiness()) return
        viewModelScope.launch {
            settingsFacade.startSubStore(
                frontendPort = frontendPort.value,
                backendPort = backendPort.value,
                allowLan = allowLanAccess.value
            )
            delay(500)
            val actuallyRunning = settingsFacade.isSubStoreRunning()
            _serviceRunningState.value = actuallyRunning
            if (actuallyRunning) setupAutoCloseTimer()
        }
    }

    private fun checkSubStoreReadiness(): Boolean {
        return when {
            !(_isExtensionInstalled.value || _isJavetLoaded.value) -> {
                showToast(MLang.Feature.SubStore.InstallExtension); false
            }

            !_isSubStoreInitialized.value -> {
                showToast(MLang.Feature.SubStore.DownloadSubStoreFirst); false
            }

            !_isJavetLoaded.value -> {
                showToast(MLang.Feature.SubStore.JavetNotReady); false
            }

            else -> true
        }
    }

    fun stopService() {
        viewModelScope.launch {
            cancelAutoCloseTimer()
            settingsFacade.stopSubStore()
            _serviceRunningState.value = false
            settingsFacade.autoCloseMode.set(AutoCloseMode.DISABLED)
        }
    }

    fun setAutoCloseMode(mode: AutoCloseMode) {
        settingsFacade.autoCloseMode.set(mode)
        if (isServiceRunning) {
            cancelAutoCloseTimer()
            setupAutoCloseTimer()
        }
    }

    fun setAllowLanAccess(allow: Boolean) = allowLanAccess.set(allow)

    fun initializeSubStoreStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSubStoreInitialized.value = SubStorePaths.isResourcesReady()
            _isExtensionInstalled.value = checkExtensionInstalled()
            initializeJavetStatus()
        }
    }

    private fun checkExtensionInstalled(): Boolean {
        val isPkgInstalled = runCatching {
            application.packageManager.getApplicationInfo(EXTENSION_PACKAGE_NAME, 0)
            true
        }.getOrDefault(false)
        if (isPkgInstalled) return true
        if (runCatching { System.loadLibrary("javet-node-android"); true }.getOrDefault(false)) {
            return true
        }
        NativeLibraryManager.initialize(application)
        val results = NativeLibraryManager.extractAllLibraries()
        return results[JAVET_LIB_NAME] == true
    }

    private fun initializeJavetStatus() {
        if (!_isExtensionInstalled.value) {
            _isJavetLoaded.value = false; return
        }
        NativeLibraryManager.initialize(application)
        _isJavetLoaded.value = if (!NativeLibraryManager.isLibraryAvailable(JAVET_LIB_NAME)) {
            NativeLibraryManager.extractAllLibraries()[JAVET_LIB_NAME] == true
        } else true
    }

    fun refreshExtensionStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            _isExtensionInstalled.value = checkExtensionInstalled()
            initializeJavetStatus()
        }
    }

    fun setSelectedPanelType(panelType: Int) {
        selectedPanelType.set(panelType)
        updatePanelPaths()
    }

    private fun updatePanelPaths() {
        viewModelScope.launch(Dispatchers.IO) {
            val filesDir = application.filesDir.absolutePath
            val paths = mutableListOf<String>()
            val installStatus = mutableListOf<Boolean>()
            PANEL_NAMES.forEachIndexed { index, name ->
                val panelDir = File("$filesDir/panel/$name")
                val entryFile = findPanelEntryFile(panelDir)
                val isInstalled = panelDir.exists() && entryFile != null
                installStatus.add(isInstalled)
                if (isInstalled) {
                    paths.add("${panelDisplayName(index)}: ${entryFile.absolutePath.substring(filesDir.length)}")
                }
            }
            _panelPaths.value = paths
            _panelInstallStatus.value = installStatus
        }
    }

    private fun findPanelEntryFile(panelDir: File): File? {
        ENTRY_FILES.forEach { File(panelDir, it).takeIf { f -> f.exists() }?.let { return it } }
        val distDir = File(panelDir, "dist")
        if (distDir.exists()) {
            ENTRY_FILES.forEach { File(distDir, it).takeIf { f -> f.exists() }?.let { return it } }
        }
        return null
    }

    fun initializePanelPaths() = updatePanelPaths()
    fun downloadSubStoreFrontend() {
        if (_isDownloadingSubStoreFrontend.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isDownloadingSubStoreFrontend.value = true
            _subStoreFrontendDownloadProgress.value = null
            runCatching {
                SubStorePaths.ensureStructure()
                SubStorePaths.frontendDir.apply { if (!exists()) mkdirs() }
                val success = DownloadUtil.downloadAndExtract(
                    url = "https://github.com/sub-store-org/Sub-Store-Front-End/releases/latest/download/dist.zip",
                    targetDir = SubStorePaths.frontendDir,
                    onProgress = { _subStoreFrontendDownloadProgress.value = it })
                showToast(
                    if (success) {
                        MLang.Feature.SubStore.FrontendDownloadSuccess
                    } else {
                        MLang.Feature.SubStore.FrontendDownloadFailed
                    }
                )
                if (success) _isSubStoreInitialized.value = SubStorePaths.isResourcesReady()
            }.onFailure { e ->
                timber.log.Timber.e(e, "下载前端失败")
                showToast(MLang.Feature.SubStore.DownloadError.format(e.message ?: MLang.Util.Error.UnknownError))
            }
            _isDownloadingSubStoreFrontend.value = false
            _subStoreFrontendDownloadProgress.value = null
        }
    }

    fun downloadSubStoreBackend() {
        if (_isDownloadingSubStoreBackend.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isDownloadingSubStoreBackend.value = true
            _subStoreBackendDownloadProgress.value = null
            runCatching {
                SubStorePaths.ensureStructure()
                SubStorePaths.backendDir.apply { if (!exists()) mkdirs() }
                val success = DownloadUtil.download(
                    url = "https://github.com/sub-store-org/Sub-Store/releases/latest/download/sub-store.bundle.js",
                    targetFile = SubStorePaths.backendBundle,
                    onProgress = { _subStoreBackendDownloadProgress.value = it })
                showToast(
                    if (success) {
                        MLang.Feature.SubStore.BackendDownloadSuccess
                    } else {
                        MLang.Feature.SubStore.BackendDownloadFailed
                    }
                )
                if (success) _isSubStoreInitialized.value = SubStorePaths.isResourcesReady()
            }.onFailure { e ->
                timber.log.Timber.e(e, "下载后端失败")
                showToast(MLang.Feature.SubStore.DownloadError.format(e.message ?: MLang.Util.Error.UnknownError))
            }
            _isDownloadingSubStoreBackend.value = false
            _subStoreBackendDownloadProgress.value = null
        }
    }

    fun downloadSubStoreAll() {
        viewModelScope.launch {
            downloadSubStoreFrontend()
            delay(1000)
            downloadSubStoreBackend()
        }
    }

    fun downloadExternalPanelEnhanced(panelType: Int = 0) {
        if (_isDownloadingPanel.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isDownloadingPanel.value = true
            runCatching {
                if (panelType !in PANEL_NAMES.indices) {
                    showToast(MLang.Feature.SubStore.InvalidPanelType)
                    return@runCatching
                }
                val panelDir = File("${application.filesDir.absolutePath}/panel/${PANEL_NAMES[panelType]}")
                if (panelDir.exists()) panelDir.deleteRecursively()
                panelDir.mkdirs()
                val success = DownloadUtil.downloadAndExtract(url = PANEL_URLS[panelType], targetDir = panelDir)
                val panelName = panelDisplayName(panelType)
                showToast(
                    if (success) {
                        MLang.Feature.SubStore.PanelInstallSuccess.format(panelName)
                    } else {
                        MLang.Feature.SubStore.PanelInstallFailed.format(panelName)
                    }
                )
                if (success) updatePanelPaths()
            }.onFailure { e ->
                timber.log.Timber.e(e, "下载面板失败")
                showToast(MLang.Feature.SubStore.PanelInstallError.format(e.message ?: MLang.Util.Error.UnknownError))
            }
            _isDownloadingPanel.value = false
        }
    }

    private fun showToast(msg: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(application, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAutoCloseTimer() {
        cancelAutoCloseTimer()
        val mode = settingsFacade.autoCloseMode.value
        mode.minutes?.let { minutes ->
            autoCloseJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                showToast(MLang.Feature.ServiceStatus.AutoClosed)
                stopService()
            }
        }
    }

    init {
        if (settingsFacade.isSubStoreRunning() && settingsFacade.autoCloseMode.value.shouldStartTimer) {
            setupAutoCloseTimer()
        }
    }

    private fun cancelAutoCloseTimer() {
        autoCloseJob?.cancel()
        autoCloseJob = null
    }

    private fun panelDisplayName(index: Int): String = when (index) {
        0 -> MLang.Feature.SubStore.Zashboard
        1 -> MLang.Feature.SubStore.OfficialPanel
        else -> MLang.Feature.Panel.Unknown
    }
}
