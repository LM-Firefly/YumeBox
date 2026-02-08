package com.github.yumelira.yumebox.service.util

import android.content.ComponentName
import com.github.yumelira.yumebox.service.common.Global

/**
 * Component name utilities for YumeBox service
 */

/**
 * Get ComponentName for Clash service
 */
fun clashServiceComponent(): ComponentName {
    return ComponentName(
        Global.application.packageName,
        "com.github.yumelira.yumebox.service.ClashService"
    )
}

/**
 * Get ComponentName for TUN service
 */
fun tunServiceComponent(): ComponentName {
    return ComponentName(
        Global.application.packageName,
        "com.github.yumelira.yumebox.service.TunService"
    )
}

