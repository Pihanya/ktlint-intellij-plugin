package com.pihanya.ktlint.util

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import java.nio.file.Path
import kotlin.io.path.Path

val Project.psiManager: PsiManager get() = service()

val Project.daemonCodeAnalyzer: DaemonCodeAnalyzer get() = DaemonCodeAnalyzer.getInstance(this)

val Project.psiDocumentManager: PsiDocumentManager get() = PsiDocumentManager.getInstance(this)

val documentManager: FileDocumentManager get() = FileDocumentManager.getInstance()

val localFileSystem: LocalFileSystem get() = LocalFileSystem.getInstance()

val notificationGroupManager: NotificationGroupManager get() = NotificationGroupManager.getInstance()

fun Project.resolveBasePath(): Path = checkNotNull(resolveBasePathOrNull()) { "Could not resolve project base path" }

fun Project.resolveBasePathOrNull(): Path? = (basePath ?: guessProjectDir()?.absolutePath)?.let(::Path)

val applicationInfo: ApplicationInfo get() = ApplicationInfo.getInstance()

val platformVersion: String get() = applicationInfo.fullVersion

val application: Application get() = ApplicationManager.getApplication()

val showSettingsUtil: ShowSettingsUtil get() = ShowSettingsUtil.getInstance()
