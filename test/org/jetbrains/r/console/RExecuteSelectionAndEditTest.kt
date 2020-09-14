/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.r.blockingGetAndDispatchEvents
import org.jetbrains.r.rinterop.RInterop

class RExecuteSelectionAndEditTest : RConsoleBaseTestCase() {
  fun testExecuteSelectionAndEdit() {
    myFixture.configureByText("a.R", """
      a <- 111
      b <- readline()
      c <- 222
      d <- 333
      e <- 444
    """.trimIndent())
    val document = FileDocumentManager.getInstance().getDocument(myFixture.file.virtualFile)!!

    var executedPromise = AsyncPromise<Unit>()
    console.executeActionHandler.addListener(object : RConsoleExecuteActionHandler.Listener {
      override fun onCommandExecuted() {
        executedPromise.setResult(Unit)
      }
    })
    rInterop.addAsyncEventsListener(object : RInterop.AsyncEventsListener {
      override fun onRequestReadLn(prompt: String) {
        invokeLater {
          runWriteAction {
            document.setText("""
              a <- 111
              b <- 123456
              a <- 666
            """.trimIndent())
          }
          FileDocumentManager.getInstance().saveAllDocuments()
          rInterop.replSendReadLn("xyz")
        }
      }
    })

    myFixture.editor.selectionModel.setSelection(0, document.textLength)
    ActionManager.getInstance().getAction("org.jetbrains.r.actions.RunSelection").actionPerformed(createAnActionEvent())
    executedPromise.blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("111", rInterop.executeCode("cat(a)").stdout)
    TestCase.assertEquals("xyz", rInterop.executeCode("cat(b)").stdout)
    TestCase.assertEquals("222", rInterop.executeCode("cat(c)").stdout)
    TestCase.assertEquals("333", rInterop.executeCode("cat(d)").stdout)
    TestCase.assertEquals("444", rInterop.executeCode("cat(e)").stdout)

    executedPromise = AsyncPromise()
    myFixture.editor.selectionModel.setSelection(0, document.textLength)
    ActionManager.getInstance().getAction("org.jetbrains.r.actions.RunSelection").actionPerformed(createAnActionEvent())
    executedPromise.blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("666", rInterop.executeCode("cat(a)").stdout)
    TestCase.assertEquals("123456", rInterop.executeCode("cat(b)").stdout)
  }
}