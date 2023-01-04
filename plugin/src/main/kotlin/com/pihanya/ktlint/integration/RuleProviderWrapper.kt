package com.pihanya.ktlint.integration

import com.pihanya.ktlint.util.memoized
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleProvider

class RuleProviderWrapper(

    /**
     * An original rule set provider of the given rule provider
     *
     * @see [com.pinterest.ktlint.core.RuleSetProviderV2]
     * @see [com.pihanya.ktlint.integration.LegacyKtlintRuleLoader.LegacyRuleSetWrapper]
     */
    val owner: Any,

    /**
     * Identifier of ruleset
     *
     * @see [com.pinterest.ktlint.core.RuleSetProviderV2.id]
     * @see [com.pihanya.ktlint.integration.LegacyKtlintRuleLoader.LegacyRuleSetWrapper.id]
     */
    val ruleSetId: RuleSetId,

    /**
     * Rule provider from ruleset with id [ruleSetId]
     */
    provider: RuleProvider,

    /**
     * Metadata about a ruleset (not a rule)
     */
    rulesetMetadata: RuleSetMetadata? = null,
) {

    private val memoizedRule: () -> Rule = provider::createNewRuleInstance.memoized()

    val provider: RuleProvider = RuleProvider(memoizedRule)

    val rule: Rule get() = memoizedRule()

    val ruleId: RuleId by lazy { RuleId.create(ruleSetId, rule.id) }

    /**
     * Non-empty info about ruleset or null
     * @see [RuleSetMetadata.isNullOrEmpty]
     */
    val rulesetMetadata: RuleSetMetadata? = rulesetMetadata.takeUnless(RuleSetMetadata?::isNullOrEmpty)

    override fun toString(): String =
        "RuleProviderWrapper(ruleSetId=$ruleSetId, provider=$provider, rulesetMetadata=$rulesetMetadata)"
}

private fun RuleSetMetadata?.isNullOrEmpty(): Boolean {
    if (this == null) return true
    return listOfNotNull(description, issueTrackerUrl, license, maintainer, repositoryUrl)
        .all(String::isNullOrBlank)
}
