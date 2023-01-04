package com.pihanya.ktlint.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.pihanya.ktlint.KOTLIN_FILE_EXTENSIONS
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.integration.runKtlint
import com.pihanya.ktlint.util.documentManager
import com.pihanya.ktlint.util.isKtlintEnabled
import com.pihanya.ktlint.util.ktLintConfig
import com.pihanya.ktlint.util.psiDocumentManager
import com.pihanya.ktlint.util.psiManager

fun formatFileWithKtlint(project: Project, source: PsiFile) {
    val lintResult = runKtlint(source, project.ktLintConfig, shouldFormat = true)
    lintResult.exception?.let { ex -> throw ex } // Throw exception if present

    if (!lintResult.isEmpty()) {
        WriteCommandAction.runWriteCommandAction(
            /*     project = */ project,
            /* commandName = */ KtlintBundle.message("ktlint.actions.format.text"),
            /*     groupID = */ null,
            /*    runnable = */
            run@{
                val documentManager = project.psiDocumentManager
                val document = source.viewProvider.document ?: return@run
                documentManager.doPostponedOperationsAndUnblockDocument(document)
                document.setText(checkNotNull(lintResult.formattedText))
            },
            /* ...files = */ source,
        )
    }
}

// TODO: In case of formatting a directory with this action, it cannot be undone.
//    We need to investigate how to make IDEA give user a possibility to rollback formatting
class FormatAction : AnAction(
    /*        text = */ KtlintBundle.message("ktlint.actions.format.text"),
    /* description = */ KtlintBundle.message("ktlint.actions.format.description"),
    /*        icon = */ null,
) {

    private val logger: Logger = logger<FormatAction>()

    override fun update(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val virtualFiles = getFilesFromEvent(event)
        val ktlintEnabled = project.isKtlintEnabled
        for (virtualFile in virtualFiles) {
            // Enable autocorrect option only when:
            event.presentation.isEnabledAndVisible = when {
                ktlintEnabled.not() -> false // ... plugin is enabled
                virtualFile.fileSystem.isReadOnly -> false // ... file is editable
                // ... dealing with directory or Kotlin file;
                virtualFile.isDirectory.not() && (virtualFile.extension !in KOTLIN_FILE_EXTENSIONS) -> false
                else -> true
            }
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val virtualFiles = getFilesFromEvent(event)
        if (project.isKtlintEnabled.not()) return

        for (virtualFile in virtualFiles) {
            if (virtualFile.fileSystem.isReadOnly) {
                logger.trace("Skipping read-only file: ${virtualFile.path}")
                return
            }

            runCatching { lintFileNode(project, virtualFile) }
                .onFailure { logger.error("Unexpected error while performing action ${javaClass.name}", it) }
                .getOrThrow()
        }
    }

    internal fun runAction(project: Project, source: PsiFile) {
        val virtualFile = source.virtualFile ?: return
        forceUpdateFile(project, virtualFile)

        formatFileWithKtlint(project, source)

        virtualFile.refresh(/* asynchronous = */ false, /* recursive = */ false)
    }

    private fun lintFileNode(project: Project, file: VirtualFile) {
        when {
            file.exists().not() -> Unit
            file.isDirectory -> file.children.forEach { child -> lintFileNode(project, child) }
            else -> runAction(
                project = project,
                source = project.psiManager.findFile(file) ?: return,
            )
        }
    }

    private fun forceUpdateFile(project: Project, virtualFile: VirtualFile) {
        WriteCommandAction.runWriteCommandAction(project) action@{
            with(documentManager) {
                val document = getDocument(virtualFile) ?: return@action
                if (isDocumentUnsaved(document)) {
                    saveDocument(document)
                }
            }
        }
    }

    private fun getFilesFromEvent(event: AnActionEvent): List<VirtualFile> = sequence {
        val virtualFileVal = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val virtualFileArray = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        when {
            (virtualFileVal != null) && !virtualFileArray.isNullOrEmpty() -> yieldAll(virtualFileArray.asSequence())
            !virtualFileArray.isNullOrEmpty() -> yieldAll(virtualFileArray.asSequence())
            (virtualFileVal != null) -> yield(virtualFileVal)
        }
    }.toList()
}
