// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RFileType
import org.jetbrains.r.RUsefulTestCase

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

abstract class RInspectionTest : RUsefulTestCase() {

  internal abstract val inspection: Class<out RInspection>

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/inspections/" + javaClass.simpleName.replace("Test", "")
  }

  private fun doTestBase(filename: String,
                         checkWarnings: Boolean = false,
                         checkInfos: Boolean = false,
                         checkWeakWarnings: Boolean = false): CodeInsightTestFixture {
    myFixture.configureByFile(filename)
    myFixture.enableInspections(inspection)
    myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings, filename)

    return myFixture
  }

  protected fun doTest(filename: String = getTestName(false) + RFileType.DOT_R_EXTENSION): CodeInsightTestFixture {
    return doTestBase(filename, checkWarnings = true)
  }

  protected fun doWeakTest(filename: String = getTestName(false) + RFileType.DOT_R_EXTENSION): CodeInsightTestFixture {
    return doTestBase(filename, checkWeakWarnings = true)
  }

  override fun configureFixture(myFixture: CodeInsightTestFixture) {
    super.configureFixture(myFixture)
    myFixture.enableInspections(inspection)
  }

  protected fun readTestDataFile(): String {
    val testDataPath = Paths.get(testDataPath, getTestName(false) + RFileType.DOT_R_EXTENSION)
    return readFileAsString(testDataPath)
  }

  protected fun readFileAsString(testDataPath: Path): String {
    try {
      return String(Files.readAllBytes(testDataPath))
    }
    catch (e: IOException) {
      throw IllegalArgumentException("could not read test resource file", e)
    }

  }

  protected fun assertUnused(@Language("R") expr: String) {
    val fixture = doExprTest(expr)
    val highlightInfo = fixture.doHighlighting()

    // make sure that they show up as unused
    assertNotEmpty(highlightInfo)
    assertEquals(highlightInfo[0].type.attributesKey, CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES)
  }

  protected fun assertAllUsed(expr: String) {
    // todo needed? doExpr will fail if there's a warning!!

    val fixture = doExprTest(expr)
    val highlightInfo = fixture.doHighlighting()

    assertTrue(highlightInfo.stream().noneMatch { it.severity !== HighlightSeverity.INFORMATION })
  }

  @JvmOverloads
  protected fun doReplacementTest(text: String, expected: String? = null) {
    myFixture.configureByText("a.R", text)
    myFixture.enableInspections(inspection)
    val highlightingInfo = myFixture.doHighlighting(HighlightSeverity.WEAK_WARNING)

    if (highlightingInfo.isEmpty()) {
      if (expected != null) {
        fail("Inspection did not highlight the code")
      }
    }
    else {
      val highlighting = highlightingInfo[0]
      if (expected == null) {
        fail("Inspection highlighted the code: \${highlighting.text}")
      }
      val action = highlighting.quickFixActionMarkers!![0].first.action

      ApplicationManager.getApplication().runWriteAction {
        CommandProcessor.getInstance().executeCommand(project, { action.invoke(project, myFixture.editor, myFixture.file) },
                                                      "", null
        )
      }
      assertEquals(expected, myFixture.file.text)
    }
  }
}
