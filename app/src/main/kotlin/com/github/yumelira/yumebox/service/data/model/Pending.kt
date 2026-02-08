@file:UseSerializers(UUIDSerializer::class)

package com.github.yumelira.yumebox.service.data.model

import com.github.yumelira.yumebox.service.util.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

@Serializable
data class Pending(
    val uuid: UUID,
    val name: String,
    val type: Profile.Type,
    val source: String,
    val interval: Long,
    val upload: Long,
    val download: Long,
    val total: Long,
    val expire: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
