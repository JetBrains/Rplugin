/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.step

import com.intellij.icons.AllIcons
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.ui.HideableDecorator
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.UIUtil
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginUtil
import org.jetbrains.r.execution.ExecuteExpressionUtils.getSynchronously
import org.jetbrains.r.interpreter.RInterpreterUtil
import org.jetbrains.r.interpreter.toLocalPathOrNull
import org.jetbrains.r.projectGenerator.panel.interpreter.RAddNewInterpreterPanel
import org.jetbrains.r.projectGenerator.panel.interpreter.RChooseInterpreterGroupPanel
import org.jetbrains.r.projectGenerator.panel.interpreter.RInterpreterPanel
import org.jetbrains.r.projectGenerator.template.RPackageProjectGenerator
import org.jetbrains.r.projectGenerator.template.RProjectGenerator
import org.jetbrains.r.projectGenerator.template.RProjectSettings
import java.awt.BorderLayout
import java.util.function.Consumer
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class RProjectSettingsStep(private val rProjectSettings: RProjectSettings,
                           projectGenerator: DirectoryProjectGenerator<RProjectSettings>,
                           callback: AbstractNewProjectStep.AbstractCallback<RProjectSettings>,
                           private val moduleStepButtonUpdater: ((Boolean) -> Unit)? = null)
  : ProjectSettingsStepBase<RProjectSettings>(projectGenerator, callback) {

  private lateinit var interpreterPanel: RChooseInterpreterGroupPanel
  private val rProjectGenerator = myProjectGenerator as RProjectGenerator
  private val isModuleBuilderStep = moduleStepButtonUpdater != null

  override fun createPanel(): JPanel {
    myLazyGeneratorPeer = createLazyPeer()
    val mainPanel = JPanel(BorderLayout())

    val label = createErrorLabel()
    val scrollPanel = createAndFillContentPanel()
    initGeneratorListeners()
    val scrollPane = JBScrollPane(scrollPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    scrollPane.border = null
    mainPanel.add(scrollPane, BorderLayout.CENTER)

    val bottomPanel = JPanel(BorderLayout())
    bottomPanel.name = FlatWelcomeFrame.BOTTOM_PANEL

    bottomPanel.add(label, BorderLayout.NORTH)
    if (!isModuleBuilderStep) {
      val button = createActionButton()
      button.addActionListener(createCloseActionListener())
      Disposer.register(this, Disposable { UIUtil.dispose(button) })
      registerValidators()
      bottomPanel.add(button, BorderLayout.EAST)
    }
    mainPanel.add(bottomPanel, BorderLayout.SOUTH)
    return mainPanel
  }

  override fun onPanelSelected() {
    setErrorText("") // Block the "Create" button before checkValid execute
    invokeLater { checkValid() }
    super.onPanelSelected()
  }

  override fun initGeneratorListeners() {
    super.initGeneratorListeners()
    rProjectGenerator.addSettingsStateListener(object : RProjectGenerator.SettingsListener {
      override fun stateChanged() {
        checkValid()
      }
    })

    (rProjectGenerator as? RPackageProjectGenerator)?.setErrorAction(
      Consumer<List<ValidationInfo>> {
        checkForError(it) { return@Consumer }
        setErrorText(null)
      })
  }

  private inline fun checkForError(validationInfos: List<ValidationInfo>, doIfNotEmpty: (() -> Unit)) {
    if (validationInfos.isNotEmpty()) {
      setErrorText(StringUtil.join<ValidationInfo>(validationInfos, { info -> info.message }, "; "))
      doIfNotEmpty()
    }
  }

  override fun checkValid(): Boolean {
    if (!super.checkValid()) {
      return false
    }

    setErrorText("") // To prevent the "create" button from blinking
    if (rProjectSettings.interpreterLocation == null) {
      setErrorText(RBundle.message("project.settings.missing.interpreter"))
      return false
    }
    checkForError(interpreterPanel.validateInterpreter()) { return false }
    if (rProjectGenerator.requiredPackageList && !rProjectSettings.isInstalledPackagesSetUpToDate) {
      if (RPluginUtil.helperPathOrNull == null) {
        checkForError(listOf(ValidationInfo(MISSING_RSCRIPT))) {
          rProjectSettings.installedPackages = emptySet()
          rProjectGenerator.validateGeneratorSettings()
          return false
        }
      }
      rProjectSettings.installedPackages = interpreterPanel.fetchInstalledPackages().toSet()
      rProjectSettings.isInstalledPackagesSetUpToDate = true
    }
    checkForError(rProjectGenerator.validateGeneratorSettings()) { return false }
    setErrorText(null)
    return true
  }

  override fun setErrorText(text: String?) {
    myErrorLabel.text = text
    myErrorLabel.foreground = MessageType.ERROR.titleForeground
    myErrorLabel.icon = if (StringUtil.isEmpty(text)) null else AllIcons.Actions.Lightning

    val isEnabled = text == null
    // one of them is null
    myCreateButton?.isEnabled = isEnabled
    moduleStepButtonUpdater?.invoke(isEnabled)
  }

  public override fun createBasePanel(): JPanel {
    val layout = BorderLayout()

    val panel = JPanel(VerticalFlowLayout(0, 2))
    if (!isModuleBuilderStep) {
      val location = createLocationComponent()
      val locationPanel = JPanel(layout)
      locationPanel.add(location, BorderLayout.CENTER)
      panel.add(locationPanel)
    }
    panel.add(createInterpretersPanel())
    return panel
  }

  override fun createAdvancedSettings(): JPanel? {
    val advancedSettings = rProjectGenerator.getSettingsPanel() ?: return null
    if (advancedSettings.components.isEmpty()) return null

    val jPanel = JPanel(VerticalFlowLayout())
    val decorator = HideableDecorator(jPanel, RBundle.message("project.settings.more.settings"), false)
    val result = interpreterPanel.interpreterLocation?.let { rProjectGenerator.validateGeneratorSettings() }
    decorator.setOn(result == null || result.isNotEmpty())
    decorator.setContentComponent(advancedSettings)
    return jPanel
  }

  private fun createInterpretersPanel(): JPanel {
    val container = JPanel(BorderLayout())
    val decoratorPanel = JPanel(VerticalFlowLayout())

    val existingInterpreters = getSynchronously(RBundle.message("project.settings.interpreters.loading")) {
      RInterpreterUtil.suggestAllInterpreters(enabledOnly = false, localOnly = true)
    }

    val newInterpreterPanel = RAddNewInterpreterPanel(existingInterpreters)

    val decorator = HideableDecorator(decoratorPanel, getProjectInterpreterTitle(newInterpreterPanel), false)

    interpreterPanel = RChooseInterpreterGroupPanel(RBundle.message("project.settings.choose.interpreter"), icon,
                                                    listOf(newInterpreterPanel), newInterpreterPanel)
    interpreterPanel.addChangeListener(Runnable {
      decorator.title = getProjectInterpreterTitle(interpreterPanel.mySelectedPanel)
      val useNewInterpreter = interpreterPanel.mySelectedPanel is RAddNewInterpreterPanel
      rProjectSettings.useNewInterpreter = useNewInterpreter
    })

    val listener = Runnable {
      val location = interpreterPanel.mySelectedPanel.interpreterLocation
      rProjectSettings.interpreterPath = location?.toLocalPathOrNull()
    }
    interpreterPanel.addChangeListener(listener)
    listener.run()

    interpreterPanel.addChangeListener(Runnable {
      rProjectSettings.isInstalledPackagesSetUpToDate = false
    })

    interpreterPanel.addChangeListener(Runnable {
      setErrorText("") // Block the "Create" button before checkValid execute
      invokeLater { checkValid() } // Most likely it should be the last listener
    })

    container.add(interpreterPanel, BorderLayout.NORTH)

    val result = interpreterPanel.validateInterpreter()
    decorator.setOn(result.isNotEmpty())
    decorator.setContentComponent(container)

    return decoratorPanel
  }

  private fun getProjectInterpreterTitle(panel: RInterpreterPanel): String {
    return RBundle.message("project.settings.interpreter", panel.panelName)
  }

  companion object {
    private val MISSING_RSCRIPT = RBundle.message("project.settings.missing.rscript")
  }
}