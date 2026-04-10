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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.core.model.TunnelState
import dev.oom_wg.purejoy.mlang.MLang
import java.security.SecureRandom

@Composable
fun GeneralEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
    ) {
        OverrideCardSection(MLang.Override.Form.RunAndLog) {
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
                title = MLang.Override.Form.ProcessMode,
                value = config.findProcessMode,
                items = listOf(MLang.Override.Form.NotModify, "Always", "Strict", "Off"),
                values = listOf(
                    null,
                    ConfigurationOverride.FindProcessMode.Always,
                    ConfigurationOverride.FindProcessMode.Strict,
                    ConfigurationOverride.FindProcessMode.Off,
                ),
                onValueChange = { onConfigChange(config.copy(findProcessMode = it)) },
            )
            NullableBooleanSelector(
                title = MLang.Override.Form.UnifiedDelay,
                value = config.unifiedDelay,
                onValueChange = { onConfigChange(config.copy(unifiedDelay = it)) },
            )
            NullableBooleanSelector(
                title = MLang.Override.Form.TcpConcurrent,
                value = config.tcpConcurrent,
                onValueChange = { onConfigChange(config.copy(tcpConcurrent = it)) },
            )
            NullableBooleanSelector(
                title = MLang.Override.Form.GeodataMode,
                value = config.geodataMode,
                onValueChange = { onConfigChange(config.copy(geodataMode = it)) },
            )
        }

        OverrideFormSection(MLang.Override.Form.RunAndLogExtra) {
            OverrideIntInputContent(
                title = MLang.Override.Label.KeepAliveInterval,
                value = config.keepAliveInterval,
                placeholder = MLang.Override.Form.Seconds,
                onValueChange = { onConfigChange(config.copy(keepAliveInterval = it)) },
            )
            OverrideIntInputContent(
                title = MLang.Override.Label.KeepAliveIdle,
                value = config.keepAliveIdle,
                placeholder = MLang.Override.Form.Seconds,
                onValueChange = { onConfigChange(config.copy(keepAliveIdle = it)) },
            )
        }

        OverrideFormSection(MLang.Override.Form.ConnectionNetwork) {
            OverrideTextInputContent(
                title = MLang.Override.Form.OutboundInterface,
                value = config.interfaceName,
                placeholder = "en0 / wlan0",
                onValueChange = { onConfigChange(config.copy(interfaceName = it)) },
            )
            OverrideIntInputContent(
                title = MLang.Override.Form.RoutingMark,
                value = config.routingMark,
                placeholder = "6666",
                onValueChange = { onConfigChange(config.copy(routingMark = it)) },
            )
            OverrideTextInputContent(
                title = MLang.Override.Form.GeositeMatcher,
                value = config.geositeMatcher,
                placeholder = "standard / succinct",
                onValueChange = { onConfigChange(config.copy(geositeMatcher = it)) },
            )
            OverrideTextInputContent(
                title = MLang.Override.Form.GlobalClientFingerprint,
                value = config.globalClientFingerprint,
                placeholder = "chrome / safari",
                onValueChange = { onConfigChange(config.copy(globalClientFingerprint = it)) },
            )
        }

        OverrideCardSection(MLang.Override.Form.LanAccess) {
            NullableBooleanSelector(
                title = MLang.Override.General.AllowLan,
                value = config.allowLan,
                onValueChange = { onConfigChange(config.copy(allowLan = it)) },
            )
            StringListWithModifiersInput(
                title = MLang.Override.Form.AllowedIPs,
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
                title = MLang.Override.Form.DisallowedIPs,
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

        OverrideFormSection(MLang.Override.Form.LanAddress) {
            OverrideTextInputContent(
                title = MLang.Override.Form.BindAddress,
                value = config.bindAddress,
                placeholder = "* / 192.168.1.1 / [::1]",
                onValueChange = { onConfigChange(config.copy(bindAddress = it)) },
            )
        }

        OverrideCardSection(MLang.Override.Form.UserAuth) {
            StringListWithModifiersInput(
                title = MLang.Override.Form.UserAuth,
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
                title = MLang.Override.Form.SkipAuthIPs,
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

        OverrideFormSection(MLang.Override.Form.ExternalControl) {
            OverrideTextInputContent(
                title = MLang.Override.Form.ExternalController,
                value = config.externalController,
                placeholder = "127.0.0.1:9090",
                onValueChange = { onConfigChange(config.copy(externalController = it)) },
            )
            OverrideTextInputContent(
                title = MLang.Override.Form.ExternalControllerHttps,
                value = config.externalControllerTLS,
                placeholder = "127.0.0.1:9443",
                onValueChange = { onConfigChange(config.copy(externalControllerTLS = it)) },
            )
            OverrideTextInputContent(
                title = MLang.Override.Form.ExternalDoH,
                value = config.externalDohServer,
                placeholder = "/dns-query",
                onValueChange = { onConfigChange(config.copy(externalDohServer = it)) },
            )
            OverrideTextInputContent(
                title = MLang.Override.Form.ApiSecret,
                value = config.secret,
                placeholder = MLang.Override.Form.ApiSecret,
                randomValueGenerator = ::generateRandomApiSecret,
                onValueChange = { onConfigChange(config.copy(secret = it)) },
            )
        }

        OverrideCardSection(MLang.Override.Form.ControllerCors) {
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
                title = MLang.Override.Form.AllowPrivateNetwork,
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

        OverrideCardSection(MLang.Override.Form.ConfigPersistence) {
            NullableBooleanSelector(
                title = MLang.Override.Form.SaveGroupSelection,
                value = config.profile.storeSelected,
                onValueChange = {
                    onConfigChange(config.copy(profile = config.profile.copy(storeSelected = it)))
                },
            )
            NullableBooleanSelector(
                title = MLang.Override.Form.SaveFakeIpMapping,
                value = config.profile.storeFakeIp,
                onValueChange = {
                    onConfigChange(config.copy(profile = config.profile.copy(storeFakeIp = it)))
                },
            )
        }

        OverrideCardSection(MLang.Override.Form.GeoResources) {
            NullableBooleanSelector(
                title = MLang.Override.Form.AutoUpdateGeo,
                value = config.geoAutoUpdate,
                onValueChange = { onConfigChange(config.copy(geoAutoUpdate = it)) },
            )
        }

        OverrideFormSection(MLang.Override.Form.GeoResources) {
            OverrideIntInputContent(
                title = MLang.Override.Form.GeoUpdateInterval,
                value = config.geoUpdateInterval,
                placeholder = MLang.Override.Form.Hours,
                onValueChange = { onConfigChange(config.copy(geoUpdateInterval = it)) },
            )
            OverrideTextInputContent(
                title = MLang.Override.Form.GeoipUrl,
                value = config.geoxurl.geoip,
                placeholder = "https://...",
                onValueChange = {
                    onConfigChange(config.copy(geoxurl = config.geoxurl.copy(geoip = it)))
                },
            )
            OverrideTextInputContent(
                title = MLang.Override.Form.GeositeUrl,
                value = config.geoxurl.geosite,
                placeholder = "https://...",
                onValueChange = {
                    onConfigChange(config.copy(geoxurl = config.geoxurl.copy(geosite = it)))
                },
            )
            OverrideTextInputContent(
                title = MLang.Override.Form.MmdbUrl,
                value = config.geoxurl.mmdb,
                placeholder = "https://...",
                onValueChange = {
                    onConfigChange(config.copy(geoxurl = config.geoxurl.copy(mmdb = it)))
                },
            )
        }
    }
}

private val apiSecretRandom = SecureRandom()
private const val API_SECRET_LENGTH = 16
private const val API_SECRET_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%^&*()_+-="

private fun generateRandomApiSecret(): String {
    val builder = StringBuilder(API_SECRET_LENGTH)
    repeat(API_SECRET_LENGTH) {
        val index = apiSecretRandom.nextInt(API_SECRET_CHARSET.length)
        builder.append(API_SECRET_CHARSET[index])
    }
    return builder.toString()
}
