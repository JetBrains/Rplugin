package org.jetbrains.r.rinterop

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import junit.framework.TestCase
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.r.blockingGetAndDispatchEvents
import org.jetbrains.r.console.RConsoleBaseTestCase
import org.jetbrains.r.console.jobs.RJobDescriptor
import org.jetbrains.r.console.jobs.RJobRunner
import org.jetbrains.r.console.jobs.RJobRunner.Listener
import org.jetbrains.r.rinterop.rstudioapi.RSessionUtils
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRBoolean
import org.jetbrains.r.rinterop.rstudioapi.RStudioApiUtils.toRString
import org.jetbrains.r.rinterop.rstudioapi.RStudioAPISourceMarkerInspection
import org.jetbrains.r.rinterop.rstudioapi.RStudioAPISourceMarkerInspection.RStudioAPIMarker
import kotlin.streams.toList

class RStudioApiUtilsTest : RConsoleBaseTestCase() {

  override fun setUp() {
    super.setUp()
    rInterop.asyncEventsStartProcessing()
    rInterop.rStudioApiEnabled = true
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

  fun testBasic_insertTextToConsole() {
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
    RSessionUtils.sendToConsole(rInterop, sendToConsoleRequest("s <- 2 + 4", true))
      .blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("> s <- 2 + 4", console.text.trim())
    TestCase.assertEquals("", console.consoleEditor.document.text)
  }

  fun testBasic_sendToConsoleWithoutExecution() {
    RSessionUtils.sendToConsole(rInterop, sendToConsoleRequest("s <- 2 + 4", false))
      .blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals("", console.text.trim())
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

  fun testBasic_setSelectionRangesWithOutOfBounds() {
    myFixture.configureByText("foo.R", """
      a <- 3
      # b
    """.trimIndent())
    console.executeText("""
      |cntx <- rstudioapi::getSourceEditorContext()
      |rstudioapi::setSelectionRanges(c(20, 4, 2, 1), cntx${"$"}id)
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(listOf("# b"),
                          myFixture.editor.caretModel.allCarets.filter { it.hasSelection() }.map { it.selectedText })
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
    val promise = createJobDonePromise()
    console.executeText("""
      |c <- 5
      |rstudioapi::jobRunScript("rstudioapi/testJobs.R", importEnv = TRUE, exportEnv = "R_GlobalEnv")
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(false, promise.blockingGet(DEFAULT_TIMEOUT))
    TestCase.assertEquals("[1] 3", rInterop.executeCode("a").stdout.trim())
    TestCase.assertEquals("[1] 8", rInterop.executeCode("b").stdout.trim())
  }

  fun testBasic_jobRunScriptWithoutExport() {
    rInterop.setWorkingDir(myFixture.testDataPath)
    val promise = createJobDonePromise()
    console.executeText("""
      |rstudioapi::jobRunScript("rstudioapi/testJobsWithoutExport.R")
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(false, promise.blockingGet(DEFAULT_TIMEOUT))
    TestCase.assertEquals("Error: object 'f' not found", rInterop.executeCode("f").stderr.trim())
  }

  fun testBasic_jobRunScriptWithoutImport() {
    rInterop.setWorkingDir(myFixture.testDataPath)
    val promise = createJobDonePromise()
    console.executeText("""
      |c <- 5
      |rstudioapi::jobRunScript("rstudioapi/testJobsWithoutImport.R", exportEnv = "R_GlobalEnv")
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    TestCase.assertEquals(true, promise.blockingGet(DEFAULT_TIMEOUT))
  }

  fun testBasic_sourceMarkersErrorFocus() {
    val offset = sourceMarkersTestHelper("error")
    TestCase.assertEquals(7, offset)
  }

  fun testBasic_sourceMarkersFirstFocus() {
    val offset = sourceMarkersTestHelper("first")
    TestCase.assertEquals(2, offset)
  }

  fun testBasic_sourceMarkersNoFocus() {
    val offset = sourceMarkersTestHelper("none")
    TestCase.assertEquals(null, offset)
  }

  private fun sourceMarkersTestHelper(type: String): Int? {
    rInterop.setWorkingDir(myFixture.testDataPath)
    val path = myFixture.testDataPath + "/rstudioapi/testJobsWithoutImport.R"
    console.executeText("""
      |markers <- list(list(type = "usage", file = "$path", line = 1, column = 3, message = "msg1"), list(type = "error", file = "$path", line = 2, column = 1, message = "msg2"))
      |print(markers)
      |b <- rstudioapi::sourceMarkers("test", markers, NULL, "$type")
      |c <- rstudioapi::sourceMarkers("test", markers, NULL, "$type")
    """.trimMargin()).blockingGetAndDispatchEvents(DEFAULT_TIMEOUT)
    val markers = myFixture.project.getUserData(RStudioAPISourceMarkerInspection.SOURCE_MARKERS_KEY)
    TestCase.assertNotNull(markers)
    TestCase.assertEquals(1, markers!!.size)
    TestCase.assertEquals(1, markers[path]?.size)
    TestCase.assertEquals(listOf(RStudioAPIMarker(type = ProblemHighlightType.WEAK_WARNING, message = "msg1", line = 1, col = 3),
                                 RStudioAPIMarker(type = ProblemHighlightType.GENERIC_ERROR, message = "msg2", line = 2, col = 1)),
                          markers[path]!!["test"])
    val editor = FileEditorManager.getInstance(rInterop.project).selectedEditor ?: return null
    val editors = EditorFactory.getInstance().editors(FileDocumentManager.getInstance().getDocument(editor.file!!)!!).toList()
    TestCase.assertEquals(1, editors.size)
    return editors.first().caretModel.offset
  }

  private fun createJobDonePromise(): Promise<Boolean> {
    val promise = AsyncPromise<Boolean>()
    val lambda = object : Listener {
      override fun onJobDescriptionCreated(rJobDescriptor: RJobDescriptor) {
        rJobDescriptor.onProcessTerminated { promise.setResult(rJobDescriptor.processFailed) }
      }
    }
    RJobRunner.getInstance(project).eventDispatcher.addListener(lambda)
    return promise
  }

  private fun sendToConsoleRequest(code: String, execute: Boolean): RObject {
    return RObject.newBuilder().setList(RObject.List.newBuilder()
                                          .addRObjects(0, code.toRString())
                                          .addRObjects(1, execute.toRBoolean())
                                          .addRObjects(2, false.toRBoolean())
                                          .addRObjects(3, false.toRBoolean())
                                          .addRObjects(4, "".toRString())
    ).build()
  }
}