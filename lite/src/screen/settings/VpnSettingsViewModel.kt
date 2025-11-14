package com.github.yumelira.yumebox.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.config.TunProfileSync
import com.github.yumelira.yumebox.core.util.PollingTimerSpecs
import com.github.yumelira.yumebox.core.util.PollingTimers
import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.data.repository.AppSettingsRepository
import com.github.yumelira.yumebox.data.repository.NetworkSettingsRepository
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.common.util.AppLanguageManager
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VpnSettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val networkSettingsRepository: NetworkSettingsRepository,
    private val proxyFacade: ProxyFacade,
    private val tunProfileSync: TunProfileSync,
) : ViewModel() {
    private var restartJob: Job? = null

    val themeMode: Preference<ThemeMode> = appSettingsRepository.themeMode
    val appLanguage: Preference<AppLanguage> = appSettingsRepository.appLanguage
    val dnsHijack: Preference<Boolean> = networkSettingsRepository.dnsHijack
    val allowBypass: Preference<Boolean> = networkSettingsRepository.allowBypass
    val enableIPv6: Preference<Boolean> = networkSettingsRepository.enableIPv6
    val systemProxy: Preference<Boolean> = networkSettingsRepository.systemProxy
    val tunStack: Preference<TunStack> = networkSettingsRepository.tunStack
    val isRunning: StateFlow<Boolean> = proxyFacade.runtimeSnapshot
        .map(RuntimeStateMapper::isActuallyRunning)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RuntimeStateMapper.isActuallyRunning(proxyFacade.runtimeSnapshot.value),
        )

    fun onDnsHijackChange(enabled: Boolean) {
        updatePreference(dnsHijack, enabled)
    }

    fun onThemeModeChange(mode: ThemeMode) {
        themeMode.set(mode)
    }

    fun onAppLanguageChange(language: AppLanguage) {
        appLanguage.set(language)
        AppLanguageManager.apply(language)
    }

    fun onAllowBypassChange(enabled: Boolean) {
        updatePreference(allowBypass, enabled)
    }

    fun onEnableIPv6Change(enabled: Boolean) {
        updatePreference(enableIPv6, enabled)
    }

    fun onSystemProxyChange(enabled: Boolean) {
        updatePreference(systemProxy, enabled)
    }

    fun onTunStackChange(stack: TunStack) {
        updatePreference(tunStack, stack)
    }

    private fun <T> updatePreference(
        preference: Preference<T>,
        value: T,
    ) {
        if (preference.value == value) return
        preference.set(value)
        scheduleRestart()
    }

    private fun scheduleRestart() {
        restartJob?.cancel()
        restartJob = viewModelScope.launch {
            PollingTimers.awaitTick(
                PollingTimerSpecs.dynamic(
                    name = "lite_vpn_settings_restart",
                    intervalMillis = 300L,
                    initialDelayMillis = 300L,
                ),
            )
            if (proxyFacade.isRunning.value) {
                try {
                    tunProfileSync.syncActiveProfile()
                    proxyFacade.startProxy(ProxyMode.Tun)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                }
            }
        }
    }
}
