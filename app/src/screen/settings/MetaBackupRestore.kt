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

package com.github.yumelira.yumebox.screen.settings

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object MetaBackupRestore {
    private const val EXCLUDED_ROOT_NAME = "frontend"
    private const val RESTORE_TEMP_DIR = "backup_restore_tmp"
    fun defaultBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "yumebox_backup_$timestamp.zip"
    }
    suspend fun backup(context: Context, targetUri: Uri): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching { backupInternal(context, targetUri) }
        }
    }
    suspend fun backupToFile(context: Context, targetFile: File): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                targetFile.parentFile?.mkdirs()
                targetFile.outputStream().use { outputStream ->
                    backupInternal(context, outputStream)
                }
            }
        }
    }
    suspend fun restore(context: Context, sourceUri: Uri): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching { restoreInternal(context, sourceUri) }
        }
    }
    suspend fun restoreFromFile(context: Context, sourceFile: File): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                FileInputStream(sourceFile).use { inputStream ->
                    restoreInternal(context, inputStream)
                }
            }
        }
    }
    private fun backupInternal(context: Context, targetUri: Uri) {
        val outputStream = requireNotNull(context.contentResolver.openOutputStream(targetUri)) {
            "unable to open backup output"
        }
        outputStream.use {
            backupInternal(context, it)
        }
    }
    private fun backupInternal(context: Context, outputStream: java.io.OutputStream) {
        val rootDir = context.filesDir
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOutputStream ->
            rootDir.listFiles()
                ?.sortedBy { it.name }
                ?.forEach { child ->
                    addEntry(rootDir, child, zipOutputStream)
                }
        }
    }
    private fun addEntry(rootDir: File, file: File, zipOutputStream: ZipOutputStream) {
        if (file.name == EXCLUDED_ROOT_NAME) {
            return
        }
        val relativePath = rootDir.toPath()
            .relativize(file.toPath())
            .toString()
            .replace(File.separatorChar, '/')
        if (file.isDirectory) {
            val children = file.listFiles()?.sortedBy { it.name }.orEmpty()
            if (children.isEmpty()) {
                zipOutputStream.putNextEntry(ZipEntry("$relativePath/"))
                zipOutputStream.closeEntry()
                return
            }
            children.forEach { child ->
                addEntry(rootDir, child, zipOutputStream)
            }
            return
        }
        zipOutputStream.putNextEntry(ZipEntry(relativePath))
        file.inputStream().use { inputStream ->
            inputStream.copyTo(zipOutputStream)
        }
        zipOutputStream.closeEntry()
    }
    private fun restoreInternal(context: Context, sourceUri: Uri) {
        val inputStream = requireNotNull(context.contentResolver.openInputStream(sourceUri)) {
            "unable to open backup input"
        }
        inputStream.use {
            restoreInternal(context, it)
        }
    }
    private fun restoreInternal(context: Context, inputStream: java.io.InputStream) {
        val rootDir = context.filesDir
        val tempDir = File(context.cacheDir, RESTORE_TEMP_DIR).apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            unzipToDirectory(inputStream, tempDir)
            val restoredChildren = tempDir.listFiles()
                ?.sortedBy { it.name }
                .orEmpty()
            require(restoredChildren.isNotEmpty()) { "backup archive is empty" }
            clearRootDirectory(rootDir)
            restoredChildren.forEach { child ->
                copyTree(child, File(rootDir, child.name))
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
    private fun unzipToDirectory(inputStream: java.io.InputStream, targetDir: File) {
        ZipInputStream(BufferedInputStream(inputStream)).use { zipInputStream ->
            while (true) {
                val entry = zipInputStream.nextEntry ?: break
                val entryName = entry.name.replace('\\', '/')
                val shouldSkip = entryName.split('/').any { it == EXCLUDED_ROOT_NAME || it.isBlank() }
                if (!shouldSkip) {
                    val targetFile = File(targetDir, entryName)
                    if (isSafeTarget(targetDir, targetFile)) {
                        if (entry.isDirectory) {
                            targetFile.mkdirs()
                        } else {
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { outputStream ->
                                zipInputStream.copyTo(outputStream)
                            }
                        }
                    }
                }
                zipInputStream.closeEntry()
            }
        }
    }
    private fun clearRootDirectory(rootDir: File) {
        rootDir.listFiles()?.forEach { child ->
            if (child.name != EXCLUDED_ROOT_NAME) {
                child.deleteRecursively()
            }
        }
    }
    private fun copyTree(source: File, target: File) {
        if (source.name == EXCLUDED_ROOT_NAME) {
            return
        }
        if (source.isDirectory) {
            target.mkdirs()
            source.listFiles()
                ?.sortedBy { it.name }
                ?.forEach { child ->
                    copyTree(child, File(target, child.name))
                }
            return
        }
        target.parentFile?.mkdirs()
        source.inputStream().use { inputStream ->
            FileOutputStream(target).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
    private fun isSafeTarget(rootDir: File, targetFile: File): Boolean {
        val rootPath = rootDir.canonicalFile.toPath()
        val targetPath = targetFile.absoluteFile.toPath().normalize()
        return targetPath.startsWith(rootPath)
    }
}
