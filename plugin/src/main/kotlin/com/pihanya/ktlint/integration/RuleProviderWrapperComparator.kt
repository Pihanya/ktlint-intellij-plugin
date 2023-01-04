package com.pihanya.ktlint.integration

/**
 * Comparator for [RuleProviderWrapper] that takes such a precedence:
 * - standard rules ([KTLINT_RULESET_STANDARD]);
 * - experimental rules ([KTLINT_RULESET_EXPERIMENTAL]);
 * - all other rules.
 */
internal object RuleProviderWrapperComparator : Comparator<RuleProviderWrapper> {

    override fun compare(o1: RuleProviderWrapper?, o2: RuleProviderWrapper?): Int =
        COMPARATOR.compare(o1, o2)

    private val COMPARATOR: Comparator<RuleProviderWrapper> = compareBy<RuleProviderWrapper>(
        { it.ruleSetId.isStandard() },
        { it.ruleSetId.isExperimental() },
        { it.ruleSetId.isCustom() },
    ).reversed() then compareBy(
        RuleProviderWrapper::ruleSetId,
        RuleProviderWrapper::rulesetMetadata,
        { it.provider.let(System::identityHashCode) },
    )
}
