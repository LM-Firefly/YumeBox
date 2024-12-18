package com.github.yumelira.yumebox.service.common.constants

import android.content.ComponentName
import com.github.yumelira.yumebox.service.common.util.packageName

object Components {
    private const val componentsPackageName = "com.github.yumelira.yumebox"

    val MAIN_ACTIVITY = ComponentName(packageName, "$componentsPackageName.MainActivity")
    val PROPERTIES_ACTIVITY = ComponentName(packageName, "$componentsPackageName.PropertiesActivity")
}
