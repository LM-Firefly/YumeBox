package com.github.yumelira.yumebox.service.clash.module

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.github.yumelira.yumebox.service.common.compat.registerReceiverCompat
import com.github.yumelira.yumebox.service.common.log.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.SelectClause1
import kotlinx.coroutines.withContext

abstract class Module<E>(val service: Service) {
    private val events: Channel<E> = Channel(Channel.UNLIMITED)
    private val receivers: MutableList<BroadcastReceiver> = mutableListOf()

    val onEvent: SelectClause1<E>
        get() = events.onReceive

    protected suspend fun enqueueEvent(event: E) {
        events.send(event)
    }

    protected fun receiveBroadcast(
        capacity: Int = Channel.UNLIMITED,
        configure: IntentFilter.() -> Unit
    ): ReceiveChannel<Intent> {
        val filter = IntentFilter().apply(configure)
        val channel = Channel<Intent>(capacity)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (context == null || intent == null) {
                    channel.close()

                    return
                }

                channel.trySend(intent)
            }
        }

        service.registerReceiverCompat(receiver, filter)

        receivers.add(receiver)

        return channel
    }

    suspend fun execute() {
        val moduleName = this.javaClass.simpleName

        try {
            Log.d("$moduleName: initialize")

            run()
        } finally {
            withContext(NonCancellable) {
                receivers.forEach {
                    it.onReceive(null, null)

                    service.unregisterReceiver(it)
                }

                Log.d("$moduleName: destroyed")
            }
        }
    }

    protected abstract suspend fun run()
}
