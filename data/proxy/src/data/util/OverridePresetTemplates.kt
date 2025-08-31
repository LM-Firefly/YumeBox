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



package com.github.yumelira.yumebox.data.util

import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private const val RULE_PROVIDER_INTERVAL = 86400
private const val URL_TEST_INTERVAL = 300
private const val URL_TEST_URL = "https://www.gstatic.com/generate_204"
private const val DEFAULT_EXCLUDE_FILTER = "(?i)GB|Traffic|Expire|Premium|频道|订阅|ISP|流量|到期|重置"
private const val GEO_GEOSITE_URL =
    "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geosite/%s.mrs"
private const val GEO_GEOIP_URL =
    "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geoip/%s.mrs"

enum class OverridePresetTemplateId(
    val title: String,
    val summary: String,
) {
    OfficialMrsCommon(
        title = "官方 MRS 常用分流",
        summary = "使用 Mihomo 官方 meta/geo mrs 规则集，按开关重建当前覆写里的规则提供者、策略组和规则。",
    ),
}

enum class OverridePresetRegion(
    val displayName: String,
    val groupName: String,
    val filter: String,
) {
    HK(
        displayName = "香港自动组",
        groupName = "HK Auto",
        filter = "(?i)(香港|HK|Hong Kong|🇭🇰)",
    ),
    TW(
        displayName = "台湾自动组",
        groupName = "TW Auto",
        filter = "(?i)(台湾|TW|Taiwan|🇹🇼)",
    ),
    JP(
        displayName = "日本自动组",
        groupName = "JP Auto",
        filter = "(?i)(日本|JP|Japan|东京|大阪|🇯🇵)",
    ),
    SG(
        displayName = "新加坡自动组",
        groupName = "SG Auto",
        filter = "(?i)(新加坡|SG|Singapore|狮城|🇸🇬)",
    ),
    US(
        displayName = "美国自动组",
        groupName = "US Auto",
        filter = "(?i)(美国|US|United States|America|洛杉矶|硅谷|🇺🇸)",
    ),
}

enum class OverridePresetItem(
    val id: String,
    val title: String,
    val summary: String,
    val isService: Boolean = false,
) {
    Ads(
        id = "ads",
        title = "广告拦截",
        summary = "启用 category-ads-all 并走 REJECT。",
    ),
    Private(
        id = "private",
        title = "私有地址直连",
        summary = "启用 private 规则集并走 DIRECT。",
    ),
    Google(
        id = "google",
        title = "Google",
        summary = "启用 Google 分流和专属策略组。",
        isService = true,
    ),
    Telegram(
        id = "telegram",
        title = "Telegram",
        summary = "启用 Telegram 分流和专属策略组。",
        isService = true,
    ),
    GitHub(
        id = "github",
        title = "GitHub",
        summary = "启用 GitHub 分流和专属策略组。",
        isService = true,
    ),
    Microsoft(
        id = "microsoft",
        title = "Microsoft",
        summary = "启用 Microsoft 分流和专属策略组。",
        isService = true,
    ),
    Apple(
        id = "apple",
        title = "Apple",
        summary = "启用 Apple 分流和专属策略组。",
        isService = true,
    ),
    YouTube(
        id = "youtube",
        title = "YouTube",
        summary = "启用 YouTube 分流和专属策略组。",
        isService = true,
    ),
    Netflix(
        id = "netflix",
        title = "Netflix",
        summary = "启用 Netflix 分流和专属策略组。",
        isService = true,
    ),
    Spotify(
        id = "spotify",
        title = "Spotify",
        summary = "启用 Spotify 分流和专属策略组。",
        isService = true,
    ),
    OpenAI(
        id = "openai",
        title = "OpenAI",
        summary = "启用 OpenAI 分流和专属策略组。",
        isService = true,
    ),
    Steam(
        id = "steam",
        title = "Steam",
        summary = "启用 Steam 分流和专属策略组。",
        isService = true,
    ),
    Cn(
        id = "cn",
        title = "中国大陆直连",
        summary = "启用 cn 域名和 IP 规则并走 DIRECT。",
    ),
    Proxy(
        id = "proxy",
        title = "代理规则集",
        summary = "启用 proxy 规则集并走 Proxy。",
    ),
    GeolocationNotCn(
        id = "geolocation_not_cn",
        title = "境外地理规则",
        summary = "启用 geolocation-!cn 并走 Proxy。",
    ),
    Match(
        id = "match",
        title = "兜底 MATCH",
        summary = "末尾追加 MATCH,Proxy。",
    ),
}

