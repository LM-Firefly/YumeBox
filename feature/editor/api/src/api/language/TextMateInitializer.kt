package com.github.yumelira.yumebox.feature.editor.api.language

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import org.eclipse.tm4e.core.registry.IThemeSource
import timber.log.Timber

object TextMateInitializer {
    private const val TAG = "TextMateInitializer"
    private const val THEME_DARK = "dark-high-contrast"
    private const val THEME_LIGHT = "light"
    private var initialized = false
    @Synchronized
    fun initialize(context: Context) {
        if (initialized) {
            Timber.tag(TAG).d("TextMate already initialized, skipping")
            return
        }
        try {
            Timber.tag(TAG).i("Initializing TextMate...")
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.applicationContext.assets))
            loadThemes()
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            initialized = true
            Timber.tag(TAG).i("TextMate initialized successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize TextMate")
        }
    }
    fun setLanguage(editor: CodeEditor, language: LanguageScope) {
        if (language == LanguageScope.Text) {
            editor.setEditorLanguage(null)
            return
        }
        try {
            editor.setEditorLanguage(TextMateLanguage.create(language.scopeName, true))
            Timber.tag(TAG).d("Language set to: ${language.displayName}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to set language: ${language.displayName}")
            editor.setEditorLanguage(null)
        }
    }
    fun setTheme(isDark: Boolean) {
        try {
            ThemeRegistry.getInstance().setTheme(if (isDark) THEME_DARK else THEME_LIGHT)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to switch theme")
        }
    }
    private fun loadThemes() {
        val themeRegistry = ThemeRegistry.getInstance()
        loadTheme(themeRegistry, THEME_DARK, "textmate/dark-colorblind.json", true)
        loadTheme(themeRegistry, THEME_LIGHT, "textmate/light-colorblind.json", false)
        themeRegistry.setTheme(THEME_DARK)
    }
    private fun loadTheme(themeRegistry: ThemeRegistry, name: String, path: String, isDark: Boolean) {
        try {
            val inputStream = FileProviderRegistry.getInstance().tryGetInputStream(path)
            if (inputStream != null) {
                val themeSource = IThemeSource.fromInputStream(inputStream, path, null)
                themeRegistry.loadTheme(ThemeModel(themeSource, name).apply { this.isDark = isDark })
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to load theme: $name")
        }
    }
}
