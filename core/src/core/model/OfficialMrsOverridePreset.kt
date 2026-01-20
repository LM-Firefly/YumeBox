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

package com.github.yumelira.yumebox.core.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private const val OFFICIAL_MRS_RULE_PROVIDER_INTERVAL = 86400
private const val OFFICIAL_MRS_URL_TEST_INTERVAL = 300
private const val OFFICIAL_MRS_URL_TEST_URL = "https://www.gstatic.com/generate_204"
private const val OFFICIAL_MRS_EXCLUDE_FILTER =
    "(?i)GB|Traffic|Expire|Premium|频道|订阅|ISP|流量|到期|重置"
private const val OFFICIAL_MRS_POPULAR_REGION_EXCLUDE_FILTER =
    "(?i)(香港|HK|Hong Kong|🇭🇰|台湾|TW|Taiwan|🇹🇼|日本|JP|Japan|东京|大阪|🇯🇵|新加坡|SG|Singapore|狮城|🇸🇬|美国|US|United States|America|洛杉矶|硅谷|🇺🇸)"
private const val OFFICIAL_MRS_GEOSITE_URL =
    "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geosite/%s.mrs"
private const val OFFICIAL_MRS_GEOIP_URL =
    "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geoip/%s.mrs"
private const val OFFICIAL_MRS_AUTO_GROUP_NAME = "Auto"
private const val OFFICIAL_MRS_FALLBACK_GROUP_NAME = "Fallback"

data class OfficialMrsPresetSelection(
    val urlTestRegionIds: Set<String> = emptySet(),
    val fallbackRegionIds: Set<String> = emptySet(),
    val enabledItemIds: Set<String> = defaultOfficialMrsEnabledItemIds(),
    val enableUrlTestGroup: Boolean = true,
    val enableFallbackGroup: Boolean = false,
)

data class OfficialMrsRegionSpec(
    val id: String,
    val displayName: String,
    val groupName: String,
    val fallbackGroupName: String,
    val filter: String? = null,
    val excludeFilter: String? = null,
    val icon: String,
)

data class OfficialMrsItemSpec(
    val id: String,
    val title: String,
    val summary: String,
    val icon: String? = null,
    val isService: Boolean = false,
    val providers: List<OfficialMrsProviderSpec> = emptyList(),
    val groupName: String? = null,
    val rules: List<OfficialMrsRuleSpec> = emptyList(),
    val detectionRules: List<String> = emptyList(),
)

data class OfficialMrsProviderSpec(
    val id: String,
    val remoteName: String,
    val behavior: OfficialMrsRuleBehavior,
)

data class OfficialMrsRuleSpec(
    val providerId: String,
    val target: String,
    val noResolve: Boolean = false,
)

private enum class OfficialMrsHealthCheckGroupType(
    val wireName: String,
) {
    UrlTest("url-test"),
    Fallback("fallback"),
}

enum class OfficialMrsRuleBehavior(
    val wireName: String,
) {
    Domain("domain"),
    IpCidr("ipcidr"),
}

fun officialMrsPresetIconUrl(iconName: String?): String? {
    return officialMrsCatalogIconUrl(iconName)
}

private val officialMrsRegions = listOf(
    OfficialMrsRegionSpec(
        id = "hk",
        displayName = "香港自动组",
        groupName = "HK Auto",
        fallbackGroupName = "HK Fallback",
        filter = "(?i)(香港|HK|Hong Kong|🇭🇰)",
        icon = officialMrsCatalogIconUrl("HK").orEmpty(),
    ),
    OfficialMrsRegionSpec(
        id = "tw",
        displayName = "台湾自动组",
        groupName = "TW Auto",
        fallbackGroupName = "TW Fallback",
        filter = "(?i)(台湾|TW|Taiwan|🇹🇼)",
        icon = officialMrsCatalogIconUrl("TW").orEmpty(),
    ),
    OfficialMrsRegionSpec(
        id = "jp",
        displayName = "日本自动组",
        groupName = "JP Auto",
        fallbackGroupName = "JP Fallback",
        filter = "(?i)(日本|JP|Japan|东京|大阪|🇯🇵)",
        icon = officialMrsCatalogIconUrl("JP").orEmpty(),
    ),
    OfficialMrsRegionSpec(
        id = "sg",
        displayName = "新加坡自动组",
        groupName = "SG Auto",
        fallbackGroupName = "SG Fallback",
        filter = "(?i)(新加坡|SG|Singapore|狮城|🇸🇬)",
        icon = officialMrsCatalogIconUrl("SG").orEmpty(),
    ),
    OfficialMrsRegionSpec(
        id = "us",
        displayName = "美国自动组",
        groupName = "US Auto",
        fallbackGroupName = "US Fallback",
        filter = "(?i)(美国|US|United States|America|洛杉矶|硅谷|🇺🇸)",
        icon = officialMrsCatalogIconUrl("US").orEmpty(),
    ),
    OfficialMrsRegionSpec(
        id = "other",
        displayName = "冷门地区自动组",
        groupName = "Other Auto",
        fallbackGroupName = "Other Fallback",
        excludeFilter = OFFICIAL_MRS_POPULAR_REGION_EXCLUDE_FILTER,
        icon = officialMrsCatalogIconUrl("XD").orEmpty(),
    ),
)

