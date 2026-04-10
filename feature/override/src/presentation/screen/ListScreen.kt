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
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.data.model.OverrideConfig
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.presentation.theme.Spacing
import com.github.yumelira.yumebox.presentation.viewmodel.OverrideConfigViewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private val OverrideConfigItemGap = Spacing().space12

@Composable
fun OverrideListScreen(
    navigator: DestinationsNavigator,
    onEditConfig: (String) -> Unit,
    onOpenCodeEditor: (configId: String, configName: String) -> Unit = { _, _ -> },
) {
    val viewModel: OverrideConfigViewModel = koinViewModel()
    val userConfigs by viewModel.userConfigs.collectAsState()
    val usageCountMap by viewModel.usageCountMap.collectAsState()
    val pendingRevealConfigId by viewModel.pendingRevealConfigId.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = MiuixScrollBehavior()

    val showCreateDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val showEditOptionsDialog = remember { mutableStateOf<OverrideConfig?>(null) }
    val isEditOptionsDialogVisible = remember { mutableStateOf(false) }
    val deleteTargetConfig = remember { mutableStateOf<OverrideConfig?>(null) }
    val exportTargetConfig = remember { mutableStateOf<OverrideConfig?>(null) }

    val listState = rememberLazyListState()
    val createFabController = rememberOverrideFabController()
    val configItems = remember(userConfigs, usageCountMap) {
        userConfigs.map { config ->
            OverrideConfigListItem(
                config = config,
                isInUse = (usageCountMap[config.id] ?: 0) > 0,
            )
        }
    }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        viewModel.reorderUserConfigs(
            fromIndex = from.index,
            toIndex = to.index,
        )
    }

    val importConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult

        val displayName = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && columnIndex >= 0) {
                cursor.getString(columnIndex)
            } else {
                ""
            }
        }.orEmpty().ifBlank {
            uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringAfterLast('\\')
                .orEmpty()
        }

        runCatching {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { reader -> reader.readText() }
                ?: error(MLang.Override.Import.ReadError)
        }.onSuccess { jsonText ->
            val importResult = viewModel.importConfigsFromJson(
                jsonString = jsonText,
                sourceName = displayName,
            )
            if (importResult.isSuccess) {
                val importedCount = importResult.getOrNull() ?: 0
                val importMessage = if (displayName.isNotBlank()) {
                    MLang.Override.Import.Success.format(displayName, importedCount)
                } else {
                    MLang.Override.Import.SuccessDefault.format(importedCount)
                }
                context.toast(importMessage)
                showCreateDialog.value = false
            } else {
                context.toast(MLang.Override.Import.Failed.format(importResult.exceptionOrNull()?.message))
            }
        }.onFailure { throwable ->
            context.toast(MLang.Override.Import.FileError.format(throwable.message))
        }
    }

    val exportConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val targetConfig = exportTargetConfig.value
        if (uri == null || targetConfig == null) {
            exportTargetConfig.value = null
            return@rememberLauncherForActivityResult
        }

        val exportedConfig = viewModel.exportConfig(targetConfig.id)
        if (exportedConfig == null) {
            context.toast(MLang.Override.Export.Failed.format(targetConfig.name))
            exportTargetConfig.value = null
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(exportedConfig.toByteArray())
                outputStream.flush()
            } ?: error(MLang.Override.Export.Failed.format(targetConfig.name))
        }.onSuccess {
            context.toast(MLang.Override.Export.Success.format(targetConfig.name))
        }.onFailure { throwable ->
            context.toast(MLang.Override.Export.Failed.format(throwable.message))
        }

        exportTargetConfig.value = null
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(configItems, pendingRevealConfigId) {
        val targetId = pendingRevealConfigId ?: return@LaunchedEffect
        val targetIndex = configItems.indexOfFirst { it.config.id == targetId }
        if (targetIndex < 0) return@LaunchedEffect
        val anchorIndex = (targetIndex - 1).coerceAtLeast(0)
        listState.animateScrollToItem(anchorIndex)
        viewModel.consumePendingRevealConfig(targetId)
    }

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = createFabController,
                visible = !showCreateDialog.value,
                imageVector = Yume.`Badge-plus`,
                contentDescription = MLang.Override.Action.Create,
                onClick = { showCreateDialog.value = true },
            )
        },
        topBar = {
            TopBar(
                title = MLang.Override.Title,
                scrollBehavior = scrollBehavior,
                navigationIconPadding = 0.dp,
                navigationIcon = { NavigationBackIcon(navigator = navigator) },
            )
        },
    ) { paddingValues ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(paddingValues, mainLikePadding),
            lazyListState = listState,
            onScrollDirectionChanged = createFabController::onScrollDirectionChanged,
        ) {
            when {
                userConfigs.isEmpty() -> {
                    item(
                        key = "override-empty",
                        contentType = "override-empty",
                    ) {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(horizontal = UiDp.dp24, vertical = UiDp.dp80)
                                .wrapContentSize(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
                        ) {
                            CenteredText(
                                firstLine = MLang.Override.Empty.Title,
                                secondLine = MLang.Override.Empty.Hint,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
                            ) {
                                Button(
                                    onClick = { showCreateDialog.value = true },
                                ) {
                                    Text(MLang.Override.Action.New)
                                }
                                Button(
                                    onClick = { importConfigLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                ) {
                                    Text(
                                        text = MLang.Override.Action.Import,
                                        color = colorScheme.background,
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    items(
                        items = configItems,
                        key = { it.config.id },
                        contentType = { "override-config-card" },
                    ) { item ->
                        val config = item.config
                        ReorderableItem(
                            state = reorderState,
                            key = config.id,
                        ) { isDragging ->
                            OverrideConfigCard(
                                config = config,
                                isDragging = isDragging,
                                isInUse = item.isInUse,
                                onCopy = {
                                    viewModel.duplicateConfig(config.id)
                                    context.toast(MLang.Override.Card.Copy + "：" + config.name)
                                },
                                onExport = {
                                    exportTargetConfig.value = config
                                    exportConfigLauncher.launch("${config.name}.json")
                                },
                                onEdit = {
                                    showEditOptionsDialog.value = config
                                    isEditOptionsDialogVisible.value = true
                                },
                                onDelete = {
                                    deleteTargetConfig.value = config
                                    showDeleteDialog.value = true
                                },
                            )
                        }
                    }
                }
            }
        }
        CreateConfigDialog(
            show = showCreateDialog,
            onImportClick = { importConfigLauncher.launch("*/*") },
            onConfirm = { name, description ->
                viewModel.createConfig(
                    name = name,
                    description = description.takeIf(String::isNotBlank),
                )
                showCreateDialog.value = false
            },
            onDismiss = {
                showCreateDialog.value = false
            },
        )

        DeleteConfirmDialog(
            show = showDeleteDialog,
            config = deleteTargetConfig.value,
            viewModel = viewModel,
            onConfirm = {
                deleteTargetConfig.value?.id?.let(viewModel::deleteConfig)
                deleteTargetConfig.value = null
                showDeleteDialog.value = false
            },
            onDismiss = {
                deleteTargetConfig.value = null
                showDeleteDialog.value = false
            },
        )

        // 编辑选项对话框
        showEditOptionsDialog.value?.let { config ->
            EditOptionsDialog(
                show = isEditOptionsDialogVisible.value,
                onVisualEdit = {
                    isEditOptionsDialogVisible.value = false
                    onEditConfig(config.id)
                },
                onCodeEditor = {
                    isEditOptionsDialogVisible.value = false
                    onOpenCodeEditor(config.id, config.name)
                },
                onDismiss = { isEditOptionsDialogVisible.value = false },
                onDismissFinished = { showEditOptionsDialog.value = null },
            )
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.OverrideConfigCard(
    config: OverrideConfig,
    isDragging: Boolean,
    isInUse: Boolean,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colorScheme = colorScheme
    val accentTintColor = colorScheme.primary
    val descriptionText = config.description?.takeIf(String::isNotBlank) ?: MLang.Override.Card.NoDescription

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OverrideConfigItemGap / 2)
            .longPressDraggableHandle()
            .alpha(if (isDragging) 0.92f else 1f),
        insideMargin = PaddingValues(UiDp.dp16),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(UiDp.dp8),
                ) {
                    Text(
                        text = config.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight(550),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = descriptionText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OverrideConfigStateIndicator(inUse = isInUse)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = UiDp.dp12),
                thickness = UiDp.dp0_5,
                color = colorScheme.outline.copy(alpha = 0.5f),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(UiDp.dp8)) {
                    OverrideCardActionIconButton(
                        imageVector = Yume.Copy,
                        contentDescription = MLang.Override.Card.Copy,
                        onClick = onCopy,
                    )

                    OverrideCardActionIconButton(
                        imageVector = Yume.Share,
                        contentDescription = MLang.Override.Card.Export,
                        onClick = onExport,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    modifier = Modifier.padding(end = UiDp.dp8),
                    backgroundColor = colorScheme.primary.copy(alpha = 0.1f),
                    minHeight = UiDp.dp35,
                    minWidth = UiDp.dp35,
                    onClick = onEdit,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = UiDp.dp10),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiDp.dp2),
                    ) {
                        Icon(
                            modifier = Modifier.size(UiDp.dp20),
                            imageVector = Yume.Edit,
                            tint = accentTintColor,
                            contentDescription = MLang.Override.Card.Edit,
                        )
                        Text(
                            modifier = Modifier.padding(end = UiDp.dp3),
                            text = MLang.Override.Card.EditButton,
                            color = accentTintColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }

                IconButton(
                    backgroundColor = colorScheme.secondaryContainer.copy(alpha = 0.78f),
                    minHeight = UiDp.dp35,
                    minWidth = UiDp.dp35,
                    onClick = onDelete,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = UiDp.dp10),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier.size(UiDp.dp20),
                            imageVector = Yume.Delete,
                            tint = colorScheme.onSurface.copy(alpha = 0.85f),
                            contentDescription = MLang.Override.Card.Delete,
                        )
                        Text(
                            modifier = Modifier.padding(start = UiDp.dp4, end = UiDp.dp3),
                            text = MLang.Override.Card.DeleteButton,
                            color = colorScheme.onSurface.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverrideConfigStateIndicator(inUse: Boolean) {
    val tint = if (inUse) {
        colorScheme.primary
    } else {
        colorScheme.onSurfaceVariantSummary
    }

    OverrideStatusBadge(
        imageVector = if (inUse) Yume.ShieldCheck else Yume.ShieldMinus,
        contentDescription = if (inUse) MLang.Override.Status.InUse else MLang.Override.Status.NotInUse,
        tint = tint,
        backgroundColor = if (inUse) colorScheme.primary.copy(alpha = 0.1f) else colorScheme.secondaryContainer.copy(
            alpha = 0.78f
        ),
    )
}

@Composable
private fun CreateConfigDialog(
    show: MutableState<Boolean>,
    onImportClick: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var name by remember(show.value) { mutableStateOf("") }
    var description by remember(show.value) { mutableStateOf("") }
    val canConfirm = name.isNotBlank()

    AppActionBottomSheet(
        show = show.value,
        title = MLang.Override.Dialog.Create.Title,
        startAction = {
            AppBottomSheetCloseAction(onClick = onDismiss)
        },
        endAction = {
            AppBottomSheetConfirmAction(
                enabled = canConfirm,
                contentDescription = MLang.Override.Action.Create,
                onClick = {
                    if (canConfirm) {
                        keyboardController?.hide()
                        onConfirm(name, description)
                    }
                },
            )
        },
        onDismissRequest = onDismiss,
        insideMargin = DpSize(UiDp.dp32, UiDp.dp12),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = MLang.Override.Dialog.Create.Name
            )

            TextField(
                value = description,
                onValueChange = { description = it },
                label = MLang.Override.Dialog.Create.Description
            )

            Card(applyHorizontalPadding = false) {
                BasicComponent(
                    title = MLang.Override.Action.ImportFile,
                    summary = MLang.Override.Dialog.Create.ImportHint,
                    startAction = {
                        Icon(
                            modifier = Modifier.padding(end = UiDp.dp16),
                            imageVector = Yume.Share,
                            contentDescription = MLang.Override.Action.ImportFile,
                            tint = colorScheme.onBackground,
                        )
                    },
                    onClick = {
                        keyboardController?.hide()
                        onImportClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    show: MutableState<Boolean>,
    config: OverrideConfig?,
    viewModel: OverrideConfigViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isInUse by remember { mutableStateOf(false) }

    LaunchedEffect(show.value, config?.id) {
        isInUse = if (show.value && config != null) {
            viewModel.isConfigInUse(config.id)
        } else {
            false
        }
    }

    val summary = when {
        config == null -> ""
        isInUse -> MLang.Override.Dialog.Delete.InUseMessage.format(config.name)
        else -> MLang.Override.Dialog.Delete.Message.format(config.name)
    }

    AppDialog(
        show = show.value,
        title = MLang.Override.Dialog.Delete.Title,
        summary = summary,
        onDismissRequest = onDismiss,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onDismiss,
            ) {
                Text(MLang.Override.Dialog.Button.Cancel)
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = MLang.Override.Dialog.Button.Delete,
                    color = colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun EditOptionsDialog(
    show: Boolean,
    onVisualEdit: () -> Unit,
    onCodeEditor: () -> Unit,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    AppDialog(
        show = show,
        title = MLang.Override.Dialog.EditOptions.Title,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onVisualEdit,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = MLang.Override.Dialog.EditOptions.VisualEditor,
                    color = colorScheme.onPrimary,
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCodeEditor,
            ) {
                Text(MLang.Override.Dialog.EditOptions.CodeEditor)
            }
        }
    }
}

private data class OverrideConfigListItem(
    val config: OverrideConfig,
    val isInUse: Boolean,
)
