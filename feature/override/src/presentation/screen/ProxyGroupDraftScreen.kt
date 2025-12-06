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
import com.github.yumelira.yumebox.presentation.theme.UiDp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Save
import com.github.yumelira.yumebox.presentation.util.OverrideExtraFieldDraft
import com.github.yumelira.yumebox.presentation.util.OverrideProxyGroupDraft
import com.github.yumelira.yumebox.presentation.util.OverrideProxyGroupTypePresets
import com.github.yumelira.yumebox.presentation.util.OverrideStructuredEditorStore
import com.github.yumelira.yumebox.presentation.util.rememberCurrentReferenceCatalog
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

@Composable
fun OverrideProxyGroupDraftEditorScreen(
    navigator: DestinationsNavigator,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val title = remember {
        OverrideStructuredEditorStore.proxyGroupDraftEditorTitle.ifBlank { MLang.Override.Editor.ProxyGroup }
    }
    val initialValue = remember { OverrideStructuredEditorStore.proxyGroupDraftEditorValue }
    val saveFabController = rememberOverrideFabController()

    var name by remember { mutableStateOf(initialValue?.name.orEmpty()) }
    var type by remember { mutableStateOf(initialValue?.type?.ifBlank { "select" } ?: "select") }
    var proxies by remember { mutableStateOf(initialValue?.proxies.orEmpty()) }
    var useText by remember { mutableStateOf(initialValue?.use.orEmpty().joinToString("\n")) }
    var url by remember { mutableStateOf(initialValue?.url.orEmpty()) }
    var intervalText by remember { mutableStateOf(initialValue?.interval?.toString().orEmpty()) }
    var timeoutText by remember { mutableStateOf(initialValue?.timeout?.toString().orEmpty()) }
    var maxFailedTimesText by remember { mutableStateOf(initialValue?.maxFailedTimes?.toString().orEmpty()) }
    var interfaceName by remember { mutableStateOf(initialValue?.interfaceName.orEmpty()) }
    var routingMarkText by remember { mutableStateOf(initialValue?.routingMark?.toString().orEmpty()) }
    var filter by remember { mutableStateOf(initialValue?.filter.orEmpty()) }
    var excludeFilter by remember { mutableStateOf(initialValue?.excludeFilter.orEmpty()) }
    var excludeType by remember { mutableStateOf(initialValue?.excludeType.orEmpty()) }
    var expectedStatus by remember { mutableStateOf(initialValue?.expectedStatus.orEmpty()) }
    var icon by remember { mutableStateOf(initialValue?.icon.orEmpty()) }
    var lazy by remember { mutableStateOf(initialValue?.lazy) }
    var disableUdp by remember { mutableStateOf(initialValue?.disableUdp) }
    var includeAll by remember { mutableStateOf(initialValue?.includeAll) }
    var includeAllProxies by remember { mutableStateOf(initialValue?.includeAllProxies) }
    var includeAllProviders by remember { mutableStateOf(initialValue?.includeAllProviders) }
    var hidden by remember { mutableStateOf(initialValue?.hidden) }
    var extraFields by remember { mutableStateOf(initialValue?.extraFields.orEmpty()) }
    var editingExtraKey by remember { mutableStateOf<String?>(null) }
    var showExtraFieldDialog by remember { mutableStateOf(false) }
    var showProxySelector by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val selectedPresetIndex by remember(type) {
        derivedStateOf {
            OverrideProxyGroupTypePresets.indexOfFirst { it.equals(type, ignoreCase = true) }.coerceAtLeast(0)
        }
    }
    val referenceCatalog = rememberCurrentReferenceCatalog()
    val excludedGroupNames = remember(name, initialValue?.name) {
        buildSet {
            name.trim().takeIf(String::isNotBlank)?.let(::add)
            initialValue?.name?.trim()?.takeIf(String::isNotBlank)?.let(::add)
        }
    }
    val availableProxyGroupNames = remember(referenceCatalog.proxyGroupNames, excludedGroupNames) {
        referenceCatalog.proxyGroupNames.filterNot(excludedGroupNames::contains)
    }
    val proxySelectionGroups = remember(referenceCatalog.proxyNames, availableProxyGroupNames) {
        listOfNotNull(
            referenceCatalog.proxyNames.takeIf { it.isNotEmpty() }?.let { values ->
                OverrideSelectionGroup(
                    title = MLang.Override.Editor.ProxyNode,
                    items = values,
                )
            },
            availableProxyGroupNames.takeIf { it.isNotEmpty() }?.let { values ->
                OverrideSelectionGroup(
                    title = MLang.Override.Editor.ProxyGroup,
                    items = values,
                )
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            OverrideStructuredEditorStore.clearProxyGroupDraftEditor()
        }
    }

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = saveFabController,
                visible = true,
                imageVector = Yume.Save,
                contentDescription = MLang.Override.Editor.SaveProxyGroup,
                onClick = {
                    if (name.trim().isBlank()) {
                        errorText = MLang.Override.Draft.NameRequired
                        return@OverrideAnimatedFab
                    }
                    if (type.trim().isBlank()) {
                        errorText = MLang.Override.Editor.TypeEmpty
                        return@OverrideAnimatedFab
                    }
                    OverrideStructuredEditorStore.submitProxyGroupDraft(
                        OverrideProxyGroupDraft(
                            name = name.trim(),
                            type = type.trim(),
                            proxies = proxies,
                            use = parseMultilineValues(useText),
                            url = url.trim(),
                            interval = intervalText.trim().toIntOrNull(),
                            lazy = lazy,
                            timeout = timeoutText.trim().toIntOrNull(),
                            maxFailedTimes = maxFailedTimesText.trim().toIntOrNull(),
                            disableUdp = disableUdp,
                            interfaceName = interfaceName.trim(),
                            routingMark = routingMarkText.trim().toIntOrNull(),
                            includeAll = includeAll,
                            includeAllProxies = includeAllProxies,
                            includeAllProviders = includeAllProviders,
                            filter = filter.trim(),
                            excludeFilter = excludeFilter.trim(),
                            excludeType = excludeType.trim(),
                            expectedStatus = expectedStatus.trim(),
                            hidden = hidden,
                            icon = icon.trim(),
                            extraFields = extraFields,
                            uiId = initialValue?.uiId ?: OverrideProxyGroupDraft().uiId,
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
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
            lazyListState = listState,
            onScrollDirectionChanged = saveFabController::onScrollDirectionChanged,
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(OverrideSectionSpacing),
                ) {
                    OverrideSection(MLang.Override.Draft.BasicInfo) {
                        OverrideSelectorCard {
                            WindowDropdownPreference(
                                title = MLang.Override.Editor.RuleType,
                                items = OverrideProxyGroupTypePresets,
                                selectedIndex = selectedPresetIndex,
                                onSelectedIndexChange = { index ->
                                    if (index in OverrideProxyGroupTypePresets.indices) {
                                        type = OverrideProxyGroupTypePresets[index]
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
                                label = MLang.Override.Draft.Name,
                                errorText = errorText?.takeIf { it.contains(MLang.Override.Draft.Name) },
                            )
                            OverrideFormField(
                                value = url,
                                onValueChange = { url = it },
                                label = MLang.Override.ProxyGroup.Field.Url,
                            )
                            OverrideFormField(
                                value = intervalText,
                                onValueChange = { intervalText = it.filter(Char::isDigit) },
                                label = MLang.Override.ProxyGroup.Field.Interval,
                            )
                            OverrideFormField(
                                value = timeoutText,
                                onValueChange = { timeoutText = it.filter(Char::isDigit) },
                                label = MLang.Override.ProxyGroup.Field.Timeout,
                            )
                            OverrideFormField(
                                value = maxFailedTimesText,
                                onValueChange = { maxFailedTimesText = it.filter(Char::isDigit) },
                                label = MLang.Override.ProxyGroup.Field.MaxFailedTimes,
                            )
                        }
                    }
                    OverrideSection(MLang.Override.Editor.MemberSource) {
                        OverrideSelectorCard {
                            ArrowPreference(
                                title = MLang.Override.ProxyGroup.Field.Proxies,
                                onClick = {
                                    showProxySelector = true
                                    errorText = null
                                },
                                holdDownState = showProxySelector,
                            )
                        }
                        OverrideFormFieldColumn {
                            OverrideFormField(
                                value = useText,
                                onValueChange = { useText = it },
                                label = MLang.Override.ProxyGroup.Field.Use,
                                supportText = MLang.Override.ProxyGroup.Field.UseHint,
                                modifier = Modifier.heightIn(min = UiDp.dp100),
                                maxLines = 8,
                            )
                        }
                    }
                    OverridePlainFormSection(MLang.Override.Editor.HealthCheckAndFilter) {
                        OverrideFormField(
                            value = interfaceName,
                            onValueChange = { interfaceName = it },
                            label = MLang.Override.ProxyGroup.Field.InterfaceName,
                        )
                        OverrideFormField(
                            value = routingMarkText,
                            onValueChange = { routingMarkText = it.filter(Char::isDigit) },
                            label = MLang.Override.ProxyGroup.Field.RoutingMark,
                        )
                        OverrideFormField(
                            value = filter,
                            onValueChange = { filter = it },
                            label = MLang.Override.ProxyGroup.Field.Filter,
                        )
                        OverrideFormField(
                            value = excludeFilter,
                            onValueChange = { excludeFilter = it },
                            label = MLang.Override.ProxyGroup.Field.ExcludeFilter,
                        )
                        OverrideFormField(
                            value = excludeType,
                            onValueChange = { excludeType = it },
                            label = MLang.Override.ProxyGroup.Field.ExcludeType,
                        )
                        OverrideFormField(
                            value = expectedStatus,
                            onValueChange = { expectedStatus = it },
                            label = MLang.Override.ProxyGroup.Field.ExpectedStatus,
                        )
                        OverrideFormField(
                            value = icon,
                            onValueChange = { icon = it },
                            label = MLang.Override.ProxyGroup.Field.Icon,
                        )
                    }
                    OverrideCardSection(MLang.Override.Draft.BooleanOptions) {
                        NullableBooleanSelector(title = MLang.Override.ProxyGroup.Field.Lazy, value = lazy, onValueChange = { lazy = it })
                        NullableBooleanSelector(title = MLang.Override.ProxyGroup.Field.DisableUdp, value = disableUdp, onValueChange = { disableUdp = it })
                        NullableBooleanSelector(title = MLang.Override.ProxyGroup.Field.IncludeAll, value = includeAll, onValueChange = { includeAll = it })
                        NullableBooleanSelector(title = MLang.Override.ProxyGroup.Field.IncludeAllProxies, value = includeAllProxies, onValueChange = { includeAllProxies = it })
                        NullableBooleanSelector(title = MLang.Override.ProxyGroup.Field.IncludeAllProviders, value = includeAllProviders, onValueChange = { includeAllProviders = it })
                        NullableBooleanSelector(title = MLang.Override.ProxyGroup.Field.Hidden, value = hidden, onValueChange = { hidden = it })
                    }
                    OverrideSection(MLang.Override.Draft.ExtraFields) {
                        OverrideExtraFieldsCard(
                            title = MLang.Override.Draft.ExtraFields,
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
            title = if (editingExtraKey == null) MLang.Override.Draft.AddExtraField else MLang.Override.Draft.EditExtraField,
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
        OverrideMultiValueSelectionSheet(
            show = showProxySelector,
            title = MLang.Override.Editor.SelectProxyGroupMember,
            values = proxies,
            groups = proxySelectionGroups,
            customInputLabel = MLang.Override.Editor.CustomMember,
            onDismiss = { showProxySelector = false },
            onConfirm = { selectedValues ->
                proxies = selectedValues
                errorText = null
                showProxySelector = false
            },
        )
    }
}

private fun parseMultilineValues(rawValue: String): List<String> {
    return rawValue
        .lines()
        .map(String::trim)
        .filter(String::isNotBlank)
}
