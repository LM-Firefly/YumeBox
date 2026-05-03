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

package com.github.yumelira.yumebox.runtime.client

import com.github.yumelira.yumebox.core.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.core.model.Profile
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimePhase
import java.util.UUID

internal class ProxyGroupPreviewCache {
    private data class CacheKey(
        val profileId: UUID,
        val profileUpdatedAt: Long,
        val excludeNotSelectable: Boolean,
        val overrideSignature: String,
    )

    private data class CacheEntry(
        val key: CacheKey,
        val groups: List<ProxyGroupInfo>,
    )

    private var entry: CacheEntry? = null

    fun store(
        profile: Profile,
        excludeNotSelectable: Boolean,
        overrideSignature: String,
        groups: List<ProxyGroupInfo>,
    ) {
        entry = CacheEntry(
            key = key(profile, excludeNotSelectable, overrideSignature),
            groups = groups,
        )
    }

    fun fallback(
        phase: RuntimePhase,
        profile: Profile?,
        excludeNotSelectable: Boolean,
        overrideSignature: String,
    ): List<ProxyGroupInfo>? {
        if (phase == RuntimePhase.Running) return null
        val cached = entry ?: return null
        if (profile == null) return cached.groups
        return cached.takeIf {
            it.key == key(profile, excludeNotSelectable, overrideSignature)
        }?.groups
    }

    private fun key(
        profile: Profile,
        excludeNotSelectable: Boolean,
        overrideSignature: String,
    ): CacheKey {
        return CacheKey(
            profileId = profile.uuid,
            profileUpdatedAt = profile.updatedAt,
            excludeNotSelectable = excludeNotSelectable,
            overrideSignature = overrideSignature,
        )
    }
}