private val officialMrsItems = listOf(
    OfficialMrsItemSpec(
        id = "proxy",
        title = "代理规则集",
        summary = "启用 proxy 规则集并走 Proxy。",
        icon = officialMrsCatalogIconUrl("Static").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("proxy_domain", "proxy", OfficialMrsRuleBehavior.Domain),
        ),
        detectionRules = listOf("RULE-SET,proxy_domain,Proxy"),
    ),
    OfficialMrsItemSpec(
        id = "ads",
        title = "广告拦截",
        summary = "启用 category-ads-all 并走 REJECT。",
        icon = officialMrsCatalogIconUrl("Adblock").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("ads_domain", "category-ads-all", OfficialMrsRuleBehavior.Domain),
        ),
        detectionRules = listOf("RULE-SET,ads_domain,REJECT"),
    ),
    officialMrsServiceItem(
        id = "google",
        title = "Google",
        icon = officialMrsCatalogIconUrl("Google").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("google_domain", "google", OfficialMrsRuleBehavior.Domain),
            OfficialMrsProviderSpec("google_ip", "google", OfficialMrsRuleBehavior.IpCidr),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("google_domain", "Google"),
            OfficialMrsRuleSpec("google_ip", "Google", noResolve = true),
        ),
    ),
    officialMrsServiceItem(
        id = "telegram",
        title = "Telegram",
        icon = officialMrsCatalogIconUrl("Telegram").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("telegram_domain", "telegram", OfficialMrsRuleBehavior.Domain),
            OfficialMrsProviderSpec("telegram_ip", "telegram", OfficialMrsRuleBehavior.IpCidr),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("telegram_domain", "Telegram"),
            OfficialMrsRuleSpec("telegram_ip", "Telegram", noResolve = true),
        ),
    ),
    officialMrsServiceItem(
        id = "whatsapp",
        title = "WhatsApp",
        groupName = "WhatsApp",
        icon = officialMrsAppIconUrl("WhatsApp.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("whatsapp_domain", "whatsapp", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("whatsapp_domain", "WhatsApp"),
        ),
    ),
    officialMrsServiceItem(
        id = "line",
        title = "LINE",
        groupName = "LINE",
        icon = officialMrsCatalogIconUrl("LineTV").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("line_domain", "line", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("line_domain", "LINE"),
        ),
    ),
    officialMrsServiceItem(
        id = "twitter",
        title = "Twitter / X",
        groupName = "Twitter",
        icon = officialMrsCatalogIconUrl("Twitter").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("twitter_domain", "twitter", OfficialMrsRuleBehavior.Domain),
            OfficialMrsProviderSpec("twitter_ip", "twitter", OfficialMrsRuleBehavior.IpCidr),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("twitter_domain", "Twitter"),
            OfficialMrsRuleSpec("twitter_ip", "Twitter", noResolve = true),
        ),
    ),
    officialMrsServiceItem(
        id = "tiktok",
        title = "TikTok",
        icon = officialMrsCatalogIconUrl("TikTok").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("tiktok_domain", "tiktok", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("tiktok_domain", "TikTok"),
        ),
    ),
    officialMrsServiceItem(
        id = "speedtest",
        title = "Speedtest",
        icon = officialMrsCatalogIconUrl("Speedtest").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("speedtest_domain", "speedtest", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("speedtest_domain", "Speedtest"),
        ),
    ),
    officialMrsServiceItem(
        id = "github",
        title = "GitHub",
        groupName = "GitHub",
        icon = officialMrsAppIconUrl("github_00.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("github_domain", "github", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("github_domain", "GitHub"),
        ),
    ),
    officialMrsServiceItem(
        id = "discord",
        title = "Discord",
        icon = officialMrsAppIconUrl("Discord.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("discord_domain", "discord", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("discord_domain", "Discord"),
        ),
    ),
    officialMrsServiceItem(
        id = "reddit",
        title = "Reddit",
        icon = officialMrsAppIconUrl("Category_Magazine.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("reddit_domain", "reddit", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("reddit_domain", "Reddit"),
        ),
    ),
    officialMrsServiceItem(
        id = "facebook",
        title = "Facebook",
        icon = officialMrsAppIconUrl("facebook.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("facebook_domain", "facebook", OfficialMrsRuleBehavior.Domain),
            OfficialMrsProviderSpec("facebook_ip", "facebook", OfficialMrsRuleBehavior.IpCidr),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("facebook_domain", "Facebook"),
            OfficialMrsRuleSpec("facebook_ip", "Facebook", noResolve = true),
        ),
    ),
    officialMrsServiceItem(
        id = "instagram",
        title = "Instagram",
        icon = officialMrsCatalogIconUrl("Instagram").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("instagram_domain", "instagram", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("instagram_domain", "Instagram"),
        ),
    ),
    officialMrsServiceItem(
        id = "threads",
        title = "Threads",
        icon = officialMrsAppIconUrl("Threads.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("threads_domain", "threads", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("threads_domain", "Threads"),
        ),
    ),
    officialMrsServiceItem(
        id = "microsoft",
        title = "Microsoft",
        icon = officialMrsCatalogIconUrl("Microsoft").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("microsoft_domain", "microsoft", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("microsoft_domain", "Microsoft"),
        ),
    ),
    officialMrsServiceItem(
        id = "bing",
        title = "Bing",
        icon = officialMrsAppIconUrl("bing.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("bing_domain", "bing", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("bing_domain", "Bing"),
        ),
    ),
    officialMrsServiceItem(
        id = "apple",
        title = "Apple",
        icon = officialMrsCatalogIconUrl("Apple").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("apple_domain", "apple", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("apple_domain", "Apple"),
        ),
    ),
    officialMrsServiceItem(
        id = "youtube",
        title = "YouTube",
        groupName = "YouTube",
        icon = officialMrsCatalogIconUrl("YouTube").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("youtube_domain", "youtube", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("youtube_domain", "YouTube"),
        ),
    ),
    officialMrsServiceItem(
        id = "netflix",
        title = "Netflix",
        icon = officialMrsCatalogIconUrl("Netflix").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("netflix_domain", "netflix", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("netflix_domain", "Netflix"),
        ),
    ),
    officialMrsServiceItem(
        id = "disney",
        title = "Disney",
        icon = officialMrsCatalogIconUrl("DisneyPlus").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("disney_domain", "disney", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("disney_domain", "Disney"),
        ),
    ),
    officialMrsServiceItem(
        id = "hbo",
        title = "HBO",
        groupName = "HBO",
        icon = officialMrsCatalogIconUrl("HBO").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("hbo_domain", "hbo", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("hbo_domain", "HBO"),
        ),
    ),
    officialMrsServiceItem(
        id = "primevideo",
        title = "Prime Video",
        groupName = "PrimeVideo",
        icon = officialMrsCatalogIconUrl("PrimeVideo").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("primevideo_domain", "primevideo", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("primevideo_domain", "PrimeVideo"),
        ),
    ),
    officialMrsServiceItem(
        id = "tvb",
        title = "TVB",
        groupName = "TVB",
        icon = officialMrsAppIconUrl("TVBAnywhere+.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("tvb_domain", "tvb", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("tvb_domain", "TVB"),
        ),
    ),
    officialMrsServiceItem(
        id = "mytvsuper",
        title = "MyTVSuper",
        groupName = "MyTVSuper",
        icon = officialMrsAppIconUrl("TVBAnywhere+.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("mytvsuper_domain", "mytvsuper", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("mytvsuper_domain", "MyTVSuper"),
        ),
    ),
    officialMrsServiceItem(
        id = "dazn",
        title = "DAZN",
        groupName = "DAZN",
        icon = officialMrsCatalogIconUrl("Streaming").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("dazn_domain", "dazn", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("dazn_domain", "DAZN"),
        ),
    ),
    officialMrsServiceItem(
        id = "spotify",
        title = "Spotify",
        icon = officialMrsCatalogIconUrl("Spotify").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("spotify_domain", "spotify", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("spotify_domain", "Spotify"),
        ),
    ),
    officialMrsServiceItem(
        id = "amazon",
        title = "Amazon",
        icon = officialMrsAppIconUrl("Amazon.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("amazon_domain", "amazon", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("amazon_domain", "Amazon"),
        ),
    ),
    officialMrsServiceItem(
        id = "paypal",
        title = "PayPal",
        groupName = "PayPal",
        icon = officialMrsCatalogIconUrl("Paypal").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("paypal_domain", "paypal", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("paypal_domain", "PayPal"),
        ),
    ),
    officialMrsServiceItem(
        id = "cloudflare",
        title = "Cloudflare",
        icon = officialMrsAppIconUrl("Cloudflare.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("cloudflare_domain", "cloudflare", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("cloudflare_domain", "Cloudflare"),
        ),
    ),
    officialMrsServiceItem(
        id = "zoom",
        title = "Zoom",
        icon = officialMrsAppIconUrl("Category_Networking.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("zoom_domain", "zoom", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("zoom_domain", "Zoom"),
        ),
    ),
    officialMrsServiceItem(
        id = "wikimedia",
        title = "Wikimedia",
        icon = officialMrsAppIconUrl("Category_Research.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("wikimedia_domain", "wikimedia", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("wikimedia_domain", "Wikimedia"),
        ),
    ),
    officialMrsServiceItem(
        id = "bilibili",
        title = "Bilibili",
        icon = officialMrsCatalogIconUrl("Bili").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("bilibili_domain", "bilibili", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("bilibili_domain", "Bilibili"),
        ),
    ),
    officialMrsServiceItem(
        id = "biliintl",
        title = "BiliIntl",
        icon = officialMrsCatalogIconUrl("Bili").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("biliintl_domain", "biliintl", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("biliintl_domain", "BiliIntl"),
        ),
    ),
    officialMrsServiceItem(
        id = "bahamut",
        title = "Bahamut",
        icon = officialMrsCatalogIconUrl("Bahamut").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("bahamut_domain", "bahamut", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("bahamut_domain", "Bahamut"),
        ),
    ),
    officialMrsServiceItem(
        id = "dmm",
        title = "DMM",
        groupName = "DMM",
        icon = officialMrsAppIconUrl("AnimeHome.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("dmm_domain", "dmm", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("dmm_domain", "DMM"),
        ),
    ),
    officialMrsServiceItem(
        id = "abema",
        title = "Abema",
        icon = officialMrsAppIconUrl("AnimeHome.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("abema_domain", "abema", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("abema_domain", "Abema"),
        ),
    ),
    officialMrsServiceItem(
        id = "ehentai",
        title = "EHentai",
        groupName = "EHentai",
        icon = officialMrsAppIconUrl("HentaiHome.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("ehentai_domain", "ehentai", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("ehentai_domain", "EHentai"),
        ),
    ),
    officialMrsServiceItem(
        id = "openai",
        title = "OpenAI",
        groupName = "OpenAI",
        icon = officialMrsAppIconUrl("ChatGPT.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("openai_domain", "openai", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("openai_domain", "OpenAI"),
        ),
    ),
    officialMrsServiceItem(
        id = "anthropic",
        title = "Claude / Anthropic",
        groupName = "Claude",
        icon = officialMrsAppIconUrl("Claude_01.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("anthropic_domain", "anthropic", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("anthropic_domain", "Claude"),
        ),
    ),
    officialMrsServiceItem(
        id = "onedrive",
        title = "OneDrive",
        groupName = "OneDrive",
        icon = officialMrsAppIconUrl("OneDrive.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("onedrive_domain", "onedrive", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("onedrive_domain", "OneDrive"),
        ),
    ),
    officialMrsServiceItem(
        id = "pixiv",
        title = "Pixiv",
        icon = officialMrsAppIconUrl("Category_Photo.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("pixiv_domain", "pixiv", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("pixiv_domain", "Pixiv"),
        ),
    ),
    officialMrsServiceItem(
        id = "niconico",
        title = "Niconico",
        groupName = "Niconico",
        icon = officialMrsAppIconUrl("AnimeHome.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("niconico_domain", "niconico", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("niconico_domain", "Niconico"),
        ),
    ),
    officialMrsServiceItem(
        id = "steam",
        title = "Steam",
        icon = officialMrsAppIconUrl("steam.png").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("steam_domain", "steam", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("steam_domain", "Steam"),
        ),
    ),
    OfficialMrsItemSpec(
        id = "cn",
        title = "中国大陆直连",
        summary = "启用 cn 域名和 IP 规则并走 DIRECT。",
        icon = officialMrsCatalogIconUrl("China").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec("cn_domain", "cn", OfficialMrsRuleBehavior.Domain),
            OfficialMrsProviderSpec("cn_ip", "cn", OfficialMrsRuleBehavior.IpCidr),
        ),
        detectionRules = listOf(
            "RULE-SET,cn_domain,DIRECT",
            "RULE-SET,cn_ip,DIRECT,no-resolve",
        ),
    ),
    OfficialMrsItemSpec(
        id = "geolocation_not_cn",
        title = "境外地理规则",
        summary = "启用 geolocation-!cn 并走 Proxy。",
        icon = officialMrsCatalogIconUrl("Global").orEmpty(),
        providers = listOf(
            OfficialMrsProviderSpec(
                "geolocation_not_cn_domain",
                "geolocation-!cn",
                OfficialMrsRuleBehavior.Domain,
            ),
        ),
        detectionRules = listOf("RULE-SET,geolocation_not_cn_domain,Proxy"),
    ),
    OfficialMrsItemSpec(
        id = "match",
        title = "兜底 MATCH",
        summary = "末尾追加 MATCH,Proxy。",
        icon = officialMrsCatalogIconUrl("Final").orEmpty(),
        detectionRules = listOf("MATCH,Proxy"),
    ),
)

