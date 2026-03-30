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

package com.github.yumelira.yumebox.presentation.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun SearchStatus.TopAppBarAnim(
    modifier: Modifier = Modifier,
    visible: Boolean = shouldCollapsed(),
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 550 else 0, easing = FastOutSlowInEasing),
        label = "SearchTopBarAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surface),
    ) {
        Box(
            modifier = Modifier.graphicsLayer { this.alpha = alpha },
        ) {
            content()
        }
    }
}

@Composable
fun SearchStatus.SearchBox(
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp = 12.dp,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    collapseBar: @Composable (SearchStatus, Dp, PaddingValues) -> Unit = { searchStatus, topPadding, innerPadding ->
        SearchBarCollapsed(
            label = searchStatus.label,
            searchBarTopPadding = topPadding,
            startPadding = startPadding,
            endPadding = endPadding,
            innerPadding = innerPadding,
        )
    },
    content: @Composable (Dp) -> Unit,
) {
    val searchStatus = this
    val density = LocalDensity.current
    val offsetY = remember { mutableIntStateOf(0) }
    val boxHeight = remember { mutableStateOf(0.dp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f)
            .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
            .offset(y = contentPadding.calculateTopPadding())
            .onGloballyPositioned { coordinates ->
                coordinates.positionInWindow().y.apply {
                    offsetY.intValue = (this * 0.9f).toInt()
                    with(density) {
                        val newOffsetY = this@apply.toDp()
                        val newBoxHeight = coordinates.size.height.toDp()
                        if (searchStatus.offsetY != newOffsetY) {
                            onSearchStatusChange(searchStatus.copy(offsetY = newOffsetY))
                        }
                        boxHeight.value = newBoxHeight
                    }
                }
            }
            .pointerInput(searchStatus.current) {
                detectTapGestures {
                    onSearchStatusChange(searchStatus.copy(current = SearchStatus.Status.EXPANDING))
                }
            }
            .background(colorScheme.surface),
    ) {
        collapseBar(searchStatus, searchBarTopPadding, contentPadding)
    }

    Box {
        AnimatedVisibility(
            visible = searchStatus.shouldCollapsed(),
            enter = fadeIn(tween(300, easing = LinearOutSlowInEasing)) +
                slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { -offsetY.intValue },
            exit = fadeOut(tween(300, easing = LinearOutSlowInEasing)) +
                slideOutVertically(tween(300, easing = LinearOutSlowInEasing)) { -offsetY.intValue },
        ) {
            content(boxHeight.value)
        }
    }
}

@Composable
fun SearchStatus.SearchPager(
    onSearchStatusChange: (SearchStatus) -> Unit,
    defaultResult: @Composable () -> Unit = {},
    emptyResult: @Composable () -> Unit = {},
    searchBarTopPadding: Dp = 12.dp,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
    result: @Composable () -> Unit,
) {
    val searchStatus = this
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val topPadding by animateDpAsState(
        targetValue = if (searchStatus.shouldExpand()) {
            systemBarsPadding + 5.dp
        } else {
            max(searchStatus.offsetY, 0.dp)
        },
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "SearchTopPadding",
        finishedListener = {
            onSearchStatusChange(searchStatus.onAnimationComplete())
        },
    )
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (searchStatus.shouldExpand()) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "SearchSurfaceAlpha",
    )

    BackHandler(enabled = !searchStatus.isCollapsed()) {
        onSearchStatusChange(
            searchStatus.copy(
                searchText = "",
                current = SearchStatus.Status.COLLAPSING,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f)
            .background(colorScheme.surface.copy(alpha = surfaceAlpha))
            .then(
                if (!searchStatus.isCollapsed()) {
                    Modifier.pointerInput(searchStatus.current) {}
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .then(
                    if (!searchStatus.isCollapsed()) {
                        Modifier.background(colorScheme.surface)
                    } else {
                        Modifier
                    },
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!searchStatus.isCollapsed()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(colorScheme.surface),
                ) {
                    SearchBar(
                        searchStatus = searchStatus,
                        onSearchStatusChange = onSearchStatusChange,
                        searchBarTopPadding = searchBarTopPadding,
                        startPadding = startPadding,
                        endPadding = endPadding,
                    )
                }
            }

            AnimatedVisibility(
                visible = searchStatus.isExpand() || searchStatus.isAnimatingExpand(),
                enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
                exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it }),
            ) {
                Text(
                    text = MLang.Component.Button.Cancel,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 16.dp, top = searchBarTopPadding, bottom = 6.dp)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            enabled = searchStatus.isExpand(),
                        ) {
                            onSearchStatusChange(
                                searchStatus.copy(
                                    searchText = "",
                                    current = SearchStatus.Status.COLLAPSING,
                                ),
                            )
                        },
                )
            }
        }

        AnimatedVisibility(
            visible = searchStatus.isExpand(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            when (searchStatus.resultStatus) {
                SearchStatus.ResultStatus.DEFAULT -> defaultResult()
                SearchStatus.ResultStatus.EMPTY -> emptyResult()
                SearchStatus.ResultStatus.SHOW -> result()
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp = 12.dp,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(searchStatus.searchText)) }

    LaunchedEffect(searchStatus.searchText) {
        if (textFieldValue.text != searchStatus.searchText) {
            textFieldValue = TextFieldValue(searchStatus.searchText)
        }
    }

    LaunchedEffect(searchStatus.current) {
        if (searchStatus.isAnimatingExpand()) {
            focusRequester.requestFocus()
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onSearchStatusChange(searchStatus.copy(searchText = it.text))
        },
        singleLine = true,
        textStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            color = colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = endPadding)
            .padding(top = searchBarTopPadding, bottom = 6.dp)
            .heightIn(min = 45.dp)
            .background(colorScheme.secondaryContainer, CircleShape)
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MiuixIcons.Basic.Search,
                    contentDescription = MLang.Component.Editor.Action.Search,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(start = 16.dp, end = 8.dp),
                    tint = colorScheme.onSurfaceVariantSummary,
                )
                Box(modifier = Modifier.weight(1f)) {
                    innerTextField()
                }
                AnimatedVisibility(
                    visible = searchStatus.searchText.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Basic.SearchCleanup,
                        contentDescription = MLang.Component.Button.Clear,
                        tint = colorScheme.onSurface,
                        modifier = Modifier
                            .size(44.dp)
                            .padding(start = 8.dp, end = 16.dp)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                            ) {
                                textFieldValue = TextFieldValue("")
                                onSearchStatusChange(searchStatus.copy(searchText = ""))
                            },
                    )
                }
            }
        },
    )
}

@Composable
private fun SearchBarCollapsed(
    label: String,
    searchBarTopPadding: Dp = 12.dp,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val layoutDirection = LocalLayoutDirection.current
    InputField(
        query = "",
        onQueryChange = {},
        label = label,
        leadingIcon = {
            Icon(
                imageVector = MiuixIcons.Basic.Search,
                contentDescription = MLang.Component.Editor.Action.Search,
                modifier = Modifier
                    .size(44.dp)
                    .padding(start = 16.dp, end = 8.dp),
                tint = colorScheme.onSurfaceVariantSummary,
            )
        },
        modifier = Modifier
            .background(colorScheme.surface)
            .fillMaxWidth()
            .padding(start = startPadding, end = endPadding)
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
            )
            .padding(top = searchBarTopPadding, bottom = 6.dp),
        onSearch = {},
        enabled = false,
        expanded = false,
        onExpandedChange = {},
    )
}
