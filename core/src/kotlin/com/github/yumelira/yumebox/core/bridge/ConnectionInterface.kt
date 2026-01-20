package com.github.yumelira.yumebox.core.bridge

import androidx.annotation.Keep

@Keep
interface ConnectionInterface {
    fun received(jsonPayload: String)
}
