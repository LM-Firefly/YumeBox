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
 * Copyright (c)  YumeLira 2025.
 *
 */

package com.github.yumelira.yumebox.presentation.webview

import android.app.Activity
import android.net.Uri
import android.webkit.ValueCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumelira.yumebox.presentation.theme.YumeTheme
import com.github.yumelira.yumebox.presentation.viewmodel.AppSettingsViewModel
import com.github.yumelira.yumebox.presentation.webview.LocalWebView
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
@Destination<RootGraph>
fun WebViewScreen(
    initialUrl: String,
    title: String? = null,
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator? = null,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
    val colorTheme = appSettingsViewModel.colorTheme.state.collectAsState().value

    var webViewError by remember { mutableStateOf<String?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    val backAction = {
        if (navigator != null) {
            navigator.popBackStack()
        } else {
            activity?.finish()
        }
    }

    BackHandler {
        backAction()
    }

    ProvideAndroidPlatformTheme {
        YumeTheme(
            themeMode = themeMode,
            colorTheme = colorTheme
        ) {
            if (title != null) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        SmallTopAppBar(
                            title = title,
                            navigationIcon = {
                                val interactionSource = remember { MutableInteractionSource() }
                                val pressed by interactionSource.collectIsPressedAsState()
                                val scale by animateFloatAsState(
                                    targetValue = if (pressed) 0.92f else 1f,
                                    animationSpec = com.github.yumelira.yumebox.presentation.theme.AnimationSpecs.ButtonPress,
                                    label = "back_icon_scale",
                                )
                                val alpha by animateFloatAsState(
                                    targetValue = if (pressed) 0.05f else 0f,
                                    animationSpec = com.github.yumelira.yumebox.presentation.theme.AnimationSpecs.ButtonPress,
                                    label = "back_icon_alpha",
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(start = 24.dp)
                                        .size(32.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .background(
                                            color = MiuixTheme.colorScheme.onBackground.copy(alpha = alpha),
                                            shape = CircleShape
                                        )
                                        .semantics { role = Role.Button }
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            onClick = { backAction() }
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Back,
                                        contentDescription = MLang.Component.Navigation.Back,
                                        tint = MiuixTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = modifier.padding(innerPadding)) {
                        WebViewContent(
                            initialUrl = initialUrl,
                            webViewError = webViewError,
                            applyStatusBarPadding = false,
                            onError = { webViewError = it },
                            onShowFileChooser = { callback, mimeTypes ->
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = callback
                                val types = if (mimeTypes.isEmpty() || (mimeTypes.size == 1 && mimeTypes[0].isEmpty())) {
                                    arrayOf("*/*")
                                } else {
                                    mimeTypes
                                }
                                fileChooserLauncher.launch(types)
                            }
                        )
                    }
                }
            } else {
                Box(modifier = modifier) {
                    WebViewContent(
                        initialUrl = initialUrl,
                        webViewError = webViewError,
                        applyStatusBarPadding = true,
                        onError = { webViewError = it },
                        onShowFileChooser = { callback, mimeTypes ->
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = callback
                            val types = if (mimeTypes.isEmpty() || (mimeTypes.size == 1 && mimeTypes[0].isEmpty())) {
                                arrayOf("*/*")
                            } else {
                                mimeTypes
                            }
                            fileChooserLauncher.launch(types)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WebViewContent(
    initialUrl: String,
    webViewError: String?,
    applyStatusBarPadding: Boolean,
    onError: (String) -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>?, Array<String>) -> Unit
) {
    if (webViewError != null) {
        Text(
            text = webViewError,
            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
            textAlign = TextAlign.Center,
        )
    } else if (initialUrl.isNotEmpty()) {
        LocalWebView(
            initialUrl = initialUrl,
            modifier = Modifier.fillMaxSize(),
            enableDebug = true,
            applyStatusBarPadding = applyStatusBarPadding,
            onPageFinished = { url: String ->
            },
            onPageError = { url: String, error: String ->
                if (url.endsWith("index.html") && (error.contains("404") || error.contains("Not Found"))) {
                    onError(MLang.Component.WebView.LoadFailed)
                }
            },
            onShowFileChooser = onShowFileChooser
        )
    } else {
        Text(
            text = MLang.Component.WebView.InvalidUrl,
            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
            textAlign = TextAlign.Center,
        )
    }
}
