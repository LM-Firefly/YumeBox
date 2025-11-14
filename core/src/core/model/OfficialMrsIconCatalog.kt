package com.github.yumelira.yumebox.core.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val OFFICIAL_MRS_ICON_JSON_APP_BASE_URL =
    "https://raw.githubusercontent.com/fmz200/wool_scripts/main/icons/apps"
private const val OFFICIAL_MRS_CATALOG_BASE_URL =
    "https://raw.githubusercontent.com/Orz-3/mini/master/Color"

internal fun officialMrsCatalogIconUrl(iconName: String?): String? {
    val normalizedIconName = iconName?.trim()?.takeIf(String::isNotBlank) ?: return null
    return "$OFFICIAL_MRS_CATALOG_BASE_URL/${encodePathSegment(normalizedIconName)}.png"
}

internal fun officialMrsAppIconUrl(iconName: String?): String? {
    val normalizedIconName = iconName?.trim()?.takeIf(String::isNotBlank) ?: return null
    return "$OFFICIAL_MRS_ICON_JSON_APP_BASE_URL/${encodePathSegment(normalizedIconName)}"
}

private fun encodePathSegment(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")
}
