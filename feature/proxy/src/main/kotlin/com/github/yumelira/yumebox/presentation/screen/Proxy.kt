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

package com.github.yumelira.yumebox.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.domain.model.*
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import com.github.yumelira.yumebox.presentation.icon.yume.Speed
import com.github.yumelira.yumebox.presentation.icon.yume.`Squares-exclude`
import com.github.yumelira.yumebox.presentation.icon.yume.Zashboard
import com.github.yumelira.yumebox.presentation.screen.node.nodeGridItems
import com.github.yumelira.yumebox.presentation.screen.node.nodeGroupItems
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.theme.LocalSpacing
import com.github.yumelira.yumebox.presentation.util.KeepLazyListTopAnchorOnReorder
import com.github.yumelira.yumebox.presentation.viewmodel.ProxyViewModel
import dev.chrisbanes.haze.hazeSource
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.roundToInt

private fun fractionToSliderValue(fraction: Float): Float {
    val normalized = normalizeProxySheetHeightFraction(fraction)
    val range = PROXY_SHEET_HEIGHT_FRACTION_MAX - PROXY_SHEET_HEIGHT_FRACTION_MIN
    return ((normalized - PROXY_SHEET_HEIGHT_FRACTION_MIN) / range).coerceIn(0f, 1f)
}

private fun sliderValueToFraction(sliderValue: Float): Float {
    val range = PROXY_SHEET_HEIGHT_FRACTION_MAX - PROXY_SHEET_HEIGHT_FRACTION_MIN
    return normalizeProxySheetHeightFraction(
        PROXY_SHEET_HEIGHT_FRACTION_MIN + sliderValue.coerceIn(0f, 1f) * range
    )
}

private object ProxyPageSpacing {
    val ContentTop = 20.dp
    val ContentHorizontal = 12.dp
    val ItemVertical = 6.dp
}

