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

import kotlinx.serialization.json.JsonElement

internal fun copyOrderedObjectList(
    value: List<Map<String, JsonElement>>?,
): List<Map<String, JsonElement>>? {
    return value?.map(::toOrderedJsonElementMap)
}

internal fun copyOrderedObjectMap(
    value: Map<String, Map<String, JsonElement>>?,
): Map<String, Map<String, JsonElement>>? {
    return toOrderedObjectMap(value)
}

internal fun copyOrderedSubRuleMap(
    value: Map<String, List<String>>?,
): Map<String, List<String>>? {
    return toOrderedSubRuleMap(value)
}

internal fun withUpdatedProxyGroupDraft(
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

internal fun withUpdatedSubRuleDraft(
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

internal fun copyRuleDraftList(
    value: List<OverrideRuleDraft>?,
): List<OverrideRuleDraft>? {
    return value?.map { draft ->
        draft.copy(extras = draft.extras.toList())
    }
}

internal fun copyProxyDraftList(
    value: List<OverrideProxyDraft>?,
): List<OverrideProxyDraft>? {
    return value?.map { draft ->
        draft.copy(extraFields = toOrderedJsonElementMap(draft.extraFields))
    }
}

internal fun copyProxyGroupDraftList(
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

internal fun copyKeyedObjectDraftList(
    value: List<OverrideKeyedObjectDraft>?,
): List<OverrideKeyedObjectDraft>? {
    return value?.map { draft ->
        draft.copy(fields = toOrderedJsonElementMap(draft.fields))
    }
}

internal fun copySubRuleGroupDraftList(
    value: List<OverrideSubRuleGroupDraft>?,
): List<OverrideSubRuleGroupDraft>? {
    return value?.map { draft ->
        draft.copy(rules = draft.rules.toList())
    }
}

internal fun copyRuleDraftValues(
    value: OverrideListModeValues<List<OverrideRuleDraft>>,
): OverrideListModeValues<List<OverrideRuleDraft>> {
    return OverrideListModeValues(
        replaceValue = copyRuleDraftList(value.replaceValue),
        mergeValue = copyRuleDraftList(value.mergeValue),
        startValue = copyRuleDraftList(value.startValue),
        endValue = copyRuleDraftList(value.endValue),
    )
}

internal fun copyProxyDraftValues(
    value: OverrideListModeValues<List<OverrideProxyDraft>>,
): OverrideListModeValues<List<OverrideProxyDraft>> {
    return OverrideListModeValues(
        replaceValue = copyProxyDraftList(value.replaceValue),
        mergeValue = copyProxyDraftList(value.mergeValue),
        startValue = copyProxyDraftList(value.startValue),
        endValue = copyProxyDraftList(value.endValue),
    )
}

internal fun copyProxyGroupDraftValues(
    value: OverrideListModeValues<List<OverrideProxyGroupDraft>>,
): OverrideListModeValues<List<OverrideProxyGroupDraft>> {
    return OverrideListModeValues(
        replaceValue = copyProxyGroupDraftList(value.replaceValue),
        mergeValue = copyProxyGroupDraftList(value.mergeValue),
        startValue = copyProxyGroupDraftList(value.startValue),
        endValue = copyProxyGroupDraftList(value.endValue),
    )
}

internal fun copyKeyedObjectDraftValues(
    value: OverrideListModeValues<List<OverrideKeyedObjectDraft>>,
): OverrideListModeValues<List<OverrideKeyedObjectDraft>> {
    return OverrideListModeValues(
        replaceValue = copyKeyedObjectDraftList(value.replaceValue),
        mergeValue = copyKeyedObjectDraftList(value.mergeValue),
        startValue = copyKeyedObjectDraftList(value.startValue),
        endValue = copyKeyedObjectDraftList(value.endValue),
    )
}

internal fun copySubRuleGroupDraftValues(
    value: OverrideListModeValues<List<OverrideSubRuleGroupDraft>>,
): OverrideListModeValues<List<OverrideSubRuleGroupDraft>> {
    return OverrideListModeValues(
        replaceValue = copySubRuleGroupDraftList(value.replaceValue),
        mergeValue = copySubRuleGroupDraftList(value.mergeValue),
        startValue = copySubRuleGroupDraftList(value.startValue),
        endValue = copySubRuleGroupDraftList(value.endValue),
    )
}

internal fun parseRuleDraftValues(
    value: OverrideListModeValues<List<String>>,
): OverrideListModeValues<List<OverrideRuleDraft>> {
    return OverrideListModeValues(
        replaceValue = value.replaceValue?.let(::parseRuleDrafts),
        mergeValue = value.mergeValue?.let(::parseRuleDrafts),
        startValue = value.startValue?.let(::parseRuleDrafts),
        endValue = value.endValue?.let(::parseRuleDrafts),
    )
}

internal fun parseProxyDraftValues(
    value: OverrideListModeValues<List<Map<String, JsonElement>>>,
): OverrideListModeValues<List<OverrideProxyDraft>> {
    return OverrideListModeValues(
        replaceValue = value.replaceValue?.let(::parseProxyDrafts),
        mergeValue = value.mergeValue?.let(::parseProxyDrafts),
        startValue = value.startValue?.let(::parseProxyDrafts),
        endValue = value.endValue?.let(::parseProxyDrafts),
    )
}

internal fun parseProxyGroupDraftValues(
    value: OverrideListModeValues<List<Map<String, JsonElement>>>,
): OverrideListModeValues<List<OverrideProxyGroupDraft>> {
    return OverrideListModeValues(
        replaceValue = value.replaceValue?.let(::parseProxyGroupDrafts),
        mergeValue = value.mergeValue?.let(::parseProxyGroupDrafts),
        startValue = value.startValue?.let(::parseProxyGroupDrafts),
        endValue = value.endValue?.let(::parseProxyGroupDrafts),
    )
}

internal fun parseKeyedObjectDraftValues(
    value: OverrideListModeValues<Map<String, Map<String, JsonElement>>>,
): OverrideListModeValues<List<OverrideKeyedObjectDraft>> {
    return OverrideListModeValues(
        replaceValue = value.replaceValue?.let(::parseKeyedObjectDrafts),
        mergeValue = value.mergeValue?.let(::parseKeyedObjectDrafts),
        startValue = value.startValue?.let(::parseKeyedObjectDrafts),
        endValue = value.endValue?.let(::parseKeyedObjectDrafts),
    )
}

internal fun parseSubRuleGroupDraftValues(
    value: OverrideListModeValues<Map<String, List<String>>>,
): OverrideListModeValues<List<OverrideSubRuleGroupDraft>> {
    return OverrideListModeValues(
        replaceValue = value.replaceValue?.let(::parseSubRuleGroupDrafts),
        mergeValue = value.mergeValue?.let(::parseSubRuleGroupDrafts),
        startValue = value.startValue?.let(::parseSubRuleGroupDrafts),
        endValue = value.endValue?.let(::parseSubRuleGroupDrafts),
    )
}

internal fun formatRuleDraftValues(
    value: OverrideListModeValues<List<OverrideRuleDraft>>,
): OverrideListModeValues<List<String>> {
    return OverrideListModeValues(
        replaceValue = value.replaceValue?.let(::formatRuleDrafts),
        mergeValue = value.mergeValue?.let(::formatRuleDrafts),
        startValue = value.startValue?.let(::formatRuleDrafts),
        endValue = value.endValue?.let(::formatRuleDrafts),
    )
}

internal fun formatProxyDraftValues(
    value: OverrideListModeValues<List<OverrideProxyDraft>>,
): OverrideListModeValues<List<Map<String, JsonElement>>> {
    return OverrideListModeValues(
        replaceValue = value.replaceValue?.let(::formatProxyDrafts),
        mergeValue = value.mergeValue?.let(::formatProxyDrafts),
        startValue = value.startValue?.let(::formatProxyDrafts),
        endValue = value.endValue?.let(::formatProxyDrafts),
    )
}

internal fun formatProxyGroupDraftValues(
    value: OverrideListModeValues<List<OverrideProxyGroupDraft>>,
): OverrideListModeValues<List<Map<String, JsonElement>>> {
    return OverrideListModeValues(
        replaceValue = value.replaceValue?.let(::formatProxyGroupDrafts),
        mergeValue = value.mergeValue?.let(::formatProxyGroupDrafts),
        startValue = value.startValue?.let(::formatProxyGroupDrafts),
        endValue = value.endValue?.let(::formatProxyGroupDrafts),
    )
}

internal fun formatKeyedObjectDraftValues(
    value: OverrideListModeValues<List<OverrideKeyedObjectDraft>>,
): OverrideListModeValues<Map<String, Map<String, JsonElement>>> {
    return OverrideListModeValues(
        replaceValue = value.replaceValue?.let(::formatKeyedObjectDrafts),
        mergeValue = value.mergeValue?.let(::formatKeyedObjectDrafts),
        startValue = value.startValue?.let(::formatKeyedObjectDrafts),
        endValue = value.endValue?.let(::formatKeyedObjectDrafts),
    )
}

internal fun formatSubRuleGroupDraftValues(
    value: OverrideListModeValues<List<OverrideSubRuleGroupDraft>>,
): OverrideListModeValues<Map<String, List<String>>> {
    return OverrideListModeValues(
        replaceValue = value.replaceValue?.let(::formatSubRuleGroupDrafts),
        mergeValue = value.mergeValue?.let(::formatSubRuleGroupDrafts),
        startValue = value.startValue?.let(::formatSubRuleGroupDrafts),
        endValue = value.endValue?.let(::formatSubRuleGroupDrafts),
    )
}
