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

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.TunnelState
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField

typealias OpenStringListEditor = (
    title: String,
    placeholder: String,
    value: List<String>?,
    onValueChange: (List<String>?) -> Unit,
) -> Unit

typealias OpenStringMapEditor = (
    title: String,
    keyPlaceholder: String,
    valuePlaceholder: String,
    value: Map<String, String>?,
    onValueChange: (Map<String, String>?) -> Unit,
) -> Unit

typealias OpenJsonEditor = (
    title: String,
    placeholder: String,
    value: String?,
    onValueChange: (String?) -> Unit,
) -> Unit

@Composable
private fun OverrideTextInputContent(
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
private fun OverrideIntInputContent(
    title: String,
    value: Int?,
    placeholder: String,
    onValueChange: (Int?) -> Unit,
) {
    val showDialog = remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(value?.toString().orEmpty()) }

    top.yukonga.miuix.kmp.extra.SuperArrow(
        title = title,
        summary = value?.toString() ?: MLang.Component.Selector.NotModify,
        onClick = {
            textValue = value?.toString().orEmpty()
            showDialog.value = true
        },
    )

    AppDialog(
        show = showDialog.value,
        title = title,
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextField(
                value = textValue,
                onValueChange = { updatedValue ->
                    textValue = updatedValue.filter { it.isDigit() || it == '-' }
                },
                label = placeholder,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        onValueChange(null)
                        showDialog.value = false
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(MLang.Component.Button.Clear)
                }
                Button(
                    onClick = {
                        onValueChange(textValue.takeIf(String::isNotBlank)?.toIntOrNull())
                        showDialog.value = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(
                        text = MLang.Component.Button.Confirm,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverridePortInputContent(
    title: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
) {
    PortInputContent(
        title = title,
        value = value,
        onValueChange = onValueChange,
    )
}

@Composable
fun InboundEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
    ) {
        OverrideFormSection("代理端口") {
            OverridePortInputContent(
                title = MLang.Override.General.HttpPort,
                value = config.httpPort,
                onValueChange = { onConfigChange(config.copy(httpPort = it)) },
            )
            OverridePortInputContent(
                title = MLang.Override.General.SocksPort,
                value = config.socksPort,
                onValueChange = { onConfigChange(config.copy(socksPort = it)) },
            )
            OverridePortInputContent(
                title = MLang.Override.General.MixedPort,
                value = config.mixedPort,
                onValueChange = { onConfigChange(config.copy(mixedPort = it)) },
            )
            OverridePortInputContent(
                title = MLang.Override.General.RedirectPort,
                value = config.redirectPort,
                onValueChange = { onConfigChange(config.copy(redirectPort = it)) },
            )
            OverridePortInputContent(
                title = MLang.Override.General.TproxyPort,
                value = config.tproxyPort,
                onValueChange = { onConfigChange(config.copy(tproxyPort = it)) },
            )
        }
    }
}

@Composable
fun GeneralEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
    ) {
        OverrideCardSection("运行与日志") {
            NullableEnumSelector(
                title = MLang.Override.General.ProxyMode,
                value = config.mode,
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
                onValueChange = { onConfigChange(config.copy(mode = it)) },
            )
            NullableBooleanSelector(
                title = MLang.Override.General.Ipv6,
                value = config.ipv6,
                onValueChange = { onConfigChange(config.copy(ipv6 = it)) },
            )
            NullableEnumSelector(
                title = MLang.Override.General.LogLevel,
                value = config.logLevel,
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
                onValueChange = { onConfigChange(config.copy(logLevel = it)) },
            )
            NullableEnumSelector(
                title = "进程匹配模式",
                value = config.findProcessMode,
                items = listOf("不修改", "Always", "Strict", "Off"),
                values = listOf(
                    null,
                    ConfigurationOverride.FindProcessMode.Always,
                    ConfigurationOverride.FindProcessMode.Strict,
                    ConfigurationOverride.FindProcessMode.Off,
                ),
                onValueChange = { onConfigChange(config.copy(findProcessMode = it)) },
            )
            NullableBooleanSelector(
                title = "统一延迟",
                value = config.unifiedDelay,
                onValueChange = { onConfigChange(config.copy(unifiedDelay = it)) },
            )
            NullableBooleanSelector(
                title = "TCP 并发",
                value = config.tcpConcurrent,
                onValueChange = { onConfigChange(config.copy(tcpConcurrent = it)) },
            )
            NullableBooleanSelector(
                title = "Geodata 模式",
                value = config.geodataMode,
                onValueChange = { onConfigChange(config.copy(geodataMode = it)) },
            )
        }

        OverrideFormSection("运行与日志补充") {
            OverrideIntInputContent(
                title = MLang.Override.Label.KeepAliveInterval,
                value = config.keepAliveInterval,
                placeholder = "秒",
                onValueChange = { onConfigChange(config.copy(keepAliveInterval = it)) },
            )
            OverrideIntInputContent(
                title = MLang.Override.Label.KeepAliveIdle,
                value = config.keepAliveIdle,
                placeholder = "秒",
                onValueChange = { onConfigChange(config.copy(keepAliveIdle = it)) },
            )
        }

        OverrideFormSection("连接与网络") {
            OverrideTextInputContent(
                title = "出站接口",
                value = config.interfaceName,
                placeholder = "en0 / wlan0",
                onValueChange = { onConfigChange(config.copy(interfaceName = it)) },
            )
            OverrideIntInputContent(
                title = "路由标记",
                value = config.routingMark,
                placeholder = "6666",
                onValueChange = { onConfigChange(config.copy(routingMark = it)) },
            )
            OverrideTextInputContent(
                title = "Geosite 匹配器",
                value = config.geositeMatcher,
                placeholder = "standard / succinct",
                onValueChange = { onConfigChange(config.copy(geositeMatcher = it)) },
            )
            OverrideTextInputContent(
                title = "全局客户端指纹",
                value = config.globalClientFingerprint,
                placeholder = "chrome / safari",
                onValueChange = { onConfigChange(config.copy(globalClientFingerprint = it)) },
            )
        }

        OverrideCardSection("局域网访问") {
            NullableBooleanSelector(
                title = MLang.Override.General.AllowLan,
                value = config.allowLan,
                onValueChange = { onConfigChange(config.copy(allowLan = it)) },
            )
            StringListWithModifiersInput(
                title = "允许 IP 段",
                replaceValue = config.lanAllowedIps,
                startValue = config.lanAllowedIpsStart,
                endValue = config.lanAllowedIpsEnd,
                placeholder = "0.0.0.0/0",
                onReplaceChange = { onConfigChange(config.copy(lanAllowedIps = it)) },
                onStartChange = { onConfigChange(config.copy(lanAllowedIpsStart = it)) },
                onEndChange = { onConfigChange(config.copy(lanAllowedIpsEnd = it)) },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "禁止 IP 段",
                replaceValue = config.lanDisallowedIps,
                startValue = config.lanDisallowedIpsStart,
                endValue = config.lanDisallowedIpsEnd,
                placeholder = "192.168.0.3/32",
                onReplaceChange = { onConfigChange(config.copy(lanDisallowedIps = it)) },
                onStartChange = { onConfigChange(config.copy(lanDisallowedIpsStart = it)) },
                onEndChange = { onConfigChange(config.copy(lanDisallowedIpsEnd = it)) },
                onEditListGroup = onEditStringList,
            )
        }

        OverrideFormSection("局域网地址") {
            OverrideTextInputContent(
                title = "绑定地址",
                value = config.bindAddress,
                placeholder = "* / 192.168.1.1 / [::1]",
                onValueChange = { onConfigChange(config.copy(bindAddress = it)) },
            )
        }

        OverrideCardSection("用户验证") {
            StringListWithModifiersInput(
                title = "用户验证",
                replaceValue = config.authentication,
                startValue = config.authenticationStart,
                endValue = config.authenticationEnd,
                placeholder = "user:password",
                onReplaceChange = { onConfigChange(config.copy(authentication = it)) },
                onStartChange = { onConfigChange(config.copy(authenticationStart = it)) },
                onEndChange = { onConfigChange(config.copy(authenticationEnd = it)) },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "跳过鉴权网段",
                replaceValue = config.skipAuthPrefixes,
                startValue = config.skipAuthPrefixesStart,
                endValue = config.skipAuthPrefixesEnd,
                placeholder = "127.0.0.1/8",
                onReplaceChange = { onConfigChange(config.copy(skipAuthPrefixes = it)) },
                onStartChange = { onConfigChange(config.copy(skipAuthPrefixesStart = it)) },
                onEndChange = { onConfigChange(config.copy(skipAuthPrefixesEnd = it)) },
                onEditListGroup = onEditStringList,
            )
        }

        OverrideFormSection("外部控制") {
            OverrideTextInputContent(
                title = "外部控制器",
                value = config.externalController,
                placeholder = "127.0.0.1:9090",
                onValueChange = { onConfigChange(config.copy(externalController = it)) },
            )
            OverrideTextInputContent(
                title = "HTTPS 控制器",
                value = config.externalControllerTLS,
                placeholder = "127.0.0.1:9443",
                onValueChange = { onConfigChange(config.copy(externalControllerTLS = it)) },
            )
            OverrideTextInputContent(
                title = "外部 DoH 服务",
                value = config.externalDohServer,
                placeholder = "/dns-query",
                onValueChange = { onConfigChange(config.copy(externalDohServer = it)) },
            )
            OverrideTextInputContent(
                title = "Secret",
                value = config.secret,
                placeholder = "API 访问密钥",
                onValueChange = { onConfigChange(config.copy(secret = it)) },
            )
        }

        OverrideCardSection("控制器 CORS") {
            StringListWithModifiersInput(
                title = "CORS Allow Origins",
                replaceValue = config.externalControllerCors.allowOrigins,
                startValue = config.externalControllerCors.allowOriginsStart,
                endValue = config.externalControllerCors.allowOriginsEnd,
                placeholder = "*",
                onReplaceChange = {
                    onConfigChange(
                        config.copy(
                            externalControllerCors = config.externalControllerCors.copy(
                                allowOrigins = it,
                            ),
                        ),
                    )
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(
                            externalControllerCors = config.externalControllerCors.copy(
                                allowOriginsStart = it,
                            ),
                        ),
                    )
                },
                onEndChange = {
                    onConfigChange(
                        config.copy(
                            externalControllerCors = config.externalControllerCors.copy(
                                allowOriginsEnd = it,
                            ),
                        ),
                    )
                },
                onEditListGroup = onEditStringList,
            )
            NullableBooleanSelector(
                title = "允许私有网络",
                value = config.externalControllerCors.allowPrivateNetwork,
                onValueChange = {
                    onConfigChange(
                        config.copy(
                            externalControllerCors = config.externalControllerCors.copy(
                                allowPrivateNetwork = it,
                            ),
                        ),
                    )
                },
            )
        }

        OverrideCardSection("配置持久化") {
            NullableBooleanSelector(
                title = "保存策略组选择",
                value = config.profile.storeSelected,
                onValueChange = {
                    onConfigChange(config.copy(profile = config.profile.copy(storeSelected = it)))
                },
            )
            NullableBooleanSelector(
                title = "保存 Fake-IP 映射",
                value = config.profile.storeFakeIp,
                onValueChange = {
                    onConfigChange(config.copy(profile = config.profile.copy(storeFakeIp = it)))
                },
            )
        }

        OverrideCardSection("GEO 资源开关") {
            NullableBooleanSelector(
                title = "自动更新 GEO",
                value = config.geoAutoUpdate,
                onValueChange = { onConfigChange(config.copy(geoAutoUpdate = it)) },
            )
        }

        OverrideFormSection("GEO 资源地址") {
            OverrideIntInputContent(
                title = "GEO 更新间隔",
                value = config.geoUpdateInterval,
                placeholder = "小时",
                onValueChange = { onConfigChange(config.copy(geoUpdateInterval = it)) },
            )
            OverrideTextInputContent(
                title = "GeoIP 地址",
                value = config.geoxurl.geoip,
                placeholder = "https://...",
                onValueChange = {
                    onConfigChange(config.copy(geoxurl = config.geoxurl.copy(geoip = it)))
                },
            )
            OverrideTextInputContent(
                title = "GeoSite 地址",
                value = config.geoxurl.geosite,
                placeholder = "https://...",
                onValueChange = {
                    onConfigChange(config.copy(geoxurl = config.geoxurl.copy(geosite = it)))
                },
            )
            OverrideTextInputContent(
                title = "MMDB 地址",
                value = config.geoxurl.mmdb,
                placeholder = "https://...",
                onValueChange = {
                    onConfigChange(config.copy(geoxurl = config.geoxurl.copy(mmdb = it)))
                },
            )
        }
    }
}

