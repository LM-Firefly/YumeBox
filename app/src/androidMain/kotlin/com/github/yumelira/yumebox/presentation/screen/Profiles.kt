package com.github.yumelira.yumebox.presentation.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.data.model.Profile
import com.github.yumelira.yumebox.data.model.ProfileType
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.ProfileLink
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.`Badge-plus`
import com.github.yumelira.yumebox.presentation.icon.yume.Chromium
import com.github.yumelira.yumebox.presentation.icon.yume.`Circle-fading-arrow-up`
import com.github.yumelira.yumebox.presentation.icon.yume.`Link-2`
import com.github.yumelira.yumebox.presentation.icon.yume.`Package-check`
import com.github.yumelira.yumebox.presentation.theme.LocalSpacing
import com.github.yumelira.yumebox.presentation.viewmodel.HomeViewModel
import com.github.yumelira.yumebox.presentation.viewmodel.ProfilesViewModel
import com.github.yumelira.yumebox.presentation.webview.WebViewActivity
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SpinnerEntry
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSpinner
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.basic.Search
import top.yukonga.miuix.kmp.icon.icons.useful.Delete
import top.yukonga.miuix.kmp.icon.icons.useful.Edit
import top.yukonga.miuix.kmp.icon.icons.useful.New
import top.yukonga.miuix.kmp.icon.icons.useful.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import sh.calvin.reorderable.ReorderableCollectionItemScope
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

