package com.github.yumelira.yumebox.config

import android.content.Context
import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.CompileRequest
import com.github.yumelira.yumebox.data.repository.NetworkSettingsRepository
import com.github.yumelira.yumebox.runtime.client.ProfilesRepository
import com.github.yumelira.yumebox.service.runtime.util.importedDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TunProfileSync(
    private val context: Context,
    private val profilesRepository: ProfilesRepository,
    private val networkSettingsRepository: NetworkSettingsRepository,
) {
    suspend fun syncActiveProfile() = withContext(Dispatchers.IO) {
        val activeProfile = profilesRepository.queryActiveProfile()
        if (activeProfile == null) {
            applyRouteExcludeAddress(emptyList())
            return@withContext
        }

        val profileDir = context.importedDir.resolve(activeProfile.uuid.toString())
        val result = Clash.compilePreview(
            CompileRequest(
                profileUuid = activeProfile.uuid.toString(),
                profileDir = profileDir.absolutePath,
                profilePath = profileDir.resolve("config.yaml").absolutePath,
                overridePaths = emptyList(),
                outputPath = profileDir.resolve("runtime.yaml").absolutePath,
            ),
        )
        check(result.success) { result.error ?: "Lite tun profile preview failed" }

        val routeExcludeAddress = Clash.inspectCompiledConfigElement(result.finalYaml)
            .routeExcludeAddress()

        applyRouteExcludeAddress(routeExcludeAddress)
    }

    private fun applyRouteExcludeAddress(routeExcludeAddress: List<String>) {
        networkSettingsRepository.tunRouteExcludeAddress.set(routeExcludeAddress)
        networkSettingsRepository.bypassPrivateNetwork.set(false)
    }
}

private fun JsonObject?.routeExcludeAddress(): List<String> {
    val tun = this?.get("tun")?.jsonObject ?: return emptyList()
    val raw = tun["route-exclude-address"] as? JsonArray ?: return emptyList()
    return raw.mapNotNull { element ->
        runCatching { element.jsonPrimitive.content.trim() }.getOrNull()
    }.filter(String::isNotEmpty)
}
