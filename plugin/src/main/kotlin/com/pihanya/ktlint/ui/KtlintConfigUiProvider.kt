package com.pihanya.ktlint.ui

import com.intellij.openapi.ui.DialogPanel

internal interface KtlintConfigUiProvider {

    fun createPanel(): DialogPanel
}