@Composable
fun ProxyPager(
    mainInnerPadding: PaddingValues,
    onNavigateToProviders: () -> Unit,
    onOpenPanel: () -> Unit,
    isActive: Boolean
) {
    val proxyViewModel = koinViewModel<ProxyViewModel>()

    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val testingGroupNames by proxyViewModel.testingGroupNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()
    val groupScrollBehavior = MiuixScrollBehavior()
    val topBarHazeState = LocalTopBarHazeState.current

    val showSettingsBottomSheet = rememberSaveable { mutableStateOf(false) }

    var selectedGroupName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedGroupSnapshot by remember { mutableStateOf<ProxyGroupInfo?>(null) }
    val onTestDelay = remember { { proxyViewModel.testDelay() } }
    val proxyGroupsByName = remember(proxyGroups) { proxyGroups.associateBy { it.name } }
    val selectedGroup by remember(selectedGroupName, proxyGroupsByName) {
        derivedStateOf {
            val name = selectedGroupName ?: return@derivedStateOf null
            proxyGroupsByName[name]
        }
    }
    // Keep a snapshot so node page doesn't flash empty during back
    LaunchedEffect(selectedGroup) {
        selectedGroup?.let { selectedGroupSnapshot = it }
    }
    val displayGroup = selectedGroup ?: selectedGroupSnapshot
    val fabGroup = displayGroup
    val isFabTesting = fabGroup?.name?.let(testingGroupNames::contains) == true

    // FAB 滚动隐藏状态
    var fabHidden by rememberSaveable { mutableStateOf(false) }

    // System back on node page → back to group list
    BackHandler(enabled = selectedGroupName != null) {
        selectedGroupName = null
    }

    LaunchedEffect(isActive) {
        proxyViewModel.ensureCoreLoaded(isActive)
    }

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedGroupName != null && fabGroup != null && !fabHidden && !isFabTesting,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = AnimationSpecs.Proxy.FabDuration),
                    initialOffsetY = { it },
                ) + fadeIn(animationSpec = tween(durationMillis = AnimationSpecs.Proxy.FabFadeDuration)),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = AnimationSpecs.Proxy.FabDuration),
                    targetOffsetY = { it },
                ) + fadeOut(animationSpec = tween(durationMillis = AnimationSpecs.Proxy.FabFadeDuration)),
                label = "proxy_test_fab_visibility",
            ) {
                FloatingActionButton(
                    modifier = Modifier.padding(end = 20.dp, bottom = 85.dp),
                    onClick = {
                        val targetGroup = fabGroup ?: return@FloatingActionButton
                        proxyViewModel.testDelay(targetGroup.name)
                    },
                ) {
                    Icon(
                        imageVector = Yume.Speed,
                        contentDescription = MLang.Proxy.Action.Test,
                        tint = MiuixTheme.colorScheme.background,
                    )
                }
            }
        },
        topBar = {
            ProxyTopBar(
                title = MLang.Proxy.Title,
                scrollBehavior = groupScrollBehavior,
                showBack = false,
                onBack = {},
                onNavigateToProviders = onNavigateToProviders,
                onOpenPanel = onOpenPanel,
                onTestDelay = if (selectedGroupName == null) onTestDelay else null,
                onShowSettings = { showSettingsBottomSheet.value = true },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { mod -> if (topBarHazeState != null) mod.hazeSource(state = topBarHazeState) else mod }
        ) {
            AnimatedContent(
                targetState = selectedGroupName,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 260, easing = AnimationSpecs.Legacy),
                            initialOffsetX = { it },
                        ) + fadeIn(animationSpec = tween(durationMillis = 220))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(durationMillis = 220, easing = AnimationSpecs.Legacy),
                                targetOffsetX = { -it / 3 },
                            ) + fadeOut(animationSpec = tween(durationMillis = 180)))
                    } else {
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = AnimationSpecs.Legacy),
                            initialOffsetX = { -it / 3 },
                        ) + fadeIn(animationSpec = tween(durationMillis = 200))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(durationMillis = 240, easing = AnimationSpecs.Legacy),
                                targetOffsetX = { it },
                            ) + fadeOut(animationSpec = tween(durationMillis = 180)))
                    }
                },
                label = "proxy_content_slide",
            ) { targetGroupName ->
                if (targetGroupName == null) {
                    if (proxyGroups.isEmpty()) {
                        CenteredText(
                            firstLine = MLang.Proxy.Empty.NoNodes,
                            secondLine = MLang.Proxy.Empty.Hint,
                        )
                    } else {
                        ProxyContent(
                            proxyGroups = proxyGroups,
                            scrollBehavior = groupScrollBehavior,
                            innerPadding = innerPadding,
                            mainInnerPadding = mainInnerPadding,
                            testingGroupNames = testingGroupNames,
                            onGroupClick = { group ->
                                selectedGroupName = group.name
                            },
                            onGroupBoundsChanged = { _, _ -> },
                        )
                    }
                } else {
                    val currentGroup = proxyGroups.find { it.name == targetGroupName } ?: displayGroup
                    NodeListPage(
                        group = currentGroup,
                        displayMode = displayMode,
                        sortMode = sortMode,
                        testingGroupNames = testingGroupNames,
                        mainInnerPadding = mainInnerPadding,
                        outerInnerPadding = innerPadding,
                        scrollBehavior = groupScrollBehavior,
                        onSelectProxy = { groupName, proxyName ->
                            proxyViewModel.selectProxy(groupName, proxyName)
                        },
                        onTestDelay = { groupName -> proxyViewModel.testDelay(groupName) },
                        onScrollDirectionChanged = { hidden -> fabHidden = hidden },
                    )
                }
            }
        }

        // Settings bottom sheet
        WindowBottomSheet(
            show = showSettingsBottomSheet.value,
            title = MLang.Proxy.Settings.Title,
            onDismissRequest = { showSettingsBottomSheet.value = false },
            insideMargin = DpSize(32.dp, 16.dp),
            enableNestedScroll = false,
        ) {
            ProxySettingsContent(
                proxyViewModel = proxyViewModel,
                sortMode = sortMode,
                onDismiss = { showSettingsBottomSheet.value = false },
            )
        }
    }
}

