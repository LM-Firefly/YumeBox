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



@file:UseSerializers(UUIDSerializer::class)

package com.github.yumelira.yumebox.runtime.api.service.runtime.entity

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import com.github.yumelira.yumebox.core.util.Parcelizer
import com.github.yumelira.yumebox.runtime.api.service.runtime.util.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.*

@Serializable
enum class RuntimeTargetMode {
    Tun,
    Http,
    RootTun,
}

@Serializable
enum class RuntimeOwner {
    None,
    LocalTun,
    LocalHttp,
    RootTun,
}

@Serializable
enum class RuntimePhase {
    Idle,
    Starting,
    Running,
    Stopping,
    Failed;
    val running: Boolean
        get() = this == Running
}

@Serializable
data class RuntimeSnapshot(
    val owner: RuntimeOwner = RuntimeOwner.None,
    val phase: RuntimePhase = RuntimePhase.Idle,
    val targetMode: RuntimeTargetMode = RuntimeTargetMode.Tun,
    val profileReady: Boolean = false,
    val groupsReady: Boolean = false,
    val trafficReady: Boolean = false,
    val configReady: Boolean = false,
    val transportReady: Boolean = false,
    val logReady: Boolean = false,
    val profileUuid: String? = null,
    val profileName: String? = null,
    val lastError: String? = null,
    val startedAt: Long? = null,
    val effectiveFingerprint: String? = null,
    val generation: Long = 0L,
    val running: Boolean = phase.running,
) {
    val payloadReady: Boolean
        get() = profileReady && groupsReady && trafficReady
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Profile(
    val uuid: UUID,
    val name: String,
    val type: Type,
    val source: String,
    val active: Boolean,
    val interval: Long,
    val upload: Long,
    val download: Long,
    val total: Long,
    val expire: Long,
    val updatedAt: Long,
) : Parcelable {
    enum class Type {
        File, Url, External
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Profile> {
        override fun createFromParcel(parcel: Parcel): Profile {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<Profile?> {
            return arrayOfNulls(size)
        }
    }
}
