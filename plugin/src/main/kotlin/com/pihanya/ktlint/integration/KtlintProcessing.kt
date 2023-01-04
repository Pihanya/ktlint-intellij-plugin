package com.pihanya.ktlint.integration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiFile
import com.pihanya.ktlint.*
import com.pihanya.ktlint.config.KtlintPluginSettings
import com.pihanya.ktlint.config.KtlintPluginSettingsV1.StyleGuide
import com.pihanya.ktlint.config.RuleSetSettings
import com.pihanya.ktlint.config.RuleSetSettings.RuleSetSourceSettings
import com.pihanya.ktlint.util.*
import com.pinterest.ktlint.core.KtLintRuleEngine
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.*
import com.pinterest.ktlint.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.core.api.editorconfig.DISABLED_RULES_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.EditorConfigProperty
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

private object KtlintProcessing

private val logger: Logger = logger<KtlintProcessing>()

fun runKtlint(file: PsiFile, config: KtlintPluginSettings, shouldFormat: Boolean = false): LintResult {
    if (!config.enableKtlint) return LintResult.Empty

    val context: LintContext = buildLintContext(file, config, shouldFormat)
    return runKtlint(context)
}

fun runKtlint(context: LintContext): LintResult =
    if (!canLint(context)) {
        val immutableContext = if (context is LintContext.Mutable) context.asImmutable() else context
        LintResult.EmptyWithContext(immutableContext)
    } else {
        val engine = KtLintRuleEngine(
            ruleProviders = context.ruleProviders.asSequence().map(RuleProviderWrapper::provider).toSet(),
            editorConfigDefaults = EditorConfigDefaults.load(context.normalizedEditorConfigPaths.firstOrNull()),
            editorConfigOverride = context.editorConfigOverride,
        )
        doRunKtlint(engine, context)
    }

@Suppress("ReturnCount")
fun canLint(context: LintContext): Boolean {
    val foundRuleProviders = context.ruleProviders.isNotEmpty()
    if (!foundRuleProviders) return false

    val fileExtension = context.normalizedFilePath.toFile().extension
    val isKotlinFile = KOTLIN_FILE_EXTENSIONS.any { ext -> fileExtension.equals(ext, ignoreCase = true) }
    if (!isKotlinFile) return false

    val isFragmentFile = (context.normalizedFilePath.toString() == KOTLIN_FRAGMENT_FILE_NAME)
    if (isFragmentFile) return false

    return true
}

fun buildLintContext(file: PsiFile, config: KtlintPluginSettings, shouldFormat: Boolean): LintContext =
    LintContext.Mutable().apply {
        requireFormatting = shouldFormat
        originalText = file.text
        // Ktlint requires the absolute file path in order to search for .editorconfig files
        normalizedFilePath = Path.of(file.absolutePath).normalize()

        normalizedEditorConfigPaths += config.editorConfigPaths.asSequence()
            .filterNot(Path::isNullOrEmpty)
            .map(Path::absolutePathString)
            .map(Path::of).map(Path::normalize)
            .toList()

        ruleProviders += resolveRuleProviders(config, file)
        editorConfigOverride = resolveEditorConfigOverride(config)
        baselineErrors += resolveBaselineErrors(config, file)
    }.asImmutable()

// Exposed for testing
internal fun doRunKtlint(engine: KtLintRuleEngine, context: LintContext): LintResult {
    val lintResult = LintResult.Mutable(context)

    lintResult.exception = runCatching {
        // Clear editorconfig cache. (ideally, we could do this if .editorconfig files were changed)
        engine.trimMemory()
        if (!context.requireFormatting) {
            engine.lint(
                filePath = context.normalizedFilePath,
                callback = buildLinterCallback(lintResult),
            )
        } else {
            lintResult.formattedText = engine.format(
                filePath = context.normalizedFilePath,
                callback = buildFormatterCallback(lintResult),
            )
        }
    }
        .recoverCatching { ex -> if (ex !is KtLintParseException) throw ex }
        .exceptionOrNull()

    return lintResult.asImmutable()
}

