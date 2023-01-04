package com.pihanya.ktlint.integration

import com.pinterest.ktlint.core.LintError

sealed interface LintResult {

    val context: LintContext

    val correctedErrors: List<LintError>

    val uncorrectedErrors: List<LintError>

    val ignoredErrors: List<LintError>

    val formattedText: String?

    val exception: Throwable?

    fun isEmpty(): Boolean =
        correctedErrors.isEmpty() &&
            uncorrectedErrors.isEmpty() &&
            ignoredErrors.isEmpty() &&
            context.isEmpty() &&
            (formattedText == null) &&
            (exception == null)

    object Empty : LintResult {

        override val context: LintContext = LintContext.Empty
        override val correctedErrors: List<LintError> = emptyList()
        override val uncorrectedErrors: List<LintError> = emptyList()
        override val ignoredErrors: List<LintError> = emptyList()
        override val formattedText: String? = null
        override val exception: Throwable? = null

        override fun isEmpty(): Boolean = true
    }

    data class EmptyWithContext(override val context: LintContext = LintContext.Empty) : LintResult {

        override val correctedErrors: List<LintError> = emptyList()
        override val uncorrectedErrors: List<LintError> = emptyList()
        override val ignoredErrors: List<LintError> = emptyList()
        override val formattedText: String? = null
        override val exception: Throwable? = null

        override fun isEmpty(): Boolean = true
    }

    class Immutable constructor(
        override val context: LintContext,
        override val correctedErrors: List<LintError>,
        override val uncorrectedErrors: List<LintError>,
        override val ignoredErrors: List<LintError>,
        override val formattedText: String?,
        override val exception: Throwable?,
    ) : LintResult

    class Mutable(override val context: LintContext) : LintResult {

        override val correctedErrors: MutableList<LintError> = mutableListOf()
        override val uncorrectedErrors: MutableList<LintError> = mutableListOf()
        override val ignoredErrors: MutableList<LintError> = mutableListOf()
        override var formattedText: String? = Empty.formattedText
        override var exception: Throwable? = Empty.exception

        fun asImmutable(): LintResult {
            val immutableContext = if (context is LintContext.Mutable) context.asImmutable() else context
            return when {
                isEmpty() && context.isEmpty() -> Empty
                isEmpty() -> EmptyWithContext(immutableContext)
                else -> Immutable(
                    context = immutableContext,
                    correctedErrors = correctedErrors.toList(),
                    uncorrectedErrors = uncorrectedErrors.toList(),
                    ignoredErrors = ignoredErrors.toList(),
                    formattedText = formattedText,
                    exception = exception,
                )
            }
        }
    }
}
