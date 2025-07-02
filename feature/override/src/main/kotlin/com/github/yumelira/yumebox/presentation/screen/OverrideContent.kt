/*
 * This file is part of YumeBox.
 */

package com.github.yumelira.yumebox.presentation.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.viewmodel.BuiltinRoutePreset
import com.github.yumelira.yumebox.presentation.viewmodel.OverrideViewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.theme.MiuixTheme

typealias OpenOverrideStringListEditor = (
    title: String,
    placeholder: String,
    value: List<String>?,
    onValueChange: (List<String>?) -> Unit,
) -> Unit

typealias OpenOverrideStringMapEditor = (
    title: String,
    keyPlaceholder: String,
    valuePlaceholder: String,
    value: Map<String, String>?,
    onValueChange: (Map<String, String>?) -> Unit,
) -> Unit

@Composable
fun OverrideContent(
    navigator: DestinationsNavigator,
    onEditStringList: OpenOverrideStringListEditor,
    onEditStringMap: OpenOverrideStringMapEditor,
) {
    @Composable
    fun ActionItem(title: String, summary: String, onClick: () -> Unit) {
        SuperArrow(title = title, summary = summary, onClick = onClick)
    }

    @Composable
    fun PortInput(title: String, value: Int?, onValueChange: (Int?) -> Unit) {
        PortInputContent(title = title, value = value, onValueChange = onValueChange)
    }

    @Composable
    fun StringInput(
        title: String,
        value: String?,
        placeholder: String = "",
        onValueChange: (String?) -> Unit,
    ) {
        StringInputContent(
            title = title,
            value = value,
            placeholder = placeholder,
            onValueChange = onValueChange,
        )
    }

    @Composable
    fun StringListInput(
        title: String,
        value: List<String>?,
        placeholder: String = "",
        onValueChange: (List<String>?) -> Unit,
    ) {
        StringListInputContent(
            title = title,
            value = value,
            onClick = { onEditStringList(title, placeholder, value, onValueChange) },
        )
    }

    @Composable
    fun StringMapInput(
        title: String,
        value: Map<String, String>?,
        keyPlaceholder: String = MLang.Component.ConfigInput.KeyPlaceholder,
        valuePlaceholder: String = MLang.Component.ConfigInput.ValuePlaceholder,
        onValueChange: (Map<String, String>?) -> Unit,
    ) {
        StringMapInputContent(
            title = title,
            value = value,
            onClick = {
                onEditStringMap(title, keyPlaceholder, valuePlaceholder, value, onValueChange)
            },
        )
    }

    val viewModel: OverrideViewModel = koinViewModel()
    val scrollBehavior = MiuixScrollBehavior()
    val context = androidx.compose.ui.platform.LocalContext.current

    val configuration by viewModel.configuration.collectAsState()
    val selectedBuiltinPreset by viewModel.selectedBuiltinRoutePreset.collectAsState()
    val showResetDialog = remember { mutableStateOf(false) }
    val importMode = remember { mutableStateOf(OverrideViewModel.ImportMode.Merge) }
    val pendingExportJson = remember { mutableStateOf("") }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
        }.onSuccess { jsonText ->
            val result = viewModel.importFromJson(jsonText, importMode.value)
            Toast.makeText(
                context,
                if (result.isSuccess) MLang.Override.Message.ImportSuccess
                else MLang.Override.Message.ImportFailed.format(result.exceptionOrNull()?.message ?: "unknown"),
                Toast.LENGTH_SHORT,
            ).show()
        }.onFailure { e ->
            Toast.makeText(
                context,
                MLang.Override.Message.ImportFailed.format(e.message ?: "unknown"),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(pendingExportJson.value.toByteArray())
                stream.flush()
            }
        }.onSuccess {
            Toast.makeText(context, MLang.Override.Message.ExportSuccess, Toast.LENGTH_SHORT).show()
        }.onFailure { e ->
            Toast.makeText(
                context,
                MLang.Override.Message.ExportFailed.format(e.message ?: "unknown"),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val sectionEntries = remember {
        listOf(
            MLang.Override.Section.Inbound to MLang.Override.Summary.Inbound,
            MLang.Override.Section.General to MLang.Override.Summary.General,
            MLang.Override.Section.Dns to MLang.Override.Summary.Dns,
            MLang.Override.Section.Sniffer to MLang.Override.Summary.Sniffer,
            MLang.Override.Section.Rules to MLang.Override.Summary.Rules,
            MLang.Override.Section.Tools to MLang.Override.Summary.Tools,
        )
    }
    val selectedSectionIndex = rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Override.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = 24.dp),
                        onClick = { showResetDialog.value = true },
                    ) {
                        Icon(MiuixIcons.Reset, contentDescription = MLang.Component.Navigation.Refresh)
                    }
                },
            )
        },
    ) { innerPadding ->
        ScreenLazyColumn(scrollBehavior = scrollBehavior, innerPadding = innerPadding) {
            item {
                SmallTitle(MLang.Override.Section.Rules)
                Card {
                    SuperSwitch(
                        title = MLang.Override.Preset.RouteRulesA,
                        summary = MLang.Override.Preset.RouteRulesASummary,
                        checked = selectedBuiltinPreset == BuiltinRoutePreset.PresetA,
                        onCheckedChange = { checked ->
                            val result = if (checked) {
                                viewModel.applyBuiltinRoutePreset(BuiltinRoutePreset.PresetA)
                            } else {
                                viewModel.applyBuiltinRoutePreset(BuiltinRoutePreset.None)
                            }
                            if (result.isFailure) {
                                Toast.makeText(
                                    context,
                                    MLang.Override.Message.ImportFailed.format(
                                        result.exceptionOrNull()?.message ?: "unknown"
                                    ),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                    SuperSwitch(
                        title = MLang.Override.Preset.RouteRulesB,
                        summary = MLang.Override.Preset.RouteRulesBSummary,
                        checked = selectedBuiltinPreset == BuiltinRoutePreset.PresetB,
                        onCheckedChange = { checked ->
                            val result = if (checked) {
                                viewModel.applyBuiltinRoutePreset(BuiltinRoutePreset.PresetB)
                            } else {
                                viewModel.applyBuiltinRoutePreset(BuiltinRoutePreset.None)
                            }
                            if (result.isFailure) {
                                Toast.makeText(
                                    context,
                                    MLang.Override.Message.ImportFailed.format(
                                        result.exceptionOrNull()?.message ?: "unknown"
                                    ),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }
            }

            item {
                SmallTitle(MLang.Override.Nav.NodePageSwitch)
                AnimatedContent(
                    targetState = selectedSectionIndex.value,
                    transitionSpec = {
                        if (initialState == null && targetState != null) {
                            (slideInHorizontally(
                                animationSpec = tween(durationMillis = 260),
                                initialOffsetX = { fullWidth -> fullWidth },
                            ) + fadeIn(animationSpec = tween(durationMillis = 180))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 240),
                                    targetOffsetX = { fullWidth -> -fullWidth },
                                ) + fadeOut(animationSpec = tween(durationMillis = 160)))
                        } else if (initialState != null && targetState == null) {
                            (slideInHorizontally(
                                animationSpec = tween(durationMillis = 240),
                                initialOffsetX = { fullWidth -> -fullWidth },
                            ) + fadeIn(animationSpec = tween(durationMillis = 180))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 220),
                                    targetOffsetX = { fullWidth -> fullWidth },
                                ) + fadeOut(animationSpec = tween(durationMillis = 160)))
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(durationMillis = 240),
                                initialOffsetX = { fullWidth -> fullWidth },
                            ) + fadeIn(animationSpec = tween(durationMillis = 180))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 220),
                                    targetOffsetX = { fullWidth -> -fullWidth },
                                ) + fadeOut(animationSpec = tween(durationMillis = 160)))
                        }
                    },
                    label = "override_section_slide",
                ) { currentSection ->
                    if (currentSection == null) {
                        Card {
                            sectionEntries.forEachIndexed { index, (title, summary) ->
                                ActionItem(
                                    title = title,
                                    summary = summary,
                                    onClick = {
                                        selectedSectionIndex.value = index
                                    },
                                )
                            }
                        }
                        return@AnimatedContent
                    }

                    Column {
                        Card {
                            ActionItem(
                                title = MLang.Component.Navigation.Back,
                                summary = MLang.Override.Nav.NodePageSwitch,
                                onClick = { selectedSectionIndex.value = null },
                            )
                        }

                        when (currentSection) {
                        0 -> {
                            Column {
                                SmallTitle(MLang.Override.Section.Inbound)
                                Card {
                                    PortInput(
                                        MLang.Override.General.HttpPort,
                                        configuration.httpPort,
                                        viewModel::setHttpPort
                                    )
                                    PortInput(
                                        MLang.Override.General.SocksPort,
                                        configuration.socksPort,
                                        viewModel::setSocksPort
                                    )
                                    PortInput(
                                        MLang.Override.General.MixedPort,
                                        configuration.mixedPort,
                                        viewModel::setMixedPort
                                    )
                                    PortInput(
                                        MLang.Override.General.RedirectPort,
                                        configuration.redirectPort,
                                        viewModel::setRedirectPort
                                    )
                                    PortInput(
                                        MLang.Override.General.TproxyPort,
                                        configuration.tproxyPort,
                                        viewModel::setTproxyPort
                                    )
                                    NullableBooleanSelector(
                                        title = MLang.Override.General.AllowLan,
                                        value = configuration.allowLan,
                                        onValueChange = viewModel::setAllowLan,
                                    )
                                }
                            }
                        }

                        1 -> {
                            Column {
                                SmallTitle(MLang.Override.Section.General)
                                Card {
                                    NullableBooleanSelector(
                                        title = MLang.Override.General.Ipv6,
                                        value = configuration.ipv6,
                                        onValueChange = viewModel::setIpv6,
                                    )
                                    NullableEnumSelector(
                                        title = MLang.Override.General.ProxyMode,
                                        value = configuration.mode,
                                        items = listOf(
                                            MLang.Component.Selector.NotModify,
                                            MLang.Proxy.Mode.Direct,
                                            MLang.Proxy.Mode.Global,
                                            MLang.Proxy.Mode.Rule,
                                        ),
                                        values = listOf(
                                            null,
                                            TunnelState.Mode.Direct,
                                            TunnelState.Mode.Global,
                                            TunnelState.Mode.Rule,
                                        ),
                                        onValueChange = viewModel::setMode,
                                    )
                                    NullableEnumSelector(
                                        title = MLang.Override.General.LogLevel,
                                        value = configuration.logLevel,
                                        items = listOf(
                                            MLang.Component.Selector.NotModify,
                                            "Info",
                                            "Warning",
                                            "Error",
                                            "Debug",
                                            "Silent",
                                        ),
                                        values = listOf(
                                            null,
                                            LogMessage.Level.Info,
                                            LogMessage.Level.Warning,
                                            LogMessage.Level.Error,
                                            LogMessage.Level.Debug,
                                            LogMessage.Level.Silent,
                                        ),
                                        onValueChange = viewModel::setLogLevel,
                                    )
                                    StringInput(
                                        title = MLang.Override.Label.KeepAliveInterval,
                                        value = configuration.keepAliveInterval?.toString(),
                                        placeholder = "秒",
                                        onValueChange = { viewModel.setKeepAliveInterval(it?.toIntOrNull()) },
                                    )
                                    StringInput(
                                        title = MLang.Override.Label.KeepAliveIdle,
                                        value = configuration.keepAliveIdle?.toString(),
                                        placeholder = "秒",
                                        onValueChange = { viewModel.setKeepAliveIdle(it?.toIntOrNull()) },
                                    )
                                }
                            }
                        }

                        2 -> {
                            Column {
                                SmallTitle(MLang.Override.Section.Dns)
                                Card {
                                    NullableEnumSelector(
                                        title = MLang.Override.Dns.Policy,
                                        value = configuration.dns.enable,
                                        items = listOf(
                                            MLang.Override.Dns.PolicyNotModify,
                                            MLang.Override.Dns.PolicyForceEnable,
                                            MLang.Override.Dns.PolicyUseBuiltin,
                                        ),
                                        values = listOf(null, true, false),
                                        onValueChange = viewModel::setDnsEnable,
                                    )
                                    Column {
                                            NullableBooleanSelector(
                                                title = MLang.Override.Dns.PreferH3,
                                                value = configuration.dns.preferH3,
                                                onValueChange = viewModel::setDnsPreferH3,
                                            )
                                            StringInput(
                                                title = MLang.Override.Dns.Listen,
                                                value = configuration.dns.listen,
                                                placeholder = MLang.Override.Dns.ListenHint,
                                                onValueChange = viewModel::setDnsListen,
                                            )
                                            NullableBooleanSelector(
                                                title = MLang.Override.Dns.Ipv6,
                                                value = configuration.dns.ipv6,
                                                onValueChange = viewModel::setDnsIpv6,
                                            )
                                            NullableBooleanSelector(
                                                title = MLang.Override.Dns.UseHosts,
                                                value = configuration.dns.useHosts,
                                                onValueChange = viewModel::setDnsUseHosts,
                                            )
                                            NullableBooleanSelector(
                                                title = MLang.Override.Label.UseSystemHosts,
                                                value = configuration.dns.useSystemHosts,
                                                onValueChange = viewModel::setDnsUseSystemHosts,
                                            )
                                            NullableBooleanSelector(
                                                title = MLang.Override.Label.RespectRules,
                                                value = configuration.dns.respectRules,
                                                onValueChange = viewModel::setDnsRespectRules,
                                            )
                                            NullableBooleanSelector(
                                                title = MLang.Override.Dns.AppendSystem,
                                                value = configuration.app.appendSystemDns,
                                                onValueChange = viewModel::setAppendSystemDns,
                                            )
                                            StringInput(
                                                title = MLang.Override.Label.CacheAlgorithm,
                                                value = configuration.dns.cacheAlgorithm,
                                                placeholder = "lru / arc",
                                                onValueChange = viewModel::setDnsCacheAlgorithm,
                                            )
                                            NullableEnumSelector(
                                                title = MLang.Override.Dns.EnhancedMode,
                                                value = configuration.dns.enhancedMode,
                                                items = listOf(
                                                    MLang.Override.Dns.EnhancedNotModify,
                                                    MLang.Override.Dns.EnhancedDisable,
                                                    MLang.Override.Dns.EnhancedFakeip,
                                                    MLang.Override.Dns.EnhancedMapping,
                                                ),
                                                values = listOf(
                                                    null,
                                                    ConfigurationOverride.DnsEnhancedMode.None,
                                                    ConfigurationOverride.DnsEnhancedMode.FakeIp,
                                                    ConfigurationOverride.DnsEnhancedMode.Mapping,
                                                ),
                                                onValueChange = viewModel::setDnsEnhancedMode,
                                            )
                                            StringListInput(
                                                MLang.Override.Dns.Servers,
                                                configuration.dns.nameServer,
                                                MLang.Override.Dns.ServersHint,
                                                viewModel::setDnsNameServer
                                            )
                                            StringListInput(
                                                MLang.Override.Dns.Fallback,
                                                configuration.dns.fallback,
                                                MLang.Override.Dns.FallbackHint,
                                                viewModel::setDnsFallback
                                            )
                                            StringListInput(
                                                MLang.Override.Dns.Default,
                                                configuration.dns.defaultServer,
                                                MLang.Override.Dns.DefaultHint,
                                                viewModel::setDnsDefaultServer
                                            )
                                            StringListInput(
                                                MLang.Override.Dns.FakeipFilter,
                                                configuration.dns.fakeIpFilter,
                                                MLang.Override.Dns.FakeipFilterHint,
                                                viewModel::setDnsFakeIpFilter
                                            )
                                            StringInput(
                                                MLang.Override.Label.FakeIpRange,
                                                configuration.dns.fakeIpRange,
                                                "198.18.0.1/16",
                                                viewModel::setDnsFakeIpRange
                                            )
                                            NullableEnumSelector(
                                                title = MLang.Override.Dns.FakeipFilterMode,
                                                value = configuration.dns.fakeIPFilterMode,
                                                items = listOf(
                                                    MLang.Override.Dns.EnhancedNotModify,
                                                    MLang.Override.Dns.FakeipBlacklist,
                                                    MLang.Override.Dns.FakeipWhitelist,
                                                    "Rule",
                                                ),
                                                values = listOf(
                                                    null,
                                                    ConfigurationOverride.FilterMode.BlackList,
                                                    ConfigurationOverride.FilterMode.WhiteList,
                                                    ConfigurationOverride.FilterMode.Rule,
                                                ),
                                                onValueChange = viewModel::setDnsFakeIpFilterMode,
                                            )
                                            NullableBooleanSelector(
                                                MLang.Override.Dns.FallbackGeoip,
                                                value = configuration.dns.fallbackFilter.geoIp,
                                                onValueChange = viewModel::setDnsFallbackGeoIp
                                            )
                                            StringInput(
                                                MLang.Override.Dns.FallbackGeoipCode,
                                                configuration.dns.fallbackFilter.geoIpCode,
                                                MLang.Override.Dns.FallbackGeoipCodeHint,
                                                viewModel::setDnsFallbackGeoIpCode
                                            )
                                            StringListInput(
                                                MLang.Override.Dns.FallbackDomain,
                                                configuration.dns.fallbackFilter.domain,
                                                MLang.Override.Dns.FallbackDomainHint,
                                                viewModel::setDnsFallbackDomain
                                            )
                                            StringListInput(
                                                MLang.Override.Dns.FallbackIpcidr,
                                                configuration.dns.fallbackFilter.ipcidr,
                                                MLang.Override.Dns.FallbackIpcidrHint,
                                                viewModel::setDnsFallbackIpcidr
                                            )
                                            StringListInput(
                                                MLang.Override.Label.FallbackGeosite,
                                                configuration.dns.fallbackFilter.geosite,
                                                "geolocation-!cn",
                                                viewModel::setDnsFallbackGeosite
                                            )
                                            StringListInput(
                                                MLang.Override.Label.ProxyServerNameserver,
                                                configuration.dns.proxyServerNameserver,
                                                "https://1.1.1.1/dns-query",
                                                viewModel::setDnsProxyServerNameserver
                                            )
                                            StringMapInput(
                                                title = MLang.Override.Dns.NameserverPolicy,
                                                value = configuration.dns.nameserverPolicy,
                                                keyPlaceholder = MLang.Override.Dns.NameserverPolicyKey,
                                                valuePlaceholder = MLang.Override.Dns.NameserverPolicyValue,
                                                onValueChange = viewModel::setDnsNameserverPolicy,
                                            )
                                    }
                                }
                            }
                        }

                        3 -> {
                            Column {
                                SmallTitle(MLang.Override.Section.Sniffer)
                                Card {
                                    NullableBooleanSelector(
                                        title = MLang.Override.Label.Enable,
                                        value = configuration.sniffer.enable,
                                        onValueChange = viewModel::setSnifferEnable,
                                    )
                                    Column {
                                            NullableBooleanSelector(
                                                title = MLang.Override.Label.ForceDnsMapping,
                                                value = configuration.sniffer.forceDnsMapping,
                                                onValueChange = viewModel::setSnifferForceDnsMapping,
                                            )
                                            NullableBooleanSelector(
                                                title = MLang.Override.Label.ParsePureIp,
                                                value = configuration.sniffer.parsePureIp,
                                                onValueChange = viewModel::setSnifferParsePureIp,
                                            )
                                            NullableBooleanSelector(
                                                title = MLang.Override.Label.OverrideDestination,
                                                value = configuration.sniffer.overrideDestination,
                                                onValueChange = viewModel::setSnifferOverrideDestination,
                                            )
                                            StringListInput(
                                                MLang.Override.Label.HttpPorts,
                                                configuration.sniffer.sniff.http.ports,
                                                "80,8080-8880",
                                                viewModel::setSnifferHttpPorts
                                            )
                                            NullableBooleanSelector(
                                                title = MLang.Override.Label.HttpOverride,
                                                value = configuration.sniffer.sniff.http.overrideDestination,
                                                onValueChange = viewModel::setSnifferHttpOverride,
                                            )
                                            StringListInput(
                                                MLang.Override.Label.TlsPorts,
                                                configuration.sniffer.sniff.tls.ports,
                                                "443,8443",
                                                viewModel::setSnifferTlsPorts
                                            )
                                            NullableBooleanSelector(
                                                title = MLang.Override.Label.TlsOverride,
                                                value = configuration.sniffer.sniff.tls.overrideDestination,
                                                onValueChange = viewModel::setSnifferTlsOverride,
                                            )
                                            StringListInput(
                                                MLang.Override.Label.QuicPorts,
                                                configuration.sniffer.sniff.quic.ports,
                                                "443,8443",
                                                viewModel::setSnifferQuicPorts
                                            )
                                            NullableBooleanSelector(
                                                title = MLang.Override.Label.QuicOverride,
                                                value = configuration.sniffer.sniff.quic.overrideDestination,
                                                onValueChange = viewModel::setSnifferQuicOverride,
                                            )
                                            StringListInput(
                                                MLang.Override.Label.ForceDomain,
                                                configuration.sniffer.forceDomain,
                                                "+.v2ex.com",
                                                viewModel::setSnifferForceDomain
                                            )
                                            StringListInput(
                                                MLang.Override.Label.SkipDomain,
                                                configuration.sniffer.skipDomain,
                                                "Mijia Cloud",
                                                viewModel::setSnifferSkipDomain
                                            )
                                            StringListInput(
                                                MLang.Override.Label.SkipSrcAddress,
                                                configuration.sniffer.skipSrcAddress,
                                                "192.168.0.0/16",
                                                viewModel::setSnifferSkipSrcAddress
                                            )
                                            StringListInput(
                                                MLang.Override.Label.SkipDstAddress,
                                                configuration.sniffer.skipDstAddress,
                                                "192.168.0.0/16",
                                                viewModel::setSnifferSkipDstAddress
                                            )
                                    }
                                }
                            }
                        }

                        4 -> {
                            Column {
                                SmallTitle(MLang.Override.Section.Rules)
                                Card {
                                    StringListInput(
                                        MLang.Override.Label.PrependRules,
                                        configuration.prependRules,
                                        "DOMAIN-SUFFIX,openai.com,PROXY",
                                        viewModel::setPrependRules
                                    )
                                    StringListInput(
                                        MLang.Override.Label.RulesReplace,
                                        configuration.rules,
                                        "MATCH,DIRECT",
                                        viewModel::setRules
                                    )
                                }
                            }
                        }

                        5 -> {
                            Column {
                                SmallTitle(MLang.Override.Section.Tools)
                                Card {
                                    NullableEnumSelector(
                                        title = MLang.Override.Tool.ImportModeTitle,
                                        value = importMode.value,
                                        items = listOf(
                                            MLang.Override.Tool.ImportModeAppend,
                                            MLang.Override.Tool.ImportModeOverride,
                                        ),
                                        values = listOf(
                                            OverrideViewModel.ImportMode.Merge,
                                            OverrideViewModel.ImportMode.Replace,
                                        ),
                                        onValueChange = { mode ->
                                            if (mode != null) importMode.value = mode
                                        },
                                    )

                                    ActionItem(
                                        title = MLang.Override.Tool.ImportClipboardSimple,
                                        summary = MLang.Override.Tool.ImportClipboardSimpleSummary,
                                    ) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val json = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                        val result = viewModel.importFromJson(json, importMode.value)
                                        Toast.makeText(
                                            context,
                                            if (result.isSuccess) MLang.Override.Message.ImportSuccess
                                            else MLang.Override.Message.ImportFailed.format(result.exceptionOrNull()?.message ?: "unknown"),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    ActionItem(
                                        title = MLang.Override.Tool.ImportFileSimple,
                                        summary = MLang.Override.Tool.ImportFileSimpleSummary,
                                    ) {
                                        importFileLauncher.launch("*/*")
                                    }
                                    ActionItem(
                                        title = MLang.Override.Tool.ExportClipboardSimple,
                                        summary = MLang.Override.Tool.ExportClipboardSimpleSummary,
                                    ) {
                                        val json = viewModel.exportToJson()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("override.json", json))
                                        Toast.makeText(context, MLang.Override.Message.Copied, Toast.LENGTH_SHORT).show()
                                    }
                                    ActionItem(
                                        title = MLang.Override.Tool.ExportFileSimple,
                                        summary = MLang.Override.Tool.ExportFileSimpleSummary,
                                    ) {
                                        pendingExportJson.value = viewModel.exportToJson()
                                        exportFileLauncher.launch("override-routes.json")
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    if (showResetDialog.value) {
        WindowDialog(
        title = MLang.Override.ResetDialog.Title,
        summary = MLang.Override.ResetDialog.Message,
        show = remember { mutableStateOf(true) },
        onDismissRequest = { showResetDialog.value = false },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { showResetDialog.value = false },
                modifier = Modifier.weight(1f),
            ) {
                Text(MLang.Component.Button.Cancel)
            }
            Button(
                onClick = {
                    viewModel.resetConfiguration()
                    showResetDialog.value = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    MLang.Component.Button.Confirm,
                    color = MiuixTheme.colorScheme.surface,
                )
            }
        }
    }
    }
}
