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


package com.github.yumelira.yumebox.screen.settings

import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.core.model.ConfigurationOverride
import com.github.yumelira.yumebox.core.model.GeoFileType
import com.github.yumelira.yumebox.core.model.GeoXItem
import com.github.yumelira.yumebox.core.model.geoXItems
import com.github.yumelira.yumebox.presentation.component.*
import com.github.yumelira.yumebox.presentation.viewmodel.OverrideViewModel
import com.github.yumelira.yumebox.screen.navigation.EditorDataHolder
import com.github.yumelira.yumebox.substore.util.DownloadUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.KeyValueEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.StringListEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.DialogDefaults
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.io.FileOutputStream

@Composable
@Destination<RootGraph>
fun MetaFeatureScreen(navigator: DestinationsNavigator) {
    val viewModel: OverrideViewModel = koinViewModel()
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val configuration by viewModel.configuration.collectAsState()
    val showResetDialog = remember { mutableStateOf(false) }
    val showGeoXDownloadSheet = remember { mutableStateOf(false) }

    @Composable
    fun StringInput(
        title: String,
        value: String?,
        placeholder: String = "",
        onValueChange: (String?) -> Unit,
    ) {
        StringInputContent(
            title = title,
            value = value,
            placeholder = placeholder,
            onValueChange = onValueChange,
        )
    }

    var pendingGeoFileType by remember { mutableStateOf<GeoFileType?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        val fileType = pendingGeoFileType ?: return@rememberLauncherForActivityResult
        pendingGeoFileType = null
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                var fileName = "unknown"
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) fileName = it.getString(nameIndex)
                    }
                }

                val ext = "." + fileName.substringAfterLast(".")
                val validExtensions = listOf(".metadb", ".db", ".dat", ".mmdb", ".bin")
                if (ext !in validExtensions) {
                    Toast.makeText(
                        context,
                        MLang.MetaFeature.Message.UnsupportedFormat.format(validExtensions.joinToString("/")),
                        Toast.LENGTH_LONG,
                    ).show()
                    return@launch
                }

                val outputFileName = when (fileType) {
                    GeoFileType.GeoIP -> "geoip$ext"
                    GeoFileType.GeoSite -> "geosite$ext"
                    GeoFileType.Country -> "country$ext"
                    GeoFileType.ASN -> "ASN$ext"
                    GeoFileType.Model -> "Model.bin"
                }

                withContext(Dispatchers.IO) {
                    val clashDir = context.filesDir.resolve("clash")
                    clashDir.mkdirs()
                    val outputFile = File(clashDir, outputFileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(outputFile).use { output -> input.copyTo(output) }
                    }
                }

                Toast.makeText(
                    context,
                    MLang.MetaFeature.Message.Imported.format(fileName),
                    Toast.LENGTH_SHORT,
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    MLang.MetaFeature.Message.ImportFailed.format(e.message ?: MLang.Util.Error.UnknownError),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.MetaFeature.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(modifier = Modifier.padding(end = 24.dp), onClick = { showResetDialog.value = true }) {
                        Icon(MiuixIcons.Reset, contentDescription = MLang.Component.Navigation.Refresh)
                    }
                },
            )
        },
    ) { innerPadding ->
        ScreenLazyColumn(scrollBehavior = scrollBehavior, innerPadding = innerPadding) {
            item {
                SmallTitle(MLang.MetaFeature.Section.CoreSettings)
                Card {
                    NullableBooleanSelector(
                        title = MLang.MetaFeature.Core.UnifiedDelayTitle,
                        summary = MLang.MetaFeature.Core.UnifiedDelaySummary,
                        value = configuration.unifiedDelay,
                        onValueChange = viewModel::setUnifiedDelay,
                    )
                    NullableBooleanSelector(
                        title = MLang.MetaFeature.Core.GeodataModeTitle,
                        summary = MLang.MetaFeature.Core.GeodataModeSummary,
                        value = configuration.geodataMode,
                        onValueChange = viewModel::setGeodataMode,
                    )
                    NullableBooleanSelector(
                        title = MLang.MetaFeature.Core.TcpConcurrentTitle,
                        summary = MLang.MetaFeature.Core.TcpConcurrentSummary,
                        value = configuration.tcpConcurrent,
                        onValueChange = viewModel::setTcpConcurrent,
                    )
                    NullableEnumSelector(
                        title = MLang.MetaFeature.Core.FindProcessModeTitle,
                        value = configuration.findProcessMode,
                        items = listOf(
                            MLang.MetaFeature.Core.FindProcessNotModify,
                            MLang.MetaFeature.Core.FindProcessOff,
                            MLang.MetaFeature.Core.FindProcessStrict,
                            MLang.MetaFeature.Core.FindProcessAlways,
                        ),
                        values = listOf(
                            null,
                            ConfigurationOverride.FindProcessMode.Off,
                            ConfigurationOverride.FindProcessMode.Strict,
                            ConfigurationOverride.FindProcessMode.Always,
                        ),
                        onValueChange = viewModel::setFindProcessMode,
                    )
                }
            }

            item {
                SmallTitle(MLang.Override.Section.AuthHosts)
                Card {
                    MetaStringListInput(
                        title = MLang.Override.AuthHosts.Authentication,
                        value = configuration.authentication,
                        placeholder = MLang.Override.AuthHosts.AuthenticationHint,
                        navigator = navigator,
                        onValueChange = viewModel::setAuthentication,
                    )
                    MetaStringMapInput(
                        title = MLang.Override.AuthHosts.HostsMapping,
                        value = configuration.hosts,
                        keyPlaceholder = MLang.Override.AuthHosts.HostsKeyHint,
                        valuePlaceholder = MLang.Override.AuthHosts.HostsValueHint,
                        navigator = navigator,
                        onValueChange = viewModel::setHosts,
                    )
                }
            }

            item {
                SmallTitle(MLang.Override.Section.ExternalController)
                Card {
                    StringInput(
                        title = MLang.Override.ExternalController.Address,
                        value = configuration.externalController,
                        placeholder = MLang.Override.ExternalController.AddressHint,
                        onValueChange = viewModel::setExternalController,
                    )
                    StringInput(
                        title = MLang.Override.ExternalController.ApiSecret,
                        value = configuration.secret,
                        placeholder = MLang.Override.ExternalController.ApiSecretHint,
                        onValueChange = viewModel::setSecret,
                    )
                    AnimatedVisibility(
                        visible = !configuration.externalController.isNullOrBlank(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        androidx.compose.foundation.layout.Column {
                            StringInput(
                                title = MLang.Override.ExternalController.Tls,
                                value = configuration.externalControllerTLS,
                                placeholder = MLang.Override.ExternalController.TlsHint,
                                onValueChange = viewModel::setExternalControllerTLS,
                            )
                            MetaStringListInput(
                                title = MLang.Override.ExternalController.CorsAllowOrigins,
                                value = configuration.externalControllerCors.allowOrigins,
                                placeholder = MLang.Override.ExternalController.CorsAllowOriginsHint,
                                navigator = navigator,
                                onValueChange = viewModel::setExternalControllerCorsAllowOrigins,
                            )
                            NullableBooleanSelector(
                                title = MLang.Override.ExternalController.CorsAllowPrivate,
                                value = configuration.externalControllerCors.allowPrivateNetwork,
                                onValueChange = viewModel::setExternalControllerCorsAllowPrivateNetwork,
                            )
                        }
                    }
                }
            }

            item {
                SmallTitle(MLang.MetaFeature.Section.GeoXFiles)
                Card {
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.ImportGeoipTitle,
                        summary = MLang.MetaFeature.GeoX.ImportGeoipSummary,
                        onClick = {
                            pendingGeoFileType = GeoFileType.GeoIP
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.ImportGeositeTitle,
                        summary = MLang.MetaFeature.GeoX.ImportGeositeSummary,
                        onClick = {
                            pendingGeoFileType = GeoFileType.GeoSite
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.ImportCountryTitle,
                        summary = MLang.MetaFeature.GeoX.ImportCountrySummary,
                        onClick = {
                            pendingGeoFileType = GeoFileType.Country
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.ImportAsnTitle,
                        summary = MLang.MetaFeature.GeoX.ImportAsnSummary,
                        onClick = {
                            pendingGeoFileType = GeoFileType.ASN
                            filePickerLauncher.launch("*/*")
                        },
                    )
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.ImportModelTitle,
                        summary = MLang.MetaFeature.GeoX.ImportModelSummary,
                        onClick = {
                            pendingGeoFileType = GeoFileType.Model
                            filePickerLauncher.launch("*/*")
                        },
                    )


                }
            }
            item {
                SmallTitle(MLang.MetaFeature.GeoX.OnlineUpdateTitle)
                Card {
                    SuperArrow(
                        title = MLang.MetaFeature.GeoX.OnlineUpdateTitle,
                        summary = MLang.MetaFeature.GeoX.OnlineUpdateSummary,
                        onClick = { showGeoXDownloadSheet.value = true },
                    )
                }
            }
        }
    }

    ConfirmDialog(
        show = showResetDialog,
        title = MLang.MetaFeature.ResetDialog.Title,
        message = MLang.MetaFeature.ResetDialog.Message,
        onConfirm = {
            viewModel.resetConfiguration()
            showResetDialog.value = false
        },
        onDismiss = { showResetDialog.value = false },
    )

    GeoXDownloadSheet(
        show = showGeoXDownloadSheet,
        context = context,
        scope = scope,
    )
}

@Composable
private fun GeoXDownloadSheet(
    show: MutableState<Boolean>,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val selectedItems = remember { mutableStateMapOf<GeoFileType, Boolean>() }

    WindowDialog(
        show = show.value,
        modifier = Modifier,
        title = MLang.MetaFeature.Download.DialogTitle,
        titleColor = DialogDefaults.titleColor(),
        summary = null,
        enableWindowDim = true,
        onDismissRequest = { show.value = false },
        content = {
            Column {
                geoXItems.forEach { item ->
                    BasicComponent(
                        title = item.title,
                        endActions = {
                            Checkbox(
                                state = ToggleableState(selectedItems[item.type] ?: false),
                                onClick = {
                                    selectedItems[item.type] = !(selectedItems[item.type] ?: false)
                                }
                            )
                        },
                        onClick = {
                            selectedItems[item.type] = !(selectedItems[item.type] ?: false)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { show.value = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(MLang.MetaFeature.Download.Cancel)
                    }
                    Button(
                        onClick = {
                            val itemsToDownload = geoXItems.filter { selectedItems[it.type] == true }
                            if (itemsToDownload.isEmpty()) {
                                Toast.makeText(context, MLang.MetaFeature.Download.SelectFiles, Toast.LENGTH_SHORT)
                                    .show()
                                return@Button
                            }
                            show.value = false
                            downloadGeoXFiles(context, scope, itemsToDownload)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(MLang.MetaFeature.Download.Download, color = MiuixTheme.colorScheme.background)
                    }
                }
            }
        })
}

private fun downloadGeoXFiles(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    items: List<GeoXItem>,
) {
    scope.launch {
        var successCount = 0
        withContext(Dispatchers.IO) {
            val clashDir = context.filesDir.resolve("clash")
            clashDir.mkdirs()
            items.forEach { item ->
                val targetFile = File(clashDir, item.fileName)
                if (DownloadUtil.download(item.url, targetFile)) {
                    successCount++
                }
            }
        }
        Toast.makeText(
            context,
            MLang.MetaFeature.Download.DownloadComplete.format(successCount, items.size),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
private fun MetaStringListInput(
    title: String,
    value: List<String>?,
    placeholder: String = "",
    navigator: DestinationsNavigator,
    onValueChange: (List<String>?) -> Unit,
) {
    StringListInputContent(
        title = title,
        value = value,
        onClick = {
            EditorDataHolder.setupListEditor(
                title = title,
                placeholder = placeholder,
                items = value,
                callback = onValueChange,
            )
            navigator.navigate(StringListEditorScreenDestination)
        },
    )
}

@Composable
private fun MetaStringMapInput(
    title: String,
    value: Map<String, String>?,
    keyPlaceholder: String = MLang.Component.ConfigInput.KeyPlaceholder,
    valuePlaceholder: String = MLang.Component.ConfigInput.ValuePlaceholder,
    navigator: DestinationsNavigator,
    onValueChange: (Map<String, String>?) -> Unit,
) {
    StringMapInputContent(
        title = title,
        value = value,
        onClick = {
            EditorDataHolder.setupMapEditor(
                title = title,
                keyPlaceholder = keyPlaceholder,
                valuePlaceholder = valuePlaceholder,
                items = value,
                callback = onValueChange,
            )
            navigator.navigate(KeyValueEditorScreenDestination)
        },
    )
}
