package com.github.yumelira.yumebox.common.util

import android.os.Build

object DeviceUtil {

    fun is32BitDevice(): Boolean {
        val supportedABIs = Build.SUPPORTED_ABIS
        return supportedABIs.isNotEmpty() && supportedABIs.all { abi ->
            abi.contains("armeabi-v7a") || abi.contains("x86")
        }
    }

    fun getPreferredAbi(): String {
        return when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "arm64-v8a"
            "x86_64" -> "x86_64"
            "armeabi-v7a" -> "armeabi-v7a"
            "x86" -> "x86"
            else -> "arm64-v8a"
        }
    }
}