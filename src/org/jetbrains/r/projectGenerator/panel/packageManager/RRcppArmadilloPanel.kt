/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.projectGenerator.panel.packageManager

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jdesktop.swingx.VerticalLayout
import org.jetbrains.r.RBundle
import org.jetbrains.r.projectGenerator.template.RProjectSettings
import javax.swing.JCheckBox

/**
 * RcppArmadillo — the R package which can use for creating a new package
 */
class RRcppArmadilloPanel(rProjectSettings: RProjectSettings) : RRcppPackageManagerPanel(rProjectSettings) {

  private val exampleCheckBox = JCheckBox(RBundle.message("project.setting.rcpp.checkbox", packageManagerName), true)

  override val panelName: String
    get() = "R package using RcppArmadillo panel"

  override val packageManagerName: String
    get() = "RcppArmadillo"

  init {
    layout = VerticalLayout()
    add(exampleCheckBox)
  }

  override fun generateProject(project: Project, baseDir: VirtualFile, module: Module) {
    addRcppSettings("example_code", exampleCheckBox.isSelected)
    generateRcppProject(project, baseDir)
  }
}