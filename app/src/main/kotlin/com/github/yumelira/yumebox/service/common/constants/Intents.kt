package com.github.yumelira.yumebox.service.common.constants

import com.github.yumelira.yumebox.service.common.util.packageName

object Intents {
    // Public
    val ACTION_PROVIDE_URL: String
        get() = "${packageName}.action.PROVIDE_URL"

    val ACTION_START_CLASH: String
        get() = "${packageName}.action.START_CLASH"

    val ACTION_STOP_CLASH: String
        get() = "${packageName}.action.STOP_CLASH"

    val ACTION_TOGGLE_CLASH: String
        get() = "${packageName}.action.TOGGLE_CLASH"

    const val EXTRA_NAME = "name"

    // Self
    val ACTION_SERVICE_RECREATED: String
        get() = "${packageName}.intent.action.CLASH_RECREATED"

    val ACTION_CLASH_STARTED: String
        get() = "${packageName}.intent.action.CLASH_STARTED"

    val ACTION_CLASH_STOPPED: String
        get() = "${packageName}.intent.action.CLASH_STOPPED"

    val ACTION_CLASH_REQUEST_STOP: String
        get() = "${packageName}.intent.action.CLASH_REQUEST_STOP"

    val ACTION_PROFILE_CHANGED: String
        get() = "${packageName}.intent.action.PROFILE_CHANGED"

    val ACTION_PROFILE_LOADED: String
        get() = "${packageName}.intent.action.PROFILE_LOADED"

    val ACTION_OVERRIDE_CHANGED: String
        get() = "${packageName}.intent.action.OVERRIDE_CHANGED"

    const val EXTRA_STOP_REASON = "stop_reason"
    const val EXTRA_UUID = "uuid"
}
