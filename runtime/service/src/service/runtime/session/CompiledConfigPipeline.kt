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



package com.github.yumelira.yumebox.service.runtime.session

import android.content.Context
import android.util.Log
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.CompileRequest
import com.github.yumelira.yumebox.core.model.CompileResult
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.OverrideInternalConstants
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.core.model.buildOfficialMrsConfigurationOverride
import com.github.yumelira.yumebox.core.model.defaultSystemOfficialMrsPresetSelection
import com.github.yumelira.yumebox.core.model.encodeConfigurationOverride
import com.github.yumelira.yumebox.core.util.PROXY_PROVIDER_SCOPE
import com.github.yumelira.yumebox.core.util.RULE_PROVIDER_SCOPE
import com.github.yumelira.yumebox.core.util.profileProviderScopeDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.security.MessageDigest

class CompiledConfigPipeline(
    private val context: Context,
) {
    private val overrideEnabled = !context.packageName.endsWith(".lite")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun resolveOverridePaths(profileUuid: String): List<String> {
        return resolveOverrideBundle(profileUuid, logger = null).paths
    }

    fun resolveOverridePaths(
        profileUuid: String,
        logger: ((String) -> Unit)?,
    ): List<String> {
        return resolveOverrideBundle(profileUuid, logger).paths
    }

    fun resolveOverrideBundle(profileUuid: String): ResolvedOverrideBundle {
        return resolveOverrideBundle(profileUuid, logger = null)
    }

    fun resolveOverrideBundle(
        profileUuid: String,
        logger: ((String) -> Unit)?,
    ): ResolvedOverrideBundle {
        if (!overrideEnabled) {
            logger?.invoke("override resolve skipped for lite package=${context.packageName} profile=$profileUuid")
            return ResolvedOverrideBundle(
                profileUuid = profileUuid,
                userOverridePaths = emptyList(),
                builtinPresetPath = null,
                customRoutingOverridePath = null,
                runtimeInternalOverridePath = null,
                paths = emptyList(),
            )
        }

        val overridesDir = context.filesDir.resolve("overrides")
        val metadataFile = overridesDir.resolve("metadata.json")
        val metadata = loadMetadataIndex(overridesDir, metadataFile, logger)
        val metadataRaw = metadataFile.takeIf(File::exists)?.readText().orEmpty()

        val binding = metadata.profileChains[profileUuid]
        val systemPresetEnabled = binding?.enabled ?: false
        logger?.invoke(
            "override resolve: profile=$profileUuid profileChain=" +
                (binding?.let { json.encodeToString(ProfileChainPayload.serializer(), it) } ?: "<none>"),
        )

        val userOverridePaths = mutableListOf<String>()
        val customRoutingEnabled = binding?.overrideIds
            .orEmpty()
            .contains(OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID)
        binding
            ?.overrideIds
            .orEmpty()
            .filterNot(::isReservedOverrideId)
            .filterNot(::isCustomRoutingId)
            .forEach { overrideId ->
                val file = resolveUserOverrideFile(overridesDir, overrideId)
                if (file == null) {
                    error("Override config not found for profile=$profileUuid id=$overrideId")
                }
                logger?.invoke(describeOverrideFile(file, overrideId))
                userOverridePaths += file.absolutePath
            }

        val builtinPresetPath = if (systemPresetEnabled) {
            resolveBuiltinPresetFile(overridesDir)?.also { file ->
                logger?.invoke(describeOverrideFile(file, BUILTIN_PRESET_FILE_ID))
            }?.absolutePath
        } else {
            null
        }

        val customRoutingOverridePath = if (customRoutingEnabled) {
            resolveCustomRoutingOverrideFile(overridesDir)?.also { file ->
                logger?.invoke(describeOverrideFile(file, OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID))
            }?.absolutePath
        } else {
            null
        }

        val runtimeInternalOverridePath = resolveRuntimeInternalOverrideFile(overridesDir, profileUuid)
            ?.also { file -> logger?.invoke(describeOverrideFile(file, "__runtime__")) }
            ?.absolutePath

        val paths = mutableListOf<String>()
        builtinPresetPath?.let(paths::add)
        customRoutingOverridePath?.let(paths::add)
        paths += userOverridePaths
        runtimeInternalOverridePath?.let(paths::add)

        logger?.invoke(
            "override resolve: profile=$profileUuid resolved=${paths.size} " +
                paths.joinToString(prefix = "[", postfix = "]"),
        )

        return ResolvedOverrideBundle(
            profileUuid = profileUuid,
            userOverridePaths = userOverridePaths,
            builtinPresetPath = builtinPresetPath,
            customRoutingOverridePath = customRoutingOverridePath,
            runtimeInternalOverridePath = runtimeInternalOverridePath,
            paths = paths,
        )
    }

    suspend fun applyOverrideToRuntimeFile(spec: RuntimeSpec): String = withContext(Dispatchers.Default) {
        val request = buildRequest(spec)
        val result = Clash.compileToFile(request)
        check(result.success) { result.error ?: "apply override to runtime config failed" }
        validateCompiledProviderPaths(result.finalYaml, File(spec.profileDir))
        result.fingerprint
    }

    suspend fun previewGroups(spec: RuntimeSpec, excludeNotSelectable: Boolean): List<ProxyGroup> {
        val result = previewOverride(spec)
        if (!result.success || result.finalYaml.isBlank()) return emptyList()
        return withContext(Dispatchers.Default) {
            Clash.inspectCompiledGroups(result.finalYaml, File(spec.profileDir), excludeNotSelectable)
        }
    }

    suspend fun previewConfig(
        profileUuid: String,
        profileDir: File,
        overridePaths: List<String> = resolveOverrideBundle(profileUuid).paths,
    ): ConfigurationOverride = withContext(Dispatchers.Default) {
        val request = CompileRequest(
            profileUuid = profileUuid,
            profileDir = profileDir.absolutePath,
            profilePath = profileDir.resolve("config.yaml").absolutePath,
            overridePaths = overridePaths,
            outputPath = profileDir.resolve("runtime.yaml").absolutePath,
        )
        val result = Clash.compilePreview(request)
        check(result.success) { result.error ?: "override preview failed" }
        validateCompiledProviderPaths(result.finalYaml, profileDir)
        Clash.inspectCompiledConfig(result.finalYaml) ?: ConfigurationOverride()
    }

    suspend fun previewOverride(spec: RuntimeSpec): CompileResult = withContext(Dispatchers.Default) {
        val result = Clash.compilePreview(buildRequest(spec))
        check(result.success) { result.error ?: "override preview failed" }
        validateCompiledProviderPaths(result.finalYaml, File(spec.profileDir))
        result
    }

    private fun buildRequest(spec: RuntimeSpec): CompileRequest {
        val profileDir = File(spec.profileDir)
        return CompileRequest(
            profileUuid = spec.profileUuid,
            profileDir = profileDir.absolutePath,
            profilePath = profileDir.resolve("config.yaml").absolutePath,
            overridePaths = spec.overridePaths,
            outputPath = spec.runtimeConfigPath.ifBlank { profileDir.resolve("runtime.yaml").absolutePath },
        )
    }

    private fun validateCompiledProviderPaths(finalYaml: String, profileDir: File) {
        val rulesBase = profileProviderScopeDir(profileDir, RULE_PROVIDER_SCOPE).absolutePath.replace('\\', '/').trimEnd('/')
        val proxiesBase = profileProviderScopeDir(profileDir, PROXY_PROVIDER_SCOPE).absolutePath.replace('\\', '/').trimEnd('/')
        val invalidPaths = mutableListOf<String>()
        PATH_PATTERN.findAll(finalYaml).forEach { match ->
            val pathValue = match.groupValues[1].replace('\\', '/').trim()
            if (!pathValue.endsWith(".yaml") && !pathValue.endsWith(".yml") && !pathValue.endsWith(".mrs")) {
                return@forEach
            }
            val isLegacyPath = pathValue.startsWith("./ruleset/") ||
                pathValue.startsWith("ruleset/") ||
                pathValue.contains("/clash/")
            val inProfileProviders = pathValue.startsWith("$rulesBase/") || pathValue.startsWith("$proxiesBase/")
            if (isLegacyPath || !inProfileProviders) {
                invalidPaths += pathValue
            }
        }
        if (invalidPaths.isNotEmpty()) {
            invalidPaths.forEach { invalidPath ->
                Log.e(TAG, "Compiled provider path invalid: $invalidPath")
            }
            error("Compiled provider path escaped profile scope: ${invalidPaths.first()}")
        }
        if (PATH_PATTERN.containsMatchIn(finalYaml)) {
            Log.i(TAG, "Compiled provider paths validated: rulesBase=$rulesBase proxiesBase=$proxiesBase")
        }
    }

    private fun loadMetadataIndex(
        overridesDir: File,
        metadataFile: File,
        logger: ((String) -> Unit)?,
    ): MetadataIndexPayload {
        val metadataRaw = metadataFile.takeIf(File::exists)?.readText().orEmpty()
        val metadata = if (metadataFile.exists()) {
            runCatching {
                json.decodeFromString(MetadataIndexPayload.serializer(), metadataRaw)
            }.getOrElse {
                logger?.invoke(
                    "override resolve: metadata decode failed path=${metadataFile.absolutePath} " +
                        "size=${metadataRaw.length} sha=${metadataRaw.sha256Short()}",
                )
                MetadataIndexPayload()
            }
        } else {
            MetadataIndexPayload()
        }
        val sanitized = sanitizeMetadataIndex(metadata)
        if (sanitized != metadata || metadataRaw.contains("\"enableSystemPreset\"")) {
            overridesDir.mkdirs()
            metadataFile.writeText(json.encodeToString(MetadataIndexPayload.serializer(), sanitized))
            logger?.invoke(
                "override resolve: metadata normalized path=${metadataFile.absolutePath}",
            )
        }
        return sanitized
    }

    private fun sanitizeMetadataIndex(metadata: MetadataIndexPayload): MetadataIndexPayload {
        return metadata.copy(
            profileChains = metadata.profileChains.mapValues { (_, binding) ->
                binding.copy(overrideIds = binding.overrideIds.filterNot(::isBuiltinPresetId))
            },
        )
    }

    private fun resolveUserOverrideFile(overridesDir: File, overrideId: String): File? {
        val configFile = overridesDir.resolve("configs/$overrideId.json")
        return configFile.takeIf(File::exists)
    }

    private fun resolveRuntimeInternalOverrideFile(overridesDir: File, profileUuid: String): File? {
        val file = overridesDir.resolve("configs/${INTERNAL_RUNTIME_PREFIX}-profile-$profileUuid.json")
        if (!file.exists()) return null
        val content = runCatching { file.readText() }.getOrElse {
            error("Runtime override file unreadable path=${file.absolutePath} reason=${it.message}")
        }
        if (content.isBlank()) {
            runCatching { file.delete() }
            return null
        }
        return file
    }

    private fun resolveCustomRoutingOverrideFile(overridesDir: File): File? {
        val file = overridesDir.resolve(
            "${OverrideInternalConstants.INTERNAL_DIR_NAME}/${OverrideInternalConstants.CUSTOM_ROUTING_FILE_NAME}",
        )
        if (!file.exists()) return null
        val content = runCatching { file.readText() }.getOrElse {
            error("Custom routing override file unreadable path=${file.absolutePath} reason=${it.message}")
        }
        if (content.isBlank()) {
            runCatching { file.delete() }
            return null
        }
        return file
    }

    private fun resolveBuiltinPresetFile(overridesDir: File): File? {
        val content = runCatching {
            encodeConfigurationOverride(
                buildOfficialMrsConfigurationOverride(defaultSystemOfficialMrsPresetSelection()),
            )
        }.getOrElse {
            Log.e(TAG, "Failed to build builtin preset file", it)
            return null
        }
        if (content.isBlank()) return null

        val file = overridesDir.resolve("${OverrideInternalConstants.INTERNAL_DIR_NAME}/$BUILTIN_PRESET_FILE_NAME")
        file.parentFile?.mkdirs()
        if (!file.exists() || file.readText() != content) {
            file.writeText(content)
        }
        return file
    }

    private fun isInternalRuntimeId(overrideId: String): Boolean {
        return overrideId.startsWith(INTERNAL_RUNTIME_PREFIX)
    }

    private fun isBuiltinPresetId(overrideId: String): Boolean {
        return overrideId.startsWith(BUILTIN_PRESET_PREFIX)
    }

    private fun isReservedOverrideId(overrideId: String): Boolean {
        return isInternalRuntimeId(overrideId) || isBuiltinPresetId(overrideId)
    }

    private fun isCustomRoutingId(overrideId: String): Boolean {
        return overrideId == OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
    }

    private fun describeOverrideFile(file: File, overrideId: String): String {
        val content = file.takeIf(File::exists)?.readText().orEmpty()
        return buildString {
            append("override resolve: file id=")
            append(overrideId)
            append(" path=")
            append(file.absolutePath)
            append(" exists=")
            append(file.exists())
            append(" size=")
            append(content.length)
            append(" sha=")
            append(content.sha256Short())
            content.lineSequence()
                .map(String::trim)
                .firstOrNull { it.isNotEmpty() }
                ?.let {
                    append(" firstLine=")
                    append(it.take(160))
                }
        }
    }

    private fun String.sha256Short(): String {
        if (isBlank()) return "empty"
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    data class ResolvedOverrideBundle(
        val profileUuid: String,
        val userOverridePaths: List<String>,
        val builtinPresetPath: String?,
        val customRoutingOverridePath: String?,
        val runtimeInternalOverridePath: String?,
        val paths: List<String>,
    )

    @Serializable
    private data class MetadataIndexPayload(
        val configs: JsonObject = buildJsonObject {},
        val profileChains: Map<String, ProfileChainPayload> = emptyMap(),
    )

    @Serializable
    private data class ProfileChainPayload(
        val enabled: Boolean = true,
        val overrideIds: List<String> = emptyList(),
    )

    private companion object {
        private const val TAG = "CompiledConfigPipeline"
        private val PATH_PATTERN = Regex("""(?m)path:\s*["']?([^"'\n]+)["']?""")
        const val INTERNAL_RUNTIME_PREFIX = "__runtime__"
        const val BUILTIN_PRESET_PREFIX = "preset-"
        const val BUILTIN_PRESET_FILE_ID = "__builtin__"
        const val BUILTIN_PRESET_FILE_NAME = "builtin-preset.json"
    }
}
