package com.pihanya.ktlint.integration

import com.pihanya.ktlint.integration.LegacyKtlintRuleLoader.LegacyRuleSetProviderWrapper
import com.pihanya.ktlint.util.toConfigString
import com.pinterest.ktlint.core.RuleSetProviderV2
import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.*

fun interface RuleSetProviderLoader<P : Any> {

    fun load(paths: List<Path>): List<P>
}

internal object RuleSetV2ProviderLoader : RuleSetProviderLoader<RuleSetProviderV2> {

    override fun load(paths: List<Path>): List<RuleSetProviderV2> =
        ServiceLoader.load(
            /* service = */ RuleSetProviderV2::class.java,
            /*  loader = */
            URLClassLoader(
                /*   urls = */ externalRulesetArray(paths),
                /* parent = */ RuleSetProviderV2::class.java.classLoader,
            ),
        ).toList()
}

internal object LegacyRuleSetProviderLoader : RuleSetProviderLoader<LegacyRuleSetProviderWrapper> {

    override fun load(paths: List<Path>): List<LegacyRuleSetProviderWrapper> {
        fun resolveClassOrNull(className: String): Class<*>? = runCatching { Class.forName(className) }.getOrNull()

        val ruleSetProviderClass: Class<*>? = resolveClassOrNull(LegacyRuleSetProviderWrapper.ORIGINAL_CLASS)
        val ruleSetClass: Class<*>? = resolveClassOrNull(LegacyKtlintRuleLoader.LegacyRuleSetWrapper.ORIGINAL_CLASS)
        if ((ruleSetProviderClass == null) || (ruleSetClass == null)) return emptyList()

        val serviceLoader = ServiceLoader.load(
            /* service = */ ruleSetProviderClass,
            /* loader  = */
            URLClassLoader(
                /* urls   = */ externalRulesetArray(paths),
                /* parent = */ ruleSetProviderClass.classLoader,
            ),
        )

        return serviceLoader.map(LegacyKtlintRuleLoader::LegacyRuleSetProviderWrapper)
    }
}

private fun externalRulesetArray(paths: List<Path>): Array<URL> = paths.asSequence()
    .map(Path::toConfigString)
    .map { it.replaceFirst(Regex("^~"), System.getProperty("user.home")) }
    .map(::File).filter(File::exists)
    .map(File::toURI).map(URI::toURL)
    .toList().toTypedArray()
