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



package com.github.yumelira.yumebox.data.migration

import android.content.Context
import com.github.yumelira.yumebox.data.repository.OverrideConfigRepository
import com.github.yumelira.yumebox.data.repository.ProfileBindingRepository
import com.github.yumelira.yumebox.domain.model.OverrideConfig
import com.github.yumelira.yumebox.domain.model.OverrideMetadata
import com.github.yumelira.yumebox.domain.model.ProfileBinding
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

object OverrideConfigMigration {

    private const val MIGRATION_KEY = "override_config_migrated_v2"
    private const val MIGRATION_VERSION = 2

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun isMigrated(mmkv: MMKV): Boolean {
        val version = mmkv.decodeInt(MIGRATION_KEY, 0)
        return version >= MIGRATION_VERSION
    }

    private fun markMigrated(mmkv: MMKV) {
        mmkv.encode(MIGRATION_KEY, MIGRATION_VERSION)
    }

    suspend fun migrate(
        context: Context,
        configRepo: OverrideConfigRepository,
        bindingRepo: ProfileBindingRepository,
        mmkv: MMKV,
    ): Boolean = withContext(Dispatchers.IO) {

        if (isMigrated(mmkv)) {
            Timber.d("[Migration] Already migrated to v$MIGRATION_VERSION, skipping")
            return@withContext true
        }

        Timber.i("[Migration] Starting OverrideConfig migration to v$MIGRATION_VERSION")

        var success = true

        try {
            val templatesMigrated = migrateTemplates(context, configRepo)
            Timber.i("[Migration] Migrated $templatesMigrated templates to configs")
        } catch (e: Exception) {
            Timber.e(e, "[Migration] Failed to migrate templates")
            success = false
        }

        try {
            val bindingsMigrated = migrateBindings(context, configRepo, bindingRepo)
            Timber.i("[Migration] Migrated $bindingsMigrated bindings")
        } catch (e: Exception) {
            Timber.e(e, "[Migration] Failed to migrate bindings")
            success = false
        }

        try {
            migrateLegacyOverrideJson(context, configRepo)
        } catch (e: Exception) {
            Timber.e(e, "[Migration] Failed to migrate legacy override.json")

        }

        if (success) {
            markMigrated(mmkv)
            Timber.i("[Migration] Successfully completed OverrideConfig migration")
        }

        success
    }

    private suspend fun migrateTemplates(
        context: Context,
        configRepo: OverrideConfigRepository,
    ): Int {
        val templatesDir = File(context.filesDir, "overrides/templates")
        if (!templatesDir.exists()) {
            return 0
        }

        var count = 0
        val files = templatesDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()

        for (file in files) {
            try {

                val content = file.readText()
                val oldTemplate = json.decodeFromString<OldOverrideTemplate>(content)

                val now = System.currentTimeMillis()
                val newConfig = OverrideConfig(
                    id = oldTemplate.id,
                    name = oldTemplate.name,
                    description = oldTemplate.description,
                    config = oldTemplate.config,
                    isSystem = oldTemplate.isSystem,
                    createdAt = oldTemplate.createdAt,
                    updatedAt = oldTemplate.updatedAt,
                )

                configRepo.save(newConfig)
                count++

                Timber.d("[Migration] Migrated template: ${oldTemplate.id} -> ${newConfig.id}")
            } catch (e: Exception) {
                Timber.w(e, "[Migration] Failed to migrate template from: ${file.name}")
            }
        }

        return count
    }

