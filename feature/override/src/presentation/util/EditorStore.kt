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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.yumelira.yumebox.feature.editor.language.LanguageScope
import kotlinx.serialization.json.JsonElement

object OverrideStructuredEditorStore {
    private val previewSession = OverrideConfigPreviewSession()
    private val stringListSession = OverrideStringListEditorSession()
    private val ruleSession = OverrideRuleEditorSession()
    private val objectSession = OverrideObjectEditorSession()
    private val keyedObjectMapSession = OverrideKeyedObjectMapEditorSession()
    private val subRuleGroupSession = OverrideSubRuleGroupEditorSession()
    private val ruleDraftSession = OverrideDraftEditorSession<OverrideRuleDraft> { value ->
        value?.copy(extras = value.extras.toList())
    }
    private val proxyDraftSession = OverrideDraftEditorSession<OverrideProxyDraft> { value ->
        value?.copy(extraFields = toOrderedJsonElementMap(value.extraFields))
    }
    private val proxyGroupDraftSession = OverrideDraftEditorSession<OverrideProxyGroupDraft> { value ->
        value?.copy(
            proxies = value.proxies.toList(),
            use = value.use.toList(),
            extraFields = toOrderedJsonElementMap(value.extraFields),
        )
    }
    private val keyedObjectDraftSession =
        OverrideTypedDraftEditorSession(OverrideStructuredMapType.RuleProviders) { value: OverrideKeyedObjectDraft? ->
            value?.copy(fields = toOrderedJsonElementMap(value.fields))
        }
    private val subRuleDraftSession = OverrideDraftEditorSession<OverrideSubRuleGroupDraft> { value ->
        value?.copy(rules = value.rules.toList())
    }

    var referenceCatalog: OverrideReferenceCatalog by mutableStateOf(OverrideReferenceCatalog())

    var configPreviewTitle: String by previewSession::title
    var configPreviewContent: String by previewSession::content
    var configPreviewLanguage: LanguageScope by previewSession::language
    var configPreviewCallback: ((String) -> Unit)? by previewSession::callback

    var stringListEditorTitle: String by stringListSession::title
    var stringListEditorPlaceholder: String by stringListSession::placeholder
    var stringListEditorAvailableModes: List<OverrideListEditorMode> by stringListSession::availableModes
    var stringListEditorSelectedMode: OverrideListEditorMode by stringListSession::selectedMode
    var stringListEditorValues: OverrideListModeValues<List<String>> by stringListSession::values
    var stringListEditorCallback: ((OverrideListModeValues<List<String>>) -> Unit)? by stringListSession::callback

    var ruleEditorTitle: String by ruleSession::title
    var ruleEditorAvailableModes: List<OverrideListEditorMode> by ruleSession::availableModes
    var ruleEditorSelectedMode: OverrideListEditorMode by ruleSession::selectedMode
    var ruleEditorValues: OverrideListModeValues<List<String>> by ruleSession::values
    var ruleEditorDraftValues: OverrideListModeValues<List<OverrideRuleDraft>> by ruleSession::draftValues
    var ruleEditorCallback: ((OverrideListModeValues<List<String>>) -> Unit)? by ruleSession::callback

    var objectEditorTitle: String by objectSession::title
    var objectEditorType: OverrideStructuredObjectType by objectSession::type
    var objectEditorAvailableModes: List<OverrideListEditorMode> by objectSession::availableModes
    var objectEditorSelectedMode: OverrideListEditorMode by objectSession::selectedMode
    var objectEditorValues: OverrideListModeValues<List<Map<String, JsonElement>>> by objectSession::values
    var objectEditorProxyDraftValues: OverrideListModeValues<List<OverrideProxyDraft>> by objectSession::proxyDraftValues
    var objectEditorProxyGroupDraftValues: OverrideListModeValues<List<OverrideProxyGroupDraft>> by objectSession::proxyGroupDraftValues
    var objectEditorCallback: ((OverrideListModeValues<List<Map<String, JsonElement>>>) -> Unit)? by objectSession::callback

    var keyedObjectMapEditorTitle: String by keyedObjectMapSession::title
    var keyedObjectMapEditorType: OverrideStructuredMapType by keyedObjectMapSession::type
    var keyedObjectMapEditorAvailableModes: List<OverrideListEditorMode> by keyedObjectMapSession::availableModes
    var keyedObjectMapEditorSelectedMode: OverrideListEditorMode by keyedObjectMapSession::selectedMode
    var keyedObjectMapEditorValues: OverrideListModeValues<Map<String, Map<String, JsonElement>>> by keyedObjectMapSession::values
    var keyedObjectMapEditorDraftValues: OverrideListModeValues<List<OverrideKeyedObjectDraft>> by keyedObjectMapSession::draftValues
    var keyedObjectMapEditorCallback: ((OverrideListModeValues<Map<String, Map<String, JsonElement>>>) -> Unit)? by keyedObjectMapSession::callback

