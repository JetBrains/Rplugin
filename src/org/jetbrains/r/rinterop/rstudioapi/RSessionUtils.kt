package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject

fun sendToConsole(rInterop: RInterop, args: RObject) {
  val code = args.list.getRObjects(0).rString.getStrings(0)
  val execute = args.list.getRObjects(1).rboolean.getBooleans(0)
  val echo = args.list.getRObjects(2).rboolean.getBooleans(0)
  val focus = args.list.getRObjects(3).rboolean.getBooleans(0)
  val call = args.list.getRObjects(4).rString.getStrings(0)

  val console = getConsoleView(rInterop) ?: return

  if (echo) {
    if (rInterop.isInSourceFileExecution.get()) {
      console.executeText(call)
    }
  }

  if (focus) {
    val toolWindow = RConsoleToolWindowFactory.getRConsoleToolWindows(console.project)
    if (toolWindow != null) {
      RConsoleToolWindowFactory.getConsoleContent(console)?.let { content ->
        toolWindow.activate {
          toolWindow.contentManager.setSelectedContent(content)
        }
      }
    }
  }

  if (execute) {
    val text = console.editorDocument.text
    console.executeText(code).then {
      invokeLater {
        val consoleEditor = console.consoleEditor
        runWriteAction {
          consoleEditor.document.setText(text)
          PsiDocumentManager.getInstance(rInterop.project).commitDocument(consoleEditor.document)
        }
        consoleEditor.caretModel.moveToOffset(consoleEditor.document.textLength)
      }
    }
  }
  else {
    invokeLater {
      val consoleEditor = console.consoleEditor
      runWriteAction {
        consoleEditor.document.setText(code)
        PsiDocumentManager.getInstance(rInterop.project).commitDocument(consoleEditor.document)
      }
      consoleEditor.caretModel.moveToOffset(consoleEditor.document.textLength)
    }
  }
}

fun restartSession(rInterop: RInterop, args: RObject) {
  // TODO
  val command = args.list.getRObjects(0).rString.getStrings(0)
  getConsoleView(rInterop)?.print("\nRestarting R session...\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
  RInterpreterManager.restartInterpreter(rInterop.project).then {
    getConsoleView(rInterop)?.executeText(command)
  }
}