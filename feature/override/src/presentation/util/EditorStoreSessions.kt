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

package com.github.yumelira.yumebox.presentation.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import kotlinx.serialization.json.JsonElement

internal class OverrideConfigPreviewSession {
    var title by mutableStateOf("")
    var content by mutableStateOf("")
    var language by mutableStateOf(LanguageScope.Json)
    var callback: ((String) -> Unit)? = null

    fun setup(
        title: String,
        content: String,
        language: LanguageScope,
        callback: ((String) -> Unit)?,
    ) {
        this.title = title
        this.content = content
        this.language = language
        this.callback = callback
    }

    fun clear() {
        title = ""
        content = ""
        language = LanguageScope.Json
        callback = null
    }
}

internal class OverrideStringListEditorSession {
    var title by mutableStateOf("")
    var placeholder by mutableStateOf("")
    var availableModes by mutableStateOf(listOf(OverrideListEditorMode.Replace))
    var selectedMode by mutableStateOf(OverrideListEditorMode.Replace)
    var values by mutableStateOf(OverrideListModeValues<List<String>>())
    var callback: ((OverrideListModeValues<List<String>>) -> Unit)? = null

    fun setup(
        title: String,
        placeholder: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<List<String>>,
        callback: (OverrideListModeValues<List<String>>) -> Unit,
    ) {
        this.title = title
        this.placeholder = placeholder
        this.availableModes = availableModes
        this.selectedMode = selectedMode
        this.values = values.copy(
            replaceValue = values.replaceValue?.toList(),
            mergeValue = values.mergeValue?.toList(),
            startValue = values.startValue?.toList(),
            endValue = values.endValue?.toList(),
        )
        this.callback = callback
    }

    fun update(
        selectedMode: OverrideListEditorMode = this.selectedMode,
        values: OverrideListModeValues<List<String>> = this.values,
    ) {
        this.selectedMode = selectedMode
        this.values = values.copy(
            replaceValue = values.replaceValue?.toList(),
            mergeValue = values.mergeValue?.toList(),
            startValue = values.startValue?.toList(),
            endValue = values.endValue?.toList(),
        )
    }

    fun apply(values: OverrideListModeValues<List<String>>) {
        update(values = values)
        callback?.invoke(this.values)
    }

    fun clear() {
        title = ""
        placeholder = ""
        availableModes = listOf(OverrideListEditorMode.Replace)
        selectedMode = OverrideListEditorMode.Replace
        values = OverrideListModeValues()
        callback = null
    }
}

internal class OverrideRuleEditorSession {
    var title by mutableStateOf("")
    var availableModes by mutableStateOf(listOf(OverrideListEditorMode.Replace))
    var selectedMode by mutableStateOf(OverrideListEditorMode.Replace)
    var values by mutableStateOf(OverrideListModeValues<List<String>>())
    var draftValues by mutableStateOf(OverrideListModeValues<List<OverrideRuleDraft>>())
    var callback: ((OverrideListModeValues<List<String>>) -> Unit)? = null

    fun setup(
        title: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<List<String>>,
        callback: (OverrideListModeValues<List<String>>) -> Unit,
    ) {
        this.title = title
        this.availableModes = availableModes
        this.selectedMode = selectedMode
        this.values = values.copy(
            replaceValue = values.replaceValue?.toList(),
            mergeValue = values.mergeValue?.toList(),
            startValue = values.startValue?.toList(),
            endValue = values.endValue?.toList(),
        )
        draftValues = parseRuleDraftValues(this.values)
        this.callback = callback
    }

    fun update(
        selectedMode: OverrideListEditorMode = this.selectedMode,
        values: OverrideListModeValues<List<String>> = this.values,
    ) {
        this.selectedMode = selectedMode
        this.values = values.copy(
            replaceValue = values.replaceValue?.toList(),
            mergeValue = values.mergeValue?.toList(),
            startValue = values.startValue?.toList(),
            endValue = values.endValue?.toList(),
        )
    }

    fun applyValues(values: OverrideListModeValues<List<String>>) {
        update(values = values)
        callback?.invoke(this.values)
    }

