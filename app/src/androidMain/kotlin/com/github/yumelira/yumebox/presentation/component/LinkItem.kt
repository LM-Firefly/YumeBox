package com.github.yumelira.yumebox.presentation.component

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.github.yumelira.yumebox.common.util.openUrl
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.extra.SuperArrow

@Composable
fun LinkItem(
    title: String,
    url: String,
    showArrow: Boolean = false,
    context: Context = LocalContext.current,
) {
    if (showArrow) {
        SuperArrow(
            title = title,
            summary = url,
            onClick = { openUrl(context, url) }
        )
    } else {
        BasicComponent(
            title = title,
            summary = url,
            onClick = { openUrl(context, url) }
        )
    }
}