@Composable
private fun ProxyTopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    showBack: Boolean,
    onBack: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onOpenPanel: () -> Unit,
    onTestDelay: (() -> Unit)?,
    onShowSettings: () -> Unit,
) {
    TopBar(title = title, scrollBehavior = scrollBehavior, navigationIcon = {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (showBack) {
                IconButton(
                    modifier = Modifier.padding(start = 24.dp),
                    onClick = onBack,
                ) {
                    Icon(MiuixIcons.Back, contentDescription = MLang.Component.Navigation.Back)
                }
            } else {
                IconButton(
                    modifier = Modifier.padding(start = 24.dp), onClick = onNavigateToProviders
                ) {
                    Icon(Yume.`Squares-exclude`, contentDescription = MLang.Proxy.Action.ExternalResources)
                }
                IconButton(onClick = onOpenPanel) {
                    Icon(Yume.Zashboard, contentDescription = MLang.Proxy.Action.Panel)
                }
            }
        }
    }, actions = {
        if (onTestDelay != null) {
            IconButton(
                modifier = Modifier.padding(end = 16.dp), onClick = { onTestDelay.invoke() }
            ) {
                Icon(Yume.Speed, contentDescription = MLang.Proxy.Action.Test)
            }
        }
        if (showBack) {
            IconButton(
                modifier = Modifier.padding(end = 24.dp), onClick = { onShowSettings() }
            ) {
                Icon(Yume.`List-chevrons-up-down`, contentDescription = MLang.Proxy.Settings.SortMode)
            }
        } else {
            IconButton(
                modifier = Modifier.padding(end = 24.dp), onClick = onShowSettings
            ) {
                Icon(Yume.`List-chevrons-up-down`, contentDescription = MLang.Proxy.Action.Settings)
            }
        }
    })
}

@Composable
private fun NodeListPage(
    group: ProxyGroupInfo?,
    displayMode: ProxyDisplayMode,
    sortMode: ProxySortMode,
    testingGroupNames: Set<String>,
    mainInnerPadding: PaddingValues,
    outerInnerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onSelectProxy: (groupName: String, proxyName: String) -> Unit,
    onTestDelay: (groupName: String) -> Unit,
    onScrollDirectionChanged: (Boolean) -> Unit,
) {
    if (group == null) {
        CenteredText(
            firstLine = MLang.Proxy.Empty.NoNodes,
            secondLine = MLang.Proxy.Empty.Hint,
        )
        return
    }
    val spacing = LocalSpacing.current
    val isTesting = testingGroupNames.contains(group.name)

    val listState = rememberSaveable(
        group.name, saver = LazyListState.Saver
    ) { LazyListState() }
    val listItemKeys = remember(group.proxies) { group.proxies.map { it.name } }

    KeepLazyListTopAnchorOnReorder(
        listState = listState,
        itemKeys = listItemKeys,
        enabled = sortMode == ProxySortMode.BY_LATENCY,
        scrollToTopOnEnabled = true,
    )

    LaunchedEffect(isTesting) {
        if (isTesting && listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    var lastScrollIndex by remember { mutableStateOf(0) }
    var lastScrollOffset by remember { mutableStateOf(0) }
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset
        if (currentIndex != lastScrollIndex) {
            onScrollDirectionChanged(currentIndex > lastScrollIndex)
        } else if (currentOffset != lastScrollOffset) {
            val delta = currentOffset - lastScrollOffset
            if (kotlin.math.abs(delta) > 10) {
                onScrollDirectionChanged(delta > 0)
            }
        }
        lastScrollIndex = currentIndex
        lastScrollOffset = currentOffset
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            start = ProxyPageSpacing.ContentHorizontal,
            end = ProxyPageSpacing.ContentHorizontal,
            top = outerInnerPadding.calculateTopPadding() + ProxyPageSpacing.ContentTop,
            bottom = mainInnerPadding.calculateBottomPadding() + spacing.md,
        ),
        overscrollEffect = null,
    ) {
        item(key = "__refresh_indicator__") {
            AnimatedVisibility(
                visible = isTesting,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = AnimationSpecs.Proxy.RefreshIndicatorDuration),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(durationMillis = AnimationSpecs.Proxy.RefreshIndicatorFadeDuration)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = AnimationSpecs.Proxy.RefreshIndicatorDuration),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween(durationMillis = AnimationSpecs.Proxy.RefreshIndicatorFadeDuration)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InfiniteProgressIndicator(
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = MLang.Proxy.Testing.InProgress,
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        nodeGridItems(
            proxies = group.proxies,
            selectedProxyName = group.now,
            onProxyClick = { proxyName ->
                if (group.type == Proxy.Type.Selector) {
                    onSelectProxy(group.name, proxyName)
                } else {
                    onTestDelay(group.name)
                }
            },
            isDelayTesting = isTesting,
            onDelayTestClick = { onTestDelay(group.name) },
            outerHorizontalPadding = 0.dp,
            itemVerticalPadding = ProxyPageSpacing.ItemVertical,
        )
    }
}

@Composable
private fun ProxyContent(
    proxyGroups: List<ProxyGroupInfo>,
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    mainInnerPadding: PaddingValues,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    testingGroupNames: Set<String>,
    onGroupBoundsChanged: (String, Rect) -> Unit,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            start = ProxyPageSpacing.ContentHorizontal,
            end = ProxyPageSpacing.ContentHorizontal,
            top = innerPadding.calculateTopPadding() + ProxyPageSpacing.ContentTop,
            bottom = mainInnerPadding.calculateBottomPadding() + spacing.md,
        ),
        overscrollEffect = null,
    ) {
        nodeGroupItems(
            groups = proxyGroups,
            onGroupClick = onGroupClick,
            testingGroupNames = testingGroupNames,
            onGroupBoundsChanged = onGroupBoundsChanged,
            itemVerticalPadding = ProxyPageSpacing.ItemVertical,
        )
    }
}

