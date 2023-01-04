package com.pihanya.ktlint.config

import com.intellij.openapi.project.Project
import com.pihanya.ktlint.config.migrate.PluginSettingsMigrator
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder

private typealias State = PluginSettingsDefinition.State
private typealias Settings<S> = PluginSettingsDefinition<S>
private typealias SettingsCompanion<D, S> = PluginSettingsDefinition.Companion<D, S>
private typealias Migrator<D, S> = PluginSettingsMigrator<D, S>

class PluginSettingsStateLoaderTests : ShouldSpec({

    lateinit var project: Project

    lateinit var globalSettingsState: KtlintGlobalSettings.State

    beforeTest {
        globalSettingsState = mockk {
            every { stateVersion = any() } just runs
            every { stateVersion } returns 0
        }
        val globalSettings: KtlintGlobalSettings = mockk() {
            every { state } returns globalSettingsState
        }
        project = mockk() {
            every { getService(KtlintGlobalSettings::class.java) } returns globalSettings
        }
    }

    context("initialization tests") {
        should("initialization should throw if no settings classes were provided") {
            shouldThrowWithMessage<IllegalStateException>("No settings classes were provided") {
                PluginSettingsStateLoader(
                    project = project,
                    settingsClasses = emptyList(),
                    settingsMigrators = emptyList(),
                )
            }
        }
        should("initialization should not throw if no migrators were provided") {
            shouldNotThrow<Throwable> {
                PluginSettingsStateLoader(
                    project = project,
                    settingsClasses = listOf(SettingsV1::class),
                    settingsMigrators = emptyList(),
                )
            }
        }
        should("initialization should throw if missing settings class of some version") {
            shouldThrowWithMessage<IllegalStateException>("Could not find settings for versions: [2, 4]") {
                PluginSettingsStateLoader(
                    project = project,
                    settingsClasses = listOf(
                        SettingsV1::class,
                        /* version 2 is missing*/
                        SettingsV3::class,
                        /* version 4 is missing,*/
                        SettingsV5::class,
                    ),
                    settingsMigrators = emptyList(),
                )
            }
        }
    }

    context("behaviour test") {
        should("return same state if no migration required (happy path)") {
            val inState = SettingsV1.StateV1()
            every { globalSettingsState.stateVersion } returns SettingsV1.VERSION // same as settings version

            val loader = PluginSettingsStateLoader(
                project = project,
                settingsClasses = listOf(SettingsV1::class),
                settingsMigrators = emptyList(),
            )
            loader.loadState(SettingsV1(), inState) shouldBe inState
        }

        should("throw if state version is greater than settings version") {
            val inState = SettingsV3.StateV3()
            every { globalSettingsState.stateVersion } returns SettingsV3.VERSION + 1 // greater than settings version

            val loader = PluginSettingsStateLoader(
                project = project,
                settingsClasses = listOf(SettingsV3::class),
                settingsMigrators = emptyList(),
            )
            shouldThrowWithMessage<IllegalArgumentException>(
                message = "State version [4] appeared to be greater than the target version [3]",
                block = { loader.loadState(SettingsV3(), inState) },
            )
        }

        should("should perform migration without errors") {
            val migrator = spyk(MigratorV1V2)
            val settings = spyk(SettingsV2())
            val state = spyk(SettingsV2.StateV2())
            every { globalSettingsState.stateVersion } returns SettingsV1.VERSION

            val settingsV1 = SettingsV1()
            mockkObject(SettingsV1.Companion)
            every { SettingsV1.getInstance(eq(project)) } returns settingsV1

            val stateV2 = SettingsV2.StateV2()
            mockkObject(SettingsV2.Companion)
            every { SettingsV2.buildEmptyState() } returns stateV2

            try {
                val loader = PluginSettingsStateLoader(
                    project = project,
                    settingsClasses = listOf(SettingsV1::class, SettingsV2::class),
                    settingsMigrators = listOf(migrator),
                )
                loader.loadState(settings, state) shouldBe stateV2

                excludeRecords { SettingsV1.VERSION; SettingsV2.VERSION } // Do not verify calls on VERSION properties
                verify(exactly = 1) { project.getService(KtlintGlobalSettings::class.java) }
                verifyOrder {
                    globalSettingsState getProperty "stateVersion"
                    SettingsV1.getInstance(eq(project))
                    SettingsV2.buildEmptyState()
                    migrator.accepts(eq(settingsV1), eq(stateV2))
                    migrator.migrate(eq(settingsV1), eq(stateV2))
                    globalSettingsState setProperty "stateVersion" value 2
                }
                confirmVerified(project, globalSettingsState, settings, state, migrator)
                checkUnnecessaryStub(project, globalSettingsState, settings, state, migrator)
            } finally {
                unmockkObject(SettingsV1, SettingsV2)
            }
        }
    }
},)

// @formatter:off
class SettingsV1 : Settings<SettingsV1.StateV1> {
    class StateV1 : State
    companion object : SettingsCompanion<SettingsV1, StateV1> {
        override val VERSION: Int = 1
        override fun getInstance(project: Project): SettingsV1 = SettingsV1()
        override fun buildEmptyState(): StateV1 = StateV1()
    }
}
class SettingsV2 : Settings<SettingsV2.StateV2> {
    class StateV2 : State
    companion object : SettingsCompanion<SettingsV2, StateV2> {
        override val VERSION: Int = 2
        override fun getInstance(project: Project): SettingsV2 = SettingsV2()
        override fun buildEmptyState(): StateV2 = StateV2()
    }
}
object MigratorV1V2 : Migrator<SettingsV1, SettingsV2.StateV2> {
    override fun migrate(fromSettings: SettingsV1, toState: SettingsV2.StateV2) { /* Nothing to do */ }
    override fun accepts(fromSettings: AnyPluginSettingsDefinition, toState: State): Boolean =
        (fromSettings is SettingsV1) && (toState is SettingsV2.StateV2)
}
class SettingsV3 : Settings<SettingsV3.StateV3> {
    class StateV3 : State
    companion object : SettingsCompanion<SettingsV3, StateV3> {
        override val VERSION: Int = 3
        override fun getInstance(project: Project): SettingsV3 = SettingsV3()
        override fun buildEmptyState(): StateV3 = StateV3()
    }
}
object MigratorV2V3 : Migrator<SettingsV2, SettingsV3.StateV3> {
    override fun migrate(fromSettings: SettingsV2, toState: SettingsV3.StateV3) { /* Nothing to do */ }
    override fun accepts(fromSettings: AnyPluginSettingsDefinition, toState: State): Boolean =
        (fromSettings is SettingsV2) && (toState is SettingsV3.StateV3)
}
class SettingsV5 : Settings<SettingsV5.StateV5> {
    class StateV5 : State
    companion object : SettingsCompanion<SettingsV5, StateV5> {
        override val VERSION: Int = 5
        override fun getInstance(project: Project): SettingsV5 = SettingsV5()
        override fun buildEmptyState(): StateV5 = StateV5()
    }
}
// @formatter:on
