package com.pihanya.ktlint

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.pihanya.ktlint.config.KtlintPluginSettings
import com.pihanya.ktlint.config.KtlintPluginSettingsV1.AnnotationMode
import com.pihanya.ktlint.integration.LintResult
import com.pihanya.ktlint.integration.RuleId
import com.pihanya.ktlint.integration.runKtlint
import com.pihanya.ktlint.intention.DisablePluginIntention
import com.pihanya.ktlint.intention.FormatIntention
import com.pihanya.ktlint.intention.GlobalDisableRuleIntention
import com.pihanya.ktlint.intention.LineDisableIntention
import com.pihanya.ktlint.util.ktLintConfig
import com.pinterest.ktlint.core.LintError

class KtlintAnnotator : ExternalAnnotator<LintResult, List<LintError>>() {

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): LintResult {
        val config = file.project.ktLintConfig
        if (config.enableKtlint.not() || config.annotationMode == AnnotationMode.NONE) {
            return LintResult.Empty
        }

        return runKtlint(file, config)
    }

    override fun doAnnotate(collectedInfo: LintResult): List<LintError> =
        collectedInfo.uncorrectedErrors

    override fun apply(file: PsiFile, errors: List<LintError>, holder: AnnotationHolder) {
        if (errors.isEmpty()) return

        val config = file.project.ktLintConfig
        for (lintError: LintError in errors) {
            handleLintError(lintError, config, file, holder)
        }
    }

    private fun handleLintError(
        lintError: LintError,
        config: KtlintPluginSettings,
        file: PsiFile,
        annotationHolder: AnnotationHolder,
    ) {
        val severity = when (config.annotationMode) {
            AnnotationMode.ERROR -> HighlightSeverity.ERROR
            AnnotationMode.WARNING -> HighlightSeverity.WARNING
            AnnotationMode.NONE -> return // Nothing to do
        }
        val errorRange = computeErrorTextRange(file, lintError)
        val message = "${lintError.detail} (${lintError.ruleId})"
        annotationHolder.newAnnotation(severity, message).apply {
            range(errorRange)
            withFix(DisablePluginIntention())
            if (lintError.canBeAutoCorrected) {
                withFix(FormatIntention())
            }
            withFix(LineDisableIntention(lintError))
            withFix(GlobalDisableRuleIntention(RuleId.fromString(lintError.ruleId)))
        }.create()
    }

    private fun computeErrorTextRange(file: PsiFile, lintError: LintError): TextRange {
        val document = file.viewProvider.document!!

        val lineIdx = lintError.line - 1
        val colIdx = lintError.col - 1

        val lineStartIdx = document.getLineStartOffset(lineIdx)
        val lineEndIdx = document.getLineEndOffset(lineIdx)

        val errorOffsetIdx = lineStartIdx + colIdx
        return file.findElementAt(errorOffsetIdx)
            ?.let { elem -> TextRange.from(errorOffsetIdx, elem.textLength) }
            // In case an indicated discern the indicated element return full line range
            ?: TextRange(lineStartIdx, lineEndIdx)
    }
}
