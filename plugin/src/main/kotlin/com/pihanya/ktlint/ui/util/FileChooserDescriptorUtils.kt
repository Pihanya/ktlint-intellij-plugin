package com.pihanya.ktlint.ui.util

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VirtualFile

fun createEditorconfigChooserDescriptor(): FileChooserDescriptor =
    createSingleFileChooserDescriptor(
        allowJars = true,
        fileFilter = { vf ->
            vf.name.startsWith(".editorconfig", ignoreCase = true) ||
                vf.extension.equals(".editorconfig", ignoreCase = true)
        },
    )

fun createJarsChooserDescriptor(): FileChooserDescriptor =
    createSingleFileChooserDescriptor(
        allowJars = true,
        fileFilter = { vFile -> vFile.extension.equals("jar", ignoreCase = true) },
    )

private inline fun createSingleFileChooserDescriptor(
    allowFiles: Boolean = true,
    allowJars: Boolean = false,
    showIncompatibleFiles: Boolean = false,
    crossinline fileFilter: (VirtualFile) -> Boolean = { true }
) = object : FileChooserDescriptor(
    /*       chooseFiles = */ allowFiles,
    /*     chooseFolders = */ false,
    /*        chooseJars = */ allowJars,
    /* chooseJarsAsFiles = */ allowJars,
    /* chooseJarContents = */ false,
    /*    chooseMultiple = */ false,
) {

    override fun isFileSelectable(file: VirtualFile?) =
        if (file == null) false else fileFilter(file)

    override fun isFileVisible(file: VirtualFile?, showHiddenFiles: Boolean) = when {
        (file == null) -> true
        file.isDirectory -> true
        showIncompatibleFiles -> true
        else -> fileFilter(file)
    }
}
