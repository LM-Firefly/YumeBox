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

object OverrideStructuredEditorStore {
    var referenceCatalog: OverrideReferenceCatalog by mutableStateOf(OverrideReferenceCatalog())

    var configPreviewTitle: String by mutableStateOf("")
    var configPreviewContent: String by mutableStateOf("")
    var configPreviewLanguage: LanguageScope by mutableStateOf(LanguageScope.Json)
    var configPreviewCallback: ((String) -> Unit)? = null

    var stringListEditorTitle: String by mutableStateOf("")
    var stringListEditorPlaceholder: String by mutableStateOf("")
    var stringListEditorAvailableModes: List<OverrideListEditorMode> by mutableStateOf(listOf(OverrideListEditorMode.Replace))
    var stringListEditorSelectedMode: OverrideListEditorMode by mutableStateOf(OverrideListEditorMode.Replace)
    var stringListEditorValues: OverrideListModeValues<List<String>> by mutableStateOf(OverrideListModeValues())
    var stringListEditorCallback: ((OverrideListModeValues<List<String>>) -> Unit)? = null

    var ruleEditorTitle: String by mutableStateOf("")
    var ruleEditorAvailableModes: List<OverrideListEditorMode> by mutableStateOf(listOf(OverrideListEditorMode.Replace))
    var ruleEditorSelectedMode: OverrideListEditorMode by mutableStateOf(OverrideListEditorMode.Replace)
    var ruleEditorValues: OverrideListModeValues<List<String>> by mutableStateOf(OverrideListModeValues())
    var ruleEditorDraftValues: OverrideListModeValues<List<OverrideRuleDraft>> by mutableStateOf(OverrideListModeValues())
    var ruleEditorCallback: ((OverrideListModeValues<List<String>>) -> Unit)? = null

    var objectEditorTitle: String by mutableStateOf("")
    var objectEditorType: OverrideStructuredObjectType by mutableStateOf(OverrideStructuredObjectType.Proxies)
    var objectEditorAvailableModes: List<OverrideListEditorMode> by mutableStateOf(listOf(OverrideListEditorMode.Replace))
    var objectEditorSelectedMode: OverrideListEditorMode by mutableStateOf(OverrideListEditorMode.Replace)
    var objectEditorValues: OverrideListModeValues<List<Map<String, JsonElement>>> by mutableStateOf(OverrideListModeValues())
    var objectEditorProxyDraftValues: OverrideListModeValues<List<OverrideProxyDraft>> by mutableStateOf(OverrideListModeValues())
    var objectEditorProxyGroupDraftValues: OverrideListModeValues<List<OverrideProxyGroupDraft>> by mutableStateOf(OverrideListModeValues())
    var objectEditorCallback: ((OverrideListModeValues<List<Map<String, JsonElement>>>) -> Unit)? = null

    var keyedObjectMapEditorTitle: String by mutableStateOf("")
    var keyedObjectMapEditorType: OverrideStructuredMapType by mutableStateOf(OverrideStructuredMapType.RuleProviders)
    var keyedObjectMapEditorAvailableModes: List<OverrideListEditorMode> by mutableStateOf(listOf(OverrideListEditorMode.Replace))
    var keyedObjectMapEditorSelectedMode: OverrideListEditorMode by mutableStateOf(OverrideListEditorMode.Replace)
    var keyedObjectMapEditorValues: OverrideListModeValues<Map<String, Map<String, JsonElement>>> by mutableStateOf(
        OverrideListModeValues(),
        neverEqualPolicy(),
    )
    var keyedObjectMapEditorDraftValues: OverrideListModeValues<List<OverrideKeyedObjectDraft>> by mutableStateOf(OverrideListModeValues())
    var keyedObjectMapEditorCallback: ((OverrideListModeValues<Map<String, Map<String, JsonElement>>>) -> Unit)? = null

    var subRuleGroupEditorTitle: String by mutableStateOf("")
    var subRuleGroupEditorAvailableModes: List<OverrideListEditorMode> by mutableStateOf(listOf(OverrideListEditorMode.Replace))
    var subRuleGroupEditorSelectedMode: OverrideListEditorMode by mutableStateOf(OverrideListEditorMode.Replace)
    var subRuleGroupEditorValues: OverrideListModeValues<Map<String, List<String>>> by mutableStateOf(
        OverrideListModeValues(),
        neverEqualPolicy(),
    )
    var subRuleGroupEditorDraftValues: OverrideListModeValues<List<OverrideSubRuleGroupDraft>> by mutableStateOf(OverrideListModeValues())
    var subRuleGroupEditorCallback: ((OverrideListModeValues<Map<String, List<String>>>) -> Unit)? = null

    var ruleDraftEditorTitle: String by mutableStateOf("")
    var ruleDraftEditorValue: OverrideRuleDraft? by mutableStateOf(null)
    var ruleDraftEditorCallback: ((OverrideRuleDraft) -> Unit)? = null

