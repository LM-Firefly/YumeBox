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

package com.github.yumelira.yumebox.presentation.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import com.github.yumelira.yumebox.common.util.AppIconHelper
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.theme.AppColorTheme
import com.github.yumelira.yumebox.presentation.theme.colorFromArgb
import com.github.yumelira.yumebox.presentation.theme.colorToArgbLong
import com.github.yumelira.yumebox.presentation.theme.DEFAULT_THEME_SEED_ARGB
import com.github.yumelira.yumebox.presentation.theme.isDefaultThemeSeedArgb
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun AppSettingsScreen(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AppSettingsViewModel>()


    val themeMode = viewModel.themeMode.state.collectAsState().value
    val colorTheme = viewModel.colorTheme.state.collectAsState().value
    val themeSeedColorArgb = viewModel.themeSeedColorArgb.state.collectAsState().value

    val automaticRestart = viewModel.automaticRestart.state.collectAsState().value
    val hideAppIcon = viewModel.hideAppIcon.state.collectAsState().value
    val showTrafficNotification = viewModel.showTrafficNotification.state.collectAsState().value
    val bottomBarFloating = viewModel.bottomBarFloating.state.collectAsState().value
    val showDivider = viewModel.showDivider.state.collectAsState().value
    val bottomBarAutoHide = viewModel.bottomBarAutoHide.state.collectAsState().value

    val oneWord = viewModel.oneWord.state.collectAsState().value
    val oneWordAuthor = viewModel.oneWordAuthor.state.collectAsState().value
    val customUserAgent = viewModel.customUserAgent.state.collectAsState().value
    val logLevel = viewModel.logLevel.state.collectAsState().value

    val showHideIconDialog = remember { mutableStateOf(false) }
    val showEditOneWordDialog = remember { mutableStateOf(false) }
    val showEditOneWordAuthorDialog = remember { mutableStateOf(false) }
    val showEditCustomUserAgentDialog = remember { mutableStateOf(false) }
    val showThemeColorPicker = remember { mutableStateOf(false) }

    val oneWordTextFieldState = remember { mutableStateOf(TextFieldValue(oneWord)) }
    val oneWordAuthorTextFieldState = remember { mutableStateOf(TextFieldValue(oneWordAuthor)) }
    val customUserAgentTextFieldState = remember { mutableStateOf(TextFieldValue(customUserAgent)) }
    val editingThemeSeedColor = remember(themeSeedColorArgb) {
        mutableStateOf(runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White))
    }
    val editingThemeSeedHex = remember(themeSeedColorArgb) {
        mutableStateOf(formatThemeSeedHex(themeSeedColorArgb))
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.AppSettings.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    NavigationBackIcon(navigator = navigator)
                }
            )
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            item {
                SmallTitle(MLang.AppSettings.Section.Behavior)
                Card {
                    SuperSwitch(
                        title = MLang.AppSettings.Behavior.AutoStartTitle,
                        summary = MLang.AppSettings.Behavior.AutoStartSummary,
                        checked = automaticRestart,
                        onCheckedChange = { viewModel.onAutomaticRestartChange(it) },
                    )
                }
                SmallTitle(MLang.AppSettings.Section.Home)
                Card {
                    BasicComponent(
                        title = MLang.AppSettings.Home.OneWordTitle,
                        summary = viewModel.oneWord.value,
                        onClick = {
                            oneWordTextFieldState.value = TextFieldValue(viewModel.oneWord.value)
                            showEditOneWordDialog.value = true
                        }
                    )
                    BasicComponent(
                        title = MLang.AppSettings.Home.OneWordAuthorTitle,
                        summary = viewModel.oneWordAuthor.value,
                        onClick = {
                            oneWordAuthorTextFieldState.value = TextFieldValue(viewModel.oneWordAuthor.value)
                            showEditOneWordAuthorDialog.value = true
                        }
                    )
                }
                SmallTitle(MLang.AppSettings.Section.Interface)
                Card {
                    EnumSelector(
                        title = MLang.AppSettings.Interface.ThemeModeTitle,
                        summary = MLang.AppSettings.Interface.ThemeModeSummary,
                        currentValue = themeMode,
                        items = listOf(
                            MLang.AppSettings.Interface.ThemeModeSystem,
                            MLang.AppSettings.Interface.ThemeModeLight,
                            MLang.AppSettings.Interface.ThemeModeDark
                        ),
                        values = ThemeMode.entries,
                        onValueChange = { viewModel.onThemeModeChange(it) },
                    )
                    EnumSelector(
                        title = MLang.AppSettings.Interface.ColorThemeTitle,
                        summary = MLang.AppSettings.Interface.ColorThemeSummary,
                        currentValue = colorTheme,
                        items = listOf(
                            MLang.AppSettings.Interface.ColorMinimal,
                            MLang.AppSettings.Interface.ColorClassic,
                            MLang.AppSettings.Interface.ColorOcean,
                            MLang.AppSettings.Interface.ColorFresh,
                            MLang.AppSettings.Interface.ColorPrincess,
                            MLang.AppSettings.Interface.ColorMystery,
                            MLang.AppSettings.Interface.ColorGolden,
                        ),
                        values = AppColorTheme.entries,
                        onValueChange = { viewModel.onColorThemeChange(it) },
                    )
                    BasicComponent(
                        title = MLang.AppSettings.Interface.ColorThemePickerTitle,
                        summary = if (isDefaultThemeSeedArgb(themeSeedColorArgb)) {
                            MLang.AppSettings.Interface.ColorThemeDefaultSummary
                        } else {
                            MLang.AppSettings.Interface.ColorThemeCustomSummary.format(
                                formatThemeSeedHex(themeSeedColorArgb)
                            )
                        },
                        onClick = {
                            editingThemeSeedColor.value = runCatching { colorFromArgb(themeSeedColorArgb) }
                                .getOrDefault(Color.White)
                            editingThemeSeedHex.value = formatThemeSeedHex(themeSeedColorArgb)
                            showThemeColorPicker.value = true
                        },
                        endActions = {
                            val previewColor = remember(themeSeedColorArgb) {
                                runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White)
                            }
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(26.dp)
                                    .background(
                                        color = previewColor,
                                        shape = RoundedCornerShape(50),
                                    )
                            )
                        },
                    )
                    SuperSwitch(
                        title = MLang.AppSettings.Interface.FloatingNavbarTitle,
                        summary = MLang.AppSettings.Interface.FloatingNavbarSummary,
                        checked = bottomBarFloating,
                        onCheckedChange = { viewModel.onBottomBarFloatingChange(it) },
                    )
                    SuperSwitch(
                        title = MLang.AppSettings.Interface.AutoHideNavbarTitle,
                        summary = MLang.AppSettings.Interface.AutoHideNavbarSummary,
                        checked = bottomBarAutoHide,
                        onCheckedChange = { viewModel.onBottomBarAutoHideChange(it) },
                    )
                    SuperSwitch(
                        title = MLang.AppSettings.Interface.ShowDividerTitle,
                        summary = MLang.AppSettings.Interface.ShowDividerSummary,
                        checked = showDivider,
                        onCheckedChange = { viewModel.onShowDividerChange(it) },
                    )
                    SuperSwitch(
                        title = MLang.AppSettings.Interface.HideIconTitle,
                        summary = MLang.AppSettings.Interface.HideIconSummary,
                        checked = hideAppIcon,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showHideIconDialog.value = true
                            } else {
                                viewModel.onHideAppIconChange(false)
                                AppIconHelper.toggleIcon(context, false)
                            }
                        },
                    )
                }
                SmallTitle(MLang.AppSettings.Section.Service)
                Card {
                    SuperSwitch(
                        title = MLang.AppSettings.ServiceSection.TrafficNotificationTitle,
                        summary = MLang.AppSettings.ServiceSection.TrafficNotificationSummary,
                        checked = showTrafficNotification,
                        onCheckedChange = { viewModel.onShowTrafficNotificationChange(it) },
                    )
                    EnumSelector(
                        title = MLang.AppSettings.ServiceSection.LogLevelTitle,
                        summary = MLang.AppSettings.ServiceSection.LogLevelSummary,
                        currentValue = logLevel,
                        items = listOf("VERBOSE", "DEBUG", "INFO", "WARN", "ERROR", "ASSERT"),
                        values = listOf(Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR, Log.ASSERT),
                        onValueChange = { viewModel.onLogLevelChange(it) },
                    )
                }
                SmallTitle(MLang.AppSettings.Section.Network)
                Card {
                    BasicComponent(
                        title = MLang.AppSettings.Network.CustomUserAgentTitle,
                        summary = if (customUserAgent.isEmpty()) {
                            MLang.AppSettings.Network.CustomUserAgentSummaryDefault
                        } else {
                            customUserAgent
                        },
                        onClick = {
                            customUserAgentTextFieldState.value = TextFieldValue(customUserAgent)
                            showEditCustomUserAgentDialog.value = true
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    WarningBottomSheet(
        show = showHideIconDialog,
        title = MLang.AppSettings.WarningDialog.Title,
        messages = listOf(
            MLang.AppSettings.WarningDialog.HideIconMsg1,
            MLang.AppSettings.WarningDialog.HideIconMsg2
        ),
        onConfirm = {
            viewModel.onHideAppIconChange(true)
            AppIconHelper.toggleIcon(context, true)
        },
    )

    TextEditBottomSheet(
        show = showEditOneWordDialog,
        title = MLang.AppSettings.EditDialog.OneWordTitle,
        textFieldValue = oneWordTextFieldState,
        onConfirm = { viewModel.onOneWordChange(it) },
    )

    TextEditBottomSheet(
        show = showEditOneWordAuthorDialog,
        title = MLang.AppSettings.EditDialog.AuthorTitle,
        textFieldValue = oneWordAuthorTextFieldState,
        onConfirm = { viewModel.onOneWordAuthorChange(it) },
    )

    TextEditBottomSheet(
        show = showEditCustomUserAgentDialog,
        title = MLang.AppSettings.EditDialog.UserAgentTitle,
        textFieldValue = customUserAgentTextFieldState,
        onConfirm = {
            viewModel.onCustomUserAgentChange(it)
            viewModel.applyCustomUserAgent(it)
        },
    )

    WindowBottomSheet(
        show = showThemeColorPicker,
        title = MLang.AppSettings.Interface.ColorThemePickerTitle,
        onDismissRequest = { showThemeColorPicker.value = false },
        insideMargin = DpSize(24.dp, 16.dp),
    ) {
        ColorPicker(
            color = editingThemeSeedColor.value,
            onColorChanged = {
                editingThemeSeedColor.value = it
                editingThemeSeedHex.value = formatThemeSeedHex(colorToArgbLong(it))
            },
            modifier = Modifier.fillMaxWidth(),
        )
        TextField(
            value = editingThemeSeedHex.value,
            onValueChange = { raw ->
                val normalized = normalizeThemeHexInput(raw)
                editingThemeSeedHex.value = normalized
                parseThemeHexColorOrNull(normalized)?.let {
                    editingThemeSeedColor.value = it
                }
            },
            label = MLang.AppSettings.Interface.ColorThemeCodeLabel,
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    viewModel.resetThemeSeedColor()
                    editingThemeSeedColor.value = Color.White
                    editingThemeSeedHex.value = formatThemeSeedHex(DEFAULT_THEME_SEED_ARGB)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(MLang.AppSettings.Interface.ColorThemeResetDefault)
            }
            Button(
                onClick = {
                    val argb = colorToArgbLong(editingThemeSeedColor.value)
                    if (isDefaultThemeSeedArgb(argb)) {
                        viewModel.resetThemeSeedColor()
                    } else {
                        viewModel.onThemeSeedColorChange(argb)
                    }
                    showThemeColorPicker.value = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(MLang.Component.Button.Confirm, color = MiuixTheme.colorScheme.background)
            }
        }
    }
}

private fun formatThemeSeedHex(argb: Long): String {
    val rgb = (argb and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')
    return "#$rgb"
}

private fun normalizeThemeHexInput(input: String): String {
    val body = input
        .uppercase()
        .filter { it in '0'..'9' || it in 'A'..'F' }
        .take(6)
    return "#$body"
}

private fun parseThemeHexColorOrNull(input: String): Color? {
    val body = input.removePrefix("#")
    if (body.length != 6) return null
    val rgb = body.toLongOrNull(16) ?: return null
    return colorFromArgb(0xFF000000L or rgb)
}
