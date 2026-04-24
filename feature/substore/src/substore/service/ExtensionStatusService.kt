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

package com.github.yumelira.yumebox.substore.service

import android.content.Context
import com.github.yumelira.yumebox.substore.engine.NativeLibraryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ExtensionStatusService(private val context: Context) {
    companion object {
        private const val EXTENSION_PACKAGE_NAME = "com.github.yumelira.yumebox.extension"
        private const val JAVET_LIB_NAME = "libjavet-node-android"
    }
    suspend fun isExtensionAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (isExtensionPackageInstalled()) {
            Timber.d("Extension available: package installed")
            return@withContext true
        }
        if (canDirectLoadLibrary()) {
            Timber.d("Extension available: direct System.loadLibrary succeeded")
            return@withContext true
        }
        if (isAvailableViaManager()) {
            Timber.d("Extension available: extracted via NativeLibraryManager")
            return@withContext true
        }
        Timber.d("Extension not available: all checks exhausted")
        false
    }
    suspend fun isJavetLoaded(): Boolean = withContext(Dispatchers.IO) {
        if (canDirectLoadLibrary()) return@withContext true
        NativeLibraryManager.initialize(context)
        if (NativeLibraryManager.isLibraryAvailable(JAVET_LIB_NAME)) return@withContext true
        NativeLibraryManager.extractAllLibraries()[JAVET_LIB_NAME] == true
    }
    fun isExtensionPackageInstalled(): Boolean = runCatching {
        context.packageManager.getApplicationInfo(EXTENSION_PACKAGE_NAME, 0)
        true
    }.getOrDefault(false)
    private fun canDirectLoadLibrary(): Boolean = runCatching {
        System.loadLibrary("javet-node-android")
        true
    }.getOrDefault(false)
    private fun isAvailableViaManager(): Boolean = runCatching {
        NativeLibraryManager.initialize(context)
        NativeLibraryManager.extractAllLibraries()[JAVET_LIB_NAME] == true
    }.getOrElse { e ->
        Timber.w(e, "NativeLibraryManager check failed")
        false
    }
}
