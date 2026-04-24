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



package com.github.yumelira.yumebox.screen.navigation

import androidx.compose.runtime.Composable
import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import com.github.yumelira.yumebox.feature.editor.screen.ConfigPreviewScreen
import com.github.yumelira.yumebox.presentation.screen.*
import com.github.yumelira.yumebox.presentation.util.OverrideStructuredEditorStore
import com.github.yumelira.yumebox.presentation.viewmodel.OverrideConfigViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.OverrideConfigPreviewRouteDestination
import com.ramcosta.composedestinations.generated.destinations.OverrideEditRouteDestination
import com.ramcosta.composedestinations.generated.destinations.OverrideStringListEditorRouteDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@Composable
@Destination<RootGraph>
fun OverrideScreen(navigator: DestinationsNavigator) {
    val overrideConfigViewModel: OverrideConfigViewModel = koinInject()

    OverrideListScreen(navigator = navigator, onEditConfig = { configId ->
        navigator.navigate(
            OverrideEditRouteDestination(
                configId = configId,
            )
        )
    }, onOpenCodeEditor = { configId, configName ->
        val jsonContent = overrideConfigViewModel.getConfigJsonContent(configId) ?: "{}"
        OverrideStructuredEditorStore.setupConfigPreview(
            title = configName,
            content = jsonContent,
            language = LanguageScope.Json,
            callback = { content ->
                overrideConfigViewModel.saveConfigJsonContent(configId, content)
            },
        )
        navigator.navigate(OverrideConfigPreviewRouteDestination)
    })
}

@Composable
@Destination<RootGraph>
fun OverrideEditRoute(
    navigator: DestinationsNavigator,
    configId: String,
) {
    OverrideEditScreen(
        navigator = navigator,
        configId = configId,
        onOpenStringListEditor = { title, placeholder, replaceValue, startValue, endValue, onReplaceChange, onStartChange, onEndChange ->
            val values = com.github.yumelira.yumebox.presentation.util.OverrideListModeValues(
                replaceValue = replaceValue,
                startValue = startValue,
                endValue = endValue,
            )
            val availableModes = listOf(
                com.github.yumelira.yumebox.presentation.util.OverrideListEditorMode.Replace,
                com.github.yumelira.yumebox.presentation.util.OverrideListEditorMode.Start,
                com.github.yumelira.yumebox.presentation.util.OverrideListEditorMode.End,
            )
            OverrideStructuredEditorStore.setupStringListEditor(
                title = title,
                placeholder = placeholder,
                availableModes = availableModes,
                selectedMode = com.github.yumelira.yumebox.presentation.util.resolveInitialEditorMode(
                    availableModes = availableModes,
                    values = values,
                ),
                values = values,
            ) { updatedValues ->
                onReplaceChange(updatedValues.replaceValue)
                onStartChange(updatedValues.startValue)
                onEndChange(updatedValues.endValue)
            }
            navigator.navigate(OverrideStringListEditorRouteDestination)
        },
    )
}

@Composable
@Destination<RootGraph>
fun OverrideStringListEditorRoute(
    navigator: DestinationsNavigator,
) {
    OverrideStringListEditorScreen(
        navigator = navigator,
    )
}

@Composable
@Destination<RootGraph>
fun OverrideConfigPreviewRoute(
    navigator: DestinationsNavigator,
) {
    ConfigPreviewScreen(
        navigator = navigator,
        title = OverrideStructuredEditorStore.configPreviewTitle,
        initialContent = OverrideStructuredEditorStore.configPreviewContent,
        language = OverrideStructuredEditorStore.configPreviewLanguage,
        onSave = OverrideStructuredEditorStore.configPreviewCallback,
    )
}
