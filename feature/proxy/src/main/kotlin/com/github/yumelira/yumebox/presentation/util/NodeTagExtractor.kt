package com.github.yumelira.yumebox.presentation.util

private val NODE_KEYWORDS = listOf(
    "IEPL", "BGP", "APL", "IPLC", "CMI", "CN2", "GIA", "MPLS",
    "专线", "中转", "游戏", "企业", "NF", "Netflix", "Disney", "GPT",
)

private val MULTIPLIER_REGEX = Regex(
    """(?<![.\d])[xX×✕](\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)[xX×✕](?![.\d])"""
)

data class NodeTags(
    val keywords: List<String>,
    val multiplier: Float?,
)

fun extractNodeTags(name: String): NodeTags {
    val upperName = name.uppercase()
    val keywords = NODE_KEYWORDS.filter { kw -> upperName.contains(kw.uppercase()) }
    val multiplierMatch = MULTIPLIER_REGEX.find(name)
    val multiplier = multiplierMatch?.let { m ->
        val v = m.groupValues[1].ifEmpty { m.groupValues[2] }
        v.toFloatOrNull()
    }
    return NodeTags(keywords = keywords, multiplier = multiplier)
}
