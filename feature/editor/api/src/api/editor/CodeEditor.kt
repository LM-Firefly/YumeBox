package com.github.yumelira.yumebox.feature.editor.api.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.github.yumelira.yumebox.feature.editor.api.language.TextMateInitializer
import com.github.yumelira.yumebox.feature.editor.api.theme.EditorThemeManager
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways
import timber.log.Timber

@Composable
fun CodeEditor(
    state: CodeEditorState,
    modifier: Modifier = Modifier,
    onTextChange: ((String) -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val editorRef = remember { mutableStateOf<CodeEditor?>(null) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> Timber.d("CodeEditor: onPause")
                Lifecycle.Event.ON_RESUME -> Timber.d("CodeEditor: onResume")
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            editorRef.value?.runCatching { release() }
            state.editor = null
            editorRef.value = null
        }
    }
    val editorThemeState = EditorThemeManager.rememberEditorTheme()
    LaunchedEffect(editorThemeState.isDark) {
        editorRef.value?.let { editor ->
            EditorThemeManager.updateTheme(editor, editorThemeState.isDark)
        }
    }
    LaunchedEffect(state.content) {
        val editor = editorRef.value
        if (editor != null && editor.text.toString() != state.content) {
            editor.setText(state.content)
        }
    }
    AndroidView(
        factory = { ctx ->
            createCodeEditor(ctx, state, editorThemeState.isDark, onTextChange).also { editor ->
                state.editor = editor
                state.refreshHistoryState()
                editorRef.value = editor
            }
        },
        modifier = modifier,
        onRelease = { editor -> editor.runCatching { release() } },
    )
}

private fun createCodeEditor(
    context: android.content.Context,
    state: CodeEditorState,
    isDark: Boolean,
    onTextChange: ((String) -> Unit)?,
): CodeEditor {
    TextMateInitializer.initialize(context)
    return CodeEditor(context).apply {
        isEditable = !state.readOnly
        val font = EditorFontManager.getEditorTypeface(context)
        typefaceText = font
        typefaceLineNumber = font
        setScrollBarEnabled(false)
        nonPrintablePaintingFlags = CodeEditor.FLAG_DRAW_LINE_SEPARATOR
        TextMateInitializer.setLanguage(this, state.language)
        EditorThemeManager.applyTheme(this)
        EditorThemeManager.updateTheme(this, isDark)
        setText(state.content)
        subscribeAlways<ContentChangeEvent> {
            state.syncContentFromEditor()
            state.updateDiagnostics()
            onTextChange?.invoke(state.content)
        }
        state.updateDiagnostics()
    }
}
