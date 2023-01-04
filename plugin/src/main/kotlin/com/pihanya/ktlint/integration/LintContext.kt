package com.pihanya.ktlint.integration

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.EditorConfigOverride
import java.nio.file.Path

sealed interface LintContext {

    val ruleProviders: Set<RuleProviderWrapper>
    val baselineErrors: List<LintError>
    val editorConfigOverride: EditorConfigOverride
    val normalizedEditorConfigPaths: List<Path>
    val normalizedFilePath: Path
    val originalText: String
    val requireFormatting: Boolean

    fun isEmpty(): Boolean =
        ruleProviders.isEmpty() && baselineErrors.isEmpty() &&
            (editorConfigOverride.properties == EditorConfigOverride.emptyEditorConfigOverride.properties) &&
            normalizedEditorConfigPaths.isNotEmpty() && normalizedFilePath.toString().isEmpty() &&
            originalText.isEmpty()

    fun asMutable(): Mutable = when (this) {
        is Mutable -> this
        else -> Mutable().also { mutable ->
            mutable.ruleProviders += ruleProviders
            mutable.baselineErrors += baselineErrors
            mutable.editorConfigOverride = editorConfigOverride
            mutable.normalizedEditorConfigPaths += normalizedEditorConfigPaths
            mutable.normalizedFilePath = normalizedFilePath
            mutable.originalText = originalText
        }
    }

    object Empty : LintContext {

        override val ruleProviders: Set<RuleProviderWrapper> = emptySet()
        override val baselineErrors: List<LintError> = emptyList()
        override val editorConfigOverride: EditorConfigOverride = EditorConfigOverride.emptyEditorConfigOverride
        override val normalizedEditorConfigPaths: List<Path> = emptyList()
        override val normalizedFilePath: Path = Path.of("")
        override val originalText: String = ""
        override val requireFormatting: Boolean = false

        override fun isEmpty(): Boolean = true
    }

    data class Immutable(
        override val ruleProviders: Set<RuleProviderWrapper>,
        override val baselineErrors: List<LintError>,
        override val editorConfigOverride: EditorConfigOverride,
        override val normalizedEditorConfigPaths: List<Path>,
        override val normalizedFilePath: Path,
        override val originalText: String,
        override val requireFormatting: Boolean,
    ) : LintContext

    class Mutable : LintContext {

        override val ruleProviders: MutableSet<RuleProviderWrapper> = mutableSetOf()
        override val baselineErrors: MutableList<LintError> = mutableListOf()
        override var editorConfigOverride: EditorConfigOverride = Empty.editorConfigOverride
        override val normalizedEditorConfigPaths: MutableList<Path> = mutableListOf()
        override var normalizedFilePath: Path = Empty.normalizedFilePath
        override var originalText: String = Empty.originalText
        override var requireFormatting: Boolean = false

        fun asImmutable(): LintContext = when {
            isEmpty() -> Empty
            else -> Immutable(
                ruleProviders = ruleProviders.toSet(),
                baselineErrors = baselineErrors.toList(),
                editorConfigOverride = editorConfigOverride,
                normalizedEditorConfigPaths = normalizedEditorConfigPaths.toList(),
                normalizedFilePath = normalizedFilePath,
                originalText = originalText,
                requireFormatting = requireFormatting,
            )
        }
    }
}
