// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.run.configuration

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.layout.*
import org.jetbrains.r.RFileType
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class RRunConfigurationForm(project: Project) : RRunConfigurationParams {
  private val rootPanel: JPanel
  private val scriptPathField = TextFieldWithBrowseButton()
  private val workingDirectoryPathField = TextFieldWithBrowseButton()

  val panel: JComponent
    get() = rootPanel

  init {
    rootPanel = panel {
      row(JLabel("Script:")) {
        scriptPathField()
      }
      row(JLabel("Working directory path:")) {
        workingDirectoryPathField()
      }
    }

    setupScriptPathField(project)
    setupWorkingDirectoryPathField(project)
  }

  override fun getScriptPath(): String = getPath(scriptPathField)

  override fun setScriptPath(scriptPath: String) = setPath(scriptPathField, scriptPath)

  override fun getWorkingDirectoryPath(): String = getPath(workingDirectoryPathField)

  override fun setWorkingDirectoryPath(workingDirectoryPath: String) = setPath(workingDirectoryPathField, workingDirectoryPath)

  private fun setupScriptPathField(project: Project) {
    val listener = object : ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
      "Select Script",
      "",
      scriptPathField,
      project,
      FileChooserDescriptorFactory.createSingleFileDescriptor(RFileType),
      TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
    ) {
      override fun onFileChosen(chosenFile: VirtualFile) {
        super.onFileChosen(chosenFile)
        RRunConfigurationUtils.setSuggestedWorkingDirectoryPathIfNotSpecified(this@RRunConfigurationForm)
      }
    }

    scriptPathField.addActionListener(listener)
  }

  private fun setupWorkingDirectoryPathField(project: Project) {
    workingDirectoryPathField.addBrowseFolderListener(
      "Select Working Directory",
      "",
      project,
      FileChooserDescriptorFactory.createSingleFolderDescriptor()
    )
  }

  private fun getPath(field: TextFieldWithBrowseButton): String {
    return FileUtil.toSystemIndependentName(field.text.trim { it <= ' ' })
  }

  private fun setPath(field: TextFieldWithBrowseButton, path: String) {
    field.text = FileUtil.toSystemDependentName(path)
  }
}
