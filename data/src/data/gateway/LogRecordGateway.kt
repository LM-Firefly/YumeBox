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



package com.github.yumelira.yumebox.data.gateway

import android.app.Application
import com.github.yumelira.yumebox.core.util.PollingTimerSpec
import timber.log.Timber
import java.io.File

interface LogRecordGateway {
    val isRecording: Boolean
    val currentLogFileName: String?
    val logPrefix: String
    val logSuffix: String
    val stopWaitSpec: PollingTimerSpec

    fun start(application: Application)
    fun stop(application: Application)
    fun getLogDir(application: Application): File
}

private const val LOG_RECORD_GATEWAY_CLASS = "com.github.yumelira.yumebox.service.LogRecordServiceGateway"
private const val LOG_RECORD_SERVICE_CLASS = "com.github.yumelira.yumebox.service.LogRecordService"

fun createLogRecordGateway(): LogRecordGateway {
    return runCatching {
        val gatewayClass = Class.forName(LOG_RECORD_GATEWAY_CLASS)
        gatewayClass.getDeclaredConstructor().newInstance() as? LogRecordGateway
    }.getOrNull() ?: NoOpLogRecordGateway
}

fun writeRuntimeLog(line: String) {
    runCatching {
        val serviceClass = Class.forName(LOG_RECORD_SERVICE_CLASS)
        serviceClass.getDeclaredMethod("writeLog", String::class.java).invoke(null, line)
    }.onFailure {
        Timber.v(it, "Runtime log bridge unavailable, skipped log append")
    }
}

private object NoOpLogRecordGateway : LogRecordGateway {
    override val isRecording: Boolean
        get() = false
    override val currentLogFileName: String?
        get() = null
    override val logPrefix: String
        get() = ""
    override val logSuffix: String
        get() = ".log"
    override val stopWaitSpec: PollingTimerSpec
        get() = PollingTimerSpec(
            name = "log_record_stop_wait",
            intervalMillis = 300L,
            initialDelayMillis = 300L,
        )
    override fun start(application: Application) = Unit
    override fun stop(application: Application) = Unit
    override fun getLogDir(application: Application): File {
        return File(application.filesDir, "logs").apply { mkdirs() }
    }
}