@Composable
fun TunEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
    ) {
        OverrideCardSection("基础开关") {
            NullableBooleanSelector(
                title = MLang.Override.Label.Enable,
                value = config.tun.enable,
                onValueChange = { onConfigChange(config.copy(tun = config.tun.copy(enable = it))) },
            )
            NullableEnumSelector(
                title = "协议栈",
                value = config.tun.stack,
                items = listOf("不修改", "system", "gvisor", "mixed"),
                values = listOf(null, "system", "gvisor", "mixed"),
                onValueChange = { onConfigChange(config.copy(tun = config.tun.copy(stack = it))) },
            )
            NullableBooleanSelector(
                title = "自动路由",
                value = config.tun.autoRoute,
                onValueChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(autoRoute = it)))
                },
            )
            NullableBooleanSelector(
                title = "自动重定向",
                value = config.tun.autoRedirect,
                onValueChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(autoRedirect = it)))
                },
            )
            NullableBooleanSelector(
                title = "自动识别网卡",
                value = config.tun.autoDetectInterface,
                onValueChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(autoDetectInterface = it)))
                },
            )
            NullableBooleanSelector(
                title = "严格路由",
                value = config.tun.strictRoute,
                onValueChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(strictRoute = it)))
                },
            )
            NullableBooleanSelector(
                title = "独立于端点 NAT",
                value = config.tun.endpointIndependentNat,
                onValueChange = {
                    onConfigChange(
                        config.copy(tun = config.tun.copy(endpointIndependentNat = it)),
                    )
                },
            )
        }

        OverrideCardSection("网络性能开关") {
            NullableBooleanSelector(
                title = "启用 GSO",
                value = config.tun.gso,
                onValueChange = { onConfigChange(config.copy(tun = config.tun.copy(gso = it))) },
            )
            NullableBooleanSelector(
                title = "禁用 ICMP 转发",
                value = config.tun.disableIcmpForwarding,
                onValueChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(disableIcmpForwarding = it)))
                },
            )
        }

        OverrideFormSection("网络性能参数") {
            OverrideIntInputContent(
                title = "MTU",
                value = config.tun.mtu,
                placeholder = "9000",
                onValueChange = { onConfigChange(config.copy(tun = config.tun.copy(mtu = it))) },
            )
            OverrideIntInputContent(
                title = "GSO 最大长度",
                value = config.tun.gsoMaxSize,
                placeholder = "65536",
                onValueChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(gsoMaxSize = it)))
                },
            )
        }

        OverrideCardSection("路由与应用") {
            StringListWithModifiersInput(
                title = "DNS 劫持",
                replaceValue = config.tun.dnsHijack,
                startValue = config.tun.dnsHijackStart,
                endValue = config.tun.dnsHijackEnd,
                placeholder = "any:53",
                onReplaceChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(dnsHijack = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(dnsHijackStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(dnsHijackEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "路由网段",
                replaceValue = config.tun.routeAddress,
                startValue = config.tun.routeAddressStart,
                endValue = config.tun.routeAddressEnd,
                placeholder = "0.0.0.0/1",
                onReplaceChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(routeAddress = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(routeAddressStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(routeAddressEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "排除路由网段",
                replaceValue = config.tun.routeExcludeAddress,
                startValue = config.tun.routeExcludeAddressStart,
                endValue = config.tun.routeExcludeAddressEnd,
                placeholder = "192.168.0.0/16",
                onReplaceChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(routeExcludeAddress = it)))
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(tun = config.tun.copy(routeExcludeAddressStart = it)),
                    )
                },
                onEndChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(routeExcludeAddressEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "包含应用",
                replaceValue = config.tun.includePackage,
                startValue = config.tun.includePackageStart,
                endValue = config.tun.includePackageEnd,
                placeholder = "com.android.chrome",
                onReplaceChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(includePackage = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(includePackageStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(includePackageEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "排除应用",
                replaceValue = config.tun.excludePackage,
                startValue = config.tun.excludePackageStart,
                endValue = config.tun.excludePackageEnd,
                placeholder = "com.android.captiveportallogin",
                onReplaceChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(excludePackage = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(excludePackageStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(tun = config.tun.copy(excludePackageEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
        }
    }
}

@Composable
fun DnsEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
    onEditStringMap: OpenStringMapEditor,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
    ) {
        OverrideCardSection("基础开关") {
            NullableEnumSelector(
                title = MLang.Override.Dns.Policy,
                value = config.dns.enable,
                items = listOf(
                    MLang.Override.Dns.PolicyNotModify,
                    MLang.Override.Dns.PolicyForceEnable,
                    MLang.Override.Dns.PolicyUseBuiltin,
                ),
                values = listOf(null, true, false),
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(enable = it)))
                },
            )
            NullableBooleanSelector(
                title = MLang.Override.Dns.PreferH3,
                value = config.dns.preferH3,
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(preferH3 = it)))
                },
            )
            NullableBooleanSelector(
                title = MLang.Override.Dns.Ipv6,
                value = config.dns.ipv6,
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(ipv6 = it)))
                },
            )
            NullableBooleanSelector(
                title = MLang.Override.Dns.UseHosts,
                value = config.dns.useHosts,
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(useHosts = it)))
                },
            )
            NullableBooleanSelector(
                title = MLang.Override.Label.UseSystemHosts,
                value = config.dns.useSystemHosts,
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(useSystemHosts = it)))
                },
            )
            NullableBooleanSelector(
                title = MLang.Override.Dns.AppendSystem,
                value = config.app.appendSystemDns,
                onValueChange = {
                    onConfigChange(config.copy(app = config.app.copy(appendSystemDns = it)))
                },
            )
            NullableBooleanSelector(
                title = MLang.Override.Label.RespectRules,
                value = config.dns.respectRules,
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(respectRules = it)))
                },
            )
            NullableEnumSelector(
                title = MLang.Override.Dns.EnhancedMode,
                value = config.dns.enhancedMode,
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
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(enhancedMode = it)))
                },
            )
            NullableBooleanSelector(
                title = "Direct 遵循 Policy",
                value = config.dns.directFollowPolicy,
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(directFollowPolicy = it)))
                },
            )
        }

        OverrideFormSection("DNS 基础参数") {
            OverrideTextInputContent(
                title = MLang.Override.Dns.Listen,
                value = config.dns.listen,
                placeholder = MLang.Override.Dns.ListenHint,
                onValueChange = { onConfigChange(config.copy(dns = config.dns.copy(listen = it))) },
            )
            OverrideTextInputContent(
                title = MLang.Override.Label.CacheAlgorithm,
                value = config.dns.cacheAlgorithm,
                placeholder = "lru / arc",
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(cacheAlgorithm = it)))
                },
            )
            OverrideIntInputContent(
                title = "IPv6 超时",
                value = config.dns.ipv6Timeout,
                placeholder = "100",
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(ipv6Timeout = it)))
                },
            )
            OverrideIntInputContent(
                title = "缓存上限",
                value = config.dns.cacheMaxSize,
                placeholder = "4096",
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(cacheMaxSize = it)))
                },
            )
        }

        OverrideCardSection("Fake-IP 模式") {
            NullableEnumSelector(
                title = MLang.Override.Dns.FakeipFilterMode,
                value = config.dns.fakeIPFilterMode,
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
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(fakeIPFilterMode = it)))
                },
            )
        }

        OverrideFormSection("Fake-IP 参数") {
            OverrideTextInputContent(
                title = MLang.Override.Label.FakeIpRange,
                value = config.dns.fakeIpRange,
                placeholder = "198.18.0.1/16",
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(fakeIpRange = it)))
                },
            )
            OverrideTextInputContent(
                title = "Fake-IP IPv6 网段",
                value = config.dns.fakeIpRange6,
                placeholder = "fdfe:dcba:9876::1/64",
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(fakeIpRange6 = it)))
                },
            )
            OverrideIntInputContent(
                title = "Fake-IP TTL",
                value = config.dns.fakeIpTtl,
                placeholder = "1",
                onValueChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(fakeIpTtl = it)))
                },
            )
        }

        OverrideCardSection("上游服务器") {
            StringListWithModifiersInput(
                title = MLang.Override.Dns.Servers,
                replaceValue = config.dns.nameServer,
                startValue = config.dns.nameServerStart,
                endValue = config.dns.nameServerEnd,
                placeholder = MLang.Override.Dns.ServersHint,
                onReplaceChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(nameServer = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(nameServerStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(nameServerEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = MLang.Override.Dns.Fallback,
                replaceValue = config.dns.fallback,
                startValue = config.dns.fallbackStart,
                endValue = config.dns.fallbackEnd,
                placeholder = MLang.Override.Dns.FallbackHint,
                onReplaceChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(fallback = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(fallbackStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(fallbackEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = MLang.Override.Dns.Default,
                replaceValue = config.dns.defaultServer,
                startValue = config.dns.defaultServerStart,
                endValue = config.dns.defaultServerEnd,
                placeholder = MLang.Override.Dns.DefaultHint,
                onReplaceChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(defaultServer = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(defaultServerStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(defaultServerEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "Proxy Server Nameserver",
                replaceValue = config.dns.proxyServerNameserver,
                startValue = config.dns.proxyServerNameserverStart,
                endValue = config.dns.proxyServerNameserverEnd,
                placeholder = "https://doh.pub/dns-query",
                onReplaceChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(proxyServerNameserver = it)))
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(dns = config.dns.copy(proxyServerNameserverStart = it)),
                    )
                },
                onEndChange = {
                    onConfigChange(
                        config.copy(dns = config.dns.copy(proxyServerNameserverEnd = it)),
                    )
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "Direct Nameserver",
                replaceValue = config.dns.directNameserver,
                startValue = config.dns.directNameserverStart,
                endValue = config.dns.directNameserverEnd,
                placeholder = "system",
                onReplaceChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(directNameserver = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(directNameserverStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(directNameserverEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
        }

        OverrideCardSection("策略映射") {
            StringMapWithModifiersInput(
                title = MLang.Override.Dns.NameserverPolicy,
                replaceValue = config.dns.nameserverPolicy,
                mergeValue = config.dns.nameserverPolicyMerge,
                keyPlaceholder = MLang.Override.Dns.NameserverPolicyKey,
                valuePlaceholder = MLang.Override.Dns.NameserverPolicyValue,
                onReplaceChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(nameserverPolicy = it)))
                },
                onMergeChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(nameserverPolicyMerge = it)))
                },
                onEditMap = { _, title, keyPlaceholder, valuePlaceholder, value, callback ->
                    onEditStringMap(title, keyPlaceholder, valuePlaceholder, value, callback)
                },
            )
            StringMapWithModifiersInput(
                title = "Proxy Server Nameserver Policy",
                replaceValue = config.dns.proxyServerNameserverPolicy,
                mergeValue = config.dns.proxyServerNameserverPolicyMerge,
                keyPlaceholder = "域名 / RuleSet",
                valuePlaceholder = "DNS 服务器",
                onReplaceChange = {
                    onConfigChange(
                        config.copy(dns = config.dns.copy(proxyServerNameserverPolicy = it)),
                    )
                },
                onMergeChange = {
                    onConfigChange(
                        config.copy(dns = config.dns.copy(proxyServerNameserverPolicyMerge = it)),
                    )
                },
                onEditMap = { _, title, keyPlaceholder, valuePlaceholder, value, callback ->
                    onEditStringMap(title, keyPlaceholder, valuePlaceholder, value, callback)
                },
            )
            StringMapWithModifiersInput(
                title = "Hosts",
                replaceValue = config.hosts,
                mergeValue = config.hostsMerge,
                keyPlaceholder = "domain",
                valuePlaceholder = "ip",
                onReplaceChange = { onConfigChange(config.copy(hosts = it)) },
                onMergeChange = { onConfigChange(config.copy(hostsMerge = it)) },
                onEditMap = { _, title, keyPlaceholder, valuePlaceholder, value, callback ->
                    onEditStringMap(title, keyPlaceholder, valuePlaceholder, value, callback)
                },
            )
        }

        OverrideCardSection("过滤列表") {
            StringListWithModifiersInput(
                title = MLang.Override.Dns.FakeipFilter,
                replaceValue = config.dns.fakeIpFilter,
                startValue = config.dns.fakeIpFilterStart,
                endValue = config.dns.fakeIpFilterEnd,
                placeholder = MLang.Override.Dns.FakeipFilterHint,
                onReplaceChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(fakeIpFilter = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(fakeIpFilterStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(dns = config.dns.copy(fakeIpFilterEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
        }

        OverrideCardSection("Fallback 开关") {
            NullableBooleanSelector(
                title = MLang.Override.Dns.FallbackGeoip,
                value = config.dns.fallbackFilter.geoIp,
                onValueChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(geoIp = it),
                            ),
                        ),
                    )
                },
            )
        }

        OverrideFormSection("Fallback 参数") {
            OverrideTextInputContent(
                title = MLang.Override.Dns.FallbackGeoipCode,
                value = config.dns.fallbackFilter.geoIpCode,
                placeholder = MLang.Override.Dns.FallbackGeoipCodeHint,
                onValueChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(geoIpCode = it),
                            ),
                        ),
                    )
                },
            )
        }

        OverrideCardSection("Fallback 过滤") {
            StringListWithModifiersInput(
                title = MLang.Override.Dns.FallbackDomain,
                replaceValue = config.dns.fallbackFilter.domain,
                startValue = config.dns.fallbackFilter.domainStart,
                endValue = config.dns.fallbackFilter.domainEnd,
                placeholder = MLang.Override.Dns.FallbackDomainHint,
                onReplaceChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(domain = it),
                            ),
                        ),
                    )
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(domainStart = it),
                            ),
                        ),
                    )
                },
                onEndChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(domainEnd = it),
                            ),
                        ),
                    )
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = MLang.Override.Dns.FallbackIpcidr,
                replaceValue = config.dns.fallbackFilter.ipcidr,
                startValue = config.dns.fallbackFilter.ipcidrStart,
                endValue = config.dns.fallbackFilter.ipcidrEnd,
                placeholder = MLang.Override.Dns.FallbackIpcidrHint,
                onReplaceChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(ipcidr = it),
                            ),
                        ),
                    )
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(ipcidrStart = it),
                            ),
                        ),
                    )
                },
                onEndChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(ipcidrEnd = it),
                            ),
                        ),
                    )
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "Fallback Geosite",
                replaceValue = config.dns.fallbackFilter.geosite,
                startValue = config.dns.fallbackFilter.geositeStart,
                endValue = config.dns.fallbackFilter.geositeEnd,
                placeholder = "gfw",
                onReplaceChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(geosite = it),
                            ),
                        ),
                    )
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(geositeStart = it),
                            ),
                        ),
                    )
                },
                onEndChange = {
                    onConfigChange(
                        config.copy(
                            dns = config.dns.copy(
                                fallbackFilter = config.dns.fallbackFilter.copy(geositeEnd = it),
                            ),
                        ),
                    )
                },
                onEditListGroup = onEditStringList,
            )
        }
    }
}