data class OverridePresetTemplateSelection(
    val templateId: OverridePresetTemplateId = OverridePresetTemplateId.OfficialMrsCommon,
    val regions: Set<OverridePresetRegion> = emptySet(),
    val enabledItems: Set<OverridePresetItem> = defaultEnabledPresetItems(),
)

private enum class RuleBehavior(
    val wireName: String,
) {
    Domain("domain"),
    IpCidr("ipcidr"),
}

private data class RuleProviderTemplate(
    val id: String,
    val remoteName: String,
    val behavior: RuleBehavior,
) {
    val urlTemplate: String
        get() = when (behavior) {
            RuleBehavior.Domain -> GEO_GEOSITE_URL
            RuleBehavior.IpCidr -> GEO_GEOIP_URL
        }
}

private data class ServiceRuleTemplate(
    val providerId: String,
    val noResolve: Boolean = false,
)

private val itemOrder = listOf(
    OverridePresetItem.Ads,
    OverridePresetItem.Private,
    OverridePresetItem.Google,
    OverridePresetItem.Telegram,
    OverridePresetItem.GitHub,
    OverridePresetItem.Microsoft,
    OverridePresetItem.Apple,
    OverridePresetItem.YouTube,
    OverridePresetItem.Netflix,
    OverridePresetItem.Spotify,
    OverridePresetItem.OpenAI,
    OverridePresetItem.Steam,
    OverridePresetItem.Cn,
    OverridePresetItem.Proxy,
    OverridePresetItem.GeolocationNotCn,
    OverridePresetItem.Match,
)

private val serviceItems = itemOrder.filter(OverridePresetItem::isService)

private val defaultEnabledItems = linkedSetOf(
    OverridePresetItem.Ads,
    OverridePresetItem.Private,
    OverridePresetItem.Google,
    OverridePresetItem.Telegram,
    OverridePresetItem.GitHub,
    OverridePresetItem.Microsoft,
    OverridePresetItem.Apple,
    OverridePresetItem.YouTube,
    OverridePresetItem.Netflix,
    OverridePresetItem.Spotify,
    OverridePresetItem.OpenAI,
    OverridePresetItem.Steam,
    OverridePresetItem.Cn,
    OverridePresetItem.Proxy,
    OverridePresetItem.GeolocationNotCn,
    OverridePresetItem.Match,
)

private val itemProviders = mapOf(
    OverridePresetItem.Ads to listOf(
        RuleProviderTemplate("ads_domain", "category-ads-all", RuleBehavior.Domain),
    ),
    OverridePresetItem.Private to listOf(
        RuleProviderTemplate("private_domain", "private", RuleBehavior.Domain),
    ),
    OverridePresetItem.Google to listOf(
        RuleProviderTemplate("google_domain", "google", RuleBehavior.Domain),
        RuleProviderTemplate("google_ip", "google", RuleBehavior.IpCidr),
    ),
    OverridePresetItem.Telegram to listOf(
        RuleProviderTemplate("telegram_domain", "telegram", RuleBehavior.Domain),
        RuleProviderTemplate("telegram_ip", "telegram", RuleBehavior.IpCidr),
    ),
    OverridePresetItem.GitHub to listOf(
        RuleProviderTemplate("github_domain", "github", RuleBehavior.Domain),
    ),
    OverridePresetItem.Microsoft to listOf(
        RuleProviderTemplate("microsoft_domain", "microsoft", RuleBehavior.Domain),
    ),
    OverridePresetItem.Apple to listOf(
        RuleProviderTemplate("apple_domain", "apple", RuleBehavior.Domain),
    ),
    OverridePresetItem.YouTube to listOf(
        RuleProviderTemplate("youtube_domain", "youtube", RuleBehavior.Domain),
    ),
    OverridePresetItem.Netflix to listOf(
        RuleProviderTemplate("netflix_domain", "netflix", RuleBehavior.Domain),
    ),
    OverridePresetItem.Spotify to listOf(
        RuleProviderTemplate("spotify_domain", "spotify", RuleBehavior.Domain),
    ),
    OverridePresetItem.OpenAI to listOf(
        RuleProviderTemplate("openai_domain", "openai", RuleBehavior.Domain),
    ),
    OverridePresetItem.Steam to listOf(
        RuleProviderTemplate("steam_domain", "steam", RuleBehavior.Domain),
    ),
    OverridePresetItem.Cn to listOf(
        RuleProviderTemplate("cn_domain", "cn", RuleBehavior.Domain),
        RuleProviderTemplate("cn_ip", "cn", RuleBehavior.IpCidr),
    ),
    OverridePresetItem.Proxy to listOf(
        RuleProviderTemplate("proxy_domain", "proxy", RuleBehavior.Domain),
    ),
    OverridePresetItem.GeolocationNotCn to listOf(
        RuleProviderTemplate("geolocation_not_cn_domain", "geolocation-!cn", RuleBehavior.Domain),
    ),
)

