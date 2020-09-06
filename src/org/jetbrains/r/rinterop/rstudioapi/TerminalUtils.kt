package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.plugins.terminal.*
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject
import java.io.File
import kotlin.math.max
import kotlin.math.min

object TerminalUtils {
  fun terminalActivate(rInterop: RInterop, args: RObject) {
    TODO()
  }

  fun terminalBuffer(rInterop: RInterop, args: RObject): RObject {
    //TODO stripAnsi
    val id = args.list.getRObjects(0).rString.getStrings(0)
    val terminal = terminalFromId(rInterop, id) ?: return rError("Unknown terminal identifier '$id'")
    val widget: ShellTerminalWidget = TerminalView.getWidgetByContent(terminal)!! as ShellTerminalWidget
    val lst = RObject.RString.newBuilder()
    val lines = widget.terminalTextBuffer.screenLines
    lst.addAllStrings(lines.trim().split("\n").map { it.trim() })
    return RObject.newBuilder().setRString(lst).build()
  }

  fun terminalBusy(rInterop: RInterop, args: RObject): RObject {
    val id = args.list.getRObjects(0).rString.getStrings(0)
    val terminal = terminalFromId(rInterop, id) ?: return false.toRBoolean()
    val widget: ShellTerminalWidget = TerminalView.getWidgetByContent(terminal)!! as ShellTerminalWidget
    return widget.hasRunningCommands().toRBoolean()
  }

  fun terminalClear(rInterop: RInterop, args: RObject): RObject {
    val id = args.list.getRObjects(0).rString.getStrings(0)
    val terminal = terminalFromId(rInterop, id) ?: return rError("Unknown terminal identifier '$id'")
    val widget: ShellTerminalWidget = TerminalView.getWidgetByContent(terminal)!! as ShellTerminalWidget
    clearTypedCommand(widget)
    widget.executeCommand("clear")
    return getRNull()
  }

  fun terminalExecute(rInterop: RInterop, args: RObject): RObject {
    val command = args.list.getRObjects(0).rString.getStrings(0)
    val workingDir = args.list.getRObjects(1).let {
      it.toStringOrNull()
    }
    val env = args.list.getRObjects(2).rString.stringsList.toList().map {
      val (f, s) = it.split("=")
      f to s
    }.associate { it }
    val show = args.list.getRObjects(3).rBoolean.getBooleans(0)

    val terminalWidget = TerminalView.getInstance(rInterop.project).createLocalShellWidget(workingDir, null)
    TerminalOptionsProvider.instance.setEnvData(com.intellij.execution.configuration.EnvironmentVariablesData.create(env, true))
    terminalWidget.executeCommand(command)
    return idFromTerminal(rInterop, terminalWidget)?.toRString() ?: getRNull()
  }

  fun terminalExitCode(rInterop: RInterop, args: RObject) {
    TODO()
  }

  fun terminalKill(rInterop: RInterop, args: RObject) {
    val id = args.list.getRObjects(0).rString.getStrings(0)
    val terminal = terminalFromId(rInterop, id) ?: return
    val widget: ShellTerminalWidget = TerminalView.getWidgetByContent(terminal)!! as ShellTerminalWidget
    widget.close()
  }

  fun terminalRunning(rInterop: RInterop, args: RObject): RObject {
    val id = args.list.getRObjects(0).rString.getStrings(0)
    val terminal = terminalFromId(rInterop, id) ?: return false.toRBoolean()
    val widget: ShellTerminalWidget = TerminalView.getWidgetByContent(terminal)!! as ShellTerminalWidget
    return widget.isSessionRunning.toRBoolean()
  }

  fun terminalSend(rInterop: RInterop, args: RObject): RObject {
    val id = args.list.getRObjects(0).rString.getStrings(0)
    val text = args.list.getRObjects(1).rString.getStrings(0)
    val terminal = terminalFromId(rInterop, id) ?: return rError("Unknown terminal identifier '$id'")
    val widget: ShellTerminalWidget = TerminalView.getWidgetByContent(terminal)!! as ShellTerminalWidget
    if (!widget.isSessionRunning) return rError("Terminal is not running and cannot accept input")
    widget.terminal.insertBlankCharacters(1)
    widget.terminalStarter.sendString(text)
    return getRNull()
  }

  fun terminalVisible(rInterop: RInterop): RObject {
    val contentManager = ToolWindowManager.getInstance(rInterop.project).getToolWindow(
      TerminalToolWindowFactory.TOOL_WINDOW_ID)?.contentManager
    val selectedContent = contentManager?.selectedContent ?: return getRNull()
    return selectedContent.hashCode().toString().toRString()
  }

  fun terminalCreate(rInterop: RInterop, args: RObject): RObject {
    //TODO shellType
    val caption = args.list.getRObjects(0).toStringOrNull()
    val show = args.list.getRObjects(1).rBoolean.getBooleans(0)
    val shellType = args.list.getRObjects(2).toStringOrNull()
    val shellWidget = TerminalView.getInstance(rInterop.project).createLocalShellWidget(rInterop.project.basePath, caption)
    return idFromTerminal(rInterop, shellWidget)?.toRString() ?: getRNull()
  }

