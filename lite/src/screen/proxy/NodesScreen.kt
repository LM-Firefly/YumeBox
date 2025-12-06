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
