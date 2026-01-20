package com.github.yumelira.yumebox.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.repeatOnLifecycle
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.NavigationBackIcon
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.presentation.viewmodel.LogViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.LogDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun LogScreen(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<LogViewModel>()
    val scrollBehavior = MiuixScrollBehavior()

    val isRecording by viewModel.isRecording.collectAsState()
    val logFiles by viewModel.logFiles.collectAsState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                delay(1000)
                viewModel.refreshLogFiles()
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Log.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    NavigationBackIcon(navigator = navigator)
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveAppLog() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Download,
                            contentDescription = "Save App Log",
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopRecording()
                            } else {
                                viewModel.startRecording()
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Yume.Square else Yume.Play,
                            contentDescription = if (isRecording) MLang.Log.Action.StopRecording else MLang.Log.Action.StartRecording,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.deleteAllLogs() },
                        modifier = Modifier.padding(end = 24.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = MLang.Log.Action.ClearLogs,
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        if (logFiles.isEmpty()) {
            CenteredText(
                firstLine = MLang.Log.Empty.NoLogs,
                secondLine = MLang.Log.Empty.Hint
            )
        } else {
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = innerPadding,
                topPadding = 20.dp,
            ) {
                item {
                    Card {
                        logFiles.forEachIndexed { index, fileInfo ->
                            LogFileItem(
                                fileInfo = fileInfo,
                                index = index,
                                onClick = {
                                    navigator.navigate(LogDetailScreenDestination(filePath = fileInfo.file.absolutePath))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFileItem(
    fileInfo: LogViewModel.LogFileInfo,
    index: Int = 0,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")

    val animatedSize by animateFloatAsState(
        targetValue = fileInfo.size.toFloat(),
        animationSpec = tween(300),
        label = "size_animation"
    )

    val sizeText = if (fileInfo.isRecording) formatFileSize(animatedSize.toLong()) else formatFileSize(fileInfo.size)
    val summary = "${dateFormat.format(Date(fileInfo.createdAt))}  ·  $sizeText"

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { -it / 2 }
        )
    ) {
        SuperArrow(
            title = fileInfo.name, summary = summary, onClick = onClick,
            endActions = {
                if (fileInfo.isRecording) {
                    Text(
                        MLang.Log.Status.Recording,
                        modifier = Modifier.padding(end = 16.dp),
                        style = MiuixTheme.textStyles.body2,
                    )
                } else null
            }
        )
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format(Locale.getDefault(), "%.2f MB", size / (1024.0 * 1024.0))
    }
}
