package com.github.yumelira.yumebox.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

data class UpdateBuildConfig(
    val versionName: String,
    val updateSource: String,
    val uiBuildId: String,
    val updateRepository: String,
    val updateMirrorTemplates: String,
    val userAgentAppName: String = "YumeBox",
)

enum class UpdateSource(val key: String, val tag: String?) {
    Latest("latest", null),
    Prerelease("prerelease", "Pre-release"),
    Smart("smart", "Smart"),
    ;
    companion object {
        fun fromKey(raw: String?): UpdateSource {
            val key = raw?.trim().orEmpty()
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Smart
        }
    }
}

@Serializable
data class GitHubReleaseAsset(
    val name: String = "",
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = "",
    val size: Long = 0L,
)

@Serializable
data class GitHubRelease(
    val id: Long = 0L,
    @SerialName("tag_name")
    val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String = "",
    @SerialName("published_at")
    val publishedAt: String? = null,
    @SerialName("target_commitish")
    val targetCommitish: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAsset> = emptyList(),
)

@Serializable
data class UpdateManifestPackage(
    val abi: String = "",
    val fileName: String = "",
    val downloadUrl: String = "",
    val size: Long = 0L,
    val sha256: String = "",
    val isUniversal: Boolean = false,
)

@Serializable
data class UpdateManifest(
    val schemaVersion: Int = 0,
    val manifestUrl: String = "",
    val channel: String = "",
    val module: String = "",
    val artifactPrefix: String = "",
    val tag: String = "",
    val versionName: String = "",
    val versionCode: Long = 0L,
    val releaseNotes: String = "",
    val releaseUrl: String = "",
    val publishedAt: String = "",
    val commitSha: String = "",
    val packages: List<UpdateManifestPackage> = emptyList(),
)

data class UpdateCandidate(
    val manifest: UpdateManifest,
) {
    val tag: String get() = manifest.tag
    val versionName: String get() = manifest.versionName
    val releaseNotes: String get() = manifest.releaseNotes
}

data class UpdateDownloadProgress(
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val message: String = "",
)
