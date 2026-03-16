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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Save
import com.github.yumelira.yumebox.presentation.util.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.serialization.json.*
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

private val ProviderKnownKeys = setOf(
    "type",
    "path",
    "url",
    "proxy",
    "behavior",
    "format",
    "vehicle",
    "interval",
    "size-limit",
    "header",
    "health-check",
    "override",
    "filter",
    "enable",
    "disable-udp",
)

private val ProviderHealthCheckKnownKeys = setOf(
    "enable",
    "url",
    "interval",
    "timeout",
    "lazy",
    "expected-status",
)

private val ProviderOverrideKnownKeys = setOf(
    "tfo",
    "mptcp",
    "udp",
    "udp-over-tcp",
    "down",
    "up",
    "skip-cert-verify",
    "dialer-proxy",
    "interface-name",
    "routing-mark",
    "ip-version",
    "additional-prefix",
    "additional-suffix",
)

@Composable
fun OverrideKeyedObjectDraftEditorScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val title = remember {
        OverrideStructuredEditorStore.keyedObjectDraftEditorTitle.ifBlank { "对象" }
    }
    val editorType = remember { OverrideStructuredEditorStore.keyedObjectDraftEditorType }
    val initialValue = remember { OverrideStructuredEditorStore.keyedObjectDraftEditorValue }
    val saveFabController = rememberOverrideFabController()
    val initialFields = initialValue?.fields.orEmpty()

    var key by remember { mutableStateOf(initialValue?.key.orEmpty()) }
    var type by remember { mutableStateOf(initialFields.stringField("type")) }
    var path by remember { mutableStateOf(initialFields.stringField("path")) }
    var url by remember { mutableStateOf(initialFields.stringField("url")) }
    var proxy by remember { mutableStateOf(initialFields.stringField("proxy")) }
    var behavior by remember { mutableStateOf(initialFields.stringField("behavior")) }
    var format by remember { mutableStateOf(initialFields.stringField("format")) }
    var vehicle by remember { mutableStateOf(initialFields.stringField("vehicle")) }
    var intervalText by remember { mutableStateOf(initialFields.intField("interval")?.toString().orEmpty()) }
    var sizeLimitText by remember { mutableStateOf(initialFields.intField("size-limit")?.toString().orEmpty()) }
    var headerText by remember { mutableStateOf(initialFields.headerField("header")) }
    val initialHealthCheckFields = remember { initialFields.objectField("health-check").orEmpty() }
    val initialOverrideFields = remember { initialFields.objectField("override").orEmpty() }
    var healthCheckEnable by remember { mutableStateOf(initialHealthCheckFields.booleanField("enable")) }
    var healthCheckUrl by remember { mutableStateOf(initialHealthCheckFields.stringField("url")) }
    var healthCheckIntervalText by remember { mutableStateOf(initialHealthCheckFields.intField("interval")?.toString().orEmpty()) }
    var healthCheckTimeoutText by remember { mutableStateOf(initialHealthCheckFields.intField("timeout")?.toString().orEmpty()) }
    var healthCheckLazy by remember { mutableStateOf(initialHealthCheckFields.booleanField("lazy")) }
    var healthCheckExpectedStatus by remember { mutableStateOf(initialHealthCheckFields.stringField("expected-status")) }
    var healthCheckExtraFields by remember {
        mutableStateOf(initialHealthCheckFields.filterKeys { it !in ProviderHealthCheckKnownKeys })
    }
    var overrideTfo by remember { mutableStateOf(initialOverrideFields.booleanField("tfo")) }
    var overrideMptcp by remember { mutableStateOf(initialOverrideFields.booleanField("mptcp")) }
    var overrideUdp by remember { mutableStateOf(initialOverrideFields.booleanField("udp")) }
    var overrideUdpOverTcp by remember { mutableStateOf(initialOverrideFields.booleanField("udp-over-tcp")) }
    var overrideDown by remember { mutableStateOf(initialOverrideFields.stringField("down")) }
    var overrideUp by remember { mutableStateOf(initialOverrideFields.stringField("up")) }
    var overrideSkipCertVerify by remember { mutableStateOf(initialOverrideFields.booleanField("skip-cert-verify")) }
    var overrideDialerProxy by remember { mutableStateOf(initialOverrideFields.stringField("dialer-proxy")) }
    var overrideInterfaceName by remember { mutableStateOf(initialOverrideFields.stringField("interface-name")) }
    var overrideRoutingMarkText by remember { mutableStateOf(initialOverrideFields.intField("routing-mark")?.toString().orEmpty()) }
    var overrideIpVersion by remember { mutableStateOf(initialOverrideFields.stringField("ip-version")) }
    var additionalPrefix by remember { mutableStateOf(initialOverrideFields.stringField("additional-prefix")) }
    var additionalSuffix by remember { mutableStateOf(initialOverrideFields.stringField("additional-suffix")) }
    var providerOverrideExtraFields by remember {
        mutableStateOf(initialOverrideFields.filterKeys { it !in ProviderOverrideKnownKeys })
    }
    var filter by remember { mutableStateOf(initialFields.stringField("filter")) }
    var enable by remember { mutableStateOf(initialFields.booleanField("enable")) }
    var disableUdp by remember { mutableStateOf(initialFields.booleanField("disable-udp")) }
    var extraFields by remember { mutableStateOf(initialFields.filterKeys { it !in ProviderKnownKeys }) }
    var editingExtraKey by remember { mutableStateOf<String?>(null) }
    var editingHealthCheckExtraKey by remember { mutableStateOf<String?>(null) }
    var editingOverrideExtraKey by remember { mutableStateOf<String?>(null) }
    var showExtraFieldDialog by remember { mutableStateOf(false) }
    var showHealthCheckExtraFieldDialog by remember { mutableStateOf(false) }
    var showOverrideExtraFieldDialog by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val keyLabel = "名称"

    DisposableEffect(Unit) {
        onDispose {
            OverrideStructuredEditorStore.clearKeyedObjectDraftEditor()
        }
    }

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = saveFabController,
                visible = true,
                imageVector = Yume.Save,
                contentDescription = "保存${editorType.itemLabel}",
                onClick = {
                    if (key.trim().isBlank()) {
                        errorText = "名称不能为空"
                        return@OverrideAnimatedFab
                    }
                    OverrideStructuredEditorStore.submitKeyedObjectDraft(
                        OverrideKeyedObjectDraft(
                            key = key.trim(),
                            fields = linkedMapOf<String, JsonElement>().apply {
                                putAll(extraFields)
                                putStringField("type", type)
                                putStringField("path", path)
                                putStringField("url", url)
                                putStringField("proxy", proxy)
                                putStringField("behavior", behavior)
                                putStringField("format", format)
                                putStringField("vehicle", vehicle)
                                putIntField("interval", intervalText.trim().toIntOrNull())
                                putIntField("size-limit", sizeLimitText.trim().toIntOrNull())
                                putObjectField("header", parseHeaderEditorText(headerText))
                                putStringField("filter", filter)
                                putBooleanField("enable", enable)
                                putBooleanField("disable-udp", disableUdp)
                                putObjectField(
                                    "health-check",
                                    linkedMapOf<String, JsonElement>().apply {
                                        putAll(healthCheckExtraFields)
                                        putBooleanField("enable", healthCheckEnable)
                                        putStringField("url", healthCheckUrl)
                                        putIntField("interval", healthCheckIntervalText.trim().toIntOrNull())
                                        putIntField("timeout", healthCheckTimeoutText.trim().toIntOrNull())
                                        putBooleanField("lazy", healthCheckLazy)
                                        putStringField("expected-status", healthCheckExpectedStatus)
                                    },
                                )
                                putObjectField(
                                    "override",
                                    linkedMapOf<String, JsonElement>().apply {
                                        putAll(providerOverrideExtraFields)
                                        putBooleanField("tfo", overrideTfo)
                                        putBooleanField("mptcp", overrideMptcp)
                                        putBooleanField("udp", overrideUdp)
                                        putBooleanField("udp-over-tcp", overrideUdpOverTcp)
                                        putStringField("down", overrideDown)
                                        putStringField("up", overrideUp)
                                        putBooleanField("skip-cert-verify", overrideSkipCertVerify)
                                        putStringField("dialer-proxy", overrideDialerProxy)
                                        putStringField("interface-name", overrideInterfaceName)
                                        putIntField("routing-mark", overrideRoutingMarkText.trim().toIntOrNull())
                                        putStringField("ip-version", overrideIpVersion)
                                        putStringField("additional-prefix", additionalPrefix)
                                        putStringField("additional-suffix", additionalSuffix)
                                    },
                                )
                            },
                            uiId = initialValue?.uiId ?: OverrideKeyedObjectDraft().uiId,
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
                OverridePlainFormSection("基础身份") {
                    OverrideFormField(
                        value = key,
                        onValueChange = {
                            key = it
                            errorText = null
                        },
                        label = keyLabel,
                        errorText = errorText?.takeIf { it.contains("名称") },
                    )
                    OverrideFormField(
                        value = type,
                        onValueChange = { type = it },
                        label = "type",
                    )
                    OverrideFormField(
                        value = vehicle,
                        onValueChange = { vehicle = it },
                        label = "vehicle",
                    )
                }
            }
            item {
                OverridePlainFormSection("核心来源") {
                    OverrideFormField(
                        value = path,
                        onValueChange = { path = it },
                        label = "path",
                    )
                    OverrideFormField(
                        value = url,
                        onValueChange = { url = it },
                        label = "url",
                    )
                    OverrideFormField(
                        value = proxy,
                        onValueChange = { proxy = it },
                        label = "proxy",
                    )
                    OverrideFormField(
                        value = intervalText,
                        onValueChange = { intervalText = it.filter(Char::isDigit) },
                        label = "interval",
                    )
                    OverrideFormField(
                        value = sizeLimitText,
                        onValueChange = { sizeLimitText = it.filter(Char::isDigit) },
                        label = "size-limit",
                    )
                }
            }
            item {
                OverridePlainFormSection("网络与认证") {
                    OverrideFormField(
                        value = behavior,
                        onValueChange = { behavior = it },
                        label = "behavior",
                    )
                    OverrideFormField(
                        value = format,
                        onValueChange = { format = it },
                        label = "format",
                    )
                    OverrideFormField(
                        value = filter,
                        onValueChange = { filter = it },
                        label = "filter",
                    )
                    OverrideFormField(
                        value = headerText,
                        onValueChange = { headerText = it },
                        label = "header",
                        supportText = "每行一个 header，格式：Key: value1 | value2",
                        modifier = Modifier.height(120.dp),
                        maxLines = 8,
                    )
                }
            }
            if (editorType == OverrideStructuredMapType.ProxyProviders) {
                item {
                    OverridePlainFormSection("Health Check") {
                        OverrideFormField(
                            value = healthCheckUrl,
                            onValueChange = { healthCheckUrl = it },
                            label = "health-check.url",
                        )
                        OverrideFormField(
                            value = healthCheckIntervalText,
                            onValueChange = { healthCheckIntervalText = it.filter(Char::isDigit) },
                            label = "health-check.interval",
                        )
                        OverrideFormField(
                            value = healthCheckTimeoutText,
                            onValueChange = { healthCheckTimeoutText = it.filter(Char::isDigit) },
                            label = "health-check.timeout",
                        )
                        OverrideFormField(
                            value = healthCheckExpectedStatus,
                            onValueChange = { healthCheckExpectedStatus = it },
                            label = "health-check.expected-status",
                        )
                    }
                }
                item {
                    OverrideCardSection("Health Check 开关") {
                        NullableBooleanSelector(
                            title = "health-check.enable",
                            value = healthCheckEnable,
                            onValueChange = { healthCheckEnable = it },
                        )
                        NullableBooleanSelector(
                            title = "health-check.lazy",
                            value = healthCheckLazy,
                            onValueChange = { healthCheckLazy = it },
                        )
                    }
                }
                item {
                    OverrideSection("Health Check 额外字段") {
                        OverrideExtraFieldsCard(
                            title = "health-check 额外字段",
                            fields = healthCheckExtraFields,
                            onAddClick = {
                                editingHealthCheckExtraKey = null
                                showHealthCheckExtraFieldDialog = true
                            },
                            onEditClick = { entryKey, _ ->
                                editingHealthCheckExtraKey = entryKey
                                showHealthCheckExtraFieldDialog = true
                            },
                            onDeleteClick = { entryKey ->
                                healthCheckExtraFields = healthCheckExtraFields - entryKey
                            },
                        )
                    }
                }
                item {
                    OverridePlainFormSection("Override") {
                        OverrideFormField(
                            value = overrideDown,
                            onValueChange = { overrideDown = it },
                            label = "override.down",
                        )
                        OverrideFormField(
                            value = overrideUp,
                            onValueChange = { overrideUp = it },
                            label = "override.up",
                        )
                        OverrideFormField(
                            value = overrideDialerProxy,
                            onValueChange = { overrideDialerProxy = it },
                            label = "override.dialer-proxy",
                        )
                        OverrideFormField(
                            value = overrideInterfaceName,
                            onValueChange = { overrideInterfaceName = it },
                            label = "override.interface-name",
                        )
                        OverrideFormField(
                            value = overrideRoutingMarkText,
                            onValueChange = { overrideRoutingMarkText = it.filter(Char::isDigit) },
                            label = "override.routing-mark",
                        )
                        OverrideFormField(
                            value = overrideIpVersion,
                            onValueChange = { overrideIpVersion = it },
                            label = "override.ip-version",
                        )
                        OverrideFormField(
                            value = additionalPrefix,
                            onValueChange = { additionalPrefix = it },
                            label = "override.additional-prefix",
                        )
                        OverrideFormField(
                            value = additionalSuffix,
                            onValueChange = { additionalSuffix = it },
                            label = "override.additional-suffix",
                        )
                    }
                }
                item {
                    OverrideCardSection("Override 开关") {
                        NullableBooleanSelector(
                            title = "override.tfo",
                            value = overrideTfo,
                            onValueChange = { overrideTfo = it },
                        )
                        NullableBooleanSelector(
                            title = "override.mptcp",
                            value = overrideMptcp,
                            onValueChange = { overrideMptcp = it },
                        )
                        NullableBooleanSelector(
                            title = "override.udp",
                            value = overrideUdp,
                            onValueChange = { overrideUdp = it },
                        )
                        NullableBooleanSelector(
                            title = "override.udp-over-tcp",
                            value = overrideUdpOverTcp,
                            onValueChange = { overrideUdpOverTcp = it },
                        )
                        NullableBooleanSelector(
                            title = "override.skip-cert-verify",
                            value = overrideSkipCertVerify,
                            onValueChange = { overrideSkipCertVerify = it },
                        )
                    }
                }
                item {
                    OverrideSection("Override 额外字段") {
                        OverrideExtraFieldsCard(
                            title = "override 额外字段",
                            fields = providerOverrideExtraFields,
                            onAddClick = {
                                editingOverrideExtraKey = null
                                showOverrideExtraFieldDialog = true
                            },
                            onEditClick = { entryKey, _ ->
                                editingOverrideExtraKey = entryKey
                                showOverrideExtraFieldDialog = true
                            },
                            onDeleteClick = { entryKey ->
                                providerOverrideExtraFields = providerOverrideExtraFields - entryKey
                            },
                        )
                    }
                }
            } else {
                item {
                    OverridePlainFormSection("Health Check") {
                        OverrideFormField(
                            value = healthCheckUrl,
                            onValueChange = { healthCheckUrl = it },
                            label = "health-check.url",
                        )
                        OverrideFormField(
                            value = healthCheckIntervalText,
                            onValueChange = { healthCheckIntervalText = it.filter(Char::isDigit) },
                            label = "health-check.interval",
                        )
                        OverrideFormField(
                            value = healthCheckTimeoutText,
                            onValueChange = { healthCheckTimeoutText = it.filter(Char::isDigit) },
                            label = "health-check.timeout",
                        )
                        OverrideFormField(
                            value = healthCheckExpectedStatus,
                            onValueChange = { healthCheckExpectedStatus = it },
                            label = "health-check.expected-status",
                        )
                    }
                }
                item {
                    OverrideCardSection("Health Check 开关") {
                        NullableBooleanSelector(
                            title = "health-check.enable",
                            value = healthCheckEnable,
                            onValueChange = { healthCheckEnable = it },
                        )
                        NullableBooleanSelector(
                            title = "health-check.lazy",
                            value = healthCheckLazy,
                            onValueChange = { healthCheckLazy = it },
                        )
                    }
                }
            }
            item {
                OverrideCardSection("布尔选项") {
                    NullableBooleanSelector(
                        title = "enable",
                        value = enable,
                        onValueChange = { enable = it },
                    )
                    NullableBooleanSelector(
                        title = "disable-udp",
                        value = disableUdp,
                        onValueChange = { disableUdp = it },
                    )
                }
            }
            item {
                OverrideSection("额外字段") {
                    OverrideExtraFieldsCard(
                        title = "额外字段",
                        fields = extraFields,
                        onAddClick = {
                            editingExtraKey = null
                            showExtraFieldDialog = true
                        },
                        onEditClick = { entryKey, _ ->
                            editingExtraKey = entryKey
                            showExtraFieldDialog = true
                        },
                        onDeleteClick = { entryKey ->
                            extraFields = extraFields - entryKey
                        },
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(OverrideSectionBottomSpacing))
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
        OverrideExtraFieldDialog(
            show = showHealthCheckExtraFieldDialog,
            title = if (editingHealthCheckExtraKey == null) {
                "新增 health-check 额外字段"
            } else {
                "编辑 health-check 额外字段"
            },
            initialValue = editingHealthCheckExtraKey?.let(healthCheckExtraFields::toExtraFieldDraft),
            onConfirm = { draft: OverrideExtraFieldDraft ->
                healthCheckExtraFields = healthCheckExtraFields.updateExtraField(
                    editingHealthCheckExtraKey,
                    draft,
                )
                editingHealthCheckExtraKey = null
                showHealthCheckExtraFieldDialog = false
            },
            onDismiss = {
                editingHealthCheckExtraKey = null
                showHealthCheckExtraFieldDialog = false
            },
        )
        OverrideExtraFieldDialog(
            show = showOverrideExtraFieldDialog,
            title = if (editingOverrideExtraKey == null) {
                "新增 override 额外字段"
            } else {
                "编辑 override 额外字段"
            },
            initialValue = editingOverrideExtraKey?.let(providerOverrideExtraFields::toExtraFieldDraft),
            onConfirm = { draft: OverrideExtraFieldDraft ->
                providerOverrideExtraFields = providerOverrideExtraFields.updateExtraField(
                    editingOverrideExtraKey,
                    draft,
                )
                editingOverrideExtraKey = null
                showOverrideExtraFieldDialog = false
            },
            onDismiss = {
                editingOverrideExtraKey = null
                showOverrideExtraFieldDialog = false
            },
        )
    }
}

private fun Map<String, JsonElement>.headerField(key: String): String {
    return objectField(key)
        ?.entries
        ?.joinToString("\n") { (headerKey, headerValue) ->
            val values = when (headerValue) {
                is JsonArray -> headerValue.jsonArray.mapNotNull { item ->
                    (item as? JsonPrimitive)?.content?.trim()?.takeIf(String::isNotBlank)
                }

                is JsonPrimitive -> listOf(headerValue.content).filter(String::isNotBlank)
                else -> emptyList()
            }
            if (values.isEmpty()) {
                headerKey
            } else {
                "$headerKey: ${values.joinToString(" | ")}"
            }
        }
        .orEmpty()
}

private fun parseHeaderEditorText(rawValue: String): Map<String, JsonElement>? {
    val headerEntries = linkedMapOf<String, JsonElement>()
    rawValue.lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .forEach { line ->
            val separatorIndex = line.indexOf(':')
            if (separatorIndex < 0) {
                return@forEach
            }
            val headerKey = line.substring(0, separatorIndex).trim()
            if (headerKey.isBlank()) {
                return@forEach
            }
            val headerValues = line.substring(separatorIndex + 1)
                .split('|')
                .map(String::trim)
                .filter(String::isNotBlank)
            headerEntries[headerKey] = JsonArray(headerValues.map(::JsonPrimitive))
        }

    return headerEntries.ifEmpty { null }
}

private fun Map<String, JsonElement>.stringField(key: String): String {
    val element = get(key) ?: return ""
    return if (element is JsonPrimitive && element.isString) {
        element.content
    } else {
        element.toString()
    }
}

private fun Map<String, JsonElement>.intField(key: String): Int? {
    return get(key)?.jsonPrimitive?.intOrNull
}

private fun Map<String, JsonElement>.booleanField(key: String): Boolean? {
    return get(key)?.jsonPrimitive?.booleanOrNull
}

private fun MutableMap<String, JsonElement>.putStringField(key: String, value: String) {
    value.trim().takeIf(String::isNotBlank)?.let {
        put(key, JsonPrimitive(it))
    }
}

private fun MutableMap<String, JsonElement>.putIntField(key: String, value: Int?) {
    value?.let {
        put(key, JsonPrimitive(it))
    }
}

private fun MutableMap<String, JsonElement>.putBooleanField(key: String, value: Boolean?) {
    value?.let {
        put(key, JsonPrimitive(it))
    }
}

private fun Map<String, JsonElement>.objectField(key: String): Map<String, JsonElement>? {
    return get(key)?.jsonObject?.let(::toOrderedJsonElementMap)
}

private fun MutableMap<String, JsonElement>.putObjectField(
    key: String,
    value: Map<String, JsonElement>?,
) {
    value?.takeIf(Map<String, JsonElement>::isNotEmpty)?.let {
        put(key, JsonObject(it))
    }
}
