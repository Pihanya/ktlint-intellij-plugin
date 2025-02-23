package com.pihanya.ktlint.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.pihanya.ktlint.config
import com.pihanya.ktlint.doLint

class FormatAction : AnAction() {
    override fun update(event: AnActionEvent) {
        val files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return

        event.presentation.apply {
            isEnabledAndVisible = files.isNotEmpty() && project.config().enableKtlint
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return

        files.forEach { lintFile(project, it) }
    }

    private fun lintFile(project: Project, file: VirtualFile) {
        if (file.isDirectory) {
            file.children.forEach { lintFile(project, it) }
        } else if (file.extension in setOf("kt", "kts")) {
            PsiManager.getInstance(project).findFile(file)?.let {
                doLint(it, project.config(), true)
            }
        }
    }
}
