package com.github.yumelira.yumebox.feature.editor.api.language

enum class LanguageScope(
    val scopeName: String,
    val displayName: String,
) {
    Yaml("source.yaml", "YAML"),
    Json("source.json", "JSON"),
    Text("text.plain", "Plain Text");
    companion object {
        fun fromExtension(extension: String): LanguageScope {
            return when (extension.lowercase()) {
                "yaml", "yml" -> Yaml
                "json" -> Json
                else -> Text
            }
        }
        fun fromScopeName(scopeName: String): LanguageScope {
            return entries.find { it.scopeName == scopeName } ?: Text
        }
    }
}
