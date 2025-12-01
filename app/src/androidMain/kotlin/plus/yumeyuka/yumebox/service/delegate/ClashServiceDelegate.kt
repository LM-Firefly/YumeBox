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

package com.github.yumelira.yumebox.service.delegate

import android.app.Service
import kotlinx.coroutines.*
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.service.loader.ServiceProfileLoader
import com.github.yumelira.yumebox.service.notification.ServiceNotificationManager

class ClashServiceDelegate(
    private val service: Service,
    private val clashManager: ClashManager,
    private val profilesStore: ProfilesStore,
    private val appSettingsStorage: AppSettingsStorage,
    private val notificationConfig: ServiceNotificationManager.Config
) {
    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val profileLoader by lazy { ServiceProfileLoader(clashManager, profilesStore) }
    val notificationManager by lazy { ServiceNotificationManager(service, notificationConfig) }
    
    private var notificationJob: Job? = null
    var currentProfileId: String? = null
        private set

    fun initialize() {
        notificationManager.createChannel()
    }

    suspend fun loadProfileIfNeeded(
        profileId: String,
        willUseTunMode: Boolean,
        quickStart: Boolean = false
    ): Result<Profile> {
        return profileLoader.loadIfNeeded(profileId, willUseTunMode, quickStart)
            .onSuccess { currentProfileId = profileId }
    }

    fun showErrorNotification(title: String, content: String) {
        service.startForeground(
            notificationConfig.notificationId,
            notificationManager.create(title, content, false)
        )
        serviceScope.launch {
            delay(3000)
            service.stopSelf()
        }
    }

    fun startNotificationUpdate() {
        notificationJob?.cancel()
        notificationJob = notificationManager.startTrafficUpdate(
            serviceScope, clashManager, appSettingsStorage
        )
    }

    fun stopNotificationUpdate() {
        notificationJob?.cancel()
        notificationJob = null
    }

    fun cleanup() {
        stopNotificationUpdate()
        currentProfileId = null
        serviceScope.cancel()
    }
}
