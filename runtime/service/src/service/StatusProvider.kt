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

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.service.common.util.Global
import com.github.yumelira.yumebox.service.common.util.initializeServiceGlobal
import com.tencent.mmkv.MMKV

class StatusProvider : ContentProvider() {
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            METHOD_CURRENT_PROFILE -> {
                return if (serviceRunning)
                    Bundle().apply {
                        putString("name", currentProfile)
                    }
                else
                    null
            }
            else -> super.call(method, arg, extras)
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw IllegalArgumentException("Stub!")
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        throw IllegalArgumentException("Stub!")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw IllegalArgumentException("Stub!")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw IllegalArgumentException("Stub!")
    }

    override fun getType(uri: Uri): String? {
        throw IllegalArgumentException("Stub!")
    }

    override fun onCreate(): Boolean {
        runCatching {
            val app = context?.applicationContext as? android.app.Application ?: return@runCatching
            initializeServiceGlobal(app)
            // MMKV 必须在使用前初始化，ContentProvider 在 Application.onCreate 之前执行
            MMKV.initialize(app)
            clearTunStarting()
        }
        return true
    }

    companion object {
        const val METHOD_CURRENT_PROFILE = "currentProfile"

        private val legacyRuntimeFiles = listOf(
            "service_running.lock",
            "service_autostart.lock",
            "service_running_mode.txt",
        )
        private const val SERVICE_CACHE_ID = "service_cache"
        private const val KEY_TUN_STARTING = "local_tun_starting"

        @Volatile
        var serviceRunning: Boolean = false
            set(value) { field = value }

        @Volatile
        var runningMode: ProxyMode? = null

        @Volatile
        var currentProfile: String? = null

        fun markRuntimeStarted(mode: ProxyMode) {
            if (mode == ProxyMode.Tun) {
                clearTunStarting()
            }
            runningMode = mode
            serviceRunning = true
        }

        fun markRuntimeStopped(mode: ProxyMode) {
            if (mode == ProxyMode.Tun) {
                clearTunStarting()
            }
            if (runningMode == mode) {
                runningMode = null
                serviceRunning = false
            }
        }

        fun isRuntimeActive(mode: ProxyMode): Boolean {
            return serviceRunning && runningMode == mode
        }

        fun markTunStarting() {
            serviceCache().encode(KEY_TUN_STARTING, true)
        }

        fun clearTunStarting() {
            serviceCache().removeValueForKey(KEY_TUN_STARTING)
        }

        fun isTunStarting(): Boolean {
            return serviceCache().decodeBool(KEY_TUN_STARTING, false)
        }

        fun clearLegacyStateFiles() {
            val filesDir = Global.application.filesDir
            legacyRuntimeFiles.forEach { name ->
                runCatching { filesDir.resolve(name).delete() }
            }
        }

        private fun serviceCache(): MMKV {
            return MMKV.mmkvWithID(SERVICE_CACHE_ID, MMKV.MULTI_PROCESS_MODE)
        }
    }
}
