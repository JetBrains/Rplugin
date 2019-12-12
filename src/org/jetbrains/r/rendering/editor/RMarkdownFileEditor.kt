/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rendering.editor

import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.ide.browsers.WebBrowserManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import icons.org.jetbrains.r.RBundle
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RENDER
import org.jetbrains.r.actions.ToggleSoftWrapAction
import org.jetbrains.r.actions.editor
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.rendering.chunk.RunChunkHandler
import org.jetbrains.r.rendering.settings.RMarkdownSettings
import org.jetbrains.r.rmarkdown.RMarkdownRenderingConsoleRunner
import org.jetbrains.r.settings.REditorSettings
import java.awt.BorderLayout
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon
import javax.swing.JComponent


class RMarkdownFileEditor(project: Project, textEditor: TextEditor, virtualFile: VirtualFile)
  : AdvancedTextEditor(project, textEditor, virtualFile) {
  init {
    val toolbarComponent = createRMarkdownEditorToolbar(project, virtualFile, textEditor.editor).component
    mainComponent.add(toolbarComponent, BorderLayout.NORTH)
  }

  override fun getName() = "RMarkdown Editor"
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
      createBuildAction(manager),
      createBuildAndShowAction(project, report, manager),
      createRunAllAction(),
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

  fun toggleBuild(e: AnActionEvent, onRenderFinished: (() -> Unit)? = null) {
    runAsync {
      if (!isRunning) {
        isRunning = true
        if (!RInterpreterManager.getInstance(project).hasInterpreter()) {
          RInterpreterManager.getInstance(project).initializeInterpreter()
        }
        val document = e.getData(CommonDataKeys.EDITOR)?.document ?: return@runAsync
        ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveDocument(document) }
        RMarkdownRenderingConsoleRunner(project).apply {
          renderingRunner = this
          render(project, report) {
            if (isRunning) {
              onRenderFinished?.invoke()  // Don't invoke this if rendering was interrupted
            }
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

private fun createBuildAction(manager: BuildManager): AnAction {
  val idleText = RBundle.message("rmarkdown.editor.toolbar.renderDocument")
  val runningText = RBundle.message("rmarkdown.editor.toolbar.interruptRenderDocument")
  val idleIcon = RENDER
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
        BrowserLauncher.instance.browse(file)
      }
    }
  }
}

class ChunkExecutionState(private val editor: Editor,
                          val terminationRequired: AtomicBoolean = AtomicBoolean(),
                          val isDebug: Boolean = false,
                          val currentPsiElement: AtomicReference<PsiElement> = AtomicReference(),
                          val pendingLineRanges: MutableList<IntRange> = ArrayList<IntRange>(),
                          @Volatile var currentLineRange: IntRange? = null,
                          val cancellableExecutionPromise: AtomicReference<CancellablePromise<Unit>> = AtomicReference()) {
  fun revalidateGutter() = invokeLater { (editor as EditorEx).gutterComponentEx.revalidateMarkup() }
}

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
  object : ComboBoxAction() {
    init {
      templatePresentation.icon = AllIcons.General.OpenDisk
      templatePresentation.description = RBundle.message("rmarkdown.editor.toolbar.chooseOutputDirectory")
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = knitRootDirectoryName
    }

    override fun createPopupActionGroup(button: JComponent?): DefaultActionGroup =
      DefaultActionGroup(
        object : AnAction(RBundle.message("rmarkdown.editor.toolbar.documentDirectory")) {
          override fun actionPerformed(e: AnActionEvent) {
            knitRootDirectory(report.parent?.path)
          }
        },
        object : AnAction(RBundle.message("rmarkdown.editor.toolbar.projectDirectory")) {
          override fun actionPerformed(e: AnActionEvent) {
            knitRootDirectory(project.basePath)
          }
        },
        object : AnAction(RBundle.message("rmarkdown.editor.toolbar.customDirectory")) {
          override fun actionPerformed(e: AnActionEvent) {
            val fileDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            val dir = FileChooser.chooseFile(fileDescriptor, project, null)
            if (dir != null && dir.isValid) {
              knitRootDirectory(dir.path)
            }
          }
        })


    private fun knitRootDirectory(path: String?) {
      RMarkdownSettings.getInstance(project).state.setKnitRootDirectory(report.path, path ?: return)
    }

    private val knitRootDirectoryName
      get() = Paths.get(RMarkdownSettings.getInstance(project).state.getKnitRootDirectory(report.path)).fileName.toString()
  }

private abstract class SameTextAction(text: String, icon: Icon? = null) : AnAction(text, text, icon)

