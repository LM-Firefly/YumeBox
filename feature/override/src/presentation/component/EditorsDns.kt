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
import dev.oom_wg.purejoy.mlang.MLang

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
        OverrideCardSection(MLang.Override.Form.DnsBasicSwitch) {
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
                title = MLang.Override.Form.CacheLimit,
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

        OverrideCardSection(MLang.Override.Form.DnsUpstream) {
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

        OverrideCardSection(MLang.Override.Form.NameserverPolicySection) {
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

        OverrideCardSection(MLang.Override.Form.FilterList) {
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