private val serviceRules = mapOf(
    OverridePresetItem.Google to listOf(
        ServiceRuleTemplate("google_domain"),
        ServiceRuleTemplate("google_ip", noResolve = true),
    ),
    OverridePresetItem.Telegram to listOf(
        ServiceRuleTemplate("telegram_domain"),
        ServiceRuleTemplate("telegram_ip", noResolve = true),
    ),
    OverridePresetItem.GitHub to listOf(
        ServiceRuleTemplate("github_domain"),
    ),
    OverridePresetItem.Microsoft to listOf(
        ServiceRuleTemplate("microsoft_domain"),
    ),
    OverridePresetItem.Apple to listOf(
        ServiceRuleTemplate("apple_domain"),
    ),
    OverridePresetItem.YouTube to listOf(
        ServiceRuleTemplate("youtube_domain"),
    ),
    OverridePresetItem.Netflix to listOf(
        ServiceRuleTemplate("netflix_domain"),
    ),
    OverridePresetItem.Spotify to listOf(
        ServiceRuleTemplate("spotify_domain"),
    ),
    OverridePresetItem.OpenAI to listOf(
        ServiceRuleTemplate("openai_domain"),
    ),
    OverridePresetItem.Steam to listOf(
        ServiceRuleTemplate("steam_domain"),
    ),
)

private val serviceGroupNames = mapOf(
    OverridePresetItem.Google to "Google",
    OverridePresetItem.Telegram to "Telegram",
    OverridePresetItem.GitHub to "GitHub",
    OverridePresetItem.Microsoft to "Microsoft",
    OverridePresetItem.Apple to "Apple",
    OverridePresetItem.YouTube to "YouTube",
    OverridePresetItem.Netflix to "Netflix",
    OverridePresetItem.Spotify to "Spotify",
    OverridePresetItem.OpenAI to "OpenAI",
    OverridePresetItem.Steam to "Steam",
)

private val templateProviderIds = itemProviders
    .values
    .flatten()
    .map(RuleProviderTemplate::id)
    .toSet()

fun defaultEnabledPresetItems(): Set<OverridePresetItem> = defaultEnabledItems.toSet()

fun defaultOverridePresetTemplateSelection(): OverridePresetTemplateSelection {
    return OverridePresetTemplateSelection(
        enabledItems = defaultEnabledPresetItems(),
    )
}

