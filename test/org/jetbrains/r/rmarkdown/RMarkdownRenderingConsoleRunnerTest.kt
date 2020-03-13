/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.rmarkdown

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import junit.framework.TestCase
import org.jetbrains.r.interpreter.RInterpreterBaseTestCase
import org.jetbrains.r.rendering.settings.RMarkdownSettings

class RMarkdownRenderingConsoleRunnerTest : RInterpreterBaseTestCase() {
  fun testOutputTypes() {
    doTestOutputType("html", "html_document")
    doTestOutputType("pdf", "pdf_document")
  }

  private fun doTestOutputType(extension: String, outputType: String) {
    val rmdFile = FileUtil.createTempFile("document", ".Rmd", true)
    try {
      rmdFile.writeText("""
      ---
      title: T
      output: $outputType
      ---
      
      Lorem ipsum dolor sit amet,
      ```{r}
      1 + 2 + 3
      ```
      consectetur adipiscing elit,
      sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
    """.trimIndent())
      val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(rmdFile)!!
      RMarkdownRenderingConsoleRunner(project).render(project, virtualFile).blockingGet(DEFAULT_TIMEOUT)
      val outputFile = RMarkdownSettings.getInstance(project).state.getProfileLastOutput(rmdFile.absolutePath)
      TestCase.assertEquals(extension, FileUtilRt.getExtension(outputFile))
    } finally {
      rmdFile.delete()
    }
  }
}