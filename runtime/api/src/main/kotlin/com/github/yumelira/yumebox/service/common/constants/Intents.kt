/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

package com.github.yumelira.yumebox.service.common.constants

import com.github.yumelira.yumebox.service.common.util.packageName

object Intents {
    fun actionProvideUrl(packageName: String): String = "$packageName.action.PROVIDE_URL"
    fun actionStartClash(packageName: String): String = "$packageName.action.START_CLASH"
    fun actionStopClash(packageName: String): String = "$packageName.action.STOP_CLASH"
    fun actionToggleClash(packageName: String): String = "$packageName.action.TOGGLE_CLASH"
    fun actionServiceRecreated(packageName: String): String = "$packageName.intent.action.CLASH_RECREATED"
    fun actionClashStarted(packageName: String): String = "$packageName.intent.action.CLASH_STARTED"
    fun actionClashStopped(packageName: String): String = "$packageName.intent.action.CLASH_STOPPED"
    fun actionClashRequestStop(packageName: String): String = "$packageName.intent.action.CLASH_REQUEST_STOP"
    fun actionProfileChanged(packageName: String): String = "$packageName.intent.action.PROFILE_CHANGED"
    fun actionProfileLoaded(packageName: String): String = "$packageName.intent.action.PROFILE_LOADED"
    fun actionOverrideChanged(packageName: String): String = "$packageName.intent.action.OVERRIDE_CHANGED"

    // Public
    val ACTION_PROVIDE_URL: String
        get() = actionProvideUrl(packageName)

    val ACTION_START_CLASH: String
        get() = actionStartClash(packageName)

    val ACTION_STOP_CLASH: String
        get() = actionStopClash(packageName)

    val ACTION_TOGGLE_CLASH: String
        get() = actionToggleClash(packageName)

    const val EXTRA_NAME = "name"

    // Self
    val ACTION_SERVICE_RECREATED: String
        get() = actionServiceRecreated(packageName)

    val ACTION_CLASH_STARTED: String
        get() = actionClashStarted(packageName)

    val ACTION_CLASH_STOPPED: String
        get() = actionClashStopped(packageName)

    val ACTION_CLASH_REQUEST_STOP: String
        get() = actionClashRequestStop(packageName)

    val ACTION_PROFILE_CHANGED: String
        get() = actionProfileChanged(packageName)

    val ACTION_PROFILE_LOADED: String
        get() = actionProfileLoaded(packageName)

    val ACTION_OVERRIDE_CHANGED: String
        get() = actionOverrideChanged(packageName)

    const val EXTRA_STOP_REASON = "stop_reason"
    const val EXTRA_UUID = "uuid"
}
