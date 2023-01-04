package com.pihanya.ktlint.config

import com.intellij.openapi.project.Project

interface PluginSettingsDefinition<S : PluginSettingsDefinition.State> {

    interface State

    interface Companion<D : PluginSettingsDefinition<S>, S : State> {

        @Suppress("PropertyName", "VariableNaming")
        val VERSION: Int

        fun getInstance(project: Project): D

        fun buildEmptyState(): S
    }
}
