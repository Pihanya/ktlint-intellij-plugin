package com.pihanya.ktlint

import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.pihanya.ktlint.config.KtlintPluginSettings
import com.pihanya.ktlint.config.KtlintPluginSettingsV1.StyleGuide
import com.pihanya.ktlint.integration.LintContext
import com.pihanya.ktlint.integration.LintResult
import com.pihanya.ktlint.integration.RuleProviderWrapper
import com.pihanya.ktlint.integration.canLint
import com.pihanya.ktlint.integration.doRunKtlint
import com.pihanya.ktlint.integration.runKtlint
import com.pinterest.ktlint.core.KtLintRuleEngine
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.KtLintParseException
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.nio.file.Path

class KtlintProcessingTest : ShouldSpec({

    lateinit var config: KtlintPluginSettings

    beforeTest {
        config = mockk {
            every { enableKtlint } returns true
            every { styleGuide } returns StyleGuide.OFFICIAL
            every { ruleSetSettingsById } returns emptyMap()
            every { baselinePath } returns null
            every { editorConfigPaths } returns emptySet()
        }
    }

    fun mockFile(): PsiFile = mockk(relaxed = true) {
        every { project } returns mockk<Project>().apply {
            every { basePath } returns null
        }
        every { viewProvider } returns mockk<FileViewProvider>().apply {
            every { document } returns null
        }
    }

    should("run successfully on required data provided") {
        val engine = mockk<KtLintRuleEngine>()
        every { engine.trimMemory() } just runs
        every { engine.lint(any<Path>(), any()) } just runs

        val context = LintContext.Mutable().apply {
            // Constructor of [KtLint.ExperimentalParams] require at least one provider in arguments
            ruleProviders += mockk<RuleProviderWrapper> {
                every { provider } returns mockk provider@{
                    // in ktlint v0.47.1 constructor of [KtLint.ExperimentalParams] evaluates rules eventually
                    // ... but it will be corrected in further versions
                    every { createNewRuleInstance() } returns Rule(id = "test")
                }
            }
        }

        doRunKtlint(engine, context) should {
            it.correctedErrors.shouldBeEmpty()
            it.uncorrectedErrors.shouldBeEmpty()
            it.ignoredErrors.shouldBeEmpty()
            it.exception.shouldBeNull()
        }

        verify(exactly = 1) { engine.lint(any<Path>(), any()) }
    }

    should("linting pass by without errors on ParseException thrown by ktlint") {
        val engine = mockk<KtLintRuleEngine>()
        every { engine.lint(any<Path>(), any()) } throws KtLintParseException(0, 0, "test")
        every { engine.trimMemory() } just runs

        val context = LintContext.Mutable().apply {
            ruleProviders += mockk<RuleProviderWrapper> {
                every { provider } returns mockk provider@{
                    every { createNewRuleInstance() } returns Rule(id = "test")
                }
            }
        }

        doRunKtlint(engine, context) should {
            it.correctedErrors.shouldBeEmpty()
            it.uncorrectedErrors.shouldBeEmpty()
            it.ignoredErrors.shouldBeEmpty()
            it.exception.shouldBeNull() // No exceptions tracked
        }

        verify(exactly = 1) { engine.lint(any<Path>(), any()) }
    }

    should("any other exception be should rethrown as is") {
        val engine = mockk<KtLintRuleEngine>()
        val exception = IllegalStateException()
        every { engine.lint(any<Path>(), any()) } throws exception
        every { engine.trimMemory() } just runs

        val context = LintContext.Mutable().apply {
            ruleProviders += mockk<RuleProviderWrapper> {
                every { provider } returns mockk provider@{
                    every { createNewRuleInstance() } returns Rule(id = "test")
                }
            }
        }

        doRunKtlint(engine, context).exception shouldBe exception

        verify(exactly = 1) { engine.lint(any<Path>(), any()) }
    }

    should("return LintResult.Empty when Ktlint disabled in config") {
        every { config.enableKtlint } returns false
        runKtlint(mockFile(), config, shouldFormat = false) shouldBe LintResult.Empty
        runKtlint(mockFile(), config, shouldFormat = true) shouldBe LintResult.Empty
    }

    context("return LintResult.EmptyWithContext when linting is not possible") {

        should("when no rule providers") {
            val expectedContext: LintContext.Immutable = mockk()
            val context: LintContext.Mutable = mockk {
                every { ruleProviders } returns mutableSetOf() // Empty set
                every { asImmutable() } returns expectedContext
            }
            canLint(context) shouldBe false
            runKtlint(context) should { result ->
                result.shouldBeTypeOf<LintResult.EmptyWithContext>()
                result.context shouldBe expectedContext
            }
        }

        should("when dealing with a non-kotlin file") {
            val expectedContext: LintContext.Immutable = mockk()

            val mockPath: Path = mockk {
                every { toFile() } returns mockk {
                    every { extension } returns "java" // Non Kotlin extension
                }
            }
            val context: LintContext.Mutable = mockk {
                every { ruleProviders } returns mutableSetOf(mockk())
                every { normalizedFilePath } returns mockPath
                every { asImmutable() } returns expectedContext
            }

            canLint(context) shouldBe false
            runKtlint(context) should { result ->
                result.shouldBeTypeOf<LintResult.EmptyWithContext>()
                result.context shouldBe expectedContext
            }
        }

        should("when dealing with a fragment file") {
            val expectedContext: LintContext.Immutable = mockk()

            val mockPath: Path = mockk path@{
                every { toFile() } returns mockk {
                    every { extension } returns "kt"
                }
                every { this@path.toString() } returns KOTLIN_FRAGMENT_FILE_NAME // Fragment files are not linted
            }
            val context: LintContext.Mutable = mockk {
                every { ruleProviders } returns mutableSetOf(mockk())
                every { normalizedFilePath } returns mockPath
                every { asImmutable() } returns expectedContext
            }

            canLint(context) shouldBe false
            runKtlint(context) should { result ->
                result.shouldBeTypeOf<LintResult.EmptyWithContext>()
                result.context shouldBe expectedContext
            }
        }
    }
},)
