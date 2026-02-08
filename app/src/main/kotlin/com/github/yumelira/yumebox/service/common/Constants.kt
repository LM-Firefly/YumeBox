package com.github.yumelira.yumebox.service.common

/**
 * Global constants for YumeBox service
 */
object Constants {
    // Package name helper
    val packageName: String
        get() = Global.application.packageName

    // Intent Actions - Public
    val ACTION_START_CLASH = "$packageName.action.START_CLASH"
    val ACTION_STOP_CLASH = "$packageName.action.STOP_CLASH"
    val ACTION_TOGGLE_CLASH = "$packageName.action.TOGGLE_CLASH"

    // Intent Actions - Internal
    val ACTION_SERVICE_RECREATED = "$packageName.intent.action.SERVICE_RECREATED"
    val ACTION_CLASH_STARTED = "$packageName.intent.action.CLASH_STARTED"
    val ACTION_CLASH_STOPPED = "$packageName.intent.action.CLASH_STOPPED"
    val ACTION_CLASH_REQUEST_STOP = "$packageName.intent.action.CLASH_REQUEST_STOP"
    val ACTION_PROFILE_CHANGED = "$packageName.intent.action.PROFILE_CHANGED"
    val ACTION_PROFILE_LOADED = "$packageName.intent.action.PROFILE_LOADED"
    val ACTION_OVERRIDE_CHANGED = "$packageName.intent.action.OVERRIDE_CHANGED"

    // Intent Extras
    const val EXTRA_UUID = "uuid"
    const val EXTRA_STOP_REASON = "stop_reason"
    const val EXTRA_NAME = "name"

    // Content Provider Authorities
    val AUTHORITY_STATUS_PROVIDER = "$packageName.status"
    val AUTHORITY_SETTINGS_PROVIDER = "$packageName.settings"
    val AUTHORITY_FILES_PROVIDER = "$packageName.files"
}
