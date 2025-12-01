/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.packageManager

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.execution.ExecuteExpressionUtils
import com.intellij.r.psi.interpreter.RInterpreterUtil
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import org.jetbrains.r.projectGenerator.template.RProjectSettings
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import java.util.function.Consumer
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class RPackageManagerGroupPanel(name: String,
                                panelIcon: Icon?,
                                private val rProjectSettings: RProjectSettings,
                                private val panels: List<RPackageManagerPanel>,
                                defaultPanel: RPackageManagerPanel) : RPackageManagerPanel(rProjectSettings) {
  override val panelName: String = name
  override val icon: Icon? = panelIcon
  private val installButton = JButton().apply { isVisible = false; isEnabled = false }
  private var selectedPanel: RPackageManagerPanel = defaultPanel
  private var errorAction: Consumer<List<ValidationInfo>>? = null
  override val initProjectScriptName: String = ""

  init {
    layout = BorderLayout()
    val contentPanel = createComboBoxPanel(panels)
    if (contentPanel.components.isNotEmpty()) {
      add(contentPanel, BorderLayout.NORTH)
    }

    for (panel in panels) {
      panel.setIsPackageInstalledAction { isPackageInstalled ->
        installButton.isVisible = !isPackageInstalled
        installButton.isEnabled = !isPackageInstalled
        installButton.text = RBundle.message("project.settings.install.button.suggest", selectedPanel.rPackageName)
      }
    }

    installButton.addActionListener {
      installButton.isEnabled = false
      installButton.text = RBundle.message("project.settings.install.button.progress")
      ExecuteExpressionUtils
        .executeScriptInBackground(rProjectSettings.interpreterLocation!!, SCRIPT_PATH, listOf(selectedPanel.rPackageName),
                                   RBundle.message("project.settings.install.progress.title", selectedPanel.rPackageName),
                                   timeout = RInterpreterUtil.DEFAULT_TIMEOUT)
      rProjectSettings.isInstalledPackagesSetUpToDate = false
      selectedPanel.runListeners()
      if (installButton.isVisible) {
        installButton.isEnabled = false
        installButton.text = RBundle.message("project.settings.install.button.failed")
        errorAction?.accept(listOf(ValidationInfo(RBundle.message("project.settings.install.failed.warning"))))
      }
    }
  }

  override val packageManagerName: String
    get() = selectedPanel.packageManagerName

  override fun addChangeListener(listener: Runnable) {
    changeListeners += listener
    for (panel in panels) {
      panel.addChangeListener(listener)
    }
  }

  override fun generateProject(project: Project, baseDir: VirtualFile, module: Module) {
    selectedPanel.generateProject(project, baseDir, module)
  }

  override fun validateSettings(): List<ValidationInfo> {
    val panel = selectedPanel
    val validationInfo = panel.validateSettings()
    if (validationInfo.isNotEmpty()) return validationInfo

    if (panel is RPackratPanel) {
      try {
        panel.updatePackratSettings(rProjectSettings.interpreterLocation!!)
      }
      catch (e: IllegalStateException) {
        e.message?.let { return listOf(ValidationInfo(it)) }
      }
    }
    return emptyList()
  }

  fun setErrorAction(action: Consumer<List<ValidationInfo>>) {
    errorAction = action
    panels.forEach { panel ->
      (panel as? RPackratPanel)?.setErrorAction(action)
    }
  }

  private fun createComboBoxPanel(panels: List<RPackageManagerPanel>): JPanel {
    val comboBox = ComboBox(panels.toTypedArray()).apply {
      renderer = SimpleListCellRenderer.create { label, value, _ ->
        label.text = value?.packageManagerName ?: return@create
        label.icon = value.icon
      }

      selectedItem = selectedPanel
      addItemListener {
        if (it.stateChange == ItemEvent.SELECTED) {
          val selected = it.item
          for (panel in panels) {
            val isSelected = panel == selected
            panel.isVisible = isSelected
            if (isSelected) {
              selectedPanel = panel
            }
          }

          runListeners()
        }
      }
    }

    val name = JPanel(BorderLayout()).apply {
      val inner = JPanel().apply {
        add(JLabel(RBundle.message("project.settings.combo.box.panel.title")))
        add(comboBox)
        add(installButton)
      }
      add(inner, BorderLayout.WEST)
    }
    name.border = JBUI.Borders.emptyLeft(10)

    layout = BorderLayout()
    val formBuilder = FormBuilder.createFormBuilder().apply {
      addComponent(name)
      for (panel in panels) {
        addComponent(panel)
        panel.border = JBUI.Borders.emptyLeft(10)
        panel.isVisible = panel == selectedPanel
      }
    }

    return formBuilder.panel
  }

  companion object {
    private const val SCRIPT_PATH = "projectGenerator/quickInstallPackage.R"
  }
}