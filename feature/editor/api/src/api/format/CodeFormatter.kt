package com.github.yumelira.yumebox.feature.editor.api.format

import com.github.yumelira.yumebox.feature.editor.api.language.LanguageScope
import org.json.JSONArray
import org.json.JSONObject

object CodeFormatter {
    fun format(content: String, language: LanguageScope): String? {
        return when (language) {
            LanguageScope.Json -> formatJson(content)
            LanguageScope.Yaml -> formatYaml(content)
            LanguageScope.Text -> content
        }
    }
    fun validate(content: String, language: LanguageScope): Boolean {
        return when (language) {
            LanguageScope.Json -> validateJson(content)
            LanguageScope.Yaml, LanguageScope.Text -> true
        }
    }
    private fun formatJson(content: String): String? {
        return try {
            val trimmed = content.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
    private fun validateJson(content: String): Boolean {
        return try {
            val trimmed = content.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed)
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> return false
            }
            true
        } catch (_: Exception) {
            false
        }
    }
    private fun formatYaml(content: String): String? {
        return try {
            content.lines().map { it.trimEnd() }.joinToString("\n").replace(Regex("\n{3,}"), "\n\n")
        } catch (_: Exception) {
            null
        }
    }
}
