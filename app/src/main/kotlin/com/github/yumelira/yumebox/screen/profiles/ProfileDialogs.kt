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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.service.runtime.entity.Profile
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun EditProfileNameDialog(
    show: MutableState<Boolean>,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editName by remember { mutableStateOf(currentName) }

    WindowDialog(
        title = MLang.ProfilesPage.EditDialog.Title, show = show, onDismissRequest = onDismiss
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
                        MLang.ProfilesPage.Button.Confirm, color = MiuixTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
internal fun DeleteConfirmDialog(
    profileName: String, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    WindowDialog(
        title = MLang.ProfilesPage.DeleteDialog.Title,
        summary = MLang.ProfilesPage.DeleteDialog.Message.format(profileName),
        show = remember { mutableStateOf(true) },
        onDismissRequest = onDismiss
    ) {
        DialogButtonRow(
            onCancel = onDismiss,
            onConfirm = onConfirm,
            cancelText = MLang.ProfilesPage.Button.Cancel,
            confirmText = MLang.ProfilesPage.DeleteDialog.Confirm
        )
    }
}

@Composable
internal fun ShareOptionsDialog(
    show: MutableState<Boolean>,
    profile: Profile,
    onDismiss: () -> Unit,
    onShareFile: (Profile) -> Unit,
    onShareLink: (Profile) -> Unit
) {
    WindowDialog(
        title = MLang.ProfilesPage.ShareDialog.Title, show = show, onDismissRequest = onDismiss
    ) {
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
                        color = MiuixTheme.colorScheme.surface
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
    }
}
