package com.pihanya.ktlint.ui

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.table.TableView
import com.intellij.util.ui.JBDimension
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.ui.FreshKtlintConfigUiProvider.Companion.bindListItems
import com.pihanya.ktlint.ui.FreshKtlintConfigUiProvider.Companion.setColumnWidth
import com.pihanya.ktlint.ui.model.RuleSetData
import com.pihanya.ktlint.ui.model.RuleStatus
import com.pihanya.ktlint.ui.model.RuleStatusTableModel
import javax.swing.JPanel
import javax.swing.JTable

class EditRulesetPanel(private val ruleSetData: RuleSetData) : KtlintConfigUiProvider {

    private fun buildRowsForRuleset(): MutableList<RuleStatus> =
        ruleSetData.ruleIds.asSequence()
            .map { ruleId ->
                RuleStatus(
                    active = (ruleId !in ruleSetData.state.disabledRuleIds),
                    ruleId = ruleId,
                )
            }
            .toMutableList()
            .apply { sortBy(RuleStatus::ruleId) }

    private fun updateDisabledRows(rows: List<RuleStatus>) {
        ruleSetData.state.disabledRuleIds = rows.asSequence()
            .filterNot(RuleStatus::active)
            .map(RuleStatus::ruleId)
            .toSet()
    }

    @Suppress("UnstableApiUsage")
    override fun createPanel(): DialogPanel = panel {
        val activeCol = KtlintBundle.message("ktlint.conf.ruleSets.dialog.edit.rules.table.active")
        val ruleIdCol = KtlintBundle.message("ktlint.conf.ruleSets.dialog.edit.rules.table.rulesetId")

        val tableModel = RuleStatusTableModel()
        val table: TableView<RuleStatus> = TableView(tableModel).apply table@{
            tableHeader.reorderingAllowed = false
            isStriped = true // aka setShowGrid(false)
            rowSelectionAllowed = true

            setColumnWidth(activeCol, 60, 60, 60)
            setColumnWidth(ruleIdCol, 200, 250)
            autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        }

        table.listTableModel.addTableModelListener {
            updateDisabledRows(table.listTableModel.items)
        }

        val tableDecorator = ToolbarDecorator.createDecorator(table)
            .disableAddAction()
            .disableRemoveAction()
            .disableUpDownActions()
            .setPreferredSize(JBDimension(0, 700))

        val tablePanel: JPanel = tableDecorator.createPanel()

        row {
            cell(tablePanel)
                .verticalAlign(VerticalAlign.BOTTOM)
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .bindListItems(tableModel, PropertyBinding(::buildRowsForRuleset, ::updateDisabledRows))
        }.layout(RowLayout.PARENT_GRID)
    }
}
