package com.github.yumelira.yumebox.service.common.util

import android.app.Application

object Global {
    lateinit var application: Application
        private set

    fun init(app: Application) {
        application = app
    }
}

val packageName: String
    get() = Global.application.packageName
