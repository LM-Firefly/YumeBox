/*
 * This file is part of FlyCat.
 *
 * FlyCat is free software: you can redistribute it and/or modify
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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.screen.home

import com.github.yumelira.yumebox.core.data.NetworkSettingsReader
import com.github.yumelira.yumebox.core.model.Profile
import com.github.yumelira.yumebox.core.util.AutoStartSessionGate
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import dev.oom_wg.purejoy.mlang.MLang
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Handles profile switching, reloading, and profile list management.
 * Extracted from HomeViewModel to separate profile logic from proxy control logic.
 */
internal class ProfileSwitchHandler(
    private val scope: CoroutineScope,
    private val profilesRepository: ProfilesRepository,
    private val proxyFacade: ProxyFacade,
    private val networkSettingsStore: NetworkSettingsReader,
    private val controlStateProvider: () -> HomeProxyControlState,
    private val onError: (String) -> Unit,
    private val onMessage: (String) -> Unit,
) {
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _recommendedProfile = MutableStateFlow<Profile?>(null)
    val recommendedProfile: StateFlow<Profile?> = _recommendedProfile.asStateFlow()

    private val _profilesLoaded = MutableStateFlow(false)
    val profilesLoaded: StateFlow<Boolean> = _profilesLoaded.asStateFlow()

    fun refreshProfiles() {
        scope.launch {
            try {
                val allProfiles = profilesRepository.queryAllProfiles()
                val active = profilesRepository.queryActiveProfile()
                _profiles.value = allProfiles
                _recommendedProfile.value = active
                _profilesLoaded.value = true
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to refresh profiles")
                _profilesLoaded.value = true
            }
        }
    }

    fun observeProfileChanges() {
        scope.launch {
            proxyFacade.currentProfile
                .map { it?.uuid }
                .distinctUntilChanged()
                .collect { refreshProfiles() }
        }
    }

    suspend fun reloadProfile() {
        try {
            val activeProfile = profilesRepository.queryActiveProfile()
            if (activeProfile == null) {
                onError(
                    MLang.Home.Message.ConfigSwitchFailed.format(
                        MLang.ProfilesVM.Error.ProfileNotExist
                    )
                )
                return
            }

            profilesRepository.updateProfile(activeProfile.uuid)
            profilesRepository.setActiveProfile(activeProfile.uuid)
            onMessage(MLang.Home.Message.ConfigSwitched)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Timber.e(error, "Failed to reload profile")
            onError(MLang.Home.Message.ConfigSwitchFailed.format(error.message))
        }
    }

    fun isCurrentProfile(profileId: UUID): Boolean {
        return proxyFacade.currentProfile.value?.uuid == profileId
    }

    fun switchActiveProfile(profileId: String) {
        scope.launch {
            try {
                val uuid = UUID.fromString(profileId)
                if (proxyFacade.currentProfile.value?.uuid == uuid) return@launch

                withContext(Dispatchers.IO) {
                    profilesRepository.setActiveProfile(uuid)
                }

                refreshProfiles()

                if (controlStateProvider() == HomeProxyControlState.Running) {
                    withContext(Dispatchers.IO) {
                        AutoStartSessionGate.clearManualPaused()
                        proxyFacade.startProxy(networkSettingsStore.proxyMode.value)
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to switch active profile")
                onError(MLang.Home.Message.ConfigSwitchFailed.format(error.message))
            }
        }
    }
}
