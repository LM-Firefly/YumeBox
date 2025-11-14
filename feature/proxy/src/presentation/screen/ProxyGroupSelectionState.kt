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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo

data class ProxyGroupSelectionState(
    val selectedGroupName: String?,
    val selectedGroup: ProxyGroupInfo?,
    val displayGroup: ProxyGroupInfo?,
    val selectGroup: (ProxyGroupInfo) -> Unit,
    val clearSelection: () -> Unit,
)

@Composable
fun rememberProxyGroupSelectionState(
    proxyGroups: List<ProxyGroupInfo>,
    onRefreshGroup: (String) -> Unit,
    retainLastKnownGroup: Boolean,
): ProxyGroupSelectionState {
    val selectedGroupNameState = rememberSaveable { mutableStateOf<String?>(null) }
    val selectedGroupSnapshotState = remember { mutableStateOf<ProxyGroupInfo?>(null) }
    val groupsByName = remember(proxyGroups) { proxyGroups.associateBy { it.name } }
    val selectedGroup by remember(selectedGroupNameState.value, groupsByName) {
        derivedStateOf {
            selectedGroupNameState.value?.let(groupsByName::get)
        }
    }

    LaunchedEffect(selectedGroup, retainLastKnownGroup) {
        if (retainLastKnownGroup) {
            selectedGroup?.let { selectedGroupSnapshotState.value = it }
        }
    }

    LaunchedEffect(proxyGroups, selectedGroupNameState.value, retainLastKnownGroup) {
        if (!retainLastKnownGroup && selectedGroupNameState.value != null && selectedGroup == null) {
            selectedGroupNameState.value = null
        }
    }

    LaunchedEffect(selectedGroupNameState.value) {
        selectedGroupNameState.value?.let(onRefreshGroup)
    }

    return remember(
        selectedGroupNameState.value,
        selectedGroup,
        selectedGroupSnapshotState.value,
        retainLastKnownGroup,
    ) {
        ProxyGroupSelectionState(
            selectedGroupName = selectedGroupNameState.value,
            selectedGroup = selectedGroup,
            displayGroup = selectedGroup ?: selectedGroupSnapshotState.value.takeIf { retainLastKnownGroup },
            selectGroup = { group -> selectedGroupNameState.value = group.name },
            clearSelection = { selectedGroupNameState.value = null },
        )
    }
}