    var proxyDraftEditorTitle: String by mutableStateOf("")
    var proxyDraftEditorValue: OverrideProxyDraft? by mutableStateOf(null)
    var proxyDraftEditorCallback: ((OverrideProxyDraft) -> Unit)? = null

    var proxyGroupDraftEditorTitle: String by mutableStateOf("")
    var proxyGroupDraftEditorValue: OverrideProxyGroupDraft? by mutableStateOf(null)
    var proxyGroupDraftEditorCallback: ((OverrideProxyGroupDraft) -> Unit)? = null

    var keyedObjectDraftEditorTitle: String by mutableStateOf("")
    var keyedObjectDraftEditorType: OverrideStructuredMapType by mutableStateOf(OverrideStructuredMapType.RuleProviders)
    var keyedObjectDraftEditorValue: OverrideKeyedObjectDraft? by mutableStateOf(null)
    var keyedObjectDraftEditorCallback: ((OverrideKeyedObjectDraft) -> Unit)? = null

    var subRuleDraftEditorTitle: String by mutableStateOf("")
    var subRuleDraftEditorValue: OverrideSubRuleGroupDraft? by mutableStateOf(null)
    var subRuleDraftEditorCallback: ((OverrideSubRuleGroupDraft) -> Unit)? = null

    private fun copyOrderedObjectList(
        value: List<Map<String, JsonElement>>?,
    ): List<Map<String, JsonElement>>? {
        return value?.map(::toOrderedJsonElementMap)
    }

    private fun copyOrderedObjectMap(
        value: Map<String, Map<String, JsonElement>>?,
    ): Map<String, Map<String, JsonElement>>? {
        return toOrderedObjectMap(value)
    }

    private fun copyOrderedSubRuleMap(
        value: Map<String, List<String>>?,
    ): Map<String, List<String>>? {
        return toOrderedSubRuleMap(value)
    }

    private fun withUpdatedProxyGroupDraft(
        values: OverrideListModeValues<List<OverrideProxyGroupDraft>>,
        selectedMode: OverrideListEditorMode,
        draft: OverrideProxyGroupDraft?,
    ): OverrideListModeValues<List<OverrideProxyGroupDraft>> {
        if (draft == null) {
            return values
        }
        var draftMatched = false
        val updatedValues = OverrideListModeValues(
            replaceValue = values.replaceValue?.map { current ->
                if (current.uiId == draft.uiId) {
                    draftMatched = true
                    draft
                } else {
                    current
                }
            },
            mergeValue = values.mergeValue?.map { current ->
                if (current.uiId == draft.uiId) {
                    draftMatched = true
                    draft
                } else {
                    current
                }
            },
            startValue = values.startValue?.map { current ->
                if (current.uiId == draft.uiId) {
                    draftMatched = true
                    draft
                } else {
                    current
                }
            },
            endValue = values.endValue?.map { current ->
                if (current.uiId == draft.uiId) {
                    draftMatched = true
                    draft
                } else {
                    current
                }
            },
        )
        if (draftMatched) {
            return updatedValues
        }
        return updatedValues.update(
            selectedMode,
            updatedValues.valueFor(selectedMode).orEmpty() + draft,
        )
    }

    private fun withUpdatedSubRuleDraft(
        values: OverrideListModeValues<List<OverrideSubRuleGroupDraft>>,
        selectedMode: OverrideListEditorMode,
        draft: OverrideSubRuleGroupDraft?,
    ): OverrideListModeValues<List<OverrideSubRuleGroupDraft>> {
        if (draft == null) {
            return values
        }
        var draftMatched = false
        val updatedValues = OverrideListModeValues(
            replaceValue = values.replaceValue?.map { current ->
                if (current.uiId == draft.uiId) {
                    draftMatched = true
                    draft
                } else {
                    current
                }
            },
            mergeValue = values.mergeValue?.map { current ->
                if (current.uiId == draft.uiId) {
                    draftMatched = true
                    draft
                } else {
                    current
                }
            },
            startValue = values.startValue?.map { current ->
                if (current.uiId == draft.uiId) {
                    draftMatched = true
                    draft
                } else {
                    current
                }
            },
            endValue = values.endValue?.map { current ->
                if (current.uiId == draft.uiId) {
                    draftMatched = true
                    draft
                } else {
                    current
                }
            },
        )
        if (draftMatched) {
            return updatedValues
        }
        return updatedValues.update(
            selectedMode,
            updatedValues.valueFor(selectedMode).orEmpty() + draft,
        )
    }

    private fun copyRuleDraftList(
        value: List<OverrideRuleDraft>?,
    ): List<OverrideRuleDraft>? {
        return value?.map { draft ->
            draft.copy(
                extras = draft.extras.toList(),
            )
        }
    }

    private fun copyProxyDraftList(
        value: List<OverrideProxyDraft>?,
    ): List<OverrideProxyDraft>? {
        return value?.map { draft ->
            draft.copy(
                extraFields = toOrderedJsonElementMap(draft.extraFields),
            )
        }
    }

