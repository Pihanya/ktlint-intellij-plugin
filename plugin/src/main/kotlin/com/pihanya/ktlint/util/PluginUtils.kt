package com.pihanya.ktlint.util

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.pihanya.ktlint.BuildConfig
import com.pihanya.ktlint.config.KtlintPluginSettings
import com.pihanya.ktlint.integration.AggregateRuleLoader
import com.pihanya.ktlint.integration.KtlintRuleLoader
import com.pihanya.ktlint.integration.LegacyKtlintRuleLoader
import com.pihanya.ktlint.integration.LegacyRuleSetProviderLoader
import com.pihanya.ktlint.integration.RuleLoaderService
import com.pihanya.ktlint.integration.RuleProviderWrapper
import com.pihanya.ktlint.integration.RuleSetV2ProviderLoader

val ruleLoaderService: RuleLoaderService<RuleProviderWrapper> = AggregateRuleLoader(
    delegateLoaderServices = listOf(
        KtlintRuleLoader(RuleSetV2ProviderLoader),
        LegacyKtlintRuleLoader(LegacyRuleSetProviderLoader),
    ),
)

val Project.ktLintConfig: KtlintPluginSettings get() = service()

val Project.isKtlintEnabled: Boolean get() = ktLintConfig.enableKtlint

val pluginVersion: String
    get() {
        val pluginId = PluginId.getId(BuildConfig.PLUGIN_ID)
        val plugin = requireNotNull(
            value = PluginManagerCore.getPlugin(pluginId),
            lazyMessage = { "Could not find '${BuildConfig.PLUGIN_ID}' plugin descriptor" },
        )
        return plugin.version
    }
