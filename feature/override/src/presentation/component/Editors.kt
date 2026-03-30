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

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField

typealias OpenStringListEditor = (
    title: String,
    placeholder: String,
    value: List<String>?,
    onValueChange: (List<String>?) -> Unit,
) -> Unit

typealias OpenStringMapEditor = (
    title: String,
    keyPlaceholder: String,
    valuePlaceholder: String,
    value: Map<String, String>?,
    onValueChange: (Map<String, String>?) -> Unit,
) -> Unit

typealias OpenJsonEditor = (
    title: String,
    placeholder: String,
    value: String?,
    onValueChange: (String?) -> Unit,
) -> Unit

@Composable
internal fun OverrideTextInputContent(
    title: String,
    value: String?,
    placeholder: String = "",
    onValueChange: (String?) -> Unit,
) {
    StringInputContent(
        title = title,
        value = value,
        placeholder = placeholder,
        onValueChange = onValueChange,
    )
}

@Composable
internal fun OverrideIntInputContent(
    title: String,
    value: Int?,
    placeholder: String,
    onValueChange: (Int?) -> Unit,
) {
    val showDialog = remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(value?.toString().orEmpty()) }

    top.yukonga.miuix.kmp.preference.ArrowPreference(
        title = title,
        summary = value?.toString() ?: MLang.Component.Selector.NotModify,
        onClick = {
            textValue = value?.toString().orEmpty()
            showDialog.value = true
        },
    )

    AppDialog(
        show = showDialog.value,
        title = title,
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextField(
                value = textValue,
                onValueChange = { updatedValue ->
                    textValue = updatedValue.filter { it.isDigit() || it == '-' }
                },
                label = placeholder,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        onValueChange(null)
                        showDialog.value = false
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(MLang.Component.Button.Clear)
                }
                Button(
                    onClick = {
                        onValueChange(textValue.takeIf(String::isNotBlank)?.toIntOrNull())
                        showDialog.value = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(MLang.Component.Button.Confirm)
                }
            }
        }
    }
}

@Composable
internal fun OverridePortInputContent(
    title: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
) {
    PortInputContent(
        title = title,
        value = value,
        onValueChange = onValueChange,
    )
}
