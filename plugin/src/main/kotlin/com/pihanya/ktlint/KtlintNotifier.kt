package com.pihanya.ktlint

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.pihanya.ktlint.action.OpenSettingsAction
import com.pihanya.ktlint.util.notificationGroupManager

object KtlintNotifier {

    fun notifyErrorWithSettings(project: Project, subtitle: String, content: String) {
        notificationGroupManager
            .getNotificationGroup(KTLINT)
            .createNotification(
                /*   title = */ KtlintBundle.message("ktlint.errors.errorWithSettings.title", subtitle),
                /* content = */ KtlintBundle.message("ktlint.errors.errorWithSettings.content", content),
                /*    type = */ NotificationType.ERROR,
            )
            .addAction(OpenSettingsAction(project))
            .notify(project)
    }
}
