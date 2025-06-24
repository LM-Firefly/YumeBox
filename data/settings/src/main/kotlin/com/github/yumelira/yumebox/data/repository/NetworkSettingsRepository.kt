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

package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.data.model.AccessControlMode
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.data.model.TunStack
import com.github.yumelira.yumebox.data.store.NetworkSettingsStorage
import com.github.yumelira.yumebox.data.store.Preference

class NetworkSettingsRepository(
    private val storage: NetworkSettingsStorage,
) {
    val proxyMode: Preference<ProxyMode> = storage.proxyMode
    val bypassPrivateNetwork: Preference<Boolean> = storage.bypassPrivateNetwork
    val dnsHijack: Preference<Boolean> = storage.dnsHijack
    val allowBypass: Preference<Boolean> = storage.allowBypass
    val enableIPv6: Preference<Boolean> = storage.enableIPv6
    val systemProxy: Preference<Boolean> = storage.systemProxy
    val tunStack: Preference<TunStack> = storage.tunStack
    val accessControlMode: Preference<AccessControlMode> = storage.accessControlMode
    val accessControlPackages: Preference<Set<String>> = storage.accessControlPackages
}
