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
