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

package com.github.yumelira.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.repository.OverrideRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber

class OverrideViewModel(
    private val overrideRepository: OverrideRepository
) : ViewModel() {

    enum class ImportMode {
        Merge,
        Replace,
    }

    data class PreviewResult(
        val snapshot: String,
        val warnings: List<String>,
    )

    data class RouteImportExportSelection(
        val ruleProviders: Boolean = true,
        val proxyGroups: Boolean = true,
        val rules: Boolean = true,
        val subRules: Boolean = true,
    )

    companion object {
        private const val TAG = "OverrideViewModel"
    }

    private val overrideJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val prettyOverrideJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    private val presetDetector = BuiltinRoutePresetDetector(overrideJson)

    private val _configuration = MutableStateFlow(ConfigurationOverride())
    val configuration: StateFlow<ConfigurationOverride> = _configuration.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedBuiltinRoutePreset = MutableStateFlow(BuiltinRoutePreset.None)
    val selectedBuiltinRoutePreset: StateFlow<BuiltinRoutePreset> = _selectedBuiltinRoutePreset.asStateFlow()

    private val _saveError = MutableSharedFlow<String?>()
    val saveError = _saveError.asSharedFlow()

    init {
        loadConfiguration()
    }

    private fun loadConfiguration() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = overrideRepository.loadPersist()
            result.onSuccess { config ->
                _configuration.value = config
                val detected = presetDetector.detect(config)
                _selectedBuiltinRoutePreset.value = detected
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Load override failed")
            }
            _isLoading.value = false
        }
    }

    fun resetConfiguration() {
        viewModelScope.launch {
            val result = overrideRepository.clearPersist()
            result.onSuccess {
                _configuration.value = ConfigurationOverride()
                _selectedBuiltinRoutePreset.value = BuiltinRoutePreset.None
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Reset override failed")
            }
        }
    }


    fun setHttpPort(port: Int?) {
        updateConfig { it.copy(httpPort = port) }
    }

    fun setSocksPort(port: Int?) {
        updateConfig { it.copy(socksPort = port) }
    }

    fun setMixedPort(port: Int?) {
        updateConfig { it.copy(mixedPort = port) }
    }

    fun setRedirectPort(port: Int?) {
        updateConfig { it.copy(redirectPort = port) }
    }

    fun setTproxyPort(port: Int?) {
        updateConfig { it.copy(tproxyPort = port) }
    }

    fun setAllowLan(allow: Boolean?) {
        updateConfig { it.copy(allowLan = allow) }
    }

    fun setBindAddress(address: String?) {
        updateConfig { it.copy(bindAddress = address) }
    }

    fun setIpv6(enabled: Boolean?) {
        updateConfig { it.copy(ipv6 = enabled) }
    }

    fun setMode(mode: TunnelState.Mode?) {
        updateConfig { it.copy(mode = mode) }
    }

    fun setLogLevel(level: LogMessage.Level?) {
        updateConfig { it.copy(logLevel = level) }
    }

    fun setExternalController(address: String?) {
        updateConfig { it.copy(externalController = address) }
    }

    fun setExternalControllerTLS(address: String?) {
        updateConfig { it.copy(externalControllerTLS = address) }
    }

    fun setSecret(secret: String?) {
        updateConfig { it.copy(secret = secret) }
    }


    fun setUnifiedDelay(enabled: Boolean?) {
        updateConfig { it.copy(unifiedDelay = enabled) }
    }

    fun setGeodataMode(enabled: Boolean?) {
        updateConfig { it.copy(geodataMode = enabled) }
    }

    fun setTcpConcurrent(enabled: Boolean?) {
        updateConfig { it.copy(tcpConcurrent = enabled) }
    }

    fun setFindProcessMode(mode: ConfigurationOverride.FindProcessMode?) {
        updateConfig { it.copy(findProcessMode = mode) }
    }

    fun setKeepAliveInterval(seconds: Int?) {
        updateConfig { it.copy(keepAliveInterval = seconds) }
    }

    fun setKeepAliveIdle(seconds: Int?) {
        updateConfig { it.copy(keepAliveIdle = seconds) }
    }


    fun setDnsEnable(enable: Boolean?) {
        updateConfig {
            it.copy(dns = it.dns.copy(enable = enable))
        }
    }

    fun setDnsPreferH3(enabled: Boolean?) {
        updateConfig {
            it.copy(dns = it.dns.copy(preferH3 = enabled))
        }
    }

    fun setDnsCacheAlgorithm(algorithm: String?) {
        updateConfig {
            it.copy(dns = it.dns.copy(cacheAlgorithm = algorithm))
        }
    }

    fun setDnsListen(address: String?) {
        updateConfig {
            it.copy(dns = it.dns.copy(listen = address))
        }
    }

    fun setDnsIpv6(enabled: Boolean?) {
        updateConfig {
            it.copy(dns = it.dns.copy(ipv6 = enabled))
        }
    }

    fun setDnsUseHosts(enabled: Boolean?) {
        updateConfig {
            it.copy(dns = it.dns.copy(useHosts = enabled))
        }
    }

    fun setDnsUseSystemHosts(enabled: Boolean?) {
        updateConfig {
            it.copy(dns = it.dns.copy(useSystemHosts = enabled))
        }
    }

    fun setDnsRespectRules(enabled: Boolean?) {
        updateConfig {
            it.copy(dns = it.dns.copy(respectRules = enabled))
        }
    }

    fun setDnsEnhancedMode(mode: ConfigurationOverride.DnsEnhancedMode?) {
        updateConfig {
            it.copy(dns = it.dns.copy(enhancedMode = mode))
        }
    }

    fun setDnsNameServer(servers: List<String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(nameServer = servers))
        }
    }

    fun setDnsFallback(servers: List<String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(fallback = servers))
        }
    }

    fun setDnsDefaultServer(servers: List<String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(defaultServer = servers))
        }
    }

    fun setDnsFakeIpFilter(filters: List<String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(fakeIpFilter = filters))
        }
    }

    fun setDnsFakeIpRange(range: String?) {
        updateConfig {
            it.copy(dns = it.dns.copy(fakeIpRange = range))
        }
    }

    fun setDnsFakeIpFilterMode(mode: ConfigurationOverride.FilterMode?) {
        updateConfig {
            it.copy(dns = it.dns.copy(fakeIPFilterMode = mode))
        }
    }

    fun setDnsFallbackGeoIp(enabled: Boolean?) {
        updateConfig {
            val newFilter = it.dns.fallbackFilter.copy(geoIp = enabled)
            it.copy(dns = it.dns.copy(fallbackFilter = newFilter))
        }
    }

    fun setDnsFallbackGeoIpCode(code: String?) {
        updateConfig {
            val newFilter = it.dns.fallbackFilter.copy(geoIpCode = code)
            it.copy(dns = it.dns.copy(fallbackFilter = newFilter))
        }
    }

    fun setDnsFallbackDomain(domains: List<String>?) {
        updateConfig {
            val newFilter = it.dns.fallbackFilter.copy(domain = domains)
            it.copy(dns = it.dns.copy(fallbackFilter = newFilter))
        }
    }

    fun setDnsFallbackIpcidr(cidrs: List<String>?) {
        updateConfig {
            val newFilter = it.dns.fallbackFilter.copy(ipcidr = cidrs)
            it.copy(dns = it.dns.copy(fallbackFilter = newFilter))
        }
    }

    fun setDnsFallbackGeosite(geosite: List<String>?) {
        updateConfig {
            val newFilter = it.dns.fallbackFilter.copy(geosite = geosite)
            it.copy(dns = it.dns.copy(fallbackFilter = newFilter))
        }
    }

    fun setDnsProxyServerNameserver(servers: List<String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(proxyServerNameserver = servers))
        }
    }

    fun setDnsNameserverPolicy(policy: Map<String, String>?) {
        updateConfig {
            it.copy(dns = it.dns.copy(nameserverPolicy = policy))
        }
    }

    fun setAppendSystemDns(enabled: Boolean?) {
        updateConfig {
            it.copy(app = it.app.copy(appendSystemDns = enabled))
        }
    }


    fun setAuthentication(auth: List<String>?) {
        updateConfig { it.copy(authentication = auth) }
    }

    fun setHosts(hosts: Map<String, String>?) {
        updateConfig { it.copy(hosts = hosts) }
    }


    fun setExternalControllerCorsAllowOrigins(origins: List<String>?) {
        updateConfig {
            it.copy(externalControllerCors = it.externalControllerCors.copy(allowOrigins = origins))
        }
    }

    fun setExternalControllerCorsAllowPrivateNetwork(allow: Boolean?) {
        updateConfig {
            it.copy(externalControllerCors = it.externalControllerCors.copy(allowPrivateNetwork = allow))
        }
    }


    fun setSnifferEnable(enable: Boolean?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(enable = enable))
        }
    }

    fun setSnifferForceDnsMapping(enabled: Boolean?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(forceDnsMapping = enabled))
        }
    }

    fun setSnifferParsePureIp(enabled: Boolean?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(parsePureIp = enabled))
        }
    }

    fun setSnifferOverrideDestination(enabled: Boolean?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(overrideDestination = enabled))
        }
    }

    fun setSnifferHttpPorts(ports: List<String>?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(http = it.sniffer.sniff.http.copy(ports = ports))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferHttpOverride(enabled: Boolean?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(http = it.sniffer.sniff.http.copy(overrideDestination = enabled))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferTlsPorts(ports: List<String>?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(tls = it.sniffer.sniff.tls.copy(ports = ports))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferTlsOverride(enabled: Boolean?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(tls = it.sniffer.sniff.tls.copy(overrideDestination = enabled))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferQuicPorts(ports: List<String>?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(quic = it.sniffer.sniff.quic.copy(ports = ports))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferQuicOverride(enabled: Boolean?) {
        updateConfig {
            val newSniff = it.sniffer.sniff.copy(quic = it.sniffer.sniff.quic.copy(overrideDestination = enabled))
            it.copy(sniffer = it.sniffer.copy(sniff = newSniff))
        }
    }

    fun setSnifferForceDomain(domains: List<String>?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(forceDomain = domains))
        }
    }

    fun setSnifferSkipDomain(domains: List<String>?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(skipDomain = domains))
        }
    }

    fun setSnifferSkipSrcAddress(addresses: List<String>?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(skipSrcAddress = addresses))
        }
    }

    fun setSnifferSkipDstAddress(addresses: List<String>?) {
        updateConfig {
            it.copy(sniffer = it.sniffer.copy(skipDstAddress = addresses))
        }
    }

    fun setGeoXGeoIp(url: String?) {
        updateConfig {
            it.copy(geoxurl = it.geoxurl.copy(geoip = url))
        }
    }

    fun setGeoXMmdb(url: String?) {
        updateConfig {
            it.copy(geoxurl = it.geoxurl.copy(mmdb = url))
        }
    }

    fun setGeoXGeosite(url: String?) {
        updateConfig {
            it.copy(geoxurl = it.geoxurl.copy(geosite = url))
        }
    }

    fun setRuleProviders(value: Map<String, Map<String, kotlinx.serialization.json.JsonElement>>?) {
        updateConfig { it.copy(ruleProviders = value) }
    }

    fun setProxyGroups(value: List<Map<String, kotlinx.serialization.json.JsonElement>>?) {
        updateConfig { it.copy(proxyGroups = value) }
    }

    fun setPrependRules(value: List<String>?) {
        updateConfig { it.copy(prependRules = value) }
    }

    fun setRules(value: List<String>?) {
        updateConfig { it.copy(rules = value) }
    }

    fun setSubRules(value: Map<String, List<String>>?) {
        updateConfig { it.copy(subRules = value) }
    }

    fun applyBuiltinRoutePreset(preset: BuiltinRoutePreset): Result<Unit> {
        return runCatching {
            if (preset == BuiltinRoutePreset.None) {
                val updated = _configuration.value.copy(
                    ruleProviders = null,
                    proxyGroups = null,
                    prependRules = null,
                    rules = null,
                    subRules = null,
                )
                _configuration.value = updated
                _selectedBuiltinRoutePreset.value = BuiltinRoutePreset.None
                viewModelScope.launch {
                    overrideRepository.savePersist(updated).onFailure { e ->
                        Timber.tag(TAG).e(e, "Clear builtin route preset failed")
                    }
                }
                return@runCatching
            }

            val fileName = when (preset) {
                BuiltinRoutePreset.PresetA -> "override.json"
                BuiltinRoutePreset.PresetB -> "override2.json"
                BuiltinRoutePreset.None -> error("Unexpected preset")
            }

            val presetText = loadResourceText(fileName)
                ?: throw IllegalArgumentException("Preset file not found: $fileName")

            val presetConfig = try {
                overrideJson.decodeFromString(ConfigurationOverride.serializer(), presetText)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to parse preset: $fileName")
                throw e
            }
            val updated = _configuration.value.copy(
                ruleProviders = presetConfig.ruleProviders,
                proxyGroups = presetConfig.proxyGroups,
                prependRules = presetConfig.prependRules,
                rules = presetConfig.rules,
                subRules = presetConfig.subRules,
            )

            _configuration.value = updated
            _selectedBuiltinRoutePreset.value = preset
            viewModelScope.launch {
                overrideRepository.savePersist(updated).onFailure { e ->
                    Timber.tag(TAG).e(e, "Save builtin route preset failed")
                }
            }
        }
    }

    fun exportToJson(): String {
        return prettyOverrideJson.encodeToString(ConfigurationOverride.serializer(), _configuration.value)
    }

    fun exportSelectedRoutesToJson(selection: RouteImportExportSelection): String {
        val base = ConfigurationOverride()
        val source = _configuration.value
        val selected = base.copy(
            ruleProviders = if (selection.ruleProviders) source.ruleProviders else null,
            proxyGroups = if (selection.proxyGroups) source.proxyGroups else null,
            prependRules = if (selection.rules) source.prependRules else null,
            rules = if (selection.rules) source.rules else null,
            subRules = if (selection.subRules) source.subRules else null,
        )
        return prettyOverrideJson.encodeToString(ConfigurationOverride.serializer(), selected)
    }

    fun importSelectedRoutesFromJson(
        jsonText: String,
        selection: RouteImportExportSelection,
    ): Result<Unit> {
        return runCatching {
            val imported = overrideJson.decodeFromString(ConfigurationOverride.serializer(), jsonText)
            val target = _configuration.value.copy(
                ruleProviders = if (selection.ruleProviders) imported.ruleProviders else _configuration.value.ruleProviders,
                proxyGroups = if (selection.proxyGroups) imported.proxyGroups else _configuration.value.proxyGroups,
                prependRules = if (selection.rules) imported.prependRules else _configuration.value.prependRules,
                rules = if (selection.rules) imported.rules else _configuration.value.rules,
                subRules = if (selection.subRules) imported.subRules else _configuration.value.subRules,
            )
            _configuration.value = target
            syncBuiltinRoutePreset(target)
            viewModelScope.launch {
                overrideRepository.savePersist(target).onFailure { e ->
                    Timber.tag(TAG).e(e, "Import selected routes save failed")
                }
            }
        }
    }

    fun importFromJson(jsonText: String, mode: ImportMode): Result<Unit> {
        return runCatching {
            val imported = overrideJson.decodeFromString(ConfigurationOverride.serializer(), jsonText)
            val target = when (mode) {
                ImportMode.Replace -> imported
                ImportMode.Merge -> ConfigurationOverrideMerger.merge(_configuration.value, imported)
            }
            _configuration.value = target
            syncBuiltinRoutePreset(target)
            viewModelScope.launch {
                overrideRepository.savePersist(target).onFailure { e ->
                    Timber.tag(TAG).e(e, "Import save override failed")
                }
            }
        }
    }

    fun previewCurrent(): PreviewResult {
        val snapshot = exportToJson()
        val warnings = RuleValidator.validate(_configuration.value)
        return PreviewResult(snapshot = snapshot, warnings = warnings)
    }



    private fun updateConfig(transform: (ConfigurationOverride) -> ConfigurationOverride) {
        val updated = transform(_configuration.value)
        viewModelScope.launch {
            val result = overrideRepository.savePersist(updated)
            result.onSuccess {
                _configuration.value = updated
                syncBuiltinRoutePreset(updated)
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Save override failed")
                _saveError.emit(e.message ?: "Save failed")
            }
        }
    }

    private fun syncBuiltinRoutePreset(config: ConfigurationOverride) {
        _selectedBuiltinRoutePreset.value = presetDetector.detect(config)
    }

    private fun loadResourceText(fileName: String): String? {
        val loader = javaClass.classLoader ?: Thread.currentThread().contextClassLoader ?: return null
        return loader.getResourceAsStream(fileName)
            ?.bufferedReader()
            ?.use { it.readText() }
    }
}
