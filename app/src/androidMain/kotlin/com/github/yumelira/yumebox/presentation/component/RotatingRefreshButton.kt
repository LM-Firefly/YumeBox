package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RotatingRefreshButton(
    isRotating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = MLang.Component.Navigation.Refresh,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    IconButton(
        modifier = modifier,
        onClick = onClick,
        enabled = !isRotating
    ) {
        Icon(
            imageVector = MiuixIcons.Useful.Refresh,
            contentDescription = contentDescription,
            modifier = if (isRotating) Modifier.rotate(rotation) else Modifier,
            tint = if (isRotating) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantActions
            }
        )
    }
}
