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

package com.github.yumelira.yumebox.presentation.component

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode

@Composable
fun ProxyNodeGrid(
    proxies: List<Proxy>,
    selectedProxyName: String,
    displayMode: ProxyDisplayMode,
    onProxyClick: ((String) -> Unit)? = null,
    isDelayTesting: Boolean = false,
    onDelayTestClick: (() -> Unit)? = null,
    listStateKey: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val showDetail = displayMode.showDetail
    val isSingleColumn = displayMode.isSingleColumn
    val listState = rememberSaveable(listStateKey, isSingleColumn, saver = LazyListState.Saver) {
        LazyListState()
    }

    if (isSingleColumn) {
        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            overscrollEffect = null,
        ) {
            items(
                items = proxies,
                key = { it.name },
                contentType = { "ProxyNodeCard1" },
            ) { proxy ->
                ProxyNodeCard(
                    proxy = proxy,
                    isSelected = proxy.name == selectedProxyName,
                    onClick = onProxyClick,
                    isSingleColumn = true,
                    showDetail = showDetail,
                    isDelayTesting = isDelayTesting,
                    onDelayTestClick = onDelayTestClick,
                    showCountryFlag = true,
                )
            }
        }
        return
    }

    val rowCount = remember(proxies) { (proxies.size + 1) / 2 }
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        overscrollEffect = null,
    ) {
        items(
            count = rowCount,
            key = { rowIndex -> proxies[rowIndex * 2].name },
            contentType = { "ProxyRow2" },
        ) { rowIndex ->
            val left = proxies[rowIndex * 2]
            val right = proxies.getOrNull(rowIndex * 2 + 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProxyNodeCard(
                    proxy = left,
                    isSelected = left.name == selectedProxyName,
                    onClick = onProxyClick,
                    isSingleColumn = false,
                    showDetail = showDetail,
                    isDelayTesting = isDelayTesting,
                    onDelayTestClick = onDelayTestClick,
                    showCountryFlag = true,
                    modifier = Modifier.weight(1f),
                )

                if (right != null) {
                    ProxyNodeCard(
                        proxy = right,
                        isSelected = right.name == selectedProxyName,
                        onClick = onProxyClick,
                        isSingleColumn = false,
                        showDetail = showDetail,
                        isDelayTesting = isDelayTesting,
                        onDelayTestClick = onDelayTestClick,
                        showCountryFlag = true,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
