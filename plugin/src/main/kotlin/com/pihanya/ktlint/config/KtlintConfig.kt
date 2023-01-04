package com.pihanya.ktlint.config

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.pihanya.ktlint.KTLINT
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.ui.FreshKtlintConfigUiProvider
import com.pihanya.ktlint.util.daemonCodeAnalyzer
import com.pihanya.ktlint.util.ktLintConfig

class KtlintConfig(private val project: Project) : BoundSearchableConfigurable(
    displayName = KtlintBundle.message("ktlint.conf.title"),
    helpTopic = KtlintBundle.message("ktlint.conf.title"),
) {

    override fun getId() = "com.pihanya.ktlint.config"

    private val settings: KtlintPluginSettings = project.ktLintConfig

    override fun createPanel(): DialogPanel = FreshKtlintConfigUiProvider(settings, project).createPanel()

    override fun apply() {
        super.apply()
        project.daemonCodeAnalyzer.restart()
    }

    override fun getDisplayName() = KTLINT
}