private fun officialMrsServiceItem(
    id: String,
    title: String,
    icon: String,
    providers: List<OfficialMrsProviderSpec>,
    rules: List<OfficialMrsRuleSpec>,
    groupName: String = title,
): OfficialMrsItemSpec {
    return OfficialMrsItemSpec(
        id = id,
        title = title,
        summary = "启用 $title 分流和专属策略组。",
        icon = icon,
        isService = true,
        providers = providers,
        groupName = groupName,
        rules = rules,
        detectionRules = rules.map(::buildOfficialMrsRuleSetRule),
    )
}

private val officialMrsItemsById = officialMrsItems.associateBy(OfficialMrsItemSpec::id)
private val officialMrsRegionsById = officialMrsRegions.associateBy(OfficialMrsRegionSpec::id)
private val officialMrsServiceItems = officialMrsItems.filter(OfficialMrsItemSpec::isService)
private val officialMrsBaseItems = officialMrsItems.filterNot(OfficialMrsItemSpec::isService)
private val officialMrsTemplateProviderIds = officialMrsItems
    .flatMap(OfficialMrsItemSpec::providers)
    .map(OfficialMrsProviderSpec::id)
    .toSet()
private val officialMrsServiceGroupNames = officialMrsServiceItems
    .mapNotNull(OfficialMrsItemSpec::groupName)
    .toSet()
