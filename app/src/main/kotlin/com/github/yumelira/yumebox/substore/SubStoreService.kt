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

package com.github.yumelira.yumebox.substore

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.github.yumelira.yumebox.App
import com.github.yumelira.yumebox.common.native.NativeLibraryManager
import dev.oom_wg.purejoy.mlang.MLang
import timber.log.Timber

class SubStoreService : Service() {

    companion object {
        var caseEngine: CaseEngine? = null
        var isRunning: Boolean = false
            private set

        fun startService(frontendPort: Int = 8080, backendPort: Int = 8081, allowLan: Boolean = false) {
            val context = com.github.yumelira.yumebox.App.instance
            val intent = Intent(context, SubStoreService::class.java).apply {
                putExtra("frontendPort", frontendPort)
                putExtra("backendPort", backendPort)
                putExtra("allowLan", allowLan)
            }
            context.startService(intent)
        }

        fun stopService() {
            val context = com.github.yumelira.yumebox.App.instance
            val intent = Intent(context, SubStoreService::class.java)
            context.stopService(intent)
        }
    }
    private val TAG = "SubStoreService"

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val frontendPort = intent?.getIntExtra("frontendPort", 8080) ?: 8080
            val backendPort = intent?.getIntExtra("backendPort", 8081) ?: 8081
            val allowLan = intent?.getBooleanExtra("allowLan", false) ?: false

            if (NetworkUtil.isPortInUse(frontendPort) || NetworkUtil.isPortInUse(backendPort)) {
                throw Exception("端口 $frontendPort 或 $backendPort 已被占用")
            }

            if (!ensureJavetLibraryLoaded()) {
                throw Exception("Javet native 库加载失败")
            }

            if (caseEngine != null && isRunning) {
                if (caseEngine?.matches(frontendPort, backendPort, allowLan) == true) {
                    return START_STICKY
                }
                caseEngine?.stopServer()
                caseEngine = null
                isRunning = false
            } else {
                caseEngine?.stopServer()
                caseEngine = null
            }

            val engine = CaseEngine(
                backendPort = backendPort,
                frontendPort = frontendPort,
                allowLan = allowLan
            )
            caseEngine = engine

            engine.startServer()
            isRunning = true

            return START_STICKY
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
            return START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            caseEngine?.stopServer()
            caseEngine = null
            isRunning = false
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to stop Sub-Store service")
        }
    }

    private fun ensureJavetLibraryLoaded(): Boolean {
        try {
            System.loadLibrary("javet-node-android")
            Timber.tag(TAG).d(MLang.Feature.SubStore.JavetSystemLoadSuccess)
            return true
        } catch (e: Throwable) {
            Timber.tag(TAG).w(MLang.Feature.SubStore.JavetSystemLoadFailed.format(e.message ?: ""))
        }

        return try {
            NativeLibraryManager.initialize(applicationContext)
            val javetLibBaseName = "libjavet-node-android"

            if (NativeLibraryManager.isLibraryAvailable(javetLibBaseName)) {
                Timber.tag(TAG).d(MLang.Feature.SubStore.JavetLibraryAvailable)
            } else {
                Timber.tag(TAG).d(MLang.Feature.SubStore.JavetLibraryNotExist)
                val results = NativeLibraryManager.extractAllLibraries()
                Timber.tag(TAG).d(MLang.Feature.SubStore.BackendExtractResult.format(results.toString()))
                if (results[javetLibBaseName] != true) {
                    Timber.tag(TAG).e(MLang.Feature.SubStore.BackendExtractFailed)
                    return false
                }
            }

            val loaded = NativeLibraryManager.loadJniLibrary(javetLibBaseName)
            if (loaded) {
                Timber.tag(TAG).d(MLang.Feature.SubStore.JavetLoadSuccess)
            } else {
                Timber.tag(TAG).e(MLang.Feature.SubStore.JavetLoadFailed.format(NativeLibraryManager.getLibraryStatus(javetLibBaseName)))
            }
            loaded
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, MLang.Feature.SubStore.JavetLoadException)
            e.printStackTrace()
            false
        }
    }
}
