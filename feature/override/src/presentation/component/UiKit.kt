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

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.chevron
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

val OverrideSectionSpacing = 12.dp
val OverrideSectionTitleSpacing = 8.dp
val OverrideSectionBottomSpacing = 32.dp
private val OverrideFieldAssistIndent = 14.dp
private val OverrideFieldAssistVerticalPadding = 8.dp
private val OverrideActionButtonSize = 35.dp
private val OverrideActionIconSize = 18.dp
private val OverrideStatusBadgeSize = 32.dp
private val OverrideStatusBadgeIconSize = 18.dp

enum class OverrideActionTone {
    Neutral,
    Primary,
    Danger,
}

@Composable
fun OverrideSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OverrideSectionTitleSpacing),
        content = {
            Title(title)
            content()
        },
    )
}

@Composable
fun OverrideCardSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OverrideSection(
        title = title,
        modifier = modifier,
    ) {
        OverrideSelectorCard(content = content)
    }
}

@Composable
fun OverrideFormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OverrideSection(
        title = title,
        modifier = modifier,
    ) {
        OverrideSelectorCard(content = content)
    }
}

@Composable
fun OverridePlainFormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OverrideSection(
        title = title,
        modifier = modifier,
    ) {
        OverrideFormFieldColumn(content = content)
    }
}

@Composable
fun OverrideFormFieldColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
    }
}

@Composable
fun OverrideFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportText: String? = null,
    errorText: String? = null,
    maxLines: Int = 1,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (maxLines > 1) {
                        Modifier.heightIn(min = 0.dp)
                    } else {
                        Modifier
                    },
                ),
            maxLines = maxLines,
        )
        supportText?.takeIf(String::isNotBlank)?.let { helper ->
            OverrideFieldAssistText(
                text = helper,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        errorText?.takeIf(String::isNotBlank)?.let { message ->
            OverrideFieldAssistText(
                text = message,
                color = MiuixTheme.colorScheme.error,
            )
        }
    }
}

@Composable
fun OverrideFieldAssistText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.body2,
        color = color,
        modifier = modifier.padding(
            start = OverrideFieldAssistIndent,
            top = OverrideFieldAssistVerticalPadding,
            bottom = OverrideFieldAssistVerticalPadding,
        ),
    )
}

@Composable
fun OverrideSelectorCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(0.dp),
        content = content,
    )
}

@Composable
fun OverrideSectionCardHeader(
    title: String,
    summary: String? = null,
    expanded: Boolean,
    onClick: () -> Unit,
    showIndicator: Boolean = true,
) {
    val indicatorRotation = animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "override_section_indicator_rotation",
    )

    BasicComponent(
        title = title,
        summary = summary.orEmpty(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        endActions = {
            if (showIndicator) {
                Icon(
                    imageVector = Yume.chevron,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.rotate(indicatorRotation.value),
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
fun OverrideSectionVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(durationMillis = 260),
            expandFrom = Alignment.Top,
        ) + fadeIn(animationSpec = tween(durationMillis = 180)),
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 220),
            shrinkTowards = Alignment.Top,
        ) + fadeOut(animationSpec = tween(durationMillis = 120)),
        label = "override_section_visibility",
    ) {
        content()
    }
}

@Composable
fun OverrideAdvancedCard(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OverrideSelectorCard(modifier = modifier) {
        OverrideSectionCardHeader(
            title = title,
            summary = summary,
            expanded = expanded,
            onClick = { onExpandedChange(!expanded) },
        )
        OverrideSectionVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun OverrideCardActionIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: OverrideActionTone = OverrideActionTone.Neutral,
    enabled: Boolean = true,
) {
    val colorScheme = MiuixTheme.colorScheme
    val (backgroundColor, iconTint) = when (tone) {
        OverrideActionTone.Neutral -> {
            colorScheme.secondaryContainer.copy(alpha = 0.78f) to
                colorScheme.onSurface.copy(alpha = if (enabled) 0.85f else 0.45f)
        }

        OverrideActionTone.Primary -> {
            colorScheme.primary.copy(alpha = 0.1f) to
                colorScheme.primary.copy(alpha = if (enabled) 1f else 0.45f)
        }

        OverrideActionTone.Danger -> {
            colorScheme.error.copy(alpha = 0.1f) to
                colorScheme.error.copy(alpha = if (enabled) 1f else 0.45f)
        }
    }

    IconButton(
        modifier = modifier,
        backgroundColor = backgroundColor,
        minHeight = OverrideActionButtonSize,
        minWidth = OverrideActionButtonSize,
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(OverrideActionIconSize),
        )
    }
}

@Composable
fun OverrideStatusBadge(
    imageVector: ImageVector,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
) {
    Box(
        modifier = modifier
            .size(OverrideStatusBadgeSize)
            .background(
                color = backgroundColor,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(OverrideStatusBadgeIconSize),
        )
    }
}
