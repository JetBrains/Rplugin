/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.notifications

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.HyperlinkLabel
import org.jetbrains.r.RBundle
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.run.visualize.actions.RImportBaseDataContextAction
import org.jetbrains.r.run.visualize.actions.RImportCsvDataContextAction
import org.jetbrains.r.run.visualize.actions.RImportDataContextAction
import java.util.function.Function
import javax.swing.JComponent

class RDataImportNotificationProvider : EditorNotificationProvider, DumbAware {
  private val availableActions = listOf(
    RImportBaseDataContextAction(),
    RImportCsvDataContextAction()
  )

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?> {
    return Function { createNotificationPanel(file, it, project) }
  }

  private fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    RConsoleManager.getInstance(project).currentConsoleOrNull?.rInterop?.let { interop ->
      if (interop.hasVariable(file.nameWithoutExtension)) {
        return null
      }
      filterActions(project, file).takeIf { it.isNotEmpty() }?.let { actions ->
        return createPanel(project, file, fileEditor, actions)
      }
    }
    return null
  }

  private fun filterActions(project: Project, file: VirtualFile): List<RImportDataContextAction> {
    return availableActions.filter { it.isSuggestedFor(file) && it.isEnabled(project) }
  }

  private fun createPanel(project: Project, file: VirtualFile, fileEditor: FileEditor, actions: List<RImportDataContextAction>): EditorNotificationPanel {
    return EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info).apply {
      text = createNotificationText(file)
      createActionLabel { label ->
        if (actions.size != 1) {
          val step = ActionListPopupStep(project, file, actions)
          step.createPopup().showUnderneathOf(label)
        } else {
          actions.first().applyTo(project, file)
        }
      }
    }
  }

  private fun EditorNotificationPanel.createActionLabel(onClick: (HyperlinkLabel) -> Unit) {
    val reference = Ref<HyperlinkLabel>()
    val label = createActionLabel(ACTION_LABEL) {
      onClick(reference.get())
    }
    reference.set(label)
  }

  private class ActionListPopupStep(
    private val project: Project,
    private val file: VirtualFile,
    actions: List<RImportDataContextAction>
  ) : BaseListPopupStep<RImportDataContextAction>(null, actions) {

    override fun getTextFor(value: RImportDataContextAction): String {
      return value.templateText ?: ""
    }

    override fun onChosen(selectedValue: RImportDataContextAction, finalChoice: Boolean): PopupStep<*>? {
      return doFinalStep {
        selectedValue.applyTo(project, file)
      }
    }

    fun createPopup(): ListPopup {
      return JBPopupFactory.getInstance().createListPopup(this)
    }
  }

  companion object {
    private const val NAME = "RDataImportNotificationProvider"
    private val ACTION_LABEL = RBundle.message("import.data.action.group.name")

    private fun RInterop.hasVariable(name: String): Boolean {
      return globalEnvLoader.variables.find { it.name == name } != null
    }

    private fun createNotificationText(file: VirtualFile): String {
      val extension = file.extension?.let { "*.$it" } ?: ""
      return RBundle.message("import.data.editor.notification.text", extension)
    }
  }
}
