/*
 * This file is part of FlyCat.
 *
 * FlyCat is free software: you can redistribute it and/or modify
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
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumelira.yumebox.core.util

import com.github.yumelira.yumebox.core.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.core.model.Proxy

class ProxyChainResolver {
    fun buildChainPathFromMap(
        groupName: String,
        currentNode: String,
        groups: Map<String, ProxyGroupInfo>,
        visited: MutableSet<String> = mutableSetOf(),
    ): List<String> {
        if (groupName in visited) return listOf(groupName)
        visited.add(groupName)
        val nextGroup = groups[currentNode] ?: return listOf(groupName, currentNode)
        val nextNow = nextGroup.now.trim()
        return if (nextGroup.type in Proxy.Type.GROUP_TYPES && nextNow.isNotBlank()) {
            listOf(groupName) + buildChainPathFromMap(currentNode, nextNow, groups, visited)
        } else {
            listOf(groupName, currentNode)
        }
    }
}
