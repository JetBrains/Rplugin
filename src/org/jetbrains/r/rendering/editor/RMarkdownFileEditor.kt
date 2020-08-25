/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.editor

import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.WebBrowserManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import icons.RIcons
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.actions.ToggleSoftWrapAction
import org.jetbrains.r.actions.editor
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.interpreter.isLocal
import org.jetbrains.r.rendering.chunk.RunChunkHandler
import org.jetbrains.r.rendering.chunk.canRunChunk
import org.jetbrains.r.rendering.settings.RMarkdownSettings
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rmarkdown.RMarkdownRenderingConsoleRunner
import org.jetbrains.r.rmarkdown.RMarkdownUtil
import org.jetbrains.r.settings.REditorSettings
import java.awt.BorderLayout
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon
import javax.swing.JComponent


class RMarkdownFileEditor(project: Project, textEditor: TextEditor, virtualFile: VirtualFile)
  : AdvancedTextEditor(project, textEditor, virtualFile) {
  init {
    val toolbarComponent = createRMarkdownEditorToolbar(project, virtualFile, textEditor.editor).component
    mainComponent.add(toolbarComponent, BorderLayout.NORTH)

    FileDocumentManager.getInstance().getDocument(virtualFile)?.let { document ->
      val manager = PsiDocumentManager.getInstance(project)
      fun updateIsShiny() {
        manager.getPsiFile(document)?.let {
          editor.putUserData(RMarkdownUtil.IS_SHINY, RMarkdownUtil.isShiny(it))
        }
      }
      document.addDocumentListener(object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
          manager.performForCommittedDocument(document) { updateIsShiny() }
        }
      }, this)
      updateIsShiny()
    }
  }

  override fun getName() = RBundle.message("editor.name.rmarkdown")
}

private fun createRMarkdownEditorToolbar(project: Project, report: VirtualFile, editor: Editor): ActionToolbar =
  ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, createActionGroup(project, report, editor), true).also {
    it.setTargetComponent(editor.contentComponent)
  }


private fun createActionGroup(project: Project, report: VirtualFile, editor: Editor): ActionGroup {
  return BuildManager(project, report).let { manager ->
    DefaultActionGroup(
      createOutputDirectoryAction(project, report),
      Separator(),
      createBuildAction(project, manager),
      createRunShinyAction(project, manager),
      createBuildAndShowAction(project, report, manager),
      createRunAllAction(project),
      ActionManager.getInstance().getAction("org.jetbrains.r.actions.RunSelection"),
      ActionManager.getInstance().getAction("RMarkdownNewChunk"),
      Separator(),
      createToggleSoftWrapAction(editor)
    )
  }
}

private fun createToggleSoftWrapAction(editor: Editor): AnAction =
  object : ToggleSoftWrapAction() {
    private var isSelected: Boolean = REditorSettings.useSoftWRapsInRMarkdown

    init {
      updateEditors()
    }

    override fun isSelected(e: AnActionEvent): Boolean {
      return isSelected
    }

    private fun updateEditors() {
      editor.getSettings().setUseSoftWraps(isSelected)
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
      isSelected = state
      updateEditors()
      REditorSettings.useSoftWRapsInRMarkdown = isSelected
    }
  }

private class BuildManager(private val project: Project, private val report: VirtualFile) {
  var isRunning: Boolean = false
    private set
  private var renderingRunner: RMarkdownRenderingConsoleRunner? = null

  fun toggleBuild(e: AnActionEvent, onRenderSuccess: (() -> Unit)? = null) {
    runAsync {
      if (!isRunning) {
        isRunning = true
        val editor = e.editor ?: return@runAsync
        val isShiny = editor.isShiny
        val document = editor.document
        ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveDocument(document) }
        RMarkdownRenderingConsoleRunner(project).apply {
          renderingRunner = this
          render(project, report, isShiny)
            .onSuccess {
              if (!isShiny) {
                onRenderSuccess?.invoke()
              }
            }
            .onProcessed {
              renderingRunner = null
              isRunning = false
            }
        }
      } else {
        isRunning = false
        renderingRunner?.interruptRendering()
      }
    }
  }
}

private fun createBuildOrRunAction(
  project: Project, manager: BuildManager, idleText: String, runningText: String, idleIcon: Icon?, isShiny: Boolean): AnAction {
  val runningIcon = AllIcons.Actions.Suspend
  return object : SameTextAction(idleText, idleIcon) {
    override fun update(e: AnActionEvent) {
      val editor = e.editor ?: return
      if (editor.isShiny != isShiny) {
        e.presentation.isEnabledAndVisible = false
        return
      }
      val text = if (manager.isRunning) runningText else idleText
      val icon = if (manager.isRunning) runningIcon else idleIcon
      e.presentation.isEnabled = RMarkdownUtil.areRequirementsSatisfied(project)
      e.presentation.description = text
      e.presentation.text = text
      e.presentation.icon = icon
    }

    override fun actionPerformed(e: AnActionEvent) {
      manager.toggleBuild(e)
    }
  }
}

private fun createBuildAction(project: Project, manager: BuildManager): AnAction {
  val idleText = RBundle.message("rmarkdown.editor.toolbar.renderDocument")
  val runningText = RBundle.message("rmarkdown.editor.toolbar.interruptRenderDocument")
  val idleIcon = RIcons.Render
  return createBuildOrRunAction(project, manager, idleText, runningText, idleIcon, false)
}