private fun buildLinterCallback(result: LintResult.Mutable): (LintError) -> Unit = { lintError ->
    when {
        lintError.canBeAutoCorrected -> result.uncorrectedErrors += lintError
        result.context.baselineErrors.containsLintError(lintError).not() -> result.uncorrectedErrors += lintError
        else -> result.ignoredErrors += lintError
    }
}

private fun buildFormatterCallback(result: LintResult.Mutable): (LintError, Boolean) -> Unit = { lintError, corrected ->
    when {
        corrected -> result.correctedErrors += lintError
        result.context.baselineErrors.containsLintError(lintError).not() -> result.uncorrectedErrors += lintError
        else -> result.ignoredErrors += lintError
    }
}

private typealias EditorConfigPropertyOverride = Pair<EditorConfigProperty<*>, *>

private fun resolveEditorConfigOverride(config: KtlintPluginSettings): EditorConfigOverride =
    buildList<EditorConfigPropertyOverride> overrides@{
        run { // Apply code style (official/android)
            val codeStyleSet = when (config.styleGuide) {
                StyleGuide.OFFICIAL -> CodeStyleValue.official
                StyleGuide.ANDROID -> CodeStyleValue.android
            }
            this@overrides += (CODE_STYLE_PROPERTY to codeStyleSet)
        }

        val disabledRuleIds = config.ruleSetSettingsById.values.asSequence()
            .flatMap(RuleSetSettings::disabledRules)
            .toSet()

        // Take disabled rules into account
        if (disabledRuleIds.isNotEmpty()) {
            val overrideValue = disabledRuleIds.joinToString(separator = ",", transform = RuleId::asString)
            this@overrides += DISABLED_RULES_PROPERTY to overrideValue
        }

        // Also use additional .editorconfig(s) provided in settings
        if (config.editorConfigPaths.size > 1) {
            // TODO: We can implement functionality to use a few .editorconfig files to
            logger.debug("Multiple .editorconfig paths are not yet supported")
        }
    }.let { overrides ->
        @Suppress("SpreadOperator")
        EditorConfigOverride.from(*overrides.toTypedArray())
    }

private fun resolveRuleProviders(config: KtlintPluginSettings, file: PsiFile): Set<RuleProviderWrapper> =
    runCatching {
        val localJarPaths = config.ruleSetSettingsById.values.asSequence()
            .map(RuleSetSettings::sourceSettings)
            .filterIsInstance<RuleSetSourceSettings.LocalJar>()
            .map { localJarSettings ->
                if (localJarSettings.relativeToProject) {
                    file.project.resolveBasePath().resolve(localJarSettings.path)
                } else {
                    Path(localJarSettings.path)
                }
            }
            .toList()
        ruleLoaderService.load(localJarPaths)
    }
        .map { providers ->
            val disabledRuleSets = config.ruleSetSettingsById.values.asSequence()
                .filterNot(RuleSetSettings::active)
                .map(RuleSetSettings::ruleSetId)
                .toSet()
            providers.asSequence()
                .filter { provider -> provider.ruleSetId !in disabledRuleSets }
                .toSet()
        }
        .getOrElse { ex ->
            KtlintNotifier.notifyErrorWithSettings(
                project = file.project,
                subtitle = KtlintBundle.message("ktlint.errors.rules.couldNotLoadBundled"),
                content = ex.toString(),
            )
            emptySet()
        }

private fun resolveBaselineErrors(config: KtlintPluginSettings, file: PsiFile): List<LintError> {
    val baselineRules: Map<String, List<LintError>> = config.baselinePath?.toString()
        ?.let(::loadBaseline)
        ?.takeIf { baseline -> baseline.status == Baseline.Status.VALID }
        ?.lintErrorsPerFile
        ?.takeIf(Map<*, *>::isNotEmpty)
        ?: return emptyList()

    val fileName = file.absolutePath
    val projectBasePath = file.project.resolveBasePathOrNull()?.toString()
    val relativeProjectPath = if (!projectBasePath.isNullOrBlank()) { // Get relative path, if possible
        fileName
            .removePrefix(projectBasePath)
            .removePrefix("/")
    } else { // ... or else just take a real path
        fileName
    }

    return baselineRules[relativeProjectPath].orEmpty()
}
