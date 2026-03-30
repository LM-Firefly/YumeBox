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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.util.VpnUtils
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.core.model.RootTunDnsMode
import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.presentation.component.AppDialog
import com.github.yumelira.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumelira.yumebox.presentation.component.PreferenceEnumItem
import com.github.yumelira.yumebox.presentation.component.PreferenceSwitchItem
import com.github.yumelira.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.theme.horizontalPadding
import com.github.yumelira.yumebox.service.root.RootAccessSupport
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AccessControlScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun NetworkSettingsScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<NetworkSettingsViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val proxyMode by viewModel.currentProxyMode.collectAsState()
    val bypassPrivateNetwork by viewModel.bypassPrivateNetwork.state.collectAsState()
    val dnsHijack by viewModel.dnsHijack.state.collectAsState()
    val allowBypass by viewModel.allowBypass.state.collectAsState()
    val enableIPv6 by viewModel.enableIPv6.state.collectAsState()
    val systemProxy by viewModel.systemProxy.state.collectAsState()
    val tunStack by viewModel.tunStack.state.collectAsState()
    val rootTunAutoRoute by viewModel.rootTunAutoRoute.state.collectAsState()
    val rootTunStrictRoute by viewModel.rootTunStrictRoute.state.collectAsState()
    val rootTunAutoRedirect by viewModel.rootTunAutoRedirect.state.collectAsState()
    val rootTunDnsMode by viewModel.rootTunDnsMode.state.collectAsState()
    val accessControlMode by viewModel.accessControlMode.state.collectAsState()

    val rootTunIfNameDraft by viewModel.rootTunIfNameDraft.collectAsState()
    val rootTunMtuDraft by viewModel.rootTunMtuDraft.collectAsState()
    val rootTunFakeIpRangeDraft by viewModel.rootTunFakeIpRangeDraft.collectAsState()
    val rootTunFakeIpRange6Draft by viewModel.rootTunFakeIpRange6Draft.collectAsState()
    var enableModeTransition by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.errors.collect { message ->
            context.toast(message)
        }
    }

    LaunchedEffect(Unit) {
        enableModeTransition = true
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onProxyModeChange(ProxyMode.Tun)
        } else {
            context.toast(MLang.NetworkSettings.Error.VpnDenied)
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = MLang.NetworkSettings.Title, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title(MLang.NetworkSettings.Section.VpnService)
                Card {
                    PreferenceEnumItem(
                        title = MLang.NetworkSettings.VpnService.RouteTrafficTitle,
                        summary = MLang.NetworkSettings.VpnService.RouteTrafficSummary,
                        currentValue = proxyMode,
                        items = listOf(
                            MLang.NetworkSettings.VpnService.SystemProxy,
                            MLang.NetworkSettings.VpnService.VpnMode,
                            MLang.NetworkSettings.VpnService.RootTunMode,
                        ),
                        values = listOf(
                            ProxyMode.Http,
                            ProxyMode.Tun,
                            ProxyMode.RootTun,
                        ),
                        onValueChange = { mode ->
                            when (mode) {
                                ProxyMode.Tun -> {
                                    if (!VpnUtils.checkVpnPermission(context)) {
                                        VpnUtils.getVpnPermissionIntent(context)?.let(vpnPermissionLauncher::launch)
                                            ?: viewModel.onProxyModeChange(mode)
                                    } else {
                                        viewModel.onProxyModeChange(mode)
                                    }
                                }

                                ProxyMode.RootTun -> {
                                    coroutineScope.launch {
                                        val rootStatus = RootAccessSupport.evaluateAsync(context)
                                        if (!rootStatus.canStartRootTun) {
                                            context.toast(rootStatus.rootTunBlockedMessage())
                                            return@launch
                                        }
                                        viewModel.onProxyModeChange(mode)
                                    }
                                }

                                ProxyMode.Http -> {
                                    viewModel.onProxyModeChange(mode)
                                }
                            }
                        },
                    )
                }
            }
            item {
                AnimatedVisibility(
                    visible = uiState.showServiceOptions,
                    enter = if (enableModeTransition) fadeIn() + expandVertically() else EnterTransition.None,
                    exit = if (enableModeTransition) fadeOut() + shrinkVertically() else ExitTransition.None,
                ) {
                    Column {
                        Title(MLang.NetworkSettings.Section.VpnOptions)
                        Card(
                            modifier = Modifier.animateContentSize(),
                        ) {
                            AnimatedContent(
                                targetState = uiState.configuredMode,
                                transitionSpec = {
                                    if (enableModeTransition) {
                                        fadeIn() togetherWith fadeOut()
                                    } else {
                                        EnterTransition.None togetherWith ExitTransition.None
                                    }
                                },
                                label = "network_service_options",
                            ) { mode ->
                                when (mode) {
                                    ProxyMode.Http -> Spacer(modifier = Modifier.height(0.dp))
                                    ProxyMode.Tun -> TunServiceOptions(
                                        bypassPrivateNetwork = bypassPrivateNetwork,
                                        dnsHijack = dnsHijack,
                                        allowBypass = allowBypass,
                                        enableIPv6 = enableIPv6,
                                        systemProxy = systemProxy,
                                        tunStack = tunStack,
                                        onBypassPrivateNetworkChange = viewModel::onBypassPrivateNetworkChange,
                                        onDnsHijackChange = viewModel::onDnsHijackChange,
                                        onAllowBypassChange = viewModel::onAllowBypassChange,
                                        onEnableIPv6Change = viewModel::onEnableIPv6Change,
                                        onSystemProxyChange = viewModel::onSystemProxyChange,
                                        onTunStackChange = viewModel::onTunStackChange,
                                    )

                                    ProxyMode.RootTun -> RootTunServiceOptions(
                                        bypassPrivateNetwork = bypassPrivateNetwork,
                                        dnsHijack = dnsHijack,
                                        enableIPv6 = enableIPv6,
                                        tunStack = tunStack,
                                        rootTunAutoRoute = rootTunAutoRoute,
                                        rootTunStrictRoute = rootTunStrictRoute,
                                        rootTunAutoRedirect = rootTunAutoRedirect,
                                        rootTunDnsMode = rootTunDnsMode,
                                        rootTunIfNameDraft = rootTunIfNameDraft,
                                        rootTunMtuDraft = rootTunMtuDraft,
                                        rootTunFakeIpRangeDraft = rootTunFakeIpRangeDraft,
                                        rootTunFakeIpRange6Draft = rootTunFakeIpRange6Draft,
                                        showFakeIpRange = uiState.showFakeIpRange,
                                        onBypassPrivateNetworkChange = viewModel::onBypassPrivateNetworkChange,
                                        onDnsHijackChange = viewModel::onDnsHijackChange,
                                        onEnableIPv6Change = viewModel::onEnableIPv6Change,
                                        onTunStackChange = viewModel::onTunStackChange,
                                        onRootTunAutoRouteChange = viewModel::onRootTunAutoRouteChange,
                                        onRootTunStrictRouteChange = viewModel::onRootTunStrictRouteChange,
                                        onRootTunAutoRedirectChange = viewModel::onRootTunAutoRedirectChange,
                                        onRootTunDnsModeChange = viewModel::onRootTunDnsModeChange,
                                        onRootTunIfNameDraftChange = viewModel::onRootTunIfNameDraftChange,
                                        onRootTunMtuDraftChange = viewModel::onRootTunMtuDraftChange,
                                        onRootTunFakeIpRangeDraftChange = viewModel::onRootTunFakeIpRangeDraftChange,
                                        onRootTunFakeIpRange6DraftChange = viewModel::onRootTunFakeIpRange6DraftChange,
                                        commitRootTunIfName = viewModel::commitRootTunIfName,
                                        commitRootTunMtu = viewModel::commitRootTunMtu,
                                        commitRootTunFakeIpRange = viewModel::commitRootTunFakeIpRange,
                                        commitRootTunFakeIpRange6 = viewModel::commitRootTunFakeIpRange6,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Title(MLang.NetworkSettings.Section.ProxyOptions)
                Card {
                    if (uiState.showAccessControlMode) {
                        PreferenceEnumItem(
                            title = MLang.NetworkSettings.ProxyOptions.AccessControlModeTitle,
                            currentValue = accessControlMode,
                            items = listOf(
                                MLang.NetworkSettings.ProxyOptions.AllowAll,
                                MLang.NetworkSettings.ProxyOptions.AllowSelected,
                                MLang.NetworkSettings.ProxyOptions.RejectSelected,
                            ),
                            values = AccessControlMode.entries,
                            onValueChange = viewModel::onAccessControlModeChange,
                        )
                    }
                    PreferenceArrowItem(
                        title = MLang.NetworkSettings.ProxyOptions.ManageAccessControlTitle,
                        summary = MLang.NetworkSettings.ProxyOptions.ManageAccessControlSummary,
                        onClick = {
                            navigator.navigate(AccessControlScreenDestination)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TunServiceOptions(
    bypassPrivateNetwork: Boolean,
    dnsHijack: Boolean,
    allowBypass: Boolean,
    enableIPv6: Boolean,
    systemProxy: Boolean,
    tunStack: TunStack,
    onBypassPrivateNetworkChange: (Boolean) -> Unit,
    onDnsHijackChange: (Boolean) -> Unit,
    onAllowBypassChange: (Boolean) -> Unit,
    onEnableIPv6Change: (Boolean) -> Unit,
    onSystemProxyChange: (Boolean) -> Unit,
    onTunStackChange: (TunStack) -> Unit,
) {
    CommonTunServiceOptions(
        bypassPrivateNetwork = bypassPrivateNetwork,
        dnsHijack = dnsHijack,
        enableIPv6 = enableIPv6,
        tunStack = tunStack,
        onBypassPrivateNetworkChange = onBypassPrivateNetworkChange,
        onDnsHijackChange = onDnsHijackChange,
        onEnableIPv6Change = onEnableIPv6Change,
        onTunStackChange = onTunStackChange,
        extraOptions = {
            PreferenceSwitchItem(
                title = MLang.NetworkSettings.VpnOptions.AllowBypassTitle,
                summary = MLang.NetworkSettings.VpnOptions.AllowBypassSummary,
                checked = allowBypass,
                onCheckedChange = onAllowBypassChange,
            )
            PreferenceSwitchItem(
                title = MLang.NetworkSettings.VpnOptions.SystemProxyTitle,
                summary = MLang.NetworkSettings.VpnOptions.SystemProxySummary,
                checked = systemProxy,
                onCheckedChange = onSystemProxyChange,
            )
        },
    )
}

@Composable
private fun RootTunServiceOptions(
    bypassPrivateNetwork: Boolean,
    dnsHijack: Boolean,
    enableIPv6: Boolean,
    tunStack: TunStack,
    rootTunAutoRoute: Boolean,
    rootTunStrictRoute: Boolean,
    rootTunAutoRedirect: Boolean,
    rootTunDnsMode: RootTunDnsMode,
    rootTunIfNameDraft: String,
    rootTunMtuDraft: String,
    rootTunFakeIpRangeDraft: String,
    rootTunFakeIpRange6Draft: String,
    showFakeIpRange: Boolean,
    onBypassPrivateNetworkChange: (Boolean) -> Unit,
    onDnsHijackChange: (Boolean) -> Unit,
    onEnableIPv6Change: (Boolean) -> Unit,
    onTunStackChange: (TunStack) -> Unit,
    onRootTunAutoRouteChange: (Boolean) -> Unit,
    onRootTunStrictRouteChange: (Boolean) -> Unit,
    onRootTunAutoRedirectChange: (Boolean) -> Unit,
    onRootTunDnsModeChange: (RootTunDnsMode) -> Unit,
    onRootTunIfNameDraftChange: (String) -> Unit,
    onRootTunMtuDraftChange: (String) -> Unit,
    onRootTunFakeIpRangeDraftChange: (String) -> Unit,
    onRootTunFakeIpRange6DraftChange: (String) -> Unit,
    commitRootTunIfName: () -> Unit,
    commitRootTunMtu: () -> Unit,
    commitRootTunFakeIpRange: () -> Unit,
    commitRootTunFakeIpRange6: () -> Unit,
) {
    CommonTunServiceOptions(
        bypassPrivateNetwork = bypassPrivateNetwork,
        dnsHijack = dnsHijack,
        enableIPv6 = enableIPv6,
        tunStack = tunStack,
        onBypassPrivateNetworkChange = onBypassPrivateNetworkChange,
        onDnsHijackChange = onDnsHijackChange,
        onEnableIPv6Change = onEnableIPv6Change,
        onTunStackChange = onTunStackChange,
        extraOptions = {
            RootTunAdvancedOptions(
                rootTunAutoRoute = rootTunAutoRoute,
                rootTunStrictRoute = rootTunStrictRoute,
                rootTunAutoRedirect = rootTunAutoRedirect,
                rootTunDnsMode = rootTunDnsMode,
                rootTunIfNameDraft = rootTunIfNameDraft,
                rootTunMtuDraft = rootTunMtuDraft,
                rootTunFakeIpRangeDraft = rootTunFakeIpRangeDraft,
                rootTunFakeIpRange6Draft = rootTunFakeIpRange6Draft,
                showFakeIpRange = showFakeIpRange,
                onRootTunAutoRouteChange = onRootTunAutoRouteChange,
                onRootTunStrictRouteChange = onRootTunStrictRouteChange,
                onRootTunAutoRedirectChange = onRootTunAutoRedirectChange,
                onRootTunDnsModeChange = onRootTunDnsModeChange,
                onRootTunIfNameDraftChange = onRootTunIfNameDraftChange,
                onRootTunMtuDraftChange = onRootTunMtuDraftChange,
                onRootTunFakeIpRangeDraftChange = onRootTunFakeIpRangeDraftChange,
                onRootTunFakeIpRange6DraftChange = onRootTunFakeIpRange6DraftChange,
                commitRootTunIfName = commitRootTunIfName,
                commitRootTunMtu = commitRootTunMtu,
                commitRootTunFakeIpRange = commitRootTunFakeIpRange,
                commitRootTunFakeIpRange6 = commitRootTunFakeIpRange6,
            )
        },
    )
}

@Composable
private fun RootTunAdvancedOptions(
    rootTunAutoRoute: Boolean,
    rootTunStrictRoute: Boolean,
    rootTunAutoRedirect: Boolean,
    rootTunDnsMode: RootTunDnsMode,
    rootTunIfNameDraft: String,
    rootTunMtuDraft: String,
    rootTunFakeIpRangeDraft: String,
    rootTunFakeIpRange6Draft: String,
    showFakeIpRange: Boolean,
    onRootTunAutoRouteChange: (Boolean) -> Unit,
    onRootTunStrictRouteChange: (Boolean) -> Unit,
    onRootTunAutoRedirectChange: (Boolean) -> Unit,
    onRootTunDnsModeChange: (RootTunDnsMode) -> Unit,
    onRootTunIfNameDraftChange: (String) -> Unit,
    onRootTunMtuDraftChange: (String) -> Unit,
    onRootTunFakeIpRangeDraftChange: (String) -> Unit,
    onRootTunFakeIpRange6DraftChange: (String) -> Unit,
    commitRootTunIfName: () -> Unit,
    commitRootTunMtu: () -> Unit,
    commitRootTunFakeIpRange: () -> Unit,
    commitRootTunFakeIpRange6: () -> Unit,
) {
    var editDialog by remember { mutableStateOf<RootTunEditDialogState?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    ) {
        PreferenceArrowItem(
            title = MLang.NetworkSettings.RootTun.IfNameTitle,
            summary = rootTunIfNameDraft.ifBlank { MLang.NetworkSettings.RootTun.IfNameSummary },
            onClick = { editDialog = RootTunEditDialogState.IfName },
        )
        PreferenceArrowItem(
            title = MLang.NetworkSettings.RootTun.MtuTitle,
            summary = rootTunMtuDraft.ifBlank { MLang.NetworkSettings.RootTun.MtuSummary },
            onClick = { editDialog = RootTunEditDialogState.Mtu },
        )
        PreferenceSwitchItem(
            title = MLang.NetworkSettings.RootTun.AutoRouteTitle,
            summary = MLang.NetworkSettings.RootTun.AutoRouteSummary,
            checked = rootTunAutoRoute,
            onCheckedChange = onRootTunAutoRouteChange,
        )
        PreferenceSwitchItem(
            title = MLang.NetworkSettings.RootTun.StrictRouteTitle,
            summary = MLang.NetworkSettings.RootTun.StrictRouteSummary,
            checked = rootTunStrictRoute,
            onCheckedChange = onRootTunStrictRouteChange,
        )
        PreferenceSwitchItem(
            title = MLang.NetworkSettings.RootTun.AutoRedirectTitle,
            summary = MLang.NetworkSettings.RootTun.AutoRedirectSummary,
            checked = rootTunAutoRedirect,
            onCheckedChange = onRootTunAutoRedirectChange,
        )
        PreferenceEnumItem(
            title = MLang.NetworkSettings.RootTun.DnsModeTitle,
            summary = MLang.NetworkSettings.RootTun.DnsModeSummary,
            currentValue = rootTunDnsMode,
            items = listOf(
                MLang.NetworkSettings.RootTun.DnsModeRedirHost,
                MLang.NetworkSettings.RootTun.DnsModeFakeIp,
            ),
            values = RootTunDnsMode.entries,
            onValueChange = onRootTunDnsModeChange,
        )
        AnimatedVisibility(
            visible = showFakeIpRange,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PreferenceArrowItem(
                    title = MLang.NetworkSettings.RootTun.FakeIpRangeTitle,
                    summary = rootTunFakeIpRangeDraft.ifBlank { MLang.NetworkSettings.RootTun.FakeIpRangeSummary },
                    onClick = { editDialog = RootTunEditDialogState.FakeIpRange },
                )
                PreferenceArrowItem(
                    title = MLang.NetworkSettings.RootTun.FakeIpRange6Title,
                    summary = rootTunFakeIpRange6Draft.ifBlank { MLang.NetworkSettings.RootTun.FakeIpRange6Summary },
                    onClick = { editDialog = RootTunEditDialogState.FakeIpRange6 },
                )
            }
        }
    }

    when (editDialog) {
        RootTunEditDialogState.IfName -> RootTunTextEditDialog(
            title = MLang.NetworkSettings.RootTun.IfNameTitle,
            value = rootTunIfNameDraft,
            onValueChange = onRootTunIfNameDraftChange,
            onDismiss = { editDialog = null },
            onCommit = {
                commitRootTunIfName()
                editDialog = null
            },
        )

        RootTunEditDialogState.Mtu -> RootTunTextEditDialog(
            title = MLang.NetworkSettings.RootTun.MtuTitle,
            value = rootTunMtuDraft,
            onValueChange = onRootTunMtuDraftChange,
            onDismiss = { editDialog = null },
            onCommit = {
                commitRootTunMtu()
                editDialog = null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
        )

        RootTunEditDialogState.FakeIpRange -> RootTunTextEditDialog(
            title = MLang.NetworkSettings.RootTun.FakeIpRangeTitle,
            value = rootTunFakeIpRangeDraft,
            onValueChange = onRootTunFakeIpRangeDraftChange,
            onDismiss = { editDialog = null },
            onCommit = {
                commitRootTunFakeIpRange()
                editDialog = null
            },
        )

        RootTunEditDialogState.FakeIpRange6 -> RootTunTextEditDialog(
            title = MLang.NetworkSettings.RootTun.FakeIpRange6Title,
            value = rootTunFakeIpRange6Draft,
            onValueChange = onRootTunFakeIpRange6DraftChange,
            onDismiss = { editDialog = null },
            onCommit = {
                commitRootTunFakeIpRange6()
                editDialog = null
            },
        )

        null -> Unit
    }
}

@Composable
private fun CommonTunServiceOptions(
    bypassPrivateNetwork: Boolean,
    dnsHijack: Boolean,
    enableIPv6: Boolean,
    tunStack: TunStack,
    onBypassPrivateNetworkChange: (Boolean) -> Unit,
    onDnsHijackChange: (Boolean) -> Unit,
    onEnableIPv6Change: (Boolean) -> Unit,
    onTunStackChange: (TunStack) -> Unit,
    extraOptions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        PreferenceSwitchItem(
            title = MLang.NetworkSettings.VpnOptions.BypassPrivateTitle,
            summary = MLang.NetworkSettings.VpnOptions.BypassPrivateSummary,
            checked = bypassPrivateNetwork,
            onCheckedChange = onBypassPrivateNetworkChange,
        )
        PreferenceSwitchItem(
            title = MLang.NetworkSettings.VpnOptions.DnsHijackTitle,
            summary = MLang.NetworkSettings.VpnOptions.DnsHijackSummary,
            checked = dnsHijack,
            onCheckedChange = onDnsHijackChange,
        )
        PreferenceSwitchItem(
            title = MLang.NetworkSettings.VpnOptions.EnableIpv6Title,
            summary = MLang.NetworkSettings.VpnOptions.EnableIpv6Summary,
            checked = enableIPv6,
            onCheckedChange = onEnableIPv6Change,
        )
        PreferenceEnumItem(
            title = MLang.NetworkSettings.ProxyOptions.TunStackTitle,
            currentValue = tunStack,
            items = listOf("System", "GVisor", "Mixed"),
            values = TunStack.entries,
            onValueChange = onTunStackChange,
        )
        extraOptions()
    }
}

@Composable
private fun RootTunTextEditDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCommit: () -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Done,
    ),
) {
    val focusManager = LocalFocusManager.current
    AppTextFieldDialog(
        show = true,
        title = title,
        value = value,
        onValueChange = onValueChange,
        onDismissRequest = onDismiss,
        onConfirm = {
            onCommit()
            onDismiss()
        },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onDone = {
                onCommit()
                onDismiss()
                focusManager.clearFocus()
            },
        ),
    )
}

private enum class RootTunEditDialogState {
    IfName,
    Mtu,
    FakeIpRange,
    FakeIpRange6,
}
