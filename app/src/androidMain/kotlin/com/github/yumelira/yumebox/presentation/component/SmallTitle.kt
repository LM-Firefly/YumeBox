package com.github.yumelira.yumebox.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.yumelira.yumebox.presentation.theme.topPadding
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
fun SmallTitle(text: String) {
    SmallTitle(
        modifier = Modifier.topPadding(),
        text = text,
    )
}
