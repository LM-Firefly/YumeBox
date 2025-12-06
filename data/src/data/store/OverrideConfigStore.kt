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



package com.github.yumelira.yumebox.data.store

import android.content.Context
import com.github.yumelira.yumebox.core.model.*
import com.github.yumelira.yumebox.data.util.OFFICIAL_MRS_PRESET_SUMMARY
import com.github.yumelira.yumebox.data.util.OFFICIAL_MRS_PRESET_TITLE
import com.github.yumelira.yumebox.data.model.OverrideConfig
import com.github.yumelira.yumebox.data.model.MetadataIndex
import com.github.yumelira.yumebox.data.model.OverrideMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

class OverrideConfigStore(
    private val context: Context,
) : OverrideConfigProvider {
    companion object {
        const val INTERNAL_RUNTIME_PREFIX = "__runtime__"

        fun isInternalRuntimeConfig(id: String): Boolean = id.startsWith(INTERNAL_RUNTIME_PREFIX)
    }

    private val overridesDir = File(context.filesDir, "overrides")
    private val configsDir = File(overridesDir, "configs")
    private val internalDir = File(overridesDir, OverrideInternalConstants.INTERNAL_DIR_NAME)
    private val metadataFile = File(overridesDir, "metadata.json")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val _configsFlow = MutableStateFlow<List<OverrideConfig>>(emptyList())

    @Volatile
    private var systemPresetsCache: List<OverrideConfig>? = null

    private suspend fun refreshConfigsFlow() {
        _configsFlow.value = getAll()
    }

    private suspend fun updateConfigsFlowSnapshot(
        metadataIndex: MetadataIndex,
        userConfigsById: Map<String, OverrideConfig>,
    ) {
        val currentSnapshot = _configsFlow.value
        if (currentSnapshot.isEmpty()) {
            refreshConfigsFlow()
            return
        }
        val systemPresets = currentSnapshot
            .filter(OverrideConfig::isSystem)
            .ifEmpty { getSystemPresets() }
        _configsFlow.value = systemPresets + metadataIndex.sortedUserMetadata().mapNotNull { metadata ->
            userConfigsById[metadata.id]
        }
    }

    override suspend fun getAll(): List<OverrideConfig> = withContext(Dispatchers.IO) {
        val systemPresets = getSystemPresets()
        val userConfigs = loadUserConfigs()
        systemPresets + userConfigs
    }

    override fun getAllFlow(): Flow<List<OverrideConfig>> = _configsFlow.asStateFlow()

    override suspend fun getById(id: String): OverrideConfig? = withContext(Dispatchers.IO) {
        if (isSystemPreset(id)) {
            return@withContext getSystemPresets().find { it.id == id }
        }
        val metadata = loadMetadataIndex().getById(id) ?: return@withContext null
        val config = loadConfigContent(id) ?: return@withContext null
        OverrideConfig(
            id = metadata.id,
            name = metadata.name,
            description = metadata.description,
            config = config,
            isSystem = false,
            createdAt = metadata.createdAt,
            updatedAt = metadata.updatedAt,
        )
    }

    override suspend fun getSystemPresets(): List<OverrideConfig> = withContext(Dispatchers.IO) {
        systemPresetsCache?.let { return@withContext it }

        try {
            val configOverride = buildOfficialMrsConfigurationOverride(
                defaultSystemOfficialMrsPresetSelection(),
            )
            val metadata = OverrideMetadata.createSystemPreset(
                name = OFFICIAL_MRS_PRESET_TITLE,
                description = OFFICIAL_MRS_PRESET_SUMMARY,
            )
            val presets = listOf(OverrideConfig(
                id = metadata.id,
                name = metadata.name,
                description = metadata.description,
                config = configOverride,
                isSystem = true,
                createdAt = metadata.createdAt,
                updatedAt = metadata.updatedAt,
            ))
            systemPresetsCache = presets
            presets
        } catch (e: Exception) {
            Timber.e(e, "Failed to build system presets")
            emptyList()
        }
    }

    override suspend fun getUserConfigs(): List<OverrideConfig> =
        loadUserConfigs().filter { it.id != OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID }

    override fun getUserConfigsFlow(): Flow<List<OverrideConfig>> =
        flow {
            emit(loadUserConfigs().filter { it.id != OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID })
        }.flowOn(Dispatchers.IO)

    override suspend fun save(config: OverrideConfig) = withContext(Dispatchers.IO) {
        if (isSystemPreset(config.id)) return@withContext

        configsDir.mkdirs()

        val configFile = configsDir.resolve("${config.id}.json")
        configFile.writeText(encodeConfigContent(config.config))

        val metadataIndex = loadMetadataIndex()
        val existingMetadata = metadataIndex.getById(config.id)
        val metadata = OverrideMetadata(
            id = config.id,
            name = config.name,
            description = config.description,
            isSystem = false,
            createdAt = config.createdAt,
            updatedAt = config.updatedAt,
            sortOrder = existingMetadata?.sortOrder ?: metadataIndex.nextUserSortOrder(),
        )
        val index = metadataIndex.upsert(metadata)
        saveMetadataIndex(index)
        val userConfigsById = _configsFlow.value
            .filterNot(OverrideConfig::isSystem)
            .associateBy(OverrideConfig::id)
            .toMutableMap()
            .apply { put(config.id, config) }
        updateConfigsFlowSnapshot(
            metadataIndex = index,
            userConfigsById = userConfigsById,
        )
    }

    override suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        if (isSystemPreset(id)) return@withContext false

        val configFile = configsDir.resolve("$id.json")
        val fileDeleted = !configFile.exists() || configFile.delete()
        val metadataExists = loadMetadataIndex().getById(id) != null
        if (fileDeleted && metadataExists) {
            val index = loadMetadataIndex().remove(id)
            saveMetadataIndex(index)
            val userConfigsById = _configsFlow.value
                .filterNot(OverrideConfig::isSystem)
                .associateBy(OverrideConfig::id)
                .toMutableMap()
                .apply { remove(id) }
            updateConfigsFlowSnapshot(
                metadataIndex = index,
                userConfigsById = userConfigsById,
            )
            return@withContext true
        }
        if (fileDeleted) {
            refreshConfigsFlow()
        }
        false
    }

    override suspend fun duplicate(id: String): OverrideConfig? = withContext(Dispatchers.IO) {
        val original = getById(id) ?: return@withContext null
        val newMetadata = OverrideMetadata(
            id = OverrideMetadata.generateId(),
            name = "${original.name} (副本)",
            description = original.description,
            isSystem = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val newConfig = OverrideConfig(
            id = newMetadata.id,
            name = newMetadata.name,
            description = newMetadata.description,
            config = original.config,
            isSystem = false,
            createdAt = newMetadata.createdAt,
            updatedAt = newMetadata.updatedAt,
        )
        save(newConfig)
        newConfig
    }

    override suspend fun exists(id: String): Boolean = withContext(Dispatchers.IO) {
        if (isSystemPreset(id)) return@withContext true
        configsDir.resolve("$id.json").exists()
    }

    override fun isSystemPreset(id: String): Boolean {
        return id.startsWith(OverrideMetadata.SYSTEM_PREFIX)
    }

    suspend fun loadCustomRouting(): ConfigurationOverride? = withContext(Dispatchers.IO) {
        val file = File(internalDir, OverrideInternalConstants.CUSTOM_ROUTING_FILE_NAME)
        if (!file.exists()) return@withContext null
        runCatching {
            json.decodeFromString<ConfigurationOverride>(file.readText())
        }.getOrNull()
    }

    suspend fun saveCustomRouting(config: ConfigurationOverride) = withContext(Dispatchers.IO) {
        val file = File(internalDir, OverrideInternalConstants.CUSTOM_ROUTING_FILE_NAME)
        file.parentFile?.mkdirs()
        file.writeText(encodeConfigContent(config))
    }

    suspend fun export(config: OverrideConfig): String = withContext(Dispatchers.IO) {
        encodeConfigContent(config.config)
    }

    suspend fun import(jsonString: String, name: String): OverrideConfig? = withContext(Dispatchers.IO) {
        try {
            val configOverride = json.decodeFromString(ConfigurationOverride.serializer(), jsonString)
            val now = System.currentTimeMillis()
            val metadata = OverrideMetadata(
                id = OverrideMetadata.generateId(),
                name = name,
                description = null,
                isSystem = false,
                createdAt = now,
                updatedAt = now,
            )
            val config = OverrideConfig(
                id = metadata.id,
                name = metadata.name,
                description = metadata.description,
                config = configOverride,
                isSystem = false,
                createdAt = metadata.createdAt,
                updatedAt = metadata.updatedAt,
            )
            save(config)
            config
        } catch (e: Exception) {
            null
        }
    }

    fun getConfigFilePath(id: String): File = configsDir.resolve("$id.json")
    fun getConfigsDirectory(): File = configsDir

    suspend fun reorderUserConfigs(orderedIds: List<String>) = withContext(Dispatchers.IO) {
        if (orderedIds.isEmpty()) return@withContext

        val metadataIndex = loadMetadataIndex()
        val sortedUserMetadata = metadataIndex.sortedUserMetadata()
        if (sortedUserMetadata.isEmpty()) return@withContext

        val userMetadataById = sortedUserMetadata.associateBy(OverrideMetadata::id)
        val reorderedIds = orderedIds.filter(userMetadataById::containsKey)
        if (reorderedIds.isEmpty()) return@withContext

        val remainingIds = sortedUserMetadata
            .map(OverrideMetadata::id)
            .filterNot(reorderedIds::contains)
        val finalOrder = reorderedIds + remainingIds
        val updatedConfigs = metadataIndex.configs.toMutableMap()
        var hasChanges = false

        finalOrder.forEachIndexed { index, id ->
            val metadata = userMetadataById[id] ?: return@forEachIndexed
            val newSortOrder = index.toLong() + 1L
            if (metadata.sortOrder != newSortOrder) {
                updatedConfigs[id] = metadata.copy(sortOrder = newSortOrder)
                hasChanges = true
            }
        }

        if (hasChanges) {
            val updatedIndex = metadataIndex.copy(configs = updatedConfigs)
            saveMetadataIndex(updatedIndex)
            val userConfigsById = _configsFlow.value
                .filterNot(OverrideConfig::isSystem)
                .associateBy(OverrideConfig::id)
            updateConfigsFlowSnapshot(
                metadataIndex = updatedIndex,
                userConfigsById = userConfigsById,
            )
        }
    }

    private fun loadUserConfigs(): List<OverrideConfig> {
        if (!configsDir.exists()) return emptyList()
        val index = loadMetadataIndex()
        return index.sortedUserMetadata()
            .mapNotNull { metadata ->
                if (isInternalRuntimeConfig(metadata.id)) {
                    return@mapNotNull null
                }
                loadConfigContent(metadata.id)?.let { config ->
                    OverrideConfig(
                        id = metadata.id,
                        name = metadata.name,
                        description = metadata.description,
                        config = config,
                        isSystem = false,
                        createdAt = metadata.createdAt,
                        updatedAt = metadata.updatedAt,
                    )
                }
            }
    }

    fun getConfigJsonContent(id: String): String? {
        val file = configsDir.resolve("$id.json")
        if (!file.exists()) return null
        return try {
            file.readText()
        } catch (e: Exception) {
            null
        }
    }

    fun saveConfigJsonContent(id: String, content: String): Boolean {
        if (isSystemPreset(id)) return false
        configsDir.mkdirs()
        val configFile = configsDir.resolve("$id.json")
        return try {
            configFile.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun loadConfigContent(id: String): ConfigurationOverride? {
        val file = configsDir.resolve("$id.json")
        if (!file.exists()) return null
        return try {
            val raw = file.readText()
            val decoded = json.decodeFromString(ConfigurationOverride.serializer(), raw)
            val cleaned = encodeConfigContent(decoded)
            if (cleaned != raw) {
                file.writeText(cleaned)
            }
            decoded
        } catch (e: Exception) {
            null
        }
    }

    private fun encodeConfigContent(config: ConfigurationOverride): String {
        return encodeConfigurationOverride(config)
    }

    private fun loadMetadataIndex(): MetadataIndex {
        val metadataIndex = if (!metadataFile.exists()) {
            MetadataIndex()
        } else {
            try {
                json.decodeFromString(MetadataIndex.serializer(), metadataFile.readText())
            } catch (e: Exception) {
                MetadataIndex()
            }
        }
        val normalizedIndex = metadataIndex.normalizeUserSortOrders()
        if (normalizedIndex != metadataIndex) {
            saveMetadataIndex(normalizedIndex)
        }
        return normalizedIndex
    }

    private fun saveMetadataIndex(index: MetadataIndex) {
        overridesDir.mkdirs()
        metadataFile.writeText(json.encodeToString(MetadataIndex.serializer(), index))
    }
}
