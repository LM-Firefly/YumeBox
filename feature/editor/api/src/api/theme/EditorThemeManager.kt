package com.github.yumelira.yumebox.feature.editor.api.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import com.github.yumelira.yumebox.feature.editor.api.language.TextMateInitializer
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.widget.CodeEditor
import top.yukonga.miuix.kmp.theme.MiuixTheme

object EditorThemeManager {
    @Composable
    fun rememberEditorTheme(): EditorThemeState {
        val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
        return remember(isDark) {
            EditorThemeState(
                isDark = isDark,
                themeName = if (isDark) "dark-high-contrast" else "light",
            )
        }
    }
    fun applyTheme(editor: CodeEditor) {
        try {
            editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        } catch (_: Exception) {
            editor.colorScheme = EditorColorSynchronizer.createColorScheme(false)
        }
    }
    fun updateTheme(editor: CodeEditor, isDark: Boolean) {
        try {
            TextMateInitializer.setTheme(isDark)
            editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        } catch (_: Exception) {
            editor.colorScheme = EditorColorSynchronizer.createColorScheme(isDark)
        }
    }
}

data class EditorThemeState(
    val isDark: Boolean,
    val themeName: String,
)
