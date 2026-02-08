package com.github.yumelira.yumebox.service.common.util

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

suspend fun ticker(delayMillis: Long): ReceiveChannel<Unit> = coroutineScope {
    produce {
        while (isActive) {
            delay(delayMillis)
            send(Unit)
        }
    }
}
