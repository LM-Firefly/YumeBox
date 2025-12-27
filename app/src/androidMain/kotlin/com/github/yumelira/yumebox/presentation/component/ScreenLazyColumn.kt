package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.theme.LocalSpacing
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ScreenLazyColumn(
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    topPadding: Dp = 0.dp,
    enableGlobalScroll: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val bottomBarScrollBehavior = if (enableGlobalScroll) LocalBottomBarScrollBehavior.current else null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .let { mod ->
                if (enableGlobalScroll && bottomBarScrollBehavior != null) {
                    mod.nestedScroll(bottomBarScrollBehavior.nestedScrollConnection)
                } else mod
            },
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + topPadding,
            bottom = innerPadding.calculateBottomPadding() + bottomPadding,
        ),
        overscrollEffect = null,
        content = content,
    )
}

@Composable
fun combinePaddingValues(
    localPadding: PaddingValues,
    mainPadding: PaddingValues,
): PaddingValues {
    return PaddingValues(
        top = localPadding.calculateTopPadding(),
        bottom = localPadding.calculateBottomPadding() + mainPadding.calculateBottomPadding() + LocalSpacing.current.md,
        start = localPadding.calculateStartPadding(LayoutDirection.Ltr),
        end = localPadding.calculateEndPadding(LayoutDirection.Ltr),
    )
}