@SuppressLint("UseKtx")
@Composable
fun ProfilesPager(mainInnerPadding: PaddingValues) {
    val profilesViewModel = koinViewModel<ProfilesViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val profiles by profilesViewModel.profiles.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()

    val showAddBottomSheet = remember { mutableStateOf(false) }
    val showLinkSettingsDialog = remember { mutableStateOf(false) }
    val showAddLinkDialog = remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Profile?>(null) }
    val showEditDialog = remember { mutableStateOf(false) }
    val showShareDialog = remember { mutableStateOf(false) }
    var profileToShare by remember { mutableStateOf<Profile?>(null) }
    var pendingProfileId by remember { mutableStateOf<String?>(null) }
    var profileToEdit by remember { mutableStateOf<Profile?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }

    var importUrlFromScheme by remember { mutableStateOf<String?>(null) }
    val pendingImportUrl by MainActivity.pendingImportUrl.collectAsState()

    var scannedUrl by remember { mutableStateOf<String?>(null) }

    val links by profilesViewModel.links.state.collectAsState()
    val linkOpenMode by profilesViewModel.linkOpenMode.state.collectAsState()
    val defaultLinkId by profilesViewModel.defaultLinkId.state.collectAsState()
    var linkToEdit by remember { mutableStateOf<ProfileLink?>(null) }
    var newLinkName by remember { mutableStateOf("") }
    var newLinkUrl by remember { mutableStateOf("") }

    LaunchedEffect(pendingImportUrl) {
        if (pendingImportUrl != null) {
            importUrlFromScheme = pendingImportUrl
            profileToEdit = null
            showAddBottomSheet.value = true
            MainActivity.clearPendingImportUrl()
        }
    }

    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingProfileId?.let { profileId ->
                homeViewModel.startProxy(profileId, useTunMode = true)
            }
        }
        pendingProfileId = null
    }

    val scrollBehavior = MiuixScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopBar(title = "配置", scrollBehavior = scrollBehavior, navigationIcon = {
                Row {
                    IconButton(
                        modifier = Modifier.padding(start = 24.dp),
                        onClick = {
                            if (links.isNotEmpty()) {
                                // 优先使用默认链接,如果没有设置默认链接则使用第一个
                                val link = if (defaultLinkId.isNotEmpty()) {
                                    links.find { it.id == defaultLinkId } ?: links.first()
                                } else {
                                    links.first()
                                }
                                val context = com.github.yumelira.yumebox.App.instance
                                if (linkOpenMode == LinkOpenMode.IN_APP) {
                                    WebViewActivity.start(context, link.url)
                                } else {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            } else {
                                showLinkSettingsDialog.value = true
                            }
                        }) {
                        Icon(imageVector = Yume.Chromium, contentDescription = "打开链接")
                    }
                    IconButton(
                        modifier = Modifier.padding(start = 12.dp),
                        onClick = { showLinkSettingsDialog.value = true }) {
                        Icon(imageVector = Yume.`Link-2`, contentDescription = "链接设置")
                    }
                }
            }, actions = {
                IconButton(
                    onClick = {
                        if (!isDownloading) {
                            isDownloading = true
                            scope.launch {
                                profiles.filter { it.type == ProfileType.URL }.forEach { p ->
                                    try {
                                        profilesViewModel.downloadProfile(p)
                                    } catch (_: Exception) {
                                    }
                                }
                                isDownloading = false
                            }
                        }
                    }, modifier = Modifier.padding(end = 12.dp)
                ) {
                    Icon(
                        Yume.`Circle-fading-arrow-up`, contentDescription = "一键更新所有"
                    )
                }

                IconButton(
                    onClick = {
                        profileToEdit = null
                        showAddBottomSheet.value = true
                    }, modifier = Modifier.padding(end = LocalSpacing.current.xxl)
                ) {
                    Icon(
                        Yume.`Badge-plus`, contentDescription = "添加配置"
                    )
                }
            })
        },
    ) { innerPadding ->
        if (profiles.isEmpty()) {

            CenteredText(
                firstLine = "暂无配置文件", secondLine = "点击右上角添加配置"
            )
        } else {
            val lazyListState = rememberLazyListState()
            val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
                profilesViewModel.reorderProfiles(from.index, to.index)
            }

            val bottomBarScrollBehavior = LocalBottomBarScrollBehavior.current

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .let { mod ->
                        if (bottomBarScrollBehavior != null) {
                            mod.nestedScroll(bottomBarScrollBehavior.nestedScrollConnection)
                        } else mod
                    },
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + mainInnerPadding.calculateBottomPadding() + LocalSpacing.current.md,
                ),
                overscrollEffect = null,
            ) {
                items(profiles.size, key = { profiles[it].id }) { index ->
                    ReorderableItem(reorderableLazyListState, key = profiles[index].id) { isDragging ->
                        val profile = profiles[index]

                        ProfileCard(
                            profile = profile,
                            workDir = File(com.github.yumelira.yumebox.App.instance.filesDir, "clash"),
                            isDownloading = isDownloading,
                            modifier = Modifier
                                .longPressDraggableHandle()
                                .alpha(if (isDragging) 0.9f else 1f),
                            onExport = { profile ->
                                if (!isDownloading) {
                                    profileToShare = profile
                                    showShareDialog.value = true
                                }
                            },
                            onUpdate = { profile ->
                                if (!isDownloading) {
                                    isDownloading = true
                                    scope.launch {
                                        profilesViewModel.downloadProfile(profile)
                                        isDownloading = false
                                    }
                                }
                            },
                            onDelete = { profile -> if (!isDownloading) showDeleteDialog = profile },
                            onEdit = { profile ->
                                if (!isDownloading) {
                                    profileToEdit = profile
                                    editName = profile.name
                                    showEditDialog.value = true
                                }
                            },
                            onToggleEnabled = { updatedProfile ->
                                if (!isDownloading) {
                                    scope.launch {
                                        profilesViewModel.toggleProfileEnabled(
                                            profile = updatedProfile,
                                            enabled = updatedProfile.enabled,
                                            onProfileEnabled = { enabledProfile ->
                                                if (isRunning) {
                                                    scope.launch {
                                                        homeViewModel.reloadProfile(enabledProfile.id)
                                                    }
                                                }
                                            })
                                    }
                                }
                            })
                    }
                }
            }
        }
    }

    LocalContext.current
    AddProfileSheet(
        show = showAddBottomSheet,
        profileToEdit = profileToEdit,
        importUrl = importUrlFromScheme ?: scannedUrl,
        onAddProfile = { profile ->

            profilesViewModel.addProfile(profile)
        },
        onUpdateProfile = { profile ->
            profilesViewModel.updateProfile(profile)
        },
        onDownloadComplete = {
            isDownloading = false
            showAddBottomSheet.value = false
            profilesViewModel.clearDownloadProgress()
        },
        profilesViewModel = profilesViewModel
    )

    showDeleteDialog?.let { profile ->
        DeleteConfirmDialog(profileName = profile.name, onConfirm = {
            profilesViewModel.removeProfile(profile.id)
            showDeleteDialog = null
        }, onDismiss = { showDeleteDialog = null })
    }

    val currentProfileToEdit = profileToEdit
    if (showEditDialog.value && currentProfileToEdit != null) {
        EditProfileNameDialog(
            show = showEditDialog,
            currentName = currentProfileToEdit.name,
            onDismiss = {
                showEditDialog.value = false
                profileToEdit = null
            },
            onConfirm = { newName ->
                if (newName.isNotBlank()) {
                    val updatedProfile = currentProfileToEdit.copy(
                        name = newName, updatedAt = System.currentTimeMillis()
                    )
                    profilesViewModel.updateProfile(updatedProfile)
                    showEditDialog.value = false
                }
            })
    }

    if (showLinkSettingsDialog.value) {
        LinkSettingsDialog(
            show = showLinkSettingsDialog,
            links = links,
            linkOpenMode = linkOpenMode,
            defaultLinkId = defaultLinkId,
            onOpenModeChange = { mode ->
                profilesViewModel.setOpenMode(mode)
            },
            onDefaultLinkChange = { linkId ->
                profilesViewModel.setDefaultLink(linkId)
            },
            onAddLink = {
                linkToEdit = null
                showAddLinkDialog.value = true
            },
            onDeleteLink = { linkId ->
                profilesViewModel.removeLink(linkId)
            },
            onOpenLink = { link ->
                val context = com.github.yumelira.yumebox.App.instance
                if (linkOpenMode == LinkOpenMode.IN_APP) {
                    WebViewActivity.start(context, link.url)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, link.url.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            })
    }

    if (showAddLinkDialog.value) {
        val currentLinkToEdit = linkToEdit
        AddLinkDialog(
            show = showAddLinkDialog,
            linkToEdit = currentLinkToEdit,
            linkName = newLinkName,
            onNameChange = { newLinkName = it },
            linkUrl = newLinkUrl,
            onUrlChange = { newLinkUrl = it },
            onDismiss = {
                showAddLinkDialog.value = false
                linkToEdit = null
                newLinkName = ""
                newLinkUrl = ""
            },
            onConfirm = {
                if (currentLinkToEdit != null) {
                    profilesViewModel.updateLink(currentLinkToEdit.id, newLinkName, newLinkUrl)
                } else {
                    val newLink = ProfileLink(
                        id = UUID.randomUUID().toString(), name = newLinkName, url = newLinkUrl
                    )
                    profilesViewModel.addLink(newLink)
                }
                showAddLinkDialog.value = false
                linkToEdit = null
                newLinkName = ""
                newLinkUrl = ""
            })
    }

    if (showShareDialog.value && profileToShare != null) {
        ShareOptionsDialog(
            show = showShareDialog,
            profile = profileToShare!!,
            onDismiss = {
                showShareDialog.value = false
                profileToShare = null
            },
            onShareFile = { profile ->
                val context = com.github.yumelira.yumebox.App.instance
                val importedDir = File(context.filesDir, "imported")
                val profileDir = File(importedDir, profile.id)
                val file = File(profileDir, "config.yaml")
                if (file.exists()) {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                intent, "分享配置文件"
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                showShareDialog.value = false
                profileToShare = null
            },
            onShareLink = { profile ->
                val context = com.github.yumelira.yumebox.App.instance
                profile.remoteUrl?.let { url ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            intent, "分享订阅链接"
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                } ?: run {
                    Toast.makeText(context, "该配置没有订阅链接", Toast.LENGTH_SHORT).show()
                }
                showShareDialog.value = false
                profileToShare = null
            }
        )
    }
}


@Composable
private fun EditProfileNameDialog(
    show: MutableState<Boolean>, currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var editName by remember { mutableStateOf(currentName) }

    SuperDialog(
        title = "编辑配置名称", show = show, onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = editName,
                onValueChange = { editName = it },
                label = "配置名称",
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss, modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { onConfirm(editName) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        "确定", color = MiuixTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
private fun AddProfileSheet(
    show: MutableState<Boolean>,
    profileToEdit: Profile? = null,
    importUrl: String? = null,
    onAddProfile: (Profile) -> Unit,
    onUpdateProfile: (Profile) -> Unit = {},
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
    var displayedProgress by remember { mutableStateOf(0) }
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
        } catch (e: SecurityException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
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
            Toast.makeText(context, "需要相机权限才能扫码", Toast.LENGTH_LONG).show()
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
                if (profileToEdit.type == ProfileType.URL) {
                    selectedTypeIndex = 0
                    url = profileToEdit.remoteUrl ?: ""
                } else {
                    selectedTypeIndex = 1
                    filePath = profileToEdit.config
                    fileName = File(profileToEdit.config).name
                }
            } else if (!importUrl.isNullOrBlank()) {
                selectedTypeIndex = 0
                url = importUrl
                Toast.makeText(context, "已从链接导入配置", Toast.LENGTH_SHORT).show()
            } else {
                selectedTypeIndex = 0
                try {
                    val clipboardUrl = readClipboardAndCheckUrl()
                    if (clipboardUrl != null) {
                        url = clipboardUrl
                        Toast.makeText(
                            context, "已从剪贴板读取链接", Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        onDispose { }
    }


    LaunchedEffect(downloadProgress) {
        val progress = downloadProgress
        if (progress != null) {
            val currentProgress = progress.progress

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
                        val stepProgress = kotlin.math.min(displayedProgress + (i * 3), actualProgress)
                        displayedProgress = stepProgress
                        delay(80)
                    }
                } else {
                    displayedProgress = actualProgress
                }
            } else if (lastProgress < 100) {
                displayedProgress = actualProgress
            }

            lastProgress = actualProgress

            if (progress.progress == 100 && displayedProgress == 100 && !hasShownCompleteAnimation) {
                hasShownCompleteAnimation = true

                delay(300)

                onDownloadComplete()
            }
        }
    }


    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            context.toast(uiState.error!!, Toast.LENGTH_LONG)
            if (isDownloading) {
                isDownloading = false
                error = uiState.error ?: "错误"
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
            } ?: "未知文件"

            val extension = actualFileName.substringAfterLast(".", "")
            if (!extension.equals("yaml", ignoreCase = true) && !extension.equals(
                    "yml", ignoreCase = true
                )
            ) {
                error = "仅支持 .yaml 或 .yml 格式的配置文件"
                return@let
            }

            filePath = it.toString()
            error = ""
            fileName = actualFileName

            val fileNameWithoutExt = actualFileName.substringBeforeLast(".")
            if (name.isBlank() || name == actualFileName) {
                name = fileNameWithoutExt.ifBlank { "新配置" }
            }
        }
    }

    val qrImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val result = readQrFromImage(context, it)
                    if (result != null) {
                        url = result
                        selectedTypeIndex = 0
                        Toast.makeText(
                            context, "识别成功", Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context, "未能识别到二维码", Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context, "识别失败: ${e.message}", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val showCameraPreview = remember { mutableStateOf(false) }

    LaunchedEffect(selectedTypeIndex, show.value, isDownloading, hasCameraPermission) {
        showCameraPreview.value = show.value && selectedTypeIndex == 2 && !isDownloading && hasCameraPermission
    }

    SuperBottomSheet(
        show = show,
        title = if (profileToEdit != null) "编辑配置文件" else "添加配置文件",
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
                visible = isDownloading, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()
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
                                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                                ) {
                                    if (complete) {
                                        Icon(
                                            imageVector = Yume.`Package-check`,
                                            contentDescription = "完成",
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
                            text = downloadProgress?.message ?: "下载中...",
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
                visible = !isDownloading, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    top.yukonga.miuix.kmp.basic.Card {
                        Box(modifier = Modifier.alpha(if (profileToEdit != null) 0.5f else 1f)) {
                            SuperSpinner(
                                title = "配置类型", items = listOf(
                                    SpinnerEntry(title = "订阅链接"),
                                    SpinnerEntry(title = "本地文件"),
                                    SpinnerEntry(title = "扫码添加")
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
                            modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                                            context, "扫描成功", Toast.LENGTH_SHORT
                                                        ).show()
                                                    })
                                            }
                                        } else if (!hasCameraPermission) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text("需要相机权限")
                                                Spacer(modifier = Modifier.height(8.dp))
                                                TextButton(
                                                    text = "授予权限", onClick = {
                                                        cameraPermissionLauncher.launch(
                                                            Manifest.permission.CAMERA
                                                        )
                                                    })
                                            }
                                        } else {
                                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        }
                                    }

                                    TextButton(
                                        text = "从相册选择二维码图片",
                                        onClick = { qrImageLauncher.launch("image/*") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            showCameraPreview.value = false
                                            show.value = false
                                            profilesViewModel.clearDownloadProgress()
                                        }, modifier = Modifier.fillMaxWidth()
                                    ) { Text("取消") }
                                }

                                else -> {
                                    TextField(
                                        value = name,
                                        onValueChange = { name = it; error = "" },
                                        label = "配置名称",
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (typeIndex == 0) {
                                        TextField(
                                            value = url,
                                            onValueChange = { url = it; error = "" },
                                            label = "订阅链接 (HTTP/HTTPS)",
                                            maxLines = 2,
                                            readOnly = profileToEdit != null,
                                            enabled = profileToEdit == null,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        TextField(
                                            value = fileName.ifEmpty { "" },
                                            onValueChange = { },
                                            label = "点击选择文件",
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
                                        ) { Text("取消") }
                                        Button(
                                            onClick = {
                                                if (typeIndex == 0 && url.isBlank()) {
                                                    error = "请输入链接"; return@Button
                                                }
                                                if (typeIndex == 1 && filePath.isBlank()) {
                                                    error = "请选择文件"; return@Button
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
                                                        val updatedProfile = profileToEdit.copy(
                                                            name = name,
                                                            remoteUrl = url,
                                                            updatedAt = System.currentTimeMillis()
                                                        )
                                                        onUpdateProfile(updatedProfile)
                                                        show.value = false
                                                    } else {
                                                        scope.launch {
                                                            val profile = Profile(
                                                                id = UUID.randomUUID().toString(),
                                                                name = name.ifBlank { "新配置" },
                                                                config = "",
                                                                remoteUrl = url,
                                                                type = ProfileType.URL,
                                                                createdAt = System.currentTimeMillis(),
                                                                updatedAt = System.currentTimeMillis()
                                                            )

                                                            val downloadedProfile = profilesViewModel.downloadProfile(
                                                                profile, saveToDb = true
                                                            )
                                                            if (downloadedProfile != null && downloadedProfile.config.isNotBlank()) {
                                                            } else {
                                                                isDownloading = false
                                                                profilesViewModel.clearDownloadProgress()
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (profileToEdit != null) {
                                                        val updatedProfile = profileToEdit.copy(
                                                            name = name, updatedAt = System.currentTimeMillis()
                                                        )
                                                        onUpdateProfile(updatedProfile)
                                                        show.value = false
                                                    } else {
                                                        scope.launch {
                                                            val importedProfile =
                                                                profilesViewModel.importProfileFromFile(
                                                                    filePath.toUri(),
                                                                    name.ifBlank { "新配置" },
                                                                    saveToDb = true
                                                                )
                                                            isDownloading = false
                                                            if (importedProfile != null) {
                                                                show.value = false
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColorsPrimary()
                                        ) {
                                            Text(
                                                "确定", color = MiuixTheme.colorScheme.background
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

@Composable
private fun StableQrScanner(
    onScanned: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnScanned by rememberUpdatedState(onScanned)

    val hasScanned = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    AndroidView(modifier = Modifier
        .fillMaxSize()
        .clipToBounds(), factory = { context ->
        val previewView = PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        val barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )

        val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

        val previewUseCase = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalysisUseCase =
            ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processStableQrImage(barcodeScanner, imageProxy) { text ->
                        if (hasScanned.compareAndSet(false, true)) {
                            currentOnScanned(text)
                        }
                    }
                }
            }

        coroutineScope.launch {
            try {
                val cameraProvider = context.getStableCameraProvider()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase,
                    imageAnalysisUseCase,
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        previewView
    }, onRelease = { previewView ->
        try {
            val context = previewView.context
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    })
}

@android.annotation.SuppressLint("UnsafeOptInUsageError")
private fun processStableQrImage(
    barcodeScanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onScanned: (String) -> Unit,
) {
    imageProxy.image?.let { image ->
        val inputImage = InputImage.fromMediaImage(
            image,
            imageProxy.imageInfo.rotationDegrees,
        )

        barcodeScanner.process(inputImage).addOnSuccessListener { barcodeList ->
            barcodeList.firstOrNull()?.rawValue?.let { text ->
                onScanned(text)
            }
        }.addOnCompleteListener {
            imageProxy.image?.close()
            imageProxy.close()
        }
    } ?: imageProxy.close()
}

private suspend fun Context.getStableCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener(
                { continuation.resume(future.get()) },
                ContextCompat.getMainExecutor(this),
            )
        }
    }

private suspend fun readQrFromImage(context: Context, uri: Uri): String? = suspendCancellableCoroutine { continuation ->
    try {
        val inputImage = InputImage.fromFilePath(context, uri)
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
        scanner.process(inputImage).addOnSuccessListener { barcodes ->
            val barcode = barcodes.getOrNull(0)
            continuation.resume(barcode?.rawValue)
        }.addOnFailureListener {
            continuation.resume(null)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        continuation.resume(null)
    }
}

@Composable
private fun DeleteConfirmDialog(
    profileName: String, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    SuperDialog(
        title = "删除配置",
        summary = "确定要删除「$profileName」吗？",
        show = remember { mutableStateOf(true) },
        onDismissRequest = onDismiss
    ) {
        DialogButtonRow(
            onCancel = onDismiss, onConfirm = onConfirm, cancelText = "取消", confirmText = "删除"
        )
    }
}

@Composable
private fun LinkSettingsDialog(
    show: MutableState<Boolean>,
    links: List<ProfileLink>,
    linkOpenMode: LinkOpenMode,
    defaultLinkId: String,
    onOpenModeChange: (LinkOpenMode) -> Unit,
    onDefaultLinkChange: (String) -> Unit,
    onAddLink: () -> Unit,
    onDeleteLink: (String) -> Unit,
    onOpenLink: (ProfileLink) -> Unit
) {
    val openModeOptions = listOf("App内打开", "外部浏览器")
    val openModeIndex = when (linkOpenMode) {
        LinkOpenMode.IN_APP -> 0
        LinkOpenMode.EXTERNAL_BROWSER -> 1
    }
    
    val defaultLinkIndex = if (defaultLinkId.isEmpty() || links.isEmpty()) {
        0
    } else {
        links.indexOfFirst { it.id == defaultLinkId }.let { if (it == -1) 0 else it }
    }

    SuperBottomSheet(
        title = "链接设置", 
        show = show, 
        onDismissRequest = { 
            show.value = false 
        }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 打开方式选择
            top.yukonga.miuix.kmp.basic.Card {
                SuperDropdown(
                    title = "打开方式",
                    items = openModeOptions,
                    selectedIndex = openModeIndex,
                    onSelectedIndexChange = { index ->
                        val mode = when (index) {
                            0 -> LinkOpenMode.IN_APP
                            1 -> LinkOpenMode.EXTERNAL_BROWSER
                            else -> LinkOpenMode.IN_APP
                        }
                        onOpenModeChange(mode)
                    }
                )
            }
            
            // 默认链接选择
            if (links.isNotEmpty()) {
                top.yukonga.miuix.kmp.basic.Card {
                    SuperDropdown(
                        title = "默认链接",
                        summary = "点击左上角快捷按钮时打开的链接",
                        items = links.map { it.name },
                        selectedIndex = defaultLinkIndex,
                        onSelectedIndexChange = { index ->
                            if (index in links.indices) {
                                onDefaultLinkChange(links[index].id)
                            }
                        }
                    )
                }
            }

            // 链接列表
            if (links.isNotEmpty()) {
                top.yukonga.miuix.kmp.basic.Card {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        links.forEachIndexed { index, link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenLink(link) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = link.name, 
                                        style = MiuixTheme.textStyles.body1
                                    )
                                    Text(
                                        text = link.url,
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                IconButton(
                                    onClick = { onDeleteLink(link.id) }
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Useful.Delete,
                                        contentDescription = "删除",
                                        tint = MiuixTheme.colorScheme.error
                                    )
                                }
                            }
                            
                            if (index < links.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MiuixTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    text = "关闭", 
                    onClick = { show.value = false }, 
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onAddLink,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("添加链接",color = MiuixTheme.colorScheme.surface)
                }
            }
        }
    }
}

@Composable
private fun AddLinkDialog(
    show: MutableState<Boolean>,
    linkToEdit: ProfileLink?,
    linkName: String,
    onNameChange: (String) -> Unit,
    linkUrl: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var error by remember { mutableStateOf("") }

    LaunchedEffect(show.value) {
        if (show.value) {
            if (linkToEdit != null) {
                onNameChange(linkToEdit.name)
                onUrlChange(linkToEdit.url)
            } else {
                onNameChange("")
                onUrlChange("")
            }
            error = ""
        }
    }

    SuperBottomSheet(
        title = "添加链接", 
        show = show, 
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = linkName, 
                onValueChange = {
                    onNameChange(it)
                    error = ""
                }, 
                label = "名称", 
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = linkUrl, 
                onValueChange = {
                    onUrlChange(it)
                    error = ""
                }, 
                label = "链接", 
                modifier = Modifier.fillMaxWidth()
            )

            if (error.isNotEmpty()) {
                Text(
                    text = error, 
                    color = MiuixTheme.colorScheme.error, 
                    style = MiuixTheme.textStyles.body2
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        when {
                            linkName.isBlank() -> error = "请输入名称"
                            linkUrl.isBlank() -> error = "请输入链接"
                            !linkUrl.startsWith("http", ignoreCase = true) -> error = "请输入有效的链接"
                            else -> onConfirm()
                        }
                    }, 
                    modifier = Modifier.weight(1f), 
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("确定", color = MiuixTheme.colorScheme.surface)
                }
            }
        }
    }
}

@Composable
private fun ShareOptionsDialog(
    show: MutableState<Boolean>,
    profile: Profile,
    onDismiss: () -> Unit,
    onShareFile: (Profile) -> Unit,
    onShareLink: (Profile) -> Unit
) {
    SuperDialog(
        title = "分享配置",
        show = show,
        onDismissRequest = onDismiss
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (profile.remoteUrl != null) {
                Button(
                    onClick = { onShareLink(profile) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("分享订阅链接", color = MiuixTheme.colorScheme.surface)
                }
            }
            Button(
                onClick = { onShareFile(profile) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("分享配置文件")
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("取消")
            }
        }
    }
}
