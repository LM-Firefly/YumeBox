package com.github.yumelira.yumebox.feature.meta.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.officialMrsPresetIconUrl
import com.github.yumelira.yumebox.data.util.*
import com.github.yumelira.yumebox.feature.meta.presentation.viewmodel.CustomRoutingViewModel
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.RoutingSwitchCard
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CustomRoutingScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel: CustomRoutingViewModel = koinViewModel()
    val config by viewModel.config.collectAsState()

    val selectedUrlTestRegions = remember { mutableStateListOf<OverridePresetRegion>() }
    val selectedFallbackRegions = remember { mutableStateListOf<OverridePresetRegion>() }
    val enabledItems = remember { mutableStateListOf<OverridePresetItem>() }
    var enableUrlTestGroup by remember { mutableStateOf(true) }
    var enableFallbackGroup by remember { mutableStateOf(false) }
    var hasInitializedSelection by remember { mutableStateOf(false) }
    var isDirty by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(config) {
        if (hasInitializedSelection && isDirty) return@LaunchedEffect
        val initialSelection = inferPresetTemplateSelection(config)
        selectedUrlTestRegions.clear()
        selectedUrlTestRegions.addAll(sortPresetRegions(initialSelection.urlTestRegions))
        selectedFallbackRegions.clear()
        selectedFallbackRegions.addAll(sortPresetRegions(initialSelection.fallbackRegions))
        enabledItems.clear()
        enabledItems.addAll(sortPresetItems(initialSelection.enabledItems))
        enableUrlTestGroup = initialSelection.enableUrlTestGroup
        enableFallbackGroup = initialSelection.enableFallbackGroup
        hasInitializedSelection = true
        isDirty = false
    }

    val scrollBehavior = MiuixScrollBehavior()

    fun saveAndExit() {
        if (isSaving) return
        val newSelection = OverridePresetTemplateSelection(
            urlTestRegions = selectedUrlTestRegions.toSet(),
            fallbackRegions = selectedFallbackRegions.toSet(),
            enabledItems = enabledItems.toSet(),
            enableUrlTestGroup = enableUrlTestGroup,
            enableFallbackGroup = enableFallbackGroup,
        )
        val newConfig = applyPresetTemplateToConfig(
            base = ConfigurationOverride(),
            selection = newSelection,
        )
        scope.launch {
            isSaving = true
            val saved = viewModel.saveConfig(newConfig)
            isSaving = false
            if (!saved) return@launch
            isDirty = false
            onNavigateBack()
        }
    }

    BackHandler {
        saveAndExit()
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.MetaFeature.CustomRouting.Title,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(paddingValues, mainLikePadding),
        ) {
            item(key = "feature-explanation") {
                Card(modifier = Modifier.padding(top = 16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = MLang.MetaFeature.CustomRouting.Description,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                }
            }

            item(key = "preset-group-types") {
                RoutingSwitchCard(
                    title = MLang.MetaFeature.CustomRouting.GroupTypeTitle,
                    items = listOf("urltest", "fallback"),
                    iconUrl = { type ->
                        officialMrsPresetIconUrl(
                            if (type == "urltest") "Urltest" else "Available",
                        )
                    },
                    isChecked = { type ->
                        when (type) {
                            "urltest" -> enableUrlTestGroup
                            else -> enableFallbackGroup
                        }
                    },
                    onCheckedChange = { item, checked ->
                        when (item) {
                            "urltest" -> {
                                enableUrlTestGroup = checked
                                isDirty = true
                            }
                            "fallback" -> {
                                enableFallbackGroup = checked
                                isDirty = true
                            }
                        }
                    },
                    itemTitle = {
                        if (it == "urltest") {
                            MLang.MetaFeature.CustomRouting.GroupTypeUrlTest
                        } else {
                            MLang.MetaFeature.CustomRouting.GroupTypeFallback
                        }
                    },
                )
            }

            item(key = "preset-urltest-regions") {
                RoutingSwitchCard(
                    title = MLang.MetaFeature.CustomRouting.UrlTestRegionGroupTitle,
                    items = orderedPresetRegions(),
                    iconUrl = OverridePresetRegion::icon,
                    isChecked = { region -> region in selectedUrlTestRegions },
                    onCheckedChange = { region, checked ->
                        toggleSelection(selectedUrlTestRegions, region, checked)
                        isDirty = true
                    },
                    itemTitle = OverridePresetRegion::displayName,
                )
            }

            item(key = "preset-fallback-regions") {
                RoutingSwitchCard(
                    title = MLang.MetaFeature.CustomRouting.FallbackRegionGroupTitle,
                    items = orderedPresetRegions(),
                    iconUrl = OverridePresetRegion::icon,
                    isChecked = { region -> region in selectedFallbackRegions },
                    onCheckedChange = { region, checked ->
                        toggleSelection(selectedFallbackRegions, region, checked)
                        isDirty = true
                    },
                    itemTitle = OverridePresetRegion::displayName,
                )
            }

            item(key = "preset-base-items") {
                RoutingSwitchCard(
                    title = MLang.Override.Draft.BasicRouting,
                    items = orderedBasePresetItems(),
                    iconUrl = OverridePresetItem::icon,
                    isChecked = { item -> item in enabledItems },
                    onCheckedChange = { item, checked ->
                        toggleSelection(enabledItems, item, checked)
                        isDirty = true
                    },
                    itemTitle = OverridePresetItem::title,
                )
            }

            item(key = "preset-service-items") {
                RoutingSwitchCard(
                    title = MLang.Override.Draft.ServiceRouting,
                    items = orderedServicePresetItems(),
                    iconUrl = OverridePresetItem::icon,
                    isChecked = { item -> item in enabledItems },
                    onCheckedChange = { item, checked ->
                        toggleSelection(enabledItems, item, checked)
                        isDirty = true
                    },
                    itemTitle = OverridePresetItem::title,
                )
            }
        }
    }
}

private fun <T> toggleSelection(
    items: MutableList<T>,
    item: T,
    checked: Boolean,
) {
    if (checked) {
        if (item !in items) {
            items.add(item)
        }
    } else {
        items.remove(item)
    }
}
