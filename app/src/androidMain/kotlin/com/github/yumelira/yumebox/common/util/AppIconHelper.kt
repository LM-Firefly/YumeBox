package com.github.yumelira.yumebox.common.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber

object AppIconHelper {
    private const val MAIN_ACTIVITY_ALIAS = "com.github.yumelira.yumebox.MainActivityAlias"

    fun hideIcon(context: Context) {
        runCatching {
            val componentName = ComponentName(context.packageName, MAIN_ACTIVITY_ALIAS)
            val currentState = context.packageManager.getComponentEnabledSetting(componentName)
            if (currentState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                return
            }

            val mainActivityComponent =
                ComponentName(context.packageName, "com.github.yumelira.yumebox.MainActivity")
            context.packageManager.setComponentEnabledSetting(
                mainActivityComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )

            context.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }.onFailure { e ->
            Timber.w(e, "Failed to hide app icon")
        }
    }

    fun showIcon(context: Context) {
        runCatching {
            val componentName = ComponentName(context.packageName, MAIN_ACTIVITY_ALIAS)
            val currentState = context.packageManager.getComponentEnabledSetting(componentName)
            if (currentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || currentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                return
            }

            val mainActivityComponent =
                ComponentName(context.packageName, "com.github.yumelira.yumebox.MainActivity")
            context.packageManager.setComponentEnabledSetting(
                mainActivityComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )

            context.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }.onFailure { e ->
            Timber.w(e, "Failed to show app icon")
        }
    }

    fun toggleIcon(context: Context, hide: Boolean) {
        if (hide) {
            hideIcon(context)
        } else {
            showIcon(context)
        }
    }
}