package com.github.yumelira.yumebox.substore

import com.github.yumelira.yumebox.App
import java.io.File

object SubStorePaths {

    private const val BASE_DIR = "SubStore"
    private const val FRONTEND_DIR = "frontend"
    private const val BACKEND_DIR = "backend"
    private const val DATA_DIR = "data"
    private const val BACKEND_BUNDLE = "sub-store.bundle.js"

    private val baseDir: File
        get() = File(App.instance.filesDir, BASE_DIR)

    val frontendDir: File
        get() = File(baseDir, FRONTEND_DIR)

    val backendDir: File
        get() = File(baseDir, BACKEND_DIR)

    val dataDir: File
        get() = File(baseDir, DATA_DIR)

    val backendBundle: File
        get() = File(backendDir, BACKEND_BUNDLE)

    val workingDir: File
        get() = baseDir

    fun ensureStructure() {
        listOf(baseDir, frontendDir, backendDir, dataDir).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    fun isBackendReady(): Boolean = backendBundle.exists()

    fun isFrontendReady(): Boolean {
        return frontendDir.exists() &&
                frontendDir.isDirectory &&
                (frontendDir.listFiles()?.isNotEmpty() == true)
    }

    fun isResourcesReady(): Boolean = isBackendReady() && isFrontendReady()
}
