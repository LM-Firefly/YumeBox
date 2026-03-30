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
fun InboundEditor(
    config: ConfigurationOverride,
    onConfigChange: (ConfigurationOverride) -> Unit,
    onEditStringList: OpenStringListModifiersEditor,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
    ) {
        OverrideFormSection(MLang.Override.Form.ProxyPorts) {
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
