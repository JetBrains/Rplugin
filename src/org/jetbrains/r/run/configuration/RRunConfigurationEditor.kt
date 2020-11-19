package org.jetbrains.r.run.configuration

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.r.RBundle
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class RRunConfigurationEditor: SettingsEditor<RRunConfiguration>() {
  private lateinit var filePath: TextFieldWithBrowseButton
  private lateinit var workingDirectory: TextFieldWithBrowseButton
  private lateinit var arguments: JTextField
  private lateinit var environmentVariables: EnvironmentVariablesComponent

  override fun resetEditorFrom(s: RRunConfiguration) {
    filePath.text = s.filePath
    workingDirectory.text = s.workingDirectory
    arguments.text = s.scriptArguments
    environmentVariables.envs = s.environmentVariablesData.envs
    environmentVariables.isPassParentEnvs = s.environmentVariablesData.isPassParentEnvs
  }

  override fun applyEditorTo(s: RRunConfiguration) {
    s.filePath = filePath.text
    s.workingDirectory = workingDirectory.text
    s.scriptArguments = arguments.text

    s.environmentVariablesData = EnvironmentVariablesData.create(environmentVariables.envs, environmentVariables.isPassParentEnvs)
  }

  override fun createEditor(): JComponent {
    val panel = JPanel(GridBagLayout())

    val g = GridBag()
      .setDefaultFill(GridBagConstraints.BOTH)
      .setDefaultAnchor(GridBagConstraints.CENTER)
      .setDefaultWeightX(1, 1.0)
      .setDefaultInsets(0, JBUI.insets(0, 0, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP))
      .setDefaultInsets(1, JBUI.insetsBottom(UIUtil.DEFAULT_VGAP))

    panel.add(JBLabel(RBundle.message("r.run.configuration.editor.file.label")), g.nextLine().next())
    filePath = TextFieldWithBrowseButton()
    filePath.addBrowseFolderListener(RBundle.message("r.run.configuration.editor.choose.file.title"), null, null,
                                     FileChooserDescriptorFactory.createSingleLocalFileDescriptor())
    panel.add(filePath, g.next().coverLine())

    panel.add(JBLabel(RBundle.message("r.run.configuration.editor.working.directory.label")), g.nextLine().next())
    workingDirectory = TextFieldWithBrowseButton()
    workingDirectory.addBrowseFolderListener(RBundle.message("r.run.configuration.editor.choose.working.directory.title"), null, null,
                                             FileChooserDescriptorFactory.createSingleFolderDescriptor())
    panel.add(workingDirectory, g.next().coverLine())

    panel.add(JBLabel(RBundle.message("r.run.configuration.editor.script.args.label")), g.nextLine().next())
    arguments = JTextField()
    panel.add(arguments, g.next().coverLine())

    panel.add(JBLabel(RBundle.message("r.run.configuration.editor.env.variables.label")), g.nextLine().next())
    environmentVariables = EnvironmentVariablesComponent()
    environmentVariables.text = ""

    panel.add(environmentVariables, g.next().coverLine())

    return panel
  }
}