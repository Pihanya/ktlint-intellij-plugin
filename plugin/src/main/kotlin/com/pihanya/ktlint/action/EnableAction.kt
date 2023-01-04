package com.pihanya.ktlint.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.util.isKtlintEnabled
import com.pihanya.ktlint.util.ktLintConfig

class EnableAction :
    ToggleAction(
        /*        text = */ KtlintBundle.message("ktlint.actions.enable.text"),
        /* description = */ KtlintBundle.message("ktlint.actions.enable.description"),
        /*        icon = */ null,
    ),
    DumbAware {

    override fun isSelected(e: AnActionEvent): Boolean =
        e.project?.isKtlintEnabled != null

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.project?.ktLintConfig
            ?.apply { enableKtlint = state }
    }
}
