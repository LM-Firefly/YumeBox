package com.github.yumelira.yumebox.data.util

import java.io.File

interface AssetDownloader {
    suspend fun download(url: String, targetFile: File): Boolean
}
