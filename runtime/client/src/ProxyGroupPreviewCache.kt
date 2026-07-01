package com.github.yumelira.yumebox.runtime.client

import com.github.yumelira.yumebox.core.model.Profile
import com.github.yumelira.yumebox.core.model.ProxyGroupInfo
import com.github.yumelira.yumebox.runtime.api.service.runtime.entity.RuntimePhase
import java.util.UUID

internal class ProxyGroupPreviewCache {
    private data class CacheKey(
        val profileId: UUID,
        val profileUpdatedAt: Long,
        val excludeNotSelectable: Boolean,
        val overrideSignature: String,
    )

    private data class CacheEntry(val key: CacheKey, val groups: List<ProxyGroupInfo>)

    private var entry: CacheEntry? = null

    fun store(
        profile: Profile,
        excludeNotSelectable: Boolean,
        overrideSignature: String,
        groups: List<ProxyGroupInfo>,
    ) {
        entry =
            CacheEntry(key = key(profile, excludeNotSelectable, overrideSignature), groups = groups)
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
        return cached
            .takeIf { it.key == key(profile, excludeNotSelectable, overrideSignature) }
            ?.groups
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
