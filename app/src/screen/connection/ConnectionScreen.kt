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



package com.github.yumelira.yumebox.screen.connection

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.ConnectionInfo
import com.github.yumelira.yumebox.feature.meta.presentation.component.ConnectionCard
import com.github.yumelira.yumebox.feature.meta.presentation.component.ConnectionDetailSheet
import com.github.yumelira.yumebox.feature.meta.presentation.component.TabRowWithContour
import com.github.yumelira.yumebox.feature.meta.presentation.viewmodel.ConnectionSort
import com.github.yumelira.yumebox.feature.meta.presentation.viewmodel.ConnectionTab
import com.github.yumelira.yumebox.feature.meta.presentation.viewmodel.ConnectionViewModel
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.theme.MiuixTheme

private object ConnectionPageSpacing {
    val ContentTop = 20.dp
    val ContentHorizontal = 12.dp
    val ItemVertical = 6.dp
}

private val SortModes = listOf(
    ConnectionSort.Time,
    ConnectionSort.Upload,
    ConnectionSort.Download,
    ConnectionSort.Host,
)

private fun ConnectionSort.getDisplayName(): String = when (this) {
    ConnectionSort.Time -> MLang.Connection.Sort.Time
    ConnectionSort.Upload -> MLang.Connection.Sort.Upload
    ConnectionSort.Download -> MLang.Connection.Sort.Download
    ConnectionSort.Host -> MLang.Connection.Sort.Host
}

@Destination<RootGraph>
@Composable
fun ConnectionScreen(
    navigator: DestinationsNavigator,
) {
    val viewModel = koinViewModel<ConnectionViewModel>()
    val state by viewModel.state.collectAsState()
    val filteredConnections by viewModel.filteredConnections.collectAsState()

    val scrollBehavior = MiuixScrollBehavior()
    var showSearchBar by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(state.searchQuery) }
    var showSortPopup by remember { mutableStateOf(false) }

    var selectedConnection by remember { mutableStateOf<ConnectionInfo?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }

    val tabs = listOf(MLang.Connection.Tab.Active, MLang.Connection.Tab.Closed)
    var selectedTabIndex by remember { mutableStateOf(0) }

    LaunchedEffect(selectedTabIndex) {
        val tab = if (selectedTabIndex == 0) ConnectionTab.ACTIVE else ConnectionTab.CLOSED
        viewModel.setTab(tab)
    }

    LaunchedEffect(searchText) {
        if (searchText != state.searchQuery) {
            viewModel.setSearchQuery(searchText)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Connection.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    Box {
                        IconButton(
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = { showSortPopup = true }) {
                            Icon(
                                imageVector = MiuixIcons.Sort,
                                contentDescription = "Sort",
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        val selectedSortIndex = SortModes.indexOf(state.sortBy).coerceAtLeast(0)
                        SuperListPopup(
                            show = showSortPopup,
                            alignment = PopupPositionProvider.Align.BottomEnd,
                            onDismissRequest = { showSortPopup = false },
                        ) {
                            ListPopupColumn {
                                SortModes.forEachIndexed { index, mode ->
                                    DropdownImpl(
                                        text = mode.getDisplayName(),
                                        optionSize = SortModes.size,
                                        isSelected = selectedSortIndex == index,
                                        onSelectedIndexChange = {
                                            if (mode != state.sortBy) viewModel.setSortBy(mode)
                                            showSortPopup = false
                                        },
                                        index = index,
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        modifier = Modifier.padding(end = 24.dp),
                        onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            contentDescription = "Search",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
            contentPadding = PaddingValues(
                start = ConnectionPageSpacing.ContentHorizontal,
                end = ConnectionPageSpacing.ContentHorizontal,
                top = innerPadding.calculateTopPadding() + ConnectionPageSpacing.ContentTop,
                bottom = innerPadding.calculateBottomPadding(),
            ),
        ) {

            item {
                TabRowWithContour(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                )
            }

            item {
                AnimatedVisibility(
                    visible = showSearchBar,
                    enter = expandVertically(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Top,
                    ) + fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
                    exit = shrinkVertically(
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Top,
                    ) + fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.weight(1f),
                            label = MLang.Connection.SearchHint,
                            singleLine = true,
                        )
                    }
                }
            }

            if (filteredConnections.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (state.isLoading) {
                                MLang.Connection.Loading
                            } else if (state.searchQuery.isNotEmpty()) {
                                MLang.Connection.NoResults
                            } else {
                                MLang.Connection.Empty
                            },
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            } else {

                items(
                    items = filteredConnections,
                    key = { it.id },
                ) { connection ->
                    ConnectionCard(
                        connectionInfo = connection,
                        onClick = {
                            selectedConnection = connection
                            showDetailSheet = true
                        },
                        modifier = Modifier.padding(vertical = ConnectionPageSpacing.ItemVertical),
                    )
                }
            }
        }

        ConnectionDetailSheet(
            show = showDetailSheet,
            connectionInfo = selectedConnection,
            onDismiss = { showDetailSheet = false },
            onDismissFinished = { selectedConnection = null },
        )
    }
}
