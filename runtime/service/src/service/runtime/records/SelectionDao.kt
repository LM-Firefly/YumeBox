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



package com.github.yumelira.yumebox.service.runtime.records

import com.github.yumelira.yumebox.service.runtime.entity.Selection
import java.util.*

object SelectionDao {
    private const val PIN_KEY_PREFIX = "pin_"

    fun migrateLegacyIfNeeded() {
        ProfileStore.migrateLegacySelectionMemoryIfNeeded()
    }

    fun queryAll(): List<Selection> {
        migrateLegacyIfNeeded()
        return ProfileStore.loadSelections()
    }

    fun querySelections(profileUUID: UUID): List<Selection> {
        return queryAll().filter { it.uuid == profileUUID }
    }

    fun queryRestorableSelections(profileUUID: UUID): List<Selection> {
        return querySelections(profileUUID)
    }

    fun upsertManualSelection(profileUUID: UUID, groupName: String, selectedProxy: String) {
        upsertManualSelection(
            Selection(
                uuid = profileUUID,
                proxy = groupName.trim(),
                selected = selectedProxy.trim(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun upsertManualSelection(selection: Selection) {
        migrateLegacyIfNeeded()
        val normalized = selection.copy(
            proxy = selection.proxy.trim(),
            selected = selection.selected.trim(),
            updatedAt = if (selection.updatedAt > 0L) selection.updatedAt else System.currentTimeMillis(),
        )
        if (normalized.proxy.isEmpty() || normalized.selected.isEmpty()) {
            return
        }
        val list = ProfileStore.loadSelections().toMutableList()
        val index = list.indexOfFirst {
            it.uuid == normalized.uuid && it.proxy == normalized.proxy
        }
        if (index >= 0) {
            list[index] = normalized
        } else {
            list.add(normalized)
        }
        ProfileStore.saveSelections(list)
    }

    fun setSelected(selection: Selection) {
        upsertManualSelection(selection)
    }

    fun clear(profileUUID: UUID) {
        migrateLegacyIfNeeded()
        val list = ProfileStore.loadSelections().toMutableList()
        list.removeAll { it.uuid == profileUUID }
        ProfileStore.saveSelections(list)
        removeAllPins(profileUUID)
    }

    fun clearAll() {
        migrateLegacyIfNeeded()
        ProfileStore.saveSelections(emptyList())
        ProfileStore.removeAllSelectionScopeKeys()
    }

    fun remove(profileUUID: UUID, proxy: String) {
        migrateLegacyIfNeeded()
        val list = ProfileStore.loadSelections().toMutableList()
        list.removeAll { it.uuid == profileUUID && it.proxy == proxy }
        ProfileStore.saveSelections(list)
    }

    fun removeSelections(profileUUID: UUID, proxies: List<String>) {
        migrateLegacyIfNeeded()
        val list = ProfileStore.loadSelections().toMutableList()
        list.removeAll { it.uuid == profileUUID && it.proxy in proxies }
        ProfileStore.saveSelections(list)
    }

    fun setPinned(profileUUID: UUID, proxyGroup: String, pinnedNode: String) {
        val normalizedGroup = proxyGroup.trim()
        val normalizedNode = pinnedNode.trim()
        if (normalizedGroup.isEmpty() || normalizedNode.isEmpty()) return
        ProfileStore.setSelectionScopeValue(makePinKey(profileUUID, normalizedGroup), normalizedNode)
    }

    fun getPinned(profileUUID: UUID, proxyGroup: String): String? {
        val normalizedGroup = proxyGroup.trim()
        if (normalizedGroup.isEmpty()) return null
        return ProfileStore.getSelectionScopeValue(makePinKey(profileUUID, normalizedGroup))
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun removePinned(profileUUID: UUID, proxyGroup: String) {
        val normalizedGroup = proxyGroup.trim()
        if (normalizedGroup.isEmpty()) return
        ProfileStore.removeSelectionScopeValue(makePinKey(profileUUID, normalizedGroup))
    }

    fun getAllPins(profileUUID: UUID): Map<String, String> {
        val prefix = makePinKeyPrefix(profileUUID)
        return ProfileStore.querySelectionScopeValues(prefix)
            .mapNotNull { (key, value) ->
                val group = key.removePrefix(prefix).trim()
                val node = value.trim()
                if (group.isEmpty() || node.isEmpty()) null else group to node
            }
            .toMap()
    }

    private fun removeAllPins(profileUUID: UUID) {
        val prefix = makePinKeyPrefix(profileUUID)
        val keys = ProfileStore.querySelectionScopeValues(prefix).keys
        keys.forEach { scopedKey ->
            ProfileStore.removeSelectionScopeValue(scopedKey)
        }
    }

    private fun makePinKey(profileUUID: UUID, proxyGroup: String): String {
        return "${PIN_KEY_PREFIX}${profileUUID}_$proxyGroup"
    }

    private fun makePinKeyPrefix(profileUUID: UUID): String {
        return "${PIN_KEY_PREFIX}${profileUUID}_"
    }
}
