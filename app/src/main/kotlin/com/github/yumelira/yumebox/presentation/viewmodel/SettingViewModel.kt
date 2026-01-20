package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.data.store.FeatureStore
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SettingViewModel(
    private val featureStore: FeatureStore,
) : ViewModel() {

    val allowLanAccess = featureStore.allowLanAccess
    val backendPort = featureStore.backendPort
    val frontendPort = featureStore.frontendPort

    private val _events = MutableSharedFlow<SettingEvent>()
    val events: SharedFlow<SettingEvent> = _events.asSharedFlow()

    fun onSubStoreCardClicked(isAllowed: Boolean = false) {
        if (!isAllowed) return
        val host = currentHost()
        val frontendUrl = buildUrl(host, frontendPort.value)
        val backendUrl = buildUrl(host, backendPort.value)
        emitEvent(SettingEvent.OpenWebView("$frontendUrl/subs?api=$backendUrl", MLang.Settings.Function.SubStore))
    }

    private fun currentHost(): String = if (allowLanAccess.value) "0.0.0.0" else "127.0.0.1"

    private fun buildUrl(host: String, port: Int): String = "http://$host:$port"

    private fun emitEvent(event: SettingEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }
}

sealed interface SettingEvent {
    data class OpenWebView(val url: String, val title: String? = null) : SettingEvent
}
