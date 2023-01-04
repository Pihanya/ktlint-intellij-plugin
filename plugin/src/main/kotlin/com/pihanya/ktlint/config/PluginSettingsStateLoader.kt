package com.pihanya.ktlint.config

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.pihanya.ktlint.config.migrate.PluginSettingsMigrator
import com.pihanya.ktlint.util.uncheckedCast
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName

internal typealias AnyPluginSettingsDefinition = PluginSettingsDefinition<*>
internal typealias AnyPluginSettingsState = PluginSettingsDefinition.State
internal typealias AnyPluginSettingsCompanion = PluginSettingsDefinition.Companion<*, *>
internal typealias AnyPluginSettingsMigrator = PluginSettingsMigrator<*, *>

internal class PluginSettingsStateLoader(
    private val project: Project,
    settingsClasses: List<KClass<out AnyPluginSettingsDefinition>>,
    private val settingsMigrators: List<AnyPluginSettingsMigrator>,
) {

    private val logger: Logger = logger<PluginSettingsStateLoader>()

    private val globalSettings: KtlintGlobalSettings = project.service()

    private val companions: SortedSet<AnyPluginSettingsCompanion> = loadCompanions(settingsClasses)

    fun <S : AnyPluginSettingsState> loadState(settings: PluginSettingsDefinition<S>, currentState: S): S {
        // Compare current (project) settings version with required version (of `settings`)
        val currentSettingsVersion: Int = getCurrentSettingsVersion()
        val targetSettingsVersion: Int = companionOf(settings::class).VERSION
        val isMigrationRequired = (currentSettingsVersion != targetSettingsVersion)
        if (!isMigrationRequired) {
            return currentState
        }

        val migratedState = performSettingsMigration(
            stateVersion = currentSettingsVersion,
            targetVersion = targetSettingsVersion,
        )
        return uncheckedCast<S>(migratedState)
            .also { setCurrentSettingsVersion(targetSettingsVersion) }
    }

    private fun performSettingsMigration(
        stateVersion: Int,
        targetVersion: Int
    ): AnyPluginSettingsState {
        require(stateVersion < targetVersion) {
            "State version [$stateVersion] appeared to be greater than the target version [$targetVersion]"
        }

        val companions: List<AnyPluginSettingsCompanion> = this.companions
            .filter { it.VERSION in stateVersion..targetVersion }
        check(companions.last().VERSION == targetVersion) {
            "Could not find settings class for version [$targetVersion]" +
                ". Last available is [${companions.last().VERSION}]"
        }
        check(companions.size >= 2) { "Could not find companions" }

        val migratedStates = companions.asSequence().zipWithNext()
            .map { (fromCompanion, toCompanion) ->
                val fromConfig = fromCompanion.getInstance(project)
                val toState = toCompanion.buildEmptyState()

                logger.info(
                    "Performing migration of settings" +
                        " from version [${fromCompanion.VERSION}] to [${toCompanion.VERSION}]",
                )
                return@map runCatching { performSettingsMigration(fromConfig, toState) }
                    .onFailure { ex ->
                        logger.error(
                            "Could not perform migration of settings" +
                                " from version [${fromCompanion.VERSION}] to [${toCompanion.VERSION}]" +
                                ": ${ex.message}",
                        )
                    }
                    .getOrThrow()
            }
            .toList()
        return uncheckedCast(migratedStates.last())
    }

    private fun performSettingsMigration(
        fromSettings: PluginSettingsDefinition<*>,
        toState: AnyPluginSettingsState
    ): AnyPluginSettingsState {
        val migrator = findMigratorFor(fromSettings, toState)
        migrator.migrate(fromSettings, toState)
        return toState
    }

    private fun getCurrentSettingsVersion(): Int =
        globalSettings.state.stateVersion

    private fun setCurrentSettingsVersion(value: Int) {
        globalSettings.state.stateVersion = value
    }

    private fun <F, T> findMigratorFor(fromConfig: F, toState: T): PluginSettingsMigrator<F, T>
        where F : PluginSettingsDefinition<*>, T : AnyPluginSettingsState =
        settingsMigrators.first { it.accepts(fromConfig, toState) }
            .let(::uncheckedCast)
            ?: error("Could not find migrator for [${fromConfig.javaClass.name} -> ${toState.javaClass.name}]")

    private fun companionOf(settingsClass: KClass<out AnyPluginSettingsDefinition>): AnyPluginSettingsCompanion {
        val settingsCompanion = settingsClass.companionObjectInstance
        checkNotNull(settingsCompanion) { "Could not resolve companion for class [${settingsClass.jvmName}]" }
        check(settingsCompanion is PluginSettingsDefinition.Companion<*, *>) {
            "Companion class of [${settingsClass.jvmName}]" +
                " does not inherit [${PluginSettingsDefinition.Companion::class.jvmName}]"
        }
        return uncheckedCast(settingsCompanion)
    }

    private fun loadCompanions(
        settingsClasses: List<KClass<out AnyPluginSettingsDefinition>>
    ): SortedSet<AnyPluginSettingsCompanion> = settingsClasses.asSequence()
        .map { companionOf(it) }
        .toSortedSet(compareBy(AnyPluginSettingsCompanion::VERSION))
        .also { set -> check(set.isNotEmpty()) { "No settings classes were provided" } }
        .also { set -> // Check all versions are sequential [1, 2, 3, ...]
            val missingVersions = set.asSequence()
                .map { it.VERSION }.zipWithNext()
                .mapNotNull { (version, nextVersion) ->
                    if (version + 1 == nextVersion) return@mapNotNull null
                    ((version + 1) until nextVersion).asSequence()
                }
                .flatten().toList()
            check(missingVersions.isEmpty()) { "Could not find settings for versions: $missingVersions" }
        }
}
