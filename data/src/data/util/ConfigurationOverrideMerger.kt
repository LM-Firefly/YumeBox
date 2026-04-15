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

package com.github.yumelira.yumebox.data.util

import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.dnsSection
import com.github.yumelira.yumebox.core.model.snifferSection
import com.github.yumelira.yumebox.core.model.supportSection
import com.github.yumelira.yumebox.core.model.withDnsSection
import com.github.yumelira.yumebox.core.model.withSnifferSection
import com.github.yumelira.yumebox.core.model.withSupportSection

internal object ConfigurationOverrideMerger {

    fun merge(base: ConfigurationOverride, incoming: ConfigurationOverride): ConfigurationOverride {
        return ConfigurationOverrideCoreMerger.merge(base, incoming)
            .withDnsSection(
                ConfigurationOverrideDnsMerger.merge(
                    base = base.dnsSection(),
                    incoming = incoming.dnsSection(),
                ),
            )
            .withSnifferSection(
                ConfigurationOverrideSnifferMerger.merge(
                    base = base.snifferSection(),
                    incoming = incoming.snifferSection(),
                ),
            )
            .withSupportSection(
                ConfigurationOverrideSupportMerger.merge(
                    base = base.supportSection(),
                    incoming = incoming.supportSection(),
                ),
            )
    }
}
