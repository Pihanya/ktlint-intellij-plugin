package com.pihanya.ktlint.action

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.config.KtlintConfig
import com.pihanya.ktlint.util.showSettingsUtil

class OpenSettingsAction(val project: Project) : NotificationAction(
    /* text = */ KtlintBundle.message("ktlint.actions.openSettings.text"),
) {

    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        showSettingsUtil.showSettingsDialog(project, KtlintConfig::class.java)
    }
}
