package com.github.yumelira.yumebox.feature.editor.api.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.yumelira.yumebox.feature.editor.api.diagnostic.JsonDiagnosticsProvider
import com.github.yumelira.yumebox.feature.editor.api.format.CodeFormatter
import com.github.yumelira.yumebox.feature.editor.api.language.LanguageScope
import io.github.rosemoe.sora.widget.CodeEditor

class CodeEditorState(
    initialContent: String = "",
    val language: LanguageScope = LanguageScope.Yaml,
    val readOnly: Boolean = false,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
) {
    var editor: CodeEditor? by mutableStateOf(null)
        internal set
    private var canUndoState: Boolean by mutableStateOf(false)
    private var canRedoState: Boolean by mutableStateOf(false)
    var content: String by mutableStateOf(initialContent)
        private set
    var isModified: Boolean by mutableStateOf(false)
        private set
    fun updateContent(newContent: String) {
        if (content != newContent) {
            content = newContent
            isModified = true
            editor?.setText(newContent)
        }
    }
    fun loadContent(newContent: String) {
        if (content != newContent) {
            content = newContent
            editor?.setText(newContent)
        }
        isModified = false
        refreshHistoryState()
    }
    fun syncContentFromEditor() {
        editor?.text?.toString()?.let { editorContent ->
            if (content != editorContent) {
                content = editorContent
                isModified = true
            }
        }
        refreshHistoryState()
    }
    fun undo() {
        editor?.undo()
        syncContentFromEditor()
    }
    fun redo() {
        editor?.redo()
        syncContentFromEditor()
    }
    fun canUndo(): Boolean = canUndoState
    fun canRedo(): Boolean = canRedoState
    internal fun refreshHistoryState() {
        canUndoState = editor?.canUndo() == true
        canRedoState = editor?.canRedo() == true
    }
    fun format(): Boolean {
        val formatted = CodeFormatter.format(content, language)
        return if (formatted != null && formatted != content) {
            content = formatted
            editor?.setText(formatted)
            isModified = true
            true
        } else {
            false
        }
    }
    fun validate(): Boolean = CodeFormatter.validate(content, language)
    fun updateDiagnostics() {
        val currentEditor = editor ?: return
        currentEditor.diagnostics = when (language) {
            LanguageScope.Json -> JsonDiagnosticsProvider.analyze(content)
            LanguageScope.Yaml, LanguageScope.Text -> null
        }
    }
}
