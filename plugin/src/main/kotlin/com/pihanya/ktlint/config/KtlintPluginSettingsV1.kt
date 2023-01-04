package com.pihanya.ktlint.config

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.pihanya.ktlint.config.RuleSetSettings.RuleSetSourceSettings
import com.pihanya.ktlint.config.migrate.findAllSettingsClasses
import com.pihanya.ktlint.config.migrate.findAllSettingsMigrators
import com.pihanya.ktlint.integration.RuleSetId
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.Path

private val DEFAULT_RULE_SET_SETTINGS_BY_ID: Map<RuleSetId, RuleSetSettings> = mapOf(
    RuleSetId.STANDARD to RuleSetSettings(
        active = true,
        ruleSetId = RuleSetId.STANDARD,
        descriptionOverride = null,
        disabledRules = emptySet(),
        sourceSettings = RuleSetSourceSettings.Bundle,
    ),
    RuleSetId.EXPERIMENTAL to RuleSetSettings(
        active = false,
        ruleSetId = RuleSetId.EXPERIMENTAL,
        descriptionOverride = null,
        disabledRules = emptySet(),
        sourceSettings = RuleSetSourceSettings.Bundle,
    )
)

@Service(Service.Level.PROJECT)
@State(name = "KtlintPluginSettingsV1", storages = [Storage("ktlint.xml")])
class KtlintPluginSettingsV1(project: Project) :
    SimplePersistentStateComponent<KtlintPluginSettingsV1.State>(State()),
    PluginSettingsDefinition<KtlintPluginSettingsV1.State> {

    var enableKtlint: Boolean by state::enableKtlint

    var lintAfterReformat: Boolean by state::lintAfterReformat

    var annotationMode: AnnotationMode by state::annotationMode

    var styleGuide: StyleGuide by state::styleGuide

    var ruleSetSettingsById: Map<RuleSetId, RuleSetSettings>
        get() = state.ruleSetSettings.asSequence()
            .map { Json.decodeFromString<RuleSetSettings>(it) }
            .associateBy(RuleSetSettings::ruleSetId)
            .takeIf(Map<*, *>::isNotEmpty) ?: DEFAULT_RULE_SET_SETTINGS_BY_ID
        set(value) {
            state.ruleSetSettings = value.values.asSequence()
                .sortedBy(RuleSetSettings::ruleSetId)
                .map { Json.encodeToString(it) }
                .toMutableSet()
        }

    var editorConfigPaths: Set<Path>
        get() = state.editorConfigPaths.asSequence().map(::Path).toSet()
        set(value) {
            state.editorConfigPaths = value.asSequence().map(Path::toString).toMutableSet()
        }

    var baselinePath: Path?
        get() = state.baselinePath?.let(::Path)
        set(value) {
            state.baselinePath = value.toString()
        }

    private val stateLoader: PluginSettingsStateLoader = PluginSettingsStateLoader(
        project = project,
        settingsClasses = findAllSettingsClasses(),
        settingsMigrators = findAllSettingsMigrators(),
    )

    override fun loadState(state: State) {
        val loadedState = stateLoader.loadState(this, state)
        super.loadState(loadedState)
    }

    class State : BaseState(), PluginSettingsDefinition.State {

        var enableKtlint: Boolean by property(true)
        var lintAfterReformat: Boolean by property(true)
        var annotationMode: AnnotationMode by enum(AnnotationMode.WARNING)
        var styleGuide: StyleGuide by enum(StyleGuide.OFFICIAL)
        var ruleSetSettings: MutableSet<String> by stringSet()
        var editorConfigPaths: MutableSet<String> by stringSet()
        var baselinePath: String? by string()
    }

    companion object : PluginSettingsDefinition.Companion<KtlintPluginSettingsV1, State> {

        override val VERSION: Int = 1

        override fun getInstance(project: Project): KtlintPluginSettings = project.service()

        override fun buildEmptyState(): State = State()
    }

    enum class AnnotationMode { ERROR, WARNING, NONE }

    enum class StyleGuide { OFFICIAL, ANDROID }
}
