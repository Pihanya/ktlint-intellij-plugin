package com.pihanya.ktlint.integration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import java.nio.file.Path

class AggregateRuleLoader(
    private val delegateLoaderServices: List<RuleLoaderService<RuleProviderWrapper>>
) : RuleLoaderService<RuleProviderWrapper> {

    private val logger: Logger = logger<AggregateRuleLoader>()

    override fun load(paths: List<Path>, skipErrors: Boolean): Set<RuleProviderWrapper> = sequence {
        for (loaderService in delegateLoaderServices) {
            val providers = loaderService.load(paths, skipErrors)
            logLoadedRules(loaderService, providers)
            yieldAll(providers)
        }
    }.toSet()

    private fun logLoadedRules(
        loaderService: RuleLoaderService<*>,
        providers: Collection<RuleProviderWrapper>,
    ) = logger.trace {
        buildString(DEFAULT_BUFFER_SIZE) str@{
            if (providers.isEmpty()) {
                append("[${loaderService.name}] No rule providers were loaded")
                return@str
            }

            val providerIdsByRuleSetId = providers.groupBy(
                keySelector = RuleProviderWrapper::ruleSetId,
                valueTransform = RuleProviderWrapper::ruleId,
            )

            /**
             * Example:
             * ```txt
             * [KtlintRuleLoader] Loaded rules from providers: {
             *     standard(5) -> [
             *         rule1, rule2, rule3,
             *         rule4, rule5
             *     ]
             *     custom(2) -> [rule1, rule2]
             * }
             * ```
             */
            val indent = "    "
            val doubleIndent = "$indent$indent"

            @Suppress("MagicNumber")
            val rulesPerLine = 3

            append("[${loaderService::class.simpleName}] Loaded rules from providers: {\n")
            for ((ruleSetId, ruleIds) in providerIdsByRuleSetId) {
                val rulesCount = ruleIds.size
                append(indent, ruleSetId, '(', rulesCount, ')', " -> [")
                if (ruleIds.size <= rulesPerLine) {
                    ruleIds.joinTo(buffer = this@str)
                } else {
                    ruleIds.chunked(rulesPerLine).joinTo(
                        buffer = this@str,
                        prefix = "\n$doubleIndent",
                        transform = { idsChunk -> idsChunk.joinToString() },
                        separator = ",\n$doubleIndent",
                        postfix = "\n$indent",
                    )
                }
                append(']', '\n')
            }
            append('}')
        }
    }
}
