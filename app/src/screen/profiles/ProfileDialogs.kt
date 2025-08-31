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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.domain.model.OverrideConfig
import com.github.yumelira.yumebox.domain.model.ProfileBinding
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumelira.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumelira.yumebox.presentation.component.AppDialog
import com.github.yumelira.yumebox.presentation.component.DialogButtonRow
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.DialogDefaults
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val PROFILE_SETTINGS_MIN_HEIGHT_FRACTION = 0.5f
private const val PROFILE_SETTINGS_MAX_HEIGHT_FRACTION = 0.7f
private val PROFILE_SETTINGS_LIST_MAX_HEIGHT = 420.dp
private const val SYSTEM_OVERRIDE_PREFIX = "preset-"

@Composable
internal fun EditProfileNameDialog(
    show: MutableState<Boolean>,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editName by remember { mutableStateOf(currentName) }

    AppDialog(
        title = MLang.ProfilesPage.EditDialog.Title, show = show.value, onDismissRequest = onDismiss
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
                label = MLang.ProfilesPage.Input.ProfileName,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss, modifier = Modifier.weight(1f)
                ) {
                    Text(MLang.ProfilesPage.Button.Cancel)
                }
                Button(
                    onClick = { onConfirm(editName) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        MLang.ProfilesPage.Button.Confirm, color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
internal fun DeleteConfirmDialog(
    show: Boolean,
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
) {
    AppDialog(
        show = show,
        modifier = Modifier,
        title = MLang.ProfilesPage.DeleteDialog.Title,
        titleColor = DialogDefaults.titleColor(),
        summary = MLang.ProfilesPage.DeleteDialog.Message.format(profileName),
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = onConfirm,
                cancelText = MLang.ProfilesPage.Button.Cancel,
                confirmText = MLang.ProfilesPage.DeleteDialog.Confirm
            )
        })
}

@Composable
internal fun ShareOptionsDialog(
    show: MutableState<Boolean>,
    profile: Profile,
    onDismiss: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
    onShareFile: (Profile) -> Unit,
    onShareLink: (Profile) -> Unit
) {
    AppDialog(
        show = show.value,
        modifier = Modifier,
        title = MLang.ProfilesPage.ShareDialog.Title,
        titleColor = DialogDefaults.titleColor(),
        summary = null,
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (profile.type == Profile.Type.Url) {
                    Button(
                        onClick = { onShareLink(profile) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(
                            MLang.ProfilesPage.ShareDialog.ShareLink,
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    }
                }
                Button(
                    onClick = { onShareFile(profile) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(MLang.ProfilesPage.ShareDialog.ShareFile)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(MLang.ProfilesPage.Button.Cancel)
                }
            }
        })
}

@Composable
internal fun ProfileSettingsDialog(
    show: MutableState<Boolean>,
    profile: Profile,
    systemPreset: OverrideConfig?,
    userConfigs: List<OverrideConfig>,
    binding: ProfileBinding?,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onSaveProfileMeta: (String, String) -> Unit,
    onSaveOverrideSettings: (Boolean, List<String>) -> Unit,
) {
    val initialOverrideIds = binding
        ?.overrideIds
        .orEmpty()
        .filterNot(::isBuiltinPresetOverrideId)
    val initialSystemPresetEnabled = binding?.enabled ?: false
    val appliedOverrideIds = initialOverrideIds
    var editName by remember { mutableStateOf(profile.name) }
    var editSource by remember { mutableStateOf("") }
    var systemPresetSelected by remember { mutableStateOf(initialSystemPresetEnabled) }
    var pendingSelectedUserOverrideIds by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(show.value, profile.uuid, profile.name, binding?.overrideIds, binding?.enabled) {
        if (show.value) {
            editName = profile.name
            editSource = ""
            systemPresetSelected = initialSystemPresetEnabled
            pendingSelectedUserOverrideIds = appliedOverrideIds
        }
    }

    val toggleUserOverrideSelection: (String, Boolean) -> Unit = { overrideId, isSelected ->
        pendingSelectedUserOverrideIds =
            toggleOverrideIdSelection(pendingSelectedUserOverrideIds, overrideId, isSelected)
    }
    val saveSettings = {
        val trimmedName = editName.trim()
        val trimmedSource = editSource.trim()
        val targetSource = if (profile.type == Profile.Type.Url && trimmedSource.isNotEmpty()) {
            trimmedSource
        } else {
            profile.source
        }
        if (trimmedName.isNotEmpty() && targetSource.isNotEmpty() &&
            (trimmedName != profile.name || targetSource != profile.source)
        ) {
            onSaveProfileMeta(trimmedName, targetSource)
        }

        val finalSelectedOverrideIds = buildFinalOverrideIds(pendingSelectedUserOverrideIds)
        onSaveOverrideSettings(systemPresetSelected, finalSelectedOverrideIds)
        onDismiss()
    }

    AppActionBottomSheet(
        show = show.value,
        modifier = Modifier,
        title = "订阅设置",
        startAction = {
            AppBottomSheetCloseAction(
                onClick = onDismiss,
                contentDescription = MLang.ProfilesPage.Button.Cancel,
            )
        },
        endAction = {
            AppBottomSheetConfirmAction(
                onClick = saveSettings,
                contentDescription = MLang.ProfilesPage.Button.Confirm,
            )
        },
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        enableNestedScroll = true,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val minimumSheetHeight = maxHeight * PROFILE_SETTINGS_MIN_HEIGHT_FRACTION
            val maximumSheetHeight = maxHeight * PROFILE_SETTINGS_MAX_HEIGHT_FRACTION

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minimumSheetHeight, max = maximumSheetHeight)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = MLang.ProfilesPage.Input.ProfileName,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (profile.type == Profile.Type.Url) {
                    TextField(
                        value = editSource,
                        onValueChange = { editSource = it },
                        label = "更改订阅链接",
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                    )
                }

                if (systemPreset != null) {
                    Card {
                        SuperSwitch(
                            title = "预设覆写",
                            summary = "如果不知道什么是分流,推荐打开",
                            checked = systemPresetSelected,
                            onCheckedChange = { systemPresetSelected = it },
                        )
                    }
                }

                if (userConfigs.isNotEmpty()) {
                    Card {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = PROFILE_SETTINGS_LIST_MAX_HEIGHT),
                        ) {
                            itemsIndexed(userConfigs, key = { _, config -> config.id }) { index, config ->
                                val isSelected = config.id in pendingSelectedUserOverrideIds
                                BasicComponent(
                                    title = config.name,
                                    summary = config.description?.takeIf { it.isNotBlank() } ?: "未设置说明",
                                    endActions = {
                                        Checkbox(
                                            state = ToggleableState(isSelected),
                                            onClick = {
                                                toggleUserOverrideSelection(config.id, isSelected)
                                            },
                                        )
                                    },
                                    onClick = {
                                        toggleUserOverrideSelection(config.id, isSelected)
                                    },
                                )
                                if (index < userConfigs.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MiuixTheme.colorScheme.outline.copy(alpha = 0.3f),
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

private fun toggleOverrideIdSelection(
    selectedOverrideIds: List<String>,
    overrideId: String,
    isSelected: Boolean,
): List<String> {
    return if (isSelected) {
        selectedOverrideIds - overrideId
    } else {
        (selectedOverrideIds + overrideId).distinct()
    }
}

private fun buildFinalOverrideIds(
    selectedUserOverrideIds: List<String>,
): List<String> {
    return selectedUserOverrideIds
        .filterNot(::isBuiltinPresetOverrideId)
        .distinct()
}

private fun isBuiltinPresetOverrideId(overrideId: String): Boolean {
    return overrideId.startsWith(SYSTEM_OVERRIDE_PREFIX)
}
