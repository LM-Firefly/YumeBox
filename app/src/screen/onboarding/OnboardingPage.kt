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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.screen.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.List
import com.github.yumelira.yumebox.presentation.icon.yume.Message
import com.github.yumelira.yumebox.presentation.icon.yume.Rocket
import com.github.yumelira.yumebox.presentation.theme.UiDp
import com.github.yumelira.yumebox.screen.settings.component.ThemeColorPickerItem
import com.github.yumelira.yumebox.screen.settings.component.ThemeModeSelectorItem
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun OnboardingSinglePage(
    permissionState: PermissionState,
    privacyAccepted: Boolean,
    onPrivacyAcceptedChange: (Boolean) -> Unit,
    onPrivacySheetRequest: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeSeedColorArgb: Long,
    onShowThemeColorPickerChange: (Boolean) -> Unit,
    onFinish: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingBackdrop()

        Column(
            modifier =
                Modifier.fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = PagePadding, vertical = UiDp.dp12),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = DetailWidth)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UiDp.dp18),
            ) {
                Spacer(modifier = Modifier.height(UiDp.dp56))

                RevealScaleBlock(delayMillis = 0) { OnboardingHeroBadge(icon = Yume.Rocket) }
                RevealBlock(delayMillis = 90) {
                    HeroWordmark(title = "YumeBox", tagline = MLang.Onboarding.Welcome.Tagline)
                }

                Spacer(modifier = Modifier.height(UiDp.dp10))

                RevealBlock(delayMillis = 160, modifier = Modifier.fillMaxWidth()) {
                    TermsContent(
                        accepted = privacyAccepted,
                        onAcceptedChange = onPrivacyAcceptedChange,
                        onPrivacySheetRequest = onPrivacySheetRequest,
                    )
                }

                RevealBlock(delayMillis = 220, modifier = Modifier.fillMaxWidth()) {
                    PermissionContent(permissionState)
                }

                RevealBlock(delayMillis = 280, modifier = Modifier.fillMaxWidth()) {
                    PersonalizeContent(
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        themeSeedColorArgb = themeSeedColorArgb,
                        onShowThemeColorPickerChange = onShowThemeColorPickerChange,
                    )
                }

                Spacer(modifier = Modifier.height(UiDp.dp12))
            }

            RevealBlock(
                delayMillis = 340,
                modifier = Modifier.fillMaxWidth().widthIn(max = DetailWidth).padding(top = UiDp.dp16),
            ) {
                PrimaryFooterAction(
                    text = MLang.Onboarding.Navigation.Enter,
                    enabled = privacyAccepted,
                    onClick = { if (privacyAccepted) onFinish() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun PermissionContent(state: PermissionState) {
    val notificationSummary =
        when {
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
            summary =
                if (state.appListGranted) {
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
    val linkStyle =
        remember(colorScheme.primary) {
            SpanStyle(color = colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    val linkStyles = remember(linkStyle) { TextLinkStyles(style = linkStyle) }
    val annotatedText =
        remember(linkStyles, onPrivacySheetRequest) {
            buildAnnotatedString {
                append(MLang.Onboarding.Privacy.RichTextLead)
                append(" ")
                append(MLang.Onboarding.Privacy.RichTextPrefix)
                withLink(
                    LinkAnnotation.Clickable(
                        tag = LinkTermsTag,
                        styles = linkStyles,
                        linkInteractionListener = { onPrivacySheetRequest() },
                    )
                ) {
                    withStyle(linkStyle) { append(MLang.Onboarding.Privacy.TermsLink) }
                }
                append(MLang.Onboarding.Privacy.RichTextConnector)
                withLink(
                    LinkAnnotation.Clickable(
                        tag = LinkPolicyTag,
                        styles = linkStyles,
                        linkInteractionListener = { onPrivacySheetRequest() },
                    )
                ) {
                    withStyle(linkStyle) { append(MLang.Onboarding.Privacy.PolicyLink) }
                }
                append(MLang.Onboarding.Privacy.RichTextSuffix)
            }
        }

    DetailGroup {
        Column(
            modifier = Modifier.padding(horizontal = UiDp.dp18, vertical = UiDp.dp18),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp14),
        ) {
            Text(
                text = annotatedText,
                style =
                    MiuixTheme.textStyles.body2.copy(
                        color = MiuixTheme.colorScheme.onSurface,
                        lineHeight = 24.sp,
                    ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
            ) {
                Checkbox(
                    state = androidx.compose.ui.state.ToggleableState(accepted),
                    onClick = { onAcceptedChange(!accepted) },
                )
                Text(
                    text = MLang.Onboarding.Privacy.Accept.Title,
                    style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
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
        ThemeModeSelectorItem(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
        DetailDivider()
        ThemeColorPickerItem(
            themeSeedColorArgb = themeSeedColorArgb,
            onThemeSeedColorChange = {},
            showBottomSheetInPlace = false,
            onOpenPickerRequest = { onShowThemeColorPickerChange(true) },
        )
    }
}
