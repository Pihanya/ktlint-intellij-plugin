package com.pihanya.ktlint.util

/**
 * Wrapper around function to avoid recalculation when a function is queried again.
 */
fun <R> (() -> R).memoized(): () -> R = Memoize0(this)

/**
 * Wrapper around function to avoid recalculation when a function is queried again.
 */
fun <R> memoize(function: () -> R): () -> R = Memoize0(function)

/**
 * Wrapper around function with 0 arguments to avoid recalculation when a function queried again.
 */
class Memoize0<out R>(
    private val function: () -> R,
    private val onMemoize: (R) -> Unit = {},
) : () -> R {

    private var value: Pair<R, Unit>? = null

    override fun invoke(): R = when (val captured = value) {
        null -> function().also {
            value = it to Unit
            onMemoize(it)
        }
        else -> captured.first
    }
}
