package org.jetbrains.r.rinterop

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFileManager
import junit.framework.TestCase
import org.jetbrains.r.blockingGetAndDispatchEvents
import org.jetbrains.r.console.RConsoleBaseTestCase
import java.nio.file.Paths
import kotlin.streams.toList

class RStudioApiUtilsTest : RConsoleBaseTestCase() {

  override fun setUp() {
    super.setUp()
    rInterop.asyncEventsStartProcessing()
  }

  fun testBasic_getSourceEditorContext() {
    myFixture.configureByText("foo.R", """
      a <- 3<caret>
      # b
    """.trimIndent())
    myFixture.editor.caretModel.addCaret(VisualPosition(0, 0), true)
    myFixture.editor.selectionModel.setSelection(0, 1)
    console.executeText("""
      |cntx <- rstudioapi::getSourceEditorContext()
      |path <- cntx${"$"}path
      |content <- cntx${"$"}contents
      |selectionSize <- length(cntx${"$"}selection)
      |selectionText1 <- cntx${"$"}selection[[1]]${"$"}text
      |selectionRange1 <- unlist(cntx${"$"}selection[[1]]${"$"}range)
      |names(selectionRange1) <- NULL
      |selectionText2 <- cntx${"$"}selection[[2]]${"$"}text
      |selectionRange2 <- unlist(cntx${"$"}selection[[2]]${"$"}range)
      |names(selectionRange2) <- NULL
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("[1] \"${myFixture.file.virtualFile.path}\"", (console.consoleRuntimeInfo.variables["path"] as RValueSimple).text)
    TestCase.assertEquals("[1] \"a <- 3\" \"# b\"", (console.consoleRuntimeInfo.variables["content"] as RValueSimple).text)
    TestCase.assertEquals("[1] 2", (console.consoleRuntimeInfo.variables["selectionSize"] as RValueSimple).text)
    TestCase.assertEquals(
      listOf("[1] \"\"", "[1] \"a\""),
      listOf(
        (console.consoleRuntimeInfo.variables["selectionText1"] as RValueSimple).text,
        (console.consoleRuntimeInfo.variables["selectionText2"] as RValueSimple).text
      ).sorted())
    TestCase.assertEquals(
      listOf("[1] 1 1 1 2", "[1] 1 7 1 7"),
      listOf(
        (console.consoleRuntimeInfo.variables["selectionRange1"] as RValueSimple).text,
        (console.consoleRuntimeInfo.variables["selectionRange2"] as RValueSimple).text
      ).sorted()
    )
  }

  fun testBasic_insertText() {
    myFixture.configureByText("foo.R", """
      a <- 3<caret>
      # b
    """.trimIndent())
    console.executeText("""
      |cntx <- rstudioapi::getSourceEditorContext()
      |rstudioapi::insertText(list(c(1,1,1,1), c(2,1,2,1)), c("# line1\n", "# line2\n"), cntx${"$"}id)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("""
      # line1
      a <- 3
      # line2
      # b
    """.trimIndent(), myFixture.editor.document.text)
  }

  fun testBasic_insertTextInf() {
    myFixture.configureByText("foo.R", """
      a <- 3<caret>
      # b
    """.trimIndent())
    console.executeText("""
      |cntx <- rstudioapi::getSourceEditorContext()
      |rstudioapi::insertText(c(1,1,Inf,1), "# line1\n", cntx${"$"}id)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("""
      # line1
      
    """.trimIndent(), myFixture.editor.document.text)
    console.executeText("""
      |rstudioapi::insertText(c(Inf,1,Inf,1), "# line1\n", cntx${"$"}id)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("""
      # line1
      # line1
      
    """.trimIndent(), myFixture.editor.document.text)
  }

  fun testBasic_insertTextWithId() {
    myFixture.configureByText("foo.R", """
      a <- 3<caret>
      # b
    """.trimIndent())
    console.executeText("""
      |cntx <- rstudioapi::getSourceEditorContext()
      |rstudioapi::insertText(c(1,1,5,Inf), "# line1", cntx${"$"}id)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("# line1", myFixture.editor.document.text)
  }

  fun testBasic_insertTextWithReplacement() {
    myFixture.configureByText("foo.R", """
      a <- 3<caret>
      # b
    """.trimIndent())
    console.executeText("""
      |cntx <- rstudioapi::getSourceEditorContext()
      |rstudioapi::insertText(list(c(1,1,1,Inf), c(2,1,2,1)), c("# line1", "# line2\n"), cntx${"$"}id)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("""
      # line1
      # line2
      # b
    """.trimIndent(), myFixture.editor.document.text)
  }

  fun testBasic_insertTextWithReplacement2() {
    myFixture.configureByText("foo.R", """
      a <- 3<caret>
      # b
    """.trimIndent())
    console.executeText("""
      |cntx <- rstudioapi::getSourceEditorContext()
      |rstudioapi::insertText(list(c(1,1,1,2), c(2,1,2,1)), c("line1", "# line2\n"), cntx${"$"}id)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("""
      line1 <- 3
      # line2
      # b
    """.trimIndent(), myFixture.editor.document.text)
  }

  fun testBasic_InsertTextToConsole() {
    console.executeText("""
      |cntx <- rstudioapi::getConsoleEditorContext()
      |rstudioapi::insertText(c(1,1,1,1), "a <- 3", cntx${"$"}id)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("a <- 3", console.consoleEditor.document.text)
  }

  fun testBasic_consoleEditorContext() {
    console.consoleEditor.caretModel.addCaret(VisualPosition(0, 0))
    console.executeText("""
      |cntx <- rstudioapi::getConsoleEditorContext()
      |path <- cntx${"$"}path
      |content <- cntx${"$"}contents
      |selectionSize <- length(cntx${"$"}selection)
      |selectionText <- cntx${"$"}selection[[1]]${"$"}text
      |selectionRange <- unlist(cntx${"$"}selection[[1]]${"$"}range)
      |names(selectionRange) <- NULL
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("[1] \"\"", (console.consoleRuntimeInfo.variables["path"] as RValueSimple).text)
    TestCase.assertEquals("[1] \"\"", (console.consoleRuntimeInfo.variables["content"] as RValueSimple).text)
    TestCase.assertEquals("[1] 1", (console.consoleRuntimeInfo.variables["selectionSize"] as RValueSimple).text)
    TestCase.assertEquals("[1] \"\"", (console.consoleRuntimeInfo.variables["selectionText"] as RValueSimple).text)
    TestCase.assertEquals("[1] 1 1 1 1", (console.consoleRuntimeInfo.variables["selectionRange"] as RValueSimple).text)
  }

  fun testBasic_sendToConsoleWithExecution() {
    console.executeText("""
      |rstudioapi::sendToConsole("s <- 2 + 4")
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    val commands = console.text.split("\n")
    TestCase.assertEquals(3, commands.size)
    TestCase.assertEquals("> s <- 2 + 4", commands[1])
  }

  fun testBasic_sendToConsoleWithoutExecution() {
    console.executeText("""
      |rstudioapi::sendToConsole("s <- 2 + 4", FALSE)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    val commands = console.text.split("\n")
    TestCase.assertEquals(2, commands.size)
    TestCase.assertEquals("s <- 2 + 4", console.consoleEditor.document.text)
  }

  fun testBasic_sendToConsoleWithExecutionNoEcho() {
    console.executeText("""
      |rstudioapi::sendToConsole("s <- 2 + 4", TRUE, FALSE)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    val commands = console.text.split("\n")
    TestCase.assertEquals(3, commands.size)
    TestCase.assertEquals("> s <- 2 + 4", commands[1])
  }

  fun testBasic_sendToConsoleWithoutExecutionNoEcho() {
    console.executeText("""
      |rstudioapi::sendToConsole("s <- 2 + 4", FALSE, FALSE)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    val commands = console.text.split("\n")
    TestCase.assertEquals(2, commands.size)
    TestCase.assertEquals("s <- 2 + 4", console.consoleEditor.document.text)
  }

  fun testBasic_getActiveProject() {
    console.executeText("""
      |a <- rstudioapi::getActiveProject()
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("[1] \"${myFixture.project.basePath}\"", (console.consoleRuntimeInfo.variables["a"] as RValueSimple).text)
  }

  fun testBasic_navigateToFile() {
    rInterop.setWorkingDir(myFixture.testDataPath)
    console.executeText("""
      |ss <- rstudioapi::navigateToFile("resolve/foo2.R", 1, 3)
      |rstudioapi::navigateToFile("resolve/foo2.R", 1, 4)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("NULL", (console.consoleRuntimeInfo.variables["ss"] as RValueSimple).text)
    val editor = FileEditorManager.getInstance(rInterop.project).selectedEditor!!
    TestCase.assertEquals("foo2.R", editor.file!!.name)
    val editors = EditorFactory.getInstance().editors(FileDocumentManager.getInstance().getDocument(editor.file!!)!!).toList()
    TestCase.assertEquals(1, editors.size)
    val offset = editors.first().caretModel.offset
    TestCase.assertEquals(3, offset)
  }

  fun testBasic_setSelectionRanges() {
    myFixture.configureByText("foo.R", """
      a <- 3
      # b
    """.trimIndent())
    console.executeText("""
      |cntx <- rstudioapi::getSourceEditorContext()
      |rstudioapi::setSelectionRanges(list(c(1,1,1,2), c(1,3,1,5)), cntx${"$"}id)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(listOf(0 to 1, 2 to 4),
                          myFixture.editor.caretModel.allCarets.filter { it.hasSelection() }.map { it.selectionStart to it.selectionEnd })
  }

  fun testBasic_setSelectionRangesWithRepeatedCalls() {
    myFixture.configureByText("foo.R", """
      a <- 3
      # b
    """.trimIndent())
    console.executeText("""
      |cntx <- rstudioapi::getSourceEditorContext()
      |rstudioapi::setSelectionRanges(c(1,1,1,3), cntx${"$"}id)
      |rstudioapi::setSelectionRanges(list(c(1,1,1,2), c(1,3,1,5)), cntx${"$"}id)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(listOf(0 to 1, 2 to 4),
                          myFixture.editor.caretModel.allCarets.filter { it.hasSelection() }.map { it.selectionStart to it.selectionEnd })
  }

  fun testBasic_jobRunScript() {
    rInterop.setWorkingDir(myFixture.testDataPath)
    console.executeText("""
      |c <- 5
      |rstudioapi::jobRunScript("rstudioapi/testJobs.R", importEnv = TRUE, exportEnv = "R_GlobalEnv")
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("[1] 3", (console.consoleRuntimeInfo.variables["a"] as RValueSimple).text)
    TestCase.assertEquals("[1] 8", (console.consoleRuntimeInfo.variables["b"] as RValueSimple).text)
  }

  fun testBasic_jobRunScriptWithoutExport() {
    rInterop.setWorkingDir(myFixture.testDataPath)
    console.executeText("""
      |c <- 5
      |rstudioapi::jobRunScript("rstudioapi/testJobs.R")
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(null, console.consoleRuntimeInfo.variables["a"])
  }

  fun testBasic_jobRunScriptWithoutImport() {
    rInterop.setWorkingDir(myFixture.testDataPath)
    console.executeText("""
      |c <- 5
      |id <- rstudioapi::jobRunScript("rstudioapi/testJobs2.R", exportEnv = "R_GlobalEnv")
      |rstudioapi::jobRunScript("rstudioapi/testJobs.R", exportEnv = "R_GlobalEnv")
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(null, console.consoleRuntimeInfo.variables["a"])
    TestCase.assertEquals(null, console.consoleRuntimeInfo.variables["b"])
    TestCase.assertEquals("[1] 3", (console.consoleRuntimeInfo.variables["e"] as RValueSimple).text)
    TestCase.assertEquals("[1] 4", (console.consoleRuntimeInfo.variables["d"] as RValueSimple).text)
  }

  fun testBasic_sourceMarkersErrorFocus() {
    rInterop.setWorkingDir(myFixture.testDataPath)
    val path = myFixture.testDataPath + "/rstudioapi/testJobs2.R"
    val file = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(path))!!
    FileEditorManager.getInstance(rInterop.project).openFile(file, true)
    val editor = EditorFactory.getInstance().editors(FileDocumentManager.getInstance().getDocument(file)!!).toList().first()
    console.executeText("""
      |markers <- list(list(type = "usage", file = "$path", line = 1, column = 3, message = "msg1"), list(type = "error", file = "$path", line = 2, column = 1, message = "msg2"))
      |b <- rstudioapi::sourceMarkers("test", markers, NULL, "error")
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(2,
                          editor.markupModel.allHighlighters.toList().filter { it.textAttributesKey?.externalName == "test" }.size)
    TestCase.assertEquals(11, editor.caretModel.primaryCaret.offset)
  }

  fun testBasic_sourceMarkersFirstFocus() {
    rInterop.setWorkingDir(myFixture.testDataPath)
    val path = myFixture.testDataPath + "/rstudioapi/testJobs2.R"
    val file = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(path))!!
    FileEditorManager.getInstance(rInterop.project).openFile(file, true)
    val editor = EditorFactory.getInstance().editors(FileDocumentManager.getInstance().getDocument(file)!!).toList().first()
    console.executeText("""
      |markers <- list(list(type = "usage", file = "$path", line = 1, column = 3, message = "msg1"), list(type = "error", file = "$path", line = 2, column = 1, message = "msg2"))
      |b <- rstudioapi::sourceMarkers("test", markers, NULL, "first")
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(2,
                          editor.markupModel.allHighlighters.toList().filter { it.textAttributesKey?.externalName == "test" }.size)
    TestCase.assertEquals(2, editor.caretModel.primaryCaret.offset)
  }

  fun testBasic_sourceMarkersNoFocus() {
    rInterop.setWorkingDir(myFixture.testDataPath)
    val path = myFixture.testDataPath + "/rstudioapi/testJobs2.R"
    val file = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(path))!!
    FileEditorManager.getInstance(rInterop.project).openFile(file, true)
    val editor = EditorFactory.getInstance().editors(FileDocumentManager.getInstance().getDocument(file)!!).toList().first()
    console.executeText("""
      |markers <- list(list(type = "usage", file = "$path", line = 1, column = 3, message = "msg1"), list(type = "error", file = "$path", line = 2, column = 1, message = "msg2"))
      |b <- rstudioapi::sourceMarkers("test", markers, NULL)
      |b <- rstudioapi::sourceMarkers("test", markers, NULL)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(2,
                          editor.markupModel.allHighlighters.toList().filter { it.textAttributesKey?.externalName == "test" }.size)
    TestCase.assertEquals(0, editor.caretModel.primaryCaret.offset)
  }
}