    fun applyDraftValues(values: OverrideListModeValues<List<OverrideRuleDraft>>) {
        val copiedValues = copyRuleDraftValues(values)
        val formattedValues = formatRuleDraftValues(copiedValues)
        draftValues = copiedValues
        this.values = formattedValues
        callback?.invoke(formattedValues)
    }

    fun clear() {
        title = ""
        availableModes = listOf(OverrideListEditorMode.Replace)
        selectedMode = OverrideListEditorMode.Replace
        values = OverrideListModeValues()
        draftValues = OverrideListModeValues()
        callback = null
    }
}

internal class OverrideObjectEditorSession {
    var title by mutableStateOf("")
    var type by mutableStateOf(OverrideStructuredObjectType.Proxies)
    var availableModes by mutableStateOf(listOf(OverrideListEditorMode.Replace))
    var selectedMode by mutableStateOf(OverrideListEditorMode.Replace)
    var values by mutableStateOf(OverrideListModeValues<List<Map<String, JsonElement>>>())
    var proxyDraftValues by mutableStateOf(OverrideListModeValues<List<OverrideProxyDraft>>())
    var proxyGroupDraftValues by mutableStateOf(OverrideListModeValues<List<OverrideProxyGroupDraft>>())
    var callback: ((OverrideListModeValues<List<Map<String, JsonElement>>>) -> Unit)? = null

    fun setup(
        type: OverrideStructuredObjectType,
        title: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<List<Map<String, JsonElement>>>,
        callback: (OverrideListModeValues<List<Map<String, JsonElement>>>) -> Unit,
    ) {
        this.title = title
        this.type = type
        this.availableModes = availableModes
        this.selectedMode = selectedMode
        this.values = values.copy(
            replaceValue = copyOrderedObjectList(values.replaceValue),
            mergeValue = copyOrderedObjectList(values.mergeValue),
            startValue = copyOrderedObjectList(values.startValue),
            endValue = copyOrderedObjectList(values.endValue),
        )
        proxyDraftValues = if (type == OverrideStructuredObjectType.Proxies) {
            parseProxyDraftValues(this.values)
        } else {
            OverrideListModeValues()
        }
        proxyGroupDraftValues = if (type == OverrideStructuredObjectType.ProxyGroups) {
            parseProxyGroupDraftValues(this.values)
        } else {
            OverrideListModeValues()
        }
        this.callback = callback
    }

    fun update(
        selectedMode: OverrideListEditorMode = this.selectedMode,
        values: OverrideListModeValues<List<Map<String, JsonElement>>> = this.values,
    ) {
        this.selectedMode = selectedMode
        this.values = values.copy(
            replaceValue = copyOrderedObjectList(values.replaceValue),
            mergeValue = copyOrderedObjectList(values.mergeValue),
            startValue = copyOrderedObjectList(values.startValue),
            endValue = copyOrderedObjectList(values.endValue),
        )
    }

    fun applyValues(values: OverrideListModeValues<List<Map<String, JsonElement>>>) {
        update(values = values)
        callback?.invoke(this.values)
    }

    fun applyProxyDraftValues(values: OverrideListModeValues<List<OverrideProxyDraft>>) {
        val copiedValues = copyProxyDraftValues(values)
        val formattedValues = formatProxyDraftValues(copiedValues)
        proxyDraftValues = copiedValues
        this.values = formattedValues
        callback?.invoke(formattedValues)
    }

    fun applyProxyDraftModeValue(
        mode: OverrideListEditorMode,
        value: List<OverrideProxyDraft>,
    ) {
        val copiedModeValue = copyProxyDraftList(value).orEmpty()
        proxyDraftValues = proxyDraftValues.update(mode, copiedModeValue)
        val formattedValues = this.values.update(mode, formatProxyDrafts(copiedModeValue))
        this.values = formattedValues
        callback?.invoke(formattedValues)
    }

