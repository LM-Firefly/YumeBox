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



package com.github.yumelira.yumebox.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.util.AppIconHelper
import com.github.yumelira.yumebox.common.util.LocaleUtil
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.screen.settings.component.ThemeColorPickerItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.text.toString

@Composable
@Destination<RootGraph>
fun AppSettingsScreen(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AppSettingsViewModel>()

    val themeMode = viewModel.themeMode.state.collectAsState().value
    val themeSeedColorArgb = viewModel.themeSeedColorArgb.state.collectAsState().value

    val automaticRestart = viewModel.automaticRestart.state.collectAsState().value
    val autoUpdateCurrentProfileOnStart =
        viewModel.autoUpdateCurrentProfileOnStart.state.collectAsState().value
    val hideAppIcon = viewModel.hideAppIcon.state.collectAsState().value
    val excludeFromRecents = viewModel.excludeFromRecents.state.collectAsState().value
    val showTrafficNotification = viewModel.showTrafficNotification.state.collectAsState().value
    val bottomBarAutoHide = viewModel.bottomBarAutoHide.state.collectAsState().value
    val topBarBlurEnabled = viewModel.topBarBlurEnabled.state.collectAsState().value
    val bottomBarLiquidGlassEnabled = viewModel.bottomBarLiquidGlassEnabled.state.collectAsState().value
    val pageScaleState = viewModel.pageScale.state.collectAsState().value
    var pageScaleLocal by remember(pageScaleState) { mutableFloatStateOf(pageScaleState) }

    val customUserAgent = viewModel.customUserAgent.state.collectAsState().value

    val showHideIconDialog = remember { mutableStateOf(false) }
    val showEditCustomUserAgentDialog = remember { mutableStateOf(false) }
    val showPageScaleSheet = remember { mutableStateOf(false) }

    val customUserAgentTextFieldState = remember { mutableStateOf(TextFieldValue(customUserAgent)) }

    Scaffold(
        topBar = {
            TopBar(title = MLang.AppSettings.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                        SuperSwitch(
                            title = MLang.AppSettings.Behavior.AutoUpdateOnStartTitle,
                            summary = MLang.AppSettings.Behavior.AutoUpdateOnStartSummary,
                            checked = autoUpdateCurrentProfileOnStart,
                            onCheckedChange = { viewModel.onAutoUpdateCurrentProfileOnStartChange(it) },
                        )
                        if (LocaleUtil.isChineseLocale()) {
                            SuperSwitch(
                                title = MLang.AppSettings.Behavior.OneChinaTitle,
                                summary = MLang.AppSettings.Behavior.OneChinaSummary,
                                checked = true,
                                onCheckedChange = { },
                                enabled = false,
                            )
                        }
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
                        ThemeColorPickerItem(
                            themeSeedColorArgb = themeSeedColorArgb,
                            onThemeSeedColorChange = { viewModel.onThemeSeedColorChange(it) },
                        )
                        SuperSwitch(
                            title = MLang.AppSettings.Interface.AutoHideNavbarTitle,
                            summary = MLang.AppSettings.Interface.AutoHideNavbarSummary,
                            checked = bottomBarAutoHide,
                            onCheckedChange = { viewModel.onBottomBarAutoHideChange(it) },
                        )
                        SuperSwitch(
                            title = MLang.AppSettings.Interface.TopBarBlurTitle,
                            summary = MLang.AppSettings.Interface.TopBarBlurSummary,
                            checked = topBarBlurEnabled,
                            onCheckedChange = { viewModel.onTopBarBlurEnabledChange(it) },
                        )
                        SuperSwitch(
                            title = MLang.AppSettings.Interface.BottomBarLiquidGlassTitle,
                            summary = MLang.AppSettings.Interface.BottomBarLiquidGlassSummary,
                            checked = bottomBarLiquidGlassEnabled,
                            onCheckedChange = { viewModel.onBottomBarLiquidGlassEnabledChange(it) },
                        )
                        SuperArrow(
                            title = MLang.AppSettings.Interface.PageScaleTitle,
                            summary = MLang.AppSettings.Interface.PageScaleSummary,
                            endActions = {
                                Text(
                                    text = "${(pageScaleLocal * 100).toInt()}%",
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            },
                            onClick = { showPageScaleSheet.value = !showPageScaleSheet.value },
                            holdDownState = showPageScaleSheet.value,
                            bottomAction = {
                                Slider(
                                    value = pageScaleLocal,
                                    onValueChange = { pageScaleLocal = it },
                                    onValueChangeFinished = { viewModel.onPageScaleChange(pageScaleLocal) },
                                    valueRange = 0.8f..1.2f,
                                    magnetThreshold = 0.01f,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                )
                            },
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
                        SuperSwitch(
                            title = MLang.AppSettings.Interface.HideFromRecentsTitle,
                            summary = MLang.AppSettings.Interface.HideFromRecentsSummary,
                            checked = excludeFromRecents,
                            onCheckedChange = { viewModel.onExcludeFromRecentsChange(it) },
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
                        SuperSwitch(
                            title = MLang.AppSettings.ServiceSection.SingleNodeTestTitle,
                            summary = MLang.AppSettings.ServiceSection.SingleNodeTestSummary,
                            checked = viewModel.singleNodeTest.state.collectAsState().value,
                            onCheckedChange = { viewModel.onSingleNodeTestChange(it) },
                        )
                    }
                    SmallTitle(MLang.AppSettings.Section.Network)
                    Card {
                        BasicComponent(
                            title = MLang.AppSettings.Network.CustomUserAgentTitle,
                            summary = customUserAgent.ifEmpty {
                                MLang.AppSettings.Network.CustomUserAgentSummaryDefault
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
                show = showEditCustomUserAgentDialog,
                title = MLang.AppSettings.EditDialog.UserAgentTitle,
                textFieldValue = customUserAgentTextFieldState,
                onConfirm = { viewModel.applyCustomUserAgent(it) },
            )

            AppDialog(
                show = showPageScaleSheet.value,
                modifier = Modifier,
                title = MLang.AppSettings.Interface.PageScaleTitle,
                summary = "80% - 120%",
                onDismissRequest = { showPageScaleSheet.value = false },
                renderInRootScaffold = true,
                content = {
                    var scaleText by remember(showPageScaleSheet.value) {
                        mutableStateOf((pageScaleLocal * 100).toInt().toString())
                    }
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        value = scaleText,
                        maxLines = 1,
                        trailingIcon = {
                            Text(
                                text = "%",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onValueChange = { v ->
                            if (v.isEmpty() || v.all { it.isDigit() }) scaleText = v
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        TextButton(
                            text = MLang.AppSettings.Button.Cancel,
                            onClick = { showPageScaleSheet.value = false },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = MLang.AppSettings.Button.Apply,
                            onClick = {
                                val parsed = scaleText.toFloatOrNull()
                                val clamped = (parsed?.coerceIn(80f, 120f) ?: (pageScaleLocal * 100)) / 100f
                                pageScaleLocal = clamped
                                viewModel.onPageScaleChange(clamped)
                                showPageScaleSheet.value = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                })
        }
    }
}
