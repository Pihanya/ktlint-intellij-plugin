@file:Suppress("DEPRECATION", "unused")

package com.pihanya.ktlint.config.migrate

import com.pihanya.ktlint.config.AnyPluginSettingsDefinition
import com.pihanya.ktlint.config.AnyPluginSettingsState
import com.pihanya.ktlint.config.KtlintPluginSettingsV0
import com.pihanya.ktlint.config.KtlintPluginSettingsV1
import com.pihanya.ktlint.config.KtlintPluginSettingsV1.AnnotationMode
import com.pihanya.ktlint.config.KtlintPluginSettingsV1.StyleGuide
import com.pihanya.ktlint.config.RuleSetSettings
import com.pihanya.ktlint.config.RuleSetSettings.RuleSetSourceSettings
import com.pihanya.ktlint.integration.RuleId
import com.pihanya.ktlint.integration.RuleProviderWrapper
import com.pihanya.ktlint.integration.RuleSetId
import com.pihanya.ktlint.util.ruleLoaderService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * Treatment for Idea's love of custom shite.
 * Required for testing.
 *
 * @see [com.pihanya.ktlint.util.toNioPath]
 * @see [com.intellij.openapi.vfs.LocalFileSystem.extractPresentableUrl]
 */
private fun String?.toNioPath(): Path? {
    if (this.isNullOrBlank()) {
        return null
    }

    // See [com.intellij.openapi.vfs.LocalFileSystem.extractPresentableUrl]
    val effectiveValue = this.replace('/', File.separatorChar)
    check(effectiveValue.isNotBlank())

    return when {
        File(effectiveValue).isFile.not() -> null
        else -> File(effectiveValue).toPath()
    }
}

internal object KtlintPluginSettingsMigratorToV1 :
    PluginSettingsMigrator<KtlintPluginSettingsV0, KtlintPluginSettingsV1.State> {

    override fun migrate(
        fromSettings: KtlintPluginSettingsV0,
        toState: KtlintPluginSettingsV1.State
    ) {
        val disabledRules = run {
            val loadedIds: List<RuleId> = ruleLoaderService.load(
                paths = fromSettings.externalJarPaths.mapNotNull(String::toNioPath),
                skipErrors = true,
            ).map(RuleProviderWrapper::ruleId)

            fromSettings.disabledRules.mapNotNull { ruleId ->
                listOfNotNull(
                    RuleId.fromString(ruleId).takeIf { it in loadedIds },
                    RuleId.create(RuleSetId.EXPERIMENTAL, ruleId).takeIf { it in loadedIds },
                    loadedIds.firstOrNull { it.value == ruleId },
                ).firstOrNull()
            }
        }

        toState.apply {
            this.enableKtlint = fromSettings.enableKtlint
            this.lintAfterReformat = fromSettings.lintAfterReformat
            this.annotationMode = when {
                fromSettings.treatAsErrors -> AnnotationMode.ERROR
                fromSettings.hideErrors -> AnnotationMode.NONE
                else -> AnnotationMode.WARNING
            }
            this.styleGuide = when {
                fromSettings.androidMode -> StyleGuide.ANDROID
                else -> StyleGuide.OFFICIAL
            }
            this.ruleSetSettings = buildRulesetSettings(fromSettings)
            this.baselinePath = fromSettings.baselinePath
            this.editorConfigPaths = fromSettings.editorConfigPath?.let { mutableSetOf(it) } ?: mutableSetOf()
        }
    }

    private fun buildRulesetSettings(from: KtlintPluginSettingsV0): MutableSet<String> = sequence {
        val rulesByRulesetId = ruleLoaderService.load(
            paths = from.externalJarPaths.map(::Path),
            skipErrors = true
        ).groupBy(RuleProviderWrapper::ruleSetId)

        fun disabledRulesForRuleset(ruleSetId: RuleSetId): Set<RuleId> {
            val allRulesetRules = rulesByRulesetId[ruleSetId].orEmpty()

            val strictDisabledRules: MutableList<RuleId> = ArrayList(from.disabledRules.size)
            val lenientDisabledRules: MutableSet<String> = HashSet(from.disabledRules.size)
            for (ruleStr in from.disabledRules) {
                runCatching { RuleId.fromString(ruleStr, strict = true) }
                    .onSuccess { strictDisabledRules += it }
                    .onFailure { lenientDisabledRules += ruleStr }
            }

            return buildSet {
                addAll(
                    strictDisabledRules.asSequence()
                        .filter { it.ruleSetId == ruleSetId }
                )
                addAll(
                    allRulesetRules.asSequence()
                        .map { it.ruleId }
                        .filter { it.value in lenientDisabledRules }
                )
            }
        }

        yield(
            RuleSetSettings(
                active = true,
                ruleSetId = RuleSetId.STANDARD,
                descriptionOverride = null,
                disabledRules = disabledRulesForRuleset(RuleSetId.STANDARD),
                sourceSettings = RuleSetSourceSettings.Bundle,
            ),
        )
        yield(
            RuleSetSettings(
                active = from.useExperimental,
                ruleSetId = RuleSetId.EXPERIMENTAL,
                descriptionOverride = null,
                disabledRules = disabledRulesForRuleset(RuleSetId.EXPERIMENTAL),
                sourceSettings = RuleSetSourceSettings.Bundle,
            ),
        )

        for (externalJarPathStr in from.externalJarPaths) {
            val externalJarPath = Path(externalJarPathStr)
            if (externalJarPath.exists()) continue

            val ruleProvider = ruleLoaderService.load(listOf(externalJarPath), skipErrors = true)
                .firstOrNull { it.ruleSetId.isCustom() } ?: continue

            yield(
                RuleSetSettings(
                    active = true,
                    ruleSetId = ruleProvider.ruleSetId,
                    descriptionOverride = null,
                    disabledRules = disabledRulesForRuleset(ruleProvider.ruleSetId),
                    sourceSettings = RuleSetSourceSettings.LocalJar(
                        path = externalJarPathStr,
                        relativeToProject = false,
                    ),
                )
            )
        }
    }.map(Json::encodeToString).toMutableSet()

    override fun accepts(
        fromSettings: AnyPluginSettingsDefinition,
        toState: AnyPluginSettingsState
    ): Boolean = (fromSettings is KtlintPluginSettingsV0) && (toState is KtlintPluginSettingsV1.State)
}
