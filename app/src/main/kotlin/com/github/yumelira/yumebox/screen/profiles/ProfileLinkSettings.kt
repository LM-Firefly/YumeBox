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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.ProfileLink
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.WindowBottomSheet
import top.yukonga.miuix.kmp.extra.WindowDropdown
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LinkSettingsDialog(
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
    val openModeOptions = listOf(
        MLang.ProfilesPage.LinkSettings.OpenModeInApp,
        MLang.ProfilesPage.LinkSettings.OpenModeExternal
    )
    val openModeIndex = when (linkOpenMode) {
        LinkOpenMode.IN_APP -> 0
        LinkOpenMode.EXTERNAL_BROWSER -> 1
    }

    val defaultLinkIndex = if (defaultLinkId.isEmpty() || links.isEmpty()) {
        0
    } else {
        links.indexOfFirst { it.id == defaultLinkId }.let { if (it == -1) 0 else it }
    }

    WindowBottomSheet(
        title = MLang.ProfilesPage.LinkSettings.Title, show = show, onDismissRequest = {
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
                WindowDropdown(
                    title = MLang.ProfilesPage.LinkSettings.OpenMode,
                    items = openModeOptions,
                    selectedIndex = openModeIndex,
                    onSelectedIndexChange = { index ->
                        val mode = when (index) {
                            0 -> LinkOpenMode.IN_APP
                            1 -> LinkOpenMode.EXTERNAL_BROWSER
                            else -> LinkOpenMode.IN_APP
                        }
                        onOpenModeChange(mode)
                    })
            }

            // 默认链接选择
            if (links.isNotEmpty()) {
                top.yukonga.miuix.kmp.basic.Card {
                    WindowDropdown(
                        title = MLang.ProfilesPage.LinkSettings.DefaultLink,
                        summary = MLang.ProfilesPage.LinkSettings.DefaultLinkSummary,
                        items = links.map { it.name },
                        selectedIndex = defaultLinkIndex,
                        onSelectedIndexChange = { index ->
                            if (index in links.indices) {
                                onDefaultLinkChange(links[index].id)
                            }
                        })
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
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = link.name, style = MiuixTheme.textStyles.body1
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
                                    onClick = { onDeleteLink(link.id) }) {
                                    Icon(
                                        imageVector = MiuixIcons.Delete,
                                        contentDescription = MLang.Component.ProfileCard.Delete,
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
                    text = MLang.ProfilesPage.LinkSettings.Close,
                    onClick = { show.value = false },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onAddLink,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        MLang.ProfilesPage.LinkSettings.AddLink,
                        color = MiuixTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
internal fun AddLinkDialog(
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
    var currentName by remember { mutableStateOf(linkName) }
    var currentUrl by remember { mutableStateOf(linkUrl) }

    LaunchedEffect(show.value, linkToEdit) {
        if (show.value) {
            if (linkToEdit != null) {
                currentName = linkToEdit.name
                currentUrl = linkToEdit.url
            } else {
                currentName = ""
                currentUrl = ""
            }
            error = ""
        }
    }

    WindowBottomSheet(
        title = if (linkToEdit != null) MLang.ProfilesPage.LinkSettings.EditLink else MLang.ProfilesPage.LinkSettings.AddLink,
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
                value = currentName, onValueChange = {
                    currentName = it
                    error = ""
                }, label = MLang.ProfilesPage.LinkSettings.Name, modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = currentUrl, onValueChange = {
                    currentUrl = it
                    error = ""
                }, label = MLang.ProfilesPage.LinkSettings.Url, modifier = Modifier.fillMaxWidth()
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
                    onClick = onDismiss, modifier = Modifier.weight(1f)
                ) {
                    Text(MLang.ProfilesPage.Button.Cancel)
                }
                Button(
                    onClick = {
                        when {
                            currentName.isBlank() -> error =
                                MLang.ProfilesPage.LinkSettings.Validation.EnterName

                            currentUrl.isBlank() -> error =
                                MLang.ProfilesPage.LinkSettings.Validation.EnterUrl

                            !currentUrl.startsWith("http", ignoreCase = true) -> error =
                                MLang.ProfilesPage.LinkSettings.Validation.InvalidUrl

                            else -> {
                                onNameChange(currentName)
                                onUrlChange(currentUrl)
                                onConfirm()
                            }
                        }
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(MLang.ProfilesPage.Button.Confirm, color = MiuixTheme.colorScheme.surface)
                }
            }
        }
    }
}
