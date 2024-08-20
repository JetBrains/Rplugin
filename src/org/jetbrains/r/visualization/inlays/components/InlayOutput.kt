/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.visualization.inlays.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.notebooks.ui.visualization.notebookAppearance
import org.jetbrains.r.visualization.ui.ToolbarUtil
import java.io.File
import javax.swing.JComponent

abstract class InlayOutput(
  parent: Disposable,
  val editor: Editor,
  val actions: List<AnAction>,
) {
  // Transferring `this` from the constructor to another class violates JMM and leads to undefined behaviour
  // when accessing `toolbarPane` inside constructor and when `toolbarPane` accesses `this`. So, be careful.
  // Since this is an abstract class with many inheritors, the only way to get rid of this issue is to convert
  // the class to the interface (or make the constructor private) and initialize `toolbarPane` inside some
  // factory method.
  @Suppress("LeakingThis")
  protected val toolbarPane = ToolbarPane(this)

  protected val project: Project = editor.project ?: error("Editor should have a project")

  /** If the output should occupy as much editor width as possible. */
  open val isFullWidth = true

  fun getComponent() = toolbarPane

  /** Clears view, removes text/html. */
  abstract fun clear()

  abstract fun addData(data: String, type: String)
  abstract fun scrollToTop()
  abstract fun getCollapsedDescription(): String

  abstract fun acceptType(type: String): Boolean

  fun updateProgressStatus(editor: Editor, progressStatus: InlayProgressStatus) {
    toolbarPane.progressComponent = InlayOutputProgressStatus.buildProgressStatusComponent(progressStatus, editor)
  }

  private fun getProgressStatusHeight(): Int {
    return toolbarPane.progressComponent?.height ?: 0
  }

  /**
   * HTML output returns the height delayed from it's Platform.runLater.
   */
  var onHeightCalculated: ((height: Int) -> Unit)? = null
    set(value) {
      field = { height: Int ->
        value?.invoke(height + getProgressStatusHeight())
      }
    }

  private val disposable: Disposable = Disposer.newDisposable()

  init {
    Disposer.register(parent, disposable)
  }

  open fun onViewportChange(isInViewport: Boolean) {
    // Do nothing by default
  }

  open fun addToolbar() {
    toolbarPane.toolbarComponent = createToolbar()
  }

  private fun createToolbar(): JComponent {
    val toolbar = ToolbarUtil.createEllipsisToolbar("NotebooksInlayOutput", actions)

    toolbar.targetComponent = toolbarPane // ToolbarPane will be in context.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT)
    toolbar.component.isOpaque = true
    toolbar.component.background = editor.notebookAppearance.getTextOutputBackground(editor.colorsScheme)

    return toolbar.component
  }

  protected fun saveWithFileChooser(@Nls title: String,
                                    @Nls description: String,
                                    extension: Array<String>,
                                    defaultName: String,
                                    onChoose: (File) -> Unit) {
    InlayOutputUtil.saveWithFileChooser(project, title, description, extension, defaultName, true, onChoose)
  }

  open fun toolbarPaneChanged(component: JComponent?) {}

  /** marker interface for [org.jetbrains.r.visualization.inlays.components.SaveOutputAction] */
  interface WithSaveAs {
    fun saveAs()
  }

  /** marker interface for [org.jetbrains.r.visualization.inlays.components.CopyImageToClipboardAction] */
  interface WithCopyImageToClipboard {
    fun copyImageToClipboard()
  }

  companion object {
    fun getToolbarPaneOrNull(e: AnActionEvent): ToolbarPane? =
      e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? ToolbarPane

    inline fun <reified T> getInlayOutput(e: AnActionEvent): T? =
      getToolbarPaneOrNull(e)?.inlayOutput as? T

    fun loadActions(vararg ids: String): List<AnAction> {
      val actionManager = ActionManager.getInstance()
      return ids.mapNotNull { actionManager.getAction(it) }
    }
  }
}


