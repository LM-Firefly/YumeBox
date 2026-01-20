package com.github.yumelira.yumebox.presentation.webview

import android.app.Activity
import android.net.Uri
import android.webkit.ValueCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                                IconButton(
                                    modifier = Modifier.padding(start = 24.dp),
                                    onClick = { backAction() }
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
