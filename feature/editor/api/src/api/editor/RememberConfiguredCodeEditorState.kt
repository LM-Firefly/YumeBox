package com.github.yumelira.yumebox.feature.editor.api.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.yumelira.yumebox.feature.editor.api.language.LanguageScope

@Composable
fun rememberConfiguredCodeEditorState(
    initialContent: String,
    language: LanguageScope,
    readOnly: Boolean = false,
): CodeEditorState {
    return remember(initialContent, language, readOnly) {
        CodeEditorState(
            initialContent = initialContent,
            language = language,
            readOnly = readOnly,
        )
    }
}