private val officialMrsRegionGroupNames = officialMrsRegions
    .flatMap { region -> listOf(region.groupName, region.fallbackGroupName) }
    .toSet()
private val officialMrsRuleOrder = buildList {
    add("ads")
    addAll(officialMrsServiceItems.map(OfficialMrsItemSpec::id))
    add("cn")
    add("proxy")
    add("geolocation_not_cn")
    add("match")
}
private val officialMrsDefaultEnabledItemIds = linkedSetOf(
    "proxy",
    "ads",
    "google",
    "telegram",
    "github",
    "microsoft",
    "bing",
    "apple",
    "youtube",
    "netflix",
    "spotify",
    "openai",
    "anthropic",
    "steam",
    "cn",
    "geolocation_not_cn",
    "match",
)
private val officialMrsDefaultSystemRegionIds = linkedSetOf(
    "hk",
    "sg",
    "jp",
    "us",
)
private val officialMrsTemplateRules = officialMrsItems
    .flatMap(OfficialMrsItemSpec::detectionRules)
    .toSet()

fun defaultOfficialMrsEnabledItemIds(): Set<String> = officialMrsDefaultEnabledItemIds.toSet()

fun defaultOfficialMrsPresetSelection(): OfficialMrsPresetSelection {
    return OfficialMrsPresetSelection(
        enabledItemIds = defaultOfficialMrsEnabledItemIds(),
        enableUrlTestGroup = true,
        enableFallbackGroup = false,
    )
}

