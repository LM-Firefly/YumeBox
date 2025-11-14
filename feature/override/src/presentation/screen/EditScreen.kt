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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.util.OverrideEditorSection
import com.github.yumelira.yumebox.presentation.util.OverrideSaveEvent
import com.github.yumelira.yumebox.presentation.util.OverrideSaveState
import com.github.yumelira.yumebox.presentation.viewmodel.OverrideConfigViewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun OverrideEditScreen(
    navigator: DestinationsNavigator,
    configId: String,
    onOpenStringListEditor: OpenStringListModifiersEditor,
    onOpenRuleListEditor: OpenRuleListEditor,
    onOpenObjectListEditor: OpenStructuredObjectListEditor,
    onOpenObjectMapEditor: OpenObjectMapEditor,
    onOpenSubRulesEditor: OpenSubRulesEditor,
) {
    val viewModel: OverrideConfigViewModel = koinViewModel()
    val editSession by viewModel.editSession.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val context = LocalContext.current

    val isNewConfig = configId == "new"
    val showDiscardDialog = remember { mutableStateOf(false) }
    val showStringMapEditor = remember { mutableStateOf(false) }
    val showJsonEditor = remember { mutableStateOf(false) }
    var currentMapEditorCallback by remember { mutableStateOf<(Map<String, String>?) -> Unit>({}) }
    var currentJsonEditorCallback by remember { mutableStateOf<(String?) -> Unit>({}) }

    var currentMapEditorTitle by remember { mutableStateOf("") }
    var currentMapEditorKeyPlaceholder by remember { mutableStateOf("") }
    var currentMapEditorValuePlaceholder by remember { mutableStateOf("") }
    var currentMapEditorValue by remember { mutableStateOf<Map<String, String>?>(null) }

    var currentJsonEditorTitle by remember { mutableStateOf("") }
    var currentJsonEditorPlaceholder by remember { mutableStateOf("") }
    var currentJsonEditorValue by remember { mutableStateOf<String?>(null) }

    val scrollBehavior = MiuixScrollBehavior()
    val editorListState = rememberLazyListState()
    var expandedSectionNames by rememberSaveable { mutableStateOf(setOf<String>()) }
    val expandedSections = remember(expandedSectionNames) {
        expandedSectionNames.mapNotNull { sectionName ->
            OverrideEditorSection.entries.firstOrNull { it.name == sectionName }
        }.toSet()
    }


    LaunchedEffect(configId) {
        viewModel.startEditSession(configId)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is OverrideSaveEvent.Saved -> Unit
                is OverrideSaveEvent.Failed -> {
                    Timber.tag("OverrideEditScreen").d("Suppress override save toast: %s", event.message)
                }
            }
        }
    }

    val isSaving = saveState == OverrideSaveState.Saving
    val hasUnsavedInvalidChanges = editSession?.hasUnsavedInvalidChanges == true

    fun requestExit() {
        when {
            showStringMapEditor.value -> showStringMapEditor.value = false
            showJsonEditor.value -> showJsonEditor.value = false
            isSaving -> Unit
            hasUnsavedInvalidChanges -> showDiscardDialog.value = true
            else -> {
                viewModel.flushDraftSave {
                    viewModel.clearEditSession()
                    navigator.navigateUp()
                }
            }
        }
    }

    BackHandler {
        requestExit()
    }

    Scaffold(
        topBar = {
            TopBar(
                title = if (isNewConfig) MLang.Override.Edit.TitleNew else MLang.Override.Edit.TitleEdit,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(paddingValues, mainLikePadding),
            lazyListState = editorListState,
        ) {
            editSession?.let { session ->
                OverrideEditContent(
                    name = session.name,
                    description = session.description,
                    config = session.config,
                    currentConfigProvider = {
                        viewModel.editSession.value?.config ?: session.config
                    },
                    expandedSections = expandedSections,
                    onNameChange = viewModel::updateDraftName,
                    onDescriptionChange = viewModel::updateDraftDescription,
                    onConfigChange = { updatedConfig ->
                        viewModel.updateDraftConfig(
                            updatedConfig = updatedConfig,
                            saveImmediately = true,
                        )
                    },
                    onSectionToggle = { section ->
                        expandedSectionNames = if (section.name in expandedSectionNames) {
                            expandedSectionNames - section.name
                        } else {
                            expandedSectionNames + section.name
                        }
                    },
                    onEditStringList = onOpenStringListEditor,
                    onEditRuleList = { title, values, availableModes, selectedMode, referenceCatalog, callback ->
                        onOpenRuleListEditor(
                            title,
                            values,
                            availableModes,
                            selectedMode,
                            referenceCatalog,
                            callback,
                        )
                    },
                    onEditStringMap = { title, keyPlaceholder, valuePlaceholder, value, callback ->
                        currentMapEditorTitle = title
                        currentMapEditorKeyPlaceholder = keyPlaceholder
                        currentMapEditorValuePlaceholder = valuePlaceholder
                        currentMapEditorValue = value
                        currentMapEditorCallback = callback
                        showStringMapEditor.value = true
                    },
                    onEditJson = { title, placeholder, value, callback ->
                        currentJsonEditorTitle = title
                        currentJsonEditorPlaceholder = placeholder
                        currentJsonEditorValue = value
                        currentJsonEditorCallback = callback
                        showJsonEditor.value = true
                    },
                    onEditObjectList = { type, title, values, availableModes, selectedMode, referenceCatalog, callback ->
                        onOpenObjectListEditor(
                            type,
                            title,
                            values,
                            availableModes,
                            selectedMode,
                            referenceCatalog,
                            callback,
                        )
                    },
                    onEditObjectMap = { type, title, values, availableModes, selectedMode, callback ->
                        onOpenObjectMapEditor(
                            type,
                            title,
                            values,
                            availableModes,
                            selectedMode,
                            callback,
                        )
                    },
                    onEditSubRules = { title, values, availableModes, selectedMode, referenceCatalog, callback ->
                        onOpenSubRulesEditor(
                            title,
                            values,
                            availableModes,
                            selectedMode,
                            referenceCatalog,
                            callback,
                        )
                    },
                )
            }
        }
    AppDialog(
            show = showDiscardDialog.value,
            title = MLang.Override.Edit.EmptyName.Title,
            summary = MLang.Override.Edit.EmptyName.Summary,
            onDismissRequest = { showDiscardDialog.value = false },
        ) {
            DialogButtonRow(
                onCancel = { showDiscardDialog.value = false },
                onConfirm = {
                    showDiscardDialog.value = false
                    viewModel.clearEditSession()
                    navigator.navigateUp()
                },
                cancelText = MLang.Override.Edit.Button.Cancel,
                confirmText = MLang.Override.Edit.Button.Discard,
            )
        }

        StringMapEditorDialog(
            show = showStringMapEditor.value,
            title = currentMapEditorTitle,
            keyPlaceholder = currentMapEditorKeyPlaceholder,
            valuePlaceholder = currentMapEditorValuePlaceholder,
            value = currentMapEditorValue,
            onValueChange = currentMapEditorCallback,
            onDismiss = { showStringMapEditor.value = false },
        )

        JsonTextEditorDialog(
            show = showJsonEditor.value,
            title = currentJsonEditorTitle,
            placeholder = currentJsonEditorPlaceholder,
            value = currentJsonEditorValue,
            onValueChange = currentJsonEditorCallback,
            onDismiss = { showJsonEditor.value = false },
        )

    }
}