fun inferPresetTemplateSelection(
    config: ConfigurationOverride,
): OverridePresetTemplateSelection {
    val providerKeys = config.ruleProviders?.keys.orEmpty().toSet()
    val groupNames = config.proxyGroups
        ?.mapNotNull { group -> group["name"]?.jsonPrimitiveOrNull?.safeContentOrNull }
        ?.toSet()
        .orEmpty()
    val rules = config.rules.orEmpty()

    val hasTemplateSignals = providerKeys.any(templateProviderIds::contains) ||
        groupNames.any(serviceGroupNames.values.toSet()::contains) ||
        groupNames.any(OverridePresetRegion.entries.map(OverridePresetRegion::groupName).toSet()::contains) ||
        rules.any(::isTemplateRule)

    if (!hasTemplateSignals) {
        return defaultOverridePresetTemplateSelection()
    }

    val inferredRegions = OverridePresetRegion.entries
        .filter { region -> region.groupName in groupNames }
        .toSet()

    val inferredItems = itemOrder.filterTo(linkedSetOf()) { item ->
        isItemEnabledInConfig(
            item = item,
            providerKeys = providerKeys,
            groupNames = groupNames,
            rules = rules,
        )
    }

    return OverridePresetTemplateSelection(
        regions = inferredRegions,
        enabledItems = inferredItems.ifEmpty { defaultEnabledPresetItems() },
    )
}

fun applyPresetTemplateToConfig(
    base: ConfigurationOverride,
    selection: OverridePresetTemplateSelection,
): ConfigurationOverride {
    return when (selection.templateId) {
        OverridePresetTemplateId.OfficialMrsCommon -> {
            val selectedRegions = selection.regions.toList().sortedBy(OverridePresetRegion::ordinal)
            val enabledItems = normalizeEnabledItems(selection.enabledItems)
            base.copy(
                ruleProviders = buildRuleProviders(enabledItems),
                ruleProvidersMerge = null,
                proxyGroups = buildProxyGroups(
                    selectedRegions = selectedRegions,
                    enabledItems = enabledItems,
                ),
                proxyGroupsStart = null,
                proxyGroupsEnd = null,
                rules = buildRules(enabledItems),
                rulesStart = null,
                rulesEnd = null,
            )
        }
    }
}

private fun normalizeEnabledItems(
    enabledItems: Set<OverridePresetItem>,
): Set<OverridePresetItem> {
    return if (enabledItems.isEmpty()) {
        linkedSetOf(OverridePresetItem.Match)
    } else {
        enabledItems.toCollection(linkedSetOf())
    }
}

private fun buildRuleProviders(
    enabledItems: Set<OverridePresetItem>,
): Map<String, Map<String, JsonElement>>? {
    val orderedProviders = itemOrder
        .filter(enabledItems::contains)
        .filterNot { item -> item == OverridePresetItem.Match }
        .flatMap { item -> itemProviders[item].orEmpty() }

    return linkedMapOf<String, Map<String, JsonElement>>().apply {
        orderedProviders.forEach { template ->
            put(
                template.id,
                linkedMapOf(
                    "type" to JsonPrimitive("http"),
                    "format" to JsonPrimitive("mrs"),
                    "behavior" to JsonPrimitive(template.behavior.wireName),
                    "path" to JsonPrimitive("./ruleset/${template.id}.mrs"),
                    "url" to JsonPrimitive(template.urlTemplate.format(template.remoteName)),
                    "interval" to JsonPrimitive(RULE_PROVIDER_INTERVAL),
                ),
            )
        }
    }.takeIf { it.isNotEmpty() }
}

private fun buildProxyGroups(
    selectedRegions: List<OverridePresetRegion>,
    enabledItems: Set<OverridePresetItem>,
): List<Map<String, JsonElement>> {
    val regionNames = selectedRegions.map(OverridePresetRegion::groupName)
    val groups = mutableListOf<Map<String, JsonElement>>()

    groups += buildAutoGroup(name = "Auto")
    groups += selectedRegions.map(::buildRegionAutoGroup)
    groups += buildProxySelectGroup(regionNames)
    groups += serviceItems
        .filter(enabledItems::contains)
        .mapNotNull(serviceGroupNames::get)
        .map { serviceName ->
            buildServiceSelectGroup(
                name = serviceName,
                regionNames = regionNames,
            )
        }

    return groups
}

