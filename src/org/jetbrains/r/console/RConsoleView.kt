/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar
import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.lang.annotation.AnnotationSession
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.EmptyAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
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
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.ui.AppUIUtil
import com.intellij.ui.JBSplitter
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.IJSwingUtilities
import com.intellij.util.PathUtil
import com.intellij.util.ui.FontInfo
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync
import org.jetbrains.r.RBundle
import org.jetbrains.r.RLanguage
import org.jetbrains.r.annotator.RAnnotatorVisitor
import org.jetbrains.r.debugger.RDebuggerUtil
import org.jetbrains.r.interpreter.RInterpreter
import org.jetbrains.r.psi.RRecursiveElementVisitor
import org.jetbrains.r.rendering.toolwindow.RToolWindowFactory
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RInteropUtil
import java.awt.BorderLayout
import java.awt.Font
import java.awt.event.KeyEvent
import javax.swing.JComponent

class RConsoleView(val rInterop: RInterop, title: String) : LanguageConsoleImpl(rInterop.project, title, RLanguage.INSTANCE) {
  val interpreter: RInterpreter
    get() = rInterop.interpreter
  val executeActionHandler = RConsoleExecuteActionHandler(this)
  val consoleRuntimeInfo = RConsoleRuntimeInfoImpl(rInterop)
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
    EmptyAction.setupAction(olderCommandAction, "RConsole.History.Older", null)
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
        workingDirectory = FileUtil.getLocationRelativeToUserHome(LocalFileSystem.getInstance().extractPresentableUrl(rInterop.workingDir))
      }
    })
  }

  var workingDirectory: String = ""
    set(directory) {
      if (field != directory) {
        field = directory
        invokeLater {
          val content = RConsoleToolWindowFactory.getConsoleContent(this) ?: return@invokeLater
          content.displayName = interpreter.suggestConsoleName(directory)
        }
      }
    }

  fun appendCommandText(text: String) {
    AppUIUtil.invokeOnEdt {
      flushDeferredText()
      runWriteAction {
        consoleEditor.document.setText(text)
        PsiDocumentManager.getInstance(project).commitDocument(consoleEditor.document)
      }
      annotateForHistory()
      prepareExecuteAction(true, false, true)
      (UndoManager.getInstance(project) as UndoManagerImpl).invalidateActionsFor(
        DocumentReferenceManager.getInstance().create(currentEditor.document))
    }
  }

  fun executeText(text: String): Promise<Unit> {
    val promise = AsyncPromise<Unit>()
    invokeLater {
      runWriteAction {
        consoleEditor.document.setText(text)
        PsiDocumentManager.getInstance(project).commitDocument(consoleEditor.document)
      }

      consoleEditor.caretModel.moveToOffset(consoleEditor.document.textLength)
      executeActionHandler.runExecuteActionImpl().onProcessed { promise.setResult(Unit) }
    }
    return promise
  }

  fun createDebuggerPanel() {
    if (ApplicationManager.getApplication().isUnitTestMode) return
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
    rInterop.state.scheduleSkeletonUpdate()
    RToolWindowFactory.refreshPackagePanel(project)
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
    private val EXECUTION_SERVICE = ConcurrencyUtil.newSingleThreadExecutor("RConsole variable view")
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
        val document = console.getConsoleEditor().getDocument()
        if (document.getTextLength() != 0) {
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
      val consoleEditor = console?.getConsoleEditor()
      val enabled = consoleEditor != null &&
                    (e.inputEvent !is KeyEvent ||
                    IJSwingUtilities.hasFocus(consoleEditor.getComponent()) &&
                    !consoleEditor.getSelectionModel().hasSelection()) &&
                    console.executeActionHandler.state != RConsoleExecuteActionHandler.State.TERMINATED
      e.presentation.isEnabled = enabled
    }
  }

  override fun addTextRangeToHistory(textRange: TextRange, inputEditor: EditorEx, preserveMarkup: Boolean): String {
    val addTextRangeToHistory = super.addTextRangeToHistory(textRange, inputEditor, preserveMarkup)
    moveHighlightingFromAnnotatorToHistory(inputEditor, textRange)
    return addTextRangeToHistory
  }

  private fun moveHighlightingFromAnnotatorToHistory(inputEditor: EditorEx, textRange: TextRange) {
    val to = DocumentMarkupModel.forDocument(editor.document, project, true)
    val from = DocumentMarkupModel.forDocument(inputEditor.document, project, true)
    val infos: List<HighlightInfo> = from.allHighlighters.mapNotNull { rangeHighlighter ->
      if (!rangeHighlighter.isValid) return@mapNotNull null
      rangeHighlighter.errorStripeTooltip as? HighlightInfo
    }
    val input = textRange.subSequence(from.document.charsSequence)
    scheduleAnnotationsForHistory(to, input, infos, textRange)
  }

  fun annotateForHistory() {
    val annotationSession = AnnotationSession(file)
    val annotationHolder = AnnotationHolderImpl(annotationSession)
    val annotator = RAnnotatorVisitor(annotationHolder)

    file.accept(object : RRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        element.accept(annotator)
      }
    })

    val infos = annotationHolder.map { HighlightInfo.fromAnnotation(it) }
    val to = DocumentMarkupModel.forDocument(editor.document, project, true)
    val input = consoleEditor.document.charsSequence
    scheduleAnnotationsForHistory(to, input, infos, TextRange(0, input.length))
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
    val promptAttributes = promptAttributes?.attributes
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
    postFlushActions.forEach { it() }
    postFlushActions.clear()
  }

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

    private fun getVirtualFile(e: AnActionEvent): VirtualFile? {
      return FileEditorManager.getInstance(e.project ?: return null).selectedEditor?.file
    }
  }

  class RestartRAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val console = getConsole(e) ?: return
      RConsoleToolWindowFactory.restartConsole(console)
    }
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
