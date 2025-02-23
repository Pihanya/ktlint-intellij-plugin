package com.pihanya.ktlint

import com.pinterest.ktlint.core.KtLint

/**
 * Wrapper intended to allow mocking of Ktlint interactions, without invoking all its internals
 */
object KtLintWrapper {
    fun format(params: KtLint.ExperimentalParams) = KtLint.format(params)
    fun lint(params: KtLint.ExperimentalParams) = KtLint.lint(params)
    fun trimMemory() = KtLint.trimMemory()
}
