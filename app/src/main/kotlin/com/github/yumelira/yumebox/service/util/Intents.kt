package com.github.yumelira.yumebox.service.util

import android.content.Intent
import com.github.yumelira.yumebox.service.common.Constants
import com.github.yumelira.yumebox.service.common.Global
import java.util.UUID

/**
 * Intent builder utilities for YumeBox service
 */

/**
 * Create intent to start Clash service
 */
fun startClashIntent(): Intent {
    return Intent(Constants.ACTION_START_CLASH).apply {
        setPackage(Global.application.packageName)
    }
}

/**
 * Create intent to stop Clash service
 */
fun stopClashIntent(): Intent {
    return Intent(Constants.ACTION_STOP_CLASH).apply {
        setPackage(Global.application.packageName)
    }
}

/**
 * Create intent to toggle Clash service
 */
fun toggleClashIntent(): Intent {
    return Intent(Constants.ACTION_TOGGLE_CLASH).apply {
        setPackage(Global.application.packageName)
    }
}

/**
 * Create profile changed broadcast intent
 */
fun profileChangedIntent(uuid: UUID): Intent {
    return Intent(Constants.ACTION_PROFILE_CHANGED).apply {
        setPackage(Global.application.packageName)
        putExtra(Constants.EXTRA_UUID, uuid.toString())
    }
}

/**
 * Create override changed broadcast intent
 */
fun overrideChangedIntent(): Intent {
    return Intent(Constants.ACTION_OVERRIDE_CHANGED).apply {
        setPackage(Global.application.packageName)
    }
}
