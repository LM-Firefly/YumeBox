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

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.WebViewActivity
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.ProfileLink
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.icon.Yume
import com.github.yumelira.yumebox.presentation.icon.yume.*
import com.github.yumelira.yumebox.presentation.theme.LocalSpacing
import com.github.yumelira.yumebox.screen.home.HomeViewModel
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.util.*

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
            TopBar(
                title = MLang.ProfilesPage.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    Row {
                        IconButton(
                            modifier = Modifier.padding(start = 24.dp), onClick = {
                                if (links.isNotEmpty()) {
                                    // 优先使用默认链接, 如果没有设置默认链接则使用第一个
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
                            Icon(
                                imageVector = Yume.Chromium,
                                contentDescription = MLang.ProfilesPage.Misc.OpenLink
                            )
                        }
                        IconButton(
                            modifier = Modifier.padding(start = 12.dp),
                            onClick = { showLinkSettingsDialog.value = true }) {
                            Icon(
                                imageVector = Yume.`Link-2`,
                                contentDescription = MLang.ProfilesPage.LinkSettings.Title
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isDownloading) {
                                isDownloading = true
                                scope.launch {
                                    profiles.filter { it.type == Profile.Type.Url }.forEach { p ->
                                        profilesViewModel.updateProfile(p.uuid)
                                    }
                                    isDownloading = false
                                }
                            }
                        }, modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            Yume.`Circle-fading-arrow-up`,
                            contentDescription = MLang.ProfilesPage.Action.UpdateAll
                        )
                    }

                    IconButton(
                        onClick = {
                            profileToEdit = null
                            showAddBottomSheet.value = true
                        }, modifier = Modifier.padding(end = LocalSpacing.current.xxl)
                    ) {
                        Icon(
                            Yume.`Badge-plus`,
                            contentDescription = MLang.ProfilesPage.Action.AddProfile
                        )
                    }
                })
        },
    ) { innerPadding ->
        if (profiles.isEmpty()) {

            CenteredText(
                firstLine = MLang.ProfilesPage.Empty.NoProfiles,
                secondLine = MLang.ProfilesPage.Empty.Hint
            )
        } else {
            val lazyListState = rememberLazyListState()
            val reorderableLazyListState =
                rememberReorderableLazyListState(lazyListState) { from, to ->
                    profilesViewModel.reorderProfiles(from.index, to.index)
                }

            ScreenLazyColumn(
                lazyListState = lazyListState,
                scrollBehavior = scrollBehavior,
                innerPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + mainInnerPadding.calculateBottomPadding() + LocalSpacing.current.md,
                ),
            ) {
                items(
                    items = profiles,
                    key = { it.uuid.toString() }
                ) { profile ->
                    ReorderableItem(
                        reorderableLazyListState,
                        key = profile.uuid.toString()
                    ) { isDragging ->
                        ProfileCard(
                            profile = profile,
                            workDir = File(
                                com.github.yumelira.yumebox.App.instance.filesDir,
                                "imported"
                            ),
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
                                        profilesViewModel.updateProfile(profile.uuid)
                                        isDownloading = false
                                    }
                                }
                            },
                            onDelete = { profile ->
                                if (!isDownloading) showDeleteDialog = profile
                            },
                            onEdit = { profile ->
                                if (!isDownloading) {
                                    profileToEdit = profile
                                    editName = profile.name
                                    showEditDialog.value = true
                                }
                            },
                            onToggleEnabled = { profile ->
                                if (!isDownloading) {
                                    scope.launch {
                                        profilesViewModel.toggleProfileEnabled(profile.uuid)
                                        if (isRunning) {
                                            homeViewModel.reloadProfile()
                                        }
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
        onAddProfile = { name, source, type, interval, fileUri ->
            profilesViewModel.createProfile(type, name, source, interval, fileUri)
        },
        onUpdateProfile = { uuid, name, source, interval ->
            profilesViewModel.patchProfile(uuid, name, source, interval)
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
            profilesViewModel.deleteProfile(profile.uuid)
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
                    profilesViewModel.patchProfile(
                        currentProfileToEdit.uuid,
                        newName,
                        currentProfileToEdit.source,
                        currentProfileToEdit.interval
                    )
                    showEditDialog.value = false
                }
            })
    }

    LinkSettingsDialog(
        show = showLinkSettingsDialog,
        links = links,
        linkOpenMode = linkOpenMode,
        defaultLinkId = defaultLinkId,
        onOpenModeChange = { mode ->
            profilesViewModel.setOpenMode(mode)
        },
        onDefaultLinkChange = { linkId ->
            profilesViewModel.defaultLinkId.set(linkId)
        },
        onAddLink = {
            linkToEdit = null
            showAddLinkDialog.value = true
        },
        onDeleteLink = { linkId ->
            val currentLinks = links
            profilesViewModel.links.set(currentLinks.filter { it.id != linkId })
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
            val currentLinks = links
            if (currentLinkToEdit != null) {
                // Update existing link
                profilesViewModel.links.set(currentLinks.map {
                    if (it.id == currentLinkToEdit.id)
                        it.copy(name = newLinkName, url = newLinkUrl)
                    else it
                })
            } else {
                // Add new link
                val newLink = ProfileLink(
                    id = UUID.randomUUID().toString(),
                    name = newLinkName,
                    url = newLinkUrl
                )
                profilesViewModel.links.set(currentLinks + newLink)
            }
            showAddLinkDialog.value = false
            linkToEdit = null
            newLinkName = ""
            newLinkUrl = ""
        })

    if (showShareDialog.value && profileToShare != null) {
        ShareOptionsDialog(show = showShareDialog, profile = profileToShare!!, onDismiss = {
            showShareDialog.value = false
            profileToShare = null
        }, onShareFile = { profile ->
            val context = com.github.yumelira.yumebox.App.instance
            val file = File(File(context.filesDir, "imported"), "${profile.uuid}/config.yaml")
                .takeIf { it.exists() }
                ?: File(File(context.filesDir, "clash"), "profiles/${profile.uuid}/config.yaml").takeIf { it.exists() }

            if (file == null) {
                Toast.makeText(context, "配置文件不存在", Toast.LENGTH_SHORT).show()
            } else {
                runCatching {
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/x-yaml"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            intent, MLang.ProfilesPage.ShareDialog.ShareFile
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                }.onFailure {
                    Toast.makeText(context, it.message ?: "share failed", Toast.LENGTH_SHORT).show()
                }
            }
            showShareDialog.value = false
            profileToShare = null
        }, onShareLink = { profile ->
            val context = com.github.yumelira.yumebox.App.instance
            val url = if (profile.type == Profile.Type.Url) profile.source else null
            url?.let {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, it)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(
                        intent, MLang.ProfilesPage.ShareDialog.ShareLink
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
            } ?: run {
                Toast.makeText(context, MLang.ProfilesPage.ShareDialog.NoLink, Toast.LENGTH_SHORT)
                    .show()
            }
            showShareDialog.value = false
            profileToShare = null
        })
    }
}
