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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import dev.oom_wg.purejoy.mlang.MLang

class AccessControlViewModel(
    application: Application,
    private val storage: NetworkSettingsStorage,
) : AndroidViewModel(application) {

    companion object {
        private const val MIUI_GET_INSTALLED_APPS_PERMISSION = "com.android.permission.GET_INSTALLED_APPS"
        private const val SELF_PACKAGE_NAME = "com.github.yumelira.yumebox"
        private const val PERMISSION_REQUEST_CODE = 999
    }

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable?,
        val isSystemApp: Boolean,
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
        val isLoading: Boolean = true,
        val apps: List<AppInfo> = emptyList(),
        val filteredApps: List<AppInfo> = emptyList(),
        val selectedPackages: Set<String> = emptySet(),
        val searchQuery: String = "",
        val showSystemApps: Boolean = false,
        val sortMode: SortMode = SortMode.LABEL,
        val descending: Boolean = false,
        val selectedFirst: Boolean = true,
        val permissionGranted: Boolean = false,
        val isMiuiSystem: Boolean = false,
        val permissionCheckCompleted: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoadApps()
    }

    private fun checkPermissionAndLoadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, permissionCheckCompleted = false) }

            val isMiuiSystem = checkIsMiuiSystem()
            val permissionGranted = if (isMiuiSystem) {
                checkMiuiPermission()
            } else {
                true
            }

            Log.d("AccessControlVM", "权限检查: MIUI=$isMiuiSystem, 权限=$permissionGranted")

            _uiState.update { state ->
                state.copy(
                    isMiuiSystem = isMiuiSystem,
                    permissionGranted = permissionGranted,
                    permissionCheckCompleted = true
                )
            }

            if (permissionGranted) {
                loadApps()
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun checkIsMiuiSystem(): Boolean {
        return try {
            val pm = getApplication<Application>().packageManager
            val permissionInfo = pm.getPermissionInfo(MIUI_GET_INSTALLED_APPS_PERMISSION, 0)
            val isMiui = permissionInfo != null && permissionInfo.packageName == "com.lbe.security.miui"
            Log.d("AccessControlVM", "MIUI系统检测结果: $isMiui, 权限包名: ${permissionInfo?.packageName}")
            isMiui
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("AccessControlVM", "非MIUI系统: ${e.message}")
            false
        }
    }

    private fun checkMiuiPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            getApplication(),
            MIUI_GET_INSTALLED_APPS_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d("AccessControlVM", "MIUI权限检查结果: $hasPermission")
        return hasPermission
    }

    
    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val selectedPackages = storage.accessControlPackages.value
            val apps = withContext(Dispatchers.IO) {
                loadInstalledApps(selectedPackages)
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    apps = apps,
                    selectedPackages = selectedPackages,
                    filteredApps = filterApps(apps, state.searchQuery, state.showSystemApps, state.sortMode, state.descending, state.selectedFirst)
                )
            }
        }
    }

    private fun loadInstalledApps(selectedPackages: Set<String>): List<AppInfo> {
        val pm = getApplication<Application>().packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return packages
            .filter { appInfo -> appInfo.packageName != SELF_PACKAGE_NAME }
            .map { appInfo ->
                val pkgInfo = runCatching { pm.getPackageInfo(appInfo.packageName, 0) }.getOrNull()
                AppInfo(
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(pm).toString(),
                    icon = runCatching { appInfo.loadIcon(pm) }.getOrNull(),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isSelected = selectedPackages.contains(appInfo.packageName),
                    installTime = pkgInfo?.firstInstallTime ?: 0L,
                    updateTime = pkgInfo?.lastUpdateTime ?: 0L
                )
            }
    }

    private fun filterApps(
        apps: List<AppInfo>,
        query: String,
        showSystemApps: Boolean,
        sortMode: SortMode = SortMode.LABEL,
        descending: Boolean = false,
        selectedFirst: Boolean = true
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
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredApps = filterApps(state.apps, query, state.showSystemApps, state.sortMode, state.descending, state.selectedFirst)
            )
        }
    }

    fun onSortModeChange(mode: SortMode) {
        _uiState.update { state ->
            state.copy(
                sortMode = mode,
                filteredApps = filterApps(state.apps, state.searchQuery, state.showSystemApps, mode, state.descending, state.selectedFirst)
            )
        }
    }

    fun onDescendingChange(desc: Boolean) {
        _uiState.update { state ->
            state.copy(
                descending = desc,
                filteredApps = filterApps(state.apps, state.searchQuery, state.showSystemApps, state.sortMode, desc, state.selectedFirst)
            )
        }
    }

    fun onSelectedFirstChange(selectedFirst: Boolean) {
        _uiState.update { state ->
            state.copy(
                selectedFirst = selectedFirst,
                filteredApps = filterApps(state.apps, state.searchQuery, state.showSystemApps, state.sortMode, state.descending, selectedFirst)
            )
        }
    }

    fun onShowSystemAppsChange(show: Boolean) {
        _uiState.update { state ->
            state.copy(
                showSystemApps = show,
                filteredApps = filterApps(state.apps, state.searchQuery, show, state.sortMode, state.descending, state.selectedFirst)
            )
        }
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
                filteredApps = filterApps(newApps, state.searchQuery, state.showSystemApps, state.sortMode, state.descending, state.selectedFirst)
            )
        }


        val packagesToSave = _uiState.value.selectedPackages
        Log.d("AccessControlVM", "保存应用列表: $packagesToSave (数量: ${packagesToSave.size})")
        storage.accessControlPackages.set(packagesToSave)
    }

    fun selectAll() {
        _uiState.update { state ->
            val allPackages = state.filteredApps.map { it.packageName }.toSet()
            val newSelectedPackages = state.selectedPackages + allPackages

            val newApps = state.apps.map { app ->
                if (allPackages.contains(app.packageName)) {
                    app.copy(isSelected = true)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
                filteredApps = filterApps(newApps, state.searchQuery, state.showSystemApps, state.sortMode, state.descending, state.selectedFirst)
            )
        }

        storage.accessControlPackages.set(_uiState.value.selectedPackages)
    }

    fun deselectAll() {
        _uiState.update { state ->
            val allPackages = state.filteredApps.map { it.packageName }.toSet()
            val newSelectedPackages = state.selectedPackages - allPackages

            val newApps = state.apps.map { app ->
                if (allPackages.contains(app.packageName)) {
                    app.copy(isSelected = false)
                } else {
                    app
                }
            }

            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
                filteredApps = filterApps(newApps, state.searchQuery, state.showSystemApps, state.sortMode, state.descending, state.selectedFirst)
            )
        }

        storage.accessControlPackages.set(_uiState.value.selectedPackages)
    }

    fun invertSelection() {
        _uiState.update { state ->
            val allPackages = state.filteredApps.map { it.packageName }.toSet()
            val newSelectedPackages = state.selectedPackages.toMutableSet()
            allPackages.forEach { pkg ->
                if (newSelectedPackages.contains(pkg)) newSelectedPackages.remove(pkg) else newSelectedPackages.add(pkg)
            }
            val newApps = state.apps.map { app ->
                if (allPackages.contains(app.packageName)) {
                    app.copy(isSelected = !app.isSelected)
                } else {
                    app
                }
            }
            state.copy(
                selectedPackages = newSelectedPackages,
                apps = newApps,
                filteredApps = filterApps(newApps, state.searchQuery, state.showSystemApps, state.sortMode, state.descending, state.selectedFirst)
            )
        }
        storage.accessControlPackages.set(_uiState.value.selectedPackages)
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
                filteredApps = filterApps(newApps, state.searchQuery, state.showSystemApps, state.sortMode, state.descending, state.selectedFirst)
            )
        }
        
        storage.accessControlPackages.set(_uiState.value.selectedPackages)
        return packages.intersect(_uiState.value.apps.map { it.packageName }.toSet()).size
    }
}
