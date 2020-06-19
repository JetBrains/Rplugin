/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.layout.*
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.RInterpreter
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class RRunJobDialog(private val interpreter: RInterpreter, private val defaultScript: VirtualFile?,
                    private val defaultWorkingDirectory: String) :
  DialogWrapper(interpreter.project, null, true, IdeModalityType.IDE) {
  private val project = interpreter.project
  private val panel: DialogPanel
  private lateinit var scriptField: TextFieldWithBrowseButton
  private lateinit var workingDirField: TextFieldWithBrowseButton

  val scriptPath get() = scriptField.text
  val workingDirectory get() = workingDirField.text
  var runWithGlobalEnvironment: Boolean = false
  var exportGlobalEnvPolicy: ExportGlobalEnvPolicy = ExportGlobalEnvPolicy.DO_NO_EXPORT

  init {
    panel = createDialogPanel()
    setTitle(RBundle.message("jobs.dialog.title"))
    setOKButtonText(RBundle.message("jobs.dialog.ok.button.text"))
    init()
  }

  override fun createCenterPanel(): JComponent = panel.also { updateOkAction() }

  private fun createDialogPanel(): DialogPanel =
    panel {
      row { label(RBundle.message("jobs.dialog.label.script.path")) }
      row {
        cell {
          label(RBundle.message("jobs.dialog.label.r.script")).withLargeLeftGap()
          component(interpreter.createFileChooserForHost(defaultScript?.let { interpreter.getFilePathAtHost(it) }.orEmpty() )
                      .also { scriptField = it }).addTextChangedListener()
        }
      }
      row {
        cell {
          label(RBundle.message("jobs.dialog.label.working.directory")).withLargeLeftGap()
          component(interpreter.createFileChooserForHost(defaultWorkingDirectory, true)
                      .also { workingDirField = it }).addTextChangedListener()
        }
      }
      row { label(RBundle.message("jobs.dialog.label.environments")) }
      row {
        checkBox(RBundle.message("jobs.dialog.checkbox.copy.global.env"), ::runWithGlobalEnvironment).withLargeLeftGap()
      }
      row {
        cell {
          label(RBundle.message("jobs.dialog.label.upon.completion")).withLargeLeftGap()
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
    runAsync {
      val script = interpreter.findFileByPathAtHost(scriptPath)
      if (script == null) {
        invokeLater {
          Messages.showErrorDialog(project, RBundle.message("jobs.dialog.file.not.found.message", scriptPath),
                                   RBundle.message("jobs.dialog.file.not.found.title"))
        }
      } else {
        val task = RJobTask(script, workingDirectory, runWithGlobalEnvironment, exportGlobalEnvPolicy)
        RJobRunner.getInstance(project).runRJob(task)
      }
    }
    super.doOKAction()
  }

  private fun updateOkAction() {
    panel.apply()
    isOKActionEnabled = scriptPath.isNotEmpty() && workingDirectory.isNotEmpty()
  }
}