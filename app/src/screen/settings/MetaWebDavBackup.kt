package com.github.yumelira.yumebox.screen.settings

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class MetaWebDavConfig(
    val url: String,
    val account: String,
    val password: String,
    val directory: String,
) {
    fun isValid(): Boolean {
        return url.isNotBlank() && account.isNotBlank() && password.isNotBlank()
    }
    fun normalizedRootUrl(): String {
        val base = url.trim().removeSuffix("/")
        val dir = directory.trim().trim('/').takeIf { it.isNotBlank() }
        val merged = if (dir == null) base else "$base/$dir"
        return "$merged/"
    }
}

internal data class MetaWebDavBackupFile(
    val name: String,
    val lastModified: Long,
)

internal object MetaWebDavBackup {
    private const val TEMP_DOWNLOAD_NAME = "meta_webdav_restore.zip"
    private const val EMPTY_XML = "<?xml version=\"1.0\"?><propfind xmlns=\"DAV:\"><prop><displayname/><getlastmodified/><resourcetype/></prop></propfind>"
    suspend fun test(config: MetaWebDavConfig): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = WebDavClient(config)
            client.ensureRootDirectory()
            client.listBackups()
            Unit
        }
    }
    suspend fun backup(context: Context, config: MetaWebDavConfig): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val client = WebDavClient(config)
            client.ensureRootDirectory()
            val tempFile = File(context.cacheDir, MetaBackupRestore.defaultBackupFileName())
            try {
                MetaBackupRestore.backupToFile(context, tempFile).getOrThrow()
                client.uploadBackup(tempFile)
                tempFile.name
            } finally {
                tempFile.delete()
            }
        }
    }
    suspend fun restoreLatest(context: Context, config: MetaWebDavConfig): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val client = WebDavClient(config)
            client.ensureRootDirectory()
            val latest = client.listBackups().maxByOrNull { it.lastModified }
                ?: throw IllegalStateException("no remote backup found")
            val tempFile = File(context.cacheDir, TEMP_DOWNLOAD_NAME)
            try {
                client.downloadBackup(latest.name, tempFile)
                MetaBackupRestore.restoreFromFile(context, tempFile).getOrThrow()
                latest.name
            } finally {
                tempFile.delete()
            }
        }
    }
    private class WebDavClient(config: MetaWebDavConfig) {
        private val rootUrl = config.normalizedRootUrl()
        private val authHeader = Credentials.basic(config.account.trim(), config.password)
        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
        fun ensureRootDirectory() {
            val mkcol = execute("MKCOL", rootUrl)
            mkcol.use { response ->
                if (!response.isSuccessful && response.code !in setOf(405, 301, 302)) {
                    throw IllegalStateException("MKCOL failed with HTTP ${response.code}")
                }
            }
        }
        fun listBackups(): List<MetaWebDavBackupFile> {
            val requestBody = EMPTY_XML.toRequestBody("text/xml".toMediaType())
            val response = execute("PROPFIND", rootUrl, requestBody, headers = mapOf("Depth" to "1"))
            response.use {
                if (!it.isSuccessful && it.code != 207) {
                    throw IllegalStateException("PROPFIND failed with HTTP ${it.code}")
                }
                val xml = it.body.string()
                return parseListResponse(xml)
                    .filter { backup -> backup.name.endsWith(".zip", ignoreCase = true) }
            }
        }
        fun uploadBackup(file: File) {
            val requestBody = file.asRequestBody("application/zip".toMediaType())
            val response = execute("PUT", rootUrl + file.name, requestBody)
            response.use {
                if (!it.isSuccessful) {
                    throw IllegalStateException("PUT failed with HTTP ${it.code}")
                }
            }
        }
        fun downloadBackup(fileName: String, targetFile: File) {
            val response = execute("GET", rootUrl + fileName)
            response.use {
                if (!it.isSuccessful) {
                    throw IllegalStateException("GET failed with HTTP ${it.code}")
                }
                targetFile.parentFile?.mkdirs()
                FileOutputStream(targetFile).use { outputStream ->
                    it.body.byteStream().copyTo(outputStream)
                }
            }
        }
        private fun execute(
            method: String,
            url: String,
            body: RequestBody? = null,
            headers: Map<String, String> = emptyMap(),
        ): Response {
            val builder = Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
            headers.forEach { (key, value) -> builder.header(key, value) }
            builder.method(method, body)
            return httpClient.newCall(builder.build()).execute()
        }
        private fun parseListResponse(xml: String): List<MetaWebDavBackupFile> {
            if (xml.isBlank()) return emptyList()
            val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
                setInput(xml.reader())
            }
            val result = mutableListOf<MetaWebDavBackupFile>()
            var currentHref: String? = null
            var currentLastModified: Long = 0L
            var currentIsDir = false
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase(Locale.ROOT)) {
                            "href" -> currentHref = parser.nextText()
                            "getlastmodified" -> {
                                val raw = parser.nextText()
                                currentLastModified = parseHttpDate(raw)
                            }
                            "collection" -> currentIsDir = true
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.lowercase(Locale.ROOT) == "response") {
                            val href = currentHref
                            if (!href.isNullOrBlank() && !currentIsDir) {
                                val name = extractFileName(href)
                                if (name.isNotBlank()) {
                                    result += MetaWebDavBackupFile(
                                        name = name,
                                        lastModified = currentLastModified,
                                    )
                                }
                            }
                            currentHref = null
                            currentLastModified = 0L
                            currentIsDir = false
                        }
                    }
                }
                parser.next()
            }
            return result
        }
        private fun extractFileName(href: String): String {
            val decoded = URLDecoder.decode(href, Charsets.UTF_8.name())
            return decoded.trimEnd('/').substringAfterLast('/', "")
        }
        private fun parseHttpDate(raw: String?): Long {
            if (raw.isNullOrBlank()) return 0L
            return runCatching {
                ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
            }.getOrDefault(0L)
        }
    }
}
