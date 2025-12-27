package com.github.yumelira.yumebox.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ConfirmDialog(
    show: MutableState<Boolean>,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = { show.value = false },
    cancelText: String = MLang.Component.Button.Cancel,
    confirmText: String = MLang.Component.Button.Confirm,
) {
    SuperBottomSheet(
        show = show,
        title = title,
        insideMargin = DpSize(32.dp, 16.dp),
        onDismissRequest = onDismiss,
    ) {
        Column {
            Text(
                text = message,
                style = MiuixTheme.textStyles.body1,
            )
            Spacer(modifier = Modifier.height(16.dp))
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = onConfirm,
                cancelText = cancelText,
                confirmText = confirmText,
            )
        }
    }
}

@Composable
fun ConfirmDialogSimple(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelText: String = MLang.Component.Button.Cancel,
    confirmText: String = MLang.Component.Button.Confirm,
) {
    SuperBottomSheet(
        show = remember { mutableStateOf(true) },
        title = title,
        insideMargin = DpSize(32.dp, 16.dp),
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = message, style = MiuixTheme.textStyles.body1)
            Spacer(modifier = Modifier.height(16.dp))
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = onConfirm,
                cancelText = cancelText,
                confirmText = confirmText,
            )
        }
    }
}