@Composable
private fun ProxySettingsContent(
    proxyViewModel: ProxyViewModel,
    sortMode: ProxySortMode,
    onDismiss: () -> Unit,
) {
    val currentMode by proxyViewModel.currentMode.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val sheetHeightFraction by proxyViewModel.sheetHeightFraction.collectAsState()

    val modeTabs = remember { listOf(MLang.Proxy.Mode.Rule, MLang.Proxy.Mode.Global, MLang.Proxy.Mode.Direct) }
    val modeValues = remember { listOf(TunnelState.Mode.Rule, TunnelState.Mode.Global, TunnelState.Mode.Direct) }
    val sortModes = remember { ProxySortMode.entries }
    val sortTabs = remember { sortModes.map { it.displayName } }
    LaunchedEffect(displayMode) {
        when (displayMode) {
            ProxyDisplayMode.DOUBLE_DETAILED,
            ProxyDisplayMode.DOUBLE_SIMPLE,
            -> proxyViewModel.setDisplayMode(ProxyDisplayMode.SINGLE_DETAILED)

            else -> Unit
        }
    }
    Column {
        Text(
            text = MLang.Proxy.Settings.ProxyMode,
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TabRowWithContour(
            tabs = modeTabs,
            selectedTabIndex = modeValues.indexOf(currentMode).coerceAtLeast(0),
            onTabSelected = { index ->
                if (index < modeValues.size) {
                    proxyViewModel.patchMode(modeValues[index])
                }
            })

        Spacer(Modifier.height(12.dp))

        Text(
            text = MLang.Proxy.Settings.SortMode,
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TabRowWithContour(
            tabs = sortTabs,
            selectedTabIndex = sortModes.indexOf(sortMode).coerceAtLeast(0),
            onTabSelected = { index ->
                if (index < sortModes.size) {
                    proxyViewModel.setSortMode(sortModes[index])
                }
            })

        Spacer(Modifier.height(12.dp))

        Text(
            text = MLang.Proxy.Settings.SheetHeight.format((sheetHeightFraction * 100f).roundToInt()),
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        var sliderValue by remember(sheetHeightFraction) {
            mutableFloatStateOf(fractionToSliderValue(sheetHeightFraction))
        }
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                sliderValue = value
                proxyViewModel.setSheetHeightFraction(sliderValueToFraction(value))
            },
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss, modifier = Modifier.weight(1f)
            ) {
                Text(MLang.Component.Button.Cancel)
            }
            Button(
                onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(MLang.Component.Button.Confirm, color = MiuixTheme.colorScheme.background)
            }
        }
    }
}

