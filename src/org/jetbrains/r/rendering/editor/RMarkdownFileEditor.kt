/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.editor

import com.intellij.diff.util.FileEditorBase
import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.WebBrowserManager
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import icons.org.jetbrains.r.RBundle
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.actions.editor
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.rendering.RMarkdownProcessor
import org.jetbrains.r.rendering.chunk.RunChunkHandler
import org.jetbrains.r.rendering.settings.RMarkdownSettings
import org.jetbrains.r.rmarkdownconsole.RMarkdownConsoleManager
import java.awt.BorderLayout
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel


class RMarkdownFileEditor(project: Project, private val editor: TextEditor, report: VirtualFile) : FileEditorBase(), TextEditor {
  private val leftToolbar = JPanel(BorderLayout())
  private val rightToolBar = JPanel(BorderLayout())

  private val toolbarComponent = createRMarkdownEditorToolbar(project, report).component
  private val editorComponent = editor.component

  private val mainComponent = JPanel(BorderLayout())

  private val renderSettings = RMarkdownSettings.getInstance(project)
  private val path = report.path

  init {
    mainComponent.add(toolbarComponent, BorderLayout.NORTH)
    mainComponent.add(editorComponent, BorderLayout.CENTER)
  }

  override fun getComponent() = mainComponent

  override fun getName() = "RMarkdown Editor"

  override fun getPreferredFocusedComponent() = editor.preferredFocusedComponent

  override fun dispose() {
    TextEditorProvider.getInstance().disposeEditor(editor)
    super.dispose()
  }

  override fun getStructureViewBuilder(): StructureViewBuilder? = editor.structureViewBuilder
  override fun getEditor(): Editor = editor.editor
  override fun navigateTo(navigatable: Navigatable) = editor.navigateTo(navigatable)
  override fun canNavigateTo(navigatable: Navigatable): Boolean = editor.canNavigateTo(navigatable)
}

private fun createRMarkdownEditorToolbar(project: Project, report: VirtualFile): ActionToolbar =
  ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, createActionGroup(project, report), true)


private fun createActionGroup(project: Project, report: VirtualFile): ActionGroup {
  return BuildManager(project, report).let { manager ->
    DefaultActionGroup(
      createOutputDirectoryAction(project, report),
      createBuildAction(manager),
      createBuildAndShowAction(project, report, manager),
      createRunAllAction(),
      ActionManager.getInstance().getAction("RMarkdownNewChunk")
    )
  }
}

private class BuildManager(private val project: Project, private val report: VirtualFile) {
  var isRunning: Boolean = false
    private set

  fun toggleBuild(e: AnActionEvent, onRenderFinished: (() -> Unit)? = null) {
    runAsync {
      if (!isRunning) {
        isRunning = true
        if (!RInterpreterManager.getInstance(project).hasInterpreter()) {
          RInterpreterManager.getInstance(project).initializeInterpreter()
        }
        val document = e.getData(CommonDataKeys.EDITOR)?.document ?: return@runAsync
        ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveDocument(document) }
        RMarkdownProcessor.render(project, report) {
          if (isRunning) {
            onRenderFinished?.invoke()  // Don't invoke this if rendering was interrupted
          }
          isRunning = false
        }
      }
      else {
        isRunning = false
        RMarkdownConsoleManager.getInstance(project).consoleRunner.interruptRendering()
      }
    }
  }
}

private fun createBuildAction(manager: BuildManager): AnAction {
  val idleText = RBundle.message("rmarkdown.editor.toolbar.renderDocument")
  val runningText = RBundle.message("rmarkdown.editor.toolbar.interruptRenderDocument")
  val idleIcon = AllIcons.Actions.Compile
  val runningIcon = AllIcons.Actions.Suspend
  return object : SameTextAction(idleText, idleIcon) {
    override fun update(e: AnActionEvent) {
      val text = if (manager.isRunning) runningText else idleText
      val icon = if (manager.isRunning) runningIcon else idleIcon
      e.presentation.description = text
      e.presentation.text = text
      e.presentation.icon = icon
    }

    override fun actionPerformed(e: AnActionEvent) {
      manager.toggleBuild(e)
    }
  }
}

