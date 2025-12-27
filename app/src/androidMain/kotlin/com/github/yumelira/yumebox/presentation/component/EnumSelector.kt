package com.github.yumelira.yumebox.presentation.component

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.extra.SuperDropdown

@Composable
fun <T> EnumSelector(
    title: String,
    summary: String? = null,
    currentValue: T,
    items: List<String>,
    values: List<T>,
    onValueChange: (T) -> Unit,
) {
    val selectedIndex = values.indexOf(currentValue).coerceAtLeast(0)

    SuperDropdown(
        title = title,
        summary = summary,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            if (index >= 0 && index < values.size) {
                onValueChange(values[index])
            }
        },
    )
}

