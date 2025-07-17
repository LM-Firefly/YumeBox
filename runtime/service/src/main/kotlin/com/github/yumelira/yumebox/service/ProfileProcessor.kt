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

package com.github.yumelira.yumebox.service

import android.content.Context
import androidx.core.net.toUri
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.service.runtime.records.ImportedDao
import com.github.yumelira.yumebox.service.runtime.records.SelectionDao
import com.github.yumelira.yumebox.service.runtime.entity.Imported
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import com.github.yumelira.yumebox.service.remote.IFetchObserver
import com.github.yumelira.yumebox.service.runtime.config.ServiceStore
import com.github.yumelira.yumebox.service.runtime.util.importedDir
import com.github.yumelira.yumebox.service.runtime.util.processingDir
import com.github.yumelira.yumebox.service.runtime.util.sendProfileChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.tencent.mmkv.MMKV
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.charset.Charset
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

object ProfileProcessor {
    private const val DEFAULT_USER_AGENT = "ClashMetaForAndroid"

    private val profileLock = Mutex()
    private val processLock = Mutex()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun resolveUserAgent(): String {
        val settings = MMKV.mmkvWithID("settings", MMKV.MULTI_PROCESS_MODE)
        val custom = settings?.decodeString("customUserAgent")?.trim().orEmpty()
        return custom.ifBlank { DEFAULT_USER_AGENT }
    }

    /**
     * 订阅信息数据类
     */
    private data class SubscriptionInfo(
        val upload: Long = 0,
        val download: Long = 0,
        val total: Long = 0,
        val expire: Long = 0,
        val title: String? = null,
        val filename: String? = null,
        val interval: Int = 24
    )

