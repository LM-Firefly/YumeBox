package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    onProxyClick: ((Proxy) -> Unit)? = null,
    onProxyDelayClick: ((Proxy) -> Unit)? = null,
    isDelayTesting: Boolean = false,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val columns = if (displayMode.isSingleColumn) 1 else 2

    val rememberedOnProxyClick = remember(onProxyClick) { onProxyClick }
    val rememberedOnProxyDelayClick = remember(onProxyDelayClick) { onProxyDelayClick }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = proxies,
            key = { it.name },
            contentType = { "ProxyNodeCard" }
        ) { proxy ->
            ProxyNodeCard(
                proxy = proxy,
                isSelected = proxy.name == selectedProxyName,
                onClick = rememberedOnProxyClick?.let { { it(proxy) } },
                isPinned = proxy.name == pinnedProxyName,
                isSingleColumn = displayMode.isSingleColumn,
                showDetail = displayMode.showDetail,
                onDelayClick = rememberedOnProxyDelayClick?.let { { it(proxy) } },
                isDelayTesting = isDelayTesting,
            )
        }
    }
}
