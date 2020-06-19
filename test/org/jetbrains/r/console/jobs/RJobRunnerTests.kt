/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.console.jobs

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.r.console.RConsoleBaseTestCase

class RJobRunnerTests : RConsoleBaseTestCase() {

  private val output = StringBuilder()
  private val stderr = StringBuilder()
  private var rJobProgressProvider = RJobProgressProvider()

  override fun setUp() {
    super.setUp()
    output.setLength(0)
    stderr.setLength(0)
    rJobProgressProvider = RJobProgressProvider()
  }

  fun testRun() {
    val text = """
      cat("Hello world")
      cat("Error", file=stderr())
  """
    val task = RJobTask(createScript(text), myFixture.testDataPath, false, ExportGlobalEnvPolicy.DO_NO_EXPORT)
    val processHandler = createProcessHandler(task)
    waitForTermination(processHandler)
    assertEquals("Hello world", output.toString())
    assertEquals("Error", stderr.toString())
    assertTrue(rJobProgressProvider.current == rJobProgressProvider.total)
  }

  fun testRunImportGlobal() {
    rInterop.executeCode("x <- 1; y <- 2")
    val text = """
      cat(x + y)
  """
    val task = RJobTask(createScript(text), myFixture.testDataPath, true, ExportGlobalEnvPolicy.DO_NO_EXPORT)
    val processHandler = createProcessHandler(task)
    waitForTermination(processHandler)
    assertEquals("3", output.toString())
    assertEquals("", stderr.toString())
    assertTrue(rJobProgressProvider.current == rJobProgressProvider.total)
  }

  fun testRunExportGlobal() {
    val text = """
      x <- 1
      y <- 2
    """
    val task = RJobTask(createScript(text), myFixture.testDataPath, false, ExportGlobalEnvPolicy.EXPORT_TO_GLOBAL_ENV)
    val processHandler = createProcessHandler(task)
    waitForTermination(processHandler)
    assertEquals("", output.toString())
    assertEquals("", stderr.toString())
    val (stdout, stderr, _) = rInterop.executeCode("cat(x + y)")
    assertEquals("3", stdout)
    assertTrue(stderr.isEmpty())
    assertTrue(rJobProgressProvider.current == rJobProgressProvider.total)
  }

  fun testRunExportVariable() {
    val text = """
      x <- 1
      y <- 2
    """
    val script = createScript(text)
    val task = RJobTask(script, myFixture.testDataPath, false, ExportGlobalEnvPolicy.EXPORT_TO_VARIABLE)
    val processHandler = createProcessHandler(task)
    waitForTermination(processHandler)
    assertEquals("", output.toString())
    assertEquals("", stderr.toString())
    val variableName = script.nameWithoutExtension + "_results"
    val (stdout, stderr, _) = rInterop.executeCode("cat($variableName${'$'}x + $variableName${'$'}y)")
    assertEquals("3", stdout)
    assertTrue(stderr.isEmpty())
    assertTrue(rJobProgressProvider.current == rJobProgressProvider.total)
  }

  private fun createProcessHandler(task: RJobTask): ProcessHandler {
    val processHandler = RJobRunner.getInstance(project).run(task).blockingGet(Int.MAX_VALUE)!!
    processHandler.installListener()
    processHandler.startNotify()
    return processHandler
  }

  private fun waitForTermination(processHandler: ProcessHandler) {
    var timeout = 0;
    while (!processHandler.isProcessTerminated && timeout < 500) {
      Thread.sleep(10)
      timeout++
    }
    assertTrue(processHandler.isProcessTerminated)
  }

  private fun ProcessHandler.installListener() {
    addProcessListener(object : ProcessAdapter() {
      val filter = RSourceProgressInputFilter { rJobProgressProvider.onProgressAvailable(it) }
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val consoleViewType = ConsoleViewContentType.getConsoleViewType(outputType)
        val filtered = filter.applyFilter(event.text, consoleViewType)
        val text = if (filtered == null) event.text else filtered[0].first
        if (consoleViewType == ConsoleViewContentType.NORMAL_OUTPUT) {
          output.append(text)
        }
        if (consoleViewType == ConsoleViewContentType.ERROR_OUTPUT) {
          stderr.append(text)
        }
      }
    })
  }

  private fun createScript(text: String): VirtualFile {
    val tempFile = FileUtil.createTempFile("test", ".R", true)
    tempFile.writeText(text)
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile)!!
  }
}