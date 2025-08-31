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



package com.github.yumelira.yumebox.runtime.client

import android.content.Context
import android.content.Intent
import com.github.yumelira.yumebox.remote.ServiceClient
import com.github.yumelira.yumebox.runtime.client.root.RootTunReloadScheduler
import com.github.yumelira.yumebox.service.common.constants.Intents
import com.github.yumelira.yumebox.service.common.util.appContextOrSelf
import com.github.yumelira.yumebox.service.remote.IFetchObserver
import com.github.yumelira.yumebox.service.root.RootTunStateStore
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*

class ProfilesRepository(
    private val context: Context,
) {
    private val appContext = context.appContextOrSelf
    private val rootTunStateStore by lazy { RootTunStateStore(appContext) }

    suspend fun createProfile(
        type: Profile.Type,
        name: String,
        source: String = ""
    ): UUID {
        Timber.d("Creating profile: type=$type, name=$name")
        ServiceClient.connect(context)
        return ServiceClient.profile().create(type, name, source)
    }

    suspend fun cloneProfile(uuid: UUID): UUID {
        Timber.d("Cloning profile: uuid=$uuid")
        ServiceClient.connect(context)
        return ServiceClient.profile().clone(uuid)
    }

    suspend fun deleteProfile(uuid: UUID) {
        Timber.d("Deleting profile: uuid=$uuid")
        ServiceClient.connect(context)
        ServiceClient.profile().delete(uuid)
    }

    suspend fun queryAllProfiles(): List<Profile> {
        return withContext(Dispatchers.IO) {
            ServiceClient.connect(context)
            ServiceClient.profile().queryAll()
        }
    }

    suspend fun queryActiveProfile(): Profile? {
        return withContext(Dispatchers.IO) {
            ServiceClient.connect(context)
            ServiceClient.profile().queryActive()
        }
    }

    suspend fun queryProfileByUUID(uuid: UUID): Profile? {
        return withContext(Dispatchers.IO) {
            ServiceClient.connect(context)
            ServiceClient.profile().queryByUUID(uuid)
        }
    }

    suspend fun setActiveProfile(uuid: UUID) {
        withContext(Dispatchers.IO) {
            val startedAt = System.currentTimeMillis()
            Timber.d("Setting active profile: uuid=$uuid")
            ServiceClient.connect(context)

            val previousActiveProfile = ServiceClient.profile().queryActive()
            val profile = ServiceClient.profile().queryByUUID(uuid)
                ?: error("Profile not found: $uuid")

            ServiceClient.profile().setActive(profile)

            notifyRuntimeOverrideChanged()

            if (isRootTunActive()) {
                RootTunReloadScheduler.schedule(appContext, RootTunReloadScheduler.Reason.PROFILE_CHANGED)
            }

            Timber.d("Active profile applied: uuid=$uuid cost=${System.currentTimeMillis() - startedAt}ms")
        }
    }

    suspend fun clearActiveProfile(profile: Profile) {
        Timber.d("Clearing active profile: uuid=${profile.uuid}")
        ServiceClient.connect(context)
        ServiceClient.profile().clearActive(profile)

        notifyRuntimeOverrideChanged()
    }

    suspend fun reorderProfiles(uuids: List<UUID>) {
        Timber.d("Reordering profiles: count=${uuids.size}")
        ServiceClient.connect(context)
        ServiceClient.profile().reorder(uuids)
    }

    suspend fun updateProfile(uuid: UUID, callback: IFetchObserver? = null) {
        Timber.d("Updating profile: uuid=$uuid")
        ServiceClient.connect(context)
        ServiceClient.profile().update(uuid, callback)
    }

    suspend fun patchProfile(
        uuid: UUID,
        name: String,
        source: String,
        interval: Long
    ) {
        Timber.d("Patching profile: uuid=$uuid")
        ServiceClient.connect(context)
        ServiceClient.profile().patch(uuid, name, source, interval)
    }

    suspend fun queryAll(): List<Profile> {
        return queryAllProfiles()
    }

    suspend fun queryActive(): Profile? {
        return queryActiveProfile()
    }

    private suspend fun restorePreviousActiveProfile(
        previousActiveProfile: Profile?,
        targetProfile: Profile,
    ) {
        runCatching {
            if (previousActiveProfile != null) {
                ServiceClient.profile().setActive(previousActiveProfile)
                notifyRuntimeOverrideChanged()
            } else {
                ServiceClient.profile().clearActive(targetProfile)
                notifyRuntimeOverrideChanged()
            }
            if (isRootTunActive()) {
                RootTunReloadScheduler.schedule(appContext, RootTunReloadScheduler.Reason.PROFILE_CHANGED)
            }
        }.onFailure { restoreError ->
            Timber.e(
                restoreError,
                "Failed to restore active profile after override sync failure: target=%s previous=%s",
                targetProfile.uuid,
                previousActiveProfile?.uuid,
            )
        }
    }

    private fun isRootTunActive(): Boolean {
        val status = rootTunStateStore.snapshot()
        return status.state.isActive || status.runtimeReady
    }

    private fun notifyRuntimeOverrideChanged() {
        appContext.sendBroadcast(
            Intent(Intents.actionOverrideChanged(appContext.packageName))
                .setPackage(appContext.packageName),
        )
    }
}
