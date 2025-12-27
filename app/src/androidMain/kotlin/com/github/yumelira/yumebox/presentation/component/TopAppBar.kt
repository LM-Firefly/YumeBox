package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar

@Composable
fun TopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}