    /**
     * 下载配置文件并解析订阅信息
     * @return Pair<成功标志, 订阅信息>
     */
    private suspend fun downloadWithSubscriptionInfo(
        url: String,
        targetFile: File,
        onProgress: ((Int) -> Unit)? = null
    ): Pair<Boolean, SubscriptionInfo?> = withContext(Dispatchers.IO + NonCancellable) {
        try {
            targetFile.parentFile?.mkdirs()
            if (targetFile.exists()) targetFile.delete()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", resolveUserAgent())
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Pair(false, null)
                }

                // 解析订阅信息
                val subInfo = parseSubscriptionInfo(
                    response.headers["Subscription-Userinfo"] ?: response.headers["subscription-userinfo"],
                    response.headers
                )

                val body = response.body ?: return@withContext Pair(false, null)
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()

                var totalBytesRead = 0L
                val lastUpdate = AtomicLong(0)

                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // 每500ms更新一次进度
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate.get() >= 500) {
                            val progress = if (contentLength > 0) {
                                ((totalBytesRead * 100) / contentLength).toInt()
                            } else 0
                            onProgress?.invoke(progress)
                            lastUpdate.set(now)
                        }
                    }
                }

                Pair(true, subInfo)
            }
        } catch (e: Exception) {
            Log.w("Failed to download with subscription info: $e", e)
            if (targetFile.exists()) targetFile.delete()
            Pair(false, null)
        }
    }

    /**
     * 解析订阅信息
     */
    private fun parseSubscriptionInfo(
        userinfo: String?,
        headers: okhttp3.Headers
    ): SubscriptionInfo {
        var upload = 0L
        var download = 0L
        var total = 0L
        var expire = 0L

        fun parseLikeJsParseInt(value: String): Long {
            val trimmed = value.trim()
            val integerPart = trimmed.takeWhile { it.isDigit() }
            if (integerPart.isNotEmpty()) return integerPart.toLongOrNull() ?: 0L
            return trimmed.substringBefore('.').toLongOrNull() ?: 0L
        }

        fun findHeaderBySuffix(suffix: String): String? {
            val target = suffix.lowercase(Locale.getDefault())
            val key = headers.names().firstOrNull {
                it.lowercase(Locale.getDefault()).endsWith(target)
            } ?: return null
            return headers[key]
        }

        fun parseExpireDate(value: String): Long? = runCatching {
            when {
                value.matches(Regex("\\d+")) -> value.toLong() * 1000L
                value.contains("-") -> {
                    val parts = value.split("-")
                    if (parts.size < 3) return@runCatching null

                    val year = parts[0].toIntOrNull() ?: return@runCatching null
                    val month = parts[1].toIntOrNull() ?: return@runCatching null
                    val day = parts[2].toIntOrNull() ?: return@runCatching null

                    val calendar = Calendar.getInstance()
                    calendar.set(year, month - 1, day, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.timeInMillis
                }
                else -> null
            }
        }.getOrNull()

        val resolvedUserinfo = userinfo ?: findHeaderBySuffix("subscription-userinfo")

        // subscription-userinfo: upload=1234; download=2234; total=1024000; expire=2218532293
        if (!resolvedUserinfo.isNullOrBlank()) {
            val flags = resolvedUserinfo.split(";")
            for (flag in flags) {
                val info = flag.trim().split("=", limit = 2)
                if (info.size >= 2) {
                    val key = info[0].trim().lowercase(Locale.getDefault())
                    val value = info[1].trim()

                    when {
                        key.contains("upload") && value.isNotEmpty() -> {
                            upload = parseLikeJsParseInt(value)
                        }
                        key.contains("download") && value.isNotEmpty() -> {
                            download = parseLikeJsParseInt(value)
                        }
                        key.contains("total") && value.isNotEmpty() -> {
                            total = parseLikeJsParseInt(value)
                        }
                        key.contains("expire") && value.isNotEmpty() -> {
                            expire = parseLikeJsParseInt(value) * 1000L
                        }
                    }
                }
            }
        }

        if (expire == 0L) {
            expire = (headers["Expires"] ?: findHeaderBySuffix("expires"))?.let { parseExpireDate(it) } ?: 0L
        }

        // 获取订阅标题（兼容 URL 编码 / base64: 前缀）
        val title = decodeSubscriptionTitle(
            headers["Profile-Title"]
                ?: headers["Subscription-Title"]
                ?: findHeaderBySuffix("profile-title")
                ?: findHeaderBySuffix("subscription-title")
        )

        // 解析文件名
        val filename = parseFilenameFromHeaders(headers)

        // 获取更新间隔
        val interval = headers["Profile-Update-Interval"]?.toIntOrNull()
            ?: headers["Subscription-Update-Interval"]?.toIntOrNull()
            ?: findHeaderBySuffix("profile-update-interval")?.toIntOrNull()
            ?: findHeaderBySuffix("subscription-update-interval")?.toIntOrNull()
            ?: 24

        return SubscriptionInfo(upload, download, total, expire, title, filename, interval)
    }

    private fun decodeSubscriptionTitle(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null

        fun decodeBase64(encoded: String): String? {
            val candidate = encoded.trim().trim('"', '\'')
            if (candidate.isBlank()) return null
            if (!candidate.matches(Regex("^[A-Za-z0-9+/=]+$"))) return null
            return runCatching {
                String(Base64.getDecoder().decode(candidate), StandardCharsets.UTF_8).trim()
            }.getOrNull()
        }

        fun decodeRfc5987(value: String): String? {
            val match = Regex("""^([^']*)'[^']*'(.*)$""").find(value.trim()) ?: return null
            val charset = match.groupValues[1].ifBlank { "UTF-8" }
            val encoded = match.groupValues[2]

            return runCatching {
                URLDecoder.decode(encoded, charset).trim()
            }.getOrNull()
        }

        return runCatching {
            val normalized = value.trim().trim('"', '\'')
            when {
                normalized.startsWith("base64:", ignoreCase = true) -> {
                    decodeBase64(normalized.substringAfter(':', "")) ?: value
                }
                else -> {
                    decodeRfc5987(normalized)
                        ?: runCatching {
                            URLDecoder.decode(normalized, StandardCharsets.UTF_8.name()).trim()
                        }.getOrNull()
                        ?: decodeBase64(normalized)
                        ?: value
                }
            }
        }.getOrElse { value }.takeIf { it.isNotBlank() }
    }

    /**
     * 从 Content-Disposition 解析文件名
     */
    private fun parseFilenameFromHeaders(headers: okhttp3.Headers): String? {
        val contentDisposition = headers["Content-Disposition"]
            ?: headers.names()
                .firstOrNull { it.lowercase(Locale.getDefault()).endsWith("content-disposition") }
                ?.let { headers[it] }
            ?: return null

        return runCatching {
            if (contentDisposition.contains("filename*=", ignoreCase = true)) {
                val regex = """filename\*=([^']*)'([^']*)'([^;]+)""".toRegex(RegexOption.IGNORE_CASE)
                regex.find(contentDisposition)?.let { match ->
                    val charset = match.groupValues[1].ifBlank { "UTF-8" }
                    val encodedFilename = match.groupValues[3].trim().trim('"', '\'')
                    val safeCharset = runCatching { Charset.forName(charset).name() }.getOrDefault("UTF-8")
                    URLDecoder.decode(encodedFilename, safeCharset).trim()
                }
            } else {
                val regex = """filename=([^;]+)""".toRegex(RegexOption.IGNORE_CASE)
                regex.find(contentDisposition)?.groupValues?.getOrNull(1)?.trim()?.trim('"', '\'')
            }?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private suspend fun fetchUrlSubscription(
        context: Context,
        uuid: UUID,
        source: String,
        onProgress: (Int) -> Unit
    ): SubscriptionInfo? {
        return try {
            onProgress(5)
            val tempFile = File(context.cacheDir, "temp_$uuid.yaml")
            val (success, info) = downloadWithSubscriptionInfo(source, tempFile) { progress ->
                onProgress(5 + (progress * 0.4).toInt())
            }
            val result = if (success) {
                tempFile.copyTo(File(context.processingDir, "config.yaml"), overwrite = true)
                info
            } else null
            tempFile.delete()
            result
        } catch (e: Exception) {
            Log.w("Failed to download with subscription info: $e", e)
            null
        }
    }

    private fun resolveSubscriptionName(
        snapshotName: String,
        snapshotSource: String,
        subInfo: SubscriptionInfo?
    ): String {
        if (!ProfileNameUtils.isAutoGeneratedProfileName(snapshotName)) return snapshotName

        val headerTitle = subInfo?.title?.takeIf { it.isNotBlank() }
        val filename = subInfo?.filename?.substringBeforeLast(".")?.takeIf { it.isNotBlank() }
        val sourceName = ProfileNameUtils.extractSourceBaseName(snapshotSource)

        if (headerTitle != null) return headerTitle
        if (filename != null) return filename
        if (sourceName != null) return sourceName
        return snapshotName
    }

    suspend fun update(context: Context, uuid: UUID, callback: IFetchObserver?) {
        withContext(Dispatchers.IO + NonCancellable) {
            processLock.withLock {
                val snapshot = profileLock.withLock {
                    val imported = ImportedDao.queryByUUID(uuid)
                        ?: throw IllegalArgumentException("profile $uuid not found")

                    context.processingDir.deleteRecursively()
                    context.processingDir.mkdirs()

                    context.importedDir.resolve(imported.uuid.toString())
                        .copyRecursively(context.processingDir, overwrite = true)

                    imported
                }

                var cb = callback
                var subInfo: SubscriptionInfo? = null

                // Fetch subscription info for URL profiles
                if (snapshot.type == Profile.Type.Url) {
                    subInfo = fetchUrlSubscription(context, snapshot.uuid, snapshot.source) { progress ->
                        try {
                            cb?.updateStatus(
                                com.github.yumelira.yumebox.core.model.FetchStatus(
                                    action = com.github.yumelira.yumebox.core.model.FetchStatus.Action.FetchConfiguration,
                                    args = emptyList(),
                                    progress = progress,
                                    max = 100
                                )
                            )
                        } catch (e: Exception) {
                            cb = null
                        }
                    }
                }

                Clash.fetchAndValid(context.processingDir, snapshot.source, true) {
                    try {
                        cb?.updateStatus(it)
                    } catch (e: Exception) {
                        cb = null
                        Log.w("Report fetch status: $e", e)
                    }
                }.await()

                profileLock.withLock {
                    if (ImportedDao.exists(snapshot.uuid)) {
                        context.importedDir.resolve(snapshot.uuid.toString()).deleteRecursively()
                        context.processingDir
                            .copyRecursively(context.importedDir.resolve(snapshot.uuid.toString()))

                        // Update database with subscription info
                        val finalName = if (snapshot.type == Profile.Type.Url) {
                            resolveSubscriptionName(snapshot.name, snapshot.source, subInfo)
                        } else snapshot.name

                        val updated = Imported(
                            snapshot.uuid,
                            finalName,
                            snapshot.type,
                            snapshot.source,
                            if (snapshot.type == Profile.Type.Url && subInfo != null) {
                                (subInfo.interval ?: 24).toLong() * 60 * 60 * 1000
                            } else snapshot.interval,
                            subInfo?.upload ?: snapshot.upload,
                            subInfo?.download ?: snapshot.download,
                            subInfo?.total ?: snapshot.total,
                            subInfo?.expire ?: snapshot.expire,
                            snapshot.createdAt
                        )
                        ImportedDao.update(updated)

                        context.sendProfileChanged(snapshot.uuid)
                    }
                }
            }
        }
    }

    suspend fun delete(context: Context, uuid: UUID) {
        withContext(Dispatchers.IO + NonCancellable) {
            profileLock.withLock {
                ImportedDao.remove(uuid)
                SelectionDao.clear(uuid)

                val imported = context.importedDir.resolve(uuid.toString())
                imported.deleteRecursively()

                context.sendProfileChanged(uuid)
            }
        }
    }

    suspend fun active(context: Context, uuid: UUID) {
        withContext(Dispatchers.IO + NonCancellable) {
            profileLock.withLock {
                if (!ImportedDao.exists(uuid)) {
                    throw IllegalArgumentException("profile $uuid is not available")
                }
                val store = ServiceStore()
                store.activeProfile = uuid
                context.sendProfileChanged(uuid)
            }
        }
    }
}