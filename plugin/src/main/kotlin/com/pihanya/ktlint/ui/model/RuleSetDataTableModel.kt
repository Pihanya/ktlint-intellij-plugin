package com.pihanya.ktlint.ui.model

import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.ui.model.RuleSetData.UiRuleSetSource
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class RuleSetDataTableModel(initialItems: List<RuleSetData> = emptyList()) : ListTableModel<RuleSetData>(
    /* columnNames = */
    arrayOf(
        ActiveColumnInfo,
        RuleSetIdColumnInfo,
        SourceColumnInfo,
        RulesTotalColumnInfo,
        RulesActiveColumnInfo,
        RulesDisabledColumnInfo,
        DescriptionColumnInfo,
    ),
    /* items = */ initialItems,
) {

    object ActiveColumnInfo : ColumnInfo<RuleSetData, Boolean>(
        /* name = */ KtlintBundle.message("ktlint.conf.ruleSets.table.active"),
    ) {

        override fun valueOf(item: RuleSetData): Boolean = item.state.active

        override fun isCellEditable(item: RuleSetData): Boolean = true

        override fun setValue(item: RuleSetData, value: Boolean) {
            item.state.active = value
        }

        override fun getRenderer(item: RuleSetData): TableCellRenderer = BooleanTableCellRenderer()

        override fun getEditor(item: RuleSetData): TableCellEditor = BooleanTableCellEditor()
    }

    object RuleSetIdColumnInfo : ColumnInfo<RuleSetData, String>(
        /* name = */ KtlintBundle.message("ktlint.conf.ruleSets.table.rulesetId"),
    ) {

        override fun valueOf(item: RuleSetData): String = item.ruleSetId.value

        override fun getRenderer(item: RuleSetData): TableCellRenderer = DefaultTableCellRenderer()
    }

    object SourceColumnInfo : ColumnInfo<RuleSetData, String>(
        /* name = */ KtlintBundle.message("ktlint.conf.ruleSets.table.source"),
    ) {

        override fun valueOf(item: RuleSetData): String = when (item.state.source) {
            UiRuleSetSource.Bundle ->
                KtlintBundle.message("ktlint.conf.ruleSets.table.source.value.bundled")

            is UiRuleSetSource.LocalJar ->
                KtlintBundle.message("ktlint.conf.ruleSets.table.source.value.localJar")

            is UiRuleSetSource.ExternalJar ->
                KtlintBundle.message("ktlint.conf.ruleSets.table.source.value.externalJar")
        }

        override fun getRenderer(item: RuleSetData): TableCellRenderer = DefaultTableCellRenderer()
    }

    object RulesTotalColumnInfo : ColumnInfo<RuleSetData, Int>(
        /* name = */ KtlintBundle.message("ktlint.conf.ruleSets.table.rules.total"),
    ) {

        override fun valueOf(item: RuleSetData): Int = item.ruleIds.size

        override fun getRenderer(item: RuleSetData): TableCellRenderer = DefaultTableCellRenderer()
    }

    object RulesActiveColumnInfo : ColumnInfo<RuleSetData, Int>(
        /* name = */ KtlintBundle.message("ktlint.conf.ruleSets.table.rules.active"),
    ) {

        override fun valueOf(item: RuleSetData): Int = (item.ruleIds - item.state.disabledRuleIds).size

        override fun getRenderer(item: RuleSetData): TableCellRenderer = DefaultTableCellRenderer()
    }

    object RulesDisabledColumnInfo : ColumnInfo<RuleSetData, Int>(
        /* name = */ KtlintBundle.message("ktlint.conf.ruleSets.table.rules.disabled"),
    ) {

        override fun valueOf(item: RuleSetData): Int = (item.ruleIds intersect item.state.disabledRuleIds).size

        override fun getRenderer(item: RuleSetData): TableCellRenderer = DefaultTableCellRenderer()
    }

    object DescriptionColumnInfo : ColumnInfo<RuleSetData, String>(
        /* name = */ KtlintBundle.message("ktlint.conf.ruleSets.table.rules.description"),
    ) {

        override fun valueOf(item: RuleSetData): String =
            (item.state.descriptionOverride ?: item.metadata?.description).orEmpty()

        override fun getRenderer(item: RuleSetData): TableCellRenderer = DefaultTableCellRenderer()
    }
}
