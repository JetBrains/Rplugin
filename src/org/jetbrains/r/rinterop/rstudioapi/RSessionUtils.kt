package org.jetbrains.r.rinterop.rstudioapi

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.console.RConsoleManager
import org.jetbrains.r.console.RConsoleToolWindowFactory
import org.jetbrains.r.interpreter.RInterpreterManager
import org.jetbrains.r.rinterop.RInterop
import org.jetbrains.r.rinterop.RObject

object RSessionUtils {
  fun sendToConsole(rInterop: RInterop, args: RObject): AsyncPromise<Unit> {
    val code = args.list.getRObjects(0).rString.getStrings(0)
    val execute = args.list.getRObjects(1).rBoolean.getBooleans(0)
    val echo = args.list.getRObjects(2).rBoolean.getBooleans(0)
    val focus = args.list.getRObjects(3).rBoolean.getBooleans(0)
    val call = args.list.getRObjects(4).rString.getStrings(0)

    val promise = AsyncPromise<Unit>()

    val console = getConsoleView(rInterop) ?: return promise.also { it.setResult(Unit) }

    if (echo) {
      if (rInterop.isInSourceFileExecution.get()) {
        console.executeActionHandler.executeLater {
          console.executeText(call)
        }
        return promise.also { it.setResult(Unit) }
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

    val consoleSetText = { text: String ->
      invokeLater {
        runWriteAction {
          val consoleEditor = console.consoleEditor
          consoleEditor.document.setText(text)
          PsiDocumentManager.getInstance(rInterop.project).commitDocument(consoleEditor.document)
          consoleEditor.caretModel.moveToOffset(consoleEditor.document.textLength)
          promise.setResult(Unit)
        }
      }
    }

    if (execute) {
      val text = console.editorDocument.text
      console.executeActionHandler.executeLater {
        console.executeText(code).then {
          consoleSetText(text)
        }
      }
    }
    else {
      console.executeActionHandler.executeLater {
        consoleSetText(code)
      }
    }
    return promise
  }

  fun restartSession(rInterop: RInterop, args: RObject) {
    val command = args.list.getRObjects(0).rString.getStrings(0)
    getConsoleView(rInterop)?.print("\nRestarting R session...\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
    RInterpreterManager.restartInterpreter(rInterop.project, Runnable {
      if (command.isNotBlank()) {
        RConsoleManager.getInstance(rInterop.project).currentConsoleAsync.onSuccess {
          it.executeActionHandler.executeLater {
            it.executeText(command)
          }
        }
      }
    })
  }
}