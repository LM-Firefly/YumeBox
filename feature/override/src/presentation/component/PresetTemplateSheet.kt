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



package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.officialMrsPresetIconUrl
import com.github.yumelira.yumebox.data.util.OverridePresetItem
import com.github.yumelira.yumebox.data.util.OverridePresetRegion
import com.github.yumelira.yumebox.data.util.OverridePresetTemplateSelection
import com.github.yumelira.yumebox.data.util.orderedBasePresetItems
import com.github.yumelira.yumebox.data.util.orderedPresetRegions
import com.github.yumelira.yumebox.data.util.orderedServicePresetItems
import com.github.yumelira.yumebox.data.util.sortPresetItems
import com.github.yumelira.yumebox.data.util.sortPresetRegions
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OverridePresetTemplateSheet(
    show: Boolean,
    initialSelection: OverridePresetTemplateSelection = OverridePresetTemplateSelection(),
    onDismiss: () -> Unit,
    onConfirm: (OverridePresetTemplateSelection) -> Unit,
) {
    val selectedUrlTestRegions = remember(show) { mutableStateListOf<OverridePresetRegion>() }
    val selectedFallbackRegions = remember(show) { mutableStateListOf<OverridePresetRegion>() }
    val enabledItems = remember(show) { mutableStateListOf<OverridePresetItem>() }
    var enableUrlTestGroup by remember(show) { mutableStateOf(initialSelection.enableUrlTestGroup) }
    var enableFallbackGroup by remember(show) { mutableStateOf(initialSelection.enableFallbackGroup) }

    LaunchedEffect(show, initialSelection) {
        selectedUrlTestRegions.clear()
        selectedUrlTestRegions.addAll(sortPresetRegions(initialSelection.urlTestRegions))
        selectedFallbackRegions.clear()
        selectedFallbackRegions.addAll(sortPresetRegions(initialSelection.fallbackRegions))
        enabledItems.clear()
        enabledItems.addAll(sortPresetItems(initialSelection.enabledItems))
        enableUrlTestGroup = initialSelection.enableUrlTestGroup
        enableFallbackGroup = initialSelection.enableFallbackGroup
    }

    AppActionBottomSheet(
        show = show,
        modifier = Modifier,
        title = MLang.Override.Draft.PresetTemplate,
        enableNestedScroll = false,
        dragHandleColor = Color.Transparent,
        startAction = {
            AppBottomSheetCloseAction(onClick = onDismiss)
        },
        endAction = {
            AppBottomSheetConfirmAction(
                contentDescription = MLang.Override.Draft.Apply,
                onClick = {
                    onConfirm(
                        OverridePresetTemplateSelection(
                            urlTestRegions = selectedUrlTestRegions.toSet(),
                            fallbackRegions = selectedFallbackRegions.toSet(),
                            enabledItems = enabledItems.toSet(),
                            enableUrlTestGroup = enableUrlTestGroup,
                            enableFallbackGroup = enableFallbackGroup,
                        ),
                    )
                },
            )
        },
        onDismissRequest = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "preset-template-intro") {
                Card(applyHorizontalPadding = false) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = MLang.Override.Draft.PresetApplySummary,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                }
            }

            item(key = "preset-group-types") {
                RoutingSwitchCard(
                    title = MLang.MetaFeature.CustomRouting.GroupTypeTitle,
                    items = listOf("urltest", "fallback"),
                    iconUrl = { type ->
                        officialMrsPresetIconUrl(
                            if (type == "urltest") "Urltest" else "Available",
                        )
                    },
                    isChecked = { type ->
                        when (type) {
                            "urltest" -> enableUrlTestGroup
                            else -> enableFallbackGroup
                        }
                    },
                    onCheckedChange = { item, checked ->
                        when (item) {
                            "urltest" -> enableUrlTestGroup = checked
                            "fallback" -> enableFallbackGroup = checked
                        }
                    },
                    itemTitle = {
                        if (it == "urltest") {
                            MLang.MetaFeature.CustomRouting.GroupTypeUrlTest
                        } else {
                            MLang.MetaFeature.CustomRouting.GroupTypeFallback
                        }
                    },
                    applyHorizontalPadding = false,
                    titleContent = { title ->
                        SmallTitle(
                            text = title,
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        )
                    },
                )
            }

            item(key = "preset-urltest-regions") {
                RoutingSwitchCard(
                    title = MLang.MetaFeature.CustomRouting.UrlTestRegionGroupTitle,
                    items = orderedPresetRegions(),
                    iconUrl = OverridePresetRegion::icon,
                    isChecked = { region -> region in selectedUrlTestRegions },
                    onCheckedChange = { region, checked ->
                        toggleSelection(selectedUrlTestRegions, region, checked)
                    },
                    itemTitle = OverridePresetRegion::displayName,
                    applyHorizontalPadding = false,
                    titleContent = { title ->
                        SmallTitle(
                            text = title,
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        )
                    },
                )
            }

            item(key = "preset-fallback-regions") {
                RoutingSwitchCard(
                    title = MLang.MetaFeature.CustomRouting.FallbackRegionGroupTitle,
                    items = orderedPresetRegions(),
                    iconUrl = OverridePresetRegion::icon,
                    isChecked = { region -> region in selectedFallbackRegions },
                    onCheckedChange = { region, checked ->
                        toggleSelection(selectedFallbackRegions, region, checked)
                    },
                    itemTitle = OverridePresetRegion::displayName,
                    applyHorizontalPadding = false,
                    titleContent = { title ->
                        SmallTitle(
                            text = title,
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        )
                    },
                )
            }

            item(key = "preset-base-items") {
                RoutingSwitchCard(
                    title = MLang.Override.Draft.BasicRouting,
                    items = orderedBasePresetItems(),
                    iconUrl = OverridePresetItem::icon,
                    isChecked = { item -> item in enabledItems },
                    onCheckedChange = { item, checked -> toggleSelection(enabledItems, item, checked) },
                    itemTitle = OverridePresetItem::title,
                    applyHorizontalPadding = false,
                    titleContent = { title ->
                        SmallTitle(
                            text = title,
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        )
                    },
                )
            }

            item(key = "preset-service-items") {
                RoutingSwitchCard(
                    title = MLang.Override.Draft.ServiceRouting,
                    items = orderedServicePresetItems(),
                    iconUrl = OverridePresetItem::icon,
                    isChecked = { item -> item in enabledItems },
                    onCheckedChange = { item, checked -> toggleSelection(enabledItems, item, checked) },
                    itemTitle = OverridePresetItem::title,
                    applyHorizontalPadding = false,
                    titleContent = { title ->
                        SmallTitle(
                            text = title,
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        )
                    },
                )
            }
        }
    }
}

private fun <T> toggleSelection(
    items: MutableList<T>,
    item: T,
    checked: Boolean,
) {
    if (checked) {
        if (item !in items) {
            items.add(item)
        }
    } else {
        items.remove(item)
    }
}
