package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.screen.EditorDataHolder
import com.ramcosta.composedestinations.generated.destinations.KeyValueEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.StringListEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperArrow
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
        summary = if (value != null) "$value" else MLang.Component.Selector.NotModify,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        val showState = remember { mutableStateOf(true) }
        if (showState.value) {
            SuperDialog(
                title = title,
                show = showState,
                onDismissRequest = {
                    showState.value = false
                    showDialog = false
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = textValue,
                        onValueChange = { textValue = it.filter { c -> c.isDigit() } },
                        label = MLang.Component.ConfigInput.PortLabel,
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
                            val port = textValue.toIntOrNull()
                            if (port == null || (port in 1..65535)) {
                                onValueChange(port)
                            }
                            showState.value = false
                            showDialog = false
                        },
                        cancelText = MLang.Component.Button.Clear,
                    )
                }
            }
        }
    }
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
        summary = value?.toString() ?: MLang.Component.Selector.NotModify,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        val showState = remember { mutableStateOf(true) }
        if (showState.value) {
            SuperDialog(
                title = title,
                show = showState,
                onDismissRequest = {
                    showState.value = false
                    showDialog = false
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = textValue,
                        onValueChange = { textValue = it.filter { c -> c.isDigit() } },
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
                            val intValue = textValue.toIntOrNull()
                            onValueChange(intValue)
                            showState.value = false
                            showDialog = false
                        },
                        cancelText = MLang.Component.Button.Clear,
                    )
                }
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
) {
    var textValue by remember(value) { mutableStateOf(value ?: "") }
    var showDialog by remember { mutableStateOf(false) }

    SuperArrow(
        title = title,
        summary = value?.takeIf { it.isNotEmpty() } ?: MLang.Component.Selector.NotModify,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        val showState = remember { mutableStateOf(true) }
        if (showState.value) {
            SuperDialog(
                title = title,
                show = showState,
                onDismissRequest = {
                    showState.value = false
                    showDialog = false
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = placeholder,
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
                            onValueChange(textValue.takeIf { it.isNotEmpty() })
                            showState.value = false
                            showDialog = false
                        },
                        cancelText = MLang.Component.Button.Clear,
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
    val displayValue = if (itemCount > 0) MLang.Component.ConfigInput.CountItems.format(itemCount) else MLang.Component.Selector.NotModify

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
    val displayValue = if (itemCount > 0) MLang.Component.ConfigInput.CountItems.format(itemCount) else MLang.Component.Selector.NotModify

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
