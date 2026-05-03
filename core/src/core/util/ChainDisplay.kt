package com.github.yumelira.yumebox.core.util

fun resolveProxyChainOrder(
    chain: List<String>,
    reverse: Boolean = true,
): List<String> {
    val normalized = chain.filter { it.isNotBlank() }
    return if (reverse) normalized.asReversed() else normalized
}

fun buildRuleChain(
    rule: String,
    chain: List<String>,
    reverse: Boolean = true,
): List<String> {
    val orderedChain = resolveProxyChainOrder(chain, reverse)
    return buildList {
        if (rule.isNotBlank()) add(rule)
        addAll(orderedChain)
    }
}

fun formatProxyChain(
    parts: List<String>,
    separator: String = " -> ",
): String {
    return parts.filter { it.isNotBlank() }.joinToString(separator)
}
