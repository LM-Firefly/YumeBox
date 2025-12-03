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

package com.github.yumelira.yumebox.clash.importer

/**
 * 配置导入状态机
 */
sealed class ImportState {
    /**
     * 空闲状态
     */
    object Idle : ImportState()

    /**
     * 准备中
     */
    data class Preparing(
        val message: String = "准备导入..."
    ) : ImportState()

    /**
     * 下载中（用于 URL 类型）
     */
    data class Downloading(
        val progress: Int,
        val currentBytes: Long,
        val totalBytes: Long,
        val message: String = "下载配置中..."
    ) : ImportState() {
        val progressPercent: Int
            get() = if (totalBytes > 0) {
                ((currentBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            } else progress
    }

    /**
     * 复制中（用于本地文件类型）
     */
    data class Copying(
        val progress: Int,
        val message: String = "复制文件中..."
    ) : ImportState()

    /**
     * 验证配置中
     */
    data class Validating(
        val progress: Int,
        val message: String = "验证配置..."
    ) : ImportState()

    /**
     * 加载 Providers
     */
    data class LoadingProviders(
        val progress: Int,
        val currentProvider: String?,
        val totalProviders: Int,
        val completedProviders: Int,
        val message: String = "加载资源..."
    ) : ImportState() {
        val providerProgress: String
            get() = "$completedProviders/$totalProviders"
    }

    /**
     * 重试中
     */
    data class Retrying(
        val attempt: Int,
        val maxAttempts: Int,
        val reason: String,
        val nextRetryInSeconds: Int
    ) : ImportState() {
        val retryMessage: String
            get() = "第 $attempt/$maxAttempts 次重试，原因: $reason"
    }

    /**
     * 清理中
     */
    data class Cleaning(
        val message: String = "清理临时文件..."
    ) : ImportState()

    /**
     * 成功
     */
    data class Success(
        val configPath: String,
        val message: String = "导入成功"
    ) : ImportState()

    /**
     * 失败
     */
    data class Failed(
        val error: String,
        val exception: Throwable?,
        val isRetryable: Boolean = false
    ) : ImportState() {
        val shortError: String
            get() = error.take(200)
    }

    /**
     * 已取消
     */
    data class Cancelled(
        val message: String = "导入已取消"
    ) : ImportState()

}

