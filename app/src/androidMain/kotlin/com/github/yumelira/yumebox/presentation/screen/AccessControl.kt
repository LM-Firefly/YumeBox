package com.github.yumelira.yumebox.presentation.screen

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.NavigationBackIcon
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.SmallTitle
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Settings-2`
import com.github.yumelira.yumebox.presentation.viewmodel.AccessControlViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val iconCache = LruCache<String, Bitmap>(100)

@Composable
@Destination<RootGraph>
fun AccessControlScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AccessControlViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val showSettingsSheet = rememberSaveable { mutableStateOf(false) }
    val searchExpanded = rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.onPermissionResult()
            }
        }
    )

    LaunchedEffect(Unit) {
        snapshotFlow { uiState.needsMiuiPermission }
            .collect { needsPermission ->
                if (needsPermission) {
                    permissionLauncher.launch("com.android.permission.GET_INSTALLED_APPS")
                }
            }
    }

    BackHandler(enabled = searchExpanded.value) {
        searchExpanded.value = false
        viewModel.onSearchQueryChange("")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopBar(
                    title = "访问控制",
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { NavigationBackIcon(navigator = navigator) },
                    actions = {
                        IconButton(
                            modifier = Modifier.padding(end = 24.dp),
                            onClick = { showSettingsSheet.value = true }
                        ) {
                            Icon(Yume.`Settings-2`, contentDescription = "访问控制设置")
                        }
                    }
                )
            },
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("loading...", color = MiuixTheme.colorScheme.onSurface)
                }
            } else {
                ScreenLazyColumn(
                    scrollBehavior = scrollBehavior,
                    innerPadding = innerPadding,
                    topPadding = 20.dp
                ) {
                    item {
                        var searchText by remember { mutableStateOf("") }
                        var expanded by remember { mutableStateOf(false) }
                        SearchBar(
                            inputField = {
                                InputField(
                                    query = searchText,
                                    onQueryChange = {
                                        searchText = it
                                        viewModel.onSearchQueryChange(it)
                                    },
                                    onSearch = { expanded = false },
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it },
                                    label = "搜索应用..."
                                )
                            },
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                        }
                    }

                    item {
                        SmallTitle("应用列表 (${uiState.selectedPackages.size} 已选择)")
                    }

                    items(
                        items = uiState.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppCard(
                            app = app,
                            onSelectionChange = { checked ->
                                viewModel.onAppSelectionChange(app.packageName, checked)
                            },
                            onClick = {
                                viewModel.onAppSelectionChange(app.packageName, !app.isSelected)
                            }
                        )
                    }
                }
            }

            SuperDialog(
                show = showSettingsSheet,
                title = "访问控制设置",
                onDismissRequest = { showSettingsSheet.value = false }
            ) {
                val context = LocalContext.current
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                Column {
                    top.yukonga.miuix.kmp.basic.Card {
                        SuperSwitch(
                            title = "显示系统应用",
                            checked = uiState.showSystemApps,
                            onCheckedChange = { viewModel.onShowSystemAppsChange(it) }
                        )
                        SuperSwitch(
                            title = "倒序排列",
                            checked = uiState.descending,
                            onCheckedChange = { viewModel.onDescendingChange(it) }
                        )
                        SuperSwitch(
                            title = "已选应用优先",
                            checked = uiState.selectedFirst,
                            onCheckedChange = { viewModel.onSelectedFirstChange(it) }
                        )
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    top.yukonga.miuix.kmp.basic.Card {
                        SuperDropdown(
                            title = "排序方式",
                            summary = "当前：${uiState.sortMode.displayName}",
                            items = AccessControlViewModel.SortMode.entries.map { it.displayName },
                            selectedIndex = AccessControlViewModel.SortMode.entries
                                .indexOf(uiState.sortMode)
                                .coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                AccessControlViewModel.SortMode.entries.getOrNull(index)
                                    ?.let { viewModel.onSortModeChange(it) }
                            }
                        )
                        SuperDropdown(
                            title = "批量操作",
                            items = listOf("全选", "全不选", "反选"),
                            selectedIndex = 0,
                            onSelectedIndexChange = { index ->
                                when (index) {
                                    0 -> viewModel.selectAll()
                                    1 -> viewModel.deselectAll()
                                    2 -> viewModel.invertSelection()
                                }
                            }
                        )
                        SuperDropdown(
                            title = "导入/导出",
                            items = listOf("从剪贴板导入", "导出到剪贴板"),
                            selectedIndex = 0,
                            onSelectedIndexChange = { index ->
                                when (index) {
                                    0 -> {
                                        val clipData = clipboardManager?.primaryClip
                                        val text = if (clipData != null && clipData.itemCount > 0) {
                                            clipData.getItemAt(0)?.text?.toString() ?: ""
                                        } else {
                                            ""
                                        }
                                        if (text.isNotEmpty()) {
                                            val count = viewModel.importPackages(text)
                                            Toast.makeText(context, "成功导入 $count 个应用", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "导入失败", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    1 -> {
                                        val exportText = viewModel.exportPackages()
                                        val clip = ClipData.newPlainText("packages", exportText)
                                        clipboardManager?.setPrimaryClip(clip)
                                        Toast.makeText(
                                            context,
                                            "成功导出 ${uiState.selectedPackages.size} 个应用",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }


                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showSettingsSheet.value = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = { showSettingsSheet.value = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text("确定", color = MiuixTheme.colorScheme.background)
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = searchExpanded.value,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        ExpandedSearchOverlay(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
            filteredApps = uiState.filteredApps,
            onAppSelectionChange = { packageName, checked ->
                viewModel.onAppSelectionChange(packageName, checked)
            },
            onDismiss = {
                searchExpanded.value = false
                viewModel.onSearchQueryChange("")
            }
        )
    }
}

@Composable
private fun ExpandedSearchOverlay(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredApps: List<AccessControlViewModel.AppInfo>,
    onAppSelectionChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = "搜索应用...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MiuixTheme.colorScheme.surface)
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    val icon by produceState<Bitmap?>(initialValue = iconCache.get(app.packageName), key1 = app.packageName) {
                        if (value == null) {
                            value = withContext(Dispatchers.IO) {
                                try {
                                    val drawable = context.packageManager.getApplicationIcon(app.packageName)
                                    val bitmap = drawable.toBitmap()
                                    iconCache.put(app.packageName, bitmap)
                                    bitmap
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                    }
                    BasicComponent(
                        title = app.label,
                        summary = app.packageName,
                        startAction = {
                            icon?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        },
                        endActions = {
                            Checkbox(
                                checked = app.isSelected,
                                onCheckedChange = { checked ->
                                    onAppSelectionChange(app.packageName, checked)
                                }
                            )
                        },
                        onClick = {
                            onAppSelectionChange(app.packageName, !app.isSelected)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCard(
    app: AccessControlViewModel.AppInfo,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val icon by produceState<Bitmap?>(initialValue = iconCache.get(app.packageName), key1 = app.packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(app.packageName)
                    val bitmap = drawable.toBitmap()
                    iconCache.put(app.packageName, bitmap)
                    bitmap
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        BasicComponent(
            title = app.label,
            summary = app.packageName,
            startAction = {
                icon?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(45.dp)
                            .padding(end = 12.dp)
                    )
                }
            },
            endActions = {
                Checkbox(
                    checked = app.isSelected,
                    onCheckedChange = onSelectionChange
                )
            },
            onClick = onClick
        )
    }
}
