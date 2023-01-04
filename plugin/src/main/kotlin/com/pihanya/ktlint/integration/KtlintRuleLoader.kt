package com.pihanya.ktlint.integration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.pinterest.ktlint.core.RuleSetProviderV2
import java.nio.file.Path
import java.util.*

class KtlintRuleLoader(
    private val providerLoader: RuleSetProviderLoader<RuleSetProviderV2>
) : RuleLoaderService<RuleProviderWrapper> {

    private val logger: Logger = logger<KtlintRuleLoader>()

    override fun load(paths: List<Path>, skipErrors: Boolean): SortedSet<RuleProviderWrapper> =
        providerLoader.load(paths).asSequence()
            .mapNotNull { provider: RuleSetProviderV2 ->
                runCatching { provider to provider.getRuleProviders() }
                    .recoverCatching { ex ->
                        if (skipErrors) {
                            logger.warn("Unexpected error while trying to resolve legacy rule sets", ex)
                            null
                        } else {
                            logger.error("Unexpected error while trying to resolve legacy rule sets", ex)
                            throw ex
                        }
                    }.getOrThrow()
            }
            .flatMap { (setProvider, ruleProviders) ->
                ruleProviders.asSequence().map { provider ->
                    RuleProviderWrapper(
                        owner = setProvider,
                        ruleSetId = RuleSetId.fromString(setProvider.id),
                        provider = provider,
                        rulesetMetadata = setProvider.about.let cl@{ about ->
                            if (about == RuleSetProviderV2.NO_ABOUT) return@cl null
                            RuleSetMetadata(
                                description = about.description,
                                issueTrackerUrl = about.issueTrackerUrl,
                                license = about.license,
                                maintainer = about.maintainer,
                                repositoryUrl = about.repositoryUrl,
                            )
                        },
                    )
                }
            }
            .toSortedSet(RuleProviderWrapperComparator)
}
