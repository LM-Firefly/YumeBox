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
import com.github.yumelira.yumebox.presentation.theme.UiDp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Badge-plus`
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val SelectionSheetListMaxHeight = UiDp.dp420

data class OverrideSelectionGroup(
    val title: String,
    val items: List<String>,
)

@Composable
fun OverrideSingleValueSelectionSheet(
    show: Boolean,
    title: String,
    value: String,
    groups: List<OverrideSelectionGroup>,
    customInputLabel: String,
    allowCustomValue: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val knownValues = remember(groups) { collectSelectionItems(groups) }
    var selectedValue by remember(show, value, knownValues) {
        mutableStateOf(value.trim())
    }
    var showCustomInputDialog by remember(show) { mutableStateOf(false) }
    val customValue = selectedValue
        .trim()
        .takeIf { allowCustomValue && it.isNotBlank() && it !in knownValues }
    val selectedKnownValues = listOfNotNull(
        selectedValue
            .trim()
            .takeIf { it.isNotBlank() && it in knownValues },
    )

    AppActionBottomSheet(
        show = show,
        modifier = Modifier,
        title = title,
        enableNestedScroll = false,
        startAction = {
            AppBottomSheetCloseAction(onClick = onDismiss)
        },
        endAction = {
            AppBottomSheetConfirmAction(
                contentDescription = MLang.Override.Editor.Confirm,
                onClick = { onConfirm(selectedValue.trim()) },
            )
        },
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = UiDp.dp16),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
        ) {
            if (allowCustomValue) {
                SelectionAddCustomCard(
                    title = MLang.Override.Editor.AddCustom,
                    onClick = { showCustomInputDialog = true },
                )
            }
            customValue?.let { value ->
                SelectionValueListCard(
                    items = listOf(value),
                    selectedValues = listOf(value),
                    onItemClick = { itemValue ->
                        if (selectedValue == itemValue) {
                            selectedValue = ""
                        }
                    },
                )
            }
            if (knownValues.isNotEmpty()) {
                SelectionValueListCard(
                    items = knownValues,
                    selectedValues = selectedKnownValues,
                    onItemClick = { itemValue ->
                        selectedValue = if (selectedValue == itemValue) {
                            ""
                        } else {
                            itemValue
                        }
                    },
                )
            }
        }
    }

    OverrideSelectionInputDialog(
        show = allowCustomValue && showCustomInputDialog,
        title = MLang.Override.Editor.AddCustom,
        label = customInputLabel,
        onConfirm = { inputValue ->
            selectedValue = inputValue.trim()
            showCustomInputDialog = false
        },
        onDismiss = { showCustomInputDialog = false },
    )
}

@Composable
fun OverrideMultiValueSelectionSheet(
    show: Boolean,
    title: String,
    values: List<String>,
    groups: List<OverrideSelectionGroup>,
    customInputLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val knownValues = remember(groups) { collectSelectionItems(groups) }
    val selectedValues = remember { mutableStateListOf<String>() }
    var showCustomInputDialog by remember(show) { mutableStateOf(false) }

    LaunchedEffect(show, values) {
        selectedValues.clear()
        selectedValues.addAll(
            values
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct(),
        )
    }

    val normalizedSelectedValues = selectedValues
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
    val customValues = normalizedSelectedValues.filterNot(knownValues::contains)

    AppActionBottomSheet(
        show = show,
        modifier = Modifier,
        title = title,
        enableNestedScroll = false,
        startAction = {
            AppBottomSheetCloseAction(onClick = onDismiss)
        },
        endAction = {
            AppBottomSheetConfirmAction(
                contentDescription = MLang.Override.Editor.Confirm,
                onClick = { onConfirm(selectedValues.toList()) },
            )
        },
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = UiDp.dp16),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
        ) {
            SelectionAddCustomCard(
                title = MLang.Override.Editor.AddCustom,
                onClick = { showCustomInputDialog = true },
            )
            if (customValues.isNotEmpty()) {
                SelectionValueListCard(
                    items = customValues,
                    selectedValues = customValues,
                    onItemClick = { itemValue ->
                        selectedValues.remove(itemValue)
                    },
                )
            }
            if (knownValues.isNotEmpty()) {
                SelectionValueListCard(
                    items = knownValues,
                    selectedValues = selectedValues,
                    onItemClick = { itemValue ->
                        if (itemValue in selectedValues) {
                            selectedValues.remove(itemValue)
                        } else {
                            selectedValues.add(itemValue)
                        }
                    },
                )
            }
        }
    }

    OverrideSelectionInputDialog(
        show = showCustomInputDialog,
        title = MLang.Override.Editor.AddCustom,
        label = customInputLabel,
        onConfirm = { inputValue ->
            val normalizedValue = inputValue.trim()
            if (normalizedValue !in selectedValues) {
                selectedValues.add(normalizedValue)
            }
            showCustomInputDialog = false
        },
        onDismiss = { showCustomInputDialog = false },
    )
}

@Composable
private fun SelectionAddCustomCard(
    title: String,
    onClick: () -> Unit,
) {
    Card(applyHorizontalPadding = false) {
        BasicComponent(
            title = title,
            endActions = {
                Icon(
                    imageVector = Yume.`Badge-plus`,
                    contentDescription = title,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            },
            onClick = onClick,
        )
    }
}

@Composable
private fun SelectionValueListCard(
    items: List<String>,
    selectedValues: List<String>,
    onItemClick: (String) -> Unit,
) {
    Card(applyHorizontalPadding = false) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = SelectionSheetListMaxHeight),
        ) {
            items(
                items = items,
                key = { itemValue -> itemValue },
            ) { itemValue ->
                BasicComponent(
                    title = itemValue,
                    endActions = {
                        Checkbox(
                            state = ToggleableState(itemValue in selectedValues),
                            onClick = { onItemClick(itemValue) },
                        )
                    },
                    onClick = { onItemClick(itemValue) },
                )
            }
        }
    }
}

@Composable
private fun OverrideSelectionInputDialog(
    show: Boolean,
    title: String,
    label: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) {
        return
    }

    var inputValue by remember(show) { mutableStateOf("") }
    var errorText by remember(show) { mutableStateOf<String?>(null) }

    AppTextFieldDialog(
        show = show,
        title = title,
        value = inputValue,
        onValueChange = {
            inputValue = it
            errorText = null
        },
        onDismissRequest = onDismiss,
        onConfirm = {
            val normalizedValue = inputValue.trim()
            if (normalizedValue.isBlank()) {
                errorText = MLang.Override.Editor.ContentEmpty
                return@AppTextFieldDialog
            }
            onConfirm(normalizedValue)
        },
        label = label,
        supportingContent = {
            errorText?.let { message ->
                OverrideFieldAssistText(
                    text = message,
                    color = MiuixTheme.colorScheme.error,
                )
            }
        },
    )
}

private fun collectSelectionItems(groups: List<OverrideSelectionGroup>): List<String> {
    val seenValues = LinkedHashSet<String>()
    groups.forEach { group ->
        group.items.forEach { item ->
            val normalizedItem = item.trim()
            if (normalizedItem.isNotBlank()) {
                seenValues += normalizedItem
            }
        }
    }
    return seenValues.toList()
}
