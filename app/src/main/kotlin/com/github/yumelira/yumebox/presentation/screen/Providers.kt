package com.github.yumelira.yumebox.presentation.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.github.yumelira.yumebox.core.model.Provider
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.CenteredText
import com.github.yumelira.yumebox.presentation.component.NavigationBackIcon
import com.github.yumelira.yumebox.presentation.component.RotatingRefreshButton
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.SmallTitle
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.viewmodel.ProvidersViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Destination<RootGraph>
fun ProvidersScreen(navigator: DestinationsNavigator) {
    val viewModel = koinViewModel<ProvidersViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current

    val providers by viewModel.providers.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isRunning) {
        if (isRunning) {
            viewModel.refreshProviders()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = MLang.Providers.Title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    NavigationBackIcon(navigator = navigator)
                },
                actions = {
                    if (isRunning && providers.any { it.vehicleType == Provider.VehicleType.HTTP }) {
                        RotatingRefreshButton(
                            isRotating = uiState.isUpdatingAll,
                            onClick = { viewModel.updateAllProviders() },
                            modifier = Modifier.padding(end = 24.dp),
                            contentDescription = MLang.Providers.Action.UpdateAll
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        if (!isRunning) {
            CenteredText(
                firstLine = MLang.Providers.Empty.NotRunning,
                secondLine = MLang.Providers.Empty.NotRunningHint
            )
        } else if (providers.isEmpty() && !uiState.isLoading) {
            CenteredText(
                firstLine = MLang.Providers.Empty.NoProviders,
                secondLine = MLang.Providers.Empty.NoProvidersHint
            )
        } else {
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = innerPadding,
            ) {
                val proxyProviders = providers.filter { it.type == Provider.Type.Proxy }
                val ruleProviders = providers.filter { it.type == Provider.Type.Rule }

                if (proxyProviders.isNotEmpty()) {
                    item {
                        SmallTitle(MLang.Providers.Type.ProxyProviders.format(proxyProviders.size))
                    }
                    proxyProviders.forEach { provider ->
                        val providerKey = "${provider.type}_${provider.name}"
                        item(key = providerKey) {
                            ProviderCard(
                                provider = provider,
                                isUpdating = uiState.updatingProviders.contains(providerKey),
                                onUpdate = { viewModel.updateProvider(provider) },
                                onUpload = { uri -> viewModel.uploadProviderFile(context, provider, uri) }
                            )
                        }
                    }
                }

                if (ruleProviders.isNotEmpty()) {
                    item {
                        SmallTitle(MLang.Providers.Type.RuleProviders.format(ruleProviders.size))
                    }
                    ruleProviders.forEach { provider ->
                        val providerKey = "${provider.type}_${provider.name}"
                        item(key = providerKey) {
                            ProviderCard(
                                provider = provider,
                                isUpdating = uiState.updatingProviders.contains(providerKey),
                                onUpdate = { viewModel.updateProvider(provider) },
                                onUpload = { uri -> viewModel.uploadProviderFile(context, provider, uri) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: Provider,
    isUpdating: Boolean,
    onUpdate: () -> Unit,
    onUpload: (Uri) -> Unit
) {
    val context = LocalContext.current
    val showPopup = remember { mutableStateOf(false) }
    val colorScheme = MiuixTheme.colorScheme
    val updateBg = remember(colorScheme) { colorScheme.tertiaryContainer.copy(alpha = 0.6f) }
    val updateTint = remember(colorScheme) { colorScheme.onTertiaryContainer.copy(alpha = 0.8f) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onUpload(it) }
    }

    Card(modifier = Modifier.padding(vertical = 4.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = provider.name,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    if (provider.count > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = provider.count.toString(),
                                style = MiuixTheme.textStyles.footnote1,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                if (provider.path.isNotBlank()) {
                    Box {
                        IconButton(
                            backgroundColor = updateBg,
                            minHeight = 35.dp,
                            minWidth = 35.dp,
                            enabled = !isUpdating,
                            onClick = { showPopup.value = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    imageVector = MiuixIcons.Edit,
                                    tint = updateTint,
                                    contentDescription = MLang.Providers.Action.Operation,
                                )
                                Text(
                                    modifier = Modifier.padding(end = 3.dp),
                                    text = MLang.Providers.Action.Operation,
                                    color = updateTint,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        val items = listOf(
                            Triple(MLang.Providers.Action.Update, MiuixIcons.Update, 0),
                            Triple(MLang.Providers.Action.Import, MiuixIcons.Download, 1),
                            Triple(MLang.Providers.Action.View, MiuixIcons.Show, 2)
                        )
                        var selectedIndex by remember { mutableStateOf(0) }
                        WindowListPopup(
                            show = showPopup,
                            popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                            alignment = PopupPositionProvider.Align.End,
                            minWidth = 0.dp,
                            onDismissRequest = { showPopup.value = false }
                        ) {
                            ListPopupColumn {
                                items.forEach { (text, icon, index) ->
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val contentColor = if (isPressed) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                                    Row(
                                        modifier = Modifier
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = androidx.compose.foundation.LocalIndication.current
                                            ) {
                                                showPopup.value = false
                                                when (index) {
                                                    0 -> onUpdate()
                                                    1 -> filePicker.launch("*/*")
                                                    2 -> {
                                                        try {
                                                            val file = File(provider.path)
                                                            val uri = FileProvider.getUriForFile(
                                                                context,
                                                                "${context.packageName}.fileprovider",
                                                                file
                                                            )
                                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                setDataAndType(uri, "text/plain")
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            val chooser = Intent.createChooser(intent, MLang.Providers.Action.View)
                                                            context.startActivity(chooser)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, MLang.Providers.Message.OpenFileFailed.format(e.message ?: ""), Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 20.dp, vertical = 12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = text,
                                            tint = contentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = text,
                                            style = MiuixTheme.textStyles.body1,
                                            color = contentColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val subInfo = provider.subscriptionInfo
            val percentageText = if (subInfo != null && subInfo.total > 0) {
                val used = subInfo.upload.toULong() + subInfo.download.toULong()
                val total = subInfo.total.toULong()
                val fraction = if (total > 0uL) used.toDouble() / total.toDouble() else 0.0
                String.format(Locale.getDefault(), "%.2f%%", fraction * 100.0)
            } else null
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = provider.vehicleType.name,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    if (provider.updatedAt > 0) {
                        Text(
                            text = "•",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Text(
                            text = formatTimestamp(provider.updatedAt),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
                if (percentageText != null) {
                    Text(
                        text = percentageText,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
            provider.subscriptionInfo?.let { info ->
                val total = info.total.toULong()
                if (total > 0uL) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val upload = info.upload.toULong()
                    val download = info.download.toULong()
                    val used = upload + download
                    val remaining = if (total > used) total - used else 0uL
                    val fraction = if (total > 0uL) used.toDouble() / total.toDouble() else 0.0
                    val percentageInt = (fraction * 100.0).roundToInt()
                    val progressColor = when {
                        percentageInt >= 90 -> MiuixTheme.colorScheme.error
                        percentageInt >= 70 -> Color(0xFFFFC107)
                        else -> Color(0xFF4CAF50)
                    }
                    val overlayColor = progressColor.copy(alpha = 0.12f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(overlayColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(progressColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = MLang.Providers.Info.RemainingTraffic.format(formatBytes(remaining)),
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Text(
                            text = MLang.Providers.Info.UsedTraffic.format(formatBytes(used), formatBytes(total)),
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
                if (info.expire > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val expireDate = Date(info.expire * 1000)
                    val now = Date()
                    val diff = info.expire * 1000 - now.time
                    val days = diff / (1000 * 60 * 60 * 24)
                    val expireText = if (diff > 0) {
                        MLang.Providers.Info.ExpireDays.format(days)
                    } else {
                        MLang.Providers.Info.Expired
                    }
                    Text(
                        text = "${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(expireDate)} ($expireText)",
                        style = MiuixTheme.textStyles.footnote1,
                        color = if (diff > 0) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}

private fun formatBytes(bytes: ULong): String {
    if (bytes <= 0uL) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val index = digitGroups.coerceIn(0, units.lastIndex)
    return String.format(Locale.getDefault(), "%.2f %s", bytes.toDouble() / Math.pow(1024.0, index.toDouble()), units[index])
}
