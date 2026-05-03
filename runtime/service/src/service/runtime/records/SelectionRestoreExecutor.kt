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



package com.github.yumelira.yumebox.runtime.service.runtime.records

import com.github.yumelira.yumebox.core.Clash
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.ProxyGroup
import com.github.yumelira.yumebox.runtime.service.common.log.Log
import com.github.yumelira.yumebox.runtime.service.runtime.entity.Selection
import java.util.*

internal object SelectionRestoreExecutor {
    private const val queryRetryCount = 3
    private const val queryRetryDelayMs = 150L

    fun restore(
        profileUuid: UUID,
        selections: List<Selection>,
        pins: Map<String, String>,
        runtimeGroups: List<ProxyGroup>,
        tag: String,
    ) {
        val selectorGroups = runtimeGroups.associateBy { it.name }
        selections.forEach { selection ->
            val group = selectorGroups[selection.proxy] ?: run {
                removeSelection(profileUuid, selection, tag, "group missing")
                return@forEach
            }
            if (group.type != Proxy.Type.Selector) {
                removeSelection(profileUuid, selection, tag, "group not selector")
                return@forEach
            }

            val currentNodes = group.proxies
                .mapNotNull { proxy -> proxy.name.trim().takeIf { it.isNotEmpty() } }
            val targetNode = selection.selected.trim()
            if (targetNode.isEmpty() || targetNode !in currentNodes) {
                removeSelection(profileUuid, selection, tag, "node missing")
                return@forEach
            }

            if (!patchSelectorWithRetry(selection.proxy, targetNode)) {
                Log.w("$tag restore selector patch failed: profile=$profileUuid group=${selection.proxy} node=$targetNode")
            }
        }

        pins.forEach { (groupName, pinnedNode) ->
            val group = selectorGroups[groupName] ?: run {
                removePinned(profileUuid, groupName, pinnedNode, tag, "group missing")
                return@forEach
            }
            if (!group.type.supportsPinnedSelection()) {
                removePinned(profileUuid, groupName, pinnedNode, tag, "group does not support pin")
                return@forEach
            }
            val currentNodes = group.proxies
                .mapNotNull { proxy -> proxy.name.trim().takeIf { it.isNotEmpty() } }
            val targetNode = pinnedNode.trim()
            if (targetNode.isEmpty() || targetNode !in currentNodes) {
                removePinned(profileUuid, groupName, pinnedNode, tag, "node missing")
                return@forEach
            }
            if (!patchForceSelectorWithRetry(groupName, targetNode)) {
                Log.w("$tag restore pin patch failed: profile=$profileUuid group=$groupName node=$targetNode")
            }
        }
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

    private fun patchForceSelectorWithRetry(group: String, node: String): Boolean {
        repeat(queryRetryCount) { attempt ->
            if (Clash.patchForceSelector(group, node)) {
                return true
            }
            if (attempt < queryRetryCount - 1) {
                Thread.sleep(queryRetryDelayMs)
            }
        }
        return false
    }

    private fun removeSelection(profileUuid: UUID, selection: Selection, tag: String, reason: String) {
        Log.w(
            "$tag remove invalid selector memory: profile=$profileUuid group=${selection.proxy} " +
                "node=${selection.selected} reason=$reason",
        )
        SelectionDao.remove(profileUuid, selection.proxy)
    }

    private fun removePinned(profileUuid: UUID, group: String, node: String, tag: String, reason: String) {
        Log.w(
            "$tag remove invalid pin memory: profile=$profileUuid group=$group node=$node reason=$reason",
        )
        SelectionDao.removePinned(profileUuid, group)
    }

    private fun Proxy.Type.supportsPinnedSelection(): Boolean {
        return this == Proxy.Type.URLTest || this == Proxy.Type.Fallback
    }
}
