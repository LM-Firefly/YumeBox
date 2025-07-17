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

package com.github.yumelira.yumebox.service.clash.module

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInfo
import com.github.yumelira.yumebox.core.Clash
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class AppListCacheModule(service: Service) : Module<Unit>(service) {
    private fun PackageInfo.uniqueUidName(): String =
        if (sharedUserId?.isNotBlank() == true) sharedUserId!! else packageName

    private fun reload() {
        val packages = service.packageManager.getInstalledPackages(0)
            .filter { it.applicationInfo != null }
            .groupBy { it.uniqueUidName() }
            .map { (_, v) ->
                val info = v[0]

                if (v.size == 1) {
                    // Force use package name if only one app in a single sharedUid group
                    // Example: firefox

                    info.applicationInfo!!.uid to info.packageName
                } else {
                    info.applicationInfo!!.uid to info.uniqueUidName()
                }
            }

        Clash.notifyInstalledAppsChanged(packages)
    }

    override suspend fun run() {
        val packageChanged = receiveBroadcast(Channel.CONFLATED) {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }

        while (true) {
            reload()

            packageChanged.receive()

            delay(10_000L.milliseconds)
        }
    }
}
