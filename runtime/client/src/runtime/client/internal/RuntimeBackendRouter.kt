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

package com.github.yumelira.yumebox.runtime.client.internal

import android.content.Context
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimeOwner
import com.github.yumelira.yumebox.runtime.client.remote.ServiceClient

internal class RuntimeBackendRouter(
    private val appContext: Context,
    private val ownerProvider: () -> RuntimeOwner,
    private val runningProvider: () -> Boolean,
) {
    val owner: RuntimeOwner get() = ownerProvider()
    val running: Boolean get() = runningProvider()
    val isRootTun: Boolean get() = ownerProvider() == RuntimeOwner.RootTun
    suspend fun <T> dispatch(
        requireRunning: Boolean = false,
        defaultIfNotRunning: () -> T = { error("Runtime is not running") },
        onRoot: suspend (Context) -> T,
        onLocal: suspend () -> T,
    ): T {
        if (requireRunning && !runningProvider()) return defaultIfNotRunning()
        return when (ownerProvider()) {
            RuntimeOwner.RootTun -> onRoot(appContext)
            else -> {
                ServiceClient.connect(appContext)
                onLocal()
            }
        }
    }
    suspend fun ensureLocalConnected() {
        if (ownerProvider() != RuntimeOwner.RootTun) {
            ServiceClient.connect(appContext)
        }
    }
}
