package com.pihanya.ktlint.config.migrate

import com.pihanya.ktlint.config.AnyPluginSettingsDefinition
import com.pihanya.ktlint.config.AnyPluginSettingsMigrator
import com.pihanya.ktlint.config.PluginSettingsDefinition
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

/**
 * TODO:
 *  These resolvers cause message in logs:
 *  ```txt
 *  WARN: Do not use URL connection as JarURLConnection
 *  ```
 *  ... so investigation of reason is required.
 */
private object TODO

internal fun findAllSettingsClasses(): List<KClass<out AnyPluginSettingsDefinition>> = Reflections(
    ConfigurationBuilder()
        .forPackage(PluginSettingsDefinition::class.java.packageName)
        .filterInputsBy(
            FilterBuilder()
                .includePackage(PluginSettingsDefinition::class.java.packageName),
        ),
).getSubTypesOf(PluginSettingsDefinition::class.java)
    .filter { Modifier.isPublic(it.modifiers) }
    .map(Class<out AnyPluginSettingsDefinition>::kotlin)

internal fun findAllSettingsMigrators(): List<AnyPluginSettingsMigrator> = Reflections(
    ConfigurationBuilder()
        .forPackage(PluginSettingsMigrator::class.java.packageName)
        .filterInputsBy(
            FilterBuilder()
                .includePackage(PluginSettingsMigrator::class.java.packageName),
        ),
).getSubTypesOf(PluginSettingsMigrator::class.java)
    .map(Class<out AnyPluginSettingsMigrator>::kotlin)
    .mapNotNull(KClass<out AnyPluginSettingsMigrator>::objectInstance)