    fun applyProxyGroupDraftValues(values: OverrideListModeValues<List<OverrideProxyGroupDraft>>) {
        val copiedValues = copyProxyGroupDraftValues(values)
        val formattedValues = formatProxyGroupDraftValues(copiedValues)
        proxyGroupDraftValues = copiedValues
        this.values = formattedValues
        callback?.invoke(formattedValues)
    }

    fun applyProxyGroupDraftModeValue(
        mode: OverrideListEditorMode,
        value: List<OverrideProxyGroupDraft>,
    ) {
        val copiedModeValue = copyProxyGroupDraftList(value).orEmpty()
        proxyGroupDraftValues = proxyGroupDraftValues.update(mode, copiedModeValue)
        val formattedValues = this.values.update(mode, formatProxyGroupDrafts(copiedModeValue))
        this.values = formattedValues
        callback?.invoke(formattedValues)
    }

    fun clear() {
        title = ""
        type = OverrideStructuredObjectType.Proxies
        availableModes = listOf(OverrideListEditorMode.Replace)
        selectedMode = OverrideListEditorMode.Replace
        values = OverrideListModeValues()
        proxyDraftValues = OverrideListModeValues()
        proxyGroupDraftValues = OverrideListModeValues()
        callback = null
    }
}

internal class OverrideKeyedObjectMapEditorSession {
    var title by mutableStateOf("")
    var type by mutableStateOf(OverrideStructuredMapType.RuleProviders)
    var availableModes by mutableStateOf(listOf(OverrideListEditorMode.Replace))
    var selectedMode by mutableStateOf(OverrideListEditorMode.Replace)
    var values by mutableStateOf(
        OverrideListModeValues<Map<String, Map<String, JsonElement>>>(),
        neverEqualPolicy(),
    )
    var draftValues by mutableStateOf(OverrideListModeValues<List<OverrideKeyedObjectDraft>>())
    var callback: ((OverrideListModeValues<Map<String, Map<String, JsonElement>>>) -> Unit)? = null

    fun setup(
        type: OverrideStructuredMapType,
        title: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<Map<String, Map<String, JsonElement>>>,
        callback: (OverrideListModeValues<Map<String, Map<String, JsonElement>>>) -> Unit,
    ) {
        this.title = title
        this.type = type
        this.availableModes = availableModes
        this.selectedMode = selectedMode
        this.values = values.copy(
            replaceValue = copyOrderedObjectMap(values.replaceValue),
            mergeValue = copyOrderedObjectMap(values.mergeValue),
            startValue = copyOrderedObjectMap(values.startValue),
            endValue = copyOrderedObjectMap(values.endValue),
        )
        draftValues = parseKeyedObjectDraftValues(this.values)
        this.callback = callback
    }

    fun update(
        selectedMode: OverrideListEditorMode = this.selectedMode,
        values: OverrideListModeValues<Map<String, Map<String, JsonElement>>> = this.values,
    ) {
        this.selectedMode = selectedMode
        this.values = values.copy(
            replaceValue = copyOrderedObjectMap(values.replaceValue),
            mergeValue = copyOrderedObjectMap(values.mergeValue),
            startValue = copyOrderedObjectMap(values.startValue),
            endValue = copyOrderedObjectMap(values.endValue),
        )
    }

    fun applyValues(values: OverrideListModeValues<Map<String, Map<String, JsonElement>>>) {
        update(values = values)
        callback?.invoke(this.values)
    }

    fun applyDraftValues(values: OverrideListModeValues<List<OverrideKeyedObjectDraft>>) {
        val copiedValues = copyKeyedObjectDraftValues(values)
        val formattedValues = formatKeyedObjectDraftValues(copiedValues)
        draftValues = copiedValues
        this.values = formattedValues
        callback?.invoke(formattedValues)
    }

    fun applyDraftModeValue(
        mode: OverrideListEditorMode,
        value: List<OverrideKeyedObjectDraft>,
    ) {
        val copiedModeValue = copyKeyedObjectDraftList(value).orEmpty()
        draftValues = draftValues.update(mode, copiedModeValue)
        val formattedValues = this.values.update(mode, formatKeyedObjectDrafts(copiedModeValue))
        this.values = formattedValues
        callback?.invoke(formattedValues)
    }

