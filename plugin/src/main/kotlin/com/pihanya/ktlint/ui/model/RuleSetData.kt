package com.pihanya.ktlint.ui.model

import com.pihanya.ktlint.integration.RuleId
import com.pihanya.ktlint.integration.RuleSetId
import com.pihanya.ktlint.integration.RuleSetMetadata

data class RuleSetData(

    val ruleSetId: RuleSetId,

    val ruleIds: Set<RuleId>,

    val metadata: RuleSetMetadata?,

    val state: State,
) {

    data class State(

        var active: Boolean,

        var disabledRuleIds: Set<RuleId>,

        var descriptionOverride: String?,

        var source: UiRuleSetSource,
    )

    sealed interface UiRuleSetSource {

        object Bundle : UiRuleSetSource

        data class LocalJar(var path: String, var useRelativePath: Boolean = false) : UiRuleSetSource

        data class ExternalJar(var uri: String, var ignoreInvalidCertificates: Boolean = false) : UiRuleSetSource
    }
}
