package com.github.yumelira.yumebox.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.yumelira.yumebox.presentation.theme.horizontalPadding
import top.yukonga.miuix.kmp.basic.Card

@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.horizontalPadding(),
    ) {
        content()
    }
}
