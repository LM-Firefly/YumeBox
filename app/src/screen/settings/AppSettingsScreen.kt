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

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.common.util.AppIconHelper
import com.github.yumelira.yumebox.common.util.BiometricHelper
import com.github.yumelira.yumebox.common.util.LocaleUtil
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.screen.settings.component.ThemeColorPickerItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AcgWallpaperCropScreenDestination
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
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun AppSettingsScreen(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AppSettingsViewModel>()

    val themeMode by viewModel.themeMode.state.collectAsState()
    val appLanguage by viewModel.appLanguage.state.collectAsState()
    val themeSeedColorArgb by viewModel.themeSeedColorArgb.state.collectAsState()

    val automaticRestart by viewModel.automaticRestart.state.collectAsState()
    val autoUpdateCurrentProfileOnStart by viewModel.autoUpdateCurrentProfileOnStart.state.collectAsState()
    val hideAppIcon by viewModel.hideAppIcon.state.collectAsState()
    val excludeFromRecents by viewModel.excludeFromRecents.state.collectAsState()
    val showTrafficNotification by viewModel.showTrafficNotification.state.collectAsState()
    val bottomBarAutoHide by viewModel.bottomBarAutoHide.state.collectAsState()
    val topBarBlurEnabled by viewModel.topBarBlurEnabled.state.collectAsState()
    val acgMainUiEnabled by viewModel.acgMainUiEnabled.state.collectAsState()
    val acgWallpaperZoom by viewModel.acgWallpaperZoom.state.collectAsState()
    val acgWallpaperBiasX by viewModel.acgWallpaperBiasX.state.collectAsState()
    val acgWallpaperBiasY by viewModel.acgWallpaperBiasY.state.collectAsState()
    val acgHomeQuote by viewModel.acgHomeQuote.state.collectAsState()
    val acgHomeQuoteAuthor by viewModel.acgHomeQuoteAuthor.state.collectAsState()
    val acgSidebarExpanded by viewModel.acgSidebarExpanded.state.collectAsState()
    val pageScaleState by viewModel.pageScale.state.collectAsState()
    val singleNodeTest by viewModel.singleNodeTest.state.collectAsState()
    val screenshotProtectionEnabled by viewModel.screenshotProtectionEnabled.state.collectAsState()
    val biometricUnlockEnabled by viewModel.biometricUnlockEnabled.state.collectAsState()
    val exitUiWhenBackground by viewModel.exitUiWhenBackground.state.collectAsState()
    var pageScaleLocal by remember(pageScaleState) { mutableFloatStateOf(pageScaleState) }

    val customUserAgent by viewModel.customUserAgent.state.collectAsState()
    val acgQuoteSummary = acgHomeQuote.ifBlank { MLang.AppSettings.Experimental.AcgQuoteDefault }
    val acgQuoteAuthorSummary = acgHomeQuoteAuthor.ifBlank { MLang.AppSettings.Experimental.AcgQuoteAuthorDefault }
    var batteryOptimizationIgnored by remember {
        mutableStateOf(isBatteryOptimizationIgnored(context))
    }

    val showHideIconDialogState = remember { mutableStateOf(false) }
    val showEditCustomUserAgentDialogState = remember { mutableStateOf(false) }
    val showEditAcgQuoteDialogState = remember { mutableStateOf(false) }
    val showEditAcgQuoteAuthorDialogState = remember { mutableStateOf(false) }
    val showPageScaleDialogState = remember { mutableStateOf(false) }
    val showBiometricUnavailableDialogState = remember { mutableStateOf(false) }
    var biometricUnavailableMessage by remember { mutableStateOf("") }

    val customUserAgentTextFieldState = remember { mutableStateOf(TextFieldValue()) }
    val acgQuoteTextFieldState = remember { mutableStateOf(TextFieldValue()) }
    val acgQuoteAuthorTextFieldState = remember { mutableStateOf(TextFieldValue()) }
    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            navigator.navigate(
                AcgWallpaperCropScreenDestination(
                    wallpaperUri = uri.toString(),
                    initialZoom = acgWallpaperZoom,
                    initialBiasX = acgWallpaperBiasX,
                    initialBiasY = acgWallpaperBiasY,
                )
            ) {
                launchSingleTop = true
            }
        }
    }

    fun refreshBatteryOptimizationState() {
        batteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
    }

    fun requestBiometricConfirmation(
        title: String,
        allowBypassWhenUnavailable: Boolean = false,
        onSuccess: () -> Unit,
    ) {
        val activity = BiometricHelper.findFragmentActivity(context)
        if (activity == null) {
            if (allowBypassWhenUnavailable) {
                onSuccess()
            } else {
                biometricUnavailableMessage = MLang.AppSettings.Privacy.BiometricUnavailableMessage
                showBiometricUnavailableDialogState.value = true
            }
            return
        }

        val canAuthenticate = BiometricHelper.canAuthenticate(activity)
        if (!canAuthenticate) {
            if (allowBypassWhenUnavailable) {
                onSuccess()
                return
            }
            biometricUnavailableMessage = BiometricHelper.getAuthenticationStatusMessage(activity)
            showBiometricUnavailableDialogState.value = true
            return
        }

        BiometricHelper.authenticate(
            activity = activity,
            title = title,
            onSuccess = onSuccess,
        )
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshBatteryOptimizationState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = MLang.AppSettings.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title(MLang.AppSettings.Section.Behavior)
                Card {
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.Behavior.AutoStartTitle,
                        summary = MLang.AppSettings.Behavior.AutoStartSummary,
                        checked = automaticRestart,
                        onCheckedChange = { viewModel.onAutomaticRestartChange(it) },
                    )
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.Behavior.AutoUpdateOnStartTitle,
                        summary = MLang.AppSettings.Behavior.AutoUpdateOnStartSummary,
                        checked = autoUpdateCurrentProfileOnStart,
                        onCheckedChange = { viewModel.onAutoUpdateCurrentProfileOnStartChange(it) },
                    )
                    if (LocaleUtil.isChineseLocale()) {
                        PreferenceSwitchItem(
                            title = MLang.AppSettings.Behavior.OneChinaTitle,
                            summary = MLang.AppSettings.Behavior.OneChinaSummary,
                            checked = true,
                            onCheckedChange = { },
                            enabled = false,
                        )
                    }
                }
            }
            item {
                Title(MLang.AppSettings.Section.Interface)
                Card {
                    PreferenceEnumItem(
                        title = MLang.AppSettings.Interface.LanguageTitle,
                        summary = MLang.AppSettings.Interface.LanguageSummary,
                        currentValue = appLanguage,
                        items = listOf(
                            MLang.AppSettings.Interface.LanguageSystem,
                            MLang.AppSettings.Interface.LanguageChinese,
                            MLang.AppSettings.Interface.LanguageEnglish,
                        ),
                        values = AppLanguage.entries,
                        onValueChange = { viewModel.onAppLanguageChange(it) },
                    )
                    PreferenceEnumItem(
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
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.Interface.AutoHideNavbarTitle,
                        summary = MLang.AppSettings.Interface.AutoHideNavbarSummary,
                        checked = bottomBarAutoHide,
                        onCheckedChange = { viewModel.onBottomBarAutoHideChange(it) },
                    )
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.Interface.TopBarBlurTitle,
                        summary = MLang.AppSettings.Interface.TopBarBlurSummary,
                        checked = topBarBlurEnabled,
                        onCheckedChange = { viewModel.onTopBarBlurEnabledChange(it) },
                    )
                    PreferenceArrowItem(
                        title = MLang.AppSettings.Interface.PageScaleTitle,
                        summary = MLang.AppSettings.Interface.PageScaleSummary,
                        endActions = {
                            Text(
                                text = "${(pageScaleLocal * 100).toInt()}%",
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onClick = { showPageScaleDialogState.value = true },
                        holdDownState = showPageScaleDialogState.value,
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
                }
            }
            item {
                Title(MLang.AppSettings.Section.Privacy)
                Card {
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.Privacy.BiometricUnlockTitle,
                        summary = MLang.AppSettings.Privacy.BiometricUnlockSummary,
                        checked = biometricUnlockEnabled,
                        onCheckedChange = { targetState ->
                            requestBiometricConfirmation(
                                title = if (targetState) {
                                    MLang.AppSettings.Privacy.BiometricDialogTitleEnable
                                } else {
                                    MLang.AppSettings.Privacy.BiometricDialogTitleDisable
                                },
                                allowBypassWhenUnavailable = !targetState,
                            ) {
                                viewModel.onBiometricUnlockEnabledChange(targetState)
                            }
                        },
                    )
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.Privacy.ScreenshotProtectionTitle,
                        summary = MLang.AppSettings.Privacy.ScreenshotProtectionSummary,
                        checked = screenshotProtectionEnabled,
                        onCheckedChange = { targetState ->
                            requestBiometricConfirmation(
                                title = if (targetState) {
                                    MLang.AppSettings.Privacy.ScreenshotDialogTitleEnable
                                } else {
                                    MLang.AppSettings.Privacy.ScreenshotDialogTitleDisable
                                },
                                allowBypassWhenUnavailable = !targetState,
                            ) {
                                viewModel.onScreenshotProtectionEnabledChange(targetState)
                            }
                        },
                    )
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.Privacy.HideIconTitle,
                        summary = MLang.AppSettings.Privacy.HideIconSummary,
                        checked = hideAppIcon,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showHideIconDialogState.value = true
                            } else {
                                viewModel.onHideAppIconChange(false)
                                AppIconHelper.toggleIcon(context, false)
                            }
                        },
                    )
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.Privacy.HideFromRecentsTitle,
                        summary = MLang.AppSettings.Privacy.HideFromRecentsSummary,
                        checked = excludeFromRecents,
                        onCheckedChange = { viewModel.onExcludeFromRecentsChange(it) },
                    )
                }
            }
            item {
                Title(MLang.AppSettings.Section.Service)
                Card {
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.ServiceSection.TrafficNotificationTitle,
                        summary = MLang.AppSettings.ServiceSection.TrafficNotificationSummary,
                        checked = showTrafficNotification,
                        onCheckedChange = { viewModel.onShowTrafficNotificationChange(it) },
                    )
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.ServiceSection.SingleNodeTestTitle,
                        summary = MLang.AppSettings.ServiceSection.SingleNodeTestSummary,
                        checked = singleNodeTest,
                        onCheckedChange = { viewModel.onSingleNodeTestChange(it) },
                    )
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.ServiceSection.ExitUiWhenBackgroundTitle,
                        summary = MLang.AppSettings.ServiceSection.ExitUiWhenBackgroundSummary,
                        checked = exitUiWhenBackground,
                        onCheckedChange = { viewModel.onExitUiWhenBackgroundChange(it) },
                    )
                    PreferenceSwitchItem(
                        title = MLang.AppSettings.ServiceSection.BatteryOptimizationTitle,
                        summary = if (batteryOptimizationIgnored) {
                            MLang.AppSettings.ServiceSection.BatteryOptimizationSummaryEnabled
                        } else {
                            MLang.AppSettings.ServiceSection.BatteryOptimizationSummaryDisabled
                        },
                        checked = batteryOptimizationIgnored,
                        onCheckedChange = {
                            if (!openBatteryOptimizationSettings(context, batteryOptimizationIgnored)) {
                                context.toast(MLang.Util.Error.UnknownError)
                            }
                        },
                    )
                }
            }
            item {
                Title(MLang.AppSettings.Section.Network)
                Card {
                    PreferenceValueItem(
                        title = MLang.AppSettings.Network.CustomUserAgentTitle,
                        summary = customUserAgent.ifEmpty {
                            MLang.AppSettings.Network.CustomUserAgentSummaryDefault
                        },
                        onClick = {
                            customUserAgentTextFieldState.value = TextFieldValue(customUserAgent)
                            showEditCustomUserAgentDialogState.value = true
                        }
                    )
                }
            }
            item {
                Title(MLang.AppSettings.Section.Experimental)
                ExperimentalSettingsSection(
                    acgMainUiEnabled = acgMainUiEnabled,
                    acgQuoteSummary = acgQuoteSummary,
                    acgQuoteAuthorSummary = acgQuoteAuthorSummary,
                    acgSidebarExpanded = acgSidebarExpanded,
                    onAcgMainUiEnabledChange = viewModel::onAcgMainUiEnabledChange,
                    onEditAcgQuote = {
                        acgQuoteTextFieldState.value = TextFieldValue(acgHomeQuote)
                        showEditAcgQuoteDialogState.value = true
                    },
                    onEditAcgQuoteAuthor = {
                        acgQuoteAuthorTextFieldState.value = TextFieldValue(acgHomeQuoteAuthor)
                        showEditAcgQuoteAuthorDialogState.value = true
                    },
                    onAcgSidebarExpandedChange = viewModel::onAcgSidebarExpandedChange,
                    onPickWallpaper = {
                        wallpaperPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                )
            }
        }

        WarningBottomSheet(
            show = showHideIconDialogState,
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
            show = showEditCustomUserAgentDialogState,
            title = MLang.AppSettings.EditDialog.UserAgentTitle,
            textFieldValue = customUserAgentTextFieldState,
            onConfirm = { viewModel.applyCustomUserAgent(it) },
        )

        TextEditBottomSheet(
            show = showEditAcgQuoteDialogState,
            title = MLang.AppSettings.Experimental.EditAcgQuoteTitle,
            textFieldValue = acgQuoteTextFieldState,
            onConfirm = { viewModel.onAcgHomeQuoteChange(it) },
        )

        TextEditBottomSheet(
            show = showEditAcgQuoteAuthorDialogState,
            title = MLang.AppSettings.Experimental.EditAcgQuoteAuthorTitle,
            textFieldValue = acgQuoteAuthorTextFieldState,
            onConfirm = { viewModel.onAcgHomeQuoteAuthorChange(it) },
        )

        PageScaleDialog(
            show = showPageScaleDialogState.value,
            pageScale = pageScaleLocal,
            onPageScaleChange = { pageScaleLocal = it },
            onApply = { viewModel.onPageScaleChange(it) },
            onDismissRequest = { showPageScaleDialogState.value = false },
        )

        WarningBottomSheet(
            show = showBiometricUnavailableDialogState,
            title = MLang.AppSettings.Privacy.BiometricUnavailableTitle,
            messages = listOf(
                biometricUnavailableMessage.ifBlank {
                    MLang.AppSettings.Privacy.BiometricUnavailableMessage
                }
            ),
            onConfirm = { showBiometricUnavailableDialogState.value = false },
        )
    }
}

