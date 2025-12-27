package com.github.yumelira.yumebox.presentation.component

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.TextButton

@Composable
fun DialogButtonRow(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    cancelText: String = MLang.Component.Button.Cancel,
    confirmText: String = MLang.Component.Button.Confirm,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(
            text = cancelText,
            onClick = onCancel,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            text = confirmText,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
        )
    }
}
