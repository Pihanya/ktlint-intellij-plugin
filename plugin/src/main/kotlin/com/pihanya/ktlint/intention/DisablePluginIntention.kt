package com.pihanya.ktlint.intention

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.pihanya.ktlint.KTLINT
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.util.daemonCodeAnalyzer
import com.pihanya.ktlint.util.ktLintConfig

class DisablePluginIntention : BaseIntentionAction() {

    override fun getFamilyName(): String = KTLINT

    override fun getText(): String = KtlintBundle.message("ktlint.intention.disable.text")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        // Disable and restart analyzer to clear annotations
        project.ktLintConfig.enableKtlint = false
        project.daemonCodeAnalyzer.restart()
    }
}