    private fun copyProxyGroupDraftList(
        value: List<OverrideProxyGroupDraft>?,
    ): List<OverrideProxyGroupDraft>? {
        return value?.map { draft ->
            draft.copy(
                proxies = draft.proxies.toList(),
                use = draft.use.toList(),
                extraFields = toOrderedJsonElementMap(draft.extraFields),
            )
        }
    }

    private fun copyKeyedObjectDraftList(
        value: List<OverrideKeyedObjectDraft>?,
    ): List<OverrideKeyedObjectDraft>? {
        return value?.map { draft ->
            draft.copy(
                fields = toOrderedJsonElementMap(draft.fields),
            )
        }
    }

    private fun copySubRuleGroupDraftList(
        value: List<OverrideSubRuleGroupDraft>?,
    ): List<OverrideSubRuleGroupDraft>? {
        return value?.map { draft ->
            draft.copy(
                rules = draft.rules.toList(),
            )
        }
    }

    private fun copyRuleDraftValues(
        value: OverrideListModeValues<List<OverrideRuleDraft>>,
    ): OverrideListModeValues<List<OverrideRuleDraft>> {
        return OverrideListModeValues(
            replaceValue = copyRuleDraftList(value.replaceValue),
            mergeValue = copyRuleDraftList(value.mergeValue),
            startValue = copyRuleDraftList(value.startValue),
            endValue = copyRuleDraftList(value.endValue),
        )
    }

    private fun copyProxyDraftValues(
        value: OverrideListModeValues<List<OverrideProxyDraft>>,
    ): OverrideListModeValues<List<OverrideProxyDraft>> {
        return OverrideListModeValues(
            replaceValue = copyProxyDraftList(value.replaceValue),
            mergeValue = copyProxyDraftList(value.mergeValue),
            startValue = copyProxyDraftList(value.startValue),
            endValue = copyProxyDraftList(value.endValue),
        )
    }

    private fun copyProxyGroupDraftValues(
        value: OverrideListModeValues<List<OverrideProxyGroupDraft>>,
    ): OverrideListModeValues<List<OverrideProxyGroupDraft>> {
        return OverrideListModeValues(
            replaceValue = copyProxyGroupDraftList(value.replaceValue),
            mergeValue = copyProxyGroupDraftList(value.mergeValue),
            startValue = copyProxyGroupDraftList(value.startValue),
            endValue = copyProxyGroupDraftList(value.endValue),
        )
    }

    private fun copyKeyedObjectDraftValues(
        value: OverrideListModeValues<List<OverrideKeyedObjectDraft>>,
    ): OverrideListModeValues<List<OverrideKeyedObjectDraft>> {
        return OverrideListModeValues(
            replaceValue = copyKeyedObjectDraftList(value.replaceValue),
            mergeValue = copyKeyedObjectDraftList(value.mergeValue),
            startValue = copyKeyedObjectDraftList(value.startValue),
            endValue = copyKeyedObjectDraftList(value.endValue),
        )
    }

    private fun copySubRuleGroupDraftValues(
        value: OverrideListModeValues<List<OverrideSubRuleGroupDraft>>,
    ): OverrideListModeValues<List<OverrideSubRuleGroupDraft>> {
        return OverrideListModeValues(
            replaceValue = copySubRuleGroupDraftList(value.replaceValue),
            mergeValue = copySubRuleGroupDraftList(value.mergeValue),
            startValue = copySubRuleGroupDraftList(value.startValue),
            endValue = copySubRuleGroupDraftList(value.endValue),
        )
    }

    private fun parseRuleDraftValues(
        value: OverrideListModeValues<List<String>>,
    ): OverrideListModeValues<List<OverrideRuleDraft>> {
        return OverrideListModeValues(
            replaceValue = value.replaceValue?.let(::parseRuleDrafts),
            mergeValue = value.mergeValue?.let(::parseRuleDrafts),
            startValue = value.startValue?.let(::parseRuleDrafts),
            endValue = value.endValue?.let(::parseRuleDrafts),
        )
    }

    private fun parseProxyDraftValues(
        value: OverrideListModeValues<List<Map<String, JsonElement>>>,
    ): OverrideListModeValues<List<OverrideProxyDraft>> {
        return OverrideListModeValues(
            replaceValue = value.replaceValue?.let(::parseProxyDrafts),
            mergeValue = value.mergeValue?.let(::parseProxyDrafts),
            startValue = value.startValue?.let(::parseProxyDrafts),
            endValue = value.endValue?.let(::parseProxyDrafts),
        )
    }

    private fun parseProxyGroupDraftValues(
        value: OverrideListModeValues<List<Map<String, JsonElement>>>,
    ): OverrideListModeValues<List<OverrideProxyGroupDraft>> {
        return OverrideListModeValues(
            replaceValue = value.replaceValue?.let(::parseProxyGroupDrafts),
            mergeValue = value.mergeValue?.let(::parseProxyGroupDrafts),
            startValue = value.startValue?.let(::parseProxyGroupDrafts),
            endValue = value.endValue?.let(::parseProxyGroupDrafts),
        )
    }

