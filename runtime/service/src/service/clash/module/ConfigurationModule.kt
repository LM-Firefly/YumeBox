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
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.service.StatusProvider
import com.github.yumelira.yumebox.runtime.api.service.common.constants.Intents
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.records.ImportedDao
import com.github.yumelira.yumebox.service.runtime.records.SelectionDao
import com.github.yumelira.yumebox.service.runtime.records.SelectionRestoreExecutor
import com.github.yumelira.yumebox.service.runtime.session.CompiledConfigPipeline
import com.github.yumelira.yumebox.service.runtime.session.SessionRuntimeSpecFactory
import com.github.yumelira.yumebox.runtime.api.service.common.util.importedDir
import com.github.yumelira.yumebox.service.runtime.util.sendProfileLoaded
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.*

class ConfigurationModule(service: Service) : Module<ConfigurationModule.LoadException>(service) {
    data class LoadException(val message: String)

    private val store = ServiceStore()
    private val compiledConfigPipeline = CompiledConfigPipeline(service)
    private val runtimeSpecFactory = SessionRuntimeSpecFactory(service)
    private val reload = Channel<Unit>(Channel.CONFLATED)

    override suspend fun run() {
        val broadcasts = receiveBroadcast {
            addAction(Intents.ACTION_PROFILE_CHANGED)
            addAction(Intents.ACTION_OVERRIDE_CHANGED)
        }

        var loaded: UUID? = null

        reload.trySend(Unit)

        while (true) {
            val changed: UUID? = select {
                broadcasts.onReceive {
                    if (it.action == Intents.ACTION_PROFILE_CHANGED)
                        UUID.fromString(it.getStringExtra(Intents.EXTRA_UUID))
                    else
                        null
                }
                reload.onReceive {
                    null
                }
            }

            try {
                val current = store.activeProfile
                    ?: throw NullPointerException("No profile selected")

                if (current == loaded && changed != null && changed != loaded)
                    continue

                loaded = current

                val active = ImportedDao.queryByUUID(current)
                    ?: throw NullPointerException("No profile selected")

                val spec = runtimeSpecFactory.createHttpSpec()
                compiledConfigPipeline.applyOverrideToRuntimeFile(spec)
                Clash.loadCompiledConfig(service.importedDir.resolve(active.uuid.toString()).resolve("runtime.yaml")).await()

                val restoreSelections = SelectionDao.queryRestorableSelections(active.uuid)
                val restorePins = SelectionDao.getAllPins(active.uuid)
                val runtimeGroups = Clash.queryGroupNames(false).map { Clash.queryGroup(it, ProxySort.Default) }
                SelectionRestoreExecutor.restore(
                    profileUuid = active.uuid,
                    selections = restoreSelections,
                    pins = restorePins,
                    runtimeGroups = runtimeGroups,
                    tag = "LOCAL",
                )

                StatusProvider.currentProfile = active.name

                service.sendProfileLoaded(current)
            } catch (e: Exception) {
                return enqueueEvent(LoadException(e.message ?: "Unknown"))
            }
        }
    }
}
