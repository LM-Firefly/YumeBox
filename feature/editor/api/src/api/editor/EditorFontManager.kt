package com.github.yumelira.yumebox.feature.editor.api.editor

import android.content.Context
import android.graphics.Typeface
import timber.log.Timber

object EditorFontManager {
    private const val FONT_PATH = "fonts/JetBrainsMono-Regular.ttf"
    private var cachedFont: Typeface? = null
    fun getEditorTypeface(context: Context): Typeface {
        return cachedFont ?: try {
            Typeface.createFromAsset(context.assets, FONT_PATH).also {
                cachedFont = it
                Timber.d("JetBrainsMono font loaded successfully")
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load JetBrainsMono font, falling back to MONOSPACE")
            Typeface.MONOSPACE
        }
    }
}
