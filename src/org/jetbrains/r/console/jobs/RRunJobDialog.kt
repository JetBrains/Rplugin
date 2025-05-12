/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.dsl.builder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.r.RBundle
import org.jetbrains.r.RPluginCoroutineScope
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.interpreter.isLocal
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import kotlin.reflect.KMutableProperty0

class RRunJobDialog(
  private val interpreter: RInterpreter, defaultScript: VirtualFile?,
  private val defaultWorkingDirectory: String,
) :
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
    }
    else if (!interpreter.isLocal() && defaultScript != null) {
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
      indent {
        if (interpreter.isLocal()) {
          row(RBundle.message("jobs.dialog.label.r.script")) {
            textFieldWithBrowseButton()
              .align(AlignX.FILL)
              .applyToComponent { addTextChangedListener(::scriptPathLocal) }
          }
        }
        else {
          row {
            val comboBox = comboBox(listOf(PathType.LOCAL, PathType.REMOTE))
              .bindItem(::scriptPathType.toNullableProperty())
              .gap(RightGap.SMALL)
              .component
            val placeholder = placeholder().align(AlignX.FILL)
            val localField = com.intellij.ui.components.textFieldWithBrowseButton(project = null, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor())
            localField.addTextChangedListener(::scriptPathLocal)
            val remoteField = interpreter.createFileChooserForHost()
            remoteField.addTextChangedListener(::scriptPathRemote)
            val listener = {
              placeholder.component = when (comboBox.item) {
                PathType.LOCAL -> localField
                PathType.REMOTE -> remoteField
                else -> null
              }
            }
            comboBox.addActionListener {
              listener()
              updateOkAction()
            }
            listener()
          }
        }
        row(RBundle.message("jobs.dialog.label.working.directory")) {
          cell(interpreter.createFileChooserForHost(defaultWorkingDirectory, true))
            .align(AlignX.FILL)
            .applyToComponent { addTextChangedListener(::workingDirectory) }
        }.layout(RowLayout.INDEPENDENT)
      }
      row { label(RBundle.message("jobs.dialog.label.environments")) }
      indent {
        row {
          checkBox(RBundle.message("jobs.dialog.checkbox.copy.global.env")).bindSelected(::runWithGlobalEnvironment)
        }
        row(RBundle.message("jobs.dialog.label.upon.completion")) {
          comboBox(listOf(ExportGlobalEnvPolicy.DO_NO_EXPORT, ExportGlobalEnvPolicy.EXPORT_TO_GLOBAL_ENV,
                          ExportGlobalEnvPolicy.EXPORT_TO_VARIABLE))
            .bindItem(::exportGlobalEnvPolicy.toNullableProperty())
        }.layout(RowLayout.INDEPENDENT)
      }
    }

  private fun TextFieldWithBrowseButton.addTextChangedListener(prop: KMutableProperty0<String>) {
    text = prop.get()
    textField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        prop.set(text)
        updateOkAction()
      }
    })
  }

  override fun doOKAction() {
    panel.apply()
    RPluginCoroutineScope.getScope(project).launch(Dispatchers.IO + ModalityState.defaultModalityState().asContextElement()) {
      val (script, scriptPath) = when (scriptPathType) {
        PathType.LOCAL -> LocalFileSystem.getInstance().findFileByPath(scriptPathLocal) to scriptPathLocal
        PathType.REMOTE -> interpreter.findFileByPathAtHost(scriptPathRemote) to scriptPathRemote
      }
      if (script == null) {
        withContext(Dispatchers.EDT) {
          Messages.showErrorDialog(project, RBundle.message("jobs.dialog.file.not.found.message", scriptPath),
                                   RBundle.message("jobs.dialog.file.not.found.title"))
        }
      }
      else {
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
