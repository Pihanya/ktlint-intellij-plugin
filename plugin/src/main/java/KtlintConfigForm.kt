@file:Suppress("DEPRECATION")
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.util.ui.JBDimension
import com.pihanya.ktlint.BuildConfig
import com.pihanya.ktlint.KtlintBundle
import com.pihanya.ktlint.config.KtlintPluginSettingsV0
import com.pihanya.ktlint.util.ruleLoaderService
import com.pihanya.ktlint.util.toNioPath
import com.pihanya.ktlint.util.uncheckedCast
import java.awt.Desktop
import java.awt.Dimension
import java.net.URI
import java.util.Objects
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.ChangeEvent

class KtlintConfigForm(
    private val project: Project,
    private val config: KtlintPluginSettingsV0,
) {

    private lateinit var mainPanel: JPanel

    private lateinit var enableKtlint: JCheckBox

    private lateinit var androidMode: JCheckBox

    private lateinit var enableExperimental: JCheckBox

    private lateinit var annotateAs: JComboBox<AnnotationMode>

    private lateinit var lintAfterReformat: JCheckBox

    private lateinit var disabledRulesContainer: JPanel

    private lateinit var externalJarPaths: TextFieldWithBrowseButton

    private lateinit var baselinePath: TextFieldWithBrowseButton

    private lateinit var editorConfigPath: TextFieldWithBrowseButton

    private lateinit var githubButton: JButton

    private lateinit var disabledRules: TextFieldWithAutoCompletion<String>

    fun createUIComponents() {
        // Nothing to do
    }

    fun createComponent(): JComponent {
        // Manually create and insert disabled rules field
        disabledRules = TextFieldWithAutoCompletion
            .create(project, resolveRuleIds(), false, "")
            .apply { toolTipText = KtlintBundle.message("ktlint.conf.disabledRules.tooltip") }

        disabledRulesContainer.add(
            disabledRules,
            @Suppress("MagicNumber")
            GridConstraints(
                0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW or GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                JBDimension(-1, -1), JBDimension(150, -1), JBDimension(-1, -1),
            ),
        )

        mainPanel.border = IdeBorderFactory.createTitledBorder("Ktlint Settings")

        enableKtlint.addChangeListener(::onPluginEnableStateChange)

        AnnotationMode.values().forEach(annotateAs::addItem)

        externalJarPaths.addActionListener {
            val descriptor = FileChooserDescriptor(false, false, true, true, false, true)
            FileChooser.chooseFiles(descriptor, project, null) { files ->
                externalJarPaths.text = files.joinToString(", ") { it.path }
            }
        }

        baselinePath.addBrowseFolderListener(
            null,
            null,
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("xml"),
        )

        editorConfigPath.addBrowseFolderListener(
            null,
            null,
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        )

        // If we're able to launch the browser, show the github button!
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            githubButton.addActionListener {
                Desktop.getDesktop().browse(URI(BuildConfig.REPOSITORY_URL))
            }
        } else {
            githubButton.isVisible = false
        }

        return mainPanel
    }

    /**
     * Disable fields when plugin disabled
     */
    @Suppress("UNUSED_PARAMETER")
    private fun onPluginEnableStateChange(event: ChangeEvent) {
        val pluginEnabled = getEnableKtlint()
        val fieldsToDisable = listOf(
            enableExperimental,
            androidMode,
            annotateAs,
            lintAfterReformat,
            disabledRules,
            baselinePath,
            externalJarPaths,
            editorConfigPath,
        )
        fieldsToDisable.forEach { field -> field.isEnabled = pluginEnabled }
    }

    fun apply() {
        config.enableKtlint = getEnableKtlint()
        config.androidMode = getAndroidMode()
        config.useExperimental = getEnableExperimental()
        config.treatAsErrors = (AnnotationMode.ERROR == getAnnotateAs())
        config.hideErrors = (AnnotationMode.NONE == getAnnotateAs())
        config.lintAfterReformat = getLintAfterReformat()
        config.disabledRules = getDisabledRules()
        config.baselinePath = baselinePath.text.takeUnless(String::isNullOrBlank)?.trim()
        config.externalJarPaths = getExternalJarPaths()
        config.editorConfigPath = getEditorConfigPath()
    }

    fun reset() {
        setEnableKtlint(config.enableKtlint)
        setAndroidMode(config.androidMode)
        setEnableExperimental(config.useExperimental)
        setAnnotateAs(AnnotationMode.fromConfig(config))
        setLintAfterReformat(config.lintAfterReformat)
        setDisabledRules(config.disabledRules)
        setBaselinePath(config.baselinePath)
        setExternalJarPaths(config.externalJarPaths)
        setEditorConfigPath(config.editorConfigPath)
    }

    val isModified: Boolean
        get() = !(
            Objects.equals(config.enableKtlint, getEnableKtlint()) &&
                Objects.equals(config.androidMode, getAndroidMode()) &&
                Objects.equals(config.useExperimental, getEnableExperimental()) &&
                Objects.equals(AnnotationMode.fromConfig(config), getAnnotateAs()) &&
                Objects.equals(config.lintAfterReformat, getLintAfterReformat()) &&
                Objects.equals(config.disabledRules, getDisabledRules()) &&
                Objects.equals(config.baselinePath, getBaselinePath()) &&
                Objects.equals(config.externalJarPaths, getExternalJarPaths()) &&
                Objects.equals(config.editorConfigPath, getEditorConfigPath())
            )

    private fun getEnableKtlint(): Boolean = enableKtlint.isSelected

    private fun setEnableKtlint(value: Boolean) {
        enableKtlint.isSelected = value
    }

    private fun getAndroidMode(): Boolean = androidMode.isSelected

    private fun setAndroidMode(value: Boolean) {
        androidMode.isSelected = value
    }

    private fun getEnableExperimental(): Boolean = enableExperimental.isSelected

    private fun setEnableExperimental(value: Boolean) {
        enableExperimental.isSelected = value
    }

    private fun getAnnotateAs(): AnnotationMode? = annotateAs.selectedItem?.let(::uncheckedCast)

    private fun setAnnotateAs(value: AnnotationMode) {
        annotateAs.selectedItem = value
    }

    private fun getLintAfterReformat(): Boolean = lintAfterReformat.isSelected

    private fun setLintAfterReformat(value: Boolean) {
        lintAfterReformat.isSelected = value
    }

    private fun getDisabledRules(): List<String> = disabledRules.text.takeUnless(String::isNullOrBlank)
        ?.run { split(",").filter(String::isNotBlank).map(String::trim) }
        .orEmpty()

    private fun setDisabledRules(value: List<String>) {
        disabledRules.text = value.joinToString(",").ifEmpty { "" }
    }

    private fun getExternalJarPaths(): List<String> = externalJarPaths.text.takeUnless(String::isNullOrBlank)
        ?.run { split(",").filter(String::isNotBlank).map(String::trim) }
        .orEmpty()

    private fun setExternalJarPaths(value: List<String>) {
        externalJarPaths.text = value.joinToString(",").ifEmpty { "" }
    }

    private fun getEditorConfigPath(): String? = editorConfigPath.text.takeUnless(String::isNullOrBlank)

    private fun setEditorConfigPath(value: String?) {
        editorConfigPath.text = value.orEmpty()
    }

    private fun getBaselinePath(): String? = baselinePath.text.takeUnless(String::isNullOrBlank)

    private fun setBaselinePath(value: String?) {
        baselinePath.text = value.orEmpty()
    }

    private fun resolveRuleIds(): List<String> {
        val externalJarPaths = config.externalJarPaths.mapNotNull(String::toNioPath)
        return runCatching { ruleLoaderService.load(externalJarPaths) }
            .recoverCatching { ruleLoaderService.load(externalJarPaths, skipErrors = true) }
            .map { providers ->
                providers
                    .filter { config.useExperimental || it.ruleSetId.isExperimental().not() }
                    .map { it.ruleId.asString() }
            }
            .getOrThrow() // TODO: We should show notification to user in case of rules loading issues
    }

    enum class AnnotationMode(private val bundleKey: String) {

        ERROR("ktlint.conf.annotationMode.value.none"),

        WARNING("ktlint.conf.annotationMode.value.warning"),

        NONE("ktlint.conf.annotationMode.value.error");

        override fun toString(): String = KtlintBundle.message(bundleKey)

        companion object {

            @JvmStatic
            fun fromConfig(config: KtlintPluginSettingsV0) = when {
                config.hideErrors -> NONE
                config.treatAsErrors -> ERROR
                else -> WARNING
            }
        }
    }
}