    private fun parseKeyedObjectDraftValues(
        value: OverrideListModeValues<Map<String, Map<String, JsonElement>>>,
    ): OverrideListModeValues<List<OverrideKeyedObjectDraft>> {
        return OverrideListModeValues(
            replaceValue = value.replaceValue?.let(::parseKeyedObjectDrafts),
            mergeValue = value.mergeValue?.let(::parseKeyedObjectDrafts),
            startValue = value.startValue?.let(::parseKeyedObjectDrafts),
            endValue = value.endValue?.let(::parseKeyedObjectDrafts),
        )
    }

    private fun parseSubRuleGroupDraftValues(
        value: OverrideListModeValues<Map<String, List<String>>>,
    ): OverrideListModeValues<List<OverrideSubRuleGroupDraft>> {
        return OverrideListModeValues(
            replaceValue = value.replaceValue?.let(::parseSubRuleGroupDrafts),
            mergeValue = value.mergeValue?.let(::parseSubRuleGroupDrafts),
            startValue = value.startValue?.let(::parseSubRuleGroupDrafts),
            endValue = value.endValue?.let(::parseSubRuleGroupDrafts),
        )
    }

    private fun formatRuleDraftValues(
        value: OverrideListModeValues<List<OverrideRuleDraft>>,
    ): OverrideListModeValues<List<String>> {
        return OverrideListModeValues(
            replaceValue = value.replaceValue?.let(::formatRuleDrafts),
            mergeValue = value.mergeValue?.let(::formatRuleDrafts),
            startValue = value.startValue?.let(::formatRuleDrafts),
            endValue = value.endValue?.let(::formatRuleDrafts),
        )
    }

    private fun formatProxyDraftValues(
        value: OverrideListModeValues<List<OverrideProxyDraft>>,
    ): OverrideListModeValues<List<Map<String, JsonElement>>> {
        return OverrideListModeValues(
            replaceValue = value.replaceValue?.let(::formatProxyDrafts),
            mergeValue = value.mergeValue?.let(::formatProxyDrafts),
            startValue = value.startValue?.let(::formatProxyDrafts),
            endValue = value.endValue?.let(::formatProxyDrafts),
        )
    }

    private fun formatProxyGroupDraftValues(
        value: OverrideListModeValues<List<OverrideProxyGroupDraft>>,
    ): OverrideListModeValues<List<Map<String, JsonElement>>> {
        return OverrideListModeValues(
            replaceValue = value.replaceValue?.let(::formatProxyGroupDrafts),
            mergeValue = value.mergeValue?.let(::formatProxyGroupDrafts),
            startValue = value.startValue?.let(::formatProxyGroupDrafts),
            endValue = value.endValue?.let(::formatProxyGroupDrafts),
        )
    }

    private fun formatKeyedObjectDraftValues(
        value: OverrideListModeValues<List<OverrideKeyedObjectDraft>>,
    ): OverrideListModeValues<Map<String, Map<String, JsonElement>>> {
        return OverrideListModeValues(
            replaceValue = value.replaceValue?.let(::formatKeyedObjectDrafts),
            mergeValue = value.mergeValue?.let(::formatKeyedObjectDrafts),
            startValue = value.startValue?.let(::formatKeyedObjectDrafts),
            endValue = value.endValue?.let(::formatKeyedObjectDrafts),
        )
    }

    private fun formatSubRuleGroupDraftValues(
        value: OverrideListModeValues<List<OverrideSubRuleGroupDraft>>,
    ): OverrideListModeValues<Map<String, List<String>>> {
        return OverrideListModeValues(
            replaceValue = value.replaceValue?.let(::formatSubRuleGroupDrafts),
            mergeValue = value.mergeValue?.let(::formatSubRuleGroupDrafts),
            startValue = value.startValue?.let(::formatSubRuleGroupDrafts),
            endValue = value.endValue?.let(::formatSubRuleGroupDrafts),
        )
    }

    fun setupStringListEditor(
        title: String,
        placeholder: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<List<String>>,
        callback: (OverrideListModeValues<List<String>>) -> Unit,
    ) {
        stringListEditorTitle = title
        stringListEditorPlaceholder = placeholder
        stringListEditorAvailableModes = availableModes
        stringListEditorSelectedMode = selectedMode
        stringListEditorValues = values.copy(
            replaceValue = values.replaceValue?.toList(),
            mergeValue = values.mergeValue?.toList(),
            startValue = values.startValue?.toList(),
            endValue = values.endValue?.toList(),
        )
        stringListEditorCallback = callback
    }

