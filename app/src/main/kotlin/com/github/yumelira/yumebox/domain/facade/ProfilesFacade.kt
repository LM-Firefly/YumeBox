package com.github.yumelira.yumebox.domain.facade

import com.github.yumelira.yumebox.clash.cancel
import com.github.yumelira.yumebox.clash.downloadAndImportProfileConfig
import com.github.yumelira.yumebox.clash.scheduleNext
import com.github.yumelira.yumebox.common.util.DownloadUtil
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.data.store.ProfileLink
import com.github.yumelira.yumebox.data.store.ProfileLinksStorage
import com.github.yumelira.yumebox.data.store.ProfilesStorage
import java.io.File
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Facade for Profile management
 * Encapsulates ProfilesStore to prevent direct access from ViewModels
 */
class ProfilesFacade(
    private val profilesStore: ProfilesStorage,
    private val profileLinksStorage: ProfileLinksStorage
) {
    private val downloadingProfileIds = mutableSetOf<String>()
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
    suspend fun downloadAndMergeSubscriptionInfo(
        profile: Profile,
        workDir: File,
        force: Boolean = true,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<Profile> {
        if (profile.id in downloadingProfileIds) {
            return Result.failure(IllegalStateException("Profile ${profile.id} is already downloading"))
        }
        downloadingProfileIds.add(profile.id)
        return try {
            val downloadResult = downloadAndImportProfileConfig(profile, workDir, force, onProgress)
            if (!downloadResult.isSuccess) {
                return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Unknown download error"))
            }
            val configPath = downloadResult.getOrThrow()
            val subscriptionInfo = if (profile.type == ProfileType.URL) {
                profile.remoteUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    runCatching {
                        val tempFile = File.createTempFile("temp_${profile.id}", ".yaml")
                        val (_, info) = DownloadUtil.downloadWithSubscriptionInfo(url, tempFile)
                        tempFile.delete()
                        info
                    }.getOrNull()
                }
            } else null
            var updated = profile.copy(
                config = configPath,
                updatedAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis()
            )
            subscriptionInfo?.filename?.let { fileName ->
                val nameWithoutExt = if (fileName.contains(".")) {
                    fileName.substringBeforeLast(".")
                } else {
                    fileName
                }
                val defaultNames = setOf("新配置")  // 通常的默认名字
                if (updated.name.isBlank() || updated.name in defaultNames || updated.name.startsWith("temp_")) {
                    updated = updated.copy(name = nameWithoutExt)
                }
            }
            subscriptionInfo?.let { info ->
                updated = updated.copy(
                    provider = info.title ?: updated.provider,
                    expireAt = info.expire.takeIf { it != 0L } ?: updated.expireAt,
                    usedBytes = info.upload + info.download,
                    totalBytes = if (info.total > 0) info.total else updated.totalBytes
                )
            }
            Result.success(updated)
        } catch (e: Exception) {
            Timber.e(e, "downloadAndMergeSubscriptionInfo failed for profile ${profile.id}")
            Result.failure(e)
        } finally {
            downloadingProfileIds.remove(profile.id)
        }
    }
    fun scheduleAutoUpdate(profile: Profile) {
        scheduleNext(profile)
    }
    fun cancelAutoUpdate(profileId: String) {
        cancel(profileId)
    }
    internal fun getStore(): ProfilesStorage = profilesStore
}