    var subRuleGroupEditorTitle: String by subRuleGroupSession::title
    var subRuleGroupEditorAvailableModes: List<OverrideListEditorMode> by subRuleGroupSession::availableModes
    var subRuleGroupEditorSelectedMode: OverrideListEditorMode by subRuleGroupSession::selectedMode
    var subRuleGroupEditorValues: OverrideListModeValues<Map<String, List<String>>> by subRuleGroupSession::values
    var subRuleGroupEditorDraftValues: OverrideListModeValues<List<OverrideSubRuleGroupDraft>> by subRuleGroupSession::draftValues
    var subRuleGroupEditorCallback: ((OverrideListModeValues<Map<String, List<String>>>) -> Unit)? by subRuleGroupSession::callback

    var ruleDraftEditorTitle: String by ruleDraftSession::title
    var ruleDraftEditorValue: OverrideRuleDraft? by ruleDraftSession::value
    var ruleDraftEditorCallback: ((OverrideRuleDraft) -> Unit)? by ruleDraftSession::callback

    var proxyDraftEditorTitle: String by proxyDraftSession::title
    var proxyDraftEditorValue: OverrideProxyDraft? by proxyDraftSession::value
    var proxyDraftEditorCallback: ((OverrideProxyDraft) -> Unit)? by proxyDraftSession::callback

    var proxyGroupDraftEditorTitle: String by proxyGroupDraftSession::title
    var proxyGroupDraftEditorValue: OverrideProxyGroupDraft? by proxyGroupDraftSession::value
    var proxyGroupDraftEditorCallback: ((OverrideProxyGroupDraft) -> Unit)? by proxyGroupDraftSession::callback

    var keyedObjectDraftEditorTitle: String by keyedObjectDraftSession::title
    var keyedObjectDraftEditorType: OverrideStructuredMapType by keyedObjectDraftSession::type
    var keyedObjectDraftEditorValue: OverrideKeyedObjectDraft? by keyedObjectDraftSession::value
    var keyedObjectDraftEditorCallback: ((OverrideKeyedObjectDraft) -> Unit)? by keyedObjectDraftSession::callback

    var subRuleDraftEditorTitle: String by subRuleDraftSession::title
    var subRuleDraftEditorValue: OverrideSubRuleGroupDraft? by subRuleDraftSession::value
    var subRuleDraftEditorCallback: ((OverrideSubRuleGroupDraft) -> Unit)? by subRuleDraftSession::callback

    fun setupStringListEditor(
        title: String,
        placeholder: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<List<String>>,
        callback: (OverrideListModeValues<List<String>>) -> Unit,
    ) {
        stringListSession.setup(title, placeholder, availableModes, selectedMode, values, callback)
    }

    fun updateStringListEditorSession(
        selectedMode: OverrideListEditorMode = stringListEditorSelectedMode,
        values: OverrideListModeValues<List<String>> = stringListEditorValues,
    ) {
        stringListSession.update(selectedMode, values)
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
        ruleSession.setup(title, availableModes, selectedMode, values, callback)
    }

    fun updateRuleEditorSession(
        selectedMode: OverrideListEditorMode = ruleEditorSelectedMode,
        values: OverrideListModeValues<List<String>> = ruleEditorValues,
    ) {
        ruleSession.update(selectedMode, values)
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
        objectSession.setup(type, title, availableModes, selectedMode, values, callback)
    }

    fun updateObjectEditorSession(
        selectedMode: OverrideListEditorMode = objectEditorSelectedMode,
        values: OverrideListModeValues<List<Map<String, JsonElement>>> = objectEditorValues,
    ) {
        objectSession.update(selectedMode, values)
    }

    fun setupKeyedObjectMapEditor(
        type: OverrideStructuredMapType,
        title: String,
        availableModes: List<OverrideListEditorMode>,
        selectedMode: OverrideListEditorMode,
        values: OverrideListModeValues<Map<String, Map<String, JsonElement>>>,
        callback: (OverrideListModeValues<Map<String, Map<String, JsonElement>>>) -> Unit,
    ) {
        keyedObjectMapSession.setup(type, title, availableModes, selectedMode, values, callback)
    }

