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
fun SnifferEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
    ) {
        OverrideCardSection(MLang.Override.Form.BasicPolicy) {
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
        OverrideCardSection(MLang.Override.Form.SkipAndForce) {
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
                title = MLang.Override.Form.SkipSrcAddress,
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
                title = MLang.Override.Form.SkipDstAddress,
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
