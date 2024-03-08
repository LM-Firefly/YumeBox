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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.store.ProxyDisplaySettingsStore
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 代理视图模型
 *
 * 重构原则：
 * 1. 完全基于状态观察，不主动拉取数据
 * 2. 所有操作都是"设置"而非"刷新"
 * 3. UI 只负责显示和触发操作
 * 4. 数据同步由 ProxyStateRepository 自动完成
 */
class ProxyViewModel(
    private val proxyFacade: ProxyFacade,
    private val proxyDisplaySettingsStore: ProxyDisplaySettingsStore
) : ViewModel() {

    // ========== UI 状态 ==========

    private val _uiState = MutableStateFlow(ProxyUiState())
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    // ========== 代理状态（来自核心） ==========

    /**
     * 当前代理模式
     */
    val currentMode: StateFlow<TunnelState.Mode> = proxyDisplaySettingsStore.proxyMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, TunnelState.Mode.Rule)

    /**
     * 显示模式
     */
    val displayMode: StateFlow<ProxyDisplayMode> = proxyDisplaySettingsStore.displayMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxyDisplayMode.DOUBLE_SIMPLE)

    /**
     * 排序模式
     */
    val sortMode: StateFlow<ProxySortMode> = proxyDisplaySettingsStore.sortMode.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProxySortMode.DEFAULT)

    /**
     * 选中的代理组索引
     */
    private val _selectedGroupIndex = MutableStateFlow(0)
    val selectedGroupIndex: StateFlow<Int> = _selectedGroupIndex.asStateFlow()

    /**
     * 代理组列表（来自 ProxyStateRepository，自动同步）
     */
    val proxyGroups: StateFlow<List<ProxyGroupInfo>> = proxyFacade.proxyGroups


    /**
     * 排序后的代理组列表
     */
    val sortedProxyGroups: StateFlow<List<ProxyGroupInfo>> =
        combine(proxyGroups, sortMode) { groups, mode ->
            if (mode == ProxySortMode.DEFAULT) {
                groups
            } else {
                groups.map { group ->
                    group.copy(proxies = sortProxies(group.proxies, mode))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ========== 操作：代理模式切换 ==========

    /**
     * 切换代理模式
     *
     * @param mode 新的代理模式
     */
    fun patchMode(mode: TunnelState.Mode) {
        // 保存到设置
        proxyDisplaySettingsStore.proxyMode.set(mode)

        viewModelScope.launch {
            runCatching {
                // 写入持久化和会话 override
                val persistOverride = Clash.queryOverride(Clash.OverrideSlot.Persist)
                persistOverride.mode = mode
                Clash.patchOverride(Clash.OverrideSlot.Persist, persistOverride)

                val sessionOverride = Clash.queryOverride(Clash.OverrideSlot.Session)
                sessionOverride.mode = mode
                Clash.patchOverride(Clash.OverrideSlot.Session, sessionOverride)

                val modeName = when (mode) {
                    TunnelState.Mode.Direct -> "直连"
                    TunnelState.Mode.Global -> "全局"
                    TunnelState.Mode.Rule -> "规则"
                    else -> "未知"
                }
                showMessage("已切换到: $modeName 模式")
            }.onFailure { e ->
                Timber.e(e, "代理模式切换失败：$mode")
                showError("切换模式失败: ${e.message}")
            }
        }
    }

    // ========== 操作：测试延迟 ==========

    /**
     * 测试延迟
     *
     * 触发延迟测试（健康检查），测试完成后会自动同步状态
     * 用于下拉刷新和测试按钮
     *
     * @param groupName 代理组名称，null 表示测试所有组
     */
    fun testDelay(groupName: String? = null) {
        viewModelScope.launch {
            try {
                setLoading(true)
                clearError()

                if (groupName != null) {
                    // 测试指定组
                    showMessage("正在测试代理组: $groupName")

                    val result = proxyFacade.testDelay(groupName)
                    if (result.isSuccess) {
                        showMessage("刷新延迟成功")
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "未知错误"
                        Timber.e("代理组延迟测试失败：$groupName - $error")
                        showError("测试失败: $error")
                    }
                } else {
                    // 测试所有组
                    showMessage("正在测试所有代理组...")

                    val result = proxyFacade.testAllDelay()
                    if (result.isSuccess) {
                        showMessage("刷新延迟成功")
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "未知错误"
                        Timber.e("所有代理组延迟测试失败：$error")
                        showError("测试失败: $error")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "延迟测试异常")
                showError("测试失败: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    // ========== 操作：选择代理节点 ==========

    /**
     * 选择代理节点
     *
     * 注意：只有 Selector 类型的代理组才能选择节点
     * 选择后会自动同步状态
     *
     * @param groupName 代理组名称
     * @param proxyName 代理节点名称
     */
    fun selectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            try {
                val result = proxyFacade.selectProxy(groupName, proxyName)

                if (result.isSuccess && result.getOrNull() == true) {
                    showMessage("已切换到: $proxyName")
                } else {
                    val error = result.exceptionOrNull()
                    if (error?.message?.contains("只有 Selector") == true) {
                        Timber.w("代理组类型不支持选择: $groupName")
                        showError("该代理组不支持手动选择节点")
                    } else {
                        Timber.w("代理节点选择失败: $groupName -> $proxyName")
                        showError("切换失败")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "代理节点选择异常: $groupName -> $proxyName")
                showError("切换失败: ${e.message}")
            }
        }
    }

    // ========== 操作：代理组选择 ==========

    /**
     * 设置选中的代理组
     *
     * @param index 代理组索引
     */
    fun setSelectedGroup(index: Int) {
        val groups = proxyGroups.value
        val newIndex = index.coerceIn(0, (groups.size - 1).coerceAtLeast(0))

        if (newIndex != _selectedGroupIndex.value) {
            _selectedGroupIndex.value = newIndex
        }
    }

    // ========== 操作：显示设置 ==========

    /**
     * 切换显示模式
     */
    fun toggleDisplayMode() {
        val current = displayMode.value
        val newMode = when (current) {
            ProxyDisplayMode.SINGLE_DETAILED -> ProxyDisplayMode.SINGLE_SIMPLE
            ProxyDisplayMode.SINGLE_SIMPLE -> ProxyDisplayMode.DOUBLE_DETAILED
            ProxyDisplayMode.DOUBLE_DETAILED -> ProxyDisplayMode.DOUBLE_SIMPLE
            ProxyDisplayMode.DOUBLE_SIMPLE -> ProxyDisplayMode.SINGLE_DETAILED
        }
        setDisplayMode(newMode)
    }

    /**
     * 设置显示模式
     *
     * @param mode 显示模式
     */
    fun setDisplayMode(mode: ProxyDisplayMode) {
        proxyDisplaySettingsStore.displayMode.set(mode)
    }

    /**
     * 切换排序模式
     */
    fun toggleSortMode() {
        val current = sortMode.value
        val newMode = when (current) {
            ProxySortMode.DEFAULT -> ProxySortMode.BY_NAME
            ProxySortMode.BY_NAME -> ProxySortMode.BY_LATENCY
            ProxySortMode.BY_LATENCY -> ProxySortMode.DEFAULT
        }
        setSortMode(newMode)
    }

    /**
     * 设置排序模式
     *
     * @param mode 排序模式
     */
    fun setSortMode(mode: ProxySortMode) {
        proxyDisplaySettingsStore.sortMode.set(mode)
    }

    // ========== 工具方法 ==========

    /**
     * 排序代理列表
     */
    private fun sortProxies(proxies: List<Proxy>, sortMode: ProxySortMode): List<Proxy> = when (sortMode) {
        ProxySortMode.DEFAULT -> proxies
        ProxySortMode.BY_NAME -> proxies.sortedBy { it.name }
        ProxySortMode.BY_LATENCY -> proxies.sortedWith(
            compareBy { proxy ->
                when {
                    proxy.delay < 0 -> Int.MAX_VALUE - 1  // TIMEOUT 排在最后
                    proxy.delay == 0 -> Int.MAX_VALUE     // N/A 排在最后
                    else -> proxy.delay
                }
            }
        )
    }

    /**
     * 设置加载状态
     */
    private fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    /**
     * 显示消息
     */
    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    /**
     * 显示错误
     */
    private fun showError(error: String) {
        _uiState.update { it.copy(error = error) }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    // ========== 数据类 ==========

    /**
     * UI 状态
     *
     * @property isLoading 是否正在加载
     * @property message 消息
     * @property error 错误消息
     */
    data class ProxyUiState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
}