private fun buildRules(
    enabledItems: Set<OverridePresetItem>,
): List<String> {
    return buildList {
        if (OverridePresetItem.Ads in enabledItems) {
            add("RULE-SET,ads_domain,REJECT")
        }
        if (OverridePresetItem.Private in enabledItems) {
            add("RULE-SET,private_domain,DIRECT")
        }
        serviceItems
            .filter(enabledItems::contains)
            .forEach { item ->
                val targetGroup = serviceGroupNames.getValue(item)
                serviceRules[item].orEmpty().forEach { template ->
                    add(buildRuleSetRule(template.providerId, targetGroup, template.noResolve))
                }
            }
        if (OverridePresetItem.Cn in enabledItems) {
            add("RULE-SET,cn_domain,DIRECT")
            add("RULE-SET,cn_ip,DIRECT,no-resolve")
        }
        if (OverridePresetItem.Proxy in enabledItems) {
            add("RULE-SET,proxy_domain,Proxy")
        }
        if (OverridePresetItem.GeolocationNotCn in enabledItems) {
            add("RULE-SET,geolocation_not_cn_domain,Proxy")
        }
        if (OverridePresetItem.Match in enabledItems || isEmpty()) {
            add("MATCH,Proxy")
        }
    }
}

private fun buildAutoGroup(
    name: String,
    filter: String? = null,
): Map<String, JsonElement> {
    return linkedMapOf<String, JsonElement>().apply {
        put("name", JsonPrimitive(name))
        put("type", JsonPrimitive("url-test"))
        put("url", JsonPrimitive(URL_TEST_URL))
        put("interval", JsonPrimitive(URL_TEST_INTERVAL))
        put("include-all", JsonPrimitive(true))
        put("exclude-filter", JsonPrimitive(DEFAULT_EXCLUDE_FILTER))
        filter?.let { put("filter", JsonPrimitive(it)) }
    }
}

private fun buildRegionAutoGroup(
    region: OverridePresetRegion,
): Map<String, JsonElement> {
    return buildAutoGroup(
        name = region.groupName,
        filter = region.filter,
    )
}

private fun buildProxySelectGroup(
    regionNames: List<String>,
): Map<String, JsonElement> {
    return linkedMapOf(
        "name" to JsonPrimitive("Proxy"),
        "type" to JsonPrimitive("select"),
        "proxies" to jsonArrayOf(listOf("Auto") + regionNames),
        "include-all" to JsonPrimitive(true),
    )
}

private fun buildServiceSelectGroup(
    name: String,
    regionNames: List<String>,
): Map<String, JsonElement> {
    return linkedMapOf(
        "name" to JsonPrimitive(name),
        "type" to JsonPrimitive("select"),
        "proxies" to jsonArrayOf(
            listOf("Proxy", "DIRECT", "Auto")
                .plus(regionNames)
                .distinct(),
        ),
    )
}

private fun buildRuleSetRule(
    providerId: String,
    target: String,
    noResolve: Boolean,
): String {
    return if (noResolve) {
        "RULE-SET,$providerId,$target,no-resolve"
    } else {
        "RULE-SET,$providerId,$target"
    }
}

private fun isItemEnabledInConfig(
    item: OverridePresetItem,
    providerKeys: Set<String>,
    groupNames: Set<String>,
    rules: List<String>,
): Boolean {
    val itemProviderIds = itemProviders[item].orEmpty().map(RuleProviderTemplate::id)
    val serviceGroupName = serviceGroupNames[item]
    return when (item) {
        OverridePresetItem.Match -> rules.any { rule -> rule.trim() == "MATCH,Proxy" }
        else -> itemProviderIds.any(providerKeys::contains) ||
            rules.any { rule -> itemProviderIds.any(rule::contains) } ||
            (serviceGroupName != null && serviceGroupName in groupNames)
    }
}

private fun isTemplateRule(
    rule: String,
): Boolean {
    val normalizedRule = rule.trim()
    return normalizedRule == "MATCH,Proxy" ||
        templateProviderIds.any { providerId -> normalizedRule.contains(providerId) }
}

private val JsonElement.jsonPrimitiveOrNull: JsonPrimitive?
    get() = this as? JsonPrimitive

private val JsonPrimitive.safeContentOrNull: String?
    get() = safeContentOrNull(this)

private fun safeContentOrNull(
    primitive: JsonPrimitive,
): String? = runCatching { primitive.content }.getOrNull()

private fun jsonArrayOf(
    values: List<String>,
): JsonArray = JsonArray(values.map(::JsonPrimitive))
