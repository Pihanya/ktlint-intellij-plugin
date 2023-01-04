//package com.pihanya.ktlint.ui
//
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.Messages
//import com.intellij.openapi.util.SystemInfo
//import com.intellij.ui.components.JBScrollPane
//import com.intellij.ui.table.JBTable
//import com.intellij.util.ui.JBUI
//import com.pihanya.ktlint.KtlintBundle
//import java.awt.*
//import java.awt.event.ActionEvent
//import java.awt.event.KeyEvent
//import java.io.IOException
//import javax.swing.*
//import javax.swing.table.TableCellRenderer
//
//class PropertiesDialogue(
//    parent: Dialog,
//    project: Project,
//    checkstyleProjectService: CheckstyleProjectService
//) : JDialog(parent) {
//
//    private val propertiesPanel: PropertiesPanel
//
//    /**
//     * Was okay clicked?
//     *
//     * @return true if okay clicked, false if cancelled.
//     */
//    var isCommitted = true
//        private set
//
//    init {
//        propertiesPanel = PropertiesPanel(project, checkstyleProjectService)
//        initialise()
//    }
//
//    fun initialise() {
//        layout = BorderLayout()
//        minimumSize = Dimension(300, 200)
//        isModal = true
//
//        val okayButton: JButton = JButton(OkayAction())
//        val cancelButton: JButton = JButton(CancelAction())
//
//        val bottomPanel = JPanel(GridBagLayout())
//
//        bottomPanel.border = JBUI.Borders.empty(4, 8, 8, 8)
//        bottomPanel.add(
//            Box.createHorizontalGlue(),
//            GridBagConstraints(
//                0, 0, 1, 1, 1.0, 0.0,
//                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(4), 0, 0,
//            ),
//        )
//        if (SystemInfo.isMac) {
//            bottomPanel.add(
//                cancelButton,
//                GridBagConstraints(
//                    1, 0, 1, 1, 0.0, 0.0,
//                    GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(4), 0, 0,
//                ),
//            )
//            bottomPanel.add(
//                okayButton,
//                GridBagConstraints(
//                    2, 0, 1, 1, 0.0, 0.0,
//                    GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(4), 0, 0,
//                ),
//            )
//        } else {
//            bottomPanel.add(
//                okayButton,
//                GridBagConstraints(
//                    1, 0, 1, 1, 0.0, 0.0,
//                    GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(4), 0, 0,
//                ),
//            )
//            bottomPanel.add(
//                cancelButton,
//                GridBagConstraints(
//                    2, 0, 1, 1, 0.0, 0.0,
//                    GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(4), 0, 0,
//                ),
//            )
//        }
//        add(propertiesPanel, BorderLayout.CENTER)
//        add(bottomPanel, BorderLayout.SOUTH)
//        getRootPane().defaultButton = okayButton
//        pack()
//        addEscapeListener()
//        val toolkit = Toolkit.getDefaultToolkit()
//        setLocation(
//            (toolkit.screenSize.width - size.width) / 2,
//            (toolkit.screenSize.height - size.height) / 2,
//        )
//    }
//
//    private fun addEscapeListener() {
//        getRootPane().registerKeyboardAction(
//            { event: ActionEvent? -> isVisible = false },
//            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
//            JComponent.WHEN_IN_FOCUSED_WINDOW,
//        )
//    }
//
//    override fun setVisible(visible: Boolean) {
//        if (visible) {
//            isCommitted = false
//        }
//        super.setVisible(visible)
//    }
//
//    var configurationLocation: ConfigurationLocation
//        /**
//         * Get the configuration location entered in the dialogue, or null if no valid location was entered.
//         *
//         * @return the location or null if no valid location entered.
//         */
//        get() = propertiesPanel.getConfigurationLocation()
//
//        /**
//         * Set the configuration location.
//         *
//         * @param configurationLocation the location.
//         */
//        set(configurationLocation) {
//            propertiesPanel.setConfigurationLocation(configurationLocation)
//        }
//
//    class PropertiesPanel(
//        private val project: Project,
//        checkstyleProjectService: CheckstyleProjectService
//    ) : JPanel(BorderLayout()) {
//
//        private val propertiesModel: PropertiesTableModel = PropertiesTableModel()
//
//        /**
//         * Properties table, hacked for enable/disable support.
//         */
//        private val propertiesTable: JBTable = object : JBTable(propertiesModel) {
//            override fun prepareRenderer(
//                renderer: TableCellRenderer,
//                row: Int,
//                column: Int
//            ): Component {
//                val comp = super.prepareRenderer(renderer, row, column)
//                comp.isEnabled = isEnabled
//                return comp
//            }
//        }
//        private val checkstyleProjectService: CheckstyleProjectService
//        private var configurationLocation: ConfigurationLocation? = null
//
//        init {
//            this.checkstyleProjectService = checkstyleProjectService
//            initialise()
//        }
//
//        private fun initialise() {
//            border = JBUI.Borders.empty(8, 8, 4, 8)
//            preferredSize = Dimension(500, 400)
//            propertiesTable.toolTipText = CheckStyleBundle.message("config.file.properties.tooltip")
//            propertiesTable.isStriped = true
//            propertiesTable.tableHeader.reorderingAllowed = false
//            val propertiesScrollPane: JScrollPane = JBScrollPane(
//                propertiesTable,
//                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
//                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS,
//            )
//            add(propertiesScrollPane, BorderLayout.CENTER)
//        }
//
//        /**
//         * Get the configuration location entered in the dialogue, or null if no valid location was entered.
//         *
//         * @return the location or null if no valid location entered.
//         */
//        fun getConfigurationLocation(): ConfigurationLocation? {
//            commitCellEdits()
//            configurationLocation.setProperties(propertiesModel.getProperties())
//            return configurationLocation
//        }
//
//        private fun commitCellEdits() {
//            val cellEditor = propertiesTable.cellEditor
//            cellEditor?.stopCellEditing()
//        }
//
//        /**
//         * Set the configuration location.
//         *
//         * @param configurationLocation the location.
//         */
//        fun setConfigurationLocation(configurationLocation: ConfigurationLocation) {
//            this.configurationLocation = configurationLocation.clone() as ConfigurationLocation
//
//            // get latest properties from file
//            try {
//                configurationLocation.resolve(checkstyleProjectService.underlyingClassLoader())
//                    .use { configInputStream ->
//                        propertiesModel.setProperties(
//                            configurationLocation.getProperties(),
//                        )
//                    }
//            } catch (e: IOException) {
//                LOG.warn("Couldn't resolve properties file", e)
//                Messages.showErrorDialog(
//                    project,
//                    CheckStyleBundle.message("config.file.resolve-failed", e.message),
//                    CheckStyleBundle.message("config.file.error.title"),
//                )
//            }
//        }
//
//        companion object {
//
//            private val LOG = Logger.getInstance(
//                PropertiesPanel::class.java,
//            )
//        }
//    }
//
//    /**
//     * Respond to an okay action.
//     */
//    private inner class OkayAction internal constructor() : AbstractAction() {
//
//        init {
//            putValue(NAME, KtlintBundle.message("config.file.okay.text",),)
//            putValue(SHORT_DESCRIPTION, KtlintBundle.message("config.file.okay.tooltip"),)
//            putValue(LONG_DESCRIPTION, KtlintBundle.message("config.file.okay.tooltip"),)
//        }
//
//        override fun actionPerformed(event: ActionEvent) {
//            isCommitted = true
//            isVisible = false
//        }
//    }
//
//    /**
//     * Respond to a cancel action.
//     */
//    private inner class CancelAction internal constructor() : AbstractAction() {
//
//        init {
//            putValue(NAME, KtlintBundle.message("config.file.cancel.text"))
//            putValue(SHORT_DESCRIPTION, KtlintBundle.message("config.file.cancel.tooltip"))
//            putValue(LONG_DESCRIPTION, KtlintBundle.message("config.file.cancel.tooltip"))
//        }
//
//        override fun actionPerformed(e: ActionEvent) {
//            isCommitted = false
//            isVisible = false
//        }
//    }
//}
