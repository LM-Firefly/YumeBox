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



package com.github.yumelira.yumebox

import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.github.yumelira.yumebox.common.runtime.StartupGate
import com.github.yumelira.yumebox.common.util.IntentController
import com.github.yumelira.yumebox.common.util.ProxyAutoStartHelper
import com.github.yumelira.yumebox.common.util.openUrl
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.presentation.component.BottomBarContent
import com.github.yumelira.yumebox.presentation.component.EmasUpdateDialogHost
import com.github.yumelira.yumebox.presentation.component.LocalBottomBarLiquidState
import com.github.yumelira.yumebox.presentation.component.LocalBottomBarScrollBehavior
import com.github.yumelira.yumebox.presentation.component.LocalHandlePageChange
import com.github.yumelira.yumebox.presentation.component.LocalNavigator
import com.github.yumelira.yumebox.presentation.component.LocalPagerState
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeStyle
import com.github.yumelira.yumebox.presentation.component.ToastDialogHost
import com.github.yumelira.yumebox.presentation.component.rememberBottomBarScrollBehavior
import com.github.yumelira.yumebox.presentation.screen.ProxyPager
import com.github.yumelira.yumebox.presentation.theme.NavigationTransitions
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.github.yumelira.yumebox.presentation.viewmodel.FeatureViewModel
import com.github.yumelira.yumebox.presentation.webview.WebViewUtils.getPanelUrl
import com.github.yumelira.yumebox.screen.home.HomePager
import com.github.yumelira.yumebox.screen.onboarding.OnboardingLauncher
import com.github.yumelira.yumebox.screen.profiles.ProfilesPager
import com.github.yumelira.yumebox.screen.settings.AppSettingsViewModel
import com.github.yumelira.yumebox.screen.settings.SettingPager
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ProvidersScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.tencent.mmkv.MMKV
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.qualifier.named
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
        private val _pendingImportUrl = MutableStateFlow<String?>(null)
        val pendingImportUrl: StateFlow<String?> = _pendingImportUrl.asStateFlow()
        fun clearPendingImportUrl() {
            _pendingImportUrl.value = null
        }
    }

    private val appSettingsStorage: com.github.yumelira.yumebox.data.store.AppSettingsStorage by inject()
    private val networkSettingsStorage: com.github.yumelira.yumebox.data.store.NetworkSettingsStorage by inject()
    private val profilesRepository: com.github.yumelira.yumebox.runtime.client.ProfilesRepository by inject()
    private val proxyFacade: com.github.yumelira.yumebox.runtime.client.ProxyFacade by inject()
    private val serviceCache: MMKV by inject(qualifier = named("service_cache"))

    private lateinit var intentController: IntentController

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupGate.loadPrimary()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)
        applyExcludeFromRecents(appSettingsStorage.excludeFromRecents.value)

        intentController = IntentController(this, lifecycleScope)
        handleIntent(intent)

        if (!appSettingsStorage.initialSetupCompleted.value) {
            OnboardingLauncher.start(this, previewMode = false)
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION,
                )
            }
        }

        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
            val themeSeedColorArgb = appSettingsViewModel.themeSeedColorArgb.state.collectAsState().value
            val excludeFromRecents = appSettingsViewModel.excludeFromRecents.state.collectAsState().value
            val topBarBlurEnabled = appSettingsViewModel.topBarBlurEnabled.state.collectAsState().value
            val pageScale = appSettingsViewModel.pageScale.state.collectAsState().value

            LaunchedEffect(excludeFromRecents) {
                this@MainActivity.applyExcludeFromRecents(excludeFromRecents)
            }

            ProvideAndroidPlatformTheme {
                val systemDensity = LocalDensity.current
                val scaledDensity = remember(systemDensity, pageScale) {
                    Density(systemDensity.density * pageScale, systemDensity.fontScale)
                }
                CompositionLocalProvider(LocalDensity provides scaledDensity) {
                YumeTheme(
                    themeMode = themeMode,
                    themeSeedColorArgb = themeSeedColorArgb,
                ) {
                    val topBarHazeState = remember { HazeState() }
                    val topBarBackground = MiuixTheme.colorScheme.surface
                    val topBarHazeStyle = remember(topBarBackground) {
                        HazeStyle(
                            backgroundColor = topBarBackground,
                            tint = HazeTint(topBarBackground.copy(0.8f)),
                        )
                    }
                    val navController = rememberNavController()

                    CompositionLocalProvider(
                        LocalTopBarHazeState provides if (topBarBlurEnabled) topBarHazeState else null,
                        LocalTopBarHazeStyle provides if (topBarBlurEnabled) topBarHazeStyle else null,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(), color = MiuixTheme.colorScheme.surface
                        ) {
                            DestinationsNavHost(
                                navGraph = NavGraphs.root,
                                navController = navController,
                                defaultTransitions = NavigationTransitions.defaultStyle,
                            )
                            EmasUpdateDialogHost(currentVersionName = BuildConfig.VERSION_NAME)
                            ToastDialogHost()
                        }
                    }
                }
                }
            }

            LaunchedEffect(Unit) {
                ProxyAutoStartHelper.checkAndAutoStart(
                    context = applicationContext,
                    proxyFacade = proxyFacade,
                    profilesRepository = profilesRepository,
                    appSettingsStorage = appSettingsStorage,
                    networkSettingsStorage = networkSettingsStorage,
                    serviceCache = serviceCache
                )
            }

        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let { safeIntent ->
            safeIntent.data?.let { uri ->
                val scheme = uri.scheme
                if (scheme == "clash" || scheme == "clashmeta") {
                    val host = uri.host
                    if (host == "install-config") {
                        val configUrl = uri.getQueryParameter("url")
                        if (!configUrl.isNullOrBlank()) {
                            _pendingImportUrl.value = configUrl
                        }
                    }
                }
            }

            intentController.handleIntent(safeIntent)
        }
    }

    @Suppress("DEPRECATION")
    private fun applyExcludeFromRecents(exclude: Boolean) {

        runCatching {
            val am = getSystemService(ActivityManager::class.java) ?: return@runCatching
            val currentTaskId = taskId
            val task = am.appTasks.firstOrNull { appTask: ActivityManager.AppTask ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appTask.taskInfo.taskId == currentTaskId
                } else {
                    appTask.taskInfo.id == currentTaskId
                }
            }
            task?.setExcludeFromRecents(exclude)
        }
    }
}

