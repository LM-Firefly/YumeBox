package com.github.yumelira.yumebox.service.util

import java.net.InetAddress

fun InetAddress.asSocketAddressText(port: Int): String {
    return if (this.hostAddress?.contains(':') == true) {
        "[${this.hostAddress}]:$port"
    } else {
        "${this.hostAddress}:$port"
    }
}
