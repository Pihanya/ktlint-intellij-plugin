package com.pihanya.ktlint.ui.model

import com.pihanya.ktlint.integration.RuleSetId

data class RuleSetStatus(var active: Boolean, val ruleSetId: RuleSetId)
