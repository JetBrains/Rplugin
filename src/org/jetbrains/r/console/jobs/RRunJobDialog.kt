/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.layout.*
import org.jetbrains.concurrency.runAsync
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class RRunJobDialog(private val project: Project, defaultScript: String, defaultWorkingDirectory: String) :
  DialogWrapper(project, null, true, IdeModalityType.IDE) {
  private val panel: DialogPanel
  var scriptPath: String = defaultScript
  var workingDirectory = defaultWorkingDirectory
  var runWithGlobalEnvironment: Boolean = false
  var exportGlobalEnvPolicy: ExportGlobalEnvPolicy = ExportGlobalEnvPolicy.DO_NO_EXPORT

  init {
    panel = createDialogPanel()
    setTitle("Run Script as Job")
    setOKButtonText("Run")
    init()
  }

  override fun createCenterPanel(): JComponent = panel.also { updateOkAction() }

  private fun createDialogPanel(): DialogPanel =
    panel {
      row { label("Script Paths:") }
      row {
        cell {
          label("R Script:").withLargeLeftGap()
          textFieldWithBrowseButton(::scriptPath).addTextChangedListener()
        }
      }
      row {
        cell {
          label("Working directory:").withLargeLeftGap()
          textFieldWithBrowseButton(
            ::workingDirectory,
            fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
          ).addTextChangedListener()
        }
      }
      row { label("Environments:") }
      row {
        checkBox("Run job with copy of global environment", ::runWithGlobalEnvironment).withLargeLeftGap()
      }
      row {
        cell {
          label("Upon completion of job:").withLargeLeftGap()
          comboBox(DefaultComboBoxModel<ExportGlobalEnvPolicy>(
            arrayOf(ExportGlobalEnvPolicy.DO_NO_EXPORT, ExportGlobalEnvPolicy.EXPORT_TO_GLOBAL_ENV,
                    ExportGlobalEnvPolicy.EXPORT_TO_VARIABLE)),
                   ::exportGlobalEnvPolicy)
        }
      }
    }

  private fun CellBuilder<TextFieldWithBrowseButton>.addTextChangedListener(): CellBuilder<TextFieldWithBrowseButton> =
    this.apply {
      component.textField.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
          updateOkAction()
        }
      })
    }

  override fun doOKAction() {
    panel.apply()
    val task = RJobTask(scriptPath, workingDirectory, runWithGlobalEnvironment, exportGlobalEnvPolicy)
    runAsync {
     RJobRunner.getInstance(project).runRJob(task)
    }
    super.doOKAction()
  }

  private fun updateOkAction() {
    panel.apply()
    isOKActionEnabled = scriptPath.isNotEmpty() && workingDirectory.isNotEmpty()
  }
}