    fun updateStringListEditorSession(
        selectedMode: OverrideListEditorMode = stringListEditorSelectedMode,
        values: OverrideListModeValues<List<String>> = stringListEditorValues,
    ) {
        stringListEditorSelectedMode = selectedMode
        stringListEditorValues = values.copy(
            replaceValue = values.replaceValue?.toList(),
            mergeValue = values.mergeValue?.toList(),
            startValue = values.startValue?.toList(),
            endValue = values.endValue?.toList(),
        )
    }

    fun setupRuleEditor(
        title: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<List<String>>,
        referenceCatalog: OverrideReferenceCatalog,
        callback: (OverrideListModeValues<List<String>>) -> Unit,
    ) {
        this.referenceCatalog = referenceCatalog
        ruleEditorTitle = title
        ruleEditorAvailableModes = availableModes
        ruleEditorSelectedMode = selectedMode
        ruleEditorValues = values.copy(
            replaceValue = values.replaceValue?.toList(),
            mergeValue = values.mergeValue?.toList(),
            startValue = values.startValue?.toList(),
            endValue = values.endValue?.toList(),
        )
        ruleEditorDraftValues = parseRuleDraftValues(ruleEditorValues)
        ruleEditorCallback = callback
    }

    fun updateRuleEditorSession(
        selectedMode: OverrideListEditorMode = ruleEditorSelectedMode,
        values: OverrideListModeValues<List<String>> = ruleEditorValues,
    ) {
        ruleEditorSelectedMode = selectedMode
        ruleEditorValues = values.copy(
            replaceValue = values.replaceValue?.toList(),
            mergeValue = values.mergeValue?.toList(),
            startValue = values.startValue?.toList(),
            endValue = values.endValue?.toList(),
        )
    }

    fun setupObjectEditor(
        type: OverrideStructuredObjectType,
        title: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<List<Map<String, JsonElement>>>,
        referenceCatalog: OverrideReferenceCatalog,
        callback: (OverrideListModeValues<List<Map<String, JsonElement>>>) -> Unit,
    ) {
        this.referenceCatalog = referenceCatalog
        objectEditorTitle = title
        objectEditorType = type
        objectEditorAvailableModes = availableModes
        objectEditorSelectedMode = selectedMode
        objectEditorValues = values.copy(
            replaceValue = copyOrderedObjectList(values.replaceValue),
            mergeValue = copyOrderedObjectList(values.mergeValue),
            startValue = copyOrderedObjectList(values.startValue),
            endValue = copyOrderedObjectList(values.endValue),
        )
        objectEditorProxyDraftValues = if (type == OverrideStructuredObjectType.Proxies) {
            parseProxyDraftValues(objectEditorValues)
        } else {
            OverrideListModeValues()
        }
        objectEditorProxyGroupDraftValues = if (type == OverrideStructuredObjectType.ProxyGroups) {
            parseProxyGroupDraftValues(objectEditorValues)
        } else {
            OverrideListModeValues()
        }
        objectEditorCallback = callback
    }

    fun updateObjectEditorSession(
        selectedMode: OverrideListEditorMode = objectEditorSelectedMode,
        values: OverrideListModeValues<List<Map<String, JsonElement>>> = objectEditorValues,
    ) {
        objectEditorSelectedMode = selectedMode
        objectEditorValues = values.copy(
            replaceValue = copyOrderedObjectList(values.replaceValue),
            mergeValue = copyOrderedObjectList(values.mergeValue),
            startValue = copyOrderedObjectList(values.startValue),
            endValue = copyOrderedObjectList(values.endValue),
        )
    }

    fun setupKeyedObjectMapEditor(
        type: OverrideStructuredMapType,
        title: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<Map<String, Map<String, JsonElement>>>,
        callback: (OverrideListModeValues<Map<String, Map<String, JsonElement>>>) -> Unit,
    ) {
        keyedObjectMapEditorTitle = title
        keyedObjectMapEditorType = type
        keyedObjectMapEditorAvailableModes = availableModes
        keyedObjectMapEditorSelectedMode = selectedMode
        keyedObjectMapEditorValues = values.copy(
            replaceValue = copyOrderedObjectMap(values.replaceValue),
            mergeValue = copyOrderedObjectMap(values.mergeValue),
            startValue = copyOrderedObjectMap(values.startValue),
            endValue = copyOrderedObjectMap(values.endValue),
        )
        keyedObjectMapEditorDraftValues = parseKeyedObjectDraftValues(keyedObjectMapEditorValues)
        keyedObjectMapEditorCallback = callback
    }

    fun updateKeyedObjectMapEditorSession(
        selectedMode: OverrideListEditorMode = keyedObjectMapEditorSelectedMode,
        values: OverrideListModeValues<Map<String, Map<String, JsonElement>>> = keyedObjectMapEditorValues,
    ) {
        keyedObjectMapEditorSelectedMode = selectedMode
        keyedObjectMapEditorValues = values.copy(
            replaceValue = copyOrderedObjectMap(values.replaceValue),
            mergeValue = copyOrderedObjectMap(values.mergeValue),
            startValue = copyOrderedObjectMap(values.startValue),
            endValue = copyOrderedObjectMap(values.endValue),
        )
    }

