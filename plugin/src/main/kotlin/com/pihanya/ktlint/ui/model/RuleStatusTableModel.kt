package com.pihanya.ktlint.ui.model

import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.pihanya.ktlint.KtlintBundle
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class RuleStatusTableModel(initialItems: List<RuleStatus> = emptyList()) : ListTableModel<RuleStatus>(
    /* columnNames = */ arrayOf(ActiveColumnInfo, RuleIdColumnInfo),
    /*       items = */ initialItems,
) {

    object ActiveColumnInfo : ColumnInfo<RuleStatus, Boolean>(
        /* name = */ KtlintBundle.message("ktlint.conf.ruleSets.dialog.edit.rules.table.active"),
    ) {

        override fun valueOf(item: RuleStatus): Boolean = item.active

        override fun isCellEditable(item: RuleStatus): Boolean = true

        override fun setValue(item: RuleStatus, value: Boolean) {
            item.active = value
        }

        override fun getRenderer(item: RuleStatus): TableCellRenderer = BooleanTableCellRenderer()

        override fun getEditor(item: RuleStatus): TableCellEditor = BooleanTableCellEditor()
    }

    object RuleIdColumnInfo : ColumnInfo<RuleStatus, String>(
        /* name = */ KtlintBundle.message("ktlint.conf.ruleSets.dialog.edit.rules.table.rulesetId"),
    ) {

        override fun valueOf(item: RuleStatus): String = item.ruleId.value

        override fun getRenderer(item: RuleStatus): TableCellRenderer = DefaultTableCellRenderer()
    }
}
