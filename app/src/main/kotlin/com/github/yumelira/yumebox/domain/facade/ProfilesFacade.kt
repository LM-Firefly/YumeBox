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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.domain.facade

import com.github.yumelira.yumebox.clash.cancel
import com.github.yumelira.yumebox.clash.downloadProfile
import com.github.yumelira.yumebox.clash.scheduleNext
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.data.store.ProfileLink
import com.github.yumelira.yumebox.data.store.ProfileLinksStorage
import com.github.yumelira.yumebox.data.store.ProfilesStorage
import java.io.File
import kotlinx.coroutines.flow.StateFlow

/**
 * Facade for Profile management
 * Encapsulates ProfilesStore to prevent direct access from ViewModels
 */
class ProfilesFacade(
    private val profilesStore: ProfilesStorage,
    private val profileLinksStorage: ProfileLinksStorage
) {
    val profiles: StateFlow<List<Profile>> = profilesStore.profiles
    val enabledProfile: StateFlow<Profile?> = profilesStore.enabledProfile
    val recommendedProfile: StateFlow<Profile?> = profilesStore.recommendedProfile

    fun getAllProfiles(): List<Profile> = profilesStore.getAllProfiles()
    
    fun getProfileById(profileId: String): Profile? = profilesStore.getProfileById(profileId)
    
    fun getEnabledProfile(): Profile? = profilesStore.getEnabledProfile()
    
    fun getRecommendedProfile(): Profile? = profilesStore.getRecommendedProfile()
    
    fun hasEnabledProfile(): Boolean = profilesStore.hasEnabledProfile()

    suspend fun addProfile(profile: Profile) = profilesStore.addProfile(profile)
    
    suspend fun removeProfile(id: String) = profilesStore.removeProfile(id)
    
    suspend fun updateProfile(profile: Profile) = profilesStore.updateProfile(profile)
    
    fun updateLastUsedProfileId(profileId: String) = profilesStore.updateLastUsedProfileId(profileId)
    
    fun getLastUsedProfileId(): String = profilesStore.lastUsedProfileId
    
    suspend fun reorderProfiles(profiles: List<Profile>) = profilesStore.reorderProfiles(profiles)

    val linkOpenMode: Preference<LinkOpenMode> = profileLinksStorage.linkOpenMode
    val links: Preference<List<ProfileLink>> = profileLinksStorage.links
    val defaultLinkId: Preference<String> = profileLinksStorage.defaultLinkId

    fun setOpenMode(mode: LinkOpenMode) = linkOpenMode.set(mode)

    fun setDefaultLink(linkId: String) = defaultLinkId.set(linkId)

    fun addLink(link: ProfileLink) = links.set(links.value + link)

    fun updateLink(linkId: String, name: String, url: String) {
        links.set(links.value.map { link ->
            if (link.id == linkId) link.copy(name = name, url = url) else link
        })
    }

    fun removeLink(linkId: String) {
        links.set(links.value.filterNot { it.id == linkId })
        if (defaultLinkId.value == linkId) {
            defaultLinkId.set("")
        }
    }

    suspend fun downloadProfile(
        profile: Profile,
        workDir: File,
        force: Boolean = true,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<String> {
        return downloadProfile(profile, workDir, force, onProgress)
    }

    fun scheduleAutoUpdate(profile: Profile) {
        scheduleNext(profile)
    }

    fun cancelAutoUpdate(profileId: String) {
        cancel(profileId)
    }
    
    // Direct access to store for special cases (use sparingly)
    internal fun getStore(): ProfilesStorage = profilesStore
}
