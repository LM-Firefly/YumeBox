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



package com.github.yumelira.yumebox.presentation.util

enum class OverrideEditorSection(
    val title: String,
    val summary: String,
) {
    General(
        title = "全局配置",
        summary = "运行模式、控制器、持久化与 GEO",
    ),
    Dns(
        title = "DNS",
        summary = "基础开关、Fake-IP、上游与策略",
    ),
    Sniffer(
        title = "域名嗅探",
        summary = "策略开关、协议端口、跳过规则",
    ),
    Inbound(
        title = "入站",
        summary = "端口、鉴权、局域网访问",
    ),
    Tun(
        title = "Tun",
        summary = "入站 Tun、路由与应用范围",
    ),
    Rules(
        title = "路由规则",
        summary = "规则链与匹配顺序",
    ),
    Proxies(
        title = "出站代理",
        summary = "代理节点与协议对象",
    ),
    ProxyProviders(
        title = "代理集合",
        summary = "Proxy Providers 合并与覆盖",
    ),
    ProxyGroups(
        title = "代理组",
        summary = "Proxy Groups 前置、覆盖、后置",
    ),
    RuleProviders(
        title = "规则集合",
        summary = "Rule Providers 合并与覆盖",
    ),
    SubRules(
        title = "子规则",
        summary = "Sub Rules 分组与合并",
    ),
}

enum class OverrideModifierVisualMode(
    val label: String,
) {
    Replace("覆盖"),
    Start("前置追加"),
    End("后置追加"),
    Merge("合并"),
    Force("强制覆盖"),
}

sealed interface OverrideSaveState {
    data object Idle : OverrideSaveState
    data object Saving : OverrideSaveState
}

sealed interface OverrideSaveEvent {
    data class Saved(
        val configId: String,
    ) : OverrideSaveEvent

    data class Failed(
        val message: String,
    ) : OverrideSaveEvent
}

data class OverrideSectionSummary(
    val modifiedCount: Int,
    val visualModes: Set<OverrideModifierVisualMode>,
) {
    val summaryText: String
        get() {
            if (modifiedCount == 0) {
                return "未修改"
            }
            val modeSummary = visualModes.joinToString(" / ") { it.label }
            return buildString {
                append("${modifiedCount} 项")
                if (modeSummary.isNotEmpty()) {
                    append(" · ")
                    append(modeSummary)
                }
            }
        }
}

data class OverrideEditorOverview(
    val changedFieldCount: Int,
    val activeSectionCount: Int,
    val replaceCount: Int,
    val appendCount: Int,
    val mergeCount: Int,
    val forceCount: Int,
    val sectionSummaries: Map<OverrideEditorSection, OverrideSectionSummary>,
    val warnings: List<String>,
) {
    val modifierSummary: String
        get() = buildList {
            if (replaceCount > 0) add("覆盖 $replaceCount")
            if (appendCount > 0) add("追加 $appendCount")
            if (mergeCount > 0) add("合并 $mergeCount")
            if (forceCount > 0) add("强制 $forceCount")
        }.joinToString(" · ").ifEmpty { "未修改" }

    val sectionSummary: String
        get() = sectionSummaries
            .filterValues { it.modifiedCount > 0 }
            .keys
            .take(3)
            .joinToString(" · ") { it.title }
            .ifEmpty { "暂无改动" }
}
