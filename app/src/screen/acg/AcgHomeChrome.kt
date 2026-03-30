package com.github.yumelira.yumebox.screen.acg

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.github.yumelira.yumebox.common.util.formatBytesForDisplay
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.presentation.component.CountryFlagCircle
import com.github.yumelira.yumebox.presentation.icon.ShellIcons
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Waiting
import com.github.yumelira.yumebox.presentation.util.extractFlaggedName
import com.github.yumelira.yumebox.screen.home.HomeProxyControlState
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AcgSidebarDecoration(
    backdrop: LayerBackdrop,
    blurEnabled: Boolean,
    blurProgress: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val surface = MiuixTheme.colorScheme.surface
    val isDarkSurface = surface.luminance() < 0.5f
    val glassBase = if (isDarkSurface) {
        Color.Black.copy(alpha = 0.24f)
    } else {
        surface.copy(alpha = 0.13f)
    }
    val glassTint = Color.Black.copy(alpha = 0.10f)
    val glassGradientStart = if (isDarkSurface) {
        Color.Black.copy(alpha = 0.36f)
    } else {
        surface.copy(alpha = 0.23f)
    }
    val glassGradientEnd = if (isDarkSurface) {
        surface.copy(alpha = 0.18f)
    } else {
        surface.copy(alpha = 0.16f)
    }
    val clampedBlurProgress = blurProgress.coerceIn(0f, 1f)
    val blurRadiusPx = lerpFloat(30f, 52f, clampedBlurProgress)
    val blurColors = BlurDefaults.blurColors(
        blendColors = listOf(
            BlendColorEntry(color = glassBase, mode = BlurBlendMode.SrcOver),
            BlendColorEntry(color = glassTint, mode = BlurBlendMode.SrcOver),
        ),
        saturation = if (isDarkSurface) 1.06f else 1.02f,
        contrast = if (isDarkSurface) 1.08f else 1.10f,
        brightness = if (isDarkSurface) 0.00f else -0.05f,
    )
    val blurModifier = if (blurEnabled) {
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = blurRadiusPx,
            noiseCoefficient = 0f,
            colors = blurColors,
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(blurModifier)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(glassGradientStart, glassGradientEnd),
                ),
                shape = RectangleShape,
            )
            .padding(horizontal = AcgUi.Sidebar.innerHorizontalPadding, vertical = 24.dp),
        content = content,
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun AcgSidebarContent(
    topValue: String,
    bottomValue: String,
    proxyMode: ProxyMode,
    icons: List<AcgSidebarIconItem>,
    visibleWidth: Dp,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val visibleContentWidth =
            (visibleWidth - AcgUi.Sidebar.innerHorizontalPadding).coerceAtLeast(0.dp)
        val visibleCenterLine = visibleContentWidth / 2
        val centerLine = visibleCenterLine + AcgUi.Sidebar.visibleOpticalOffset
        val laneStart = centerLine - (AcgUi.Sidebar.statsWidth / 2)

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(AcgUi.Sidebar.statsWidth)
                .offset(x = laneStart),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(AcgUi.Sidebar.topInset))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AcgUi.Sidebar.timeGap),
            ) {
                AcgSidebarTimeValue(topValue)
                Box(
                    modifier = Modifier
                        .width(AcgUi.Sidebar.dividerWidth)
                        .height(AcgUi.Sidebar.dividerHeight)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = AcgUi.Sidebar.dividerAlpha)),
                )
                AcgSidebarTimeValue(bottomValue)
            }

            AcgSidebarModeText(
                mode = proxyMode.toAcgDisplayName(),
                modifier = Modifier.padding(top = AcgUi.Sidebar.modeTopGap),
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = AcgUi.Sidebar.iconPillAlpha))
                    .padding(
                        horizontal = AcgUi.Sidebar.iconPillHorizontalPadding,
                        vertical = AcgUi.Sidebar.iconPillVerticalPadding,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AcgUi.Sidebar.iconSpacing),
            ) {
                icons.forEach { item ->
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = AcgUi.Sidebar.iconAlpha),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = item.onClick,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(AcgUi.Sidebar.bottomInset))
        }
    }
}

@Composable
private fun AcgSidebarModeText(
    mode: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = mode.uppercase(),
        modifier = modifier,
        style = MiuixTheme.textStyles.footnote1.copy(
            fontSize = AcgUi.Sidebar.modeFontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
        ),
        color = Color.White.copy(alpha = 0.96f),
    )
}

