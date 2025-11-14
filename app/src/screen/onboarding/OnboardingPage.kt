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



package com.github.yumelira.yumebox.screen.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.screen.settings.component.ThemeColorPickerItem
import com.github.yumelira.yumebox.screen.settings.component.ThemeModeSelectorItem
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun StartupHeroShell(
    enabled: Boolean,
    onStart: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DetailBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = PagePadding, vertical = 12.dp),
        ) {
            Spacer(modifier = Modifier.height(212.dp))

            RevealBlock(
                delayMillis = 0,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                StartupTypewriterWord(
                    phrases = StartupTypewriterPhrases,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = DetailWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            RevealScaleBlock(
                delayMillis = 680,
                modifier = Modifier
                    .widthIn(max = DetailWidth)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .offset(y = (-156).dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    HeroStartButton(
                        enabled = enabled,
                        onStart = onStart,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProvisionDetailShell(
    previewIcon: ImageVector,
    title: String,
    subtitle: String,
    primaryText: String,
    primaryEnabled: Boolean,
    onPrimaryClick: () -> Unit,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DetailBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = PagePadding, vertical = 12.dp),
        ) {
            Spacer(modifier = Modifier.height(88.dp))

            RevealBlock(
                delayMillis = 0,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                DetailPreviewBadge(icon = previewIcon)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = DetailWidth)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RevealBlock(delayMillis = 50) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.title2.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MiuixTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
                RevealBlock(delayMillis = 110) {
                    Text(
                        text = subtitle,
                        style = MiuixTheme.textStyles.body2.copy(lineHeight = 22.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = DetailWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    RevealBlock(delayMillis = 160) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            content = content,
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            RevealBlock(
                delayMillis = 220,
                modifier = Modifier
                    .widthIn(max = DetailWidth)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .offset(y = (-36).dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SecondaryFooterAction(
                        text = MLang.Onboarding.Navigation.Back,
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryFooterAction(
                        text = primaryText,
                        enabled = primaryEnabled,
                        onClick = onPrimaryClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PermissionContent(state: PermissionState) {
    val notificationSummary = when {
        state.notificationGranted -> MLang.Onboarding.Permission.Common.Granted
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ->
            MLang.Onboarding.Permission.Notification.SummaryNeed
        else -> MLang.Onboarding.Permission.Notification.SummaryNotRequired
    }

    DetailGroup {
        PermissionRow(
            icon = Yume.Message,
            title = MLang.Onboarding.Permission.Notification.Title,
            summary = notificationSummary,
            granted = state.notificationGranted,
            onClick = {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    state.onRequestNotification()
                    return@PermissionRow
                }
                if (!state.notificationGranted) {
                    state.onRequestNotification()
                }
            },
        )
        DetailDivider()
        PermissionRow(
            icon = Yume.List,
            title = MLang.Onboarding.Permission.AppList.Title,
            summary = if (state.appListGranted) {
                MLang.Onboarding.Permission.Common.Granted
            } else {
                MLang.Onboarding.Permission.AppList.SummaryNeed
            },
            granted = state.appListGranted,
            onClick = {
                if (!state.appListGranted) {
                    state.onRequestAppList()
                }
            },
        )
    }
}

@Composable
internal fun TermsContent(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onPrivacySheetRequest: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val linkStyle = remember(colorScheme.primary) {
        SpanStyle(
            color = colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
    val linkStyles = remember(linkStyle) { TextLinkStyles(style = linkStyle) }
    val annotatedText = remember(linkStyles, onPrivacySheetRequest) {
        buildAnnotatedString {
            append(MLang.Onboarding.Privacy.RichTextLead)
            append(" ")
            append(MLang.Onboarding.Privacy.RichTextPrefix)
            withLink(
                LinkAnnotation.Clickable(
                    tag = LinkTermsTag,
                    styles = linkStyles,
                    linkInteractionListener = { onPrivacySheetRequest() }
                )
            ) {
                withStyle(linkStyle) {
                    append(MLang.Onboarding.Privacy.TermsLink)
                }
            }
            append(MLang.Onboarding.Privacy.RichTextConnector)
            withLink(
                LinkAnnotation.Clickable(
                    tag = LinkPolicyTag,
                    styles = linkStyles,
                    linkInteractionListener = { onPrivacySheetRequest() }
                )
            ) {
                withStyle(linkStyle) {
                    append(MLang.Onboarding.Privacy.PolicyLink)
                }
            }
            append(MLang.Onboarding.Privacy.RichTextSuffix)
        }
    }

    DetailGroup {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = annotatedText,
                style = MiuixTheme.textStyles.body2.copy(
                    color = MiuixTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(
                    state = androidx.compose.ui.state.ToggleableState(accepted),
                    onClick = { onAcceptedChange(!accepted) },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = MLang.Onboarding.Privacy.Accept.Title,
                        style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PersonalizeContent(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeSeedColorArgb: Long,
    onShowThemeColorPickerChange: (Boolean) -> Unit,
) {
    DetailGroup {
        ThemeModeSelectorItem(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
        )
        DetailDivider()
        ThemeColorPickerItem(
            themeSeedColorArgb = themeSeedColorArgb,
            onThemeSeedColorChange = {},
            showBottomSheetInPlace = false,
            onOpenPickerRequest = { onShowThemeColorPickerChange(true) },
        )
    }
}

@Composable
internal fun FinishHeroShell(
    enabled: Boolean,
    onPrimaryClick: () -> Unit,
    onGithubClick: () -> Unit,
    onCommunityClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DetailBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = PagePadding, vertical = 12.dp),
        ) {
            Spacer(modifier = Modifier.height(88.dp))

            RevealBlock(
                delayMillis = 0,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                DetailPreviewBadge(icon = Yume.CircleCheckBig)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = DetailWidth)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RevealBlock(delayMillis = 50) {
                    Text(
                        text = MLang.Onboarding.Finish.Title,
                        style = MiuixTheme.textStyles.title2.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MiuixTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
                RevealBlock(delayMillis = 110) {
                    Text(
                        text = MLang.Onboarding.Finish.Subtitle,
                        style = MiuixTheme.textStyles.body2.copy(lineHeight = 22.sp),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = DetailWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    RevealBlock(delayMillis = 160) {
                        DetailGroup {
                            ProjectLinkRow(
                                icon = Yume.Github,
                                title = MLang.Onboarding.Project.Github.Title,
                                summary = MLang.Onboarding.Project.Github.Summary,
                                onClick = onGithubClick,
                            )
                            DetailDivider()
                            ProjectLinkRow(
                                icon = Yume.Message,
                                title = MLang.Onboarding.Project.Community.Title,
                                summary = MLang.Onboarding.Project.Community.Summary,
                                onClick = onCommunityClick,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            RevealBlock(
                delayMillis = 220,
                modifier = Modifier
                    .widthIn(max = DetailWidth)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .offset(y = (-36).dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PrimaryFooterAction(
                        text = MLang.Onboarding.Navigation.Enter,
                        enabled = enabled,
                        onClick = onPrimaryClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
