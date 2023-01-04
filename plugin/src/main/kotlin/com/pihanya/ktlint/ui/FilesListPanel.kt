package com.pihanya.ktlint.ui

import com.intellij.CommonBundle
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.openapi.util.NlsContexts.Label
import com.intellij.openapi.vcs.changes.ui.VirtualFileListCellRenderer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.pihanya.ktlint.ui.model.FileListListModel
import javax.swing.JPanel
import javax.swing.ListSelectionModel

@Suppress("UnstableApiUsage") // For NlsContexts annotations
internal class FilesListPanel(
    private val project: Project,
    @DialogTitle private val title: String,
    @Label private val description: String,
    private val listModel: FileListListModel,
    private val descriptorProvider: () -> FileChooserDescriptor
) {

    private val list: JBList<VirtualFile> = JBList(listModel).apply {
        dragEnabled = false
        selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        cellRenderer = VirtualFileListCellRenderer(project)
    }

    fun createPanel(): JPanel = ToolbarDecorator.createDecorator(list)
        .setAddAction { onAddFileClick(listModel) }
        .setRemoveAction { onRemoveFileClick(listModel) }
        .setButtonComparator(CommonBundle.message("button.add"), CommonBundle.message("button.remove"))
        .disableUpDownActions()
        .createPanel()

    private fun onRemoveFileClick(listModel: FileListListModel) {
        listModel.removeAt(list.selectedIndices.toList()).isNotEmpty()
    }

    private fun onAddFileClick(listModel: FileListListModel) {
        val descriptor = descriptorProvider().apply {
            title = this@FilesListPanel.title
            description = this@FilesListPanel.description
        }

        val files = FileChooser.chooseFiles(descriptor, list, project, null)
        for (file: VirtualFile? in files) {
            if ((file != null) && (file !in listModel.items)) {
                listModel += file
            }
        }
    }
}
