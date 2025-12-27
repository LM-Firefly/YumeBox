package com.github.yumelira.yumebox.presentation.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.github.yumelira.yumebox.presentation.webview.WebViewActivity
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.Text

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LocalWebView(
    initialUrl: String,
    modifier: Modifier = Modifier,
    enableDebug: Boolean = true,
    applyStatusBarPadding: Boolean = true,
    onPageFinished: (String) -> Unit = {},
    onPageError: (String, String) -> Unit = { _, _ -> },
    onShowFileChooser: ((ValueCallback<Array<Uri>>?, Array<String>) -> Unit)? = null,
) {
    LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(enableDebug) {
        if (enableDebug) {
            try {
                WebView.setWebContentsDebuggingEnabled(true)
            } catch (_: Exception) {
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webViewRef.value?.onPause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    webViewRef.value?.onResume()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef.value?.let { webView ->
                webView.stopLoading()
                webView.onPause()
                webView.visibility = View.GONE
                webView.removeAllViews()
                (webView.parent as? ViewGroup)?.removeView(webView)
                Handler(Looper.getMainLooper()).postDelayed({
                    webView.destroy()
                }, 300)
            }
            webViewRef.value = null
        }
    }

    if (initialUrl.isEmpty()) {
        Box(
            modifier = if (applyStatusBarPadding) modifier.statusBarsPadding() else modifier, contentAlignment = Alignment.Center
        ) {
            Text(MLang.Component.WebView.InvalidUrl)
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            createWebView(ctx, initialUrl, onPageFinished, onPageError, onShowFileChooser).also {
                webViewRef.value = it
            }
        },
        modifier = if (applyStatusBarPadding) modifier.statusBarsPadding() else modifier,
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    initialUrl: String,
    onPageFinished: (String) -> Unit,
    onPageError: (String, String) -> Unit,
    onShowFileChooser: ((ValueCallback<Array<Uri>>?, Array<String>) -> Unit)?,
): WebView {
    val activity = context as? WebViewActivity

    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true

            allowFileAccess = true
            allowContentAccess = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true


            setSupportZoom(true)
            builtInZoomControls = false
            displayZoomControls = false

            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false

            cacheMode = WebSettings.LOAD_DEFAULT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val scheme = request.url?.scheme
                
                // 处理自定义 scheme (非 http/https)
                if (scheme != null && scheme != "http" && scheme != "https" && scheme != "file") {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, request.url)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return true
                    }
                }
                
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onPageFinished(url ?: "")
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                @Suppress("DEPRECATION") super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val errorUrl = request.url?.toString() ?: "unknown"
                    val errorCode = error?.errorCode ?: -1
                    val errorMessage = error?.description?.toString() ?: "unknown error"
                    onPageError(errorUrl, "Error $errorCode: $errorMessage")
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?,
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    val errorUrl = request.url?.toString() ?: "unknown"
                    val statusCode = errorResponse?.statusCode ?: -1
                    if (statusCode == 404) {
                        onPageError(errorUrl, "404 Not Found")
                    }
                }
            }

            @Suppress("DEPRECATION")
            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?,
            ) {
                @Suppress("DEPRECATION") super.onReceivedError(view, errorCode, description, failingUrl)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    onPageError(failingUrl ?: "unknown", "Error $errorCode: $description")
                }
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return true
            }

            override fun onShowFileChooser(
                webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?
            ): Boolean {
                val mimeTypes = fileChooserParams?.acceptTypes ?: arrayOf("*/*")
                if (onShowFileChooser != null) {
                    onShowFileChooser(filePathCallback, mimeTypes)
                    return true
                }
                if (activity == null) {
                    filePathCallback?.onReceiveValue(null)
                    return true
                }
                activity.launchFilePicker(filePathCallback, mimeTypes)
                return true
            }
        }

        loadUrl(initialUrl)
    }
}