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

package com.github.yumelira.yumebox.clash.manager

import kotlinx.coroutines.*
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.clash.config.ClashConfiguration
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.FetchStatus
import com.github.yumelira.yumebox.core.model.TunnelState
import java.io.File
import timber.log.Timber

class ProfileManager(private val workDir: File) {
    companion object {
        private const val IMPORTED_DIR = "imported"
        private const val CONFIG_FILE = "config.yaml"
    }

    fun getProfileDir(profileId: String): File {
        val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR) ?: File(workDir, IMPORTED_DIR)
        return File(importedDir, profileId)
    }

    fun getConfigFile(profile: Profile): File {
        return when (profile.type) {
            ProfileType.FILE -> File(profile.config)
            ProfileType.URL -> File(getProfileDir(profile.id), CONFIG_FILE)
        }
    }

    fun isProfileImported(profile: Profile): Boolean {
        return getConfigFile(profile).exists()
    }

    suspend fun loadProfile(
        profile: Profile,
        forceDownload: Boolean = false,
        onProgress: ((String, Int) -> Unit)? = null,
        willUseTunMode: Boolean = false,
        quickStart: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val configFile = getConfigFile(profile)
            val profileDir = configFile.parentFile ?: configFile

            if (!configFile.exists()) {
                if (quickStart) {
                    return@withContext Result.failure(
                        IllegalStateException("配置未导入，请先下载配置: ${profile.name}")
                    )
                }
                onProgress?.invoke("正在下载配置...", 10)
                prepareConfigFile(profile, forceDownload) { msg, progress ->
                    onProgress?.invoke(msg, 10 + (progress * 0.5).toInt())
                }
            }

            Clash.reset()
            Clash.clearOverride(Clash.OverrideSlot.Session)
            
            onProgress?.invoke("正在加载配置...", 70)
            Clash.load(profileDir).await()
            
            onProgress?.invoke("加载完成", 100)
            Timber.d("Profile loaded: ${profile.name}")
            Result.success(configFile.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "配置加载失败: ${profile.name}")
            Result.failure(e)
        }
    }

    suspend fun downloadProfileOnly(
        profile: Profile,
        forceDownload: Boolean = true,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("准备下载配置...", 0)

            val configFile = prepareConfigFile(profile, forceDownload) { msg, progress ->
                onProgress?.invoke(msg, (progress * 0.9).toInt())
            }
            
            onProgress?.invoke("下载完成", 100)
            Result.success(configFile.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "配置下载失败: ${profile.name}")
            Result.failure(e)
        }
    }

    suspend fun reloadCurrentProfile(currentProfile: Profile?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = currentProfile
                ?: return@withContext Result.failure(Exception("没有当前配置"))

            val configFile = getConfigFile(profile)
            if (!configFile.exists()) {
                return@withContext Result.failure(Exception("配置文件不存在: ${profile.name}"))
            }
            
            val profileDir = configFile.parentFile ?: configFile
            Clash.load(profileDir).await()
            Timber.d("Profile reloaded: ${profile.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "重新加载配置失败")
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        profile: Profile,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke("正在更新配置...", 0)
            
            val configFile = prepareConfigFile(profile, forceDownload = true) { msg, progress ->
                onProgress?.invoke(msg, progress)
            }
            
            onProgress?.invoke("更新完成", 100)
            Timber.d("Profile updated: ${profile.name}")
            Result.success(configFile.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "配置更新失败: ${profile.name}")
            Result.failure(e)
        }
    }

    private suspend fun prepareConfigFile(
        profile: Profile,
        forceDownload: Boolean,
        onProgress: ((String, Int) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        when (profile.type) {
            ProfileType.URL -> {
                downloadProfileFromUrl(profile, forceDownload, onProgress)
            }
            ProfileType.FILE -> {
                processLocalFileProfile(profile, onProgress)
            }
        }
    }

    private suspend fun downloadProfileFromUrl(
        profile: Profile,
        force: Boolean,
        onProgress: ((String, Int) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        val importedDir = workDir.parentFile?.resolve(IMPORTED_DIR) ?: File(workDir, IMPORTED_DIR)
        val targetDir = File(importedDir, profile.id)
        val remoteUrl = profile.remoteUrl ?: profile.config
        val configFile = File(targetDir, CONFIG_FILE)

        try {
            Clash.fetchAndValid(
                path = targetDir,
                url = remoteUrl,
                force = force,
                reportStatus = { status ->
                    val message = when (status.action) {
                        FetchStatus.Action.FetchConfiguration -> "正在下载配置文件..."
                        FetchStatus.Action.FetchProviders -> "正在更新外部资源..."
                        FetchStatus.Action.Verifying -> "正在验证配置..."
                    }
                    onProgress?.invoke(message, if (status.max > 0) (status.progress * 100 / status.max) else 0)
                }
            ).await()
        } catch (e: Exception) {
            configFile.takeIf { it.exists() }?.delete()
            throw e
        }

        configFile
    }

    private suspend fun processLocalFileProfile(
        profile: Profile,
        onProgress: ((String, Int) -> Unit)? = null
    ): File {
        val configFile = File(profile.config)
        if (!configFile.exists()) throw IllegalArgumentException("配置文件不存在: ${profile.config}")
        val configDir = configFile.parentFile ?: throw IllegalArgumentException("无法获取配置目录")
        onProgress?.invoke("正在验证配置...", 10)

        Clash.fetchAndValid(
            path = configDir,
            url = "",
            force = false,
            reportStatus = { status ->
                val message = when (status.action) {
                    FetchStatus.Action.FetchConfiguration -> "正在读取配置文件..."
                    FetchStatus.Action.FetchProviders -> "正在更新外部资源..."
                    FetchStatus.Action.Verifying -> "正在验证配置..."
                }
                onProgress?.invoke(message, if (status.max > 0) (status.progress * 100 / status.max) else 50)
            }
        ).await()

        return configFile
    }

    fun applyDefaultOverrides(willUseTunMode: Boolean = false) {
        val override = ConfigurationOverride(
            mode = TunnelState.Mode.Rule,
            mixedPort = if (!willUseTunMode) 7890 else null
        )
        Clash.patchOverride(Clash.OverrideSlot.Session, override)
    }
}