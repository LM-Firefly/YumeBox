package com.github.yumelira.yumebox.feature.editor.api.diagnostic

import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object JsonDiagnosticsProvider {
    fun analyze(content: String): DiagnosticsContainer {
        val container = DiagnosticsContainer()
        if (content.isBlank()) return container
        val trimmed = content.trim()
        try {
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed)
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> container.addDiagnostic(
                    DiagnosticRegion(
                        0,
                        content.length.coerceAtMost(1),
                        DiagnosticRegion.SEVERITY_ERROR,
                        0,
                        DiagnosticDetail(
                            briefMessage = "JSON 格式错误",
                            detailedMessage = "JSON 必须以 '{' 或 '[' 开头",
                        ),
                    ),
                )
            }
        } catch (e: JSONException) {
            parseJsonException(e, content)?.let(container::addDiagnostic)
        } catch (e: Exception) {
            Timber.w(e, "JSON analysis failed")
        }
        return container
    }
    private fun parseJsonException(e: JSONException, content: String): DiagnosticRegion? {
        val message = e.message ?: return null
        val errorIndex = "character (\\d+)".toRegex().find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val safeIndex = errorIndex.coerceIn(0, content.length - 1)
        return DiagnosticRegion(
            safeIndex,
            (safeIndex + 1).coerceAtMost(content.length),
            DiagnosticRegion.SEVERITY_ERROR,
            0,
            DiagnosticDetail(
                briefMessage = "JSON 语法错误",
                detailedMessage = formatErrorMessage(message),
            ),
        )
    }
    private fun formatErrorMessage(message: String): String {
        return when {
            message.contains("Unterminated") -> "未终止的字符串或对象"
            message.contains("Expected") -> "期望 ${"Expected (\\S+)".toRegex().find(message)?.groupValues?.get(1) ?: "未知"}"
            message.contains("No value") -> "缺少值"
            message.contains("Duplicate") -> "重复的键"
            else -> message
        }
    }
}
