package com.github.yumelira.yumebox.service.common

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Kotlin extensions for YumeBox service
 */

/**
 * Get ComponentName for a KClass
 */
val KClass<*>.componentName: ComponentName
    get() = ComponentName(Global.application.packageName, this.java.name)

/**
 * Create Intent for a KClass
 */
val KClass<*>.intent: Intent
    get() = Intent(Global.application, this.java)

/**
 * Grant URI permissions to an Intent
 */
fun Intent.grantPermissions(read: Boolean = true, write: Boolean = true): Intent {
    var flags = 0

    if (read)
        flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION

    if (write)
        flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    addFlags(flags)
    return this
}

/**
 * UUID extension for Intent
 */
var Intent.uuid: UUID?
    get() {
        return data?.takeIf { it.scheme == "uuid" }?.schemeSpecificPart?.let(UUID::fromString)
    }
    set(value) {
        data = Uri.fromParts("uuid", value.toString(), null)
    }

fun Intent.setUUID(uuid: UUID): Intent {
    this.uuid = uuid
    return this
}

/**
 * fileName extension for Intent
 */
var Intent.fileName: String?
    get() {
        return data?.takeIf { it.scheme == "file" }?.schemeSpecificPart
    }
    set(value) {
        data = Uri.fromParts("file", value, null)
    }

fun Intent.setFileName(fileName: String): Intent {
    this.fileName = fileName
    return this
}
