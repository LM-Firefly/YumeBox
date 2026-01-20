package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode

@Composable
fun ProxyNodeGrid(
    proxies: List<Proxy>,
    selectedProxyName: String,
    pinnedProxyName: String,
    displayMode: ProxyDisplayMode,
    onProxyClick: (Proxy) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val columns = if (displayMode.isSingleColumn) 1 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 12.dp + contentPadding.calculateTopPadding(),
            bottom = 12.dp + contentPadding.calculateBottomPadding()
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(proxies) { proxy ->
            ProxyNodeCard(
                proxy = proxy,
                isSelected = proxy.name == selectedProxyName,
                onClick = { onProxyClick(proxy) },
                isPinned = proxy.name == pinnedProxyName,
                isSingleColumn = displayMode.isSingleColumn,
                showDetail = displayMode.showDetail
            )
        }
    }
}

