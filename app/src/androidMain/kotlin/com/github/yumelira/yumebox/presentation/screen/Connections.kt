package com.github.yumelira.yumebox.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.yumelira.yumebox.domain.model.Connection
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.InfoRow
import com.github.yumelira.yumebox.presentation.component.NavigationBackIcon
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.viewmodel.ConnectionSortType
import com.github.yumelira.yumebox.presentation.viewmodel.ConnectionTab
import com.github.yumelira.yumebox.presentation.viewmodel.ConnectionsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.icon.icons.useful.Cancel
import top.yukonga.miuix.kmp.icon.icons.useful.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme
import dev.oom_wg.purejoy.mlang.MLang
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Destination<RootGraph>
@Composable
fun ConnectionsScreen(
    navigator: DestinationsNavigator,
    viewModel: ConnectionsViewModel = koinViewModel()
) {
    val connections by viewModel.connections.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val closedCount by viewModel.closedCount.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedConnection by remember { mutableStateOf<Connection?>(null) }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Connections.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 24.dp)) {
                        IconButton(onClick = { navigator.popBackStack() }) {
                            Icon(
                                imageVector = MiuixIcons.Useful.Back,
                                contentDescription = MLang.Component.Navigation.Back,
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.togglePause() }) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) MLang.Connections.Action.Resume else MLang.Connections.Action.Pause,
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = MLang.Connections.Sort.Desc,
                                tint = MiuixTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = MiuixTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            ConnectionSortType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = when (type) {
                                                ConnectionSortType.Host -> MLang.Connections.Sort.Host
                                                ConnectionSortType.Download -> MLang.Connections.Sort.Download
                                                ConnectionSortType.Upload -> MLang.Connections.Sort.Upload
                                                ConnectionSortType.DownloadSpeed -> MLang.Connections.Sort.DownloadSpeed
                                                ConnectionSortType.UploadSpeed -> MLang.Connections.Sort.UploadSpeed
                                                ConnectionSortType.Time -> MLang.Connections.Sort.Time
                                            },
                                            color = MiuixTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.updateSortType(type)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.closeAllConnections() },
                        modifier = Modifier.padding(end = 24.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Useful.Delete,
                            contentDescription = MLang.Connections.Action.CloseAll,
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ConnectionTab.entries.forEach { tab ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (currentTab == tab) MiuixTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { viewModel.updateTab(tab) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val tabName = when (tab) {
                                    ConnectionTab.Active -> MLang.Connections.Tab.Active
                                    ConnectionTab.Closed -> MLang.Connections.Tab.Closed
                                }
                                Text(
                                    text = "$tabName (${if (tab == ConnectionTab.Active) activeCount else closedCount})",
                                    color = if (currentTab == tab) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Search
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = MLang.Connections.Search.Label,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = MLang.Connections.Search.Desc,
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    )
                }
            }
            items(connections) { connection ->
                ConnectionItem(
                    connection = connection,
                    onClose = { viewModel.closeConnection(connection.id) },
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = { selectedConnection = connection }
                )
            }
        }
        if (selectedConnection != null) {
            ConnectionDetailsDialog(
                connection = selectedConnection!!,
                onDismiss = { selectedConnection = null }
            )
        }
    }
}

@Composable
fun ConnectionItem(connection: Connection, onClose: () -> Unit, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Host and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = connection.metadata.host.ifEmpty { connection.metadata.destinationIP },
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 1
                )
                Text(
                    text = calculateDuration(connection.start),
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
                    text = "${connection.metadata.type} | ${connection.metadata.network.uppercase()}",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface
                )
                if (connection.metadata.process.isNotEmpty()) {
                    Text(
                        text = connection.metadata.process,
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
                    text = "${(listOf(connection.rule) + connection.chains.reversed()).joinToString(" -> ")}",
                    style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
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
                            text = "${formatBytes(connection.downloadSpeed)}/s",
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "↓ ${formatBytes(connection.download)}",
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
                            text = "${formatBytes(connection.uploadSpeed)}/s",
                            style = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp),
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "↑ ${formatBytes(connection.upload)}",
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
                        imageVector = MiuixIcons.Useful.Cancel,
                        contentDescription = MLang.Connections.Action.Close,
                        tint = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
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
    // Clash returns ISO 8601 like "2023-10-27T10:00:00.000Z"
    // We need to parse it. For simplicity in this snippet, we'll try standard parsing.
    // In a real app, use java.time or kotlinx-datetime
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
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        top.yukonga.miuix.kmp.basic.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MiuixTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = MLang.Connections.Detail.Title,
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = MiuixIcons.Useful.Cancel,
                            contentDescription = MLang.Component.Button.Cancel
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Content
                SelectionContainer {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
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
                        InfoRow(label = MLang.Connections.Detail.ID, value = connection.id, maxLines = Int.MAX_VALUE)
                        InfoRow(label = MLang.Connections.Detail.StartTime, value = formatTime(connection.start))
                        InfoRow(label = MLang.Connections.Detail.Duration, value = calculateDuration(connection.start))

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
                            InfoRow(label = MLang.Connections.Sort.Upload, value = formatBytes(connection.upload), modifier = Modifier.weight(1f))
                            InfoRow(label = MLang.Connections.Sort.Download, value = formatBytes(connection.download), modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoRow(label = MLang.Connections.Sort.UploadSpeed, value = "${formatBytes(connection.uploadSpeed)}/s", modifier = Modifier.weight(1f))
                            InfoRow(label = MLang.Connections.Sort.DownloadSpeed, value = "${formatBytes(connection.downloadSpeed)}/s", modifier = Modifier.weight(1f))
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
                            InfoRow(label = MLang.Connections.Detail.Network, value = connection.metadata.network.uppercase(), modifier = Modifier.weight(1f))
                            InfoRow(label = MLang.Connections.Detail.Type, value = connection.metadata.type, modifier = Modifier.weight(1f))
                        }
                        InfoRow(label = MLang.Connections.Sort.Host, value = connection.metadata.host.ifEmpty { "N/A" }, maxLines = Int.MAX_VALUE)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoRow(label = MLang.Connections.Detail.DestIP, value = connection.metadata.destinationIP, modifier = Modifier.weight(1f), maxLines = Int.MAX_VALUE)
                            InfoRow(label = MLang.Connections.Detail.DestPort, value = connection.metadata.destinationPort, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoRow(label = MLang.Connections.Detail.SourceIP, value = connection.metadata.sourceIP, modifier = Modifier.weight(1f), maxLines = Int.MAX_VALUE)
                            InfoRow(label = MLang.Connections.Detail.SourcePort, value = connection.metadata.sourcePort, modifier = Modifier.weight(1f))
                        }
                        if (connection.metadata.process.isNotEmpty()) {
                            InfoRow(label = MLang.Connections.Detail.Process, value = connection.metadata.process, maxLines = Int.MAX_VALUE)
                        }
                        if (connection.metadata.processPath.isNotEmpty()) {
                            InfoRow(label = MLang.Connections.Detail.ProcessPath, value = connection.metadata.processPath, maxLines = Int.MAX_VALUE)
                        }
                        InfoRow(label = MLang.Connections.Detail.DNSMode, value = connection.metadata.dnsMode)

                        // Rules
                        Text(
                            text = MLang.Connections.Detail.RulesAndChains,
                            style = MiuixTheme.textStyles.title4.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        InfoRow(label = MLang.Connections.Detail.Rule, value = connection.rule, maxLines = Int.MAX_VALUE)
                        InfoRow(label = MLang.Connections.Detail.RulePayload, value = connection.rulePayload, maxLines = Int.MAX_VALUE)
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
