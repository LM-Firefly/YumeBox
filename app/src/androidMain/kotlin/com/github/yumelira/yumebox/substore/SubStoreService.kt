package com.github.yumelira.yumebox.substore

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.github.yumelira.yumebox.App
import com.github.yumelira.yumebox.common.native.NativeLibraryManager
import dev.oom_wg.purejoy.mlang.MLang

class SubStoreService : Service() {

    companion object {
        var caseEngine: CaseEngine? = null
        var isRunning: Boolean = false
            private set

        fun startService(frontendPort: Int = 8080, backendPort: Int = 8081, allowLan: Boolean = false) {
            val context = App.instance
            val intent = Intent(context, SubStoreService::class.java).apply {
                putExtra("frontendPort", frontendPort)
                putExtra("backendPort", backendPort)
                putExtra("allowLan", allowLan)
            }
            context.startService(intent)
        }

        fun stopService() {
            val context = App.instance
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
                throw Exception(MLang.Feature.SubStore.PortOccupied.format(frontendPort, backendPort))
            }

            if (!ensureJavetLibraryLoaded()) {
                throw Exception(MLang.Feature.SubStore.JavetNotReady)
            }

            val engine = CaseEngine(
                backendPort = backendPort,
                frontendPort = frontendPort,
                allowLan = allowLan
            )
            caseEngine = engine

            if (!engine.isInitialized()) {
                throw Exception(MLang.Feature.SubStore.CaseEngineInitFailed)
            }

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
            e.printStackTrace()
        }
    }

    private fun ensureJavetLibraryLoaded(): Boolean {
        try {
            System.loadLibrary("javet-node-android")
            Log.d(TAG, MLang.Feature.SubStore.JavetSystemLoadSuccess)
            return true
        } catch (e: Throwable) {
            Log.w(TAG, MLang.Feature.SubStore.JavetSystemLoadFailed.format(e.message ?: ""))
        }

        return try {
            NativeLibraryManager.initialize(applicationContext)
            val javetLibBaseName = "libjavet-node-android"

            if (NativeLibraryManager.isLibraryAvailable(javetLibBaseName)) {
                Log.d(TAG, MLang.Feature.SubStore.JavetLibraryAvailable)
            } else {
                Log.d(TAG, MLang.Feature.SubStore.JavetLibraryNotExist)
                val results = NativeLibraryManager.extractAllLibraries()
                Log.d(TAG, MLang.Feature.SubStore.BackendExtractResult.format(results.toString()))
                if (results[javetLibBaseName] != true) {
                    Log.e(TAG, MLang.Feature.SubStore.BackendExtractFailed)
                    return false
                }
            }

            val loaded = NativeLibraryManager.loadJniLibrary(javetLibBaseName)
            if (loaded) {
                Log.d(TAG, MLang.Feature.SubStore.JavetLoadSuccess)
            } else {
                Log.e(TAG, MLang.Feature.SubStore.JavetLoadFailed.format(NativeLibraryManager.getLibraryStatus(javetLibBaseName)))
            }
            loaded
        } catch (e: Exception) {
            Log.e(TAG, MLang.Feature.SubStore.JavetLoadException, e)
            e.printStackTrace()
            false
        }
    }
}
