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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.common.util.AppIconHelper
import com.github.yumelira.yumebox.common.util.BiometricHelper
import com.github.yumelira.yumebox.common.util.LocaleUtil
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumelira.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumelira.yumebox.presentation.component.PreferenceEnumItem
import com.github.yumelira.yumebox.presentation.component.PreferenceSwitchItem
import com.github.yumelira.yumebox.presentation.component.PreferenceValueItem
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TextEditBottomSheet
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.WarningBottomSheet
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.screen.settings.component.ThemeColorPickerItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AcgWallpaperCropScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.core.net.toUri

@Composable
@Destination<RootGraph>
fun AppSettingsScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AppSettingsViewModel>()

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
            item { AppBehaviorSettingsSection(viewModel) }
            item { AppInterfaceSettingsSection(viewModel) }
            item { AppPrivacySettingsSection(viewModel) }
            item { AppServiceSettingsSection(viewModel) }
            item { AppNetworkSettingsSection(viewModel) }
            item {
                AppExperimentalSettingsSection(
                    viewModel = viewModel,
                    navigator = navigator,
                )
            }
        }
    }
}

@Composable
private fun AppBehaviorSettingsSection(viewModel: AppSettingsViewModel) {
    val automaticRestart by viewModel.automaticRestart.state.collectAsState()
    val autoUpdateCurrentProfileOnStart by viewModel.autoUpdateCurrentProfileOnStart.state.collectAsState()
    val isChineseLocale = remember { LocaleUtil.isChineseLocale() }

    Title(MLang.AppSettings.Section.Behavior)
    Card {
        PreferenceSwitchItem(
            title = MLang.AppSettings.Behavior.AutoStartTitle,
            summary = MLang.AppSettings.Behavior.AutoStartSummary,
            checked = automaticRestart,
            onCheckedChange = viewModel::onAutomaticRestartChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Behavior.AutoUpdateOnStartTitle,
            summary = MLang.AppSettings.Behavior.AutoUpdateOnStartSummary,
            checked = autoUpdateCurrentProfileOnStart,
            onCheckedChange = viewModel::onAutoUpdateCurrentProfileOnStartChange,
        )
        if (isChineseLocale) {
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

@Composable
private fun AppInterfaceSettingsSection(viewModel: AppSettingsViewModel) {
    val themeMode by viewModel.themeMode.state.collectAsState()
    val appLanguage by viewModel.appLanguage.state.collectAsState()
    val themeSeedColorArgb by viewModel.themeSeedColorArgb.state.collectAsState()
    val bottomBarAutoHide by viewModel.bottomBarAutoHide.state.collectAsState()
    val bottomBarUseLegacyStyle by viewModel.bottomBarUseLegacyStyle.state.collectAsState()
    val topBarBlurEnabled by viewModel.topBarBlurEnabled.state.collectAsState()
    val pageScale by viewModel.pageScale.state.collectAsState()

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
            onValueChange = viewModel::onAppLanguageChange,
        )
        PreferenceEnumItem(
            title = MLang.AppSettings.Interface.ThemeModeTitle,
            summary = MLang.AppSettings.Interface.ThemeModeSummary,
            currentValue = themeMode,
            items = listOf(
                MLang.AppSettings.Interface.ThemeModeSystem,
                MLang.AppSettings.Interface.ThemeModeLight,
                MLang.AppSettings.Interface.ThemeModeDark,
            ),
            values = ThemeMode.entries,
            onValueChange = viewModel::onThemeModeChange,
        )
        ThemeColorPickerItem(
            themeSeedColorArgb = themeSeedColorArgb,
            onThemeSeedColorChange = viewModel::onThemeSeedColorChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Interface.AutoHideNavbarTitle,
            summary = MLang.AppSettings.Interface.AutoHideNavbarSummary,
            checked = bottomBarAutoHide,
            onCheckedChange = viewModel::onBottomBarAutoHideChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Interface.LegacyNavbarStyleTitle,
            summary = MLang.AppSettings.Interface.LegacyNavbarStyleSummary,
            checked = bottomBarUseLegacyStyle,
            onCheckedChange = viewModel::onBottomBarUseLegacyStyleChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Interface.TopBarBlurTitle,
            summary = MLang.AppSettings.Interface.TopBarBlurSummary,
            checked = topBarBlurEnabled,
            onCheckedChange = viewModel::onTopBarBlurEnabledChange,
        )
        PageScalePreferenceItem(
            pageScale = pageScale,
            onApply = viewModel::onPageScaleChange,
        )
    }
}

@Composable
private fun AppPrivacySettingsSection(viewModel: AppSettingsViewModel) {
    val context = LocalContext.current
    val biometricUnlockEnabled by viewModel.biometricUnlockEnabled.state.collectAsState()
    val screenshotProtectionEnabled by viewModel.screenshotProtectionEnabled.state.collectAsState()
    val hideAppIcon by viewModel.hideAppIcon.state.collectAsState()
    val excludeFromRecents by viewModel.excludeFromRecents.state.collectAsState()
    val showHideIconDialogState = remember { mutableStateOf(false) }
    val showBiometricUnavailableDialogState = remember { mutableStateOf(false) }
    var biometricUnavailableMessage by remember { mutableStateOf("") }

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
            onCheckedChange = viewModel::onExcludeFromRecentsChange,
        )
    }

    WarningBottomSheet(
        show = showHideIconDialogState,
        title = MLang.AppSettings.WarningDialog.Title,
        messages = listOf(
            MLang.AppSettings.WarningDialog.HideIconMsg1,
            MLang.AppSettings.WarningDialog.HideIconMsg2,
        ),
        onConfirm = {
            viewModel.onHideAppIconChange(true)
            AppIconHelper.toggleIcon(context, true)
        },
    )

    WarningBottomSheet(
        show = showBiometricUnavailableDialogState,
        title = MLang.AppSettings.Privacy.BiometricUnavailableTitle,
        messages = listOf(
            biometricUnavailableMessage.ifBlank {
                MLang.AppSettings.Privacy.BiometricUnavailableMessage
            },
        ),
        onConfirm = { showBiometricUnavailableDialogState.value = false },
    )
}

