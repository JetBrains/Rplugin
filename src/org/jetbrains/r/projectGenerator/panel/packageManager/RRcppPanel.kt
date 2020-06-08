/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.packageManager

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import org.jetbrains.r.RBundle
import org.jetbrains.r.projectGenerator.template.RProjectSettings
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JLabel


/**
 * Rcpp — the R package which can use for creating a new package
 */
class RRcppPanel(rProjectSettings: RProjectSettings) : RRcppPackageManagerPanel(rProjectSettings) {

  private val authorTextField = JBTextField(RBundle.message("project.setting.rcpp.author"))
  private val maintainerTextField = JBTextField(RBundle.message("project.setting.rcpp.maintainer"))
  private val emailTextField = JBTextField(RBundle.message("project.setting.rcpp.email"))
  private val licenseTextField = JBTextField(RBundle.message("project.setting.rcpp.license"))
  private val exampleCheckBox = JCheckBox(RBundle.message("project.setting.rcpp.checkbox", packageManagerName), true)
  private val attributesCheckBox = JCheckBox(RBundle.message("project.setting.rcpp.attributes"), true)
  private val moduleCheckBox = JCheckBox(RBundle.message("project.setting.rcpp.module"), false)

  private val panel = panel {
    row(RBundle.message("project.setting.rcpp.author.label")) { authorTextField() }
    row(RBundle.message("project.setting.rcpp.maintainer.label")) { maintainerTextField() }
    row(RBundle.message("project.setting.rcpp.email.label")) { emailTextField() }
    row(RBundle.message("project.setting.rcpp.license.label")) { licenseTextField() }
    row { exampleCheckBox() }
    row { attributesCheckBox().enableIf(exampleCheckBox.selected) }
    row { moduleCheckBox() }
  }

  private val authorLabel: JLabel? = null
  private val maintainerLabel: JLabel? = null
  private val licenseLabel: JLabel? = null
  private val emailLabel: JLabel? = null

  override val panelName: String
    get() = "R package using Rcpp panel"

  override val packageManagerName: String
    get() = "Rcpp"

  init {
    layout = BorderLayout()
    add(panel)
  }

  override fun generateProject(project: Project, baseDir: VirtualFile, module: Module) {
    addRcppSettings("author", authorTextField.text)
    addRcppSettings("maintainer", maintainerTextField.text)
    addRcppSettings("email", emailTextField.text)
    addRcppSettings("license", licenseTextField.text)
    addRcppSettings("example_code", exampleCheckBox.isSelected)
    if (attributesCheckBox.isEnabled) {
      addRcppSettings("attributes", attributesCheckBox.isSelected)
    }
    addRcppSettings("module", moduleCheckBox.isSelected)

    generateRcppProject(project, baseDir)
  }
}