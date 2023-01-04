package com.pihanya.ktlint.ui

import com.intellij.CommonBundle
import com.intellij.ide.BrowserUtil.browse
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.AnActionButton
import com.intellij.ui.AnActionButtonRunnable
import com.intellij.ui.AnActionButtonUpdater
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.selected
import com.intellij.ui.table.TableView
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.TableViewModel
import com.pihanya.ktlint.BuildConfig
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.config.KtlintPluginSettings
import com.pihanya.ktlint.config.KtlintPluginSettingsV1
import com.pihanya.ktlint.config.KtlintPluginSettingsV1.AnnotationMode
import com.pihanya.ktlint.config.RuleSetSettings
import com.pihanya.ktlint.config.RuleSetSettings.*
import com.pihanya.ktlint.integration.RuleProviderWrapper
import com.pihanya.ktlint.ui.model.FileListListModel
import com.pihanya.ktlint.ui.model.RuleSetData
import com.pihanya.ktlint.ui.model.RuleSetData.UiRuleSetSource
import com.pihanya.ktlint.ui.model.RuleSetDataTableModel
import com.pihanya.ktlint.ui.util.createEditorconfigChooserDescriptor
import com.pihanya.ktlint.util.*
import java.awt.Component
import java.awt.Desktop
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.*
import javax.swing.AbstractAction
import javax.swing.Box
import javax.swing.JPanel
import javax.swing.JTable
import kotlin.io.path.Path
import kotlin.reflect.KMutableProperty0

