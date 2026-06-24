/*
 * This file is part of FlyCat.
 *
 * FlyCat is free software: you can redistribute it and/or modify
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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.screen.proxy

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yumelira.yumebox.feature.proxy.presentation.screen.ProxyPager
import com.github.yumelira.yumebox.presentation.component.Navigator
import com.github.yumelira.yumebox.presentation.navigation.Route
import com.github.yumelira.yumebox.screen.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun NodesScreen(navigator: Navigator) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val isRunning by homeViewModel.isRunning.collectAsStateWithLifecycle()

    ProxyPager(
        mainInnerPadding = PaddingValues(),
        onNavigateToProviders = { navigator.push(Route.Providers) },
        isActive = isRunning,
    )
}
