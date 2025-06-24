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

package com.github.yumelira.yumebox.presentation.util

import androidx.compose.runtime.*

/**
 * Shared pull-to-refresh logic for proxy/testing group refresh.
 * Returns Triple(pullRefreshing, setPullRefreshing, pullRefreshObservedTesting, setPullRefreshObservedTesting)
 */
@Composable
fun usePullToRefreshTesting(
    testingGroupNames: Set<String>,
    onReset: (() -> Unit)? = null
): Pair<MutableState<Boolean>, MutableState<Boolean>> {
    val pullRefreshing = remember { mutableStateOf(false) }
    val pullRefreshObservedTesting = remember { mutableStateOf(false) }

    LaunchedEffect(pullRefreshing.value, testingGroupNames) {
        if (!pullRefreshing.value) return@LaunchedEffect
        val isTesting = testingGroupNames.isNotEmpty()
        if (!pullRefreshObservedTesting.value) {
            if (isTesting) {
                pullRefreshObservedTesting.value = true
            }
            return@LaunchedEffect
        }
        if (!isTesting) {
            pullRefreshing.value = false
            pullRefreshObservedTesting.value = false
            onReset?.invoke()
        }
    }
    return pullRefreshing to pullRefreshObservedTesting
}
