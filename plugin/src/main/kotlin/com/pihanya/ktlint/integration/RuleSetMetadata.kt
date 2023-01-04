package com.pihanya.ktlint.integration

data class RuleSetMetadata(
    val description: String?,
    val issueTrackerUrl: String?,
    val license: String?,
    val maintainer: String?,
    val repositoryUrl: String?,
) : Comparable<RuleSetMetadata> {

    override fun compareTo(other: RuleSetMetadata): Int =
        compareValuesBy(
            this,
            other,
            RuleSetMetadata::description,
            RuleSetMetadata::issueTrackerUrl,
            RuleSetMetadata::license,
            RuleSetMetadata::maintainer,
            RuleSetMetadata::repositoryUrl,
        )
}
