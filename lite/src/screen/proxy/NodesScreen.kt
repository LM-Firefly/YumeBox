package com.github.yumelira.yumebox.screen.proxy

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.yumelira.yumebox.screen.home.HomeViewModel
import com.github.yumelira.yumebox.presentation.screen.ProxyPager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ProvidersScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel

@Destination<RootGraph>
@androidx.compose.runtime.Composable
fun NodesScreen(navigator: DestinationsNavigator) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val isRunning by homeViewModel.isRunning.collectAsState()

    ProxyPager(
        mainInnerPadding = PaddingValues(),
        onNavigateToProviders = { navigator.navigate(ProvidersScreenDestination) },
        onOpenPanel = null,
        isActive = isRunning,
    )
}