    fun setupSubRuleGroupEditor(
        title: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<Map<String, List<String>>>,
        referenceCatalog: OverrideReferenceCatalog,
        callback: (OverrideListModeValues<Map<String, List<String>>>) -> Unit,
    ) {
        this.referenceCatalog = referenceCatalog
        subRuleGroupEditorTitle = title
        subRuleGroupEditorAvailableModes = availableModes
        subRuleGroupEditorSelectedMode = selectedMode
        subRuleGroupEditorValues = values.copy(
            replaceValue = copyOrderedSubRuleMap(values.replaceValue),
            mergeValue = copyOrderedSubRuleMap(values.mergeValue),
            startValue = copyOrderedSubRuleMap(values.startValue),
            endValue = copyOrderedSubRuleMap(values.endValue),
        )
        subRuleGroupEditorDraftValues = parseSubRuleGroupDraftValues(subRuleGroupEditorValues)
        subRuleGroupEditorCallback = callback
    }

    fun updateSubRuleGroupEditorSession(
        selectedMode: OverrideListEditorMode = subRuleGroupEditorSelectedMode,
        values: OverrideListModeValues<Map<String, List<String>>> = subRuleGroupEditorValues,
    ) {
        subRuleGroupEditorSelectedMode = selectedMode
        subRuleGroupEditorValues = values.copy(
            replaceValue = copyOrderedSubRuleMap(values.replaceValue),
            mergeValue = copyOrderedSubRuleMap(values.mergeValue),
            startValue = copyOrderedSubRuleMap(values.startValue),
            endValue = copyOrderedSubRuleMap(values.endValue),
        )
    }

    fun applyStringListValues(
        values: OverrideListModeValues<List<String>>,
    ) {
        updateStringListEditorSession(values = values)
        stringListEditorCallback?.invoke(stringListEditorValues)
    }

    fun applyRuleValues(
        values: OverrideListModeValues<List<String>>,
    ) {
        updateRuleEditorSession(values = values)
        ruleEditorCallback?.invoke(ruleEditorValues)
    }

    fun applyRuleDraftValues(
        values: OverrideListModeValues<List<OverrideRuleDraft>>,
    ) {
        val copiedValues = copyRuleDraftValues(values)
        val formattedValues = formatRuleDraftValues(copiedValues)
        ruleEditorDraftValues = copiedValues
        ruleEditorValues = formattedValues
        ruleEditorCallback?.invoke(formattedValues)
    }

    fun applyObjectValues(
        values: OverrideListModeValues<List<Map<String, JsonElement>>>,
    ) {
        updateObjectEditorSession(values = values)
        objectEditorCallback?.invoke(objectEditorValues)
    }

    fun applyProxyDraftValues(
        values: OverrideListModeValues<List<OverrideProxyDraft>>,
    ) {
        val copiedValues = copyProxyDraftValues(values)
        val formattedValues = formatProxyDraftValues(copiedValues)
        objectEditorProxyDraftValues = copiedValues
        objectEditorValues = formattedValues
        objectEditorCallback?.invoke(formattedValues)
    }

    fun applyProxyGroupDraftValues(
        values: OverrideListModeValues<List<OverrideProxyGroupDraft>>,
    ) {
        val copiedValues = copyProxyGroupDraftValues(values)
        val formattedValues = formatProxyGroupDraftValues(copiedValues)
        objectEditorProxyGroupDraftValues = copiedValues
        objectEditorValues = formattedValues
        objectEditorCallback?.invoke(formattedValues)
    }

    fun applyKeyedObjectValues(
        values: OverrideListModeValues<Map<String, Map<String, JsonElement>>>,
    ) {
        updateKeyedObjectMapEditorSession(values = values)
        keyedObjectMapEditorCallback?.invoke(keyedObjectMapEditorValues)
    }

    fun applyKeyedObjectDraftValues(
        values: OverrideListModeValues<List<OverrideKeyedObjectDraft>>,
    ) {
        val copiedValues = copyKeyedObjectDraftValues(values)
        val formattedValues = formatKeyedObjectDraftValues(copiedValues)
        keyedObjectMapEditorDraftValues = copiedValues
        keyedObjectMapEditorValues = formattedValues
        keyedObjectMapEditorCallback?.invoke(formattedValues)
    }

    fun applySubRuleValues(
        values: OverrideListModeValues<Map<String, List<String>>>,
    ) {
        updateSubRuleGroupEditorSession(values = values)
        subRuleGroupEditorCallback?.invoke(subRuleGroupEditorValues)
    }

    fun applySubRuleDraftValues(
        values: OverrideListModeValues<List<OverrideSubRuleGroupDraft>>,
    ) {
        val copiedValues = copySubRuleGroupDraftValues(values)
        val formattedValues = formatSubRuleGroupDraftValues(copiedValues)
        subRuleGroupEditorDraftValues = copiedValues
        subRuleGroupEditorValues = formattedValues
        subRuleGroupEditorCallback?.invoke(formattedValues)
    }

