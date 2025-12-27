package com.github.yumelira.yumebox.clash.exception

import dev.oom_wg.purejoy.mlang.MLang

sealed class ConfigImportException(
    message: String, cause: Throwable? = null
) : Exception(message, cause) {
    abstract val isRetryable: Boolean

    abstract val userFriendlyMessage: String
}

class NetworkException(
    message: String, cause: Throwable? = null, val url: String? = null, val statusCode: Int? = null
) : ConfigImportException(message, cause) {
    override val isRetryable: Boolean = true
    override val userFriendlyMessage: String
        get() = when {
            statusCode != null -> MLang.ProfilesPage.Import.Message.NetworkHttpFailed.format(statusCode, message)
            url != null -> MLang.ProfilesPage.Import.Message.UnableToConnect.format(url)
            else -> MLang.ProfilesPage.Import.Message.NetworkConnFailed.format(message)
        }
}

class ConfigValidationException(
    message: String, cause: Throwable? = null, val validationErrors: List<String> = emptyList()
) : ConfigImportException(message, cause) {
    override val isRetryable: Boolean = false
    override val userFriendlyMessage: String
        get() = if (validationErrors.isNotEmpty()) {
            MLang.ProfilesPage.Import.Message.FormatValidationErrors.format(validationErrors.joinToString("\n"))
        } else {
            MLang.ProfilesPage.Import.Message.FormatValidationError.format(message)
        }
}

class FileAccessException(
    message: String, val reason: Reason = Reason.UNKNOWN, cause: Throwable? = null, val filePath: String? = null
) : ConfigImportException(message, cause) {

    enum class Reason {
        NOT_FOUND, NO_READ_PERMISSION, NO_WRITE_PERMISSION, INSUFFICIENT_SPACE, INVALID_PATH, UNKNOWN
    }

    override val isRetryable: Boolean = reason == Reason.INSUFFICIENT_SPACE
    override val userFriendlyMessage: String
        get() = when (reason) {
            Reason.NOT_FOUND -> MLang.ProfilesPage.Import.Message.FileNotFound.format(filePath ?: MLang.ProfilesPage.Message.UnknownFile)
            Reason.NO_READ_PERMISSION -> MLang.ProfilesPage.Import.Message.NoReadPermission.format(filePath ?: MLang.ProfilesPage.Message.UnknownFile)
            Reason.NO_WRITE_PERMISSION -> MLang.ProfilesPage.Import.Message.NoWritePermission.format(filePath ?: MLang.ProfilesPage.Message.UnknownFile)
            Reason.INSUFFICIENT_SPACE -> MLang.ProfilesPage.Import.Message.InsufficientSpace
            Reason.INVALID_PATH -> MLang.ProfilesPage.Import.Message.InvalidPath.format(filePath ?: MLang.ProfilesPage.Message.UnknownFile)
            Reason.UNKNOWN -> MLang.ProfilesPage.Import.Message.FileAccessError.format(message)
        }
}


class TimeoutException(
    message: String, cause: Throwable? = null, val timeoutMs: Long? = null, val operation: String? = null
) : ConfigImportException(message, cause) {
    override val isRetryable: Boolean = true
    override val userFriendlyMessage: String
        get() {
            val opDesc = operation?.let { "[$it]" } ?: ""
            val timeDesc = timeoutMs?.let { " (超时时间: ${it / 1000}秒)" } ?: ""
            return MLang.ProfilesPage.Import.Message.OperationTimeout.format("$opDesc$timeDesc")
        }
}


class ImportCancelledException(
    message: String = MLang.ProfilesPage.Import.Message.ImportCancelled
) : ConfigImportException(message, null) {
    override val isRetryable: Boolean = false
    override val userFriendlyMessage: String
        get() = message ?: MLang.ProfilesPage.Import.Message.ImportCancelled
}

class UnknownException(
    message: String, cause: Throwable? = null
) : ConfigImportException(message, cause) {
    override val isRetryable: Boolean = false
    override val userFriendlyMessage: String
        get() = "$message"
}

fun Throwable.toConfigImportException(): ConfigImportException {
    return when (this) {
        is ConfigImportException -> this

        is com.github.yumelira.yumebox.core.bridge.ClashException -> ConfigValidationException(
            this.message ?: MLang.ProfilesPage.Import.Message.FormatValidationError.format(""), this
        )

        is java.net.UnknownHostException -> NetworkException(
            MLang.ProfilesPage.Import.Message.UnableToConnect.format(this.message), this
        )

        is java.net.SocketTimeoutException -> TimeoutException(
            MLang.ProfilesPage.Import.Message.OperationTimeout.format("[" + MLang.ProfilesPage.Import.Message.NetworkRequest + "]"), this, operation = MLang.ProfilesPage.Import.Message.NetworkRequest
        )

        is java.net.ConnectException -> NetworkException(
            MLang.ProfilesPage.Import.Message.UnableToConnect.format(this.message), this
        )

        is java.io.FileNotFoundException -> FileAccessException(
            this.message ?: MLang.ProfilesPage.Import.Message.FileNotFound.format(""), FileAccessException.Reason.NOT_FOUND, this
        )

        is java.io.IOException -> when {
            message?.contains("Permission denied", ignoreCase = true) == true -> FileAccessException(
                this.message ?: MLang.ProfilesPage.Import.Message.NoWritePermission.format(""), FileAccessException.Reason.NO_WRITE_PERMISSION, this
            )

            message?.contains("No space left", ignoreCase = true) == true -> FileAccessException(
                MLang.ProfilesPage.Import.Message.InsufficientSpace, FileAccessException.Reason.INSUFFICIENT_SPACE, this
            )

            else -> FileAccessException(
                this.message ?: MLang.ProfilesPage.Import.Message.FileAccessError.format(""), FileAccessException.Reason.UNKNOWN, this
            )
        }

        is kotlinx.coroutines.CancellationException -> ImportCancelledException()
        is java.util.concurrent.TimeoutException -> TimeoutException(
            MLang.ProfilesPage.Import.Message.OperationTimeout.format(""), this
        )

        else -> UnknownException(this.message ?: MLang.ProfilesPage.Import.Message.UnknownError, this)
    }
}