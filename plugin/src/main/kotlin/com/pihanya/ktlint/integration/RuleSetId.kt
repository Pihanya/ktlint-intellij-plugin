package com.pihanya.ktlint.integration

import com.pihanya.ktlint.KTLINT_RULESET_EXPERIMENTAL
import com.pihanya.ktlint.KTLINT_RULESET_STANDARD
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class RuleSetId(val value: String) : Comparable<RuleSetId> {

    override fun compareTo(other: RuleSetId): Int =
        compareValuesBy(
            this,
            other,
            RuleSetId::value,
            { it == EXPERIMENTAL },
            { it == STANDARD },
        )

    fun isStandard(): Boolean = (value == KTLINT_RULESET_STANDARD)

    fun isExperimental(): Boolean = (value == KTLINT_RULESET_EXPERIMENTAL)

    fun isBundled(): Boolean = isStandard() || isExperimental()

    fun isCustom(): Boolean = isBundled().not()

    internal fun asString(): String = value

    override fun toString(): String = "RuleSetId($value)"

    companion object {

        val STANDARD: RuleSetId = RuleSetId(KTLINT_RULESET_STANDARD)

        val EXPERIMENTAL: RuleSetId = RuleSetId(KTLINT_RULESET_EXPERIMENTAL)

        internal fun fromString(value: String): RuleSetId {
            check(value.isNotBlank()) { "Appeared value to be non-blank string: [$value]" }
            return RuleSetId(value)
        }
    }
}