    fun setupRuleDraftEditor(
        title: String,
        value: OverrideRuleDraft?,
        callback: (OverrideRuleDraft) -> Unit,
    ) {
        ruleDraftEditorTitle = title
        ruleDraftEditorValue = value?.copy(
            extras = value.extras.toList(),
        )
        ruleDraftEditorCallback = callback
    }

    fun updateRuleDraftEditorSession(value: OverrideRuleDraft?) {
        ruleDraftEditorValue = value?.copy(
            extras = value.extras.toList(),
        )
    }

    fun setupProxyDraftEditor(
        title: String,
        value: OverrideProxyDraft?,
        callback: (OverrideProxyDraft) -> Unit,
    ) {
        proxyDraftEditorTitle = title
        proxyDraftEditorValue = value?.copy(
            extraFields = toOrderedJsonElementMap(value.extraFields),
        )
        proxyDraftEditorCallback = callback
    }

    fun updateProxyDraftEditorSession(value: OverrideProxyDraft?) {
        proxyDraftEditorValue = value?.copy(
            extraFields = toOrderedJsonElementMap(value.extraFields),
        )
    }

    fun setupProxyGroupDraftEditor(
        title: String,
        value: OverrideProxyGroupDraft?,
        callback: (OverrideProxyGroupDraft) -> Unit,
    ) {
        proxyGroupDraftEditorTitle = title
        proxyGroupDraftEditorValue = value?.copy(
            proxies = value.proxies.toList(),
            use = value.use.toList(),
            extraFields = toOrderedJsonElementMap(value.extraFields),
        )
        proxyGroupDraftEditorCallback = callback
    }

    fun updateProxyGroupDraftEditorSession(value: OverrideProxyGroupDraft?) {
        proxyGroupDraftEditorValue = value?.copy(
            proxies = value.proxies.toList(),
            use = value.use.toList(),
            extraFields = toOrderedJsonElementMap(value.extraFields),
        )
    }

    fun setupKeyedObjectDraftEditor(
        type: OverrideStructuredMapType,
        title: String,
        value: OverrideKeyedObjectDraft?,
        callback: (OverrideKeyedObjectDraft) -> Unit,
    ) {
        keyedObjectDraftEditorTitle = title
        keyedObjectDraftEditorType = type
        keyedObjectDraftEditorValue = value?.copy(
            fields = toOrderedJsonElementMap(value.fields),
        )
        keyedObjectDraftEditorCallback = callback
    }

    fun updateKeyedObjectDraftEditorSession(value: OverrideKeyedObjectDraft?) {
        keyedObjectDraftEditorValue = value?.copy(
            fields = toOrderedJsonElementMap(value.fields),
        )
    }

    fun setupSubRuleDraftEditor(
        title: String,
        value: OverrideSubRuleGroupDraft?,
        callback: (OverrideSubRuleGroupDraft) -> Unit,
    ) {
        subRuleDraftEditorTitle = title
        subRuleDraftEditorValue = value?.copy(
            rules = value.rules.toList(),
        )
        subRuleDraftEditorCallback = callback
    }

    fun updateSubRuleDraftEditorSession(value: OverrideSubRuleGroupDraft?) {
        subRuleDraftEditorValue = value?.copy(
            rules = value.rules.toList(),
        )
    }

    fun currentReferenceCatalog(): OverrideReferenceCatalog {
        var currentCatalog = referenceCatalog

        if (objectEditorCallback != null) {
            currentCatalog = when (objectEditorType) {
                OverrideStructuredObjectType.Proxies -> currentCatalog.copy(
                    proxyNames = collectProxyNames(objectEditorProxyDraftValues),
                )

                OverrideStructuredObjectType.ProxyGroups -> currentCatalog.copy(
                    proxyGroupNames = collectProxyGroupNames(
                        withUpdatedProxyGroupDraft(
                            values = objectEditorProxyGroupDraftValues,
                            selectedMode = objectEditorSelectedMode,
                            draft = proxyGroupDraftEditorValue,
                        ),
                    ),
                )
            }
        }

        if (subRuleGroupEditorCallback != null) {
            currentCatalog = currentCatalog.copy(
                subRuleNames = collectSubRuleNames(
                    withUpdatedSubRuleDraft(
                        values = subRuleGroupEditorDraftValues,
                        selectedMode = subRuleGroupEditorSelectedMode,
                        draft = subRuleDraftEditorValue,
                    ),
                ),
            )
        }

        if (
            keyedObjectMapEditorCallback != null &&
            keyedObjectMapEditorType == OverrideStructuredMapType.RuleProviders
        ) {
            currentCatalog = currentCatalog.copy(
                ruleProviderNames = collectRuleProviderNames(keyedObjectMapEditorDraftValues),
            )
        }

        return currentCatalog
    }

