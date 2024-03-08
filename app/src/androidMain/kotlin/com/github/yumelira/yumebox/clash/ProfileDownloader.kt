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

package com.github.yumelira.yumebox.clash

import com.github.yumelira.yumebox.clash.exception.ConfigImportException
import com.github.yumelira.yumebox.clash.exception.UnknownException
import com.github.yumelira.yumebox.clash.importer.ConfigImportService
import com.github.yumelira.yumebox.clash.importer.ImportState
import com.github.yumelira.yumebox.data.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * 配置文件下载器 - 使用 ConfigImportService 的便捷包装
 *
 * 这是对 ConfigImportService 的简化封装，保持向后兼容
 */
suspend fun downloadProfile(
    profile: Profile,
    workDir: File,
    force: Boolean = true,
    onProgress: ((String, Int) -> Unit)? = null
): Result<String> = withContext(Dispatchers.IO) {
    try {
        Timber.d("开始下载配置: ${profile.name}, force=$force")

        // 创建导入服务
        val importService = ConfigImportService(workDir)

        // 执行导入
        val result = importService.importProfile(
            profile = profile,
            force = force,
            onProgress = { state ->
                // 将状态转换为进度回调
                val (message, progress) = mapStateToProgress(state)
                onProgress?.invoke(message, progress)
            }
        )

        if (result.isSuccess) {
            val configPath = result.getOrThrow()
            Timber.d("配置下载成功: ${profile.name}, path=$configPath")
            Result.success(configPath)
        } else {
            val error = result.exceptionOrNull()
            Timber.e(error, "配置下载失败: ${profile.name}")

            // 包装异常信息使其更友好
            val friendlyError = when (error) {
                is ConfigImportException -> error.userFriendlyMessage
                else -> error?.message ?: "未知错误"
            }

            Result.failure(UnknownException(friendlyError, error))
        }
    } catch (e: Exception) {
        Timber.e(e, "下载配置异常: ${profile.name}")
        Result.failure(e)
    }
}

/**
 * 将导入状态映射到进度信息
 */
private fun mapStateToProgress(state: ImportState): Pair<String, Int> {
    return when (state) {
        is ImportState.Idle -> "空闲" to 0
        is ImportState.Preparing -> state.message to 5
        is ImportState.Downloading -> state.message to (10 + (state.progressPercent * 0.4).toInt())
        is ImportState.Copying -> state.message to (10 + (state.progress * 0.4).toInt())
        is ImportState.Validating -> state.message to (50 + (state.progress * 0.2).toInt())
        is ImportState.LoadingProviders -> state.message to (70 + (state.progress * 0.2).toInt())
        is ImportState.Retrying -> state.retryMessage to 5
        is ImportState.Cleaning -> state.message to 95
        is ImportState.Success -> state.message to 100
        is ImportState.Failed -> state.shortError to 0
        is ImportState.Cancelled -> state.message to 0
    }
}

/**
 * 批量下载配置（带并发控制）
 */
suspend fun downloadProfiles(
    profiles: List<Profile>,
    workDir: File,
    force: Boolean = true,
    maxConcurrent: Int = 3,
    onProgress: ((Profile, String, Int) -> Unit)? = null,
    onComplete: ((Profile, Result<String>) -> Unit)? = null
): Map<String, Result<String>> = withContext(Dispatchers.IO) {
    val results = mutableMapOf<String, Result<String>>()

    profiles.chunked(maxConcurrent).forEach { batch ->
        coroutineScope {
            val batchResults = batch.map { profile ->
                profile to async {
                    val result = downloadProfile(
                        profile = profile,
                        workDir = workDir,
                        force = force,
                        onProgress = { msg, progress ->
                            onProgress?.invoke(profile, msg, progress)
                        }
                    )
                    onComplete?.invoke(profile, result)
                    result
                }
            }.map { (profile, deferred) ->
                profile.id to deferred.await()
            }

            results.putAll(batchResults)
        }
    }

    results
}

/**
 * 取消配置下载
 */
fun cancelProfileDownload(
    profile: Profile,
    workDir: File
): Boolean {
    val importService = ConfigImportService(workDir)
    return importService.cancelImport(profile.id)
}

/**
 * 清理孤儿配置（数据库中不存在的配置）
 * 应该在应用启动时调用
 */
fun cleanupOrphanedConfigs(
    workDir: File,
    validProfiles: List<Profile>
) {
    try {
        val importService = ConfigImportService(workDir)
        val validIds = validProfiles.map { it.id }.toSet()
        importService.cleanupOrphanedConfigs(validIds)
        Timber.d("孤儿配置清理完成")
    } catch (e: Exception) {
        Timber.e(e, "孤儿配置清理失败")
    }
}

/**
 * 检查配置是否已保存
 */
fun Profile.isConfigSaved(workDir: File): Boolean {
    val importedDir = workDir.parentFile?.resolve("imported") ?: return false
    val configFile = File(importedDir, "$id/config.yaml")
    return configFile.exists() && configFile.length() > 0
}
