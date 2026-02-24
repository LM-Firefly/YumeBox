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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.domain.model.PROXY_SHEET_HEIGHT_FRACTION_MAX
import com.github.yumelira.yumebox.domain.model.PROXY_SHEET_HEIGHT_FRACTION_MIN
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import com.github.yumelira.yumebox.domain.model.normalizeProxySheetHeightFraction
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.LocalTopBarHazeState
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
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
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
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
    val fabVisible = selectedGroupName != null && fabGroup != null
    val isFabTesting = fabGroup?.name?.let(testingGroupNames::contains) == true

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
                visible = fabVisible,
                enter = scaleIn(
                    animationSpec = tween(durationMillis = 260, easing = AnimationSpecs.Legacy),
                    initialScale = 0.72f,
                ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                exit = scaleOut(
                    animationSpec = tween(durationMillis = 140, easing = AnimationSpecs.Legacy),
                    targetScale = 0.72f,
                ) + fadeOut(animationSpec = tween(durationMillis = 100)),
                label = "proxy_test_fab_visibility",
            ) {
                FloatingActionButton(
                    modifier = Modifier.padding(end = 20.dp, bottom = 70.dp),
                    onClick = {
                        val targetGroup = fabGroup ?: return@FloatingActionButton
                        if (!isFabTesting) proxyViewModel.testDelay(targetGroup.name)
                    },
                ) {
                    ProxyTestingRefreshIcon(isRotating = isFabTesting)
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
                    NodeListPage(
                        group = proxyGroupsByName[targetGroupName] ?: displayGroup,
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
                    )
                }
            }
        }

        // Settings bottom sheet
        WindowBottomSheet(
            show = showSettingsBottomSheet,
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
private fun ProxyTestingRefreshIcon(isRotating: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "proxy_test_fab_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
        ),
        label = "proxy_test_fab_rotation_value",
    )

    if (isRotating) {
        Icon(
            imageVector = MiuixIcons.Refresh,
            contentDescription = MLang.Proxy.Action.Test,
            tint = MiuixTheme.colorScheme.background,
            modifier = Modifier.rotate(rotation),
        )
    } else {
        Icon(
            imageVector = Yume.Speed,
            contentDescription = MLang.Proxy.Action.Test,
            tint = MiuixTheme.colorScheme.background,
        )
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
) {
    if (group == null) {
        CenteredText(
            firstLine = MLang.Proxy.Empty.NoNodes,
            secondLine = MLang.Proxy.Empty.Hint,
        )
        return
    }
    val spacing = LocalSpacing.current

    val listState = rememberSaveable(
        group.name, saver = LazyListState.Saver
    ) { LazyListState() }
    val listItemKeys = remember(group.proxies) { group.proxies.map { it.name } }
    KeepLazyListTopAnchorOnReorder(
        listState = listState,
        itemKeys = listItemKeys,
        enabled = sortMode == ProxySortMode.BY_LATENCY,
    )

    ScreenLazyColumn(
        scrollBehavior = scrollBehavior,
        innerPadding = PaddingValues(top = outerInnerPadding.calculateTopPadding()),
        topPadding = ProxyPageSpacing.ContentTop,
        bottomPadding = mainInnerPadding.calculateBottomPadding() + spacing.md,
        lazyListState = listState,
        enableGlobalScroll = false,
    ) {
        nodeGridItems(
            proxies = group.proxies,
            selectedProxyName = group.now,
            displayMode = displayMode,
            onProxyClick = { proxyName ->
                if (group.type == Proxy.Type.Selector) {
                    onSelectProxy(group.name, proxyName)
                } else {
                    onTestDelay(group.name)
                }
            },
            isDelayTesting = testingGroupNames.contains(group.name),
            onDelayTestClick = { onTestDelay(group.name) },
            outerHorizontalPadding = ProxyPageSpacing.ContentHorizontal,
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

