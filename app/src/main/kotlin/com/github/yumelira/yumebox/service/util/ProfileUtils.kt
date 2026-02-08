package com.github.yumelira.yumebox.service.util

import com.github.yumelira.yumebox.service.data.ImportedDao
import com.github.yumelira.yumebox.service.data.PendingDao
import java.util.*

suspend fun generateProfileUUID(): UUID {
    var result = UUID.randomUUID()

    while (ImportedDao.exists(result) || PendingDao.exists(result)) {
        result = UUID.randomUUID()
    }

    return result
}
