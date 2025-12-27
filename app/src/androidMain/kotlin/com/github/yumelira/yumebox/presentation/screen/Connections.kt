package com.github.yumelira.yumebox.presentation.screen

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.github.yumelira.yumebox.domain.model.Connection
import com.github.yumelira.yumebox.presentation.component.InfoRow
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TabRowWithContour
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.horizontalPadding
import com.github.yumelira.yumebox.presentation.viewmodel.ConnectionSortType
import com.github.yumelira.yumebox.presentation.viewmodel.ConnectionsViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.ConnectionTab
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val TAG = "ConnectionsScreen"
private val iconCache = LruCache<String, Bitmap>(100)

@Destination<RootGraph>
@Composable
fun ConnectionsScreen(
    navigator: DestinationsNavigator,
    viewModel: ConnectionsViewModel = koinViewModel()
) {
    val activeConnections by viewModel.activeConnections.collectAsState()
    val closedConnections by viewModel.closedConnections.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val closedCount by viewModel.closedCount.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedConnection by remember { mutableStateOf<Connection?>(null) }
    val showDetails = remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { ConnectionTab.entries.size })
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.updateTab(ConnectionTab.entries[page])
        }
    }
    LaunchedEffect(currentTab) {
        if (pagerState.currentPage != currentTab.ordinal) {
            pagerState.animateScrollToPage(
                page = currentTab.ordinal,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
    }
    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Connections.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 24.dp)) {
                        IconButton(onClick = { navigator.popBackStack() }) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = MLang.Component.Navigation.Back,
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePause() }) {
                        Icon(
                            imageVector = if (isPaused) Yume.Play else Yume.Square,
                            contentDescription = if (isPaused) MLang.Connections.Action.Resume else MLang.Connections.Action.Pause,
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = MiuixIcons.Sort,
                                contentDescription = MLang.Connections.Sort.Desc,
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                        SuperListPopup(
                            show = remember { mutableStateOf(showSortMenu) }.apply { value = showSortMenu },
                            popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                            alignment = PopupPositionProvider.Align.End,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            ListPopupColumn {
                                ConnectionSortType.entries.forEach { type ->
                                    val isSelected = type == sortType
                                    val text = when (type) {
                                        ConnectionSortType.Host -> MLang.Connections.Sort.Host
                                        ConnectionSortType.Download -> MLang.Connections.Sort.Download
                                        ConnectionSortType.Upload -> MLang.Connections.Sort.Upload
                                        ConnectionSortType.DownloadSpeed -> MLang.Connections.Sort.DownloadSpeed
                                        ConnectionSortType.UploadSpeed -> MLang.Connections.Sort.UploadSpeed
                                        ConnectionSortType.Time -> MLang.Connections.Sort.Time
                                    }
                                    val displayText = if (isSelected) {
                                        "$text ${if (isAscending) "↑" else "↓"}"
                                    } else {
                                        text
                                    }
                                    val textColor = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.updateSortType(type)
                                                showSortMenu = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = displayText,
                                            style = MiuixTheme.textStyles.body1,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.closeAllConnections() },
                        modifier = Modifier.padding(end = 24.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = MLang.Connections.Action.CloseAll,
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Tabs
                val tabs = ConnectionTab.entries.map { tab ->
                    val tabName = when (tab) {
                        ConnectionTab.Active -> MLang.Connections.Tab.Active
                        ConnectionTab.Closed -> MLang.Connections.Tab.Closed
                    }
                    "$tabName (${if (tab == ConnectionTab.Active) activeCount else closedCount})"
                }
                TabRowWithContour(
                    tabs = tabs,
                    selectedTabIndex = currentTab.ordinal,
                    onTabSelected = { index -> viewModel.updateTab(ConnectionTab.entries[index]) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Search
                var expanded by remember { mutableStateOf(false) }
                SearchBar(
                    inputField = {
                        InputField(
                            query = searchQuery,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onSearch = { expanded = false },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            label = MLang.Connections.Search.Desc
                        )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageConnections = if (page == 0) activeConnections else closedConnections
                ScreenLazyColumn(
                    scrollBehavior = scrollBehavior,
                    innerPadding = PaddingValues(0.dp),
                ) {
                    items(pageConnections) { connection ->
                        ConnectionItem(
                            connection = connection,
                            onClose = { viewModel.closeConnection(connection.id) },
                            modifier = Modifier.padding(bottom = 8.dp),
                            onClick = {
                                selectedConnection = connection
                                showDetails.value = true
                            }
                        )
                    }
                }
            }
        }
        if (selectedConnection != null) {
            ConnectionDetailsDialog(
                connection = selectedConnection!!,
                show = showDetails,
                onDismiss = { showDetails.value = false }
            )
        }
    }
}

@Composable
fun ConnectionItem(connection: Connection, onClose: () -> Unit, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val processName = connection.metadata.process
    val icon by produceState<Bitmap?>(initialValue = iconCache.get(processName), key1 = processName) {
        if (value == null && processName.isNotEmpty()) {
            value = withContext(Dispatchers.IO) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(processName)
                    val bitmap = drawable.toBitmap()
                    iconCache.put(processName, bitmap)
                    bitmap
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    val failed = remember { mutableStateOf(false) }
    if (!failed.value) {
        val precompute = runCatching {
            data class RenderData(
                val hostText: String,
                val durationText: String,
                val protocolNetwork: String,
                val processText: String?,
                val ruleChain: String,
                val downloadSpeedText: String,
                val downloadText: String,
                val uploadSpeedText: String,
                val uploadText: String
            )
            RenderData(
                hostText = connection.metadata.host.ifEmpty { connection.metadata.destinationIP },
                durationText = calculateDuration(connection.start),
                protocolNetwork = "${connection.metadata.type} | ${connection.metadata.network.uppercase()}",
                processText = connection.metadata.process.ifEmpty { null },
                ruleChain = (listOf(connection.rule) + connection.chains.reversed()).joinToString(" -> "),
                downloadSpeedText = "${formatBytes(connection.downloadSpeed)}/s",
                downloadText = "↓ ${formatBytes(connection.download)}",
                uploadSpeedText = "${formatBytes(connection.uploadSpeed)}/s",
                uploadText = "↑ ${formatBytes(connection.upload)}"
            )
        }
        if (precompute.isFailure) {
            failed.value = true
            Timber.tag(TAG).e(precompute.exceptionOrNull(), "Failed to precompute render data for ConnectionItem id=%s", connection.id)
        } else {
            val d = precompute.getOrThrow()
            Card(modifier = modifier.horizontalPadding()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppTheme.radii.lg))
                        .clickable(onClick = onClick)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Image(
                                bitmap = icon!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            // Row 1: Host and Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = d.hostText,
                                    style = MiuixTheme.textStyles.title3,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    maxLines = 1
                                )
                                Text(
                                    text = d.durationText,
                                    style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Row 2: Protocol | Network
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = d.protocolNetwork,
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                if (d.processText != null) {
                                    Text(
                                        text = d.processText,
                                        style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Row 3: Rule -> Chain
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = d.ruleChain,
                                    style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Row 4: Stats and Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed and Traffic
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = MLang.Connections.Detail.Down,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFF4CAF50) // Green
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = d.downloadSpeedText,
                                    style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = d.downloadText,
                                    style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = MLang.Connections.Detail.Up,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFF2196F3) // Blue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = d.uploadSpeedText,
                                    style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = d.uploadText,
                                    style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        // Close Button
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Delete,
                                contentDescription = MLang.Connections.Action.Close,
                                tint = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    if (failed.value) {
        Card(modifier = modifier.horizontalPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Broken connection: ${connection.id}",
                    color = MiuixTheme.colorScheme.onSurface
                )
                Text(
                    text = connection.toString().take(200),
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

fun calculateDuration(startTimeStr: String): String {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val start = java.time.OffsetDateTime.parse(startTimeStr).toInstant().toEpochMilli()
            val now = System.currentTimeMillis()
            val diff = now - start
            val seconds = diff / 1000
            val minutes = seconds / 60
            if (minutes < 1) MLang.Connections.Time.JustNow else "$minutes ${MLang.Connections.Time.MinAgo}"
        } else {
            MLang.Connections.Tab.Active
        }
    } catch (e: Exception) {
        MLang.Connections.Tab.Active
    }
}

@Composable
fun ConnectionDetailsDialog(
    connection: Connection,
    show: MutableState<Boolean>,
    onDismiss: () -> Unit
) {
    val failed = remember(connection) { mutableStateOf(false) }
    if (!failed.value) {
        val precompute = runCatching {
            data class DialogData(
                val id: String,
                val startTime: String,
                val duration: String,
                val upload: String,
                val download: String,
                val uploadSpeed: String,
                val downloadSpeed: String,
                val network: String,
                val type: String,
                val host: String,
                val destIP: String,
                val destPort: String,
                val sourceIP: String,
                val sourcePort: String,
                val process: String?,
                val processPath: String?
            )
            DialogData(
                id = connection.id,
                startTime = formatTime(connection.start),
                duration = calculateDuration(connection.start),
                upload = formatBytes(connection.upload),
                download = formatBytes(connection.download),
                uploadSpeed = "${formatBytes(connection.uploadSpeed)}/s",
                downloadSpeed = "${formatBytes(connection.downloadSpeed)}/s",
                network = connection.metadata.network.uppercase(),
                type = connection.metadata.type,
                host = connection.metadata.host.ifEmpty { "N/A" },
                destIP = connection.metadata.destinationIP,
                destPort = connection.metadata.destinationPort,
                sourceIP = connection.metadata.sourceIP,
                sourcePort = connection.metadata.sourcePort,
                process = connection.metadata.process.ifEmpty { null },
                processPath = connection.metadata.processPath.ifEmpty { null }
            )
        }
        if (precompute.isFailure) {
            failed.value = true
            Timber.tag(TAG).e(precompute.exceptionOrNull(), "Failed to precompute dialog data for ConnectionDetailsDialog id=%s", connection.id)
        } else {
            val d = precompute.getOrThrow()
            SuperBottomSheet(
                show = show,
                title = MLang.Connections.Detail.Title,
                onDismissRequest = onDismiss,
            ) {
                    val configuration = LocalConfiguration.current
                    val screenHeight = configuration.screenHeightDp.dp
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = screenHeight * 0.52f)
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                        // General
                        Text(
                            text = MLang.Connections.Detail.BasicInfo,
                            style = MiuixTheme.textStyles.title4.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        InfoRow(label = MLang.Connections.Detail.ID, value = d.id, maxLines = Int.MAX_VALUE)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoRow(label = MLang.Connections.Detail.StartTime, value = d.startTime, modifier = Modifier.weight(2f))
                            InfoRow(label = MLang.Connections.Detail.Duration, value = d.duration, modifier = Modifier.weight(1f))
                        }
                        // Traffic
                        Text(
                            text = MLang.Connections.Detail.TrafficStats,
                            style = MiuixTheme.textStyles.title4.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoRow(label = MLang.Connections.Sort.Upload, value = d.upload, modifier = Modifier.weight(1f))
                            InfoRow(label = MLang.Connections.Sort.Download, value = d.download, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoRow(label = MLang.Connections.Sort.UploadSpeed, value = d.uploadSpeed, modifier = Modifier.weight(1f))
                            InfoRow(label = MLang.Connections.Sort.DownloadSpeed, value = d.downloadSpeed, modifier = Modifier.weight(1f))
                        }
                        // Metadata
                        Text(
                            text = MLang.Connections.Detail.Metadata,
                            style = MiuixTheme.textStyles.title4.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoRow(label = MLang.Connections.Detail.Network, value = d.network, modifier = Modifier.weight(1f))
                            InfoRow(label = MLang.Connections.Detail.Type, value = d.type, modifier = Modifier.weight(1f))
                            InfoRow(label = MLang.Connections.Detail.DNSMode, value = connection.metadata.dnsMode, modifier = Modifier.weight(1f))
                        }
                        InfoRow(label = MLang.Connections.Sort.Host, value = d.host, maxLines = Int.MAX_VALUE)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoRow(label = MLang.Connections.Detail.DestIP, value = d.destIP, modifier = Modifier.weight(1f), maxLines = Int.MAX_VALUE)
                            InfoRow(label = MLang.Connections.Detail.DestPort, value = d.destPort, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoRow(label = MLang.Connections.Detail.SourceIP, value = d.sourceIP, modifier = Modifier.weight(1f), maxLines = Int.MAX_VALUE)
                            InfoRow(label = MLang.Connections.Detail.SourcePort, value = d.sourcePort, modifier = Modifier.weight(1f))
                        }
                        if (d.process != null) {
                            InfoRow(label = MLang.Connections.Detail.Process, value = d.process, maxLines = Int.MAX_VALUE)
                        }
                        if (d.processPath != null) {
                            InfoRow(label = MLang.Connections.Detail.ProcessPath, value = d.processPath, maxLines = Int.MAX_VALUE)
                        }
                        // Rules
                        Text(
                            text = MLang.Connections.Detail.RulesAndChains,
                            style = MiuixTheme.textStyles.title4.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoRow(label = MLang.Connections.Detail.Rule, value = connection.rule, modifier = Modifier.weight(1f), maxLines = Int.MAX_VALUE)
                            InfoRow(label = MLang.Connections.Detail.RulePayload, value = connection.rulePayload, modifier = Modifier.weight(1f), maxLines = Int.MAX_VALUE)
                        }
                        InfoRow(label = MLang.Connections.Detail.Chain, value = connection.chains.reversed().joinToString(" -> "), maxLines = Int.MAX_VALUE)
                    }
                }
            }
        }
    }
}

fun formatTime(timeStr: String): String {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val parsed = java.time.OffsetDateTime.parse(timeStr)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            parsed.format(formatter)
        } else {
            timeStr
        }
    } catch (e: Exception) {
        timeStr
    }
}
