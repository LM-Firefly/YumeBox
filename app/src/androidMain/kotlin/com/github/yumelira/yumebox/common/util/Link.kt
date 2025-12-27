package com.github.yumelira.yumebox.common.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri


fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(intent)
}
