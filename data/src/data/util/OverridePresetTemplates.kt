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
import com.github.yumelira.yumebox.core.model.OfficialMrsPresetSelection
import com.github.yumelira.yumebox.core.model.buildOfficialMrsConfigurationOverride
import com.github.yumelira.yumebox.core.model.defaultOfficialMrsEnabledItemIds
import com.github.yumelira.yumebox.core.model.inferOfficialMrsPresetSelection
import com.github.yumelira.yumebox.core.model.officialMrsItemById
import com.github.yumelira.yumebox.core.model.officialMrsRegionById
import com.github.yumelira.yumebox.core.model.orderedOfficialMrsBaseItems
import com.github.yumelira.yumebox.core.model.orderedOfficialMrsItems
import com.github.yumelira.yumebox.core.model.orderedOfficialMrsServiceItems
import com.github.yumelira.yumebox.core.model.orderedOfficialMrsRegions
import com.github.yumelira.yumebox.core.model.sortOfficialMrsItemIds

const val OFFICIAL_MRS_PRESET_TITLE = "官方 MRS 常用分流"
const val OFFICIAL_MRS_PRESET_SUMMARY =
    "使用 Mihomo 官方 meta/geo mrs 规则集，按开关重建当前覆写里的规则提供者、策略组和规则。"

enum class OverridePresetRegion(
    val specId: String,
) {
    HK("hk"),
    TW("tw"),
    JP("jp"),
    SG("sg"),
    US("us"),
    Other("other"),

    ;

    private val spec by lazy(LazyThreadSafetyMode.NONE) { requireRegionSpec(specId) }

    val displayName: String
        get() = spec.displayName

    val groupName: String
        get() = spec.groupName

    val fallbackGroupName: String
        get() = spec.fallbackGroupName

    val filter: String?
        get() = spec.filter

    val icon: String
        get() = spec.icon
}

enum class OverridePresetItem(
    val id: String,
) {
    Ads("ads"),
    Google("google"),
    Telegram("telegram"),
    WhatsApp("whatsapp"),
    Line("line"),
    Twitter("twitter"),
    TikTok("tiktok"),
    Speedtest("speedtest"),
    GitHub("github"),
    Discord("discord"),
    Reddit("reddit"),
    Facebook("facebook"),
    Instagram("instagram"),
    Threads("threads"),
    Microsoft("microsoft"),
    Bing("bing"),
    Apple("apple"),
    YouTube("youtube"),
    Netflix("netflix"),
    Disney("disney"),
    Hbo("hbo"),
    PrimeVideo("primevideo"),
    Tvb("tvb"),
    MyTvSuper("mytvsuper"),
    Dazn("dazn"),
    Spotify("spotify"),
    Amazon("amazon"),
    PayPal("paypal"),
    Cloudflare("cloudflare"),
    Zoom("zoom"),
    Wikimedia("wikimedia"),
    Bilibili("bilibili"),
    BiliIntl("biliintl"),
    Bahamut("bahamut"),
    Dmm("dmm"),
    Abema("abema"),
    OpenAI("openai"),
    Anthropic("anthropic"),
    OneDrive("onedrive"),
    Pixiv("pixiv"),
    Niconico("niconico"),
    Steam("steam"),
    Ehentai("ehentai"),
    Cn("cn"),
    Proxy("proxy"),
    GeolocationNotCn("geolocation_not_cn"),
    Match("match"),

    ;

    private val spec by lazy(LazyThreadSafetyMode.NONE) { requireItemSpec(id) }

    val title: String
        get() = spec.title

    val summary: String
        get() = spec.summary

    val icon: String?
        get() = spec.icon

    val isService: Boolean
        get() = spec.isService
}

data class OverridePresetTemplateSelection(
    val urlTestRegions: Set<OverridePresetRegion> = emptySet(),
    val fallbackRegions: Set<OverridePresetRegion> = emptySet(),
    val enabledItems: Set<OverridePresetItem> = defaultEnabledPresetItems(),
    val enableUrlTestGroup: Boolean = true,
    val enableFallbackGroup: Boolean = false,
)

