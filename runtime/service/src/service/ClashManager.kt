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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.service

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.service.remote.ILogObserver
import com.github.yumelira.yumebox.service.runtime.util.sendBroadcastSelf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClashManager(private val context: Context) :
    CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private var logReceiver: ReceiveChannel<LogMessage>? = null

    fun requestStop() {
        runCatching { context.sendBroadcastSelf(Intent(Intents.ACTION_CLASH_REQUEST_STOP)) }

        runCatching {
            context.stopService(Intent(context, TunService::class.java))
            context.stopService(Intent(context, ClashService::class.java))
        }

        runCatching {
            Clash.stopHttp()
            Clash.stopTun()
            Clash.reset()
        }
    }

    fun setLogObserver(observer: ILogObserver?) {
        synchronized(this) {
            logReceiver?.apply {
                cancel()

                Clash.forceGc()
            }

            if (observer != null) {
                logReceiver =
                    Clash.subscribeLogcat().also { receiver ->
                        launch {
                            try {
                                while (isActive) {
                                    observer.newItem(receiver.receive())
                                }
                            } catch (_: CancellationException) {} catch (error: Exception) {
                                Log.w("UI crashed", error)
                            } finally {
                                withContext(NonCancellable) {
                                    receiver.cancel()

                                    Clash.forceGc()
                                }
                            }
                        }
                    }
            }
        }
    }
}
