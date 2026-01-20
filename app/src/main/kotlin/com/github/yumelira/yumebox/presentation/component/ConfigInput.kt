package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.screen.EditorDataHolder
import com.ramcosta.composedestinations.generated.destinations.KeyValueEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.StringListEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.extra.SuperDialog


@Composable
fun PortInput(
    title: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value?.toString() ?: "") }
    var showDialog by remember { mutableStateOf(false) }

    SuperArrow(
        title = title,
        summary = if (value != null) "$value" else "不修改",
        onClick = { showDialog = true },
    )

    if (showDialog) {
        WindowBottomSheet(
            show = remember(showDialog) { mutableStateOf(true) },
            title = title,
            onDismissRequest = { showDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = textValue,
                    onValueChange = { textValue = it.filter { c -> c.isDigit() } },
                    label = "端口号 (留空表示不修改)",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                DialogButtonRow(
                    onCancel = {
                        onValueChange(null)
                        showDialog = false
                    },
                    onConfirm = {
                        val port = textValue.toIntOrNull()
                        if (port == null || (port in 1..65535)) {
                            onValueChange(port)
                        }
                        showDialog = false
                    },
                    cancelText = "清除",
                )
            }
        }
    }
}

@Composable
fun StringInput(
    title: String,
    value: String?,
    placeholder: String = "",
    onValueChange: (String?) -> Unit,
    onRandomGenerate: (() -> String)? = null,
    initiallyExpanded: Boolean = false,
) {
    var textValue by remember(value) { mutableStateOf(value ?: "") }
    var showDialog by remember { mutableStateOf(initiallyExpanded) }

    SuperArrow(
        title = title,
        summary = value?.takeIf { it.isNotEmpty() } ?: "不修改",
        onClick = { showDialog = true },
    )

    if (showDialog) {
        val showState = remember(showDialog) { mutableStateOf(true) }
        if (showState.value) {
            WindowBottomSheet(
                title = title,
                show = showState,
                onDismissRequest = {
                    showState.value = false
                    showDialog = false
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (onRandomGenerate != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = textValue,
                                onValueChange = { textValue = it },
                                label = placeholder,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                text = MLang.Component.Button.Random,
                                onClick = {
                                    textValue = onRandomGenerate()
                                }
                            )
                        }
                    } else {
                        TextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = placeholder,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    DialogButtonRow(
                        onCancel = {
                            onValueChange(null)
                            showState.value = false
                            showDialog = false
                        },
                        onConfirm = {
                            onValueChange(textValue.takeIf { it.isNotEmpty() })
                            showState.value = false
                            showDialog = false
                        },
                        cancelText = "清除",
                    )
                }
            }
        }
    }
}

@Composable
fun StringListInput(
    title: String,
    value: List<String>?,
    placeholder: String = "",
    navigator: DestinationsNavigator,
    onValueChange: (List<String>?) -> Unit,
) {
    val itemCount = value?.size ?: 0
    val displayValue = if (itemCount > 0) "${itemCount} 项" else "不修改"

    SuperArrow(
        title = title,
        summary = displayValue,
        onClick = {
            EditorDataHolder.setupListEditor(
                title = title,
                placeholder = placeholder,
                items = value,
                callback = onValueChange,
            )
            navigator.navigate(StringListEditorScreenDestination)
        },
    )
}

@Composable
fun StringMapInput(
    title: String,
    value: Map<String, String>?,
    keyPlaceholder: String = "",
    valuePlaceholder: String = "",
    navigator: DestinationsNavigator,
    onValueChange: (Map<String, String>?) -> Unit,
) {
    val itemCount = value?.size ?: 0
    val displayValue = if (itemCount > 0) "${itemCount} 项" else "不修改"

    SuperArrow(
        title = title,
        summary = displayValue,
        onClick = {
            EditorDataHolder.setupMapEditor(
                title = title,
                keyPlaceholder = keyPlaceholder.ifBlank { MLang.Component.ConfigInput.KeyPlaceholder },
                valuePlaceholder = valuePlaceholder.ifBlank { MLang.Component.ConfigInput.ValuePlaceholder },
                items = value,
                callback = onValueChange,
            )
            navigator.navigate(KeyValueEditorScreenDestination)
        },
    )
}

@Composable
fun IntInput(
    title: String,
    value: Int?,
    label: String = "",
    onValueChange: (Int?) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value?.toString() ?: "") }
    var showDialog by remember { mutableStateOf(false) }
    SuperArrow(
        title = title,
        summary = value?.toString() ?: "不修改",
        onClick = { showDialog = true },
    )
    if (showDialog) {
        val showState = remember(showDialog) { mutableStateOf(true) }
        if (showState.value) {
            WindowBottomSheet(
                show = showState,
                title = title,
                onDismissRequest = {
                    showState.value = false
                    showDialog = false
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = textValue,
                        onValueChange = { textValue = it.filter { c -> c.isDigit() || c == '-' } },
                        label = label,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DialogButtonRow(
                        onCancel = {
                            onValueChange(null)
                            showState.value = false
                            showDialog = false
                        },
                        onConfirm = {
                            val v = textValue.toIntOrNull()
                            onValueChange(v)
                            showState.value = false
                            showDialog = false
                        },
                        cancelText = "清除",
                    )
                }
            }
        }
    }
}
