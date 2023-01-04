package com.pihanya.ktlint.config.migrate

import com.pihanya.ktlint.config.RuleSetSettings
import com.pihanya.ktlint.integration.RuleId
import com.pihanya.ktlint.integration.RuleSetId
import com.pihanya.ktlint.integration.RuleSetV2ProviderLoader
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Suppress("DEPRECATION")
private typealias PrevSettings = com.pihanya.ktlint.config.KtlintPluginSettingsV0

private typealias NewSettings = com.pihanya.ktlint.config.KtlintPluginSettingsV1
private typealias AnnotationMode = com.pihanya.ktlint.config.KtlintPluginSettingsV1.AnnotationMode
private typealias StyleGuide = com.pihanya.ktlint.config.KtlintPluginSettingsV1.StyleGuide

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
class KtlintPluginSettingsMigratorToV1Tests : ShouldSpec({

    val migrator = KtlintPluginSettingsMigratorToV1
    lateinit var loadedRuleSets: MutableList<RuleSetProviderV2>

    fun mockProviderV2(ruleSetId: String, vararg ruleIds: String): RuleSetProviderV2 = mockk {
        every { id } returns ruleSetId
        every { getRuleProviders() } answers {
            ruleIds.asSequence()
                .map { ruleId -> RuleProvider { mockk { every { id } returns ruleId } } }
                .toSet()
        }
        every { about } returns RuleSetProviderV2.NO_ABOUT
    }

    beforeTest {
        // [com.pihanya.ktlint.util.ruleLoaderService] cannot be mocked.
        // Thanks god you allowed people create static shite.
        // We should think about using some lightweight IoC framework (koin/Kodein)
        loadedRuleSets = mutableListOf()
        mockkObject(RuleSetV2ProviderLoader)
        every { RuleSetV2ProviderLoader.load(any()) } answers { loadedRuleSets }
    }

    afterTest {
        loadedRuleSets.clear()
        unmockkObject(RuleSetV2ProviderLoader)
    }

    should("default config successfully migrated") {
        val newState = NewSettings.buildEmptyState()
        migrator.migrate(PrevSettings(), newState)
        newState.should {
            it.enableKtlint shouldBe true
            it.lintAfterReformat shouldBe true
            it.annotationMode shouldBe AnnotationMode.ERROR
            it.styleGuide shouldBe StyleGuide.OFFICIAL
            it.ruleSetSettings shouldBe setOf(
                RuleSetSettings(
                    active = false,
                    ruleSetId = RuleSetId.EXPERIMENTAL,
                    disabledRules = emptySet(),
                    sourceSettings = RuleSetSettings.RuleSetSourceSettings.Bundle,
                    descriptionOverride = null,
                ),
                RuleSetSettings(
                    active = true,
                    ruleSetId = RuleSetId.STANDARD,
                    disabledRules = emptySet(),
                    sourceSettings = RuleSetSettings.RuleSetSourceSettings.Bundle,
                    descriptionOverride = null,
                ),
            ).asSequence()
                .map(Json::encodeToString)
                .toSet()
            it.editorConfigPaths shouldBe emptySet()
            it.baselinePath.shouldBeNull()
        }
    }

    should("filled config successfully migrated") {
        val baselinePath = "/home/pihanya/.config/ktlint/baseline.xml"
        val editorConfigPath = "/home/pihanya/.config/ktlint/.editorconfig"
        val customJarPath = "/home/pihanya/.config/ktlint/custom-rules.jar"

        val disabledStandardRule = RuleId.create(RuleSetId.STANDARD, "filename")
        val disabledExperimentalRule = RuleId.create(RuleSetId.EXPERIMENTAL, "comment-wrapping")
        val disabledCustomRule = RuleId.create(RuleSetId("custom"), "custom-rule")

        loadedRuleSets = mutableListOf(
            mockProviderV2(disabledStandardRule.ruleSetId.value, disabledStandardRule.value),
            mockProviderV2(disabledExperimentalRule.ruleSetId.value, disabledExperimentalRule.value),
            mockProviderV2(disabledCustomRule.ruleSetId.value, disabledCustomRule.value),
        )

        val oldSettings = PrevSettings().apply {
            this.enableKtlint = true
            this.androidMode = true
            this.useExperimental = true
            this.treatAsErrors = false
            this.hideErrors = true
            this.lintAfterReformat = true
            this.disabledRules = listOf(
                disabledStandardRule.value,
                disabledExperimentalRule.value,
                disabledCustomRule.value,
            )
            this.baselinePath = baselinePath
            this.editorConfigPath = editorConfigPath
            this.externalJarPaths = listOf(customJarPath)
        }
        val newState = NewSettings.buildEmptyState()

        migrator.migrate(oldSettings, newState)
        newState.should {
            it.enableKtlint shouldBe true
            it.lintAfterReformat shouldBe true
            it.annotationMode shouldBe AnnotationMode.NONE
            it.styleGuide shouldBe StyleGuide.ANDROID
            it.ruleSetSettings shouldBe listOf(
                RuleSetSettings(
                    active = true,
                    ruleSetId = RuleSetId.STANDARD,
                    disabledRules = setOf(disabledStandardRule),
                    sourceSettings = RuleSetSettings.RuleSetSourceSettings.Bundle,
                    descriptionOverride = null,
                ),
                RuleSetSettings(
                    active = true,
                    ruleSetId = RuleSetId.EXPERIMENTAL,
                    disabledRules = setOf(disabledExperimentalRule),
                    sourceSettings = RuleSetSettings.RuleSetSourceSettings.Bundle,
                    descriptionOverride = null,
                ),
                RuleSetSettings(
                    active = true,
                    ruleSetId = RuleSetId("custom"),
                    disabledRules = setOf(disabledCustomRule),
                    sourceSettings = RuleSetSettings.RuleSetSourceSettings.LocalJar(
                        path = "/home/pihanya/.config/ktlint/custom-rules.jar",
                        relativeToProject = false,
                    ),
                    descriptionOverride = null,
                ),
            ).asSequence()
                .map(Json::encodeToString)
                .toSet()
            it.editorConfigPaths shouldBe setOf(editorConfigPath)
            it.baselinePath shouldBe baselinePath
        }
    }
},)
