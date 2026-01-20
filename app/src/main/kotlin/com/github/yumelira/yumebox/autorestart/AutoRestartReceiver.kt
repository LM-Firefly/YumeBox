package com.github.yumelira.yumebox.autorestart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class AutoRestartReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AutoRestartReceiver"
    }
    override fun onReceive(context: Context, intent: Intent) {
        Timber.tag(TAG).i("接收到广播: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Timber.tag(TAG).d("触发自动重启服务")
                AutoRestartService.start(context)
            }
            else -> {
                Timber.tag(TAG).w("未知的广播: ${intent.action}")
            }
        }
    }
}