@Composable
fun SnifferEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
    ) {
        OverrideCardSection("基础策略") {
            NullableBooleanSelector(
                title = MLang.Override.Label.Enable,
                value = config.sniffer.enable,
                onValueChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(enable = it)))
                },
            )
            NullableBooleanSelector(
                title = MLang.Override.Label.ForceDnsMapping,
                value = config.sniffer.forceDnsMapping,
                onValueChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(forceDnsMapping = it)))
                },
            )
            NullableBooleanSelector(
                title = MLang.Override.Label.ParsePureIp,
                value = config.sniffer.parsePureIp,
                onValueChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(parsePureIp = it)))
                },
            )
            NullableBooleanSelector(
                title = MLang.Override.Label.OverrideDestination,
                value = config.sniffer.overrideDestination,
                onValueChange = {
                    onConfigChange(
                        config.copy(sniffer = config.sniffer.copy(overrideDestination = it)),
                    )
                },
            )
        }
        OverrideCardSection("HTTP") {
            StringListWithModifiersInput(
                title = "HTTP 端口",
                replaceValue = config.sniffer.sniff.http.ports,
                startValue = config.sniffer.sniff.http.portsStart,
                endValue = config.sniffer.sniff.http.portsEnd,
                placeholder = "80,8080-8880",
                onReplaceChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    http = config.sniffer.sniff.http.copy(ports = it),
                                ),
                            ),
                        ),
                    )
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    http = config.sniffer.sniff.http.copy(portsStart = it),
                                ),
                            ),
                        ),
                    )
                },
                onEndChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    http = config.sniffer.sniff.http.copy(portsEnd = it),
                                ),
                            ),
                        ),
                    )
                },
                onEditListGroup = onEditStringList,
            )
            NullableBooleanSelector(
                title = MLang.Override.Label.HttpOverride,
                value = config.sniffer.sniff.http.overrideDestination,
                onValueChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    http = config.sniffer.sniff.http.copy(
                                        overrideDestination = it,
                                    ),
                                ),
                            ),
                        ),
                    )
                },
            )
        }
        OverrideCardSection("TLS") {
            StringListWithModifiersInput(
                title = "TLS 端口",
                replaceValue = config.sniffer.sniff.tls.ports,
                startValue = config.sniffer.sniff.tls.portsStart,
                endValue = config.sniffer.sniff.tls.portsEnd,
                placeholder = "443,8443",
                onReplaceChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    tls = config.sniffer.sniff.tls.copy(ports = it),
                                ),
                            ),
                        ),
                    )
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    tls = config.sniffer.sniff.tls.copy(portsStart = it),
                                ),
                            ),
                        ),
                    )
                },
                onEndChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    tls = config.sniffer.sniff.tls.copy(portsEnd = it),
                                ),
                            ),
                        ),
                    )
                },
                onEditListGroup = onEditStringList,
            )
            NullableBooleanSelector(
                title = MLang.Override.Label.TlsOverride,
                value = config.sniffer.sniff.tls.overrideDestination,
                onValueChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    tls = config.sniffer.sniff.tls.copy(
                                        overrideDestination = it,
                                    ),
                                ),
                            ),
                        ),
                    )
                },
            )
        }
        OverrideCardSection("QUIC") {
            StringListWithModifiersInput(
                title = "QUIC 端口",
                replaceValue = config.sniffer.sniff.quic.ports,
                startValue = config.sniffer.sniff.quic.portsStart,
                endValue = config.sniffer.sniff.quic.portsEnd,
                placeholder = "443,8443",
                onReplaceChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    quic = config.sniffer.sniff.quic.copy(ports = it),
                                ),
                            ),
                        ),
                    )
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    quic = config.sniffer.sniff.quic.copy(portsStart = it),
                                ),
                            ),
                        ),
                    )
                },
                onEndChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    quic = config.sniffer.sniff.quic.copy(portsEnd = it),
                                ),
                            ),
                        ),
                    )
                },
                onEditListGroup = onEditStringList,
            )
            NullableBooleanSelector(
                title = MLang.Override.Label.QuicOverride,
                value = config.sniffer.sniff.quic.overrideDestination,
                onValueChange = {
                    onConfigChange(
                        config.copy(
                            sniffer = config.sniffer.copy(
                                sniff = config.sniffer.sniff.copy(
                                    quic = config.sniffer.sniff.quic.copy(
                                        overrideDestination = it,
                                    ),
                                ),
                            ),
                        ),
                    )
                },
            )
        }
        OverrideCardSection("跳过与强制") {
            StringListWithModifiersInput(
                title = MLang.Override.Label.ForceDomain,
                replaceValue = config.sniffer.forceDomain,
                startValue = config.sniffer.forceDomainStart,
                endValue = config.sniffer.forceDomainEnd,
                placeholder = "+.v2ex.com",
                onReplaceChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(forceDomain = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(forceDomainStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(forceDomainEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = MLang.Override.Label.SkipDomain,
                replaceValue = config.sniffer.skipDomain,
                startValue = config.sniffer.skipDomainStart,
                endValue = config.sniffer.skipDomainEnd,
                placeholder = "Mijia Cloud",
                onReplaceChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(skipDomain = it)))
                },
                onStartChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(skipDomainStart = it)))
                },
                onEndChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(skipDomainEnd = it)))
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "跳过来源地址",
                replaceValue = config.sniffer.skipSrcAddress,
                startValue = config.sniffer.skipSrcAddressStart,
                endValue = config.sniffer.skipSrcAddressEnd,
                placeholder = "192.168.0.3/32",
                onReplaceChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(skipSrcAddress = it)))
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(sniffer = config.sniffer.copy(skipSrcAddressStart = it)),
                    )
                },
                onEndChange = {
                    onConfigChange(
                        config.copy(sniffer = config.sniffer.copy(skipSrcAddressEnd = it)),
                    )
                },
                onEditListGroup = onEditStringList,
            )
            StringListWithModifiersInput(
                title = "跳过目标地址",
                replaceValue = config.sniffer.skipDstAddress,
                startValue = config.sniffer.skipDstAddressStart,
                endValue = config.sniffer.skipDstAddressEnd,
                placeholder = "192.168.0.3/32",
                onReplaceChange = {
                    onConfigChange(config.copy(sniffer = config.sniffer.copy(skipDstAddress = it)))
                },
                onStartChange = {
                    onConfigChange(
                        config.copy(sniffer = config.sniffer.copy(skipDstAddressStart = it)),
                    )
                },
                onEndChange = {
                    onConfigChange(
                        config.copy(sniffer = config.sniffer.copy(skipDstAddressEnd = it)),
                    )
                },
                onEditListGroup = onEditStringList,
            )
        }
    }
}
