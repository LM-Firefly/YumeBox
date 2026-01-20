package com.github.yumelira.yumebox.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.yumelira.yumebox.data.model.Selection

class SelectionDao(context: Context) {
    companion object {
        private const val PREFS_NAME = "proxy_selections"
        private const val KEY_PREFIX = "selection_"
        private const val PIN_KEY_PREFIX = "pin_"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setSelected(selection: Selection) {
        try {
            val key = makeKey(selection.profileId, selection.proxyGroup)
            prefs.edit {
                putString(key, selection.selectedNode)
            }
        } catch (_: Exception) {
        }
    }

    fun getSelected(profileId: String, proxyGroup: String): String? {
        return try {
            val key = makeKey(profileId, proxyGroup)
            prefs.getString(key, null)
        } catch (e: Exception) {
            null
        }
    }

    fun removeSelected(profileId: String, proxyGroup: String) {
        try {
            val key = makeKey(profileId, proxyGroup)
            prefs.edit {
                remove(key)
            }
        } catch (e: Exception) {
        }
    }

    fun getAllSelections(profileId: String): Map<String, String> {
        return try {
            val prefix = makeKeyPrefix(profileId)
            val selections = mutableMapOf<String, String>()

            prefs.all.forEach { (key, value) ->
                if (key.startsWith(prefix) && value is String) {
                    val proxyGroup = key.substring(prefix.length)
                    selections[proxyGroup] = value
                }
            }

            selections
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun setPinned(profileId: String, proxyGroup: String, pinnedNode: String) {
        try {
            val key = makePinKey(profileId, proxyGroup)
            prefs.edit {
                putString(key, pinnedNode)
            }
        } catch (e: Exception) {
        }
    }

    fun getPinned(profileId: String, proxyGroup: String): String? {
        return try {
            val key = makePinKey(profileId, proxyGroup)
            prefs.getString(key, null)
        } catch (e: Exception) {
            null
        }
    }

    fun removePinned(profileId: String, proxyGroup: String) {
        try {
            val key = makePinKey(profileId, proxyGroup)
            prefs.edit {
                remove(key)
            }
        } catch (e: Exception) {
        }
    }

    fun getAllPins(profileId: String): Map<String, String> {
        return try {
            val prefix = makePinKeyPrefix(profileId)
            val pins = mutableMapOf<String, String>()
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(prefix) && value is String) {
                    val proxyGroup = key.substring(prefix.length)
                    pins[proxyGroup] = value
                }
            }
            pins
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun clearAllSelections(profileId: String) {
        try {
            val prefix = makeKeyPrefix(profileId)
            prefs.edit {
                prefs.all.keys
                    .filter { it.startsWith(prefix) }
                    .forEach { remove(it) }
            }
        } catch (e: Exception) {
        }
    }

    fun setSelections(profileId: String, selections: Map<String, String>) {
        try {
            prefs.edit {
                selections.forEach { (proxyGroup, selectedNode) ->
                    val key = makeKey(profileId, proxyGroup)
                    putString(key, selectedNode)
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun makeKey(profileId: String, proxyGroup: String): String {
        return "${KEY_PREFIX}${profileId}_$proxyGroup"
    }

    private fun makeKeyPrefix(profileId: String): String {
        return "${KEY_PREFIX}${profileId}_"
    }

    private fun makePinKey(profileId: String, proxyGroup: String): String {
        return "${PIN_KEY_PREFIX}${profileId}_$proxyGroup"
    }

    private fun makePinKeyPrefix(profileId: String): String {
        return "${PIN_KEY_PREFIX}${profileId}_"
    }
}
