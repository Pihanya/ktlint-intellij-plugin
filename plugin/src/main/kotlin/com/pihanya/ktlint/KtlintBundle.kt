package com.pihanya.ktlint

import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.kotlin.com.intellij.DynamicBundle
import java.util.function.Supplier

private const val BUNDLE_NAME = "locales.ktlintBundle"

object KtlintBundle : DynamicBundle(BUNDLE_NAME) {

    @Nls
    fun message(
        @PropertyKey(resourceBundle = BUNDLE_NAME) key: String,
        vararg params: Any,
    ): String = getMessage(key, *params)

    @Nls
    @Suppress("unused")
    fun messagePointer(
        @PropertyKey(resourceBundle = BUNDLE_NAME) key: String,
        vararg params: Any,
    ): Supplier<String> = getLazyMessage(key, *params)
}
