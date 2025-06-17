package com.github.yumelira.yumebox.presentation.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun WindowBlurEffect(useBlur: Boolean, blurRadius: Int = 30) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val view = LocalView.current
    val activity = view.context.findActivity() ?: return
    val window = activity.window

    SideEffect {
        if (useBlur) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = blurRadius
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = 0
        }
        window.attributes = window.attributes
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