    fun clear() {
        title = ""
        type = OverrideStructuredMapType.RuleProviders
        availableModes = listOf(OverrideListEditorMode.Replace)
        selectedMode = OverrideListEditorMode.Replace
        values = OverrideListModeValues()
        draftValues = OverrideListModeValues()
        callback = null
    }
}

internal class OverrideSubRuleGroupEditorSession {
    var title by mutableStateOf("")
    var availableModes by mutableStateOf(listOf(OverrideListEditorMode.Replace))
    var selectedMode by mutableStateOf(OverrideListEditorMode.Replace)
    var values by mutableStateOf(
        OverrideListModeValues<Map<String, List<String>>>(),
        neverEqualPolicy(),
    )
    var draftValues by mutableStateOf(OverrideListModeValues<List<OverrideSubRuleGroupDraft>>())
    var callback: ((OverrideListModeValues<Map<String, List<String>>>) -> Unit)? = null

    fun setup(
        title: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<Map<String, List<String>>>,
        callback: (OverrideListModeValues<Map<String, List<String>>>) -> Unit,
    ) {
        this.title = title
        this.availableModes = availableModes
        this.selectedMode = selectedMode
        this.values = values.copy(
            replaceValue = copyOrderedSubRuleMap(values.replaceValue),
            mergeValue = copyOrderedSubRuleMap(values.mergeValue),
            startValue = copyOrderedSubRuleMap(values.startValue),
            endValue = copyOrderedSubRuleMap(values.endValue),
        )
        draftValues = parseSubRuleGroupDraftValues(this.values)
        this.callback = callback
    }

    fun update(
        selectedMode: OverrideListEditorMode = this.selectedMode,
        values: OverrideListModeValues<Map<String, List<String>>> = this.values,
    ) {
        this.selectedMode = selectedMode
        this.values = values.copy(
            replaceValue = copyOrderedSubRuleMap(values.replaceValue),
            mergeValue = copyOrderedSubRuleMap(values.mergeValue),
            startValue = copyOrderedSubRuleMap(values.startValue),
            endValue = copyOrderedSubRuleMap(values.endValue),
        )
    }

    fun applyValues(values: OverrideListModeValues<Map<String, List<String>>>) {
        update(values = values)
        callback?.invoke(this.values)
    }

    fun applyDraftValues(values: OverrideListModeValues<List<OverrideSubRuleGroupDraft>>) {
        val copiedValues = copySubRuleGroupDraftValues(values)
        val formattedValues = formatSubRuleGroupDraftValues(copiedValues)
        draftValues = copiedValues
        this.values = formattedValues
        callback?.invoke(formattedValues)
    }

    fun clear() {
        title = ""
        availableModes = listOf(OverrideListEditorMode.Replace)
        selectedMode = OverrideListEditorMode.Replace
        values = OverrideListModeValues()
        draftValues = OverrideListModeValues()
        callback = null
    }
}

internal open class OverrideDraftEditorSession<T>(
    private val cloneValue: (T?) -> T?,
) {
    var title by mutableStateOf("")
    var value: T? by mutableStateOf(null)
    var callback: ((T) -> Unit)? = null

    open fun setup(
        title: String,
        value: T?,
        callback: (T) -> Unit,
    ) {
        this.title = title
        update(value)
        this.callback = callback
    }

    fun update(value: T?) {
        this.value = cloneValue(value)
    }

    fun submit(value: T) {
        update(value)
        callback?.invoke(this.value ?: value)
    }

    open fun clear() {
        title = ""
        value = null
        callback = null
    }
}

internal class OverrideTypedDraftEditorSession<K, T>(
    private val defaultType: K,
    cloneValue: (T?) -> T?,
) : OverrideDraftEditorSession<T>(cloneValue) {
    var type by mutableStateOf(defaultType)

    fun setup(
        type: K,
        title: String,
        value: T?,
        callback: (T) -> Unit,
    ) {
        this.type = type
        super.setup(title, value, callback)
    }

    override fun clear() {
        type = defaultType
        super.clear()
    }
}
