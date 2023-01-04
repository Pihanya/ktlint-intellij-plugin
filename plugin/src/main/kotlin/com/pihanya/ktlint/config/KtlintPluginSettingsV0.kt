package com.pihanya.ktlint.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Tag

@State(name = "KtlintProjectConfiguration", storages = [Storage("ktlint.xml")])
@Suppress("DEPRECATION")
@Deprecated("Provided for migration purposes only, use [KtlintPluginSettingsView] instead.")
class KtlintPluginSettingsV0 :
    PersistentStateComponent<KtlintPluginSettingsV0>,
    PluginSettingsDefinition<KtlintPluginSettingsV0>,
    PluginSettingsDefinition.State {

    /**
     * @see [KtlintPluginSettingsV1.enableKtlint]
     */
    @Tag var enableKtlint: Boolean = true

    /**
     * Removed. Migrated to a single field with enum value
     *
     * @see [KtlintPluginSettingsV1.styleGuide]
     */
    @Tag var androidMode: Boolean = false

    /**
     * @see [KtlintPluginSettingsV1.useExperimental]
     */
    @Tag var useExperimental: Boolean = false

    /**
     * Removed. Migrated to a single field with enum value
     *
     * @see [KtlintPluginSettingsV1.annotationMode]
     */
    @Tag var treatAsErrors: Boolean = true

    /**
     * Removed. Migrated to a single field with enum value
     *
     * @see [KtlintPluginSettingsV1.annotationMode]
     */
    @Tag var hideErrors: Boolean = false

    /**
     * @see [KtlintPluginSettingsV1.lintAfterReformat]
     */
    @Tag var lintAfterReformat: Boolean = true

    /**
     * @see [KtlintPluginSettingsV1.disabledRules]
     */
    @Tag var disabledRules: List<String> = emptyList()

    /**
     * @see [KtlintPluginSettingsV1.baselinePath]
     */
    @Tag var baselinePath: String? = null

    /**
     * Migrated to list
     *
     * @see [KtlintPluginSettingsV1.editorConfigPaths]
     */
    @Tag var editorConfigPath: String? = null

    /**
     * @see [KtlintPluginSettingsV1.localJarPaths]
     */
    @Tag var externalJarPaths: List<String> = emptyList()

    override fun getState(): KtlintPluginSettingsV0 = this

    override fun loadState(state: KtlintPluginSettingsV0) {
        this.enableKtlint = state.enableKtlint
        this.androidMode = state.androidMode
        this.useExperimental = state.useExperimental
        this.treatAsErrors = state.treatAsErrors
        this.hideErrors = state.hideErrors
        this.lintAfterReformat = state.lintAfterReformat
        this.disabledRules = state.disabledRules
        this.baselinePath = state.baselinePath
        this.editorConfigPath = state.editorConfigPath
        this.externalJarPaths = state.externalJarPaths
    }

    companion object : PluginSettingsDefinition.Companion<KtlintPluginSettingsV0, KtlintPluginSettingsV0> {

        override val VERSION: Int = 0

        /**
         * Get instance of [KtlintPluginSettingsV0] for given project.
         *
         * @param project the project
         */
        override fun getInstance(project: Project): KtlintPluginSettingsV0 = project.service()

        override fun buildEmptyState(): KtlintPluginSettingsV0 = KtlintPluginSettingsV0()
    }
}