  fun terminalContext(rInterop: RInterop, args: RObject): RObject {
    val id = args.list.getRObjects(0).rString.getStrings(0)
    return asRStudioTerminal(terminalFromId(rInterop, id) ?: return getRNull(), rInterop)
  }

  fun terminalList(rInterop: RInterop): RObject {
    val contents = listTerminals(rInterop) ?: return getRNull()
    val lst = RObject.RString.newBuilder()
    lst.addAllStrings(contents.map { it.hashCode().toString() })
    return RObject.newBuilder().setRString(lst).build()
  }

  private fun terminalFromId(rInterop: RInterop, id: String): Content? {
    val numId = id.toInt()
    val terminals = listTerminals(rInterop)
    return terminals?.find { it.hashCode() == numId }
  }

  private fun idFromTerminal(rInterop: RInterop, shellWidget: ShellTerminalWidget): String? {
    val toolWindowManager =
      ToolWindowManager.getInstance(rInterop.project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
    val contents = toolWindowManager?.contentManager?.contents ?: return null
    val content = contents.find { TerminalView.getWidgetByContent(it) == shellWidget }
    return content.hashCode().toString()
  }

  private fun listTerminals(rInterop: RInterop): Array<Content>? {
    val contentManager = ToolWindowManager.getInstance(rInterop.project).getToolWindow(
      TerminalToolWindowFactory.TOOL_WINDOW_ID)?.contentManager
    return contentManager?.contents
  }

  private fun clearTypedCommand(widget: ShellTerminalWidget) {
    val textBuffer = widget.terminalTextBuffer
    textBuffer.lock()
    val commandLength = widget.typedShellCommand.length
    val y = max(0, min(widget.terminal.cursorY, textBuffer.height)) - 1
    val x = textBuffer.getLine(y).text.length - commandLength
    val column = max(0, min(widget.terminal.cursorX - 1, textBuffer.width - 1)) - textBuffer.getLine(y).text.length + commandLength
    widget.terminal.cursorForward(commandLength - column - 1)
    val distance = commandLength - column
    val bytes = ByteArray(3 * distance)
    for (i in 0 until 3 * distance step 3) {
      bytes[i] = 27
      bytes[i + 1] = 91
      bytes[i + 2] = 67
    }
    widget.terminalStarter.sendBytes(bytes)
    widget.terminalStarter.sendString(127.toChar().toString().repeat(commandLength))
    textBuffer.deleteCharacters(x, y, commandLength)
    textBuffer.unlock()
  }

  private fun asRStudioTerminal(terminal: Content, rInterop: RInterop): RObject {
    val caption = terminal.tabName
    val handle = terminal.hashCode().toString()
    FUCounterUsageLogger.getInstance()
    val widget: ShellTerminalWidget = TerminalView.getWidgetByContent(terminal)!! as ShellTerminalWidget
    val busy = widget.hasRunningCommands()
    val active = widget.isSessionRunning
    val lines = widget.terminalTextBuffer.screenLinesCount
    val rows = widget.terminalDisplay.rowCount
    val columns = widget.terminalDisplay.columnCount
    val shellPath = TerminalProjectOptionsProvider.getInstance(rInterop.project).shellPath
    val command = ParametersListUtil.parse(shellPath, false, true)
    val shellCommand = if (command.size > 0) command[0] else null
    val shellName = shellCommand?.let { File(shellCommand).name }
    return RObject.newBuilder()
      .setNamedList(RObject.NamedList.newBuilder()
                      .addRObjects(0, RObject.KeyValue.newBuilder().setKey("handle").setValue(handle.toRString()))
                      .addRObjects(1, RObject.KeyValue.newBuilder().setKey("caption").setValue(caption.toRString()))
                      .addRObjects(2, RObject.KeyValue.newBuilder().setKey("title").setValue(getRNull()))
                      .addRObjects(3, RObject.KeyValue.newBuilder().setKey("working_dir").setValue(getRNull()))
                      .addRObjects(4, RObject.KeyValue.newBuilder().setKey("shell").setValue(shellName?.toRString() ?: getRNull()))
                      .addRObjects(5, RObject.KeyValue.newBuilder().setKey("running").setValue(active.toRBoolean()))
                      .addRObjects(6, RObject.KeyValue.newBuilder().setKey("busy").setValue(busy.toRBoolean()))
                      .addRObjects(7, RObject.KeyValue.newBuilder().setKey("exit_code").setValue(getRNull()))
                      .addRObjects(8, RObject.KeyValue.newBuilder().setKey("connection").setValue(getRNull()))
                      .addRObjects(9, RObject.KeyValue.newBuilder().setKey("sequence").setValue(getRNull()))
                      .addRObjects(10, RObject.KeyValue.newBuilder().setKey("lines").setValue(lines.toRInt()))
                      .addRObjects(11, RObject.KeyValue.newBuilder().setKey("cols").setValue(columns.toRInt()))
                      .addRObjects(12, RObject.KeyValue.newBuilder().setKey("rows").setValue(rows.toRInt()))
                      .addRObjects(13, RObject.KeyValue.newBuilder().setKey("pid").setValue(getRNull()))
                      .addRObjects(14, RObject.KeyValue.newBuilder().setKey("full_screen").setValue(getRNull()))
      ).build()
  }
}