    fun submitRuleDraft(value: OverrideRuleDraft) {
        updateRuleDraftEditorSession(value)
        ruleDraftEditorCallback?.invoke(ruleDraftEditorValue ?: value)
    }

    fun submitProxyDraft(value: OverrideProxyDraft) {
        updateProxyDraftEditorSession(value)
        proxyDraftEditorCallback?.invoke(proxyDraftEditorValue ?: value)
    }

    fun submitProxyGroupDraft(value: OverrideProxyGroupDraft) {
        updateProxyGroupDraftEditorSession(value)
        proxyGroupDraftEditorCallback?.invoke(proxyGroupDraftEditorValue ?: value)
    }

    fun submitKeyedObjectDraft(value: OverrideKeyedObjectDraft) {
        updateKeyedObjectDraftEditorSession(value)
        keyedObjectDraftEditorCallback?.invoke(keyedObjectDraftEditorValue ?: value)
    }

    fun submitSubRuleDraft(value: OverrideSubRuleGroupDraft) {
        updateSubRuleDraftEditorSession(value)
        subRuleDraftEditorCallback?.invoke(subRuleDraftEditorValue ?: value)
    }

    fun clearStringListEditor() {
        stringListEditorTitle = ""
        stringListEditorPlaceholder = ""
        stringListEditorAvailableModes = listOf(OverrideListEditorMode.Replace)
        stringListEditorSelectedMode = OverrideListEditorMode.Replace
        stringListEditorValues = OverrideListModeValues()
        stringListEditorCallback = null
    }

    fun clearRuleEditor() {
        ruleEditorTitle = ""
        ruleEditorAvailableModes = listOf(OverrideListEditorMode.Replace)
        ruleEditorSelectedMode = OverrideListEditorMode.Replace
        ruleEditorValues = OverrideListModeValues()
        ruleEditorDraftValues = OverrideListModeValues()
        ruleEditorCallback = null
    }

    fun clearRuleDraftEditor() {
        ruleDraftEditorTitle = ""
        ruleDraftEditorValue = null
        ruleDraftEditorCallback = null
    }

    fun clearObjectEditor() {
        objectEditorTitle = ""
        objectEditorType = OverrideStructuredObjectType.Proxies
        objectEditorAvailableModes = listOf(OverrideListEditorMode.Replace)
        objectEditorSelectedMode = OverrideListEditorMode.Replace
        objectEditorValues = OverrideListModeValues()
        objectEditorProxyDraftValues = OverrideListModeValues()
        objectEditorProxyGroupDraftValues = OverrideListModeValues()
        objectEditorCallback = null
    }

    fun clearKeyedObjectMapEditor() {
        keyedObjectMapEditorTitle = ""
        keyedObjectMapEditorType = OverrideStructuredMapType.RuleProviders
        keyedObjectMapEditorAvailableModes = listOf(OverrideListEditorMode.Replace)
        keyedObjectMapEditorSelectedMode = OverrideListEditorMode.Replace
        keyedObjectMapEditorValues = OverrideListModeValues()
        keyedObjectMapEditorDraftValues = OverrideListModeValues()
        keyedObjectMapEditorCallback = null
    }

    fun clearSubRuleGroupEditor() {
        subRuleGroupEditorTitle = ""
        subRuleGroupEditorAvailableModes = listOf(OverrideListEditorMode.Replace)
        subRuleGroupEditorSelectedMode = OverrideListEditorMode.Replace
        subRuleGroupEditorValues = OverrideListModeValues()
        subRuleGroupEditorDraftValues = OverrideListModeValues()
        subRuleGroupEditorCallback = null
    }

    fun clearProxyDraftEditor() {
        proxyDraftEditorTitle = ""
        proxyDraftEditorValue = null
        proxyDraftEditorCallback = null
    }

    fun clearProxyGroupDraftEditor() {
        proxyGroupDraftEditorTitle = ""
        proxyGroupDraftEditorValue = null
        proxyGroupDraftEditorCallback = null
    }

    fun clearKeyedObjectDraftEditor() {
        keyedObjectDraftEditorTitle = ""
        keyedObjectDraftEditorType = OverrideStructuredMapType.RuleProviders
        keyedObjectDraftEditorValue = null
        keyedObjectDraftEditorCallback = null
    }

    fun clearSubRuleDraftEditor() {
        subRuleDraftEditorTitle = ""
        subRuleDraftEditorValue = null
        subRuleDraftEditorCallback = null
    }

    fun setupConfigPreview(
        title: String,
        content: String,
        language: LanguageScope = LanguageScope.Json,
        callback: ((String) -> Unit)? = null,
    ) {
        configPreviewTitle = title
        configPreviewContent = content
        configPreviewLanguage = language
        configPreviewCallback = callback
    }

    fun clearConfigPreview() {
        configPreviewTitle = ""
        configPreviewContent = ""
        configPreviewLanguage = LanguageScope.Json
        configPreviewCallback = null
    }
}