    private suspend fun migrateBindings(
        context: Context,
        configRepo: OverrideConfigRepository,
        bindingRepo: ProfileBindingRepository,
    ): Int {

        val mmkv = MMKV.defaultMMKV()
        val oldBindingsKey = "profile_override_bindings"
        val str = mmkv.decodeString(oldBindingsKey, "{}") ?: "{}"

        val oldBindingsMap: Map<String, OldProfileOverrideBinding> = try {
            json.decodeFromString(str)
        } catch (e: Exception) {
            Timber.w(e, "[Migration] Failed to parse old bindings")
            return 0
        }

        var count = 0

        for ((profileId, oldBinding) in oldBindingsMap) {
            try {

                val overrideIds = mutableListOf<String>()

                oldBinding.templateId?.let { overrideIds.add(it) }

                overrideIds.addAll(oldBinding.templateIds)

                for (groupId in oldBinding.groupIds) {
                    val groupFile = File(context.filesDir, "overrides/groups/$groupId.json")
                    if (groupFile.exists()) {
                        try {
                            val groupContent = groupFile.readText()
                            val group = json.decodeFromString<OldConfigGroup>(groupContent)
                            overrideIds.addAll(group.templateIds)
                        } catch (e: Exception) {
                            Timber.w(e, "[Migration] Failed to expand group: $groupId")
                        }
                    }
                }

                if (oldBinding.customConfig != null) {
                    val customConfigId = OverrideMetadata.generateId()
                    val now = System.currentTimeMillis()
                    val customConfig = OverrideConfig(
                        id = customConfigId,
                        name = "独立配置 (${profileId.take(8)})",
                        description = "从旧版独立配置迁移",
                        config = oldBinding.customConfig,
                        isSystem = false,
                        createdAt = now,
                        updatedAt = now,
                    )
                    configRepo.save(customConfig)
                    overrideIds.add(customConfigId)
                }

                val newBinding = ProfileBinding(
                    profileId = profileId,
                    overrideIds = overrideIds.distinct(),
                    enabled = oldBinding.enabled,
                )

                bindingRepo.setBinding(newBinding)
                count++

                Timber.d("[Migration] Migrated binding for profile: $profileId with ${overrideIds.size} overrides")
            } catch (e: Exception) {
                Timber.w(e, "[Migration] Failed to migrate binding for profile: $profileId")
            }
        }

        return count
    }

    private suspend fun migrateLegacyOverrideJson(
        context: Context,
        configRepo: OverrideConfigRepository,
    ) {
        val legacyFile = File(context.filesDir.parentFile, "clash/override.json")
        if (!legacyFile.exists()) {
            return
        }

        try {
            val content = legacyFile.readText()
            val config = json.decodeFromString<com.github.yumelira.yumebox.core.model.ConfigurationOverride>(content)

            val now = System.currentTimeMillis()
            val newConfig = OverrideConfig(
                id = OverrideMetadata.generateId(),
                name = "从旧配置导入",
                description = "由旧版 override.json 自动迁移",
                config = config,
                isSystem = false,
                createdAt = now,
                updatedAt = now,
            )

            configRepo.save(newConfig)

            val backupDir = File(context.filesDir, "overrides/imports")
            backupDir.mkdirs()
            val backupFile = File(backupDir, "legacy-backup-$now.json")
            legacyFile.copyTo(backupFile, overwrite = false)

            legacyFile.delete()

            Timber.i("[Migration] Migrated legacy override.json")
        } catch (e: Exception) {
            Timber.w(e, "[Migration] Failed to migrate legacy override.json")
        }
    }

    @kotlinx.serialization.Serializable
    private data class OldOverrideTemplate(
        val id: String,
        val name: String,
        val description: String? = null,
        val config: com.github.yumelira.yumebox.core.model.ConfigurationOverride,
        val isSystem: Boolean = false,
        val createdAt: Long,
        val updatedAt: Long,
    )

    @kotlinx.serialization.Serializable
    private data class OldProfileOverrideBinding(
        val profileId: String,
        val templateId: String? = null,
        val customConfig: com.github.yumelira.yumebox.core.model.ConfigurationOverride? = null,
        val enabled: Boolean = true,
        val groupIds: List<String> = emptyList(),
        val templateIds: List<String> = emptyList(),
    )

    @kotlinx.serialization.Serializable
    private data class OldConfigGroup(
        val id: String,
        val name: String,
        val templateIds: List<String> = emptyList(),
    )
}