@Composable
private fun AppServiceSettingsSection(viewModel: AppSettingsViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val showTrafficNotification by viewModel.showTrafficNotification.state.collectAsState()
    val singleNodeTest by viewModel.singleNodeTest.state.collectAsState()
    val exitUiWhenBackground by viewModel.exitUiWhenBackground.state.collectAsState()
    var batteryOptimizationIgnored by remember {
        mutableStateOf(isBatteryOptimizationIgnored(context))
    }
    val batteryOptimizationSummary = remember(batteryOptimizationIgnored) {
        if (batteryOptimizationIgnored) {
            MLang.AppSettings.ServiceSection.BatteryOptimizationSummaryEnabled
        } else {
            MLang.AppSettings.ServiceSection.BatteryOptimizationSummaryDisabled
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Title(MLang.AppSettings.Section.Service)
    Card {
        PreferenceSwitchItem(
            title = MLang.AppSettings.ServiceSection.TrafficNotificationTitle,
            summary = MLang.AppSettings.ServiceSection.TrafficNotificationSummary,
            checked = showTrafficNotification,
            onCheckedChange = viewModel::onShowTrafficNotificationChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.ServiceSection.SingleNodeTestTitle,
            summary = MLang.AppSettings.ServiceSection.SingleNodeTestSummary,
            checked = singleNodeTest,
            onCheckedChange = viewModel::onSingleNodeTestChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.ServiceSection.ExitUiWhenBackgroundTitle,
            summary = MLang.AppSettings.ServiceSection.ExitUiWhenBackgroundSummary,
            checked = exitUiWhenBackground,
            onCheckedChange = viewModel::onExitUiWhenBackgroundChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.ServiceSection.BatteryOptimizationTitle,
            summary = batteryOptimizationSummary,
            checked = batteryOptimizationIgnored,
            onCheckedChange = {
                if (!openBatteryOptimizationSettings(context, batteryOptimizationIgnored)) {
                    context.toast(MLang.Util.Error.UnknownError)
                }
            },
        )
    }
}

@Composable
private fun AppNetworkSettingsSection(viewModel: AppSettingsViewModel) {
    val customUserAgent by viewModel.customUserAgent.state.collectAsState()

    Title(MLang.AppSettings.Section.Network)
    Card {
        CustomUserAgentPreferenceItem(
            customUserAgent = customUserAgent,
            onConfirm = viewModel::applyCustomUserAgent,
        )
    }
}

@Composable
private fun AppExperimentalSettingsSection(
    viewModel: AppSettingsViewModel,
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val acgMainUiEnabled by viewModel.acgMainUiEnabled.state.collectAsState()
    val acgHomeQuote by viewModel.acgHomeQuote.state.collectAsState()
    val acgHomeQuoteAuthor by viewModel.acgHomeQuoteAuthor.state.collectAsState()
    val acgSidebarExpanded by viewModel.acgSidebarExpanded.state.collectAsState()
    val acgQuoteSummary = remember(acgHomeQuote) {
        acgHomeQuote.ifBlank { MLang.AppSettings.Experimental.AcgQuoteDefault }
    }
    val acgQuoteAuthorSummary = remember(acgHomeQuoteAuthor) {
        acgHomeQuoteAuthor.ifBlank { MLang.AppSettings.Experimental.AcgQuoteAuthorDefault }
    }
    val showEditAcgQuoteDialogState = remember { mutableStateOf(false) }
    val showEditAcgQuoteAuthorDialogState = remember { mutableStateOf(false) }
    val acgQuoteTextFieldState = remember { mutableStateOf(TextFieldValue()) }
    val acgQuoteAuthorTextFieldState = remember { mutableStateOf(TextFieldValue()) }
    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val wallpaperZoom = viewModel.acgWallpaperZoom.value
            val wallpaperBiasX = viewModel.acgWallpaperBiasX.value
            val wallpaperBiasY = viewModel.acgWallpaperBiasY.value
            navigator.navigate(
                AcgWallpaperCropScreenDestination(
                    wallpaperUri = uri.toString(),
                    initialZoom = wallpaperZoom,
                    initialBiasX = wallpaperBiasX,
                    initialBiasY = wallpaperBiasY,
                ),
            ) {
                launchSingleTop = true
            }
        }
    }

    Title(MLang.AppSettings.Section.Experimental)
    Card {
        PreferenceSwitchItem(
            title = MLang.AppSettings.Experimental.AcgHomeTitle,
            summary = MLang.AppSettings.Experimental.AcgHomeSummary,
            checked = acgMainUiEnabled,
            onCheckedChange = viewModel::onAcgMainUiEnabledChange,
        )
        PreferenceSwitchItem(
            title = MLang.AppSettings.Experimental.AcgSidebarExpandedTitle,
            summary = MLang.AppSettings.Experimental.AcgSidebarExpandedSummary,
            checked = acgSidebarExpanded,
            onCheckedChange = viewModel::onAcgSidebarExpandedChange,
        )
        PreferenceValueItem(
            title = MLang.AppSettings.Experimental.AcgQuoteTitle,
            summary = acgQuoteSummary,
            onClick = {
                acgQuoteTextFieldState.value = TextFieldValue(acgHomeQuote)
                showEditAcgQuoteDialogState.value = true
            },
        )
        PreferenceValueItem(
            title = MLang.AppSettings.Experimental.AcgQuoteAuthorTitle,
            summary = acgQuoteAuthorSummary,
            onClick = {
                acgQuoteAuthorTextFieldState.value = TextFieldValue(acgHomeQuoteAuthor)
                showEditAcgQuoteAuthorDialogState.value = true
            },
        )
        PreferenceArrowItem(
            title = MLang.AppSettings.Experimental.WallpaperTitle,
            summary = MLang.AppSettings.Experimental.WallpaperSummary,
            onClick = {
                wallpaperPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }

    TextEditBottomSheet(
        show = showEditAcgQuoteDialogState,
        title = MLang.AppSettings.Experimental.EditAcgQuoteTitle,
        textFieldValue = acgQuoteTextFieldState,
        onConfirm = viewModel::onAcgHomeQuoteChange,
    )

    TextEditBottomSheet(
        show = showEditAcgQuoteAuthorDialogState,
        title = MLang.AppSettings.Experimental.EditAcgQuoteAuthorTitle,
        textFieldValue = acgQuoteAuthorTextFieldState,
        onConfirm = viewModel::onAcgHomeQuoteAuthorChange,
    )
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
                    data = "package:${context.packageName}".toUri()
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
private fun PageScalePreferenceItem(
    pageScale: Float,
    onApply: (Float) -> Unit,
) {
    var pageScaleLocal by remember(pageScale) { mutableFloatStateOf(pageScale) }
    val pageScalePercentText = remember(pageScaleLocal) { "${(pageScaleLocal * 100).toInt()}%" }
    val showPageScaleDialogState = remember { mutableStateOf(false) }

    PreferenceArrowItem(
        title = MLang.AppSettings.Interface.PageScaleTitle,
        summary = MLang.AppSettings.Interface.PageScaleSummary,
        endActions = {
            Text(
                text = pageScalePercentText,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = { showPageScaleDialogState.value = true },
        holdDownState = showPageScaleDialogState.value,
        bottomAction = {
            Slider(
                value = pageScaleLocal,
                onValueChange = { pageScaleLocal = it },
                onValueChangeFinished = { onApply(pageScaleLocal) },
                valueRange = 0.8f..1.2f,
                magnetThreshold = 0.01f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            )
        },
    )

    PageScaleDialog(
        show = showPageScaleDialogState.value,
        pageScale = pageScaleLocal,
        onPageScaleChange = { pageScaleLocal = it },
        onApply = onApply,
        onDismissRequest = { showPageScaleDialogState.value = false },
    )
}

@Composable
private fun CustomUserAgentPreferenceItem(
    customUserAgent: String,
    onConfirm: (String) -> Unit,
) {
    val customUserAgentSummary = remember(customUserAgent) {
        customUserAgent.ifEmpty {
            MLang.AppSettings.Network.CustomUserAgentSummaryDefault
        }
    }
    val showEditCustomUserAgentDialogState = remember { mutableStateOf(false) }
    val customUserAgentTextFieldState = remember { mutableStateOf(TextFieldValue()) }

    PreferenceValueItem(
        title = MLang.AppSettings.Network.CustomUserAgentTitle,
        summary = customUserAgentSummary,
        onClick = {
            customUserAgentTextFieldState.value = TextFieldValue(customUserAgent)
            showEditCustomUserAgentDialogState.value = true
        },
    )

    TextEditBottomSheet(
        show = showEditCustomUserAgentDialogState,
        title = MLang.AppSettings.EditDialog.UserAgentTitle,
        textFieldValue = customUserAgentTextFieldState,
        onConfirm = onConfirm,
    )
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
