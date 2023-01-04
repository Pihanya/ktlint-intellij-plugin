package com.pihanya.ktlint.intention

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.pihanya.ktlint.KTLINT
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.action.FormatAction
import com.pihanya.ktlint.util.isKtlintEnabled

class FormatIntention : BaseIntentionAction(), HighPriorityAction {

    override fun getFamilyName(): String = KTLINT

    override fun getText(): String = KtlintBundle.message("ktlint.intention.format.text")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        (file != null) && project.isKtlintEnabled

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file != null) FormatAction().runAction(project, file)
    }
}
