@file:UseSerializers(UUIDSerializer::class)

package com.github.yumelira.yumebox.service.data

import com.github.yumelira.yumebox.service.data.model.Imported
import com.github.yumelira.yumebox.service.data.model.Pending
import com.github.yumelira.yumebox.service.data.model.Selection
import com.github.yumelira.yumebox.service.util.UUIDSerializer
import com.tencent.mmkv.MMKV
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * MMKV-based storage manager for profile data
 */
object ProfileStore {
    private val mmkv by lazy { MMKV.mmkvWithID("profiles", MMKV.MULTI_PROCESS_MODE) }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Imported operations
    fun saveImported(list: List<Imported>) {
        val jsonString = json.encodeToString(ListSerializer(Imported.serializer()), list)
        mmkv.encode("imported", jsonString)
    }

    fun loadImported(): List<Imported> {
        val jsonString = mmkv.decodeString("imported") ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(Imported.serializer()), jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Pending operations
    fun savePending(list: List<Pending>) {
        val jsonString = json.encodeToString(ListSerializer(Pending.serializer()), list)
        mmkv.encode("pending", jsonString)
    }

    fun loadPending(): List<Pending> {
        val jsonString = mmkv.decodeString("pending") ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(Pending.serializer()), jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Selection operations
    fun saveSelections(list: List<Selection>) {
        val jsonString = json.encodeToString(ListSerializer(Selection.serializer()), list)
        mmkv.encode("selections", jsonString)
    }

    fun loadSelections(): List<Selection> {
        val jsonString = mmkv.decodeString("selections") ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(Selection.serializer()), jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear() {
        mmkv.clearAll()
    }
}
