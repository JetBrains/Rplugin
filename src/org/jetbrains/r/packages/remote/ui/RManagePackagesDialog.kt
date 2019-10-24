// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.packages.remote.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.webcore.packaging.ManagePackagesDialog
import com.intellij.webcore.packaging.PackageManagementService
import org.jetbrains.r.packages.remote.RPackageManagementService

import javax.swing.*
import java.awt.*

class RManagePackagesDialog(
  private val project: Project,
  private val packageManagementService: RPackageManagementService,
  packageListener: PackageManagementService.Listener,
  private val packagesPanel: RInstalledPackagesPanel
) : ManagePackagesDialog(project, packageManagementService, packageListener) {

  init {
    createCenterPanel()?.let { panel ->
      traverseComponents(panel, panel.components)
    }
  }

  private fun traverseComponents(parent: JComponent, components: Array<Component>) {  // Note: this ridiculous mix of both `JComponent` and `Component` was intentional
    for (component in components) {
      when (component) {
        is JButton -> {
          if (component.text.contains(MANAGE_REPO_BUTTON_TEXT) && component.isVisible) {
            replaceManageRepoButtonListeners(component)
          }
        }
        is JCheckBox -> {
          if (component.text in CHECK_BOX_TEXTS) {
            removeAdditionalOptions(parent)
          }
        }
        is JPanel -> traverseComponents(component, component.components)
        is JSplitPane -> traverseComponents(component, arrayOf(component.rightComponent))
      }
    }
  }

  private fun removeAdditionalOptions(parent: JComponent) {
    for (component in parent.components) {
      when (component) {
        is JCheckBox, is JComboBox<*>, is JTextField -> parent.remove(component)
      }
    }
  }

  private fun replaceManageRepoButtonListeners(button: JButton) {
    for (listener in button.actionListeners) {
      button.removeActionListener(listener)
    }
    button.addActionListener {
      RManageRepoDialog(project, packageManagementService) { needRefresh ->
        if (needRefresh) {
          refreshPackages()
        }
      }.show()
    }
  }

  private fun refreshPackages() {
    setDownloadStatus(true)
    ApplicationManager.getApplication().executeOnPooledThread {
      packageManagementService.reloadAllPackages()
      initModel()
      setDownloadStatus(false)
      packagesPanel.scheduleRefresh()
    }
  }

  companion object {
    private const val MANAGE_REPO_BUTTON_TEXT = "Manage Repositories"  // Note: this string is hardcoded in platform. Do not move to bundle!
    private const val VERSION_CHECK_BOX_TEXT = "Specify version"  // Note: this string is hardcoded in platform. Do not move to bundle!
    private const val OPTIONS_CHECK_BOX_TEXT = "Options"  // Note: this string is hardcoded in platform. Do not move to bundle!

    private val CHECK_BOX_TEXTS = listOf(VERSION_CHECK_BOX_TEXT, OPTIONS_CHECK_BOX_TEXT)
  }
}
