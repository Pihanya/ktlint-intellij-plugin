package com.pihanya.ktlint.intention

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.pihanya.ktlint.KTLINT
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.config.RuleSetSettings
import com.pihanya.ktlint.integration.RuleId
import com.pihanya.ktlint.util.daemonCodeAnalyzer
import com.pihanya.ktlint.util.ktLintConfig

class GlobalDisableRuleIntention(private val ruleToDisable: RuleId) : BaseIntentionAction() {

    override fun getFamilyName(): String = KTLINT

    override fun getText(): String {
        val localeMessageId = when {
            ruleToDisable.ruleSetId.isExperimental() -> "ktlint.intention.disableExperimentalRule.globally.text"
            else -> "ktlint.intention.disableRule.globally.text"
        }
        val ruleIdRepresentation = when {
            ruleToDisable.ruleSetId.isStandard() || ruleToDisable.ruleSetId.isExperimental() -> ruleToDisable.value
            else -> ruleToDisable.asString()
        }
        return KtlintBundle.message(localeMessageId, ruleIdRepresentation)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    // TODO: Consider adding notification that allows to rollback disabled rule
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val config = project.ktLintConfig

        config.ruleSetSettingsById = config.ruleSetSettingsById.values.asSequence()
            .map { settings ->
                if (settings.ruleSetId != ruleToDisable.ruleSetId) {
                    settings
                } else {
                    settings.copy(disabledRules = settings.disabledRules + ruleToDisable)
                }
            }
            .associateBy(RuleSetSettings::ruleSetId)

        project.daemonCodeAnalyzer.restart()
    }
}
