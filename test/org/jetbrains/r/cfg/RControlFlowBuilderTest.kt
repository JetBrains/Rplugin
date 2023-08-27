/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.cfg

import com.intellij.internal.cfgView.ShowControlFlowHandler
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import org.jetbrains.r.psi.api.RFile
import java.io.File
import java.io.IOException


private const val GENERATE_SVG = false

class RControlFlowTest: RLightCodeInsightFixtureTestCase() {

  override fun getTestDataPath(): String = File(super.getTestDataPath(), "/parser/r/").path

  @Throws(IOException::class)
  private fun doTest() {
    val filename = getTestName(false)
    val fullPath = File(testDataPath, filename).path
    val file = myFixture.configureByFile("$filename.r") as RFile
    val functionExpressions = PsiTreeUtil.collectElementsOfType(file,
                                                                org.jetbrains.r.psi.api.RFunctionExpression::class.java).sortedBy { it.textOffset }

    val builder = StringBuilder()

    var counter = 0
    builder.appendLine("[${counter++}]File:")

    if (GENERATE_SVG) {
      ShowControlFlowHandler.toSvgFile("$fullPath.$counter.svg", file)
    }
    file.controlFlow.instructions.forEach { instruction -> builder.appendln(instruction) }
    for (functionExpression in functionExpressions) {
      builder.appendLine(
        "[${counter++}]Function ${(functionExpression.parent as? org.jetbrains.r.psi.api.RAssignmentStatement)?.name ?: "anon"}")
      functionExpression.controlFlow.instructions.forEach { instruction -> builder.appendln(instruction) }
      if (GENERATE_SVG) {
        ShowControlFlowHandler.toSvgFile("$fullPath.$counter.svg", functionExpression)
      }
    }

    assertSameLinesWithFile("$fullPath.cfg.txt", builder.toString())
  }

  fun testTrue() {
    doTest()
  }

  fun testSlice() {
    doTest()
  }

  fun testAssignment() {
    doTest()
  }

  fun testBinary() {
    doTest()
  }

  fun testBinarySlice() {
    doTest()
  }

  fun testBooleanExpressions() {
    doTest()
  }

  fun testFunctionCall() {
    doTest()
  }

  fun testFunctionDefinition() {
    doTest()
  }

  fun testInPlaceFunctionDefCall() {
    doTest()
  }

  fun testIfStatement() {
    doTest()
  }

  fun testForStatement() {
    doTest()
  }

  fun testRepeatStatement() {
    doTest()
  }

  fun testRepeatBlockStatement() {
    doTest()
  }

  fun testWhileStatement() {
    doTest()
  }

  fun testHelpStatement() {
    doTest()
  }

  fun testSubscription() {
    doTest()
  }

  fun testStatementBreak() {
    doTest()
  }

  fun testStatementBreakAssignment() {
    doTest()
  }

  fun testHelpOnKeyword() {
    doTest()
  }

  fun testBreak() {
    doTest()
  }

  fun testOperator() {
    doTest()
  }

  fun testOperatorsPriority() {
    doTest()
  }

  fun testParenthesized() {
    doTest()
  }

  fun testSemicolon() {
    doTest()
  }

  fun testPrecedence() {
    doTest()
  }

  fun testStringKeywordArg() {
    doTest()
  }

  fun testKeywordArg() {
    doTest()
  }

  fun testBlockAsArgument() {
    doTest()
  }

  fun testFormulae() {
    doTest()
  }

  fun testFunctionBodyAsExpression() {
    doTest()
  }

  fun testFunctionAsCallArgument() {
    doTest()
  }

  fun testIfShortForm() {
    doTest()
  }

  fun testAssignmentInSubscription() {
    doTest()
  }

  fun testEmptyKeywordArgument() {
    doTest()
  }

  fun testIfInKeywordArgument() {
    doTest()
  }

  fun testIfStatementAsArgument() {
    doTest()
  }

  fun testReferenceWithAt() {
    doTest()
  }

  fun testDotAsReference() {
    doTest()
  }

  fun testReprAsKeywordArgument() {
    doTest()
  }

  fun testEmptyExpressionInSubscription() {
    doTest()
  }

  fun testReprInFunctionDef() {
    doTest()
  }

  fun testStatementAsDefaultValue() {
    doTest()
  }

  fun testDoubleDoubleBracket() {
    doTest()
  }

  fun testDotAsFunctionParameter() {
    doTest()
  }

  fun testExpressionAsBinaryExpressionPart() {
    doTest()
  }

  fun testDotsInSublist() {
    doTest()
  }

  fun testNewLineAfterKeywordArgument() {
    doTest()
  }

  fun testDotAsKeywordArgument() {
    doTest()
  }

  fun testReprInSublist() {
    doTest()
  }

  fun testBinaryExpressionNewLine() {
    doTest()
  }

  fun testMemberAccess() {
    doTest()
  }

  fun testChainedCalls() {
    doTest()
  }

  fun testWhileInsideIf() {
    doTest()
  }
}