fun defaultSystemOfficialMrsPresetSelection(): OfficialMrsPresetSelection {
    return OfficialMrsPresetSelection(
        urlTestRegionIds = officialMrsDefaultSystemRegionIds,
        enabledItemIds = defaultOfficialMrsEnabledItemIds(),
        enableUrlTestGroup = true,
        enableFallbackGroup = false,
    )
}

fun orderedOfficialMrsBaseItems(): List<OfficialMrsItemSpec> = officialMrsBaseItems.toList()

fun orderedOfficialMrsServiceItems(): List<OfficialMrsItemSpec> = officialMrsServiceItems.toList()

fun orderedOfficialMrsItems(): List<OfficialMrsItemSpec> = officialMrsItems.toList()

fun orderedOfficialMrsRegions(): List<OfficialMrsRegionSpec> = officialMrsRegions.toList()

fun sortOfficialMrsItemIds(itemIds: Collection<String>): List<String> {
    return officialMrsItems.map(OfficialMrsItemSpec::id).filter(itemIds::contains)
}

fun buildOfficialMrsConfigurationOverride(
    selection: OfficialMrsPresetSelection = defaultOfficialMrsPresetSelection(),
): ConfigurationOverride {
    // Simplified: Returns empty ConfigurationOverride as proxy groups and rules are no longer supported
    return ConfigurationOverride()
}

