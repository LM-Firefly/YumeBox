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

package com.github.yumelira.yumebox.screen.profiles

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.extra.WindowSpinner
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun AddProfileSheet(
    show: MutableState<Boolean>,
    profileToEdit: Profile? = null,
    importUrl: String? = null,
    onAddProfile: (name: String, source: String, type: Profile.Type, interval: Long, fileUri: android.net.Uri?) -> Unit,
    onUpdateProfile: (uuid: UUID, name: String, source: String, interval: Long) -> Unit,
    onDownloadComplete: () -> Unit,
    profilesViewModel: ProfilesViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedTypeIndex by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var filePath by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadStartTime by remember { mutableLongStateOf(0L) }

    val downloadProgress by profilesViewModel.downloadProgress.collectAsState()
    val uiState by profilesViewModel.uiState.collectAsState()
    var displayedProgress by remember { mutableIntStateOf(0) }
    var lastProgress by remember { mutableIntStateOf(0) }
    var hasShownCompleteAnimation by remember { mutableStateOf(false) }

    val urlPattern = remember {
        Regex(
            pattern = "^https?://\\S+$", options = setOf(RegexOption.IGNORE_CASE)
        )
    }

    val readClipboardAndCheckUrl: () -> String? = {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                val clipText = item?.text?.toString()?.trim() ?: ""
                if (clipText.isNotEmpty() && urlPattern.matches(clipText)) {
                    clipText
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    val clearAllState = {
        name = ""
        url = ""
        filePath = ""
        fileName = ""
        error = ""
        isDownloading = false
        displayedProgress = 0
        lastProgress = 0
        hasShownCompleteAnimation = false
    }


    val clearCurrentTypeState = {
        when (selectedTypeIndex) {
            0 -> url = ""
            1 -> {
                filePath = ""
                fileName = ""
            }

            2 -> {
            }
        }
        error = ""
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, MLang.ProfilesPage.QrScanner.NeedCamera, Toast.LENGTH_LONG)
                .show()
            selectedTypeIndex = 0
        }
    }

    LaunchedEffect(selectedTypeIndex) {
        if (selectedTypeIndex == 2 && !hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }


    DisposableEffect(show.value, profileToEdit, importUrl) {
        if (show.value) {
            clearAllState()
            if (profileToEdit != null) {
                name = profileToEdit.name
                if (profileToEdit.type == Profile.Type.Url) {
                    selectedTypeIndex = 0
                    url = profileToEdit.source
                } else {
                    selectedTypeIndex = 1
                    filePath = profileToEdit.source
                    fileName = if (profileToEdit.source.isNotEmpty()) File(profileToEdit.source).name else ""
                }
            } else if (!importUrl.isNullOrBlank()) {
                selectedTypeIndex = 0
                url = importUrl
                Toast.makeText(context, MLang.ProfilesPage.Message.UrlImported, Toast.LENGTH_SHORT)
                    .show()
            } else {
                selectedTypeIndex = 0
                try {
                    val clipboardUrl = readClipboardAndCheckUrl()
                    if (clipboardUrl != null) {
                        url = clipboardUrl
                        Toast.makeText(
                            context, MLang.ProfilesPage.Message.ClipboardRead, Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (_: Exception) {
                }
            }
        }
        onDispose { }
    }


    LaunchedEffect(downloadProgress) {
        val progress = downloadProgress
        if (progress != null) {
            val currentProgress = progress.percent

            if (downloadStartTime == 0L && currentProgress > 0) {
                downloadStartTime = System.currentTimeMillis()
            }

            val actualProgress = when {
                currentProgress == 0 -> {
                    0
                }

                currentProgress < 100 -> {
                    if (currentProgress < 10) {
                        val cycleTime = 2000L
                        val elapsed = (System.currentTimeMillis() % cycleTime)
                        val cycleProgress = (elapsed.toFloat() / cycleTime * 3).toInt()
                        (currentProgress + cycleProgress).coerceAtMost(10)
                    } else {
                        currentProgress
                    }
                }

                currentProgress == 100 -> {
                    100
                }

                else -> {
                    lastProgress
                }
            }

            if (actualProgress >= displayedProgress) {
                if (actualProgress - displayedProgress > 3) {
                    val steps = ((actualProgress - displayedProgress) / 3).coerceAtLeast(1)
                    for (i in 1..steps) {
                        val stepProgress =
                            kotlin.math.min(displayedProgress + (i * 3), actualProgress)
                        displayedProgress = stepProgress
                        delay(40.milliseconds)
                    }
                } else {
                    displayedProgress = actualProgress
                }
            } else if (lastProgress < 100) {
                displayedProgress = actualProgress
            }

            lastProgress = actualProgress

            if (progress.percent == 100 && displayedProgress == 100 && !hasShownCompleteAnimation) {
                hasShownCompleteAnimation = true

                delay(100.milliseconds)

                onDownloadComplete()
            }
        }
    }


    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            context.toast(uiState.error!!, Toast.LENGTH_LONG)
            if (isDownloading) {
                isDownloading = false
                error = uiState.error ?: MLang.ProfilesPage.Misc.Error
            }
            profilesViewModel.clearError()
        }
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            profilesViewModel.clearMessage()
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val actualFileName = context.contentResolver.query(
                it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: MLang.ProfilesPage.Message.UnknownFile

            val extension = actualFileName.substringAfterLast(".", "")
            if (!extension.equals("yaml", ignoreCase = true) && !extension.equals(
                    "yml", ignoreCase = true
                )
            ) {
                error = MLang.ProfilesPage.Validation.YamlOnly
                return@let
            }

            filePath = it.toString()
            error = ""
            fileName = actualFileName

            val fileNameWithoutExt = actualFileName.substringBeforeLast(".")
            if (name.isBlank() || name == actualFileName) {
                name = fileNameWithoutExt.ifBlank { MLang.ProfilesPage.Input.NewProfile }
            }
        }
    }

    val qrImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                scope.launch {
                    try {
                        val result = readQrFromImage(context, it)
                        if (result != null) {
                            url = result
                            selectedTypeIndex = 0
                            Toast.makeText(
                                context,
                                MLang.ProfilesPage.QrScanner.RecognizeSuccess,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                MLang.ProfilesPage.QrScanner.RecognizeFailed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            MLang.ProfilesPage.QrScanner.RecognizeError.format(e.message ?: ""),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    val showCameraPreview = remember { mutableStateOf(false) }

    LaunchedEffect(selectedTypeIndex, show.value, isDownloading, hasCameraPermission) {
        showCameraPreview.value =
            show.value && selectedTypeIndex == 2 && !isDownloading && hasCameraPermission
    }

    WindowBottomSheet(
        show = show.value,
        title = if (profileToEdit != null) MLang.ProfilesPage.Sheet.EditTitle else MLang.ProfilesPage.Sheet.AddTitle,
        backgroundColor = MiuixTheme.colorScheme.surface,
        dragHandleColor = MiuixTheme.colorScheme.onSurfaceVariantActions,
        insideMargin = DpSize(32.dp, 16.dp),
        onDismissRequest = {
            if (!isDownloading) {
                showCameraPreview.value = false
                show.value = false
                profilesViewModel.clearDownloadProgress()
            }
        }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = isDownloading,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp), contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            16.dp, Alignment.CenterVertically
                        )
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = displayedProgress == 100,
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                                        animationSpec = tween(300)
                                    )
                                },
                                label = "ProgressIcon"
                            ) { complete ->
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (complete) {
                                        Icon(
                                            imageVector = Yume.`Package-check`,
                                            contentDescription = MLang.ProfilesPage.Misc.Complete,
                                            tint = MiuixTheme.colorScheme.primary,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .padding(start = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = downloadProgress?.message ?: MLang.ProfilesPage.Progress.Downloading,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "$displayedProgress%",
                            style = MiuixTheme.textStyles.body2,
                            color = if (displayedProgress >= 100) {
                                androidx.compose.ui.graphics.Color.Transparent
                            } else {
                                MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isDownloading,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    top.yukonga.miuix.kmp.basic.Card {
                        Box(modifier = Modifier.alpha(if (profileToEdit != null) 0.5f else 1f)) {
                            WindowSpinner(
                                title = MLang.ProfilesPage.Type.Title, items = listOf(
                                    SpinnerEntry(title = MLang.ProfilesPage.Type.Subscription),
                                    SpinnerEntry(title = MLang.ProfilesPage.Type.LocalFile),
                                    SpinnerEntry(title = MLang.ProfilesPage.Type.QrScan)
                                ), selectedIndex = selectedTypeIndex, onSelectedIndexChange = {
                                    if (profileToEdit == null) {
                                        selectedTypeIndex = it
                                        clearCurrentTypeState()
                                    }
                                })

                            if (profileToEdit != null) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = {})
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = selectedTypeIndex, transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(
                                animationSpec = tween(
                                    150
                                )
                            )
                        }, label = "ProfileTypeContent"
                    ) { typeIndex ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (typeIndex) {
                                2 -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MiuixTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (showCameraPreview.value) {
                                            key("qr_scanner_stable") {
                                                StableQrScanner(
                                                    onScanned = { scannedUrl ->
                                                        showCameraPreview.value = false
                                                        url = scannedUrl
                                                        selectedTypeIndex = 0
                                                        Toast.makeText(
                                                            context,
                                                            MLang.ProfilesPage.QrScanner.ScanSuccess,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    })
                                            }
                                        } else if (!hasCameraPermission) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(MLang.ProfilesPage.QrScanner.NeedPermission)
                                            }
                                        } else {
                                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        }
                                    }

                                    TextButton(
                                        text = MLang.ProfilesPage.QrScanner.SelectFromAlbum,
                                        onClick = { qrImageLauncher.launch("image/*") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            showCameraPreview.value = false
                                            show.value = false
                                            profilesViewModel.clearDownloadProgress()
                                        }, modifier = Modifier.fillMaxWidth()
                                    ) { Text(MLang.ProfilesPage.Button.Cancel) }
                                }

                                else -> {
                                    TextField(
                                        value = name,
                                        onValueChange = { name = it; error = "" },
                                        label = MLang.ProfilesPage.Input.ProfileName,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (typeIndex == 0) {
                                        TextField(
                                            value = url,
                                            onValueChange = { url = it; error = "" },
                                            label = MLang.ProfilesPage.Input.SubscriptionUrl,
                                            maxLines = 2,
                                            readOnly = profileToEdit != null,
                                            enabled = profileToEdit == null,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        TextField(
                                            value = fileName.ifEmpty { "" },
                                            onValueChange = { },
                                            label = MLang.ProfilesPage.Input.SelectFile,
                                            readOnly = true,
                                            enabled = false,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }) {
                                                    launcher.launch(
                                                        "*/*"
                                                    )
                                                })
                                    }

                                    if (error.isNotEmpty()) {
                                        Text(
                                            text = error,
                                            color = MiuixTheme.colorScheme.error,
                                            style = MiuixTheme.textStyles.body2
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = {
                                                show.value = false
                                                profilesViewModel.clearDownloadProgress()
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) { Text(MLang.ProfilesPage.Button.Cancel) }
                                        Button(
                                            onClick = {
                                                if (typeIndex == 0 && url.isBlank()) {
                                                    error =
                                                        MLang.ProfilesPage.Validation.EnterUrl; return@Button
                                                }
                                                if (typeIndex == 1 && filePath.isBlank()) {
                                                    error =
                                                        MLang.ProfilesPage.Validation.SelectFile; return@Button
                                                }

                                                keyboardController?.hide()
                                                profilesViewModel.clearError()
                                                downloadStartTime = System.currentTimeMillis()
                                                displayedProgress = 0
                                                lastProgress = 0
                                                hasShownCompleteAnimation = false
                                                isDownloading = true

                                                if (typeIndex == 0) {
                                                    if (profileToEdit != null) {
                                                        onUpdateProfile(
                                                            profileToEdit.uuid,
                                                            name,
                                                            url,
                                                            profileToEdit.interval
                                                        )
                                                    } else {
                                                        onAddProfile(
                                                            name.ifBlank { MLang.ProfilesPage.Input.NewProfile },
                                                            url,
                                                            Profile.Type.Url,
                                                            0L,
                                                            null  // URL 类型不需要 fileUri
                                                        )
                                                    }
                                                } else {
                                                    if (profileToEdit != null) {
                                                        onUpdateProfile(
                                                            profileToEdit.uuid,
                                                            name,
                                                            profileToEdit.source,
                                                            profileToEdit.interval
                                                        )
                                                    } else {
                                                        onAddProfile(
                                                            name.ifBlank { MLang.ProfilesPage.Input.NewProfile },
                                                            filePath,
                                                            Profile.Type.File,
                                                            0L,
                                                            filePath.toUri()  // File 类型需要传递 fileUri
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColorsPrimary()
                                        ) {
                                            Text(
                                                MLang.ProfilesPage.Button.Confirm,
                                                color = MiuixTheme.colorScheme.background
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