private fun isBatteryOptimizationIgnored(context: android.content.Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatteryOptimizationSettings(
    context: android.content.Context,
    alreadyIgnored: Boolean,
): Boolean {
    val intents = buildList {
        if (!alreadyIgnored) {
            add(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
        add(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    intents.forEach { intent ->
        if (runCatching { context.startActivity(intent) }.isSuccess) {
            return true
        }
    }
    return false
}

@Composable
private fun ExperimentalSettingsSection(
    acgMainUiEnabled: Boolean,
    acgQuoteSummary: String,
    acgQuoteAuthorSummary: String,
    acgSidebarExpanded: Boolean,
    onAcgMainUiEnabledChange: (Boolean) -> Unit,
    onEditAcgQuote: () -> Unit,
    onEditAcgQuoteAuthor: () -> Unit,
    onAcgSidebarExpandedChange: (Boolean) -> Unit,
    onPickWallpaper: () -> Unit,
) {
    Card {
        PreferenceSwitchItem(
            title = MLang.AppSettings.Experimental.AcgHomeTitle,
            summary = MLang.AppSettings.Experimental.AcgHomeSummary,
            checked = acgMainUiEnabled,
            onCheckedChange = onAcgMainUiEnabledChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Experimental.AcgSidebarExpandedTitle,
            summary = MLang.AppSettings.Experimental.AcgSidebarExpandedSummary,
            checked = acgSidebarExpanded,
            onCheckedChange = onAcgSidebarExpandedChange,
        )
        PreferenceValueItem(
            title = MLang.AppSettings.Experimental.AcgQuoteTitle,
            summary = acgQuoteSummary,
            onClick = onEditAcgQuote,
        )
        PreferenceValueItem(
            title = MLang.AppSettings.Experimental.AcgQuoteAuthorTitle,
            summary = acgQuoteAuthorSummary,
            onClick = onEditAcgQuoteAuthor,
        )
        PreferenceArrowItem(
            title = MLang.AppSettings.Experimental.WallpaperTitle,
            summary = MLang.AppSettings.Experimental.WallpaperSummary,
            onClick = onPickWallpaper,
        )
    }
}

@Composable
private fun PageScaleDialog(
    show: Boolean,
    pageScale: Float,
    onPageScaleChange: (Float) -> Unit,
    onApply: (Float) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var scaleText by remember(show, pageScale) {
        mutableStateOf((pageScale * 100).toInt().toString())
    }

    AppTextFieldDialog(
        show = show,
        title = MLang.AppSettings.Interface.PageScaleTitle,
        value = scaleText,
        onValueChange = { value ->
            if (value.isEmpty() || value.all(Char::isDigit)) {
                scaleText = value
            }
        },
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val parsedPercent = scaleText.toFloatOrNull() ?: (pageScale * 100)
            val clampedScale = parsedPercent.coerceIn(80f, 120f) / 100f
            onPageScaleChange(clampedScale)
            onApply(clampedScale)
            onDismissRequest()
        },
        summary = MLang.AppSettings.Interface.PageScaleDialogSummary,
        renderInRootScaffold = true,
        singleLine = true,
        trailingIcon = {
            Text(
                text = "%",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
    )
}
