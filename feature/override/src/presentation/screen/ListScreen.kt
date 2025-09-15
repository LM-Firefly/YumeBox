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

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import com.github.yumelira.yumebox.domain.model.OverrideConfig
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.presentation.viewmodel.OverrideConfigViewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
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

private val OverrideConfigItemGap = 12.dp

@Composable
fun OverrideListScreen(
    navigator: DestinationsNavigator,
    onEditConfig: (String) -> Unit,
    onOpenCodeEditor: (configId: String, configName: String) -> Unit = { _, _ -> },
) {
    val viewModel: OverrideConfigViewModel = koinViewModel()
    val userConfigs by viewModel.userConfigs.collectAsState()
    val usageCountMap by viewModel.usageCountMap.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = MiuixScrollBehavior()

    val showCreateDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val showEditOptionsDialog = remember { mutableStateOf<OverrideConfig?>(null) }
    val deleteTargetConfig = remember { mutableStateOf<OverrideConfig?>(null) }
    val exportTargetConfig = remember { mutableStateOf<OverrideConfig?>(null) }
    val listState = rememberLazyListState()
    val createFabController = rememberOverrideFabController()
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
                ?: error("无法读取导入文件")
        }.onSuccess { jsonText ->
            val importResult = viewModel.importConfigsFromJson(
                jsonString = jsonText,
                sourceName = displayName,
            )
            if (importResult.isSuccess) {
                val importedCount = importResult.getOrNull() ?: 0
                val importMessage = if (displayName.isNotBlank()) {
                    "已从 $displayName 导入 $importedCount 个配置"
                } else {
                    "已导入 $importedCount 个配置"
                }
                context.toast(importMessage)
                showCreateDialog.value = false
            } else {
                context.toast("导入失败: ${importResult.exceptionOrNull()?.message}")
            }
        }.onFailure { throwable ->
            context.toast("读取文件失败: ${throwable.message}")
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
            context.toast("导出失败：${targetConfig.name}")
            exportTargetConfig.value = null
            return@rememberLauncherForActivityResult
        }

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(exportedConfig.toByteArray())
                outputStream.flush()
            } ?: error("无法写入导出文件")
        }.onSuccess {
            context.toast("已导出配置：${targetConfig.name}")
        }.onFailure { throwable ->
            context.toast("导出失败: ${throwable.message}")
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

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = createFabController,
                visible = !showCreateDialog.value,
                imageVector = Yume.`Badge-plus`,
                contentDescription = "创建配置",
                onClick = { showCreateDialog.value = true },
            )
        },
        topBar = {
            TopBar(
                title = "覆写配置",
                scrollBehavior = scrollBehavior
            )
        },
    ) { paddingValues ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = paddingValues,
            topPadding = 20.dp,
            lazyListState = listState,
            onScrollDirectionChanged = createFabController::onScrollDirectionChanged,
        ) {
            when {
                userConfigs.isEmpty() -> {
                    item(key = "override-empty") {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(horizontal = 24.dp, vertical = 80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                CenteredText(
                                    firstLine = "暂无覆写配置",
                                    secondLine = "点击下方按钮创建新配置，或导入 JSON",
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Button(
                                        onClick = { showCreateDialog.value = true },
                                    ) {
                                        Text("新建配置")
                                    }
                                    Button(
                                        onClick = { importConfigLauncher.launch("*/*") },
                                        colors = ButtonDefaults.buttonColorsPrimary(),
                                    ) {
                                        Text(
                                            text = "导入 JSON",
                                            color = colorScheme.background,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    items(
                        count = userConfigs.size,
                        key = { index -> userConfigs[index].id },
                    ) { index ->
                        val config = userConfigs[index]
                        val isInUse = (usageCountMap[config.id] ?: 0) > 0
                        ReorderableItem(
                            state = reorderState,
                            key = config.id,
                        ) { isDragging ->
                            OverrideConfigCard(
                                config = config,
                                isDragging = isDragging,
                                isInUse = isInUse,
                                onCopy = {
                                    viewModel.duplicateConfig(config.id)
                                    context.toast("已复制配置：${config.name}")
                                },
                                onExport = {
                                    exportTargetConfig.value = config
                                    exportConfigLauncher.launch("${config.name}.json")
                                },
                                onEdit = { showEditOptionsDialog.value = config },
                                onDelete = {
                                    deleteTargetConfig.value = config
                                    showDeleteDialog.value = true
                                },
                            )
                        }
                    }
                }
            }

            item(key = "override-list-bottom-spacer") {
                Spacer(modifier = Modifier.height(32.dp))
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
                show = showEditOptionsDialog,
                onVisualEdit = {
                    showEditOptionsDialog.value = null
                    onEditConfig(config.id)
                },
                onCodeEditor = {
                    showEditOptionsDialog.value = null
                    onOpenCodeEditor(config.id, config.name)
                },
                onDismiss = { showEditOptionsDialog.value = null },
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
    val colorScheme = MiuixTheme.colorScheme
    val accentTintColor = colorScheme.primary
    val descriptionText = config.description?.takeIf(String::isNotBlank) ?: "未填写描述"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OverrideConfigItemGap / 2)
            .longPressDraggableHandle()
            .alpha(if (isDragging) 0.92f else 1f),
        insideMargin = PaddingValues(16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = colorScheme.outline.copy(alpha = 0.5f),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverrideCardActionIconButton(
                        imageVector = Yume.Copy,
                        contentDescription = "复制配置",
                        onClick = onCopy,
                    )

                    OverrideCardActionIconButton(
                        imageVector = Yume.Share,
                        contentDescription = "导出配置",
                        onClick = onExport,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    modifier = Modifier.padding(end = 8.dp),
                    backgroundColor = colorScheme.primary.copy(alpha = 0.1f),
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    onClick = onEdit,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Yume.Edit,
                            tint = accentTintColor,
                            contentDescription = "编辑配置",
                        )
                        Text(
                            modifier = Modifier.padding(end = 3.dp),
                            text = "编辑",
                            color = accentTintColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }

                IconButton(
                    backgroundColor = colorScheme.secondaryContainer.copy(alpha = 0.78f),
                    minHeight = 35.dp,
                    minWidth = 35.dp,
                    onClick = onDelete,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Yume.Delete,
                            tint = colorScheme.onSurface.copy(alpha = 0.85f),
                            contentDescription = "删除配置",
                        )
                        Text(
                            modifier = Modifier.padding(start = 4.dp, end = 3.dp),
                            text = "删除",
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
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }

    OverrideStatusBadge(
        imageVector = if (inUse) Yume.ShieldCheck else Yume.ShieldMinus,
        contentDescription = if (inUse) "使用中" else "未使用",
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
        title = "添加配置",
        startAction = {
            AppBottomSheetCloseAction(onClick = onDismiss)
        },
        endAction = {
            AppBottomSheetConfirmAction(
                enabled = canConfirm,
                contentDescription = "创建",
                onClick = {
                    if (canConfirm) {
                        keyboardController?.hide()
                        onConfirm(name, description)
                    }
                },
            )
        },
        onDismissRequest = onDismiss,
        insideMargin = DpSize(32.dp, 12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = "配置名称"
            )

            TextField(
                value = description,
                onValueChange = { description = it },
                label = "配置描述"
            )

            Card(applyHorizontalPadding = false) {
                BasicComponent(
                    title = "导入配置文件",
                    summary = "选择 JSON 文件导入覆写配置",
                    startAction = {
                        Icon(
                            modifier = Modifier.padding(end = 16.dp),
                            imageVector = Yume.Share,
                            contentDescription = "导入配置文件",
                            tint = MiuixTheme.colorScheme.onBackground,
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
        isInUse -> "配置 ${config.name} 正在被订阅使用，删除后将解除绑定关系。确定要删除吗？此操作不可恢复。"
        else -> "确定要删除配置 ${config.name} 吗？此操作不可恢复。"
    }

    AppDialog(
        show = show.value,
        title = "删除配置",
        summary = summary,
        onDismissRequest = onDismiss,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onDismiss,
            ) {
                Text("取消")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = "删除",
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun EditOptionsDialog(
    show: MutableState<OverrideConfig?>,
    onVisualEdit: () -> Unit,
    onCodeEditor: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        show = show.value != null,
        title = "编辑配置",
        onDismissRequest = onDismiss,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCodeEditor,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = "代码编辑器",
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onVisualEdit,
            ) {
                Text("可视化编辑")
            }

        }
    }
}
