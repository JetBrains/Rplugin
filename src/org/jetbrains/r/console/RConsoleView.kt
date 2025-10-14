/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.r.psi.RBundle
import com.intellij.r.psi.RLanguage
import com.intellij.r.psi.RPluginCoroutineScope
import com.intellij.r.psi.interpreter.RInterpreter
import com.intellij.r.psi.psi.RRecursiveElementVisitor
import com.intellij.ui.JBSplitter
import com.intellij.util.IJSwingUtilities
import com.intellij.util.PathUtil
import com.intellij.util.ui.FontInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.asPromise
import org.jetbrains.concurrency.await
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.annotator.RAnnotatorVisitor
import org.jetbrains.r.debugger.RDebuggerUtil
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rinterop.RInteropImpl
import org.jetbrains.r.rinterop.RInteropUtil
import org.jetbrains.r.run.visualize.RVisualizeTableUtil
import java.awt.BorderLayout
import java.awt.Font
import java.awt.event.KeyEvent
import javax.swing.JComponent

class RConsoleView(val rInterop: RInteropImpl, title: String) : LanguageConsoleImpl(rInterop.project, title, RLanguage.INSTANCE) {
  val interpreter: RInterpreter
    get() = rInterop.interpreter
  val executeActionHandler = RConsoleExecuteActionHandler(this)
  val consoleRuntimeInfo = rInterop.consoleRuntimeInfo
  val isRunningCommand: Boolean
    get() = executeActionHandler.isRunningCommand

  private val onSelectListeners = mutableListOf<() -> Unit>()
  var debuggerPanel: RDebuggerPanel? = null
    private set

  private val postFlushActions = ArrayList<() -> Unit>()

  private val olderCommandAction = RConsoleHistoryOlderCommandAction(this)

  init {
    // we want to dispose interop asynchronously in unit tests
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      Disposer.register(this, rInterop)
    }
    file.putUserData(IS_R_CONSOLE_KEY, true)
    consoleEditor.putUserData(RConsoleAutopopupBlockingHandler.REPL_KEY, this)
    ActionUtil.mergeFrom(olderCommandAction, "RConsole.History.Older")
    olderCommandAction.registerCustomShortcutSet(CustomShortcutSet(KeyEvent.VK_UP), consoleEditor.component)

