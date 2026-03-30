package com.github.yumelira.yumebox.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.AppLanguage
import com.github.yumelira.yumebox.data.model.ThemeMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumelira.yumebox.presentation.component.PreferenceEnumItem
import com.github.yumelira.yumebox.presentation.component.PreferenceSwitchItem
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AccessControlScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import dev.oom_wg.purejoy.mlang.MLang

private const val TUN_STACK_TITLE = "协议栈"
private val TUN_STACK_ITEMS = listOf("System", "GVisor", "Mixed")

@Destination<RootGraph>
@Composable
fun VpnSettingsScreen(navigator: DestinationsNavigator) {
    val vpnViewModel = koinViewModel<VpnSettingsViewModel>()
    val accessViewModel = koinViewModel<AccessControlViewModel>()

    val themeMode by vpnViewModel.themeMode.state.collectAsState()
    val appLanguage by vpnViewModel.appLanguage.state.collectAsState()
    val dnsHijack by vpnViewModel.dnsHijack.state.collectAsState()
    val allowBypass by vpnViewModel.allowBypass.state.collectAsState()
    val enableIPv6 by vpnViewModel.enableIPv6.state.collectAsState()
    val systemProxy by vpnViewModel.systemProxy.state.collectAsState()
    val tunStack by vpnViewModel.tunStack.state.collectAsState()

    val accessUiState by accessViewModel.uiState.collectAsState()
    val accessControlMode by accessViewModel.accessControlMode.state.collectAsState()

    val scrollBehavior = MiuixScrollBehavior()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                accessViewModel.onPermissionResult()
            }
        },
    )

    LaunchedEffect(accessUiState.needsMiuiPermission) {
        if (accessUiState.needsMiuiPermission) {
            permissionLauncher.launch("com.android.permission.GET_INSTALLED_APPS")
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
                Title(MLang.AppSettings.Section.Interface)
                Card {
                    PreferenceEnumItem(
                        title = MLang.AppSettings.Interface.LanguageTitle,
                        currentValue = appLanguage,
                        items = listOf(
                            MLang.AppSettings.Interface.LanguageSystem,
                            MLang.AppSettings.Interface.LanguageChinese,
                            MLang.AppSettings.Interface.LanguageEnglish,
                        ),
                        values = AppLanguage.entries,
                        onValueChange = vpnViewModel::onAppLanguageChange,
                    )
                    PreferenceEnumItem(
                        title = MLang.AppSettings.Interface.ThemeModeTitle,
                        currentValue = themeMode,
                        items = listOf(
                            MLang.AppSettings.Interface.ThemeModeSystem,
                            MLang.AppSettings.Interface.ThemeModeLight,
                            MLang.AppSettings.Interface.ThemeModeDark,
                        ),
                        values = ThemeMode.entries,
                        onValueChange = vpnViewModel::onThemeModeChange,
                    )
                }

                Title("VPN 服务")
                Card {
                    PreferenceSwitchItem(
                        title = "DNS 劫持",
                        checked = dnsHijack,
                        onCheckedChange = vpnViewModel::onDnsHijackChange,
                    )
                    PreferenceSwitchItem(
                        title = "允许应用绕过 VPN Service",
                        checked = allowBypass,
                        onCheckedChange = vpnViewModel::onAllowBypassChange,
                    )
                    PreferenceSwitchItem(
                        title = "启用 IPv6",
                        checked = enableIPv6,
                        onCheckedChange = vpnViewModel::onEnableIPv6Change,
                    )
                    PreferenceSwitchItem(
                        title = "VPN 系统代理",
                        checked = systemProxy,
                        onCheckedChange = vpnViewModel::onSystemProxyChange,
                    )
                    PreferenceEnumItem(
                        title = TUN_STACK_TITLE,
                        currentValue = tunStack,
                        items = TUN_STACK_ITEMS,
                        values = TunStack.entries,
                        onValueChange = vpnViewModel::onTunStackChange,
                    )
                }

                Title("访问控制")
                Card {
                    PreferenceEnumItem(
                        title = "访问模式",
                        currentValue = accessControlMode,
                        items = listOf("允许全部", "仅允许选中", "拒绝选中"),
                        values = AccessControlMode.entries,
                        onValueChange = accessViewModel::onAccessControlModeChange,
                    )
                    PreferenceArrowItem(
                        title = "管理访问控制",
                        summary = "已选 ${accessUiState.selectedPackages.size} 个应用",
                        onClick = { navigator.navigate(AccessControlScreenDestination) },
                    )
                }
            }
        }
    }
}
