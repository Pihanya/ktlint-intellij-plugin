package com.pihanya.ktlint.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.Gaps
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.dsl.gridLayout.VerticalGaps
import com.intellij.util.ui.JBUI
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.config.KtlintPluginSettings
import com.pihanya.ktlint.config.RuleSetSettings
import com.pihanya.ktlint.config.RuleSetSettings.RuleSetSourceSettings
import com.pihanya.ktlint.integration.RuleProviderWrapper
import com.pihanya.ktlint.ui.FreshKtlintConfigUiProvider.Companion.validateAsFilePath
import com.pihanya.ktlint.ui.util.createJarsChooserDescriptor
import com.pihanya.ktlint.util.resolveBasePath
import com.pihanya.ktlint.util.ruleLoaderService
import com.pihanya.ktlint.util.toNioPath
import javax.swing.Timer
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

private const val DEFAULT_DESCRIPTION: String = ""
private const val DEFAULT_LOCAL_JAR_PATH: String = ""
private const val DEFAULT_LOCAL_JAR_STORE_RELATIVE: Boolean = false
private const val DEFAULT_EXTERNAL_JAR_URL: String = ""
private const val DEFAULT_EXTERNAL_JAR_IGNORE_INVALID_CERTS: Boolean = false

class AddRulesetPanel(
    private val project: Project,
    private val settings: KtlintPluginSettings,
) : KtlintConfigUiProvider {

    data class State(
        var description: String = DEFAULT_DESCRIPTION,
        var localJarPath: String = DEFAULT_LOCAL_JAR_PATH,
        var localJarStoreRelative: Boolean = DEFAULT_LOCAL_JAR_STORE_RELATIVE,
        var externalJarUrl: String = DEFAULT_EXTERNAL_JAR_URL,
        var externalJarIgnoreInvalidCerts: Boolean = DEFAULT_EXTERNAL_JAR_IGNORE_INVALID_CERTS,

        var localJarRadioSelected: Boolean = true,
        var externalJarRadioSelected: Boolean = false
    )

    @Suppress("UnstableApiUsage")
    override fun createPanel(): DialogPanel = panel {
        val state = State()
        descriptionRow(state)
        buttonGroup {
            localJarRows(state)
            // externalJarRow(state)
            externalJarRowComingSoon()
        }

        onIsModified {
            when {
                state.localJarRadioSelected -> {
                    val newPath = state.localJarPath.toNioPath() ?: return@onIsModified false

                    val configJarPaths = settings.ruleSetSettingsById.values.asSequence()
                        .filterIsInstance<RuleSetSourceSettings.LocalJar>()
                        .filter { it.relativeToProject == state.localJarStoreRelative }
                        .map(RuleSetSourceSettings.LocalJar::path)
                        .map(::Path)
                        .toSet()

                    newPath !in configJarPaths
                }

                state.externalJarRadioSelected -> TODO()

                else -> false
            }
        }

        onApply {
            when {
                state.localJarRadioSelected -> {
                    val newPath = state.localJarPath.toNioPath() ?: return@onApply

                    val ruleSetId = ruleLoaderService.load(listOf(newPath)).asSequence()
                        .filter { it.ruleSetId.isCustom() } // TODO: Add resolution that not includes standard rules
                        .map(RuleProviderWrapper::ruleSetId)
                        .distinct()
                        .singleOrNull()

                    // TODO: Add human readable error displaying
                    check(ruleSetId != null) { "JAR with path [$newPath] does not contain any rules" }

                    val effectiveNewPath = if (state.localJarStoreRelative) {
                        newPath.relativeTo(project.resolveBasePath())
                    } else {
                        newPath
                    }

                    val newSettings = buildMap(settings.ruleSetSettingsById.size + 1) {
                        putAll(settings.ruleSetSettingsById)
                        RuleSetSettings(
                            active = true,
                            ruleSetId = ruleSetId,
                            descriptionOverride = state.description.takeUnless(String::isNullOrBlank),
                            disabledRules = emptySet(),
                            sourceSettings = RuleSetSourceSettings.LocalJar(
                                path = effectiveNewPath.toString(),
                                relativeToProject = state.localJarStoreRelative,
                            ),
                        ).let { put(ruleSetId, it) }
                    }
                    settings.ruleSetSettingsById = newSettings
                }

                state.externalJarRadioSelected -> {
                    TODO()
                }
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Panel.descriptionRow(state: State): Row = row {
        textField()
            .horizontalAlign(HorizontalAlign.FILL)
            .label(KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.description.title"))
            .bindText(state::description)
    }.bottomGap(BottomGap.SMALL)

    @Suppress("UnstableApiUsage")
    private fun Panel.localJarRows(state: State): RowsRange = rowsRange {
        lateinit var localJarRadio: Cell<JBRadioButton>

        row {
            localJarRadio = radioButton(KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.localJar.title"))
                .bindSelected(state::localJarRadioSelected)
        }
        panel {
            row {
                val label = label(KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.localJar.path.title"))
                    .verticalAlign(VerticalAlign.TOP)
                    .component
                    .apply { toolTipText = KtlintBundle.message("ktlint.conf.baseLinePath.tooltip") }

                textFieldWithBrowseButton(
                    browseDialogTitle = KtlintBundle.message("ktlint.conf.baseLinePath.dialog.title"),
                    project = project,
                    fileChooserDescriptor = createJarsChooserDescriptor().apply {
                        description = KtlintBundle.message("ktlint.conf.baseLinePath.dialog.description")
                    },
                )
                    .bindText(state::localJarPath)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnInput { textField -> validateAsFilePath(textField.text, causeError = false) }
                    .validationOnApply { textField -> validateAsFilePath(textField.text) }
                    .applyToComponent { label.labelFor = this }
            }.layout(RowLayout.PARENT_GRID)

            row("") {
                checkBox(KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.localJar.storeRelative.title"))
                    .horizontalAlign(HorizontalAlign.FILL)
                    .applyToComponent {
                        toolTipText =
                            KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.localJar.storeRelative.tooltip")
                    }
                    .bindSelected(state::localJarStoreRelative)
            }
        }
            .customize(Gaps(left = JBUI.scale(20)))
            .enabledIf(localJarRadio.selected)
    }

    @Suppress("UnstableApiUsage")
    private fun Panel.externalJarRow(state: State): RowsRange = rowsRange {
        lateinit var externalJarRadio: Cell<JBRadioButton>

        row {
            externalJarRadio = radioButton(KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.externalJar.title"))
                .bindSelected(state::externalJarRadioSelected)
        }
        panel {
            row {
                textField()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .label(KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.externalJar.path.title"))
                    .bindText(state::externalJarUrl)
            }.layout(RowLayout.PARENT_GRID)

            row("") {
                checkBox(KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.externalJar.ignoreInvalidCerts.title"))
                    .horizontalAlign(HorizontalAlign.FILL)
                    .bindSelected(state::externalJarIgnoreInvalidCerts)
            }
        }
            .customize(Gaps(left = JBUI.scale(20)))
            .enabledIf(externalJarRadio.selected)
    }

    @Suppress("UnstableApiUsage")
    private fun Panel.externalJarRowComingSoon(): RowsRange = rowsRange {
        lateinit var externalJarRadio: Cell<JBRadioButton>

        row {
            externalJarRadio = radioButton(KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.externalJar.title"))
                .enabled(false)
        }.customize(VerticalGaps(top = JBUI.scale(20)))
        panel {
            row {
                cell(JBLabel("COMING SOON"))
                    .applyToComponent {
                        fun String.withDots(count: Int) = replace(".", "") + ".".repeat(count)

                        val timer = Timer(1_500) {
                            val dots = text.count { it == '.' }
                            text = text.withDots((dots + 1) % 4)
                        }.apply { isRepeats = true; start() }

                        Timer(60_000) { timer.stop(); text = text.withDots(3) }.start()
                    }
            }
        }
            .customize(Gaps(left = JBUI.scale(20)))
            .enabledIf(externalJarRadio.selected)
    }
}