internal class FreshKtlintConfigUiProvider(
    private val settings: KtlintPluginSettings,
    private val project: Project,
) : KtlintConfigUiProvider {

    @Suppress("UnstableApiUsage")
    override fun createPanel(): DialogPanel = panel {
        val stateHolder = object {
            lateinit var ktlintEnabled: ComponentPredicate
        }

        generalSettingsRow(stateHolder::ktlintEnabled)

        group(
            title = KtlintBundle.message("ktlint.conf.groups.rules"),
            indent = false,
        ) {
            ruleSetSettingsTableRow(stateHolder.ktlintEnabled)
        }

        group(
            title = KtlintBundle.message("ktlint.conf.groups.configs"),
            indent = false,
        ) {
            configurationFilesRow(stateHolder.ktlintEnabled)
            baselineFileRow(stateHolder.ktlintEnabled)
        }

        linkButtonsRow()
            .visible(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
    }

    @Suppress("UnstableApiUsage")
    private fun Panel.generalSettingsRow(
        ktlintEnabledProp: KMutableProperty0<ComponentPredicate>
    ): Row = threeColumnsRowInternal(
        first = {
            var ktlintEnabled by ktlintEnabledProp
            row(KtlintBundle.message("ktlint.conf.groups.linting")) {
                // Nothing to do
            }
            row {
                checkBox(KtlintBundle.message("ktlint.conf.enableKtlint.title"))
                    .bindSelected(settings::enableKtlint)
                    .component
                    .apply {
                        toolTipText = KtlintBundle.message("ktlint.conf.enableKtlint.tooltip")
                    }
                    .also { ktlintEnabled = it.selected }
            }
            row {
                checkBox(KtlintBundle.message("ktlint.conf.lintAfterReformat.title"))
                    .bindSelected(settings::lintAfterReformat)
                    .enabledIf(ktlintEnabled)
                    .component
                    .apply {
                        toolTipText = KtlintBundle.message("ktlint.conf.lintAfterReformat.tooltip")
                    }
            }
        },
        second = {
            val ktlintEnabled by ktlintEnabledProp
            enabledIf(ktlintEnabled)

            buttonGroup(
                prop = settings::annotationMode,
                title = KtlintBundle.message("ktlint.conf.annotationMode.title"),
                indent = false,
            ) {
                row {
                    radioButton(
                        text = KtlintBundle.message("ktlint.conf.annotationMode.value.error"),
                        value = AnnotationMode.ERROR,
                    )
                }
                row {
                    radioButton(
                        text = KtlintBundle.message("ktlint.conf.annotationMode.value.warning"),
                        value = AnnotationMode.WARNING,
                    )
                }
                row {
                    radioButton(
                        text = KtlintBundle.message("ktlint.conf.annotationMode.value.none"),
                        value = AnnotationMode.NONE,
                    )
                }
            }
        },
        third = {
            val ktlintEnabled by ktlintEnabledProp
            enabledIf(ktlintEnabled)

            buttonGroup(
                prop = settings::styleGuide,
                title = KtlintBundle.message("ktlint.conf.styleGuide.title"),
                indent = false,
            ) {
                row {
                    radioButton(
                        text = KtlintBundle.message("ktlint.conf.styleGuide.value.official"),
                        value = KtlintPluginSettingsV1.StyleGuide.OFFICIAL,
                    )
                }
                row {
                    radioButton(
                        text = KtlintBundle.message("ktlint.conf.styleGuide.value.android"),
                        value = KtlintPluginSettingsV1.StyleGuide.ANDROID,
                    )
                }
            }
        },
    )

    @Suppress("UnstableApiUsage")
    private fun Panel.ruleSetSettingsTableRow(ktlintEnabled: ComponentPredicate): Row = row {
        val activeCol = KtlintBundle.message("ktlint.conf.ruleSets.table.active")
        val rulesetIdCol = KtlintBundle.message("ktlint.conf.ruleSets.table.rulesetId")
        val sourceCol = KtlintBundle.message("ktlint.conf.ruleSets.table.source")
        val rulesTotalCol = KtlintBundle.message("ktlint.conf.ruleSets.table.rules.total")
        val rulesActiveCol = KtlintBundle.message("ktlint.conf.ruleSets.table.rules.active")
        val rulesDisabledCol = KtlintBundle.message("ktlint.conf.ruleSets.table.rules.disabled")
        val descriptionCol = KtlintBundle.message("ktlint.conf.ruleSets.table.rules.description")

        val tableModel = RuleSetDataTableModel()
        val table: TableView<RuleSetData> = TableView(tableModel).apply table@{
            tableHeader.reorderingAllowed = false
            isStriped = true // aka setShowGrid(false)
            rowSelectionAllowed = true

            setColumnWidth(activeCol, 40, 40, 40)
            setColumnWidth(rulesetIdCol, 100, 130, 200)
            setColumnWidth(sourceCol, 100, 100, 150)
            setColumnWidth(rulesTotalCol, 60, 60, 90)
            setColumnWidth(rulesActiveCol, 60, 60, 90)
            setColumnWidth(rulesDisabledCol, 65, 65, 90)
            setColumnWidth(descriptionCol, 200, 200)
            autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        }

        val selectedRulesetResolver: () -> RuleSetData = { checkNotNull(table.selectedObject) }
        val onDialogClose: () -> Unit = { tableModel.fireTableDataChanged() }

        // Handle doubleclick on table row
        table.addMouseListener(
            object : MouseAdapter() {
                override fun mousePressed(mouseEvent: MouseEvent) {
                    if ((mouseEvent.clickCount == 2) && (table.selectedObject != null)) {
                        EditRuleSetSettingsAction(project, selectedRulesetResolver, onDialogClose)
                            .actionPerformed(null)
                    }
                }
            },
        )

        val tableDecorator = ToolbarDecorator.createDecorator(table)
            .setAddAction(
                AddRuleSetAction(settings, project, { tableModel.items = loadRuleSetData(); onDialogClose() }),
            )
            .setEditAction(EditRuleSetSettingsAction(project, selectedRulesetResolver, onDialogClose))
            .setRemoveAction(RemoveRuleSetAction(settings, table))
            .setAddActionUpdater(EnableWhenParentEnabled(table))
            .setEditActionUpdater(EnableWhenSelected(table))
            .setRemoveActionUpdater(EnableWhenSelectedAndRemovable(table))
            .disableUpDownActions()
            .setButtonComparator(
                CommonBundle.message("button.add"),
                CommonBundle.message("button.edit"),
                CommonBundle.message("button.remove"),
            )

        val tablePanel: JPanel = tableDecorator.createPanel()
        cell(tablePanel)
            .horizontalAlign(HorizontalAlign.FILL)
            .resizableColumn()
            .enabledIf(ktlintEnabled)
            .bindListItems(tableModel, PropertyBinding(::loadRuleSetData, ::setRuleSetInfos))
    }.layout(RowLayout.LABEL_ALIGNED)

    private fun setRuleSetInfos(value: List<RuleSetData>) {
        settings.ruleSetSettingsById = value.associateBy(
            keySelector = { it.ruleSetId },
            valueTransform = { ruleSetData ->
                RuleSetSettings(
                    active = ruleSetData.state.active,
                    ruleSetId = ruleSetData.ruleSetId,
                    descriptionOverride = ruleSetData.state.descriptionOverride,
                    disabledRules = ruleSetData.state.disabledRuleIds,
                    sourceSettings = when (val source = ruleSetData.state.source) {
                        UiRuleSetSource.Bundle -> RuleSetSourceSettings.Bundle
                        is UiRuleSetSource.LocalJar -> RuleSetSourceSettings.LocalJar(
                            path = source.path,
                            relativeToProject = source.useRelativePath,
                        )
                        is UiRuleSetSource.ExternalJar -> TODO()
                    },
                )
            },
        )
    }

    private fun loadRuleSetData(): List<RuleSetData> =
        settings.ruleSetSettingsById.values.asSequence()
            .map(RuleSetSettings::sourceSettings)
            .filterIsInstance<RuleSetSourceSettings.LocalJar>()
            .map { ruleSetSettings ->
                if (ruleSetSettings.relativeToProject) {
                    project.resolveBasePath().resolve(ruleSetSettings.path)
                } else {
                    Path(ruleSetSettings.path)
                }
            }
            .toList()
            .let(ruleLoaderService::load)
            .groupBy(RuleProviderWrapper::ruleSetId)
            .map { (ruleSetId, providers) ->
                assert(providers.all { it.owner == providers.first().owner })
                val infoProvider = providers.first()
                val ruleSetSettings = settings.ruleSetSettingsById.getValue(ruleSetId)

                val rulesetMetadata = infoProvider.rulesetMetadata
                val source = when (ruleSetSettings.sourceSettings) {
                    RuleSetSourceSettings.Bundle -> UiRuleSetSource.Bundle
                    is RuleSetSourceSettings.LocalJar -> UiRuleSetSource.LocalJar(
                        path = ruleSetSettings.sourceSettings.path,
                        useRelativePath = ruleSetSettings.sourceSettings.relativeToProject,
                    )

                    is RuleSetSourceSettings.ExternalJar -> TODO()
                }

                RuleSetData(
                    ruleSetId = ruleSetId,
                    ruleIds = providers.asSequence().map(RuleProviderWrapper::ruleId).toSet(),
                    metadata = rulesetMetadata,
                    state = RuleSetData.State(
                        active = ruleSetSettings.active,
                        disabledRuleIds = ruleSetSettings.disabledRules,
                        descriptionOverride = ruleSetSettings.descriptionOverride,
                        source = source,
                    ),
                )
            }

    @Suppress("UnstableApiUsage")
    private fun Panel.configurationFilesRow(ktlintEnabled: ComponentPredicate) {
        row {
            val label = label(KtlintBundle.message("ktlint.conf.editorconfigPaths.title"))
                .verticalAlign(VerticalAlign.TOP)
                .component
                .apply { toolTipText = KtlintBundle.message("ktlint.conf.editorconfigPaths.tooltip") }

            val dialogTitle = KtlintBundle.message("ktlint.conf.editorconfigPaths.dialog.title")
            val dialogDesc = KtlintBundle.message("ktlint.conf.editorconfigPaths.dialog.description")

            textFieldWithBrowseButton(
                browseDialogTitle = dialogTitle,
                project = project,
                fileChooserDescriptor = createEditorconfigChooserDescriptor()
                    .apply { description = dialogDesc },
            )
                .bindText(
                    getter = { settings.editorConfigPaths.firstOrNull()?.toConfigString().orEmpty() },
                    setter = { value -> value.toNioPath()?.let { settings.editorConfigPaths = setOf(it) } },
                )
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .enabledIf(ktlintEnabled)
                .validationOnInput { textField -> validateAsFilePath(textField.text, causeError = false) }
                .validationOnApply { textField -> validateAsFilePath(textField.text) }
                .applyToComponent { label.labelFor = this }
        }.layout(RowLayout.PARENT_GRID)

        commentHtmlTrick(KtlintBundle.message("ktlint.conf.editorconfigPaths.comment"))
    }

    @Suppress("UnstableApiUsage")
    private fun Panel.baselineFileRow(ktlintEnabled: ComponentPredicate) {
        row {
            val label = label(KtlintBundle.message("ktlint.conf.baseLinePath.title"))
                .verticalAlign(VerticalAlign.TOP)
                .component
                .apply { toolTipText = KtlintBundle.message("ktlint.conf.baseLinePath.tooltip") }

            val dialogTitle = KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.localJar.path.dialog.title")
            val dialogDesc = KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.localJar.path.dialog.description")

            textFieldWithBrowseButton(
                browseDialogTitle = dialogTitle,
                project = project,
                fileChooserDescriptor = createEditorconfigChooserDescriptor()
                    .apply { description = dialogDesc },
            )
                .bindText(
                    getter = settings.baselinePath::toConfigString,
                    setter = { settings.baselinePath = it.toNioPath() },
                )
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .enabledIf(ktlintEnabled)
                .validationOnInput { textField -> validateAsFilePath(textField.text, causeError = false) }
                .validationOnApply { textField -> validateAsFilePath(textField.text) }
                .applyToComponent { label.labelFor = this }
        }.layout(RowLayout.PARENT_GRID)

        commentHtmlTrick(KtlintBundle.message("ktlint.conf.baseLinePath.comment"))
    }

    @Suppress("UnstableApiUsage")
    private fun Panel.linkButtonsRow(): Row = row {
        cell(Box.createHorizontalBox()) // Tricky one. Allows shifting buttons to the right border
            .gap(RightGap.SMALL)
            .resizableColumn()
            .horizontalAlign(HorizontalAlign.FILL)

        button(KtlintBundle.message("ktlint.conf.reportingButton.text"), OpenReportingAction())
            .applyToComponent { minimumSize = JBDimension(175, 30) }
        button(KtlintBundle.message("ktlint.conf.donateButton.text"), OpenDonationAction())
            .applyToComponent { minimumSize = JBDimension(100, 30) }
    }

    companion object {

        fun ValidationInfoBuilder.validateAsFilePath(text: String, causeError: Boolean = true): ValidationInfo? =
            if (text.isNotEmpty() && File(text).isFile.not()) {
                if (causeError) {
                    error(KtlintBundle.message("ktlint.conf.validation.filePath"))
                } else {
                    warning(KtlintBundle.message("ktlint.conf.validation.filePath"))
                }
            } else {
                null
            }

        @Suppress("UnstableApiUsage")
        private fun Panel.threeColumnsRowInternal(
            first: Panel.() -> Unit,
            second: Panel.() -> Unit,
            third: Panel.() -> Unit,
        ): Row = row {
            for (block in sequenceOf(first, second, third)) {
                panel {
                    verticalAlign(VerticalAlign.TOP)
                    block()
                }.gap(RightGap.COLUMNS)
            }
        }.topGap(TopGap.NONE)

        @Suppress("UnstableApiUsage")
        fun <T> Cell<JPanel>.bindListItems(
            tableModel: TableViewModel<T>,
            fileSetProperty: PropertyBinding<List<T>>,
        ) = bind(
            componentGet = { tableModel.items },
            componentSet = { _, items ->
                tableModel.items = when (items) {
                    is MutableList -> items.toList()
                    else -> items
                }
            },
            binding = fileSetProperty,
        )

        @Suppress("UnstableApiUsage")
        private fun Cell<JPanel>.bindFileSet(
            listModel: FileListListModel,
            filesProperty: PropertyBinding<Set<Path>>,
        ) = bind(
            componentGet = { listModel.items.toPathsSet() },
            componentSet = { _, filePaths -> listModel += filePaths.toVirtualFilesList() },
            binding = filesProperty,
        )

        fun TableView<*>.setColumnWidth(
            columnId: Any,
            minSize: Int,
            preferredSize: Int,
            maxSize: Int? = null,
        ) {
            getColumn(columnId).apply {
                minWidth = JBUI.scale(minSize)
                width = JBUI.scale(preferredSize)
                preferredWidth = JBUI.scale(preferredSize)
                if (maxSize != null) {
                    maxWidth = JBUI.scale(maxSize)
                }
            }
        }

        /**
         * It's a trick that allows creating HTML description for some component.
         * ```kt
         * row {
         *   cell(JBox())
         *     .enabledIf(...)
         * }
         * commentHtmlTrick("Description for cell()")
         * ```
         *
         * The reason why this trick is needed:
         * ```kt
         * row {
         *   cell(JBox())
         *     .commentHtml("Description for cell()") // will be disabled together with parent cell
         *     .enabled(false)
         * }
         * ```
         * The code above will disable both cell content and comment.
         * It will cause clickable links in HTML-comment to become disabled.
         */
        @Suppress("UnstableApiUsage")
        fun Panel.commentHtmlTrick(@NlsContexts.DetailedDescription text: String) {
            row("") {
                commentHtml(text)
            }.bottomGap(BottomGap.MEDIUM)
        }
    }

    /**
     * Process the addition of a configuration location.
     */
    private class AddRuleSetAction(
        private val settings: KtlintPluginSettings,
        private val project: Project,
        private val onDialogClose: () -> Unit = {},
    ) : SettingsAction() {

        init {
            putValue(NAME, KtlintBundle.message("ktlint.actions.conf.addRuleSet.name"))
            putValue(SHORT_DESCRIPTION, KtlintBundle.message("ktlint.actions.conf.addRuleSet.description"))
            putValue(LONG_DESCRIPTION, KtlintBundle.message("ktlint.actions.conf.addRuleSet.description"))
        }

        override fun actionPerformed(e: ActionEvent?) {
            val provider = AddRulesetPanel(project, settings)
            val ruleSetStatusPanel = provider.createPanel()

            val dialog = DialogBuilder(project).apply dialog@{
                title(KtlintBundle.message("ktlint.conf.ruleSets.dialog.add.title"))
                with(window) window@{
                    minimumSize = JBDimension(500, 500)
                    size = JBDimension(500, 500)
                }
                dialogWrapper.isModal = true
                setNorthPanel(ruleSetStatusPanel)
            }

            try {
                val isOk = dialog.showAndGet()
                if (isOk) {
                    ruleSetStatusPanel.apply()
                } else {
                    ruleSetStatusPanel.reset()
                }
            } finally {
                onDialogClose()
            }
        }
    }

    private class EditRuleSetSettingsAction(
        private val project: Project,
        private val currentRulesetResolver: () -> RuleSetData,
        private val onDialogClose: () -> Unit,
    ) : SettingsAction() {

        init {
            putValue(NAME, KtlintBundle.message("ktlint.actions.conf.editRuleSet.name"))
            putValue(SHORT_DESCRIPTION, KtlintBundle.message("ktlint.actions.conf.editRuleSet.description"))
            putValue(LONG_DESCRIPTION, KtlintBundle.message("ktlint.actions.conf.editRuleSet.description"))
        }

        override fun actionPerformed(e: ActionEvent?) {
            val currentRuleset = currentRulesetResolver()
            val provider = EditRulesetPanel(currentRuleset)
            val ruleSetStatusPanel = provider.createPanel()

            val dialog = DialogBuilder(project).apply dialog@{
                title(KtlintBundle.message("ktlint.conf.ruleSets.dialog.edit.title", currentRuleset.ruleSetId.value))
                // TODO: setPreferredFocusComponent()
                with(window) window@{
                    minimumSize = JBDimension(500, 500)
                    size = JBDimension(500, 500)
                }
                dialogWrapper.isModal = true
                setNorthPanel(ruleSetStatusPanel)
            }

            try {
                val isOk = dialog.showAndGet()
                if (!isOk) {
                    ruleSetStatusPanel.reset()
                }
            } finally {
                onDialogClose()
            }
        }
    }

    private class RemoveRuleSetAction(
        private val settings: KtlintPluginSettings,
        private val table: TableView<RuleSetData>
    ) : SettingsAction() {

        init {
            putValue(NAME, KtlintBundle.message("ktlint.actions.conf.removeRuleSet.name"))
            putValue(SHORT_DESCRIPTION, KtlintBundle.message("ktlint.actions.conf.removeRuleSet.description"))
            putValue(LONG_DESCRIPTION, KtlintBundle.message("ktlint.actions.conf.removeRuleSet.description"))
        }

        override fun actionPerformed(e: ActionEvent?) {
            val ruleSetData = table.selectedObject ?: return
            val selectedRowIndex = table.selectedRow.takeIf { it != -1 } ?: return

            table.listTableModel.removeRow(selectedRowIndex)
        }
    }

    private class OpenReportingAction : DumbAwareAction() {

        override fun actionPerformed(e: AnActionEvent) {
            browse(URI(BuildConfig.REPOSITORY_URL))
        }
    }

    private class OpenDonationAction : DumbAwareAction() {

        override fun actionPerformed(e: AnActionEvent) {
            browse(URI(BuildConfig.DONATION_URL))
        }
    }

    internal abstract class SettingsAction : AbstractAction(), AnActionButtonRunnable {

        override fun run(anActionButton: AnActionButton) {
            actionPerformed(null)
        }
    }

    private class EnableWhenParentEnabled(private val parent: Component) : AnActionButtonUpdater {

        override fun isEnabled(e: AnActionEvent): Boolean = parent.isEnabled
    }

    private class EnableWhenSelectedAndRemovable(private val table: TableView<RuleSetData>) : AnActionButtonUpdater {

        override fun isEnabled(e: AnActionEvent): Boolean {
            val rulesetData = table.selectedObject ?: return false
            return table.isEnabled && isRemovable(rulesetData)
        }

        private fun isRemovable(rulesetData: RuleSetData): Boolean {
            if (rulesetData.state.source == UiRuleSetSource.Bundle) return false
            return true
        }
    }

    private class EnableWhenSelected(private val table: TableView<RuleSetData>) : AnActionButtonUpdater {

        override fun isEnabled(e: AnActionEvent): Boolean = table.isEnabled && (table.selectedObject != null)
    }
}