    fun updateKeyedObjectMapEditorSession(
        selectedMode: OverrideListEditorMode = keyedObjectMapEditorSelectedMode,
        values: OverrideListModeValues<Map<String, Map<String, JsonElement>>> = keyedObjectMapEditorValues,
    ) {
        keyedObjectMapSession.update(selectedMode, values)
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
        subRuleGroupSession.setup(title, availableModes, selectedMode, values, callback)
    }

    fun updateSubRuleGroupEditorSession(
        selectedMode: OverrideListEditorMode = subRuleGroupEditorSelectedMode,
        values: OverrideListModeValues<Map<String, List<String>>> = subRuleGroupEditorValues,
    ) {
        subRuleGroupSession.update(selectedMode, values)
    }

    fun applyStringListValues(values: OverrideListModeValues<List<String>>) {
        stringListSession.apply(values)
    }

    fun applyRuleValues(values: OverrideListModeValues<List<String>>) {
        ruleSession.applyValues(values)
    }

    fun applyRuleDraftValues(values: OverrideListModeValues<List<OverrideRuleDraft>>) {
        ruleSession.applyDraftValues(values)
    }

    fun applyObjectValues(values: OverrideListModeValues<List<Map<String, JsonElement>>>) {
        objectSession.applyValues(values)
    }

    fun applyProxyDraftValues(values: OverrideListModeValues<List<OverrideProxyDraft>>) {
        objectSession.applyProxyDraftValues(values)
    }

    fun applyProxyDraftModeValue(
        mode: OverrideListEditorMode,
        value: List<OverrideProxyDraft>,
    ) {
        objectSession.applyProxyDraftModeValue(mode, value)
    }

    fun applyProxyGroupDraftValues(values: OverrideListModeValues<List<OverrideProxyGroupDraft>>) {
        objectSession.applyProxyGroupDraftValues(values)
    }

    fun applyProxyGroupDraftModeValue(
        mode: OverrideListEditorMode,
        value: List<OverrideProxyGroupDraft>,
    ) {
        objectSession.applyProxyGroupDraftModeValue(mode, value)
    }

    fun applyKeyedObjectValues(values: OverrideListModeValues<Map<String, Map<String, JsonElement>>>) {
        keyedObjectMapSession.applyValues(values)
    }

    fun applyKeyedObjectDraftValues(values: OverrideListModeValues<List<OverrideKeyedObjectDraft>>) {
        keyedObjectMapSession.applyDraftValues(values)
    }

    fun applyKeyedObjectDraftModeValue(
        mode: OverrideListEditorMode,
        value: List<OverrideKeyedObjectDraft>,
    ) {
        keyedObjectMapSession.applyDraftModeValue(mode, value)
    }

    fun applySubRuleValues(values: OverrideListModeValues<Map<String, List<String>>>) {
        subRuleGroupSession.applyValues(values)
    }

    fun applySubRuleDraftValues(values: OverrideListModeValues<List<OverrideSubRuleGroupDraft>>) {
        subRuleGroupSession.applyDraftValues(values)
    }

    fun setupRuleDraftEditor(
        title: String,
        value: OverrideRuleDraft?,
        callback: (OverrideRuleDraft) -> Unit,
    ) {
        ruleDraftSession.setup(title, value, callback)
    }

    fun updateRuleDraftEditorSession(value: OverrideRuleDraft?) {
        ruleDraftSession.update(value)
    }

    fun setupProxyDraftEditor(
        title: String,
        value: OverrideProxyDraft?,
        callback: (OverrideProxyDraft) -> Unit,
    ) {
        proxyDraftSession.setup(title, value, callback)
    }

    fun updateProxyDraftEditorSession(value: OverrideProxyDraft?) {
        proxyDraftSession.update(value)
    }

    fun setupProxyGroupDraftEditor(
        title: String,
        value: OverrideProxyGroupDraft?,
        callback: (OverrideProxyGroupDraft) -> Unit,
    ) {
        proxyGroupDraftSession.setup(title, value, callback)
    }

    fun updateProxyGroupDraftEditorSession(value: OverrideProxyGroupDraft?) {
        proxyGroupDraftSession.update(value)
    }

    fun setupKeyedObjectDraftEditor(
        type: OverrideStructuredMapType,
        title: String,
        value: OverrideKeyedObjectDraft?,
        callback: (OverrideKeyedObjectDraft) -> Unit,
    ) {
        keyedObjectDraftSession.setup(type, title, value, callback)
    }

