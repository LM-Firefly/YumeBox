package com.github.yumelira.yumebox.presentation.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import com.github.yumelira.yumebox.common.util.WebViewUtils.getLocalBaseUrl
import com.github.yumelira.yumebox.common.util.WebViewUtils.getPanelUrl
import com.github.yumelira.yumebox.core.model.Proxy
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.domain.model.ProxyDisplayMode
import com.github.yumelira.yumebox.domain.model.ProxyGroupInfo
import com.github.yumelira.yumebox.domain.model.ProxySortMode
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.proxyGroupGridItems
import com.github.yumelira.yumebox.presentation.component.ProxyGroupTabs
import com.github.yumelira.yumebox.presentation.component.ProxyNodeCard
import com.github.yumelira.yumebox.presentation.component.ProxyNodeGrid
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`List-chevrons-up-down`
import com.github.yumelira.yumebox.presentation.icon.yume.`Squares-exclude`
import com.github.yumelira.yumebox.presentation.icon.yume.Rocket
import com.github.yumelira.yumebox.presentation.icon.yume.Speed
import com.github.yumelira.yumebox.presentation.icon.yume.Zap
import com.github.yumelira.yumebox.presentation.icon.yume.Zashboard
import com.github.yumelira.yumebox.presentation.viewmodel.FeatureViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.ProxyViewModel
import com.github.yumelira.yumebox.presentation.webview.WebViewActivity
import com.ramcosta.composedestinations.generated.destinations.ProvidersScreenDestination
import com.ramcosta.composedestinations.generated.destinations.WebViewScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ProxyPager(
    mainInnerPadding: PaddingValues, navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val proxyViewModel = koinViewModel<ProxyViewModel>()
    val featureViewModel = koinViewModel<FeatureViewModel>()
    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()
    val testingGroupNames by proxyViewModel.testingGroupNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()
    val selectedPanelType by featureViewModel.selectedPanelType.state.collectAsState()
    val globalTimeout by proxyViewModel.globalTimeout.collectAsState()
    val uiState by proxyViewModel.uiState.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    val showSettingsBottomSheet = rememberSaveable { mutableStateOf(false) }
    val showGroupBottomSheet = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            proxyViewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            proxyViewModel.clearError()
        }
    }
    var sheetGroupName by rememberSaveable { mutableStateOf<String?>(null) }
    val onTestDelay = remember { { proxyViewModel.testDelay() } }
    Scaffold(
        topBar = {
            ProxyTopBar(
                scrollBehavior = scrollBehavior,
                context = context,
                selectedPanelType = selectedPanelType,
                onNavigateToProviders = { navigator.navigate(ProvidersScreenDestination) { launchSingleTop = true } },
                onNavigateToPanel = { url, title -> navigator.navigate(WebViewScreenDestination(initialUrl = url, title = title)) { launchSingleTop = true } },
                onTestDelay = onTestDelay,
                onShowSettings = { showSettingsBottomSheet.value = true })
        }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (proxyGroups.isEmpty()) {
                CenteredText(
                    firstLine = MLang.Proxy.Empty.NoNodes, secondLine = MLang.Proxy.Empty.Hint
                )
            } else {
                ProxyContent(
                    proxyGroups = proxyGroups,
                    displayMode = displayMode,
                    scrollBehavior = scrollBehavior,
                    innerPadding = innerPadding,
                    mainInnerPadding = mainInnerPadding,
                    testingGroupNames = testingGroupNames,
                    onGroupClick = { group ->
                        sheetGroupName = group.name
                        showGroupBottomSheet.value = true
                    },
                    onGroupDelayClick = { group ->
                        proxyViewModel.testDelay(group.name)
                    },
                    globalTimeout = globalTimeout,
                    onSelectProxy = proxyViewModel::selectProxy,
                    onPinProxy = proxyViewModel::forceSelectProxy,
                    getResolvedDelay = { nodeName -> proxyViewModel.getResolvedDelay(nodeName) }
                )
            }
        }

        WindowBottomSheet(
            show = showSettingsBottomSheet,
            title = MLang.Proxy.Settings.Title,
            onDismissRequest = { showSettingsBottomSheet.value = false },
            insideMargin = DpSize(32.dp, 16.dp),
            backgroundColor = (MiuixTheme.colorScheme.surface.copy(alpha = 0.98f))
        ) {
            ProxySettingsContent(
                proxyViewModel = proxyViewModel, onDismiss = { showSettingsBottomSheet.value = false })
        }

        val proxyGroupsState = rememberUpdatedState(proxyGroups)
        val sheetGroup by remember {
            derivedStateOf {
                val name = sheetGroupName ?: return@derivedStateOf null
                proxyGroupsState.value.firstOrNull { it.name == name }
            }
        }

        WindowBottomSheet(
            show = showGroupBottomSheet,
            title = sheetGroupName.orEmpty(),
            backgroundColor = (MiuixTheme.colorScheme.surface.copy(alpha = 0.98f)),
            startAction = {
                val showPopup = remember { mutableStateOf(false) }
                val modes = remember {
                    listOf(
                        ProxySortMode.DEFAULT,
                        ProxySortMode.BY_NAME,
                        ProxySortMode.BY_LATENCY,
                    )
                }
                var selectedIndex by remember(sortMode) {
                    mutableStateOf(modes.indexOf(sortMode).coerceAtLeast(0))
                }

                IconButton(onClick = { showPopup.value = true }) {
                    Icon(Yume.`List-chevrons-up-down`, contentDescription = MLang.Proxy.Settings.SortMode)
                }

                WindowListPopup(
                    show = showPopup,
                    popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                    alignment = PopupPositionProvider.Align.Start,
                    onDismissRequest = { showPopup.value = false }) {
                    ListPopupColumn {
                        modes.forEachIndexed { index, mode ->
                            DropdownImpl(
                                text = mode.displayName,
                                optionSize = modes.size,
                                isSelected = selectedIndex == index,
                                onSelectedIndexChange = {
                                    selectedIndex = index
                                    proxyViewModel.setSortMode(mode)
                                    showPopup.value = false
                                },
                                index = index
                            )
                        }
                    }
                }
            },
            endAction = {
                val group = sheetGroup ?: return@WindowBottomSheet
                IconButton(onClick = { proxyViewModel.testDelay(group.name) }) {
                    Icon(Yume.Zap, contentDescription = MLang.Proxy.Action.Test)
                }
            },
            onDismissRequest = { showGroupBottomSheet.value = false },
            insideMargin = DpSize(16.dp, 16.dp),
        ) {
            val group = sheetGroup ?: return@WindowBottomSheet
            ProxyGroupSelectorContent(
                group = group,
                displayMode = displayMode,
                onSelectProxy = { proxyName ->
                    proxyViewModel.selectProxy(group.name, proxyName)
                    showGroupBottomSheet.value = false
                },
                isDelayTesting = testingGroupNames.contains(group.name),
                onTestDelay = { proxyViewModel.testDelay(group.name) },
                onPinProxy = proxyViewModel::forceSelectProxy,
                getResolvedDelay = { nodeName -> proxyViewModel.getResolvedDelay(nodeName) }
            )
        }
    }
}

