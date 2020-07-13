/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.execution.console.ConsoleHistoryController
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiDocumentManager
import junit.framework.TestCase
import org.jetbrains.r.blockingGetAndDispatchEvents


class RConsoleHistoryTest : RConsoleBaseTestCase() {
  fun testHistoryNavigation() {
    val text1 = "x <- 10\ny <- 20"
    console.executeText(text1).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)

    val text2 = "z <- 30"
    val editor = console.consoleEditor
    val document = editor.document
    runWriteAction {
      document.setText(text2)
      PsiDocumentManager.getInstance(project).commitDocument(document)
    }
    val historyController: ConsoleHistoryController? = ConsoleHistoryController.getController(console)
    check(historyController != null)
    consoleOlder(historyController)
    TestCase.assertEquals(text1, document.text)
    TestCase.assertEquals(editor.caretModel.offset, document.textLength)
    consoleNewer(historyController)
    TestCase.assertEquals(text2, document.text)
    TestCase.assertEquals(editor.caretModel.offset, document.textLength)
  }

  private fun consoleOlder(historyController: ConsoleHistoryController) {
    historyController.getHistoryNext().actionPerformed(AnActionEvent.createFromDataContext("test", null, DataContext.EMPTY_CONTEXT))
  }

  private fun consoleNewer(historyController: ConsoleHistoryController) {
    historyController.getHistoryPrev().actionPerformed(AnActionEvent.createFromDataContext("test", null, DataContext.EMPTY_CONTEXT))
  }
}
