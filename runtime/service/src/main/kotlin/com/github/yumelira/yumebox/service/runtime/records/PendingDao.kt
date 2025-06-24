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

import com.github.yumelira.yumebox.service.runtime.entity.Pending
import java.util.*

/**
 * DAO for Pending profile operations using MMKV
 */
object PendingDao {
    
    fun queryAll(): List<Pending> {
        return ProfileStore.loadPending()
    }

    fun queryByUUID(uuid: UUID): Pending? {
        return ProfileStore.loadPending().find { it.uuid == uuid }
    }

    fun insert(pending: Pending): Long {
        val list = ProfileStore.loadPending().toMutableList()
        list.add(pending)
        ProfileStore.savePending(list)
        return 1L
    }

    fun update(pending: Pending) {
        val list = ProfileStore.loadPending().toMutableList()
        val index = list.indexOfFirst { it.uuid == pending.uuid }
        if (index >= 0) {
            list[index] = pending
            ProfileStore.savePending(list)
        }
    }

    fun remove(uuid: UUID) {
        val list = ProfileStore.loadPending().toMutableList()
        list.removeAll { it.uuid == uuid }
        ProfileStore.savePending(list)
    }

    fun removeAll(uuids: Collection<UUID>) {
        val list = ProfileStore.loadPending().toMutableList()
        list.removeAll { it.uuid in uuids }
        ProfileStore.savePending(list)
    }

    fun exists(uuid: UUID): Boolean {
        return ProfileStore.loadPending().any { it.uuid == uuid }
    }

    fun queryAllUUIDs(): List<UUID> {
        return ProfileStore.loadPending().map { it.uuid }
    }
}