    RDebuggerUtil.createBreakpointListener(rInterop, this)
    executeActionHandler.addListener(object : RConsoleExecuteActionHandler.Listener {
      var previousWidth = 0

      override fun beforeExecution() {
        val width = (editor as? EditorImpl)?.let {
          val fontMetrics = it.getFontMetrics(Font.PLAIN)
          if (FontInfo.isMonospaced(fontMetrics.font)) {
            (it.component.width - it.scrollPane.verticalScrollBar.width) / fontMetrics.charWidth(' ')
          } else {
            null
          }
        } ?: DEFAULT_WIDTH
        if (previousWidth != width) {
          previousWidth = width
          rInterop.setOutputWidth(width)
        }
      }
    })
    executeActionHandler.addListener(object : RConsoleExecuteActionHandler.Listener {
      override fun onCommandExecuted() {
        workingDirectory = rInterop.workingDir
        RVisualizeTableUtil.refreshTables(project)
      }
    })
  }

  var workingDirectory: String = ""
    set(directory) {
      if (field != directory) {
        field = directory
        RPluginCoroutineScope.getScope(project).launch(Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()) {
          val content = RConsoleToolWindowFactory.getConsoleContent(this@RConsoleView) ?: return@launch
          content.displayName = interpreter.suggestConsoleName(directory)
        }
      }
    }

  fun appendCommandText(text: String) {
    RPluginCoroutineScope.getScope(project).launch(Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()) {
      flushDeferredText()
      writeAction {
        consoleEditor.document.setText(text)
        PsiDocumentManager.getInstance(project).commitDocument(consoleEditor.document)
      }
      annotateForHistory()
      prepareExecuteAction(true, false, true)
      (UndoManager.getInstance(project) as UndoManagerImpl).invalidateActionsFor(
        DocumentReferenceManager.getInstance().create(currentEditor.document))
    }
  }

  fun executeText(text: String): Promise<Unit> =
    RPluginCoroutineScope.getScope(project).async(Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement()) {
      writeAction {
        consoleEditor.document.setText(text)
        PsiDocumentManager.getInstance(project).commitDocument(consoleEditor.document)
      }

      consoleEditor.caretModel.moveToOffset(consoleEditor.document.textLength)
      executeActionHandler.runExecuteActionImpl().await()
    }.asCompletableFuture().asPromise()

  fun createDebuggerPanel() {
    debuggerPanel = RDebuggerPanel(this).also {
      Disposer.register(this, it)
      executeActionHandler.addListener(it)
      splitWindow(it)
    }
  }

  fun resetHandler() {
    executeActionHandler.resetListeners()
  }

  fun addOnSelectListener(listener: () -> Unit) {
    onSelectListeners.add(listener)
  }

  fun onSelect() {
    for (listener in onSelectListeners) {
      listener()
    }
    if (rInterop.isAlive) {
      rInterop.invalidateCaches()
      RToolWindowFactory.refreshPackagePanel(project)
    }
  }

  private fun splitWindow(splitView: JComponent) {
    val console = getComponent(0)
    removeAll()
    val p = JBSplitter(false, 1f / 2)
    p.setFirstComponent(console as JComponent)
    p.setSecondComponent(splitView)
    p.isShowDividerControls = true
    p.setHonorComponentsMinimumSize(true)

    add(p, BorderLayout.CENTER)
    validate()
    repaint()
  }

  companion object {
    private const val DEFAULT_WIDTH = 160
    val IS_R_CONSOLE_KEY = Key.create<Boolean>("IS_R_CONSOLE")
    val R_CONSOLE_DATA_KEY = DataKey.create<RConsoleView>("R_CONSOLE")
    const val INTERRUPT_ACTION_ID = "org.jetbrains.r.console.RConsoleView.RInterruptAction"
    const val EOF_ACTION_ID = "org.jetbrains.r.console.RConsoleView.REofAction"

    private fun getConsole(e: AnActionEvent) =
      e.getData(R_CONSOLE_DATA_KEY) ?: e.project?.let { RConsoleManager.getInstance(it).currentConsoleOrNull }
  }

  class RInterruptAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val console = getConsole(e) ?: return
      if (console.isRunningCommand) {
        console.executeActionHandler.interruptTextExecution()
        console.print("^C\n", ConsoleViewContentType.SYSTEM_OUTPUT)
      } else {
        val document = console.consoleEditor.getDocument()
        if (document.textLength != 0) {
          runWriteAction {
            CommandProcessor.getInstance().runUndoTransparentAction {
              document.deleteString(0, document.getLineEndOffset(document.getLineCount() - 1))
            }
          }
        }
      }
    }

    override fun update(e: AnActionEvent) {
      val console = getConsole(e)
      val consoleEditor = console?.consoleEditor
      val enabled = consoleEditor != null &&
                    (e.inputEvent !is KeyEvent ||
                    IJSwingUtilities.hasFocus(consoleEditor.getComponent()) &&
                    !consoleEditor.getSelectionModel().hasSelection()) &&
                    console.executeActionHandler.state != RConsoleExecuteActionHandler.State.TERMINATED
      e.presentation.isEnabled = enabled
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  private fun annotateForHistory() {
    val holder:MutableList<HighlightInfo> = ArrayList()

    val annotator = RAnnotatorVisitor(holder, UserDataHolderBase())

    file.accept(object : RRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        element.accept(annotator)
      }
    })

    val to = DocumentMarkupModel.forDocument(editor!!.document, project, true)
    val input = consoleEditor.document.charsSequence
    scheduleAnnotationsForHistory(to, input, holder, TextRange(0, input.length))
  }

  private fun scheduleAnnotationsForHistory(to: MarkupModel, input: CharSequence, infos: List<HighlightInfo>, textRange: TextRange) {
    if (infos.isEmpty()) return
    val severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project)
    val nextLineBegins: List<Int> = input.mapIndexedNotNull { i, c -> if (c == '\n') i + 1 else null } + input.length

    var nextNewLineIndex = 0
    var currentLineStart = 0

    val lineToHighlights = arrayOfNulls<MutableList<(Int) -> Unit>>(nextLineBegins.size)

    val sorted = infos.sortedWith(Comparator
                                    .comparingInt<HighlightInfo> { it.startOffset }
                                     .thenComparingInt { it.endOffset })
    for (highlightInfo in sorted) {
      if (highlightInfo.severity !== HighlightSeverity.INFORMATION) continue
      if (highlightInfo.type.attributesKey === EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES) continue
      if (highlightInfo.startOffset < textRange.startOffset || highlightInfo.endOffset > textRange.endOffset) continue

      val startInRange = highlightInfo.startOffset - textRange.startOffset
      val endInRange = highlightInfo.endOffset - textRange.startOffset

      while (startInRange >= nextLineBegins[nextNewLineIndex]) {
        currentLineStart = nextLineBegins[nextNewLineIndex]
        nextNewLineIndex++
      }
      if (endInRange > nextLineBegins[nextNewLineIndex]) {
        // Do not consider highlights for several lines at once!
        continue
      }
      val start = startInRange - currentLineStart
      val end = endInRange - currentLineStart

      val layer = getLayer(highlightInfo, severityRegistrar)
      val textAttributes = highlightInfo.getTextAttributes(file, consoleEditor.colorsScheme)

      val applyHighlight = { lineOffset: Int ->
        val h = to.addRangeHighlighter(start + lineOffset, end + lineOffset, layer,
                                       textAttributes, HighlighterTargetArea.EXACT_RANGE)
        (h as RangeHighlighterEx).isAfterEndOfLine = false
      }
      val list = lineToHighlights[nextNewLineIndex] ?: ArrayList<(Int) -> Unit>().also {
        lineToHighlights[nextNewLineIndex] = it
      }
      list.add(applyHighlight)
    }

    val historyLengthBeforeInput = to.document.textLength
    val promptAttributes = promptAttributes.attributes
    postFlushActions.add {
      val allHighlighters = to.allHighlighters.toList()
      var currentOffset = historyLengthBeforeInput
      for (currentLine in 0..nextLineBegins.size) {
        // Firstly, find console prompt
        val promptIndex = allHighlighters.indexOfFirst { it.startOffset == currentOffset }.takeIf { it != -1 } ?: break
        val prompt = allHighlighters[promptIndex]
        if (prompt.textAttributes != promptAttributes)
          break // very strange

        // shift to the start of input code
        currentOffset = prompt.endOffset

        // and apply highlights from the current line
        lineToHighlights[currentLine]?.forEach {
          it(currentOffset)
        }
        currentOffset += nextLineBegins[currentLine] - (if (currentLine > 0) nextLineBegins[currentLine - 1] else 0)
      }
    }
  }

  override fun flushDeferredText() {
    super.flushDeferredText()
    RPluginCoroutineScope.getScope(project).launch(Dispatchers.EDT + ModalityState.any().asContextElement()) {
      if (!hasDeferredOutput()) {
        postFlushActions.forEach { it() }
        postFlushActions.clear()
      }
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  class REofAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val console = getConsole(e) ?: return
      if (console.executeActionHandler.state == RConsoleExecuteActionHandler.State.SUBPROCESS_INPUT) {
        console.rInterop.replSendEof()
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isVisible = false
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  class RSetCurrentDirectoryFromEditor : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val console = getConsole(e) ?: return
      val file = getVirtualFile(e) ?: return
      val filePath = console.interpreter.getFilePathAtHost(file) ?: return
      val dirPath = PathUtil.getParentPath(filePath)
      runAsync {
        console.rInterop.setWorkingDir(dirPath)
        console.executeActionHandler.fireCommandExecuted()
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = false
      val console = getConsole(e) ?: return
      val file = getVirtualFile(e) ?: return
      console.interpreter.getFilePathAtHost(file) ?: return
      e.presentation.isEnabled = true
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    private fun getVirtualFile(e: AnActionEvent): VirtualFile? {
      return FileEditorManager.getInstance(e.project ?: return null).selectedEditor?.file
    }
  }

  class RestartRAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val console = getConsole(e) ?: return
      RConsoleToolWindowFactory.restartConsole(console)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  class TerminateRWithReportAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val console = getConsole(e) ?: return
      val handler = console.rInterop.getUserData(RInteropUtil.TERMINATE_WITH_REPORT_HANDLER) ?: return
      val yesNo = Messages.showYesNoDialog(e.project, RBundle.message("console.terminate.with.report.message"),
                                           RBundle.message("console.terminate.with.report.title"), null)
      if (yesNo == Messages.YES) {
        console.rInterop.processHandler.putUserData(RInteropUtil.PROCESS_TERMINATED_WITH_REPORT, true)
        handler()
      }
    }

    override fun update(e: AnActionEvent) {
      val console = getConsole(e)
      e.presentation.isEnabled = console?.rInterop?.isAlive == true &&
                                 console.rInterop.getUserData(RInteropUtil.TERMINATE_WITH_REPORT_HANDLER) != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }
}

internal const val R_CONSOLE_PROMPT = "> "
internal const val R_CONSOLE_DEBUG_PROMPT = "Debug> "
internal const val R_CONSOLE_CONTINUE = "+ "
internal const val R_CONSOLE_READ_LN_PROMPT = "?> "

// Copy-paste from UpdateHighlightersUtil#getLayer
private fun getLayer(info: HighlightInfo, severityRegistrar: SeverityRegistrar): Int {
  val severity = info.severity
  return when {
    severity === HighlightSeverity.WARNING -> HighlighterLayer.WARNING
    severity === HighlightSeverity.WEAK_WARNING -> HighlighterLayer.WEAK_WARNING
    severityRegistrar.compare(severity, HighlightSeverity.ERROR) >= 0 -> HighlighterLayer.ERROR
    severity === HighlightInfoType.INJECTED_FRAGMENT_SEVERITY -> HighlighterLayer.CARET_ROW - 1
    severity === HighlightInfoType.INJECTED_FRAGMENT_SYNTAX_SEVERITY -> HighlighterLayer.CARET_ROW - 2
    severity === HighlightInfoType.ELEMENT_UNDER_CARET_SEVERITY -> HighlighterLayer.ELEMENT_UNDER_CARET
    else -> HighlighterLayer.ADDITIONAL_SYNTAX
  }
}
