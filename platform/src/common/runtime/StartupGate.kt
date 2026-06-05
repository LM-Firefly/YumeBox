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

package com.github.yumelira.yumebox.common.runtime

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.android.apksig.ApkVerifier
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.zip.ZipFile
import kotlin.system.exitProcess
import timber.log.Timber

object StartupGate {
    private const val maskBase = 0x39

    @Volatile private var primaryLoaded = false

    @Volatile private var verificationCompleted = false

    @Volatile private var verificationFailure: String? = null

    fun verify(application: Application) {
        val isDebuggable =
            (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val failure = evaluateFailure(application).getOrElse { throwable ->
            val message = "exception:${throwable.javaClass.name}"
            Timber.e(throwable, "Startup gate threw, bypassing: %s", message)
            message
        }

        verificationFailure = failure
        verificationCompleted = true

        if (failure != null) {
            if (isDebuggable) {
                Timber.w("Startup gate check failed: %s (bypassed)", failure)
            } else {
                Timber.w("Startup gate check failed in release: %s (bypassed)", failure)
            }
        }
    }

    fun loadPrimary(context: android.content.Context? = null) {
        if (primaryLoaded) return
        synchronized(this) {
            if (primaryLoaded) return
            resolveVerificationFailure(context)?.let { failure ->
                Timber.i("Skip native primary library load: %s", failure)
                return
            }
            runCatching {
                System.loadLibrary(unmask(intArrayOf(64, 79, 86, 89)))
                primaryLoaded = true
            }.getOrElse { error ->
                if (error is UnsatisfiedLinkError) {
                    Timber.w(error, "Native primary library load failed; continuing without it")
                } else {
                    throw error
                }
            }
        }
    }

    private fun resolveVerificationFailure(context: android.content.Context?): String? {
        if (verificationCompleted) {
            return verificationFailure
        }

        val application = context?.applicationContext as? Application ?: return null
        val failure = evaluateFailure(application).getOrNull()
        verificationFailure = failure
        verificationCompleted = true
        return failure
    }

    private fun evaluateFailure(application: Application): Result<String?> = runCatching {
        val ctx = application.applicationContext
        when {
            !checkPkg(ctx.packageName) -> "checkPkg"
            !checkApkPath(ctx.packageName, application.applicationInfo.sourceDir) -> "checkApkPath"
            !checkApkV2(application.applicationInfo.sourceDir) -> "checkApkV2"
            !checkSigner(application.packageManager, ctx.packageName) -> "checkSigner"
            !checkAppClass(ctx::class.java.name) -> "checkAppClass"
            !checkAppParent(ctx::class.java.superclass?.name) -> "checkAppParent"
            !checkPackagedPrimary(application) -> "checkPackagedPrimary"
            else -> null
        }
    }

    private fun checkPkg(actual: String): Boolean =
        actual ==
            unmask(
                intArrayOf(
                    90,
                    85,
                    86,
                    18,
                    90,
                    87,
                    75,
                    40,
                    52,
                    32,
                    109,
                    61,
                    48,
                    84,
                    95,
                    87,
                    85,
                    79,
                    95,
                    17,
                    57,
                    52,
                    47,
                    38,
                    38,
                    42,
                    65,
                )
            )

    private fun checkApkPath(packageName: String, sourceDir: String?): Boolean {
        if (sourceDir.isNullOrBlank()) return false
        val pmPath = queryPmPath(packageName) ?: return false
        return sourceDir == pmPath
    }

    private fun checkApkV2(sourceDir: String?): Boolean {
        if (sourceDir.isNullOrBlank()) return false
        return runCatching {
            ApkVerifier.Builder(File(sourceDir)).build().verify().isVerifiedUsingV2Scheme
        }.getOrElse { error ->
            // Some builds may hit ApkSig ASN.1 parser incompatibilities at runtime.
            // Treat this as an unsupported runtime check instead of a hard failure.
            if (error.javaClass.name.startsWith("com.android.apksig.internal.asn1.")) {
                Timber.w(error, "Skip ApkVerifier V2 check due to ASN.1 parser incompatibility")
                true
            } else {
                false
            }
        }
    }

    private fun checkSigner(pm: PackageManager, packageName: String): Boolean {
        val digests = getSignerSha256(pm, packageName)
        if (digests.isEmpty()) return false
        val isDebuggable = isDebuggablePackage(pm, packageName)
        if (isDebuggable) return true
        return digests.any { it == releaseFingerprint() }
    }

    private fun isDebuggablePackage(pm: PackageManager, packageName: String): Boolean {
        val appInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.getApplicationInfo(packageName, 0)
            }
        return (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun getSignerSha256(pm: PackageManager, packageName: String): List<String> {
        val pkg =
            runCatching { getPackageInfoCompat(pm, packageName) }.getOrNull() ?: return emptyList()
        val signatures =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
            } else {
                @Suppress("DEPRECATION") pkg.signatures?.map { it.toByteArray() }.orEmpty()
            }
        if (signatures.isEmpty()) return emptyList()

        val md = MessageDigest.getInstance(unmask(intArrayOf(106, 114, 122, 17, 15, 11, 9)))
        val certFactory = CertificateFactory.getInstance("X.509")
        return signatures.map { raw ->
            val cert = certFactory.generateCertificate(ByteArrayInputStream(raw)) as X509Certificate
            val digest = md.digest(cert.encoded)
            digest.joinToString(":") { byte -> "%02X".format(Locale.US, byte.toInt() and 0xFF) }
        }
    }

    private fun getPackageInfoCompat(pm: PackageManager, packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION") pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        }
    }

    private fun checkAppClass(actual: String?): Boolean =
        actual ==
            unmask(
                intArrayOf(
                    90,
                    85,
                    86,
                    18,
                    90,
                    87,
                    75,
                    40,
                    52,
                    32,
                    109,
                    61,
                    48,
                    84,
                    95,
                    87,
                    85,
                    79,
                    95,
                    17,
                    57,
                    52,
                    47,
                    38,
                    38,
                    42,
                    65,
                    20,
                    122,
                    76,
                    77,
                )
            )

    private fun checkAppParent(actual: String?): Boolean =
        actual ==
            unmask(
                intArrayOf(
                    88,
                    84,
                    95,
                    78,
                    82,
                    87,
                    91,
                    110,
                    32,
                    50,
                    51,
                    106,
                    4,
                    73,
                    74,
                    87,
                    85,
                    94,
                    95,
                    75,
                    41,
                    46,
                    44,
                )
            )

    private fun checkPackagedPrimary(application: Application): Boolean {
        val soName = System.mapLibraryName(unmask(intArrayOf(64, 79, 86, 89)))
        val apkPaths =
            buildList {
                    add(application.applicationInfo.sourceDir)
                    application.applicationInfo.splitSourceDirs?.let(::addAll)
                }
                .filter { !it.isNullOrBlank() }

        return apkPaths.any { apkPath ->
            runCatching {
                    ZipFile(apkPath).use { zip ->
                        zip.entries().asSequence().any { entry ->
                            !entry.isDirectory &&
                                entry.name.startsWith("lib/") &&
                                entry.name.endsWith("/$soName")
                        }
                    }
                }
                .getOrDefault(false)
        }
    }

    private fun queryPmPath(packageName: String): String? =
        runCatching {
                val process =
                    ProcessBuilder("sh", "-c", "pm path $packageName")
                        .redirectErrorStream(true)
                        .start()
                try {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.firstOrNull { it.startsWith("package:") }?.removePrefix("package:")
                    }
                } finally {
                    process.destroy()
                }
            }
            .getOrNull()

    private fun releaseFingerprint(): String = unmask(
        intArrayOf(
            1, 8, 1, 11, 5, 4, 9, 1, 123, 6, 112, 126, 1, 122, 0, 8,
            10, 7, 12, 125, 122, 118, 4, 121, 115, 117, 3, 10, 9, 6, 124, 10,
            5, 117, 7, 120, 116, 113, 127, 13, 13, 1, 122, 121, 4, 126, 115, 123,
            117, 0, 126, 119, 13, 0, 121, 4, 7, 11, 13, 122, 7, 1, 121, 124,
            112, 3, 3, 10, 6, 11, 14, 5, 121, 0, 120, 116, 112, 127, 0, 121,
            1, 12, 12, 4, 126, 6, 123, 116, 1, 126, 6, 127, 0, 11, 4,
        )
    )

    private fun unmask(values: IntArray): String =
        buildString(values.size) {
            values.forEachIndexed { index, value ->
                append((value xor (maskBase + (index % 13))).toChar())
            }
        }

    private fun die(): Nothing {
        Process.killProcess(Process.myPid())
        exitProcess(-1)
    }
}