private val itemById = OverridePresetItem.entries.associateBy(OverridePresetItem::id)
private val regionById = OverridePresetRegion.entries.associateBy(OverridePresetRegion::specId)
private val officialMrsCatalogCoverage = run {
    check(orderedOfficialMrsRegions().map { it.id }.toSet() == OverridePresetRegion.entries.map(OverridePresetRegion::specId).toSet()) {
        "OverridePresetRegion 与 core OfficialMrsRegionSpec 覆盖不一致"
    }
    check(orderedOfficialMrsItems().map { it.id }.toSet() == OverridePresetItem.entries.map(OverridePresetItem::id).toSet()) {
        "OverridePresetItem 与 core OfficialMrsItemSpec 覆盖不一致"
    }
}

fun defaultEnabledPresetItems(): Set<OverridePresetItem> {
    officialMrsCatalogCoverage
    return defaultOfficialMrsEnabledItemIds()
        .mapNotNull(itemById::get)
        .toCollection(linkedSetOf())
}

fun orderedPresetRegions(): List<OverridePresetRegion> {
    officialMrsCatalogCoverage
    return orderedOfficialMrsRegions().mapNotNull { spec -> regionById[spec.id] }
}

fun orderedBasePresetItems(): List<OverridePresetItem> {
    officialMrsCatalogCoverage
    return orderedOfficialMrsBaseItems().mapNotNull { spec -> itemById[spec.id] }
}

fun orderedServicePresetItems(): List<OverridePresetItem> {
    officialMrsCatalogCoverage
    return orderedOfficialMrsServiceItems().mapNotNull { spec -> itemById[spec.id] }
}

fun sortPresetRegions(
    regions: Collection<OverridePresetRegion>,
): List<OverridePresetRegion> {
    val regionIds = regions.map(OverridePresetRegion::specId).toSet()
    return orderedPresetRegions().filter { it.specId in regionIds }
}

fun sortPresetItems(
    items: Collection<OverridePresetItem>,
): List<OverridePresetItem> {
    officialMrsCatalogCoverage
    return sortOfficialMrsItemIds(items.map(OverridePresetItem::id))
        .mapNotNull(itemById::get)
}

fun defaultOverridePresetTemplateSelection(): OverridePresetTemplateSelection =
    OverridePresetTemplateSelection(
        enabledItems = defaultEnabledPresetItems(),
        enableUrlTestGroup = true,
        enableFallbackGroup = false,
    )

fun inferPresetTemplateSelection(
    config: ConfigurationOverride,
): OverridePresetTemplateSelection {
    val inferredSelection = inferOfficialMrsPresetSelection(config)
    return OverridePresetTemplateSelection(
        urlTestRegions = inferredSelection.urlTestRegionIds.mapNotNull(regionById::get).toSet(),
        fallbackRegions = inferredSelection.fallbackRegionIds.mapNotNull(regionById::get).toSet(),
        enabledItems = inferredSelection.enabledItemIds.mapNotNull(itemById::get).toSet(),
        enableUrlTestGroup = inferredSelection.enableUrlTestGroup,
        enableFallbackGroup = inferredSelection.enableFallbackGroup,
    )
}

fun applyPresetTemplateToConfig(
    base: ConfigurationOverride,
    selection: OverridePresetTemplateSelection,
): ConfigurationOverride {
    val generated = buildOfficialMrsConfigurationOverride(
        OfficialMrsPresetSelection(
            urlTestRegionIds = selection.urlTestRegions
                .mapTo(linkedSetOf(), OverridePresetRegion::specId),
            fallbackRegionIds = selection.fallbackRegions
                .mapTo(linkedSetOf(), OverridePresetRegion::specId),
            enabledItemIds = selection.enabledItems.mapTo(linkedSetOf(), OverridePresetItem::id),
            enableUrlTestGroup = selection.enableUrlTestGroup,
            enableFallbackGroup = selection.enableFallbackGroup,
        ),
    )
    return base.copy()
}

private fun requireRegionSpec(id: String) = checkNotNull(officialMrsRegionById(id))

private fun requireItemSpec(id: String) = checkNotNull(officialMrsItemById(id))
