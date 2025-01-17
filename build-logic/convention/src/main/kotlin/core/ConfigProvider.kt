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
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package core

import org.gradle.api.Project
import java.util.Properties

class ConfigProvider(private val project: Project) {
    private val catalog by lazy { project.rootProject.extensions.findByName("libs") }
    private val externalProperties by lazy { loadExternalProperties() }

    private fun loadExternalProperties(): Properties {
        val file = project.rootProject.file("kernel.properties")
        val props = Properties()
        if (file.isFile) {
            runCatching { file.inputStream().use(props::load) }
        }
        return props
    }

    private fun fromExternalConfig(key: String): String? {
        return externalProperties.getProperty(key)?.takeIf { it.isNotBlank() }
    }

    private fun invokeMethod(target: Any, name: String, vararg args: Any?): Any? =
        target.javaClass.methods.firstOrNull { it.name == name }?.let { method ->
            runCatching { method.invoke(target, *args) }.getOrNull()
        }

    private fun fromGropify(key: String): String? {
        val ext = project.extensions.findByName("gropify") ?: return null
        return (invokeMethod(ext, "getPropertyValue", key) as? String)?.takeIf { it.isNotBlank() }
    }

    private fun fromGradleProperties(key: String): String? {
        return project.findProperty(key)?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun fromCatalog(key: String): String? {
        val libs = catalog ?: return null
        val versionObj = invokeMethod(libs, "findVersion", key) ?: return null
        val reqVersion = invokeMethod(versionObj, "get") ?: return null
        val requiredVersion = invokeMethod(reqVersion, "getRequiredVersion") as? String
        return requiredVersion?.takeIf { it.isNotBlank() }
    }

    fun getString(key: String, fallback: String): String {
        return fromExternalConfig(key)
            ?: fromGropify(key)
            ?: fromGradleProperties(key)
            ?: fromCatalog(key)
            ?: fallback
    }

    fun getInt(key: String, fallback: Int): Int {
        return getString(key, fallback.toString()).toIntOrNull() ?: fallback
    }

    fun getCsv(key: String, fallback: String): List<String> {
        return getString(key, fallback).split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}

fun Project.gropifyString(path: String, fallback: String): String {
    val ext = extensions.findByName("gropify") ?: return fallback
    val method = ext.javaClass.methods.firstOrNull { it.name == "getPropertyValue" } ?: return fallback
    val value = runCatching {
        method.invoke(ext, path) as? String
    }.getOrNull()
    return if (value.isNullOrBlank()) fallback else value
}
