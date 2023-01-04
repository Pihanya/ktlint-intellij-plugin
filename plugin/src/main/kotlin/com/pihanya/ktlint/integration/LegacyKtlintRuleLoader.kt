package com.pihanya.ktlint.integration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.pihanya.ktlint.util.uncheckedCast
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleProvider
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmName

class LegacyKtlintRuleLoader(
    private val providerLoader: RuleSetProviderLoader<LegacyRuleSetProviderWrapper>
) : RuleLoaderService<RuleProviderWrapper> {

    private val logger: Logger = logger<LegacyKtlintRuleLoader>()

    override fun load(paths: List<Path>, skipErrors: Boolean): SortedSet<RuleProviderWrapper> =
        providerLoader.load(paths).asSequence()
            .mapNotNull { setProvider ->
                runCatching { setProvider to setProvider.get() }
                    .recoverCatching { ex ->
                        if (skipErrors) {
                            logger.warn("Unexpected error while trying to resolve legacy rule sets", ex)
                            null
                        } else {
                            logger.error("Unexpected error while trying to resolve legacy rule sets", ex)
                            throw ex
                        }
                    }
                    .getOrThrow()
            }
            .flatMap { (setProvider, ruleSet) ->
                val tuple = runCatching { ruleSet.id to ruleSet.rules }
                    .onFailure { ex -> logger.error("Unexpected error while trying to use legacy rule sets", ex) }
                if (skipErrors && tuple.isFailure) return@flatMap emptySequence()

                val (ruleSetId, rules) = tuple.getOrThrow()
                rules.asSequence().map { rule ->
                    RuleProviderWrapper(
                        owner = setProvider,
                        ruleSetId = RuleSetId.fromString(ruleSetId),
                        provider = RuleProvider { rule },
                        rulesetMetadata = null,
                    )
                }
            }
            .toSortedSet(RuleProviderWrapperComparator)

    /**
     * Wrapper for [com.pinterest.ktlint.core.RuleSetProvider] that uses reflection
     * to delegate calls to original [value] object.
     *
     * @see [LegacyRuleSetWrapper]
     * @see [com.pinterest.ktlint.core.RuleSetProvider]
     */
    class LegacyRuleSetProviderWrapper internal constructor(internal val value: Any) {

        init {
            check(value::class.jvmName == ORIGINAL_CLASS)
        }

        /**
         * @see [com.pinterest.ktlint.core.RuleSetProvider.get]
         */
        fun get(): LegacyRuleSetWrapper = getFunction.call(value).let(::LegacyRuleSetWrapper)

        companion object {

            const val ORIGINAL_CLASS: String = "com.pinterest.ktlint.core.RuleSetProvider"

            /**
             * Original signature:
             * ```kt
             * public fun get(): com.pinterest.ktlint.core.RuleSet
             * ```
             *
             * @see [com.pinterest.ktlint.core.RuleSetProvider.get]
             */
            private val getFunction: KFunction<Any> by lazy {
                Class.forName(ORIGINAL_CLASS)
                    .kotlin.functions
                    .single cl@{ fn ->
                        if (fn.name != "get") return@cl false
                        if (fn.parameters.isNotEmpty()) return@cl false

                        val ruleSetKType: KType = Class.forName(LegacyRuleSetWrapper.ORIGINAL_CLASS).kotlin.createType()
                        if (fn.returnType != ruleSetKType) return@cl false

                        return@cl true
                    }
                    .let(::uncheckedCast)
            }
        }
    }

    /**
     * Wrapper for [com.pinterest.ktlint.core.RuleSet] that uses reflection
     * to delegate calls to original [value] object.
     *
     * @see [LegacyRuleSetProviderWrapper]
     * @see [com.pinterest.ktlint.core.RuleSet]
     */
    class LegacyRuleSetWrapper internal constructor(private val value: Any) : Iterable<Rule> {

        /**
         * @see [com.pinterest.ktlint.core.RuleSet.id]
         */
        val id: String get() = idProperty.call(value)

        /**
         * @see [com.pinterest.ktlint.core.RuleSet.rules]
         */
        val rules: Array<Rule> get() = rulesProperty.call(value)

        init {
            check(value::class.jvmName == ORIGINAL_CLASS)
        }

        override fun iterator(): Iterator<Rule> = rules.iterator()

        companion object {

            const val ORIGINAL_CLASS: String = "com.pinterest.ktlint.core.RuleSet"

            /**
             * Original signature:
             * ```kt
             * public val id: String
             * ```
             *
             * @see [com.pinterest.ktlint.core.RuleSet.id]
             */
            private val idProperty: KProperty<String> by lazy {
                Class.forName(ORIGINAL_CLASS)
                    .kotlin.memberProperties
                    .single cl@{ prop ->
                        if (prop.name != "id") return@cl false

                        val stringKType: KType = String::class.createType()
                        if (prop.returnType != stringKType) return@cl false

                        return@cl true
                    }
                    .let(::uncheckedCast)
            }

            /**
             * Original signature:
             * ```kt
             * public val rules: Array<Rule>
             * ```
             *
             * @see [com.pinterest.ktlint.core.RuleSet.rules]
             */
            private val rulesProperty: KProperty<Array<Rule>> by lazy {
                Class.forName(ORIGINAL_CLASS)
                    .kotlin.memberProperties
                    .single cl@{ prop ->
                        if (prop.name != "id") return@cl false

                        // Array<com.pinterest.ktlint.core.Rule>
                        val ruleArrayKType: KType = Array::class.createType(
                            arguments = listOf(KTypeProjection.invariant(Rule::class.createType())),
                        )
                        if (prop.returnType != ruleArrayKType) return@cl false

                        return@cl true
                    }
                    .let(::uncheckedCast)
            }
        }
    }
}