@Composable
@Destination<RootGraph>
fun MainScreen(
    navigator: DestinationsNavigator,
    initialPage: Int = 0,
) {
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, 3), pageCount = { 4 })
    val hazeState = remember { HazeState() }
    val bottomBarLiquidState = rememberLiquidState()

    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val featureViewModel = koinViewModel<FeatureViewModel>()
    val bottomBarAutoHideEnabled by appSettingsViewModel.bottomBarAutoHide.state.collectAsState()
    val bottomBarLiquidGlassEnabled by appSettingsViewModel.bottomBarLiquidGlassEnabled.state.collectAsState()
    val selectedPanelType by featureViewModel.selectedPanelType.state.collectAsState()
    val panelOpenMode by featureViewModel.panelOpenMode.state.collectAsState()
    val bottomBarScrollBehavior = rememberBottomBarScrollBehavior(autoHideEnabled = bottomBarAutoHideEnabled)

    var pageChangeJob by remember { mutableStateOf<Job?>(null) }

    val handlePageChange: (Int) -> Unit =
        remember(pagerState, coroutineScope) {
            { targetPage ->
                if (targetPage == pagerState.currentPage && !pagerState.isScrollInProgress) return@remember

                pageChangeJob?.cancel()
                pageChangeJob = coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        page = targetPage,
                    )
                }
            }
        }

    CompositionLocalProvider(
        LocalPagerState provides pagerState,
        LocalHandlePageChange provides handlePageChange,
        LocalNavigator provides navigator,
        LocalBottomBarScrollBehavior provides bottomBarScrollBehavior,
        LocalBottomBarLiquidState provides if (bottomBarLiquidGlassEnabled) bottomBarLiquidState else null,
    ) {
        Scaffold { innerPadding ->
            Box(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val layoutDirection = LocalLayoutDirection.current
                val systemBottomInset = with(density) {
                    val navBottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.getBottom(this)
                    val gestureBottom = androidx.compose.foundation.layout.WindowInsets.systemGestures.getBottom(this)
                    max(navBottom, gestureBottom).toDp()
                }
                val bottomBarReservedHeight = 74.dp
                val safeMainPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + bottomBarReservedHeight + systemBottomInset,
                    start = WindowInsets.systemBars.asPaddingValues().calculateStartPadding(layoutDirection),
                    end = WindowInsets.systemBars.asPaddingValues().calculateEndPadding(layoutDirection),
                )

                HorizontalPager(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = hazeState)
                        .liquefiable(bottomBarLiquidState),
                    state = pagerState,
                    beyondViewportPageCount = 2,
                    userScrollEnabled = true,
                    pageNestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
                        state = pagerState,
                        orientation = androidx.compose.foundation.gestures.Orientation.Horizontal
                    ),
                ) { page ->
                    when (page) {
                        0 -> HomePager(safeMainPadding)
                        1 -> ProxyPager(
                            mainInnerPadding = safeMainPadding,
                            onNavigateToProviders = {
                                navigator.navigate(ProvidersScreenDestination) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenPanel = onOpenPanel@{
                                val context = activity ?: return@onOpenPanel
                                val panelUrl = getPanelUrl(selectedPanelType)
                                if (panelUrl.isEmpty()) return@onOpenPanel
                                when (panelOpenMode) {
                                    LinkOpenMode.IN_APP -> WebViewActivity.start(context, panelUrl)
                                    LinkOpenMode.EXTERNAL_BROWSER -> openUrl(context, panelUrl)
                                }
                            },
                            isActive = page == pagerState.currentPage,
                        )

                        2 -> ProfilesPager(safeMainPadding)
                        3 -> SettingPager(safeMainPadding)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    BottomBarContent()
                }
            }
        }
    }
}
