package com.github.yumelira.yumebox.screen.proxy

import com.github.yumelira.yumebox.presentation.screen.ProvidersContent
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<RootGraph>
@androidx.compose.runtime.Composable
fun ProvidersScreen(navigator: DestinationsNavigator) {
    ProvidersContent(navigator = navigator)
}
