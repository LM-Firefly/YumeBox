package com.github.yumelira.yumebox.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.LocalNavigator
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.screen.home.HomeIdleContent
import com.github.yumelira.yumebox.presentation.screen.home.HomeRunningContent
import com.github.yumelira.yumebox.presentation.screen.home.ProxyControlButton
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.viewmodel.HomeViewModel
import com.ramcosta.composedestinations.generated.destinations.ConnectionsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TrafficStatisticsScreenDestination
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

private enum class HomeDisplayState {
    Idle,
    Running
}

@Composable
fun HomePager(mainInnerPadding: PaddingValues) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val navigator = LocalNavigator.current

    val displayRunning by homeViewModel.displayRunning.collectAsStateWithLifecycle()
    val isToggling by homeViewModel.isToggling.collectAsStateWithLifecycle()
    val trafficNow by homeViewModel.trafficNow.collectAsStateWithLifecycle()
    val profiles by homeViewModel.profiles.collectAsStateWithLifecycle()
    val ipMonitoringState by homeViewModel.ipMonitoringState.collectAsStateWithLifecycle()
    val recommendedProfile by homeViewModel.recommendedProfile.collectAsStateWithLifecycle()
    val hasEnabledProfile by homeViewModel.hasEnabledProfile.collectAsStateWithLifecycle(initialValue = false)
    val currentProfile by homeViewModel.currentProfile.collectAsStateWithLifecycle()
    val oneWord by homeViewModel.oneWord.collectAsStateWithLifecycle()
    val oneWordAuthor by homeViewModel.oneWordAuthor.collectAsStateWithLifecycle()
    val selectedServerName by homeViewModel.selectedServerName.collectAsStateWithLifecycle()
    val selectedServerPing by homeViewModel.selectedServerPing.collectAsStateWithLifecycle()
    val speedHistory by homeViewModel.speedHistory.collectAsStateWithLifecycle()
    val connections by homeViewModel.connections.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    val displayState = if (displayRunning) HomeDisplayState.Running else HomeDisplayState.Idle

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

    Scaffold(
        topBar = { TopBar(title = MLang.Home.Title, scrollBehavior = scrollBehavior) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING)
                            .padding(top = AppConstants.UI.DEFAULT_VERTICAL_SPACING),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(AppConstants.UI.DEFAULT_VERTICAL_SPACING)
                    ) {
                        AnimatedContent(
                            targetState = displayState,
                            transitionSpec = { createHomeTransitionSpec() },
                            label = "HomeContentTransition"
                        ) { state ->
                            when (state) {
                                HomeDisplayState.Idle -> HomeIdleContent(
                                    oneWord = oneWord,
                                    author = oneWordAuthor
                                )

                                HomeDisplayState.Running -> HomeRunningContent(
                                    trafficNow = trafficNow,
                                    profileName = currentProfile?.name,
                                    tunnelMode = null,
                                    serverName = selectedServerName,
                                    serverPing = selectedServerPing,
                                    ipMonitoringState = ipMonitoringState,
                                    speedHistory = speedHistory,
                                    connections = connections,
                                    onChartClick = {
                                        navigator.navigate(TrafficStatisticsScreenDestination) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onTopologyClick = {
                                        navigator.navigate(ConnectionsScreenDestination) { launchSingleTop = true }
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }

            ProxyControlButton(
                isRunning = displayRunning,
                isEnabled = profiles.isNotEmpty() && hasEnabledProfile && !isToggling,
                hasEnabledProfile = hasEnabledProfile,
                hasProfiles = profiles.isNotEmpty(),
                onClick = {
                    handleProxyToggle(
                        isRunning = displayRunning,
                        recommendedProfile = recommendedProfile,
                        profiles = profiles,
                        onStart = { profile ->
                            pendingProfileId = profile.id
                            coroutineScope.launch {
                                homeViewModel.startProxy(profileId = profile.id)
                            }
                        },
                        onStop = {
                            coroutineScope.launch {
                                homeViewModel.stopProxy()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING)
                    .padding(bottom = mainInnerPadding.calculateBottomPadding() + 32.dp)
                    .padding(top = AppConstants.UI.DEFAULT_VERTICAL_SPACING)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<HomeDisplayState>.createHomeTransitionSpec(): ContentTransform {
    val fadeOutDuration = 180
    val fadeInDuration = 240
    val delayBetween = 40

    val exitTransition = fadeOut(
        animationSpec = tween(fadeOutDuration, easing = AnimationSpecs.ExitEasing),
        targetAlpha = 0f
    )

    val enterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = fadeInDuration,
            delayMillis = fadeOutDuration + delayBetween,
            easing = AnimationSpecs.EnterEasing
        ),
        initialAlpha = 0f
    )

    return enterTransition.togetherWith(exitTransition).using(
        SizeTransform(
            clip = false,
            sizeAnimationSpec = { _, _ ->
                tween(
                    durationMillis = fadeOutDuration + delayBetween,
                    easing = AnimationSpecs.EmphasizedDecelerate
                )
            }
        )
    )
}

private fun handleProxyToggle(
    isRunning: Boolean,
    recommendedProfile: com.github.yumelira.yumebox.data.model.Profile?,
    profiles: List<com.github.yumelira.yumebox.data.model.Profile>,
    onStart: (com.github.yumelira.yumebox.data.model.Profile) -> Unit,
    onStop: () -> Unit
) {
    if (!isRunning) {
        val targetProfile = recommendedProfile ?: profiles.find { it.enabled }
        targetProfile?.let { profile -> onStart(profile) }
    } else {
        onStop()
    }
}
