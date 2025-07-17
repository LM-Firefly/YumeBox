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

package com.github.yumelira.yumebox.service

import android.app.Service
import com.github.yumelira.yumebox.service.clash.ClashRuntime
import com.github.yumelira.yumebox.service.notification.ServiceNotificationManager
import com.github.yumelira.yumebox.service.runtime.util.sendClashStarted
import com.github.yumelira.yumebox.service.runtime.util.sendClashStopped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

object ServiceCommons {

    fun onCreate(
        service: Service,
        notificationManager: ServiceNotificationManager,
        notificationConfig: ServiceNotificationManager.Config
    ): Boolean {
        if (StatusProvider.serviceRunning) {
            return true
        }

        StatusProvider.serviceRunning = true

        com.github.yumelira.yumebox.service.common.util.CoreRuntimeConfig.applyCustomUserAgentIfPresent(service)

        notificationManager.createChannel()
        service.startForeground(
            notificationConfig.notificationId,
            notificationManager.createInitialNotification()
        )

        return false
    }

    fun onStartCommand(
        service: Service,
        notificationManager: ServiceNotificationManager,
        notificationJob: Job?
    ): Job {
        service.sendClashStarted()
        if (notificationJob?.isActive != true) {
            return notificationManager.startTrafficUpdate(CoroutineScope(Dispatchers.Main))
        }
        return notificationJob
    }

    fun onDestroy(
        service: Service,
        notificationJob: Job?,
        reason: String?
    ) {
        notificationJob?.cancel()

        StatusProvider.serviceRunning = false

        service.sendClashStopped(reason)

        com.github.yumelira.yumebox.service.common.log.Log.i("Service destroyed: ${reason ?: "successfully"}")
    }

    fun onTrimMemory(runtime: ClashRuntime) {
        runtime.requestGc()
    }
}
