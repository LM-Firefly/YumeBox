package com.github.yumelira.yumebox.runtime.service.document

import android.provider.DocumentsContract
import java.io.File
import java.util.UUID

enum class Flag { Writable, Deletable, Virtual }

interface Document {
    val id: String
    val name: String
    val mimeType: String
    val size: Long
    val updatedAt: Long
    val flags: Set<Flag>
}

class FileDocument(
    val file: File,
    override val flags: Set<Flag>,
    private val idOverride: String? = null,
    private val nameOverride: String? = null,
) : Document {
    override val id: String get() = idOverride ?: file.name
    override val name: String get() = nameOverride ?: file.name
    override val mimeType: String
        get() = if (file.isDirectory) DocumentsContract.Document.MIME_TYPE_DIR else "text/plain"
    override val size: Long get() = file.length()
    override val updatedAt: Long get() = file.lastModified()
}

class VirtualDocument(
    override val id: String,
    override val name: String,
    override val mimeType: String,
    override val size: Long,
    override val updatedAt: Long,
    override val flags: Set<Flag>,
) : Document

data class Path(
    val uuid: UUID?,
    val scope: Scope?,
    val relative: List<String>?,
) {
    enum class Scope { Runtime, Configuration, Providers }
    override fun toString(): String {
        if (uuid == null) {
            if (scope == null) return "/"
            if (scope == Scope.Runtime) {
                if (relative == null) return "/${Paths.RUNTIME_ID}"
                return "/${Paths.RUNTIME_ID}/${relative.joinToString(separator = "/")}"
            }
            throw IllegalStateException("invalid path state without uuid: scope=$scope")
        }
        if (scope == null) return "/$uuid"
        val sc = when (scope) {
            Scope.Runtime -> throw IllegalStateException("runtime scope should not contain uuid")
            Scope.Configuration -> Paths.CONFIGURATION_ID
            Scope.Providers -> Paths.PROVIDERS_ID
        }
        if (relative == null) return "/$uuid/$sc"
        return "/$uuid/$sc/${relative.joinToString(separator = "/")}"
    }
}

object Paths {
    const val RUNTIME_ID = "runtime"
    const val CONFIGURATION_ID = "config.yaml"
    const val PROVIDERS_ID = "providers"
    fun resolve(path: String): Path {
        val segments = path.split("/").filter { it.isNotBlank() && it != "." && it != ".." }
        return when (segments.size) {
            0 -> Path(uuid = null, scope = null, relative = null)
            1 -> {
                if (segments[0] == RUNTIME_ID) {
                    Path(uuid = null, scope = Path.Scope.Runtime, relative = null)
                } else {
                    Path(uuid = UUID.fromString(segments[0]), scope = null, relative = null)
                }
            }
            2 -> Path(
                uuid = if (segments[0] == RUNTIME_ID) null else UUID.fromString(segments[0]),
                scope = if (segments[0] == RUNTIME_ID) {
                    Path.Scope.Runtime
                } else {
                    when (segments[1]) {
                        CONFIGURATION_ID -> Path.Scope.Configuration
                        PROVIDERS_ID -> Path.Scope.Providers
                        else -> throw IllegalArgumentException("unknown scope ${segments[1]}")
                    }
                },
                relative = if (segments[0] == RUNTIME_ID) segments.drop(1) else null,
            )
            else -> Path(
                uuid = if (segments[0] == RUNTIME_ID) null else UUID.fromString(segments[0]),
                scope = if (segments[0] == RUNTIME_ID) {
                    Path.Scope.Runtime
                } else {
                    when (segments[1]) {
                        CONFIGURATION_ID -> Path.Scope.Configuration
                        PROVIDERS_ID -> Path.Scope.Providers
                        else -> throw IllegalArgumentException("unknown scope ${segments[1]}")
                    }
                },
                relative = if (segments[0] == RUNTIME_ID) segments.drop(1) else segments.drop(2),
            )
        }
    }
}
