package com.github.yumelira.yumebox.service.common

import android.app.Application

/**
 * Global application holder for YumeBox service module
 */
object Global {
    lateinit var application: Application
        private set

    fun init(app: Application) {
        application = app
    }
}
