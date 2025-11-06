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
import dev.oom_wg.purejoy.mlang.MLang

internal object RuleValidator {

    fun validate(config: ConfigurationOverride): List<String> {
        val warnings = mutableListOf<String>()
        val rules = config.rules.orEmpty()
        rules.forEachIndexed { index, raw ->
            val rule = raw.trim()
            if (rule.isEmpty()) {
                warnings += MLang.Override.Rule.EmptyWarning.format(index + 1)
                return@forEachIndexed
            }
            val parts = rule.split(',').map { it.trim() }
            if (parts.size < 2) {
                warnings += MLang.Override.Rule.InvalidFormatWarning.format(index + 1, rule)
                return@forEachIndexed
            }
            if (parts.first().equals("RULE-SET", ignoreCase = true) && parts.size < 3) {
                warnings += MLang.Override.Rule.MissingTargetWarning.format(index + 1, rule)
            }
        }
        return warnings
    }
}
