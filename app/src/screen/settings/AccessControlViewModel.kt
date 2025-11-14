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



package com.github.yumelira.yumebox.screen.settings

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.presentation.AndroidContractStateViewModel
import com.github.yumelira.yumebox.core.presentation.LoadableState
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.repository.NetworkSettingsRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import com.github.yumelira.yumebox.service.root.RootPackageShell
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class AccessControlViewModel(
    application: Application,
    private val repository: NetworkSettingsRepository,
    private val proxyFacade: ProxyFacade,
) : AndroidContractStateViewModel<AccessControlViewModel.UiState, AccessControlViewModel.AccessControlUiEffect>(
    application,
    UiState(),
) {

    data class AppInfo(
        val packageName: String,
        val label: String,
        val isSystemApp: Boolean,
        val isChinaApp: Boolean,
        val isSelected: Boolean,
        val installTime: Long = 0L,
        val updateTime: Long = 0L,
    )

    enum class SortMode {
        PACKAGE_NAME,
        LABEL,
        INSTALL_TIME,
        UPDATE_TIME;

        val displayName: String
            get() = when (this) {
                PACKAGE_NAME -> MLang.AccessControl.SortMode.PackageName
                LABEL -> MLang.AccessControl.SortMode.Label
                INSTALL_TIME -> MLang.AccessControl.SortMode.InstallTime
                UPDATE_TIME -> MLang.AccessControl.SortMode.UpdateTime
            }
    }

    data class UiState(
        override val isLoading: Boolean = true,
        val apps: List<AppInfo> = emptyList(),
        val selectedPackages: Set<String> = emptySet(),
        val searchQuery: String = "",
        val showSystemApps: Boolean = false,
        val sortMode: SortMode = SortMode.LABEL,
        val selectedFirst: Boolean = true,
        val needsMiuiPermission: Boolean = false,
        override val message: String? = null,
        override val error: String? = null,
    ) : LoadableState<UiState> {
        override fun withLoading(loading: Boolean): UiState = copy(isLoading = loading)
        override fun withError(error: String?): UiState = copy(error = error)
        override fun withMessage(message: String?): UiState = copy(message = message)
    }

    private var applyPackagesJob: Job? = null
    val filteredApps: StateFlow<List<AppInfo>> = combine(
        uiState,
        uiState,
        uiState,
        uiState,
        uiState,
    ) { stateForApps, stateForQuery, stateForSystem, stateForSort, stateForSelectedFirst ->
        filterApps(
            apps = stateForApps.apps,
            query = stateForQuery.searchQuery,
            showSystemApps = stateForSystem.showSystemApps,
            sortMode = stateForSort.sortMode,
            selectedFirst = stateForSelectedFirst.selectedFirst,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    sealed interface AccessControlUiEffect {
        data class ShowMessage(val message: String) : AccessControlUiEffect
        data class ShowError(val message: String) : AccessControlUiEffect
    }

    init {
        checkAndLoad()
    }

    private fun checkAndLoad() {
        val context = getApplication<Application>()
        val permission = "com.android.permission.GET_INSTALLED_APPS"

        if (RootPackageShell.hasRootAccess()) {
            loadApps()
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            loadApps()
        } else {
            val isMiui = runCatching {
                val permissionInfo = context.packageManager.getPermissionInfo(permission, 0)
                permissionInfo.packageName == "com.lbe.security.miui"
            }.getOrElse { false }

            if (isMiui) {
                _uiState.update { it.copy(needsMiuiPermission = true, isLoading = false) }
            } else {
                loadApps()
            }
        }
    }

    fun onPermissionResult() {
        _uiState.update { it.copy(needsMiuiPermission = false) }
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val selectedPackages = repository.accessControlPackages.value
            val apps = runCatching {
                withContext(Dispatchers.IO) {
                    loadInstalledApps(selectedPackages)
                }
            }.getOrElse {
                _uiState.update { state -> state.copy(isLoading = false, needsMiuiPermission = true) }
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    apps = apps,
                    selectedPackages = selectedPackages,
                )
            }
        }
    }

    private fun loadInstalledApps(selectedPackages: Set<String>): List<AppInfo> {
        val pm = getApplication<Application>().packageManager
        val selfPackageName = getApplication<Application>().packageName

        val packages = runCatching {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }.getOrElse { error ->
            if (error is SecurityException) {
                loadInstalledAppsFromRoot(pm, selfPackageName)
            } else {
                throw error
            }
        }

        return packages.filter { it.packageName != selfPackageName }.map { appInfo ->
            val pkgInfo = runCatching { pm.getPackageInfo(appInfo.packageName, 0) }.getOrNull()
            AppInfo(
                packageName = appInfo.packageName,
                label = appInfo.loadLabel(pm).toString(),
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isChinaApp = isChinaPackage(appInfo.packageName),
                isSelected = selectedPackages.contains(appInfo.packageName),
                installTime = pkgInfo?.firstInstallTime ?: 0L,
                updateTime = pkgInfo?.lastUpdateTime ?: 0L
            )
        }
    }

    private fun loadInstalledAppsFromRoot(
        pm: PackageManager,
        selfPackageName: String,
    ): List<ApplicationInfo> {
        val packageNames = RootPackageShell.queryInstalledPackageNames()
            ?: throw SecurityException("Unable to query installed packages from root shell")

        return packageNames
            .asSequence()
            .filterNot { it == selfPackageName }
            .mapNotNull { packageName ->
                runCatching { pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA) }.getOrNull()
            }
            .toList()
    }

    private fun filterApps(
        apps: List<AppInfo>,
        query: String,
        showSystemApps: Boolean,
        sortMode: SortMode = SortMode.LABEL,
        selectedFirst: Boolean = true,
        descending: Boolean = false,
    ): List<AppInfo> {
        val filtered = apps.filter { app ->
            val matchesQuery = query.isEmpty() ||
                    app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            val matchesSystemFilter = showSystemApps || !app.isSystemApp
            matchesQuery && matchesSystemFilter
        }
        val comparator = when (sortMode) {
            SortMode.PACKAGE_NAME -> compareBy<AppInfo> { it.packageName.lowercase() }
            SortMode.LABEL -> compareBy { it.label.lowercase() }
            SortMode.INSTALL_TIME -> compareBy { it.installTime }
            SortMode.UPDATE_TIME -> compareBy { it.updateTime }
        }
        val sorted = if (descending) filtered.sortedWith(comparator.reversed()) else filtered.sortedWith(comparator)
        return if (selectedFirst) {
            sorted.sortedByDescending { it.isSelected }
        } else {
            sorted
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query) }
    }

    fun onSortModeChange(mode: SortMode) {
        _uiState.update { state -> state.copy(sortMode = mode) }
    }

    fun onSelectedFirstChange(selectedFirst: Boolean) {
        _uiState.update { state -> state.copy(selectedFirst = selectedFirst) }
    }

    fun onShowSystemAppsChange(show: Boolean) {
        _uiState.update { state -> state.copy(showSystemApps = show) }
    }

    fun onAppSelectionChange(packageName: String, selected: Boolean) {
        _uiState.update { state ->
            val newSelectedPackages = if (selected) {
                state.selectedPackages + packageName
            } else {
                state.selectedPackages - packageName
            }

            val newApps = state.apps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isSelected = selected)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
            )
        }

        persistSelectionAndApply()
    }

    fun selectAll() {
        val currentFilteredPackages = filteredApps.value.map { it.packageName }.toSet()
        _uiState.update { state ->
            val newSelectedPackages = state.selectedPackages + currentFilteredPackages

            val newApps = state.apps.map { app ->
                if (currentFilteredPackages.contains(app.packageName)) {
                    app.copy(isSelected = true)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
            )
        }

        persistSelectionAndApply()
    }

    fun deselectAll() {
        val currentFilteredPackages = filteredApps.value.map { it.packageName }.toSet()
        _uiState.update { state ->
            val newSelectedPackages = state.selectedPackages - currentFilteredPackages

            val newApps = state.apps.map { app ->
                if (currentFilteredPackages.contains(app.packageName)) {
                    app.copy(isSelected = false)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
            )
        }

        persistSelectionAndApply()
    }

    fun invertSelection() {
        val currentFilteredPackages = filteredApps.value.map { it.packageName }.toSet()
        _uiState.update { state ->
            val newSelectedPackages = state.selectedPackages.toMutableSet()
            currentFilteredPackages.forEach { pkg ->
                if (newSelectedPackages.contains(pkg)) newSelectedPackages.remove(pkg) else newSelectedPackages.add(pkg)
            }
            val newApps = state.apps.map { app ->
                if (currentFilteredPackages.contains(app.packageName)) {
                    app.copy(isSelected = !app.isSelected)
                } else {
                    app
                }
            }
            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
            )
        }
        persistSelectionAndApply()
    }

    fun selectChinaAppsInCurrentList(): Int {
        return applyRegionalSelectionInCurrentList(selectChina = true)
    }

    fun selectNonChinaAppsInCurrentList(): Int {
        return applyRegionalSelectionInCurrentList(selectChina = false)
    }

    private fun applyRegionalSelectionInCurrentList(selectChina: Boolean): Int {
        val currentFiltered = filteredApps.value
        var selectedCount = 0
        _uiState.update { state ->
            val currentPackages = currentFiltered.map { it.packageName }.toSet()
            val targetPackages = currentFiltered
                .filter { it.isChinaApp == selectChina }
                .mapTo(mutableSetOf()) { it.packageName }
            selectedCount = targetPackages.size

            val newSelectedPackages = state.selectedPackages
                .minus(currentPackages)
                .plus(targetPackages)

            val newApps = state.apps.map { app ->
                if (app.packageName in currentPackages) {
                    app.copy(isSelected = app.packageName in targetPackages)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
            )
        }
        persistSelectionAndApply()
        return selectedCount
    }

    fun exportPackages(): String {
        return _uiState.value.selectedPackages.joinToString("\n")
    }

    fun importPackages(text: String): Int {
        val packages = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        _uiState.update { state ->
            val validPackages = packages.intersect(state.apps.map { it.packageName }.toSet())
            val newSelectedPackages = state.selectedPackages + validPackages

            val newApps = state.apps.map { app ->
                if (validPackages.contains(app.packageName)) {
                    app.copy(isSelected = true)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
            )
        }

        persistSelectionAndApply()
        return packages.intersect(_uiState.value.apps.map { it.packageName }.toSet()).size
    }

    private fun persistSelectionAndApply() {
        repository.accessControlPackages.set(_uiState.value.selectedPackages)

        applyPackagesJob?.cancel()
        applyPackagesJob = viewModelScope.launch {
            PollingTimers.awaitTick(
                PollingTimerSpecs.dynamic(
                    name = "access_control_apply_packages",
                    intervalMillis = 350L,
                    initialDelayMillis = 350L,
                ),
            )

            if (!proxyFacade.isRunning.value) return@launch
            val activeMode = RuntimeStateMapper.modeForOwner(proxyFacade.runtimeSnapshot.value.owner)
            if (activeMode == ProxyMode.Http) return@launch
            if (repository.accessControlMode.value == AccessControlMode.ALLOW_ALL) return@launch

            runCatching {
                proxyFacade.startProxy(activeMode ?: repository.proxyMode.value)
            }
        }
    }

    private fun isChinaPackage(packageName: String): Boolean {
        val normalized = packageName.lowercase()
        skipPrefixList.forEach {
            if (normalized == it || normalized.startsWith("$it.")) {
                return false
            }
        }
        if (normalized.startsWith("cn.") || normalized.contains(".cn.") || normalized.endsWith(".cn")) {
            return true
        }
        return normalized.matches(chinaAppRegex)
    }

    companion object {
        private val skipPrefixList = listOf(
            "com.google",
            "com.android.chrome",
            "com.android.vending",
            "com.microsoft",
            "com.apple",
            "com.zhiliaoapp.musically",
            "com.android.providers.downloads",
        )

        private val chinaAppPrefixList = listOf(
            "com.tencent",
            "com.tencent.mobileqq",
            "com.tencent.mm",
            "com.tencent.qqlive",
            "com.tencent.news",
            "com.tencent.wework",
            "com.tencent.weishi",
            "com.tencent.karaoke",
            "com.tencent.qqmusic",
            "com.alibaba",
            "com.alibaba.android",
            "com.alibaba.wireless",
            "com.alibaba.rimet",
            "com.umeng",
            "com.qihoo",
            "com.ali",
            "com.alipay",
            "com.amap",
            "com.sina",
            "com.weibo",
            "com.sankuai",
            "com.sankuai.meituan",
            "com.sankuai.meituan.takeoutnew",
            "com.dianping",
            "com.jingdong",
            "com.xunmeng",
            "com.xingin",
            "com.zhihu",
            "com.bilibili",
            "com.coolapk",
            "tv.danmaku",
            "com.kuaishou",
            "com.smile.gifmaker",
            "com.ss.android",
            "com.ss.android.ugc",
            "com.ss.android.article",
            "com.qiyi",
            "com.youku",
            "com.youku.phone",
            "com.sohu",
            "com.autonavi",
            "com.sogou",
            "com.sogou.inputmethod",
            "com.iflytek",
            "com.iflytek.inputmethod",
            "com.kingsoft",
            "com.qzone",
            "com.vivo",
            "com.xiaomi",
            "com.huawei",
            "com.taobao",
            "com.taobao.idlefish",
            "com.secneo",
            "s.h.e.l.l",
            "com.stub",
            "com.kiwisec",
            "com.secshell",
            "com.wrapper",
            "cn.securitystack",
            "com.mogosec",
            "com.secoen",
            "com.netease",
            "com.mx",
            "com.qq.e",
            "com.baidu",
            "com.bytedance",
            "com.bugly",
            "com.miui",
            "com.oppo",
            "com.coloros",
            "com.iqoo",
            "com.meizu",
            "com.gionee",
            "com.oplus",
            "andes.oplus",
            "com.unionpay",
        )

        private val chinaAppRegex by lazy {
            ("(" + chinaAppPrefixList.joinToString("|").replace(".", "\\.") + ").*").toRegex()
        }
    }
}
