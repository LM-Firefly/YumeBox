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

package com.github.yumelira.yumebox.runtime.client.internal

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import com.github.yumelira.yumebox.runtime.api.service.common.constants.Intents
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

internal sealed interface ProxyServiceEvent {
    data object ClashStarted : ProxyServiceEvent
    data class ClashStopped(val reason: String?) : ProxyServiceEvent
    data object ProfileChanged : ProxyServiceEvent
    data object ProfileLoaded : ProxyServiceEvent
    data object OverrideChanged : ProxyServiceEvent
    data object ServiceRecreated : ProxyServiceEvent
    data class RootRuntimeFailed(val error: String?) : ProxyServiceEvent
}

internal class ProxyEventBus(private val appContext: Context) {
    private val _events = MutableSharedFlow<ProxyServiceEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ProxyServiceEvent> = _events.asSharedFlow()
    private val _screenOn = MutableStateFlow(
        appContext.getSystemService(PowerManager::class.java)?.isInteractive != false,
    )
    val screenOn: StateFlow<Boolean> = _screenOn.asStateFlow()
    val actionClashRequestStop: String get() = Intents.actionClashRequestStop(appContext.packageName)
    private val actionServiceRecreated: String get() = Intents.actionServiceRecreated(appContext.packageName)
    private val actionClashStarted: String get() = Intents.actionClashStarted(appContext.packageName)
    private val actionClashStopped: String get() = Intents.actionClashStopped(appContext.packageName)
    private val actionProfileChanged: String get() = Intents.actionProfileChanged(appContext.packageName)
    private val actionProfileLoaded: String get() = Intents.actionProfileLoaded(appContext.packageName)
    private val actionOverrideChanged: String get() = Intents.actionOverrideChanged(appContext.packageName)
    private val actionRootRuntimeFailed: String get() = Intents.actionRootRuntimeFailed(appContext.packageName)
    private val serviceEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val event = when (intent?.action ?: return) {
                actionClashStarted -> ProxyServiceEvent.ClashStarted
                actionClashStopped -> ProxyServiceEvent.ClashStopped(
                    intent.getStringExtra(Intents.EXTRA_STOP_REASON),
                )
                actionProfileLoaded -> ProxyServiceEvent.ProfileLoaded
                actionProfileChanged -> ProxyServiceEvent.ProfileChanged
                actionOverrideChanged -> ProxyServiceEvent.OverrideChanged
                actionServiceRecreated -> ProxyServiceEvent.ServiceRecreated
                actionRootRuntimeFailed -> ProxyServiceEvent.RootRuntimeFailed(
                    intent.getStringExtra("error"),
                )
                else -> return
            }
            _events.tryEmit(event)
        }
    }
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> _screenOn.value = true
                Intent.ACTION_SCREEN_OFF -> _screenOn.value = false
            }
        }
    }
    private val serviceEventsRegistered = AtomicBoolean(false)
    private val screenStateRegistered = AtomicBoolean(false)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun register() {
        registerServiceEventReceiver()
        registerScreenStateReceiver()
    }
    fun unregister() {
        if (serviceEventsRegistered.compareAndSet(true, false)) {
            runCatching { appContext.unregisterReceiver(serviceEventsReceiver) }
                .onFailure { Timber.w(it, "Failed to unregister service event receiver") }
        }
        if (screenStateRegistered.compareAndSet(true, false)) {
            runCatching { appContext.unregisterReceiver(screenStateReceiver) }
                .onFailure { Timber.w(it, "Failed to unregister screen state receiver") }
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerServiceEventReceiver() {
        if (!serviceEventsRegistered.compareAndSet(false, true)) return
        val filter = IntentFilter().apply {
            addAction(actionClashStarted)
            addAction(actionClashStopped)
            addAction(actionProfileChanged)
            addAction(actionProfileLoaded)
            addAction(actionOverrideChanged)
            addAction(actionServiceRecreated)
            addAction(actionRootRuntimeFailed)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(serviceEventsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(serviceEventsReceiver, filter)
            }
        }.onFailure { error ->
            serviceEventsRegistered.set(false)
            Timber.w(error, "Failed to register service event receiver")
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerScreenStateReceiver() {
        if (!screenStateRegistered.compareAndSet(false, true)) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(screenStateReceiver, filter)
            }
        }.onFailure { error ->
            screenStateRegistered.set(false)
            Timber.w(error, "Failed to register screen state receiver")
        }
    }
}
