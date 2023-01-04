package com.pihanya.ktlint

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.pihanya.ktlint.action.formatFileWithKtlint
import com.pihanya.ktlint.config.KtlintPluginSettings
import com.pihanya.ktlint.util.ktLintConfig

class KtlintPostFormatProcessor : PostFormatProcessor {

    override fun processElement(source: PsiElement, settings: CodeStyleSettings) =
        source

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        val config = source.project.ktLintConfig
        if (shouldLint(source, config)) {
            formatFileWithKtlint(project = source.project, source = source)
        }
        return rangeToReformat
    }

    companion object {

        // Perform linting when:
        private fun shouldLint(source: PsiFile, config: KtlintPluginSettings) = when {
            config.enableKtlint.not() -> false // (1) ... plugin is enabled;
            config.lintAfterReformat.not() -> false // (2) ... linting after reformat is enabled
            isInProject(source).not() -> false // (3) ... source file is in project
            (source.fileType.name != "Kotlin") -> false // (4) ... source file is a Kotlin file
            else -> true
        }

        private fun isInProject(source: PsiFile): Boolean {
            val virtualFile = source.virtualFile ?: return false
            return ProjectFileIndex.getInstance(source.project).isInContent(virtualFile)
        }
    }
}
