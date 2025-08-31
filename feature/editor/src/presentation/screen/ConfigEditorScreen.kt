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



package com.github.yumelira.yumebox.feature.editor.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.feature.editor.editor.CodeEditor
import com.github.yumelira.yumebox.feature.editor.editor.CodeEditorState
import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import com.github.yumelira.yumebox.feature.editor.language.TextMateInitializer
import com.github.yumelira.yumebox.feature.editor.theme.EditorThemeManager
import com.github.yumelira.yumebox.feature.editor.viewmodel.ConfigType
import com.github.yumelira.yumebox.feature.editor.viewmodel.ConfigEditorViewModel
import com.github.yumelira.yumebox.presentation.component.*
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun ConfigEditorScreen(
    navigator: DestinationsNavigator,
    configId: String,
    configType: ConfigType = ConfigType.Override,
    initialContent: String = "",
    language: LanguageScope = LanguageScope.Yaml,
) {
    val viewModel: ConfigEditorViewModel = koinViewModel()
    val context = LocalContext.current

    val editorState = remember(configId, initialContent) {
        CodeEditorState(
            initialContent = initialContent,
            language = language,
            readOnly = false
        )
    }

    val editorThemeState = EditorThemeManager.rememberEditorTheme()
    val showDiscardDialog = remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(configId) {
        TextMateInitializer.initialize(context)
        viewModel.loadConfig(configId, configType)
    }

    LaunchedEffect(editorThemeState.isDark) {
        editorState.editor?.let { editor ->
            TextMateInitializer.setTheme(editorThemeState.isDark)
        }
    }

    BackHandler {
        if (editorState.isModified) {
            showDiscardDialog.value = true
        } else {
            navigator.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = when (configType) {
                    ConfigType.Override -> "编辑覆写配置"
                    ConfigType.Profile -> "编辑订阅配置"
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            CodeEditor(
                state = editorState,
                modifier = Modifier.fillMaxSize(),
                onTextChange = { content ->
                    viewModel.updateDraft(content)
                }
            )
        }

        AppDialog(
            show = showDiscardDialog.value,
            title = "放弃修改",
            summary = "当前有未保存的修改，确定要放弃吗？",
            onDismissRequest = { showDiscardDialog.value = false }
        ) {
            DialogButtonRow(
                onCancel = { showDiscardDialog.value = false },
                onConfirm = {
                    showDiscardDialog.value = false
                    navigator.navigateUp()
                },
                cancelText = "取消",
                confirmText = "放弃"
            )
        }
    }
}