fun inferOfficialMrsPresetSelection(config: ConfigurationOverride): OfficialMrsPresetSelection {
    // Simplified: Always return default selection as proxy groups and rules are no longer supported
    return defaultOfficialMrsPresetSelection()
}

fun officialMrsItemById(id: String): OfficialMrsItemSpec? = officialMrsItemsById[id]

fun officialMrsRegionById(id: String): OfficialMrsRegionSpec? = officialMrsRegionsById[id]

private fun normalizeOfficialMrsEnabledItemIds(enabledItemIds: Set<String>): Set<String> {
    return if (enabledItemIds.isEmpty()) {
        linkedSetOf("match")
    } else {
        enabledItemIds
            .filter(officialMrsItemsById::containsKey)
            .toCollection(linkedSetOf())
            .ifEmpty { linkedSetOf("match") }
    }
}

private fun buildOfficialMrsRuleProviders(
    enabledItemIds: Set<String>,
): Map<String, Map<String, JsonElement>>? {
    val orderedProviders = officialMrsItems
        .filter { spec -> spec.id in enabledItemIds && spec.id != "match" }
        .flatMap(OfficialMrsItemSpec::providers)

    return linkedMapOf<String, Map<String, JsonElement>>().apply {
        orderedProviders.forEach { provider ->
                put(
                    provider.id,
                    linkedMapOf(
                        "type" to JsonPrimitive("http"),
                        "format" to JsonPrimitive("mrs"),
                        "behavior" to JsonPrimitive(provider.behavior.wireName),
                        "url" to JsonPrimitive(provider.urlTemplate().format(provider.remoteName)),
                        "path" to JsonPrimitive(buildOfficialMrsRuleProviderPath(provider.id)),
                        "interval" to JsonPrimitive(OFFICIAL_MRS_RULE_PROVIDER_INTERVAL),
                    ),
                )
        }
    }.takeIf { it.isNotEmpty() }
}

private fun buildOfficialMrsRuleProviderPath(providerId: String): String {
    return "./providers/rules/$providerId.mrs"
}

