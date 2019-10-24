// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.debugger

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ConcurrencyUtil
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XBreakpointListener
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.impl.XDebuggerUtilImpl
import com.intellij.xdebugger.impl.frame.XDebuggerFramesList
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter
import icons.PlatformDebuggerImplIcons
import icons.org.jetbrains.r.RBundle
import org.intellij.datavis.inlays.components.ProcessOutput
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedCancellablePromise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.console.RConsoleView
import org.jetbrains.r.debugger.exception.RDebuggerException
import org.jetbrains.r.rinterop.RRef
import org.jetbrains.r.rinterop.Service.DebuggerCommand
import org.jetbrains.r.run.debug.RLineBreakpointType
import org.jetbrains.r.run.debug.stack.RXStackFrame
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class RDebugger(private val consoleView: RConsoleView) {
  @Volatile var isEnabled = false
    private set
  private val actions = mutableListOf<AnAction>()
  private val executor = ConcurrencyUtil.newSingleThreadExecutor("RDebugger")
  @Volatile var actionsEnabled = false
    private set
  private val rInterop = consoleView.rInterop
  val project = consoleView.project
  private val codeGenerator = RDebugCodeGenerator(project)
  private val debugCodeViewer = RDebugCodeViewer(project, consoleView)

  private data class LocationEntry(val functionName: String, var position: XSourcePosition?, var equalityId: Any? = null) {
    override fun equals(other: Any?): Boolean {
      return other is LocationEntry && functionName == other.functionName &&
             position?.line == other.position?.line && position?.file?.path == other.position?.file?.path
    }
  }
  private val locationsStack = mutableListOf<LocationEntry>()
  private var currentEqualityId = 0
  private val globalStackFrameList = listOf(RXStackFrame("[global]", null, rInterop.globalEnvLoader, executor,
                                                         true, equalityObject = -1))

  private val positionHighlighter = ExecutionPointHighlighter(project)

  private val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
  private val breakpoints = mutableMapOf<XSourcePositionWrapper, XLineBreakpoint<XBreakpointProperties<*>>>()
  private var isTracedStep = false
  private val debugCommandsQueue = mutableListOf<DebuggerCommand>()

  private var variablesView: RXVariablesView? = null
  private var framesView: XDebuggerFramesList? = null
  @Volatile var stack: List<RXStackFrame> = emptyList()
    private set

  private var currentSourceFile: VirtualFile? = null

  private sealed class Command {
    object Resume : Command()
    object Pause : Command()
    class StepInto(val level: Int) : Command()
    object ForceStepInto : Command()
    class StepOver(val level: Int) : Command() {
      var done = false
    }
    class StepOut(val level: Int) : Command() {
      var done = false
    }
    class RunToPosition(val position: XSourcePosition) : Command()
  }
  private var currentCommand: Command? = null
  private var interactivePromise: AsyncPromise<Unit>? = null
  private var debuggerEndPromise: AsyncPromise<Unit>? = null

  @Volatile private var breakpointsMuted = false

  @Volatile var isVariableRefreshEnabled = true
    set(value) {
      field = value
      if (value) {
        refreshVariableView()
      }
    }

  init {
    val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(RLineBreakpointType::class.java)
    val listener = BreakpointListener()
    breakpointManager.addBreakpointListener(breakpointType, listener, consoleView)
    breakpointManager.getBreakpoints(breakpointType).forEach { listener.breakpointAdded(it) }
  }

  private fun enable(): AsyncPromise<Unit> {
    if (isEnabled) return debuggerEndPromise!!
    isEnabled = true
    isTracedStep = false
    currentCommand = null
    updateStack(emptyList())
    debuggerEndPromise = AsyncPromise()
    return debuggerEndPromise!!
  }

  private fun disable() {
    if (!isEnabled) return
    leaveInteractive()
    debugCodeViewer.prepareCleanUp()
    debugCodeViewer.cleanUp()
    debugCommandsQueue.clear()
    currentSourceFile = null
    isEnabled = false
    debuggerEndPromise?.let {
      debuggerEndPromise = null
      it.setResult(Unit)
    }
  }

  private fun resolveInteractivePromise() {
    interactivePromise?.let {
      interactivePromise = null
      it.setResult(Unit)
    }
  }

  private fun enterInteractive() {
    if (!isEnabled) return
    currentCommand = null
    actionsEnabled = true
    debugCodeViewer.cleanUp()
    refreshVariableView()
    resolveInteractivePromise()
  }

  private fun leaveInteractive() {
    if (!isEnabled) return
    actionsEnabled = false
    updateStack(emptyList())
    positionHighlighter.hide()
  }

  fun refreshVariableView() {
    if (!isVariableRefreshEnabled) return
    if (isEnabled) {
      updateStack(calculateRXStackFrames())
    } else {
      updateStack(globalStackFrameList)
    }
  }

  private fun executeAction(f: () -> Unit): Promise<Unit> {
    val promise = AsyncPromise<Unit>()
    executor.execute {
      interactivePromise = promise
      f()
    }
    return promise
  }

  fun resume(): Promise<Unit> {
    if (!actionsEnabled) return resolvedCancellablePromise(Unit)
    return executeAction {
      currentCommand = Command.Resume
      rInterop.replSendDebuggerCommand(DebuggerCommand.CONTINUE)
    }
  }

  fun pause(): Promise<Unit> {
    if (!isEnabled) return resolvedCancellablePromise(Unit)
    return executeAction {
      currentCommand = Command.Pause
    }
  }

  fun stop(): Promise<Unit> {
    if (!isEnabled) return resolvedCancellablePromise(Unit)
    return executeAction {
      rInterop.replInterrupt()
      rInterop.replSendDebuggerCommand(DebuggerCommand.QUIT)
    }
  }

  fun stepOver(): Promise<Unit> {
    if (!actionsEnabled) return resolvedCancellablePromise(Unit)
    return executeAction {
      currentCommand = Command.StepOver(locationsStack.size)
      rInterop.replSendDebuggerCommand(DebuggerCommand.NEXT)
    }
  }

  fun stepInto(): Promise<Unit> {
    if (!actionsEnabled) return resolvedCancellablePromise(Unit)
    return executeAction {
      currentCommand = Command.StepInto(locationsStack.size)
      rInterop.replSendDebuggerCommand(DebuggerCommand.NEXT)
    }
  }

  fun forceStepInto(): Promise<Unit> {
    if (!actionsEnabled) return resolvedCancellablePromise(Unit)
    return executeAction {
      currentCommand = Command.ForceStepInto
      rInterop.replSendDebuggerCommand(DebuggerCommand.STEP)
    }
  }

  fun stepOut(): Promise<Unit> {
    if (!actionsEnabled) return resolvedCancellablePromise(Unit)
    return executeAction {
      currentCommand = Command.StepOut(locationsStack.size)
      rInterop.replSendDebuggerCommand(DebuggerCommand.CONTINUE)
    }
  }

  fun runToCursor(position: XSourcePosition): Promise<Unit> {
    if (!actionsEnabled) return resolvedCancellablePromise(Unit)
    if (RDebugCodeViewer.isViewerFile(position.file)) {
      val notification = Notification(
        "RDebugger", RBundle.message("debugger.title"), RBundle.message("debugger.run.to.cursor.not.available"),
        NotificationType.WARNING, null)
      notification.notify(project)
      return resolvedPromise(Unit)
    }
    return executeAction {
      currentCommand = Command.RunToPosition(position)
      rInterop.replSendDebuggerCommand(DebuggerCommand.CONTINUE)
    }
  }

  fun handlePrompt(isDebug: Boolean, debugLines: List<String>): Promise<Boolean> {
    val promise = AsyncPromise<Boolean>()
    executor.execute {
      if (!isDebug) {
        disable()
        refreshVariableView()
        resolveInteractivePromise()
        promise.setResult(true)
        return@execute
      }
      if (!isEnabled) {
        rInterop.replSendDebuggerCommand(DebuggerCommand.FINISH)
        promise.setResult(true)
        return@execute
      }
      if (debugCommandsQueue.isNotEmpty()) {
        rInterop.replSendDebuggerCommand(debugCommandsQueue.removeAt(debugCommandsQueue.lastIndex))
        promise.setResult(false)
        return@execute
      }
      if (debugLines.isEmpty()) {
        enterInteractive()
        promise.setResult(true)
        return@execute
      }
      val isTracedStepNow = isTracedStep
      if (isTracedStepNow) {
        recalculateStack()
      }
      isTracedStep = false
      for (debugLine in debugLines) {
        handleBreakpointStart(debugLine)?.let {
          isTracedStep = true
          rInterop.replSendDebuggerCommand(it[0])
          debugCommandsQueue.addAll(it.drop(1).reversed())
          promise.setResult(false)
          return@execute
        }
        when {
          debugLine.startsWith("Called from:") -> {
            if (debugLine.trim() != "Called from: top level") {
              rInterop.replSendDebuggerCommand(DebuggerCommand.NEXT)
              isTracedStep = true
              promise.setResult(false)
              return@execute
            }
            recalculateStack()
          }
          debugLine.startsWith("debug at ") || debugLine.startsWith("debug: ") -> {
            val position = getSourcePosition(debugLine, 0, locationsStack.last().functionName)
            updateStackPosition(position)
            when (val command = currentCommand) {
              is Command.StepOut -> if (locationsStack.size < command.level) command.done = true
              is Command.StepOver -> if (locationsStack.size <= command.level) command.done = true
            }
          }
          debugLine.startsWith("exiting from: ") -> {
            popStackFrame()
            when (val command = currentCommand) {
              is Command.StepOut -> if (locationsStack.size < command.level) command.done = true
              is Command.StepOver -> if (locationsStack.size <= command.level) command.done = true
            }
          }
          debugLine.startsWith("debugging in: ") -> {
            pushStackFrame(RDebuggerUtils.getFunctionNameByText(debugLine.drop("debugging in: ".length), project))
          }
        }
      }

      val position = locationsStack.lastOrNull()?.position
      if (isTracedStepNow && position != null) {
        val breakpoint = breakpoints[XSourcePositionWrapper(position)]
        if (breakpoint != null && breakpoint.isEnabled && !breakpointsMuted) {
          rInterop.updateSysFrames()
          val condition = breakpoint.conditionExpression
          val conditionResult = condition?.let {
            RRef.expressionRef(it.expression, rInterop).evaluateAsBoolean()
          } ?: true
          if (conditionResult) {
            breakpoint.logExpressionObject?.let {
              val result = RRef.expressionRef(it.expression, rInterop).evaluateAsText().trim()
              consoleView.print(result + "\n", ConsoleViewContentType.ERROR_OUTPUT)
            }
            if (breakpoint.isTemporary) {
              ApplicationManager.getApplication().invokeLater {
                runWriteAction { breakpointManager.removeBreakpoint(breakpoint) }
              }
            }
            if (breakpoint.suspendPolicy != SuspendPolicy.NONE) {
              enterInteractive()
              promise.setResult(true)
              return@execute
            }
          }
        }
      }

      val suspend = if (currentSourceFile != null && !locationsStack.any { it.position?.file == currentSourceFile }) {
        false
      } else {
        when (val command = currentCommand) {
          null -> false
          Command.Resume -> false
          Command.Pause -> true
          is Command.StepInto -> {
            val file = locationsStack.last().position?.file
            !(command.level < locationsStack.size && (file == null || RDebugCodeViewer.isViewerFile(file)))
          }
          Command.ForceStepInto -> true
          is Command.StepOver -> command.done
          is Command.StepOut -> command.done
          is Command.RunToPosition -> position != null && XSourcePositionWrapper(position) == XSourcePositionWrapper(command.position)
        }
      }
      if (suspend) {
        enterInteractive()
      } else {
        rInterop.replSendDebuggerCommand(DebuggerCommand.CONTINUE)
      }
      promise.setResult(suspend)
    }
    return promise
  }

  private fun getSourcePosition(debugLine: String, level: Int, functionName: String): XSourcePosition? {
    val match = Regex("(debug|where \\d+)( at (.*#\\d+))?: (.*)", RegexOption.DOT_MATCHES_ALL).matchEntire(debugLine) ?: return null
    val position = codeGenerator.parseXSourcePosition(match.groupValues[3])
    if (position != null) return position
    if (functionName == RBundle.message("debugger.global.stack.frame")) return null
    val text = rInterop.debugGetSysFunctionCode(level)
    return debugCodeViewer.calculatePosition(functionName, text, match.groupValues[4])
  }

  fun executeDebugSource(sourceFile: VirtualFile): Promise<Unit> {
    if (isEnabled) throw RDebuggerException(RBundle.message("debugger.still.running"))
    return executeAction {
      val (code, fileId) = codeGenerator.prepareDebugSource(sourceFile)
      executeImpl(sourceFile, code, fileId)
      consoleView.print(RBundle.message("debugger.debugging.message", sourceFile.canonicalPath.orEmpty()) + "\n",
                        ConsoleViewContentType.ERROR_OUTPUT)
    }
  }

  fun executeChunk(sourceFile: VirtualFile, range: TextRange): Promise<List<ProcessOutput>> {
    if (isEnabled) throw RDebuggerException(RBundle.message("debugger.still.running"))
    val resultPromise = AsyncPromise<List<ProcessOutput>>()
    executeAction {
      val (code, fileId) = codeGenerator.prepareDebugSource(sourceFile, range)
      executeImpl(sourceFile, code, fileId).then { resultPromise.setResult(emptyList()) }
      consoleView.print(RBundle.message("debugger.debugging.chunk.message", sourceFile.canonicalPath.orEmpty()) + "\n",
                        ConsoleViewContentType.ERROR_OUTPUT)
    }
    return resultPromise
  }

  private fun executeImpl(sourceFile: VirtualFile, source: String, fileId: String): Promise<Unit> {
    if (isEnabled) throw RDebuggerException(RBundle.message("debugger.still.running"))
    currentSourceFile = sourceFile
    isEnabled = true
    isTracedStep = false
    currentCommand = null
    updateStack(emptyList())
    debuggerEndPromise = AsyncPromise()
    rInterop.debugExecute(source, fileId)
    return debuggerEndPromise!!
  }

  private fun updateStack(stack: List<RXStackFrame>) {
    this.stack = stack
    val wasSelected = variablesView?.stackFrame?.equalityObject?.takeIf { framesView?.selectedIndex != 0 }
    framesView?.model?.replaceAll(stack)
    if (stack.isEmpty()) {
      variablesView?.stackFrame = null
    } else {
      framesView?.selectedIndex = stack.indexOfFirst { wasSelected != null && it.equalityObject == wasSelected }.takeIf { it != -1 } ?: 0
    }
  }

  fun createDebugWindow(): JComponent {
    val variablesView = RXVariablesView(project, consoleView)
    this.variablesView = variablesView
    val framesView = XDebuggerFramesList(project)
    this.framesView = framesView
    framesView.addListSelectionListener {
      val frame = framesView.selectedValue as? RXStackFrame
      if (isEnabled) {
        val position = frame?.sourcePosition
        if (position == null) {
          positionHighlighter.hide()
        } else {
          positionHighlighter.show(position, frame != framesView.model.getElementAt(0), null)
        }
      }
      variablesView.stackFrame = frame
    }
    val framesViewScrollPane = JBScrollPane(framesView)

    val tabs = JBTabbedPane(SwingConstants.TOP)
    tabs.add(RBundle.message("debugger.tab.variables"), variablesView.panel)
    tabs.add(RBundle.message("debugger.tab.frames"), framesViewScrollPane)

    val toolbarActions = createDebugActions()
    val actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false)
    val debugWindow = JPanel(BorderLayout())
    debugWindow.add(tabs, BorderLayout.CENTER)
    debugWindow.add(actionToolbar.component, BorderLayout.WEST)
    Disposer.register(consoleView, variablesView)
    refreshVariableView()
    return debugWindow
  }

  private fun createDebugActions(): ActionGroup {
    val actions = DefaultActionGroup()
    actions.add(createAction(
      ActionsBundle.message("action.Resume.text"), AllIcons.Actions.Resume, "Resume") { resume() })
    actions.add(createAction(
      ActionsBundle.message("action.Pause.text"), AllIcons.Actions.Pause, isActive = { isEnabled && !actionsEnabled }) { pause() })
    actions.add(createAction(
      ActionsBundle.message("action.Stop.text"), AllIcons.Actions.Suspend, "Stop", isActive = { isEnabled }) { stop() })
    actions.addSeparator()
    actions.add(createAction(
      ActionsBundle.message("action.StepOver.text"), AllIcons.Actions.TraceOver, "StepOver") { stepOver() })
    actions.add(createAction(
      ActionsBundle.message("action.StepInto.text"), AllIcons.Actions.TraceInto, "StepInto") { stepInto() })
    actions.add(createAction(
      ActionsBundle.message("action.ForceStepInto.text"),
      PlatformDebuggerImplIcons.Actions.Force_step_into, "ForceStepInto") { forceStepInto() })
    actions.add(createAction(
      ActionsBundle.message("action.StepOut.text"), AllIcons.Actions.StepOut, "StepOut") { stepOut() })
    actions.add(createAction(
      ActionsBundle.message("action.RunToCursor.text"), AllIcons.Actions.RunToCursor, "RunToCursor") {
      val position = XDebuggerUtilImpl.getCaretPosition(project, it.dataContext)
      if (position != null) runToCursor(position)
    })
    actions.addSeparator()
    actions.add(ActionManager.getInstance().getAction("ViewBreakpoints"))
    actions.add(createMuteBreakpointsAction())
    actions.addSeparator()
    actions.add(createAction("Evaluate Expression", AllIcons.Debugger.EvaluateExpression, "EvaluateExpression",
                             isActive = { if (isEnabled) actionsEnabled else !consoleView.isRunningCommand }) {
      RDebuggerEvaluateHandler.perform(this, (variablesView?.stackFrame ?: globalStackFrameList[0]).evaluator,
                                       it.dataContext)
    })
    return actions
  }

  private fun createAction(text: String, icon: Icon, actionId: String? = null, isActive: () -> Boolean = { actionsEnabled },
                           callback: (AnActionEvent) -> Unit): AnAction {
    var toolWindow: ToolWindow? = null
    RConsoleToolWindowFactory.runWhenAvailable(project) { toolWindow = it }
    val action = object : AnAction(text, null, icon) {
      override fun actionPerformed(e: AnActionEvent) = callback(e)

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = toolWindow?.isVisible == true && isActive()
      }
    }
    if (actionId != null) {
      action.registerCustomShortcutSet(ShortcutSet { KeymapManager.getInstance().activeKeymap.getShortcuts(actionId) },
                                       WindowManager.getInstance().getFrame(project)?.rootPane, consoleView)
    }
    actions.add(action)
    return action
  }

  private fun createMuteBreakpointsAction(): ToggleAction {
    return object : DumbAwareToggleAction(null, ActionsBundle.message("action.Debugger.MuteBreakpoints.text"),
                                          AllIcons.Debugger.MuteBreakpoints) {
      override fun isSelected(e: AnActionEvent) = breakpointsMuted

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        breakpointsMuted = state
      }
    }
  }

  private fun recalculateStack() {
    debugCodeViewer.prepareCleanUp()
    var previousFunctionName = RBundle.message("debugger.global.stack.frame")
    val lines = splitWhereOutput(rInterop.debugWhere())
    var keepEqualityId = true
    val newStack = lines.reversed().mapIndexed { index, line ->
      val match = WHERE_ENTRY_REGEX.matchEntire(line)
      val position = getSourcePosition(line, lines.size - index, previousFunctionName)
      val entry = LocationEntry(previousFunctionName, position)
      if (entry != locationsStack.getOrNull(index)) keepEqualityId = false
      entry.equalityId = if (keepEqualityId) locationsStack[index].equalityId else currentEqualityId++
      previousFunctionName = RDebuggerUtils.getFunctionNameByText(match?.groupValues?.getOrNull(3).orEmpty(), project)
      entry
    }
    val lastEntry = LocationEntry(previousFunctionName, null)
    lastEntry.equalityId = if (keepEqualityId && lastEntry.functionName == locationsStack.getOrNull(newStack.size)?.functionName) {
      locationsStack[newStack.size].equalityId
    } else {
      currentEqualityId++
    }
    locationsStack.clear()
    locationsStack.addAll(newStack)
    locationsStack.add(lastEntry)
  }

  private fun updateStackPosition(position: XSourcePosition?) {
    locationsStack.lastOrNull()?.let { it.position = position }
  }

  private fun popStackFrame() {
    if (locationsStack.isEmpty()) return
    locationsStack.removeAt(locationsStack.lastIndex)
  }

  private fun pushStackFrame(functionName: String) {
    locationsStack.add(LocationEntry(functionName, null, currentEqualityId++))
  }

  private fun calculateRXStackFrames(): List<RXStackFrame> {
    val locationsStackReversed = locationsStack
      .dropWhile { currentSourceFile != null && it.position?.file != currentSourceFile }
      .reversed()
    val nFrames = rInterop.updateSysFrames()
    return locationsStackReversed.mapIndexed { index, it ->
      val frameIndex = (nFrames - 1 - index).takeIf { it >= 0 }
      val loader = if (frameIndex != null) RRef.sysFrameRef(frameIndex, rInterop).createVariableLoader() else rInterop.globalEnvLoader
      val functionName = if (currentSourceFile != null && index == locationsStackReversed.lastIndex) {
        RBundle.message("debugger.global.stack.frame")
      } else {
        it.functionName
      }
      RXStackFrame(functionName, it.position, loader, executor,
                   it.position?.file?.let { RDebugCodeViewer.isViewerFile(it) } ?: true,
                   equalityObject = it.equalityId)
    }
  }

  private inner class BreakpointListener : XBreakpointListener<XLineBreakpoint<XBreakpointProperties<*>>> {
    override fun breakpointAdded(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
      breakpoint.sourcePosition?.let { breakpoints[XSourcePositionWrapper(it)] = breakpoint }
    }

    override fun breakpointRemoved(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
      breakpoint.sourcePosition?.let { breakpoints.remove(XSourcePositionWrapper(it)) }
    }

    override fun breakpointChanged(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
      breakpoints.asSequence().firstOrNull { it.value == breakpoint }
        ?.let { breakpoints.remove(it.key) }
      breakpoint.sourcePosition?.let { breakpoints[XSourcePositionWrapper(it)] = breakpoint }
    }
  }

  companion object {
    private val WHERE_ENTRY_REGEX = Regex("where \\d+( at ([^\n]*#\\d+))?: (.*)", RegexOption.DOT_MATCHES_ALL)

    private fun splitWhereOutput(text: String): List<String> {
      val result = mutableListOf<String>()
      val last = StringBuilder()
      var index = 1
      for (line in StringUtil.splitByLinesKeepSeparators(text)) {
        if (line.startsWith("where $index")) {
          index++
          if (last.isNotEmpty()) {
            result.add(last.toString())
            last.clear()
          }
        }
        if (index > 1) last.append(line)
      }
      if (last.isNotEmpty()) {
        result.add(last.toString())
      }
      return result
    }

    private fun handleBreakpointStart(debugLine: String): List<DebuggerCommand>? {
      val lines = StringUtil.splitByLines(debugLine)
      if (lines.size == 1 && lines[0].trim().endsWith(": .doTrace(browser())")) {
        return listOf(DebuggerCommand.NEXT, DebuggerCommand.FINISH)
      }
      if (lines.size >= 2 && lines[0].trim().endsWith(": {") && lines[1].trim() == ".doTrace(browser())") {
        return listOf(DebuggerCommand.NEXT, DebuggerCommand.NEXT, DebuggerCommand.FINISH)
      }
      return null
    }
  }
}

private class XSourcePositionWrapper(private val position: XSourcePosition) {
  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other == null || javaClass != other.javaClass) return false
    val wrapper = other as XSourcePositionWrapper
    return position.line == wrapper.position.line && position.file.path == wrapper.position.file.path
  }

  override fun hashCode() = 31 * position.line + position.file.path.hashCode()
}
