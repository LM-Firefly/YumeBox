package com.github.yumelira.yumebox.screen.navigation

import androidx.compose.runtime.Composable
import com.github.yumelira.yumebox.feature.meta.presentation.screen.CustomRoutingScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
@Destination<RootGraph>
fun CustomRoutingRoute(navigator: DestinationsNavigator) {
    CustomRoutingScreen(
        onNavigateBack = { navigator.navigateUp() }
    )
}
