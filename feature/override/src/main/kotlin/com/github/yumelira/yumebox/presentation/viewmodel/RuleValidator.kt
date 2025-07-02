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

package com.github.yumelira.yumebox.presentation.viewmodel

import com.github.yumelira.yumebox.core.model.ConfigurationOverride

internal object RuleValidator {

    fun validate(config: ConfigurationOverride): List<String> {
        val warnings = mutableListOf<String>()
        val rules = config.rules.orEmpty() + config.prependRules.orEmpty()
        rules.forEachIndexed { index, raw ->
            val rule = raw.trim()
            if (rule.isEmpty()) {
                warnings += "规则 #${index + 1} 为空"
                return@forEachIndexed
            }
            val parts = rule.split(',').map { it.trim() }
            if (parts.size < 2) {
                warnings += "规则 #${index + 1} 格式可能不正确: $rule"
                return@forEachIndexed
            }
            if (parts.first().equals("RULE-SET", ignoreCase = true) && parts.size < 3) {
                warnings += "规则 #${index + 1} 缺少策略组目标: $rule"
            }
        }
        return warnings
    }
}