private fun buildOfficialMrsProxyGroups(
    selectedUrlTestRegions: List<OfficialMrsRegionSpec>,
    selectedFallbackRegions: List<OfficialMrsRegionSpec>,
    enabledItemIds: Set<String>,
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): List<Map<String, JsonElement>> {
    val regionNames = buildList {
        if (enableUrlTestGroup) {
            addAll(selectedUrlTestRegions.map(OfficialMrsRegionSpec::groupName))
        }
        if (enableFallbackGroup) {
            addAll(selectedFallbackRegions.map(OfficialMrsRegionSpec::fallbackGroupName))
        }
    }
    val groups = mutableListOf<Map<String, JsonElement>>()

    groups += buildOfficialMrsProxySelectGroup(
        regionNames = regionNames,
        enableUrlTestGroup = enableUrlTestGroup,
        enableFallbackGroup = enableFallbackGroup,
    )
    if (enableUrlTestGroup) {
        groups += buildOfficialMrsAutoGroup(name = OFFICIAL_MRS_AUTO_GROUP_NAME)
    }
    if (enableFallbackGroup) {
        groups += buildOfficialMrsFallbackGroup(name = OFFICIAL_MRS_FALLBACK_GROUP_NAME)
    }
    groups += officialMrsServiceItems
        .filter { it.id in enabledItemIds }
        .map {
            spec -> buildOfficialMrsServiceSelectGroup(
                spec = spec,
                regionNames = regionNames,
                enableUrlTestGroup = enableUrlTestGroup,
                enableFallbackGroup = enableFallbackGroup,
            )
        }
    groups += buildList {
        if (enableUrlTestGroup) {
            addAll(selectedUrlTestRegions.map(::buildOfficialMrsRegionAutoGroup))
        }
        if (enableFallbackGroup) {
            addAll(selectedFallbackRegions.map(::buildOfficialMrsRegionFallbackGroup))
        }
    }

    return groups
}

private fun buildOfficialMrsRules(
    enabledItemIds: Set<String>,
): List<String> {
    return buildList {
        officialMrsRuleOrder
            .mapNotNull(officialMrsItemsById::get)
            .filter { it.id in enabledItemIds }
            .forEach { spec ->
                addAll(spec.detectionRules)
            }
        if ("match" in enabledItemIds || isEmpty()) {
            if ("MATCH,Proxy" !in this) {
                add("MATCH,Proxy")
            }
        }
    }
}

private fun buildOfficialMrsAutoGroup(
    name: String,
    filter: String? = null,
    excludeFilter: String? = null,
    icon: String = officialMrsCatalogIconUrl("Urltest").orEmpty(),
): Map<String, JsonElement> {
    return buildOfficialMrsHealthCheckGroup(
        name = name,
        type = OfficialMrsHealthCheckGroupType.UrlTest,
        filter = filter,
        excludeFilter = excludeFilter,
        icon = icon,
    )
}

private fun buildOfficialMrsFallbackGroup(
    name: String,
    filter: String? = null,
    excludeFilter: String? = null,
    icon: String = officialMrsCatalogIconUrl("Available").orEmpty(),
): Map<String, JsonElement> {
    return buildOfficialMrsHealthCheckGroup(
        name = name,
        type = OfficialMrsHealthCheckGroupType.Fallback,
        filter = filter,
        excludeFilter = excludeFilter,
        icon = icon,
    )
}

private fun buildOfficialMrsHealthCheckGroup(
    name: String,
    type: OfficialMrsHealthCheckGroupType,
    filter: String? = null,
    excludeFilter: String? = null,
    icon: String = officialMrsCatalogIconUrl("Urltest").orEmpty(),
): Map<String, JsonElement> {
    return linkedMapOf<String, JsonElement>().apply {
        put("name", JsonPrimitive(name))
        put("type", JsonPrimitive(type.wireName))
        put("icon", JsonPrimitive(icon))
        put("url", JsonPrimitive(OFFICIAL_MRS_URL_TEST_URL))
        put("interval", JsonPrimitive(OFFICIAL_MRS_URL_TEST_INTERVAL))
        put("include-all", JsonPrimitive(true))
        put("exclude-filter", JsonPrimitive(combineOfficialMrsExcludeFilters(excludeFilter)))
        filter?.let { put("filter", JsonPrimitive(it)) }
    }
}

private fun buildOfficialMrsRegionAutoGroup(
    region: OfficialMrsRegionSpec,
): Map<String, JsonElement> {
    return buildOfficialMrsAutoGroup(
        name = region.groupName,
        filter = region.filter,
        excludeFilter = region.excludeFilter,
        icon = region.icon,
    )
}

