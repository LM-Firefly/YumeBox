package com.github.yumelira.yumebox.presentation.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton

internal val OverrideTopBarActionSpacing = 12.dp

@Composable
internal fun RowScope.OverrideTopBarAction(
    icon: ImageVector,
    contentDescription: String,
    spacedFromNext: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = if (spacedFromNext) Modifier.padding(end = OverrideTopBarActionSpacing) else Modifier,
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}
