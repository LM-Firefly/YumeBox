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



package com.github.yumelira.yumebox.screen.about

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.R
import com.github.yumelira.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.util.strippedLicenseContent
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Destination<RootGraph>
fun OpenSourceLicensesScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()
    var showLicenseSheet by remember { mutableStateOf(false) }
    var selectedLibrary by remember { mutableStateOf<Library?>(null) }

    BackHandler {
        navigator.popBackStack()
    }

    val libraries by produceLibraries(R.raw.aboutlibraries)
    val libraryItems = remember(libraries) { libraries?.libraries.orEmpty() }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.OpenSourceLicenses.Title,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            val mainLikePadding = rememberStandalonePageMainPadding()
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
            ) {
                if (libraryItems.isNotEmpty()) {
                    items(
                        items = libraryItems,
                        key = { library -> "${library.uniqueId}:${library.artifactId}:${library.name}" },
                    ) { library ->
                        LibraryItem(
                            library = library,
                            onClick = {
                                selectedLibrary = library
                                showLicenseSheet = true
                            },
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            selectedLibrary?.let { library ->
                LicenseBottomSheet(
                    show = showLicenseSheet,
                    library = library,
                    onDismiss = { showLicenseSheet = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryItem(
    library: Library,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.padding(bottom = 12.dp),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = library.name,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                library.artifactVersion?.let { version ->
                    Text(
                        text = version,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }

            library.developers.firstOrNull()?.name?.let { author ->
                Text(
                    text = author,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            if (library.licenses.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    library.licenses.forEach { license ->
                        LicenseChip(licenseName = license.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseChip(licenseName: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = licenseName,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LicenseBottomSheet(
    show: Boolean,
    library: Library,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val licenseContent = remember(library) { library.strippedLicenseContent.takeIf { it.isNotEmpty() } }

    AppActionBottomSheet(
        show = show,
        title = library.name,
        onDismissRequest = onDismiss,
        content = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp)) {
                if (licenseContent != null) {
                    Text(
                        modifier = Modifier.verticalScroll(scrollState),
                        text = licenseContent,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = PaddingValues(16.dp),
                    ) {
                        Text(
                            text = MLang.OpenSourceLicenses.LicenseSheet.NoContent,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    )
}
