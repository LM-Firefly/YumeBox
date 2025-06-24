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

package com.github.yumelira.yumebox.service.runtime.util

import android.os.Binder
import android.os.Parcel
import android.os.Parcelable

/**
 * Parcelable utilities for sliced list transmission
 */

private class SliceParcelableListBpBinder(
    val list: List<Parcelable>,
    val flags: Int
) : Binder() {
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, tFlags: Int): Boolean {
        when (code) {
            TRANSACTION_GET_ITEMS -> {
                reply ?: return false

                val offset = data.readInt()
                val chunk = data.readInt()

                val end = (offset + chunk).coerceAtMost(list.size)

                reply.writeInt(end - offset)

                for (i in offset until end) {
                    list[i].writeToParcel(reply, flags)
                }

                return true
            }
        }

        return super.onTransact(code, data, reply, flags)
    }

    companion object {
        const val TRANSACTION_GET_ITEMS = 10
    }
}

/**
 * Write a list of Parcelables to Parcel in slices
 */
fun <T : Parcelable> List<T>.writeToParcelSlice(parcel: Parcel, flags: Int) {
    val bp = SliceParcelableListBpBinder(this, flags)

    parcel.writeInt(size)
    parcel.writeStrongBinder(bp)
}

/**
 * Create a list of Parcelables from Parcel slices
 */
fun <T : Parcelable> Parcelable.Creator<T>.createListFromParcelSlice(
    parcel: Parcel,
    flags: Int,
    chunk: Int = 100,
): List<T> {
    val total = parcel.readInt()
    val remote = parcel.readStrongBinder()
    val result = ArrayList<T>(total)

    var offset = 0

    while (offset < total) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()

        try {
            data.writeInt(offset)
            data.writeInt(chunk)

            if (!remote.transact(
                    SliceParcelableListBpBinder.TRANSACTION_GET_ITEMS,
                    data,
                    reply,
                    flags
                )
            ) {
                break
            }

            val size = reply.readInt()

            repeat(size) {
                result.add(createFromParcel(reply))
            }

            offset += size

            if (size == 0)
                break
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    return result
}