@Composable
private fun ProxyTopBar(
    scrollBehavior: ScrollBehavior,
    context: Context,
    selectedPanelType: Int,
    onNavigateToProviders: () -> Unit,
    onNavigateToPanel: (String, String) -> Unit,
    onTestDelay: (() -> Unit)?,
    onShowSettings: () -> Unit
) {
    TopBar(title = MLang.Proxy.Title, scrollBehavior = scrollBehavior, navigationIcon = {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(
                modifier = Modifier.padding(start = 24.dp), onClick = onNavigateToProviders
            ) {
                Icon(Yume.`Squares-exclude`, contentDescription = MLang.Proxy.Action.ExternalResources)
            }
            IconButton(
                onClick = {
                    val panelUrl = getPanelUrl(context, selectedPanelType)
                    val webViewUrl = panelUrl.ifEmpty {
                        val localUrl = getLocalBaseUrl(context)
                        if (localUrl.isNotEmpty()) localUrl + "index.html" else ""
                    }
                    if (webViewUrl.isNotEmpty()) {
                        onNavigateToPanel(webViewUrl, MLang.Proxy.Action.Panel)
                    }
                }) {
                Icon(Yume.Zashboard, contentDescription = MLang.Proxy.Action.Panel)
            }
        }
    }, actions = {
        IconButton(
            modifier = Modifier.padding(end = 16.dp), onClick = {
                onTestDelay?.invoke()
            }) {
            Icon(Yume.Zap, contentDescription = MLang.Proxy.Action.Test)
        }
        IconButton(
            modifier = Modifier.padding(end = 24.dp), onClick = onShowSettings
        ) {
            Icon(Yume.`List-chevrons-up-down`, contentDescription = MLang.Proxy.Action.Settings)
        }
    })
}

