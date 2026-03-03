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

package com.github.yumelira.yumebox.screen.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.presentation.component.LocalNavigator
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.screen.home.IpInfoDisplay
import com.github.yumelira.yumebox.screen.home.NodeInfoDisplay
import com.github.yumelira.yumebox.screen.home.SpeedChart
import com.github.yumelira.yumebox.screen.home.TrafficDisplay
import com.github.yumelira.yumebox.screen.home.HomeViewModel
import com.ramcosta.composedestinations.generated.destinations.TrafficStatisticsScreenDestination
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun HomePager(mainInnerPadding: PaddingValues) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val navigator = LocalNavigator.current

    val displayRunning by homeViewModel.displayRunning.collectAsState()
    val isToggling by homeViewModel.isToggling.collectAsState()
    val trafficNow by homeViewModel.trafficNow.collectAsState()
    val profiles by homeViewModel.profiles.collectAsState()
    val profilesLoaded by homeViewModel.profilesLoaded.collectAsState()
    val ipMonitoringState by homeViewModel.ipMonitoringState.collectAsState()
    val recommendedProfile by homeViewModel.recommendedProfile.collectAsState()
    val hasEnabledProfile by homeViewModel.hasEnabledProfile.collectAsState(initial = false)
    val currentProfile by homeViewModel.currentProfile.collectAsState()
    val selectedServerName by homeViewModel.selectedServerName.collectAsState()
    val selectedServerPing by homeViewModel.selectedServerPing.collectAsState()
    val speedHistory by homeViewModel.speedHistory.collectAsState()
    val isTunMode by homeViewModel.isTunMode.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    var pendingProfileId by remember { mutableStateOf<String?>(null) }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingProfileId?.let { profileId ->
                homeViewModel.startProxy(profileId, useTunMode = true)
            }
        }
        pendingProfileId = null
    }

    LaunchedEffect(Unit) {
        homeViewModel.vpnPrepareIntent.collect { intent ->
            vpnPermissionLauncher.launch(intent)
        }
    }

    val scrollBehavior = MiuixScrollBehavior()

    val isProxyEnabled = profilesLoaded && profiles.isNotEmpty() && hasEnabledProfile && !isToggling

    Scaffold(
        topBar = { TopBar(title = MLang.Home.Title, scrollBehavior = scrollBehavior) },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(AppConstants.UI.DEFAULT_VERTICAL_SPACING)
                ) {
                    // 可点击的流量区域 — 点击即启停代理
                    TrafficDisplay(
                        trafficNow = if (displayRunning) {
                            TrafficData.from(trafficNow)
                        } else {
                            TrafficData.ZERO
                        },
                        profileName = currentProfile?.name,
                        tunnelMode = null,
                        isRunning = displayRunning,
                        isTunMode = isTunMode,
                        isEnabled = isProxyEnabled,
                        onClick = {
                            handleProxyToggle(
                                isRunning = displayRunning,
                                recommendedProfile = recommendedProfile,
                                onStart = { profile ->
                                    pendingProfileId = profile.uuid.toString()
                                    coroutineScope.launch {
                                        homeViewModel.startProxy(
                                            profileId = profile.uuid.toString(),
                                            useTunMode = true
                                        )
                                    }
                                },
                                onStop = {
                                    coroutineScope.launch { homeViewModel.stopProxy() }
                                }
                            )
                        }
                    )

                    // 节点 / IP / 速度图表 — 始终显示
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppConstants.UI.DEFAULT_VERTICAL_SPACING)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            NodeInfoDisplay(
                                serverName = selectedServerName,
                                serverPing = selectedServerPing
                            )
                            IpInfoDisplay(state = ipMonitoringState)
                        }

                        SpeedChart(
                            speedHistory = speedHistory,
                            onClick = {
                                navigator.navigate(TrafficStatisticsScreenDestination) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

private fun handleProxyToggle(
    isRunning: Boolean,
    recommendedProfile: com.github.yumelira.yumebox.service.runtime.entity.Profile?,
    onStart: (com.github.yumelira.yumebox.service.runtime.entity.Profile) -> Unit,
    onStop: () -> Unit
) {
    if (!isRunning) {
        recommendedProfile?.let { profile -> onStart(profile) }
    } else {
        onStop()
    }
}
