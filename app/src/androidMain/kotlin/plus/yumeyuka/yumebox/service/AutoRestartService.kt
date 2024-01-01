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

package com.github.yumelira.yumebox.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.data.repository.ProxyConnectionService
import com.github.yumelira.yumebox.data.store.AppSettingsStorage
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.ProfilesStore
import com.github.yumelira.yumebox.common.util.ProxyAutoStartHelper
import timber.log.Timber

class AutoRestartService : Service() {

    companion object {
        private const val TAG = "AutoRestartService"
    }

    private val appSettingsStorage: AppSettingsStorage by inject()
    private val networkSettingsStorage: NetworkSettingsStorage by inject()
    private val profilesStore: ProfilesStore by inject()
    private val clashManager: ClashManager by inject()
    private val proxyConnectionService: ProxyConnectionService by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).d("AutoRestartService 启动")
        
        serviceScope.launch {
            try {
                ProxyAutoStartHelper.checkAndAutoStart(
                    proxyConnectionService = proxyConnectionService,
                    appSettingsStorage = appSettingsStorage,
                    networkSettingsStorage = networkSettingsStorage,
                    profilesStore = profilesStore,
                    clashManager = clashManager,
                    isBootCompleted = true
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "自动启动失败: ${e.message}")
            }
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}