    fun updateKeyedObjectDraftEditorSession(value: OverrideKeyedObjectDraft?) {
        keyedObjectDraftSession.update(value)
    }

    fun setupSubRuleDraftEditor(
        title: String,
        value: OverrideSubRuleGroupDraft?,
        callback: (OverrideSubRuleGroupDraft) -> Unit,
    ) {
        subRuleDraftSession.setup(title, value, callback)
    }

    fun updateSubRuleDraftEditorSession(value: OverrideSubRuleGroupDraft?) {
        subRuleDraftSession.update(value)
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
        ruleDraftSession.submit(value)
    }

    fun submitProxyDraft(value: OverrideProxyDraft) {
        proxyDraftSession.submit(value)
    }

    fun submitProxyGroupDraft(value: OverrideProxyGroupDraft) {
        proxyGroupDraftSession.submit(value)
    }

    fun submitKeyedObjectDraft(value: OverrideKeyedObjectDraft) {
        keyedObjectDraftSession.submit(value)
    }

    fun submitSubRuleDraft(value: OverrideSubRuleGroupDraft) {
        subRuleDraftSession.submit(value)
    }

    fun clearStringListEditor() {
        stringListSession.clear()
    }

    fun clearRuleEditor() {
        ruleSession.clear()
    }

    fun clearRuleDraftEditor() {
        ruleDraftSession.clear()
    }

    fun clearObjectEditor() {
        objectSession.clear()
    }

    fun clearKeyedObjectMapEditor() {
        keyedObjectMapSession.clear()
    }

    fun clearSubRuleGroupEditor() {
        subRuleGroupSession.clear()
    }

    fun clearProxyDraftEditor() {
        proxyDraftSession.clear()
    }

    fun clearProxyGroupDraftEditor() {
        proxyGroupDraftSession.clear()
    }

    fun clearKeyedObjectDraftEditor() {
        keyedObjectDraftSession.clear()
    }

    fun clearSubRuleDraftEditor() {
        subRuleDraftSession.clear()
    }

    fun setupConfigPreview(
        title: String,
        content: String,
        language: LanguageScope = LanguageScope.Json,
        callback: ((String) -> Unit)? = null,
    ) {
        previewSession.setup(title, content, language, callback)
    }

    fun clearConfigPreview() {
        previewSession.clear()
    }
}

@Composable
fun rememberCurrentReferenceCatalog(): OverrideReferenceCatalog {
    val referenceCatalog = OverrideStructuredEditorStore.referenceCatalog
    val objectEditorCallback = OverrideStructuredEditorStore.objectEditorCallback
    val objectEditorType = OverrideStructuredEditorStore.objectEditorType
    val objectEditorProxyDraftValues = OverrideStructuredEditorStore.objectEditorProxyDraftValues
    val objectEditorProxyGroupDraftValues = OverrideStructuredEditorStore.objectEditorProxyGroupDraftValues
    val objectEditorSelectedMode = OverrideStructuredEditorStore.objectEditorSelectedMode
    val proxyGroupDraftEditorValue = OverrideStructuredEditorStore.proxyGroupDraftEditorValue
    val subRuleGroupEditorCallback = OverrideStructuredEditorStore.subRuleGroupEditorCallback
    val subRuleGroupEditorDraftValues = OverrideStructuredEditorStore.subRuleGroupEditorDraftValues
    val subRuleGroupEditorSelectedMode = OverrideStructuredEditorStore.subRuleGroupEditorSelectedMode
    val subRuleDraftEditorValue = OverrideStructuredEditorStore.subRuleDraftEditorValue
    val keyedObjectMapEditorCallback = OverrideStructuredEditorStore.keyedObjectMapEditorCallback
    val keyedObjectMapEditorType = OverrideStructuredEditorStore.keyedObjectMapEditorType
    val keyedObjectMapEditorDraftValues = OverrideStructuredEditorStore.keyedObjectMapEditorDraftValues

    return remember(
        referenceCatalog,
        objectEditorCallback,
        objectEditorType,
        objectEditorProxyDraftValues,
        objectEditorProxyGroupDraftValues,
        objectEditorSelectedMode,
        proxyGroupDraftEditorValue,
        subRuleGroupEditorCallback,
        subRuleGroupEditorDraftValues,
        subRuleGroupEditorSelectedMode,
        subRuleDraftEditorValue,
        keyedObjectMapEditorCallback,
        keyedObjectMapEditorType,
        keyedObjectMapEditorDraftValues,
    ) {
        OverrideStructuredEditorStore.currentReferenceCatalog()
    }
}