@Composable
private fun ProxyContent(
    proxyGroups: List<ProxyGroupInfo>,
    displayMode: ProxyDisplayMode,
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    mainInnerPadding: PaddingValues,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupDelayClick: (ProxyGroupInfo) -> Unit,
    testingGroupNames: Set<String>,
    globalTimeout: Int,
    onSelectProxy: (String, String) -> Unit,
    onPinProxy: (String, String) -> Unit,
    getResolvedDelay: (String) -> Int? = { null }
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = innerPadding.calculateTopPadding() + 12.dp,
            bottom = mainInnerPadding.calculateBottomPadding(),
        ),
        overscrollEffect = null,
    ) {
        proxyGroupGridItems(
            groups = proxyGroups,
            displayMode = displayMode,
            onGroupClick = onGroupClick,
            onGroupDelayClick = onGroupDelayClick,
            testingGroupNames = testingGroupNames,
            getResolvedDelay = getResolvedDelay
        )
    }
}

@Composable
private fun ProxyGroupSelectorContent(
    group: ProxyGroupInfo,
    displayMode: ProxyDisplayMode,
    onSelectProxy: (String) -> Unit = {},
    onPinProxy: (String, String) -> Unit = { _, _ -> },
    globalTimeout: Int = 0,
    isDelayTesting: Boolean,
    onTestDelay: () -> Unit,
    scrollBehavior: ScrollBehavior = MiuixScrollBehavior(),
    mainInnerPadding: PaddingValues = PaddingValues(0.dp),
    getResolvedDelay: (String) -> Int? = { null }
) {
    val groupName = group.name
    val isSelectable = group.type == Proxy.Type.Selector
    val isSmartGroup = group.type == Proxy.Type.URLTest || group.type == Proxy.Type.Fallback
    val onProxyClick: (String) -> Unit = remember(groupName, isSelectable, onSelectProxy, isSmartGroup, group.fixed, onTestDelay) {
        if (isSelectable) {
            { proxyName: String -> onSelectProxy(proxyName) }
        } else if (isSmartGroup) {
            { proxyName: String ->
                if (proxyName == group.fixed) {
                    onPinProxy(groupName, "")
                } else {
                    onPinProxy(groupName, proxyName)
                }
            }
        } else {
            { _: String -> onTestDelay() }
        }
    }

    val shouldShowLoading = remember(group.proxies.size) {
        group.proxies.size > 12
    }

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(shouldShowLoading) {
        showContent = true
    }

    val contentPadding = remember {
        PaddingValues(top = 12.dp, bottom = 16.dp)
    }

    val configuration = LocalConfiguration.current
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp }
    val minSheetHeight = (screenHeight * 0.26f).coerceAtLeast(200.dp).coerceAtMost(300.dp)
    val maxSheetHeight = (screenHeight * 0.72f).coerceAtLeast(420.dp).coerceAtMost(620.dp)

    Box(
        modifier = Modifier.fillMaxWidth().let { base ->
            // IMPORTANT: 当需要显示 loading 时，固定到最终高度，避免 loading-> 内容切换时“往上顶一下”。
            // 在 Dialog(WindowBottomSheet/SuperBottomSheet) 场景下，高度跳变会非常明显。
            if (shouldShowLoading) {
                base.height(maxSheetHeight)
            } else {
                base.heightIn(min = minSheetHeight, max = maxSheetHeight)
            }
        }, contentAlignment = Alignment.Center
    ) {
        if (!showContent && shouldShowLoading) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                InfiniteProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (showContent) 1f else 0f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (group.chainPath.isNotEmpty()) {
                        ProxyChainIndicator(
                            chain = group.chainPath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    ProxyNodeGrid(
                        proxies = group.proxies,
                        selectedProxyName = group.now,
                        pinnedProxyName = group.fixed,
                        displayMode = displayMode,
                        onProxyClick = { proxy: Proxy -> onProxyClick(proxy.name) },
                        isDelayTesting = isDelayTesting,
                        onDelayTestClick = onTestDelay,
                        contentPadding = PaddingValues(top = if (group.chainPath.isNotEmpty()) 8.dp else 12.dp, bottom = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth(),
                        getResolvedDelay = getResolvedDelay,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProxySettingsContent(
    proxyViewModel: ProxyViewModel, onDismiss: () -> Unit
) {
    val currentMode by proxyViewModel.currentMode.collectAsState()
    val displayMode by proxyViewModel.displayMode.collectAsState()

    val modeTabs = remember { listOf(MLang.Proxy.Mode.Rule, MLang.Proxy.Mode.Global, MLang.Proxy.Mode.Direct) }
    val modeValues = remember { listOf(TunnelState.Mode.Rule, TunnelState.Mode.Global, TunnelState.Mode.Direct) }
    val displayModes = remember { listOf(ProxyDisplayMode.SINGLE_DETAILED, ProxyDisplayMode.DOUBLE_DETAILED) }
    val displayTabs = remember { displayModes.map { it.displayName } }
    val selectedDisplayMode = remember(displayMode) {
        when (displayMode) {
            ProxyDisplayMode.SINGLE_SIMPLE -> ProxyDisplayMode.SINGLE_DETAILED
            ProxyDisplayMode.DOUBLE_SIMPLE -> ProxyDisplayMode.DOUBLE_DETAILED
            else -> displayMode
        }
    }

    Column {
        Text(
            text = MLang.Proxy.Settings.ProxyMode,
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TabRowWithContour(
            tabs = modeTabs,
            selectedTabIndex = modeValues.indexOf(currentMode).coerceAtLeast(0),
            onTabSelected = { index ->
                if (index < modeValues.size) {
                    proxyViewModel.patchMode(modeValues[index])
                }
            })

        Spacer(Modifier.height(12.dp))

        Text(
            text = MLang.Proxy.Settings.DisplayMode,
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TabRowWithContour(
            tabs = displayTabs,
            selectedTabIndex = displayModes.indexOf(selectedDisplayMode).coerceAtLeast(0),
            onTabSelected = { index ->
                if (index < displayModes.size) {
                    proxyViewModel.setDisplayMode(displayModes[index])
                }
            })

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDismiss, modifier = Modifier.weight(1f)
            ) {
                Text(MLang.Component.Button.Cancel)
            }

            Button(
                onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(MLang.Component.Button.Confirm, color = MiuixTheme.colorScheme.background)
            }
        }
    }
}

fun LazyListScope.proxyNodeGridItems(
    groupName: String,
    proxies: List<Proxy>,
    selectedProxyName: String,
    pinnedProxyName: String,
    displayMode: ProxyDisplayMode,
    globalTimeout: Int,
    onProxyClick: ((String) -> Unit)?
) {
    val columns = if (displayMode.isSingleColumn) 1 else 2
    val showDetail = displayMode.showDetail
    if (columns == 1) {
        items(
            count = proxies.size,
            key = { index -> "${groupName}_${proxies.getOrNull(index)?.name ?: "null"}_$index" }
        ) { index ->
            val proxy = proxies[index]
            ProxyNodeCard(
                proxy = proxy,
                isSelected = proxy.name == selectedProxyName,
                onClick = onProxyClick?.let { { it(proxy.name) } },
                isPinned = proxy.name == pinnedProxyName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                isSingleColumn = true,
                globalTimeout = globalTimeout,
                showDetail = showDetail
            )
        }
    } else {
        val rowCount = (proxies.size + 1) / 2
        items(
            count = rowCount,
            key = { rowIndex ->
                val startIndex = rowIndex * 2
                val first = proxies.getOrNull(startIndex)?.name ?: ""
                val second = proxies.getOrNull(startIndex + 1)?.name ?: ""
                "${groupName}_${first}_${second}_$rowIndex"
            }
        ) { rowIndex ->
            val startIndex = rowIndex * 2
            val firstProxy = proxies.getOrNull(startIndex)
            val secondProxy = proxies.getOrNull(startIndex + 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    firstProxy?.let { firstProxy ->
                        ProxyNodeCard(
                            proxy = firstProxy,
                            isSelected = firstProxy.name == selectedProxyName,
                            onClick = onProxyClick?.let { { it(firstProxy.name) } },
                            isPinned = firstProxy.name == pinnedProxyName,
                            isSingleColumn = false,
                            globalTimeout = globalTimeout,
                            showDetail = showDetail
                        )
                    } ?: Spacer(modifier = Modifier.fillMaxWidth())
                }
                if (secondProxy != null) {
                    Box(modifier = Modifier.weight(1f)) {
                        secondProxy.let { secondProxy ->
                            ProxyNodeCard(
                                proxy = secondProxy,
                                isSelected = secondProxy.name == selectedProxyName,
                                onClick = onProxyClick?.let { { it(secondProxy.name) } },
                                isPinned = secondProxy.name == pinnedProxyName,
                                isSingleColumn = false,
                                globalTimeout = globalTimeout,
                                showDetail = showDetail
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProxyChainIndicator(
    chain: List<String>, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            chain.forEachIndexed { index, nodeName ->
                Text(
                    text = nodeName, style = MiuixTheme.textStyles.body2, color = if (index == chain.lastIndex) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    }
                )
                if (index < chain.lastIndex) {
                    Text(
                        text = "→",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
        }
    }
}
