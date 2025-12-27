package com.github.yumelira.yumebox.common.util

import android.content.Context
import android.content.Intent
import android.net.VpnService

object VpnUtils {

    fun checkVpnPermission(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    fun getVpnPermissionIntent(context: Context): Intent? {
        return VpnService.prepare(context)
    }
}