@Composable
internal fun AcgQuoteText(
    quote: AcgQuote,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AcgUi.Quote.contentGap),
    ) {
        Text(
            text = quote.text,
            color = color,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.Medium,
            fontSize = AcgUi.Quote.textSize,
            lineHeight = 31.sp,
            softWrap = true,
            overflow = TextOverflow.Clip,
        )
        Text(
            text = "— ${quote.author}",
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = AcgUi.Quote.authorTopGap),
            color = color.copy(alpha = AcgUi.Quote.authorAlpha),
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.Medium,
            fontSize = AcgUi.Quote.authorSize,
            softWrap = false,
        )
    }
}

@Composable
internal fun AcgLaunchButton(
    controlState: HomeProxyControlState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isRunning = controlState == HomeProxyControlState.Running
    val background = if (isRunning) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onBackground
    }
    val contentColor = if (isRunning) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.background
    }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) AcgUi.Button.pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.42f,
            stiffness = 520f,
        ),
        label = "acg_launch_button_scale",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .width(AcgUi.Button.fixedWidth)
            .clip(AcgUi.Shape.launchButton)
            .background(background, AcgUi.Shape.launchButton)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = AcgUi.Button.horizontalPadding,
                vertical = AcgUi.Button.verticalPadding,
            ),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = when (controlState) {
                    HomeProxyControlState.Idle -> ShellIcons.StartProxy
                    HomeProxyControlState.Running -> ShellIcons.StopProxy
                    HomeProxyControlState.Connecting,
                    HomeProxyControlState.Disconnecting,
                        -> Yume.Waiting
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = when (controlState) {
                    HomeProxyControlState.Idle -> MLang.Home.Control.Start
                    HomeProxyControlState.Connecting -> MLang.Home.Status.Connecting
                    HomeProxyControlState.Running -> MLang.Home.Control.Stop
                    HomeProxyControlState.Disconnecting -> MLang.Home.Status.Disconnecting
                },
                color = contentColor,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun AcgSidebarTimeValue(value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AcgUi.Sidebar.timeValueHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value,
            color = Color.White.copy(alpha = AcgUi.Sidebar.timeAlpha),
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.SemiBold,
            fontSize = 37.sp,
            letterSpacing = AcgUi.Sidebar.digitLetterSpacing,
            softWrap = false,
        )
    }
}

@Composable
internal fun AcgTrafficStrip(
    downloadSpeed: Long,
    uploadSpeed: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AcgUi.Hero.trafficRowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            AcgTrafficItem(
                label = MLang.Home.Traffic.UpShort,
                speed = uploadSpeed,
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            AcgTrafficItem(
                label = MLang.Home.Traffic.DownShort,
                speed = downloadSpeed,
            )
        }
    }
}

@Composable
private fun AcgTrafficItem(
    label: String,
    speed: Long,
) {
    val (value, unit) = formatBytesForDisplay(speed)
    val onSurface = MiuixTheme.colorScheme.onSurface
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = label,
            color = onSurface.copy(alpha = 0.62f),
            style = MiuixTheme.textStyles.footnote1,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 3.dp),
        )
        Text(
            text = value,
            color = onSurface,
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
        )
        Text(
            text = unit,
            color = onSurface.copy(alpha = 0.55f),
            style = MiuixTheme.textStyles.footnote1,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
}

@Composable
internal fun AcgHomeInfoPanel(
    serverName: String?,
    serverPing: Int?,
    modifier: Modifier = Modifier,
) {
    val flaggedNode = remember(serverName) { serverName?.let(::extractFlaggedName) }
    val resolvedNodeName = flaggedNode?.displayName ?: serverName.orEmpty().ifBlank { "" }
    val resolvedPing = serverPing
        ?.takeIf { it in 1..1000 }
        ?.let { ping -> MLang.Home.NodeInfo.DelayValue.format(ping) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AcgUi.Hero.infoRowMinHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (resolvedNodeName.isNotBlank()) {
            AcgInfoBlock(
                value = resolvedNodeName,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                leading = {
                    flaggedNode?.countryCode?.let { countryCode ->
                        CountryFlagCircle(countryCode = countryCode, size = 16.dp)
                    }
                },
            )
        } else {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
            )
        }

        if (resolvedPing != null) {
            AcgInfoBlock(
                value = resolvedPing,
                modifier = Modifier.width(AcgUi.Hero.delayWidth),
                valueColor = when {
                    serverPing < 500 -> Color(0xFF0E7A34)
                    else -> Color(0xFFB87900)
                },
                alignEnd = true,
            )
        }
    }
}

@Composable
private fun AcgInfoBlock(
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    valueFontFamily: FontFamily? = null,
    alignEnd: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = if (alignEnd) Alignment.End else Alignment.Start,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(
            text = value,
            color = valueColor,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            fontFamily = valueFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = if (alignEnd) Modifier else Modifier.weight(1f),
        )
    }
}
