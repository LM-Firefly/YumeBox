/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */



package com.github.yumelira.yumebox.feature.editor.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

object EditorColorSynchronizer {

    private val DARK_BACKGROUND = Color(0xFF1E1E1E)
    private val DARK_TEXT = Color(0xFFD4D4D4)
    private val DARK_LINE_NUMBER = Color(0xFF858585)
    private val DARK_LINE_NUMBER_BG = Color(0xFF1E1E1E)
    private val DARK_CURRENT_LINE = Color(0xFF2D2D2D)
    private val DARK_SELECTION_BG = Color(0xFF264F78)
    private val DARK_TEXT_ACTION_BG = Color(0xFF2D2D2D)
    private val DARK_TEXT_ACTION_ICON = Color.White

    private val LIGHT_BACKGROUND = Color.White
    private val LIGHT_TEXT = Color(0xFF1E1E1E)
    private val LIGHT_LINE_NUMBER = Color(0xFF6E6E6E)
    private val LIGHT_LINE_NUMBER_BG = Color(0xFFF0F0F0)
    private val LIGHT_CURRENT_LINE = Color(0xFFF5F5F5)
    private val LIGHT_SELECTION_BG = Color(0xFFADD6FF)
    private val LIGHT_TEXT_ACTION_BG = Color(0xFFF0F0F0)
    private val LIGHT_TEXT_ACTION_ICON = Color(0xFF333333)

    private val ACCENT_COLOR = Color(0xFF007ACC)

    fun createColorScheme(isDark: Boolean): EditorColorScheme {
        return object : EditorColorScheme(isDark) {
            override fun applyDefault() {
                super.applyDefault()

                setColor(WHOLE_BACKGROUND, if (isDark) DARK_BACKGROUND.toArgb() else LIGHT_BACKGROUND.toArgb())
                setColor(TEXT_NORMAL, if (isDark) DARK_TEXT.toArgb() else LIGHT_TEXT.toArgb())
                setColor(LINE_NUMBER, if (isDark) DARK_LINE_NUMBER.toArgb() else LIGHT_LINE_NUMBER.toArgb())
                setColor(LINE_NUMBER_BACKGROUND, if (isDark) DARK_LINE_NUMBER_BG.toArgb() else LIGHT_LINE_NUMBER_BG.toArgb())
                setColor(CURRENT_LINE, if (isDark) DARK_CURRENT_LINE.toArgb() else LIGHT_CURRENT_LINE.toArgb())

                setColor(SELECTION_INSERT, ACCENT_COLOR.toArgb())
                setColor(SELECTION_HANDLE, ACCENT_COLOR.toArgb())
                setColor(SELECTED_TEXT_BACKGROUND, if (isDark) DARK_SELECTION_BG.toArgb() else LIGHT_SELECTION_BG.toArgb())

                setColor(TEXT_ACTION_WINDOW_BACKGROUND, if (isDark) DARK_TEXT_ACTION_BG.toArgb() else LIGHT_TEXT_ACTION_BG.toArgb())
                setColor(TEXT_ACTION_WINDOW_ICON_COLOR, if (isDark) DARK_TEXT_ACTION_ICON.toArgb() else LIGHT_TEXT_ACTION_ICON.toArgb())

                setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, if (isDark) Color(0xFF569CD6).toArgb() else Color(0xFF0000FF).toArgb())
            }
        }
    }

    fun updateColors(editor: CodeEditor, isDark: Boolean) {
        val scheme = editor.colorScheme

        scheme.setColor(EditorColorScheme.SELECTION_INSERT, ACCENT_COLOR.toArgb())
        scheme.setColor(EditorColorScheme.SELECTION_HANDLE, ACCENT_COLOR.toArgb())
        scheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND,
            if (isDark) DARK_SELECTION_BG.toArgb() else LIGHT_SELECTION_BG.toArgb())

        scheme.setColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND,
            if (isDark) DARK_TEXT_ACTION_BG.toArgb() else LIGHT_TEXT_ACTION_BG.toArgb())
        scheme.setColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR,
            if (isDark) DARK_TEXT_ACTION_ICON.toArgb() else LIGHT_TEXT_ACTION_ICON.toArgb())

        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND,
            if (isDark) Color(0xFF569CD6).toArgb() else Color(0xFF0000FF).toArgb())
        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND,
            if (isDark) Color(0x2646A2D4).toArgb() else Color(0x2646A2D4).toArgb())
    }
}
