package com.pihanya.ktlint.integration

import java.nio.file.Path

interface RuleLoaderService<T : Any> {

    val name: String get() = this.javaClass.simpleName

    fun load(paths: List<Path>, skipErrors: Boolean = false): Set<T>
}
