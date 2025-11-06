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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.data.util.OverridePresetItem
import com.github.yumelira.yumebox.data.util.OverridePresetRegion
import com.github.yumelira.yumebox.data.util.OverridePresetTemplateSelection
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OverridePresetTemplateSheet(
    show: Boolean,
    initialSelection: OverridePresetTemplateSelection = OverridePresetTemplateSelection(),
    onDismiss: () -> Unit,
    onConfirm: (OverridePresetTemplateSelection) -> Unit,
) {
    val selectedRegions = remember(show) { mutableStateListOf<OverridePresetRegion>() }
    val enabledItems = remember(show) { mutableStateListOf<OverridePresetItem>() }
    val templateId = initialSelection.templateId

    LaunchedEffect(show, initialSelection) {
        selectedRegions.clear()
        selectedRegions.addAll(initialSelection.regions.toList().sortedBy(OverridePresetRegion::ordinal))
        enabledItems.clear()
        enabledItems.addAll(initialSelection.enabledItems.toList().sortedBy(OverridePresetItem::ordinal))
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
                            templateId = templateId,
                            regions = selectedRegions.toSet(),
                            enabledItems = enabledItems.toSet(),
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
                            text = "应用后会覆盖当前覆写里的规则提供者、策略组和规则",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                }
            }

            presetSwitchCard(
                key = "preset-regions",
                title = MLang.Override.Draft.RegionalAutoGroup,
                items = OverridePresetRegion.entries.toList(),
                isChecked = { region -> region in selectedRegions },
                onCheckedChange = { region, checked ->
                    if (checked) {
                        if (region !in selectedRegions) {
                            selectedRegions.add(region)
                        }
                    } else {
                        selectedRegions.remove(region)
                    }
                },
                itemTitle = OverridePresetRegion::groupName,
            )

            presetSwitchCard(
                key = "preset-base-items",
                title = MLang.Override.Draft.BasicRouting,
                items = OverridePresetItem.entries.filterNot(OverridePresetItem::isService),
                isChecked = { item -> item in enabledItems },
                onCheckedChange = { item, checked ->
                    togglePresetItem(enabledItems, item, checked)
                },
                itemTitle = OverridePresetItem::title,
            )

            presetSwitchCard(
                key = "preset-service-items",
                title = MLang.Override.Draft.ServiceRouting,
                items = OverridePresetItem.entries.filter(OverridePresetItem::isService),
                isChecked = { item -> item in enabledItems },
                onCheckedChange = { item, checked ->
                    togglePresetItem(enabledItems, item, checked)
                },
                itemTitle = OverridePresetItem::title,
            )
        }
    }
}

private fun togglePresetItem(
    enabledItems: MutableList<OverridePresetItem>,
    item: OverridePresetItem,
    checked: Boolean,
) {
    if (checked) {
        if (item !in enabledItems) {
            enabledItems.add(item)
        }
    } else {
        enabledItems.remove(item)
    }
}

private fun <T> LazyListScope.presetSwitchCard(
    key: String,
    title: String,
    items: List<T>,
    isChecked: (T) -> Boolean,
    onCheckedChange: (T, Boolean) -> Unit,
    itemTitle: (T) -> String,
    itemSummary: ((T) -> String)? = null,
) {
    item(key = key) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SmallTitle(
                text = title,
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            )
            Card(applyHorizontalPadding = false) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items.forEach { item ->
                        SuperSwitch(
                            title = itemTitle(item),
                            summary = itemSummary?.invoke(item),
                            checked = isChecked(item),
                            onCheckedChange = { checked -> onCheckedChange(item, checked) },
                        )
                    }
                }
            }
        }
    }
}
