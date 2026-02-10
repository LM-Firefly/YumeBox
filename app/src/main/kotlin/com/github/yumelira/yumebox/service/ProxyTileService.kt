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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.R
import com.github.yumelira.yumebox.domain.facade.ProfilesRepository
import com.github.yumelira.yumebox.domain.facade.ProxyFacade
import com.github.yumelira.yumebox.remote.VpnPermissionRequired
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject
import timber.log.Timber

@SuppressLint("NewApi")
class ProxyTileService : TileService() {

    companion object {
        private const val TAG = "ProxyTileService"
    }

    private val proxyFacade: ProxyFacade by inject()
    private val profilesRepository: ProfilesRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateJob?.cancel()
        updateJob = scope.launch {
            proxyFacade.isRunning.collect { running ->
                updateTileState(running)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        updateJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val isRunning = proxyFacade.isRunning.first()
            
            // Sync with actual state if inconsistent
            val tileState = qsTile?.state
            if ((isRunning && tileState == Tile.STATE_INACTIVE) || (!isRunning && tileState == Tile.STATE_ACTIVE)) {
                updateTileState(isRunning)
                return@launch
            }

            try {
                if (isRunning) {
                    proxyFacade.stopProxy()
                } else {
                    val activeProfile = profilesRepository.queryActiveProfile()
                    if (activeProfile == null) {
                        // Open app to select profile
                        val intent = Intent(this@ProxyTileService, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivityAndCollapseCompat(intent)
                        return@launch
                    }
                    
                    try {
                        // Defaulting to Tun mode for Tile quick start
                        proxyFacade.startProxy(useTun = true)
                    } catch (e: VpnPermissionRequired) {
                        val intent = e.intent
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivityAndCollapseCompat(intent)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling proxy from tile")
            }
        }
    }

    private fun startActivityAndCollapseCompat(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState(isRunning: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        
        tile.label = if (isRunning) {
            MLang.Home.Control.Stop
        } else {
            MLang.Home.Control.Start
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isRunning) {
                MLang.Service.Notification.Connected
            } else {
                MLang.Home.Message.ProxyStopped
            }
        }

        tile.icon = Icon.createWithResource(
            this,
            if (isRunning) R.drawable.ic_logo_service else R.drawable.ic_logo_service
        )
        
        tile.updateTile()
    }
}
