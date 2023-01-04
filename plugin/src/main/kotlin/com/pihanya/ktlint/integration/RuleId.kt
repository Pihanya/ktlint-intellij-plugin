package com.pihanya.ktlint.integration

import kotlinx.serialization.Serializable

@Serializable
data class RuleId internal constructor(val ruleSetId: RuleSetId, val value: String) : Comparable<RuleId> {

    override fun compareTo(other: RuleId): Int =
        compareValuesBy(this, other, RuleId::ruleSetId, RuleId::value)

    fun asString(): String {
        val ruleSetIdValue = ruleSetId.value
        check(ruleSetIdValue.indexOf(SEPARATOR_CHAR) == -1) {
            "Rule Set ID [$ruleSetIdValue] appeared to contain unexpected char [$SEPARATOR_CHAR]"
        }
        check(value.indexOf(SEPARATOR_CHAR) == -1) {
            "Rule ID [$value] appeared to contain unexpected char [$SEPARATOR_CHAR]"
        }
        return ruleSetIdValue + SEPARATOR_CHAR + value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RuleId
        if (ruleSetId != other.ruleSetId) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int {
        var result = ruleSetId.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String = "RuleId(ruleSetId=${ruleSetId.value}, value='$value')"

    companion object {

        private const val SEPARATOR_CHAR: Char = ':'

        fun create(ruleSetId: RuleSetId, value: String): RuleId {
            val effectiveValue: String = run {
                val tokens = value.split(SEPARATOR_CHAR)
                when {
                    (tokens.size == 1) -> tokens[0]
                    (tokens.size == 2) && (tokens.first() == ruleSetId.value) -> tokens[1]
                    else -> throw IllegalArgumentException("Unsupported value in constructor [$ruleSetId, $value]")
                }
            }
            return RuleId(ruleSetId, effectiveValue)
        }

        fun fromString(value: String, strict: Boolean = false): RuleId {
            val tokens = value.split(SEPARATOR_CHAR)
            check(tokens.size <= 2) {
                "Expected rule id" +
                    " in format [\${ruleSetId}$SEPARATOR_CHAR\${ruleId}]. Got [$value]"
            }
            check((tokens.size > 1) || !strict) { "Rule id must consist of tokens separated with '$SEPARATOR_CHAR'" }

            return when {
                (tokens.size == 1) -> RuleId(RuleSetId.STANDARD, tokens.first())
                else -> RuleId(RuleSetId.fromString(tokens[0]), value = tokens[1])
            }
        }
    }
}
