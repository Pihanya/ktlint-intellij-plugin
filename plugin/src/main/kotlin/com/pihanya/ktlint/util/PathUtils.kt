package com.pihanya.ktlint.util

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun String?.toNioPath(): Path? {
    if (this.isNullOrBlank()) {
        return null
    }

    val effectiveValue = localFileSystem.extractPresentableUrl(this)
    check(effectiveValue.isNotBlank())

    return when {
        File(effectiveValue).isFile.not() -> null
        else -> localFileSystem.findFileByPath(effectiveValue)?.toNioPath()
    }
}

fun Path?.isNullOrEmpty(): Boolean = (this == null) || (this.toString().isBlank())

fun Path?.toConfigString(): String = this
    ?.toString()
    ?.let(localFileSystem::extractPresentableUrl)
    .orEmpty()

fun Set<Path>.toVirtualFilesList(): MutableList<VirtualFile> = asSequence()
    .map(Path::toConfigString)
    .mapNotNull(localFileSystem::findFileByPath)
    .sortedBy(VirtualFile::getName)
    .toMutableList()

fun List<VirtualFile>.toPathsSet(): MutableSet<Path> = asSequence()
    .filter(VirtualFile::exists)
    .map(VirtualFile::toNioPath)
    .toMutableSet()

val VirtualFile.absolutePath: String get() = toNioPath().absolutePathString()

val PsiFile.absolutePath: String
    get() {
        val file: PsiFile = this

        file.viewProvider.document
            ?.let(documentManager::getFile)
            ?.let(VirtualFile::absolutePath)
            ?.let { return it }

        return (file.virtualFile ?: file.viewProvider.virtualFile)
            .let(VirtualFile::absolutePath)
    }
