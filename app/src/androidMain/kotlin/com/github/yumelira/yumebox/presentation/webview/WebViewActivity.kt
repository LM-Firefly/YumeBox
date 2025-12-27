package com.github.yumelira.yumebox.presentation.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

class WebViewActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_INITIAL_URL = "initial_url"
        private const val EXTRA_TITLE = "title"

        fun start(
            context: Context,
            initialUrl: String = "file://${context.filesDir}/frontend/index.html",
            title: String? = null,
        ) {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_URL, initialUrl)
                if (title != null) {
                    putExtra(EXTRA_TITLE, title)
                }
            }
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris?.toTypedArray())
        filePathCallback = null
    }

    fun launchFilePicker(
        callback: ValueCallback<Array<Uri>>?,
        mimeTypes: Array<String>
    ) {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        val types = if (mimeTypes.isEmpty() || (mimeTypes.size == 1 && mimeTypes[0].isEmpty())) {
            arrayOf("*/*")
        } else {
            mimeTypes
        }
        fileChooserLauncher.launch(types)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        val initialUrl = intent.getStringExtra(EXTRA_INITIAL_URL) ?: "file://${filesDir}/frontend/index.html"
        val title = intent.getStringExtra(EXTRA_TITLE)

        setContent {
            WebViewScreen(initialUrl = initialUrl, title = title)
        }
    }
}