private fun createBuildAndShowAction(project: Project, report: VirtualFile, manager: BuildManager): AnAction {
  val idleText = RBundle.message("rmarkdown.editor.toolbar.renderAndOpenDocument")
  val runningText = RBundle.message("rmarkdown.editor.toolbar.interruptRenderDocument")
  val icon = WebBrowserManager.getInstance().firstActiveBrowser?.icon ?: AllIcons.Nodes.PpWeb
  return object : SameTextAction(idleText, icon) {
    override fun update(e: AnActionEvent) {
      val text = if (manager.isRunning) runningText else idleText
      e.presentation.description = text
      e.presentation.text = text
    }

    override fun actionPerformed(e: AnActionEvent) {
      manager.toggleBuild(e) {
        openDocument()
      }
    }

    private fun openDocument() {
      val profileLastOutput = RMarkdownSettings.getInstance(project).state.getProfileLastOutput(report.path)
      val file = File(profileLastOutput)
      if (profileLastOutput.isNotEmpty() && file.exists()) {
        RMarkdownProcessor.openResult(file)
      } else {
        val rMarkdownConsoleManager = ServiceManager.getService(project, RMarkdownConsoleManager::class.java)
        rMarkdownConsoleManager.consoleRunner.reportResultNotFound()
      }
    }
  }
}

class ChunkExecutionState(val terminationRequired: AtomicBoolean = AtomicBoolean(),
                          val isDebug: Boolean = false,
                          val currentPsiElement: AtomicReference<PsiElement> = AtomicReference(),
                          val cancellableExecutionPromise: AtomicReference<CancellablePromise<Unit>> = AtomicReference())


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


private fun createRunAllAction(): AnAction =
  object : SameTextAction(RBundle.message("rmarkdown.editor.toolbar.runAllChunks")) {

    override fun update(e: AnActionEvent) {
      val state = e.editor?.chunkExecutionState
      e.presentation.icon = if (state == null) AllIcons.Actions.RunAll else AllIcons.Actions.Suspend
    }

    override fun actionPerformed(e: AnActionEvent) {
      val editor = e.editor ?: return
      val state = editor.chunkExecutionState
      if (state == null) {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        ChunkExecutionState().apply {
          editor.chunkExecutionState = this
          RunChunkHandler.runAllChunks(psiFile, currentPsiElement, terminationRequired).onProcessed { editor.chunkExecutionState = null }
        }
      }
      else {
        state.terminationRequired.set(true)
        val element = state.currentPsiElement.get() ?: return
        RunChunkHandler.interruptChunkExecution(element.project)
      }
    }
  }


private fun createOutputDirectoryAction(project: Project, report: VirtualFile): ComboBoxAction =
  object : ComboBoxAction() {
    init {
      templatePresentation.icon = AllIcons.General.OpenDisk
      templatePresentation.description = RBundle.message("rmarkdown.editor.toolbar.chooseOutputDirectory")
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = renderDirectoryName
    }

    override fun createPopupActionGroup(button: JComponent?): DefaultActionGroup =
      DefaultActionGroup(
        object : AnAction(RBundle.message("rmarkdown.editor.toolbar.documentDirectory")) {
          override fun actionPerformed(e: AnActionEvent) {
            setOutputPath(report.parent?.path)
          }
        },
        object : AnAction(RBundle.message("rmarkdown.editor.toolbar.projectDirectory")) {
          override fun actionPerformed(e: AnActionEvent) {
            setOutputPath(project.basePath)
          }
        },
        object : AnAction(RBundle.message("rmarkdown.editor.toolbar.customDirectory")) {
          override fun actionPerformed(e: AnActionEvent) {
            val fileDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            val dir = FileChooser.chooseFile(fileDescriptor, project, null)
            if (dir != null && dir.isValid) {
              setOutputPath(dir.path)
            }
          }
        })


    private fun setOutputPath(path: String?) {
      RMarkdownSettings.getInstance(project).state.setProfileRenderDirectory(report.path, path ?: return)
    }

    private val renderDirectoryName
      get() = Paths.get(RMarkdownSettings.getInstance(project).state.getProfileRenderDirectory(report.path)).fileName.toString()
  }

private abstract class SameTextAction(text: String, icon: Icon? = null) : AnAction(text, text, icon)