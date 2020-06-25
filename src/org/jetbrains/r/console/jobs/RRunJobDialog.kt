/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.layout.*
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.isLocal
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import kotlin.reflect.KMutableProperty0

class RRunJobDialog(private val interpreter: RInterpreter, private val defaultScript: VirtualFile?,
                    private val defaultWorkingDirectory: String) :
  DialogWrapper(interpreter.project, null, true, IdeModalityType.IDE) {
  private val project = interpreter.project
  private val panel: DialogPanel

  enum class PathType {
    LOCAL {
      override fun toString() = "Local R Script"
    },
    REMOTE {
      override fun toString() = "Remote R Script"
    }
  }
  private var scriptPathType = PathType.LOCAL
  private var scriptPathLocal: String = ""
  private var scriptPathRemote: String = ""

  private var workingDirectory: String = defaultWorkingDirectory
  private var runWithGlobalEnvironment: Boolean = false
  private var exportGlobalEnvPolicy: ExportGlobalEnvPolicy = ExportGlobalEnvPolicy.DO_NO_EXPORT

  init {
    if (defaultScript?.isInLocalFileSystem == true) {
      scriptPathLocal = defaultScript.path
    } else if (!interpreter.isLocal() && defaultScript != null) {
      interpreter.getFilePathAtHost(defaultScript)?.let {
        scriptPathType = PathType.REMOTE
        scriptPathRemote = it
      }
    }

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
          if (interpreter.isLocal()) {
            label(RBundle.message("jobs.dialog.label.r.script")).withLargeLeftGap()
            textFieldWithBrowseButton().addTextChangedListener(::scriptPathLocal)
          } else {
            val comboBox = comboBox(DefaultComboBoxModel(arrayOf(PathType.LOCAL, PathType.REMOTE)), ::scriptPathType).component
            val localField = textFieldWithBrowseButton().addTextChangedListener(::scriptPathLocal).component
            val remoteField = component(interpreter.createFileChooserForHost()).addTextChangedListener(::scriptPathRemote)
              .withLeftGap(0).component
            val listener = {
              localField.isVisible = comboBox.item == PathType.LOCAL
              remoteField.isVisible = comboBox.item == PathType.REMOTE
            }
            comboBox.addActionListener {
              listener()
              updateOkAction()
            }
            listener()
          }
        }
      }
      row {
        cell {
          label(RBundle.message("jobs.dialog.label.working.directory")).withLargeLeftGap()
          component(interpreter.createFileChooserForHost(defaultWorkingDirectory, true)).addTextChangedListener(::workingDirectory)
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

  private fun CellBuilder<TextFieldWithBrowseButton>.addTextChangedListener(prop: KMutableProperty0<String>? = null):
    CellBuilder<TextFieldWithBrowseButton> {
    if (prop != null) {
      component.text = prop.get()
    }
    component.textField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        prop?.set(component.text)
        updateOkAction()
      }
    })
    return this
  }

  override fun doOKAction() {
    panel.apply()
    runAsync {
      val (script, scriptPath) = when (scriptPathType) {
        PathType.LOCAL -> LocalFileSystem.getInstance().findFileByPath(scriptPathLocal) to scriptPathLocal
        PathType.REMOTE -> interpreter.findFileByPathAtHost(scriptPathRemote) to scriptPathRemote
      }
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
    isOKActionEnabled = workingDirectory.isNotBlank() && when (scriptPathType) {
      PathType.LOCAL -> scriptPathLocal.isNotBlank()
      PathType.REMOTE -> scriptPathRemote.isNotBlank()
    }
  }
}