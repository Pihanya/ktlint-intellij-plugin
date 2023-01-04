package com.pihanya.ktlint.ui.model

import com.pihanya.ktlint.integration.RuleId

data class RuleStatus(var active: Boolean, val ruleId: RuleId)