private fun createRunShinyAction(project: Project, manager: BuildManager): AnAction {
  val idleText = RBundle.message("rmarkdown.editor.toolbar.runShinyDocument")
  val runningText = RBundle.message("rmarkdown.editor.toolbar.interruptRunShinyDocument")
  val idleIcon = AllIcons.Actions.Execute
  return createBuildOrRunAction(project, manager, idleText, runningText, idleIcon, true)
}

private fun createBuildAndShowAction(project: Project, report: VirtualFile, manager: BuildManager): AnAction {
  val text = RBundle.message("rmarkdown.editor.toolbar.renderAndOpenDocument")
  val icon = WebBrowserManager.getInstance().firstActiveBrowser?.icon ?: AllIcons.Nodes.PpWeb
  return object : SameTextAction(text, icon) {
    override fun update(e: AnActionEvent) {
      val editor = e.editor ?: return
      e.presentation.isEnabled = RMarkdownUtil.areRequirementsSatisfied(project) && !manager.isRunning && !editor.isShiny
    }

    override fun actionPerformed(e: AnActionEvent) {
      manager.toggleBuild(e) {
        openDocument()
      }
    }

    private fun openDocument() {
      val profileLastOutput = RMarkdownSettings.getInstance(project).state.getProfileLastOutput(report)
      if (profileLastOutput.isBlank()) {
        return
      }
      val interop = RConsoleManager.getInstance(project).currentConsoleOrNull?.rInterop
      if (interop != null && !interop.interpreter.isLocal()) {
        interop.interpreter.showFileInViewer(interop, profileLastOutput)
      } else {
        val file = File(profileLastOutput)
        if (file.exists()) {
          RToolWindowFactory.showFile(project, file.absolutePath)
        }
      }
    }
  }
}

class ChunkExecutionState(private val editor: Editor,
                          val terminationRequired: AtomicBoolean = AtomicBoolean(),
                          val isDebug: Boolean = false,
                          val currentPsiElement: AtomicReference<PsiElement> = AtomicReference(),
                          val interrupt: AtomicReference<() -> Unit> = AtomicReference())

var Editor.chunkExecutionState: ChunkExecutionState?
  get() = project?.chunkExecutionState
  set(value) {
    project?.chunkExecutionState = value
  }

var Project.chunkExecutionState: ChunkExecutionState?
  get() = RConsoleManager.getInstance(this).currentConsoleOrNull?.executeActionHandler?.chunkState
  set(value) {
    RConsoleManager.getInstance(this).currentConsoleOrNull?.executeActionHandler?.chunkState = value
  }


private fun createRunAllAction(project: Project): AnAction =
  object : SameTextAction(RBundle.message("rmarkdown.editor.toolbar.runAllChunks")) {

    override fun update(e: AnActionEvent) {
      val state = e.editor?.chunkExecutionState
      e.presentation.icon = if (state == null) AllIcons.Actions.RunAll else AllIcons.Actions.Suspend
      e.presentation.isEnabled = state != null || canRunChunk(project)
    }

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.editor ?: return
      val state = editor.chunkExecutionState
      if (state == null) {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        ChunkExecutionState(editor).apply {
          editor.chunkExecutionState = this
          RunChunkHandler.runAllChunks(psiFile, editor, currentPsiElement, terminationRequired).onProcessed { editor.chunkExecutionState = null }
        }
      } else {
        state.terminationRequired.set(true)
        val element = state.currentPsiElement.get() ?: return
        RunChunkHandler.interruptChunkExecution(element.project)
      }
    }
  }


private fun createOutputDirectoryAction(project: Project, report: VirtualFile): ComboBoxAction =
  object : ComboBoxAction(), DumbAware {
    init {
      templatePresentation.icon = AllIcons.General.OpenDisk
      templatePresentation.description = RBundle.message("rmarkdown.editor.toolbar.chooseOutputDirectory")
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = outputDirectoryName
    }

    override fun createPopupActionGroup(button: JComponent?): DefaultActionGroup =
      DefaultActionGroup(
        object : AnAction(RBundle.message("rmarkdown.editor.toolbar.documentDirectory")) {
          override fun actionPerformed(e: AnActionEvent) {
            outputDirectory(report.parent)
          }
        },
        object : AnAction(RBundle.message("rmarkdown.editor.toolbar.projectDirectory")) {
          override fun actionPerformed(e: AnActionEvent) {
            outputDirectory(project.basePath)
          }
        },
        object : AnAction(RBundle.message("rmarkdown.editor.toolbar.customDirectory")) {
          override fun actionPerformed(e: AnActionEvent) {
            val fileDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            val dir = FileChooser.chooseFile(fileDescriptor, project, null)
            if (dir != null && dir.isValid) {
              outputDirectory(dir)
            }
          }
        })

    private fun outputDirectory(dir: VirtualFile?) {
      RMarkdownSettings.getInstance(project).state.setOutputDirectory(report, dir ?: return)
    }

    private fun outputDirectory(dir: String?) {
      outputDirectory(LocalFileSystem.getInstance().findFileByPath(dir ?: return))
    }

    private val outputDirectoryName
      get() = RMarkdownSettings.getInstance(project).state.getOutputDirectory(report)?.name.orEmpty()
  }

private abstract class SameTextAction(text: String, icon: Icon? = null) : DumbAwareAction(text, text, icon)

private val Editor.isShiny
  get() = getUserData(RMarkdownUtil.IS_SHINY) == true
