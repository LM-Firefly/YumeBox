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



package com.github.yumelira.yumebox.service.runtime.session

import android.content.Context
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import java.io.File

class RuntimeStartupLogStore(
    context: Context,
    private val scope: Scope,
) {
    enum class Scope(
        val fileName: String,
        val tag: String,
    ) {
        LOCAL_TUN("local_tun_startup.log", "LOCAL_TUN"),
        LOCAL_HTTP("local_http_startup.log", "LOCAL_HTTP"),
        ROOT_TUN("root_tun_startup.log", "ROOT_TUN"),
    }

    private val appContext = context.appContextOrSelf
    private val file = File(appContext.filesDir, scope.fileName)

    fun path(): String = ""

    fun clear() {
        synchronized(lock) {
            runCatching { file.delete() }
        }
    }

    fun append(line: String) {
        if (line.isBlank()) return
        synchronized(lock) {
            runCatching { file.delete() }
        }
    }

    fun snapshot(): String {
        return ""
    }

    companion object {
        private val lock = Any()

        fun scopeForMode(mode: ProxyMode): Scope {
            return when (mode) {
                ProxyMode.Tun -> Scope.LOCAL_TUN
                ProxyMode.Http -> Scope.LOCAL_HTTP
                ProxyMode.RootTun -> Scope.ROOT_TUN
            }
        }
    }
}
