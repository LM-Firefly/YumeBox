package com.github.yumelira.yumebox.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.repeatOnLifecycle
import com.github.yumelira.yumebox.core.model.LogMessage
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.NavigationBackIcon
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.Square
import com.github.yumelira.yumebox.presentation.viewmodel.LogViewModel
import com.github.yumelira.yumebox.service.LogRecordService
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun LogDetailScreen(
    navigator: DestinationsNavigator,
    filePath: String
) {
    val viewModel = koinViewModel<LogViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isRecording by viewModel.isRecording.collectAsState()
    val file = remember { File(filePath) }
    var logEntries by remember { mutableStateOf<List<LogViewModel.LogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val isCurrentFileRecording = isRecording && file.name == LogRecordService.currentLogFileName
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "保存文件失败")
                }
            }
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(filePath) {
        logEntries = viewModel.readLogContent(file).reversed()
        isLoading = false
    }

    LaunchedEffect(isCurrentFileRecording, lifecycleOwner) {
        if (isCurrentFileRecording) {
            lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                while (true) {
                    delay(1000)
                    logEntries = viewModel.readLogContent(file).reversed()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = if (isCurrentFileRecording) "实时日志" else file.name,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    NavigationBackIcon(navigator = navigator)
                },
                actions = {
                    if (isCurrentFileRecording) {
                        IconButton(
                            onClick = {
                                viewModel.stopRecording()
                            },
                            modifier = Modifier.padding(end = 24.dp)
                        ) {
                            Icon(
                                imageVector = Yume.Square,
                                contentDescription = "暂停记录",
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                saveFileLauncher.launch(file.name)
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Download,
                                contentDescription = "保存",
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.deleteLogFile(file)
                                navigator.navigateUp()
                            },
                            modifier = Modifier.padding(end = 24.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Delete,
                                contentDescription = "删除",
                            )
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        when {
            isLoading -> {
                CenteredText(
                    firstLine = "加载中...",
                    secondLine = ""
                )
            }

            logEntries.isEmpty() -> {
                CenteredText(
                    firstLine = if (isCurrentFileRecording) "等待日志..." else "日志为空",
                    secondLine = if (isCurrentFileRecording) "日志将在产生时显示" else "该文件没有日志内容"
                )
            }

            else -> {
                ScreenLazyColumn(
                    scrollBehavior = scrollBehavior,
                    innerPadding = innerPadding,
                    topPadding = 20.dp,
                ) {
                    logEntries.forEachIndexed { index, entry ->
                        item(key = "${logEntries.size}_$index") {
                            LogEntryCard(
                                entry = entry,
                                index = index,
                                isNewEntry = isCurrentFileRecording && index < 3
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(
    entry: LogViewModel.LogEntry,
    index: Int = 0,
    isNewEntry: Boolean = false
) {
    val levelColor = when (entry.level) {
        LogMessage.Level.Debug -> Color(0xFF9E9E9E)
        LogMessage.Level.Info -> MiuixTheme.colorScheme.primary
        LogMessage.Level.Warning -> Color(0xFFFF9800)
        LogMessage.Level.Error -> Color(0xFFF44336)
        LogMessage.Level.Silent -> Color(0xFF9E9E9E)
        LogMessage.Level.Unknown -> Color(0xFF9E9E9E)
    }

    var visible by remember { mutableStateOf(!isNewEntry) }
    LaunchedEffect(Unit) {
        if (isNewEntry) {
            delay(index * 50L)
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
            animationSpec = tween(200),
            initialOffsetY = { -it / 2 }
        )
    ) {
        Card(modifier = Modifier.padding(vertical = 4.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entry.time,
                        style = MiuixTheme.textStyles.body2.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Text(
                        text = entry.level.name.uppercase().take(1),
                        style = MiuixTheme.textStyles.body2.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = levelColor
                    )
                }
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = entry.message,
                    style = MiuixTheme.textStyles.body2.copy(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
