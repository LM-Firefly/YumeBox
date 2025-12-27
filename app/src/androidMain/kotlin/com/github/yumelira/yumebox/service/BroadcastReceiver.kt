package com.github.yumelira.yumebox.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.worker.AutoRestartWorker

class DialerReceiver : BroadcastReceiver() {

    companion object {
        private const val SECRET_CODE = "*#*#0721#*#*"
    }

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "android.provider.Telephony.SECRET_CODE" -> {
                startMainActivity(context)
            }

            Intent.ACTION_NEW_OUTGOING_CALL -> {
                val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                if (SECRET_CODE == phoneNumber) {
                    setResultData(null)
                    startMainActivity(context)
                }
            }
        }
    }

    private fun startMainActivity(context: Context) {
        try {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(launchIntent)
        } catch (e: Exception) {
        }
    }
}

class RestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
                -> {
                val request = OneTimeWorkRequestBuilder<AutoRestartWorker>().build()
                WorkManager.getInstance(context).enqueue(request)
            }
        }
    }
}
