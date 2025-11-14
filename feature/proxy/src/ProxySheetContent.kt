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

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetAction
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetIconAction
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import com.github.yumelira.yumebox.presentation.icon.yume.Speed
import com.github.yumelira.yumebox.presentation.screen.rememberProxyGroupSelectionState
import com.github.yumelira.yumebox.presentation.screen.node.NodeGroupSheetContent
import com.github.yumelira.yumebox.presentation.screen.node.NodeSheetContent
import com.github.yumelira.yumebox.presentation.screen.node.NodeSortPopup
import com.github.yumelira.yumebox.presentation.theme.AnimationSpecs
import com.github.yumelira.yumebox.presentation.viewmodel.ProxyViewModel
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val NOTIFICATION_PROXY_SHEET_HEIGHT_FRACTION = 0.55f

private fun LazyListState.isScrolledFromTop(): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0
}

@Composable
fun ProxySheetContent(
    onDismiss: () -> Unit,
    proxyViewModel: ProxyViewModel = koinViewModel(),
) {
    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val testingGroupNames by proxyViewModel.testingGroupNames.collectAsState()
    val testingProxyNames by proxyViewModel.testingProxyNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()

    val showSheet = remember { mutableStateOf(true) }
    val showSortPopup = remember { mutableStateOf(false) }
    val groupSelection = rememberProxyGroupSelectionState(
        proxyGroups = proxyGroups,
        onRefreshGroup = proxyViewModel::refreshGroup,
        retainLastKnownGroup = false,
    )
    val selectedGroupName = groupSelection.selectedGroupName
    val selectedGroup = groupSelection.selectedGroup
    val coroutineScope = rememberCoroutineScope()
    val groupListState = rememberLazyListState()
    val nodeListState = rememberSaveable(selectedGroupName, saver = LazyListState.Saver) {
        LazyListState()
    }

    DisposableEffect(Unit) {
        proxyViewModel.ensureCoreLoaded(true, source = "proxy_sheet")
        onDispose {
            proxyViewModel.ensureCoreLoaded(false, source = "proxy_sheet")
        }
    }

    val dismissSheet = remember(onDismiss) {
        {
            showSortPopup.value = false
            showSheet.value = false
        }
    }
    LaunchedEffect(showSheet.value) {
        if (!showSheet.value) {
            onDismiss()
        }
    }

    WindowBottomSheet(
        show = showSheet.value,
        title = selectedGroup?.name ?: MLang.Proxy.Title,
        backgroundColor = MiuixTheme.colorScheme.surface,
        startAction = {
            if (selectedGroup != null) {
                AppBottomSheetIconAction(
                    action = AppBottomSheetAction(
                        icon = MiuixIcons.Back,
                        contentDescription = MLang.Component.Navigation.Back,
                        onClick = groupSelection.clearSelection,
                    ),
                )
            } else {
                Box {
                    AppBottomSheetIconAction(
                        action = AppBottomSheetAction(
                            icon = Yume.`List-chevrons-up-down`,
                            contentDescription = MLang.Proxy.Action.Sort,
                            onClick = { showSortPopup.value = true },
                        ),
                    )
                    NodeSortPopup(
                        show = showSortPopup,
                        onDismiss = { showSortPopup.value = false },
                        sortMode = sortMode,
                        alignment = PopupPositionProvider.Align.BottomStart,
                        onSortSelected = proxyViewModel::setSortMode,
                    )
                }
            }
        },
        endAction = {
            AppBottomSheetIconAction(
                action = AppBottomSheetAction(
                    icon = Yume.Speed,
                    contentDescription = MLang.Proxy.Action.Test,
                    onClick = {
                        val group = selectedGroup
                        coroutineScope.launch {
                            if (group == null) {
                                if (groupListState.isScrolledFromTop()) {
                                    groupListState.animateScrollToItem(0)
                                }
                                proxyViewModel.testDelay()
                            } else {
                                if (nodeListState.isScrolledFromTop()) {
                                    nodeListState.animateScrollToItem(0)
                                }
                                proxyViewModel.testDelay(group.name)
                            }
                        }
                    },
                ),
            )
        },
        onDismissRequest = {
            dismissSheet()
        },
        enableWindowDim = true,
        insideMargin = DpSize(16.dp, 16.dp),
        enableNestedScroll = false
    ) {
        AnimatedContent(
            targetState = selectedGroupName,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = AnimationSpecs.Proxy.SheetSlideInDuration,
                            easing = AnimationSpecs.Legacy
                        ),
                        initialOffsetX = { it },
                    ) + fadeIn(animationSpec = tween(durationMillis = AnimationSpecs.Proxy.SheetFadeInDuration))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(
                                    durationMillis = AnimationSpecs.Proxy.SheetSlideOutDuration,
                                    easing = AnimationSpecs.Legacy
                                ),
                                targetOffsetX = { -it / 3 },
                            ) + fadeOut(animationSpec = tween(durationMillis = AnimationSpecs.Proxy.SheetFadeOutDuration)))
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = AnimationSpecs.Proxy.SheetSlideOutDuration,
                            easing = AnimationSpecs.Legacy
                        ),
                        initialOffsetX = { -it / 3 },
                    ) + fadeIn(animationSpec = tween(durationMillis = AnimationSpecs.Proxy.SheetFadeInDuration - 20))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(
                                    durationMillis = AnimationSpecs.Proxy.SheetSlideInDuration - 20,
                                    easing = AnimationSpecs.Legacy
                                ),
                                targetOffsetX = { it },
                            ) + fadeOut(animationSpec = tween(durationMillis = AnimationSpecs.Proxy.SheetFadeOutDuration)))
                }
            },
            label = "notification_node_sheet_content",
        ) { targetGroupName ->
            val group = targetGroupName?.let { name -> proxyGroups.find { it.name == name } }
            if (group == null) {
                NodeGroupSheetContent(
                    groups = proxyGroups,
                    onGroupClick = groupSelection.selectGroup,
                    testingGroupNames = testingGroupNames,
                    sheetHeightFraction = NOTIFICATION_PROXY_SHEET_HEIGHT_FRACTION,
                    listState = groupListState,
                )
            } else {
                NodeSheetContent(
                    group = group,
                    displayMode = displayMode,
                    isDelayTesting = testingGroupNames.contains(group.name),
                    testingProxyNames = testingProxyNames,
                    onSelectProxy = { proxyName ->
                        if (group.type == Proxy.Type.Selector) {
                            proxyViewModel.selectProxy(group.name, proxyName)
                        } else {
                            coroutineScope.launch {
                                if (nodeListState.isScrolledFromTop()) {
                                    nodeListState.animateScrollToItem(0)
                                }
                                proxyViewModel.testDelay(group.name)
                            }
                        }
                    },
                    onTestDelay = {
                        coroutineScope.launch {
                            if (nodeListState.isScrolledFromTop()) {
                                nodeListState.animateScrollToItem(0)
                            }
                            proxyViewModel.testDelay(group.name)
                        }
                    },
                    onTestProxyDelay = { proxyName ->
                        proxyViewModel.testProxyDelay(group.name, proxyName)
                    },
                    sheetHeightFraction = NOTIFICATION_PROXY_SHEET_HEIGHT_FRACTION,
                    listState = nodeListState,
                )
            }
        }
    }
}
