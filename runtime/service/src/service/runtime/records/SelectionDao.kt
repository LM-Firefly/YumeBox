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
}
