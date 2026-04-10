package com.github.yumelira.yumebox.feature.editor.api.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.yumelira.yumebox.feature.editor.api.editor.CodeEditor
import com.github.yumelira.yumebox.feature.editor.api.editor.rememberConfiguredCodeEditorState
import com.github.yumelira.yumebox.feature.editor.api.language.LanguageScope
import com.github.yumelira.yumebox.presentation.component.AppDialog
import com.github.yumelira.yumebox.presentation.component.DialogButtonRow
import com.github.yumelira.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CodeEditorDialog(
    show: Boolean,
    title: String,
    subtitle: String? = null,
    value: String?,
    language: LanguageScope = LanguageScope.Json,
    placeholder: String = "",
    onValueChange: (String?) -> Unit,
    onDismiss: () -> Unit = {},
) {
    val editorState = rememberConfiguredCodeEditorState(
        initialContent = value ?: placeholder,
        language = language,
        readOnly = false,
    )
    if (show) {
        AppDialog(show = show, title = title, onDismissRequest = onDismiss) {
            Column(modifier = Modifier.padding(UiDp.dp20)) {
                subtitle?.let { text ->
                    Text(
                        text = text,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.outline,
                    )
                    Spacer(modifier = Modifier.height(UiDp.dp12))
                }
                CodeEditor(
                    state = editorState,
                    modifier = Modifier.fillMaxWidth().heightIn(min = UiDp.dp280, max = UiDp.dp400),
                    onTextChange = {},
                )
                Spacer(modifier = Modifier.height(UiDp.dp24))
                DialogButtonRow(
                    onCancel = onDismiss,
                    onConfirm = {
                        onValueChange(editorState.content.takeIf { it.isNotBlank() })
                        onDismiss()
                    },
                    cancelText = "取消",
                    confirmText = "确定",
                )
            }
        }
    }
}

@Composable
fun JsonEditorDialog(
    show: Boolean,
    title: String,
    subtitle: String? = "使用 JSON 格式编辑",
    value: String?,
    onValueChange: (String?) -> Unit,
    onDismiss: () -> Unit = {},
) {
    CodeEditorDialog(
        show = show,
        title = title,
        subtitle = subtitle,
        value = value,
        language = LanguageScope.Json,
        onValueChange = onValueChange,
        onDismiss = onDismiss,
    )
}
