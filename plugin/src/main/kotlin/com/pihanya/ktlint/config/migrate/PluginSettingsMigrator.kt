package com.pihanya.ktlint.config.migrate

import com.pihanya.ktlint.config.PluginSettingsDefinition

internal interface PluginSettingsMigrator<in F, in T>
    where F : PluginSettingsDefinition<*>, T : PluginSettingsDefinition.State {

    fun migrate(fromSettings: F, toState: T)

    fun accepts(fromSettings: PluginSettingsDefinition<*>, toState: PluginSettingsDefinition.State): Boolean
}
