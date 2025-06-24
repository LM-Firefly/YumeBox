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

package tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream

abstract class DownloadGeoFilesTask : DefaultTask() {
    companion object {
        private val ALLOWED_SCHEMES = setOf("https")
        private val ALLOWED_HOSTS = setOf("github.com")
    }

    @get:Input
    abstract val assetUrls: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val destinationDir = outputDirectory.get().asFile
        destinationDir.mkdirs()
        val expectedFiles = assetUrls.get().keys

        destinationDir.listFiles()?.forEach { stale ->
            if (stale.name !in expectedFiles) {
                stale.deleteRecursively()
            }
        }

        assetUrls.get().forEach { (fileName, url) ->
            val outputFile = destinationDir.resolve(fileName)
            runCatching {
                val validatedUri = validateDownloadUri(url)
                validatedUri.toURL().openStream().use { input ->
                    val sourceIsXz = validatedUri.path.lowercase().endsWith(".xz")
                    val targetIsXz = fileName.lowercase().endsWith(".xz")
                    when {
                        targetIsXz && !sourceIsXz -> {
                            outputFile.outputStream().buffered().use { rawOutput ->
                                XZOutputStream(rawOutput, LZMA2Options()).use { xzOutput ->
                                    input.copyTo(xzOutput)
                                }
                            }
                        }
                        else -> {
                            Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                }
                logger.lifecycle("$fileName downloaded to ${outputFile.absolutePath}")
            }.onFailure { error ->
                logger.warn("Failed to download $fileName from $url", error)
            }
        }
    }

    private fun validateDownloadUri(rawUrl: String): URI {
        val uri = try {
            URI(rawUrl)
        } catch (e: URISyntaxException) {
            throw IllegalArgumentException("Invalid download URL: $rawUrl", e)
        }

        val scheme = uri.scheme?.lowercase()
        require(scheme in ALLOWED_SCHEMES) {
            "Unsupported download URL scheme '$scheme', only https is allowed"
        }
        require(uri.userInfo == null) {
            "User info is not allowed in download URL"
        }
        require(uri.rawQuery == null && uri.rawFragment == null) {
            "Query/fragment is not allowed in download URL"
        }

        val host = uri.host?.lowercase()
        require(!host.isNullOrBlank()) {
            "Download URL host is missing"
        }
        require(host in ALLOWED_HOSTS) {
            "Download URL host '$host' is not in allowlist"
        }
        require(uri.port == -1 || uri.port == 443) {
            "Only default HTTPS port is allowed"
        }

        return uri
    }
}
