package com.pihanya.ktlint.config

import com.pihanya.ktlint.integration.RuleId
import com.pihanya.ktlint.integration.RuleSetId
import kotlinx.serialization.Serializable

@Serializable
data class RuleSetSettings(

    val active: Boolean,

    val ruleSetId: RuleSetId,

    val descriptionOverride: String?,

    val disabledRules: Set<RuleId>,

    val sourceSettings: RuleSetSourceSettings
) {

    @Serializable
    sealed interface RuleSetSourceSettings {

        @Serializable
        object Bundle : RuleSetSourceSettings

        @Serializable
        data class LocalJar(
            val path: String,
            val relativeToProject: Boolean,
        ) : RuleSetSourceSettings

        @Serializable
        data class ExternalJar(
            val url: String,
            val ignoreInvalidCerts: Boolean,
        ) : RuleSetSourceSettings
    }
}
