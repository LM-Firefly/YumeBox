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

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.ProxySort
import com.github.yumelira.yumebox.service.common.log.Log
import com.github.yumelira.yumebox.service.runtime.entity.Selection
import java.util.*

internal object SelectionRestoreExecutor {
    private const val queryRetryCount = 3
    private const val queryRetryDelayMs = 150L

    fun restore(profileUuid: UUID, selections: List<Selection>, tag: String) {
        selections.forEach { selection ->
            val group = queryGroupWithRetry(selection.proxy)

            val currentNodes = group?.proxies
                ?.mapNotNull { proxy -> proxy.name.trim().takeIf { it.isNotEmpty() } }
                .orEmpty()
            val targetNode = selection.selected.trim()
            if (targetNode.isEmpty() || targetNode !in currentNodes) {
                clearProfileSelections(profileUuid, selection, tag)
                return
            }

            if (!patchSelectorWithRetry(selection.proxy, targetNode)) {
                Log.w("$tag restore selector patch failed: profile=$profileUuid group=${selection.proxy} node=$targetNode")
            }
        }
    }

    private fun queryGroupWithRetry(group: String): com.github.yumelira.yumebox.core.model.ProxyGroup? {
        repeat(queryRetryCount) { attempt ->
            val result = runCatching {
                Clash.queryGroup(group, ProxySort.Default)
            }.getOrNull()
            if (result != null) {
                return result
            }
            if (attempt < queryRetryCount - 1) {
                Thread.sleep(queryRetryDelayMs)
            }
        }
        return null
    }

    private fun patchSelectorWithRetry(group: String, node: String): Boolean {
        repeat(queryRetryCount) { attempt ->
            if (Clash.patchSelector(group, node)) {
                return true
            }
            if (attempt < queryRetryCount - 1) {
                Thread.sleep(queryRetryDelayMs)
            }
        }
        return false
    }

    private fun clearProfileSelections(profileUuid: UUID, selection: Selection, tag: String) {
        Log.w("$tag restore selector not found: profile=$profileUuid group=${selection.proxy} node=${selection.selected}")
        SelectionDao.clear(profileUuid)
        SelectionDao.removeSelectionScopeKey(profileUuid)
        Log.i("$tag cleared persisted selections: profile=$profileUuid")
    }
}
