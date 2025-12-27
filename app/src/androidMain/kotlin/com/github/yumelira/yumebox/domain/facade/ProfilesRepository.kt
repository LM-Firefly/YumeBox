package com.github.yumelira.yumebox.domain.facade

import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.store.ProfilesStore
import kotlinx.coroutines.flow.StateFlow

class ProfilesRepository(
    private val profilesStore: ProfilesStore
) {
    val profiles: StateFlow<List<Profile>> = profilesStore.profiles
    val enabledProfile: StateFlow<Profile?> = profilesStore.enabledProfile
    val recommendedProfile: StateFlow<Profile?> = profilesStore.recommendedProfile

    fun getAllProfiles(): List<Profile> = profilesStore.getAllProfiles()
    fun getEnabledProfile(): Profile? = profilesStore.getEnabledProfile()
    fun getRecommendedProfile(): Profile? = profilesStore.getRecommendedProfile()
    fun hasEnabledProfile(): Boolean = profilesStore.hasEnabledProfile()
}