private fun buildOfficialMrsRegionFallbackGroup(
    region: OfficialMrsRegionSpec,
): Map<String, JsonElement> {
    return buildOfficialMrsFallbackGroup(
        name = region.fallbackGroupName,
        filter = region.filter,
        excludeFilter = region.excludeFilter,
        icon = region.icon,
    )
}

private fun combineOfficialMrsExcludeFilters(extraExcludeFilter: String?): String {
    return listOf(OFFICIAL_MRS_EXCLUDE_FILTER, extraExcludeFilter)
        .filterNotNull()
        .joinToString("|")
}

private fun buildOfficialMrsProxySelectGroup(
    regionNames: List<String>,
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): Map<String, JsonElement> {
    return linkedMapOf(
        "name" to JsonPrimitive("Proxy"),
        "type" to JsonPrimitive("select"),
        "icon" to JsonPrimitive(officialMrsItemById("proxy")?.icon.orEmpty()),
        "proxies" to jsonArrayOf(
            buildOfficialMrsSelectableGroupNames(
                regionNames = regionNames,
                enableUrlTestGroup = enableUrlTestGroup,
                enableFallbackGroup = enableFallbackGroup,
            ),
        ),
        "include-all" to JsonPrimitive(true),
    )
}

private fun buildOfficialMrsServiceSelectGroup(
    spec: OfficialMrsItemSpec,
    regionNames: List<String>,
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): Map<String, JsonElement> {
    val groupName = checkNotNull(spec.groupName)
    return linkedMapOf<String, JsonElement>().apply {
        put("name", JsonPrimitive(groupName))
        put("type", JsonPrimitive("select"))
        spec.icon?.takeIf(String::isNotBlank)?.let { put("icon", JsonPrimitive(it)) }
        put(
            "proxies",
            jsonArrayOf(
                buildList {
                    add("Proxy")
                    add("DIRECT")
                    if (enableUrlTestGroup) {
                        add(OFFICIAL_MRS_AUTO_GROUP_NAME)
                    }
                    if (enableFallbackGroup) {
                        add(OFFICIAL_MRS_FALLBACK_GROUP_NAME)
                    }
                    addAll(regionNames)
                }.distinct(),
            ),
        )
    }
}

private fun buildOfficialMrsSelectableGroupNames(
    regionNames: List<String>,
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): List<String> {
    return buildList {
        if (enableUrlTestGroup) {
            add(OFFICIAL_MRS_AUTO_GROUP_NAME)
        }
        if (enableFallbackGroup) {
            add(OFFICIAL_MRS_FALLBACK_GROUP_NAME)
        }
        addAll(regionNames)
    }.distinct()
}

private fun OfficialMrsProviderSpec.urlTemplate(): String {
    return when (behavior) {
        OfficialMrsRuleBehavior.Domain -> OFFICIAL_MRS_GEOSITE_URL
        OfficialMrsRuleBehavior.IpCidr -> OFFICIAL_MRS_GEOIP_URL
    }
}

private fun buildOfficialMrsRuleSetRule(rule: OfficialMrsRuleSpec): String {
    return if (rule.noResolve) {
        "RULE-SET,${rule.providerId},${rule.target},no-resolve"
    } else {
        "RULE-SET,${rule.providerId},${rule.target}"
    }
}

private fun isOfficialMrsItemEnabledInConfig(
    spec: OfficialMrsItemSpec,
    providerKeys: Set<String>,
    groupNames: Set<String>,
    rules: List<String>,
): Boolean {
    val providerIds = spec.providers.map(OfficialMrsProviderSpec::id)
    return when (spec.id) {
        "match" -> rules.any { rule -> rule.trim() == "MATCH,Proxy" }
        else -> providerIds.any(providerKeys::contains) ||
            spec.detectionRules.any(rules::contains) ||
            (spec.groupName != null && spec.groupName in groupNames)
    }
}

private fun isOfficialMrsTemplateRule(rule: String): Boolean {
    val normalizedRule = rule.trim()
    return normalizedRule in officialMrsTemplateRules ||
        officialMrsTemplateProviderIds.any { providerId -> normalizedRule.contains(providerId) }
}

private val JsonElement.jsonPrimitiveOrNull: JsonPrimitive?
    get() = this as? JsonPrimitive

private val JsonPrimitive.safeContentOrNull: String?
    get() = runCatching { content }.getOrNull()

private fun jsonArrayOf(values: List<String>): JsonArray = JsonArray(values.map(::JsonPrimitive))
