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



package com.github.yumelira.yumebox.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Save
import com.github.yumelira.yumebox.presentation.util.OverrideExtraFieldDraft
import com.github.yumelira.yumebox.presentation.util.OverrideProxyDraft
import com.github.yumelira.yumebox.presentation.util.OverrideProxyTypePresets
import com.github.yumelira.yumebox.presentation.util.OverrideStructuredEditorStore
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.extra.WindowDropdown

@Composable
fun OverrideProxyDraftEditorScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val title = remember { OverrideStructuredEditorStore.proxyDraftEditorTitle.ifBlank { "代理节点" } }
    val initialValue = remember { OverrideStructuredEditorStore.proxyDraftEditorValue }
    val saveFabController = rememberOverrideFabController()

    var name by remember { mutableStateOf(initialValue?.name.orEmpty()) }
    var type by remember { mutableStateOf(initialValue?.type?.ifBlank { "ss" } ?: "ss") }
    var server by remember { mutableStateOf(initialValue?.server.orEmpty()) }
    var portText by remember { mutableStateOf(initialValue?.port?.toString().orEmpty()) }
    var ipVersion by remember { mutableStateOf(initialValue?.ipVersion.orEmpty()) }
    var interfaceName by remember { mutableStateOf(initialValue?.interfaceName.orEmpty()) }
    var routingMarkText by remember { mutableStateOf(initialValue?.routingMark?.toString().orEmpty()) }
    var dialerProxy by remember { mutableStateOf(initialValue?.dialerProxy.orEmpty()) }
    var udp by remember { mutableStateOf(initialValue?.udp) }
    var tfo by remember { mutableStateOf(initialValue?.tfo) }
    var mptcp by remember { mutableStateOf(initialValue?.mptcp) }
    var extraFields by remember { mutableStateOf(initialValue?.extraFields.orEmpty()) }
    var editingExtraKey by remember { mutableStateOf<String?>(null) }
    var showExtraFieldDialog by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val selectedPresetIndex = OverrideProxyTypePresets.indexOfFirst {
        it.equals(type, ignoreCase = true)
    }.coerceAtLeast(0)

    DisposableEffect(Unit) {
        onDispose {
            OverrideStructuredEditorStore.clearProxyDraftEditor()
        }
    }

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = saveFabController,
                visible = true,
                imageVector = Yume.Save,
                contentDescription = "保存代理节点",
                onClick = {
                    if (name.trim().isBlank()) {
                        errorText = "名称不能为空"
                        return@OverrideAnimatedFab
                    }
                    if (type.trim().isBlank()) {
                        errorText = "类型不能为空"
                        return@OverrideAnimatedFab
                    }
                    OverrideStructuredEditorStore.submitProxyDraft(
                        OverrideProxyDraft(
                            name = name.trim(),
                            type = type.trim(),
                            server = server.trim(),
                            port = portText.trim().toIntOrNull(),
                            ipVersion = ipVersion.trim(),
                            udp = udp,
                            interfaceName = interfaceName.trim(),
                            routingMark = routingMarkText.trim().toIntOrNull(),
                            tfo = tfo,
                            mptcp = mptcp,
                            dialerProxy = dialerProxy.trim(),
                            extraFields = extraFields,
                            uiId = initialValue?.uiId ?: OverrideProxyDraft().uiId,
                        ),
                    )
                    navigator.navigateUp()
                },
            )
        },
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
            lazyListState = listState,
            onScrollDirectionChanged = saveFabController::onScrollDirectionChanged,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
                ) {
                    OverrideSection("基础连接") {
                        OverrideSelectorCard {
                            WindowDropdown(
                                title = "类型",
                                items = OverrideProxyTypePresets,
                                selectedIndex = selectedPresetIndex,
                                onSelectedIndexChange = { index ->
                                    if (index in OverrideProxyTypePresets.indices) {
                                        type = OverrideProxyTypePresets[index]
                                        errorText = null
                                    }
                                },
                            )
                        }
                        OverrideFormFieldColumn {
                            OverrideFormField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    errorText = null
                                },
                                label = "名称",
                                errorText = errorText?.takeIf { it.contains("名称") },
                            )
                            OverrideFormField(
                                value = server,
                                onValueChange = { server = it },
                                label = "server",
                            )
                            OverrideFormField(
                                value = portText,
                                onValueChange = { portText = it.filter(Char::isDigit) },
                                label = "port",
                                supportText = "留空表示不覆写端口",
                            )
                        }
                    }
                    OverridePlainFormSection("网络与路由") {
                        OverrideFormField(
                            value = ipVersion,
                            onValueChange = { ipVersion = it },
                            label = "ip-version",
                        )
                        OverrideFormField(
                            value = interfaceName,
                            onValueChange = { interfaceName = it },
                            label = "interface-name",
                        )
                        OverrideFormField(
                            value = routingMarkText,
                            onValueChange = { routingMarkText = it.filter(Char::isDigit) },
                            label = "routing-mark",
                        )
                        OverrideFormField(
                            value = dialerProxy,
                            onValueChange = { dialerProxy = it },
                            label = "dialer-proxy",
                        )
                    }
                    OverrideCardSection("选项") {
                        NullableBooleanSelector(
                            title = "udp",
                            value = udp,
                            onValueChange = { udp = it },
                        )
                        NullableBooleanSelector(
                            title = "tfo",
                            value = tfo,
                            onValueChange = { tfo = it },
                        )
                        NullableBooleanSelector(
                            title = "mptcp",
                            value = mptcp,
                            onValueChange = { mptcp = it },
                        )
                    }
                    OverrideSection("额外字段") {
                        OverrideExtraFieldsCard(
                            title = "额外字段",
                            fields = extraFields,
                            onAddClick = {
                                editingExtraKey = null
                                showExtraFieldDialog = true
                            },
                            onEditClick = { key, _ ->
                                editingExtraKey = key
                                showExtraFieldDialog = true
                            },
                            onDeleteClick = { key ->
                                extraFields = extraFields - key
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(OverrideSectionBottomSpacing))
                }
            }
        }
        OverrideExtraFieldDialog(
            show = showExtraFieldDialog,
            title = if (editingExtraKey == null) "新增额外字段" else "编辑额外字段",
            initialValue = editingExtraKey?.let(extraFields::toExtraFieldDraft),
            onConfirm = { draft: OverrideExtraFieldDraft ->
                extraFields = extraFields.updateExtraField(editingExtraKey, draft)
                editingExtraKey = null
                showExtraFieldDialog = false
            },
            onDismiss = {
                editingExtraKey = null
                showExtraFieldDialog = false
            },
        )
    }
}
