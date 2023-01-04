package com.pihanya.ktlint

import com.intellij.diagnostic.IdeaReportingEvent
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import com.pihanya.ktlint.util.platformVersion
import com.pihanya.ktlint.util.pluginVersion
import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder
import java.awt.Component

class KtlintErrorHandler : ErrorReportSubmitter() {

    override fun getReportActionText(): String = KtlintBundle.message("ktlint.errorHandler.actionText")

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        if (events.isEmpty()) return true

        return runCatching {
            useRollbar { rollbar: Rollbar ->
                for (event in events) {
                    val extraData = buildExtraData(event, additionalInfo)
                    when (event) {
                        is IdeaReportingEvent -> rollbar.error(event.data.throwable, extraData)
                        else -> rollbar.error(event.throwable, extraData, event.message)
                    }
                }
            }
            consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
            true
        }.getOrElse {
            consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.FAILED))
            false
        }
    }

    companion object {

        @Suppress("UNUSED_PARAMETER")
        private fun buildExtraData(
            event: IdeaLoggingEvent,
            additionalInfo: String?,
        ): Map<String, Any> = buildMap {
            put("last_action", IdeaLogger.ourLastActionId)
            put("additional_info", additionalInfo.orEmpty())
            put("plugin_version", pluginVersion)
            put("ide_version", platformVersion)
            put("java_vendor", SystemInfo.JAVA_VENDOR)
            put("java_version", SystemInfo.JAVA_RUNTIME_VERSION)
            put("os_name", SystemInfo.OS_NAME)
            put("os_version", SystemInfo.OS_VERSION)
            put("os_arch", SystemInfo.OS_ARCH)
        }.filterValues(String::isNotBlank)

        private fun useRollbar(block: (rollbar: Rollbar) -> Unit) {
            val config = ConfigBuilder.withAccessToken(BuildConfig.ROLLBAR_ACCESS_TOKEN).apply {
                environment("production")
                appPackages(listOf(BuildConfig.PLUGIN_ID))
                codeVersion(BuildConfig.PLUGIN_VERSION)
            }.build()

            Rollbar.init(config).apply {
                block(/* rollbar = */ this)
                close(/* wait = */ true)
            }
        }
    }
}
