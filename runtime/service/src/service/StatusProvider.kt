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

        @Volatile
        var serviceRunning: Boolean = false
            set(value) { field = value }

        @Volatile
        var runningMode: ProxyMode? = null

        @Volatile
        var currentProfile: String? = null

        fun markRuntimeStarted(mode: ProxyMode) {
            runningMode = mode
            serviceRunning = true
        }

        fun markRuntimeStopped(mode: ProxyMode) {
            if (runningMode == mode) {
                runningMode = null
                serviceRunning = false
            }
        }

        fun isRuntimeActive(mode: ProxyMode): Boolean {
            return serviceRunning && runningMode == mode
        }

        fun clearLegacyStateFiles() {
            val filesDir = Global.application.filesDir
            legacyRuntimeFiles.forEach { name ->
                runCatching { filesDir.resolve(name).delete() }
            }
        }
    }
}
