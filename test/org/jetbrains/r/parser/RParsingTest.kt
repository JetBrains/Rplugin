// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.r.parser

import com.intellij.testFramework.ParsingTestCase
import com.intellij.testFramework.TestDataPath
import org.jetbrains.r.RUsefulTestCase
import java.io.File

@TestDataPath("/testData/parser/r")
class RParsingTest : RUsefulTestCase() {

  override fun getTestDataPath(): String = File(super.getTestDataPath(), "/parser/r/").path

  fun testIdentifierLexing() = doTest()

  fun testTrue() = doTest()

  fun testSlice() = doTest()

  fun testAssignment() = doTest()

  fun testBinary() = doTest()

  fun testBinarySlice() = doTest()

  fun testBooleanExpressions() = doTest()

  fun testFunctionCall() = doTest()

  fun testFunctionDefinition() = doTest()

  /**
   * note: it may look wrong to parse the tailing () into a call on the block, but R does so as well.
   * Actually even if this defines syntactically correct R code, R can not evaluate it, and errors with an
   * `Error in foo() : attempt to apply non-function`
   * To call an anonymously defined function in place additional brackets are requires:
   * `(function(){1})()`
   */
  fun testInPlaceFunctionDefCall() = doTest()

  fun testIfStatement() = doTest()

  fun testForStatement() = doTest()

  fun testRepeatStatement() = doTest()

  fun testRepeatBlockStatement() = doTest()

  fun testWhileStatement() = doTest()

  fun testHelpStatement() = doTest()

  fun testSubscription() = doTest()

  fun testStatementBreak() = doTest()

  fun testStatementBreakAssignment() = doTest()

  fun testHelpOnKeyword() = doTest()

  fun testBreak() = doTest()

  fun testOperator() = doTest()

  fun testOperatorsPriority() = doTest()

  fun testParenthesized() = doTest()

  fun testSemicolon() = doTest()

  fun testPrecedence() = doTest()

  fun testStringKeywordArg() = doTest()

  fun testKeywordArg() = doTest()

  fun testBlockAsArgument() = doTest()

  fun testFormulae() = doTest()

  fun testFunctionBodyAsExpression() = doTest()

  fun testFunctionAsCallArgument() = doTest()

  fun testIfShortForm() = doTest()

  fun testAssignmentInSubscription() = doTest()

  fun testEmptyKeywordArgument() = doTest()

  fun testIfInKeywordArgument() = doTest()

  fun testIfStatementAsArgument() = doTest()

  fun testReferenceWithAt() = doTest()

  fun testDotAsReference() = doTest()

  fun testReprAsKeywordArgument() = doTest()

  fun testEmptyExpressionInSubscription() = doTest()

  fun testReprInFunctionDef() = doTest()

  fun testStatementAsDefaultValue() = doTest()

  fun testDoubleDoubleBracket() = doTest()

  fun testDotAsFunctionParameter() = doTest()

  fun testExpressionAsBinaryExpressionPart() = doTest()

  fun testDotsInSublist() = doTest()

  fun testNewLineAfterKeywordArgument() = doTest()

  fun testDotAsKeywordArgument() = doTest()

  fun testReprInSublist() = doTest()

  fun testBinaryExpressionNewLine() = doTest()

  fun testMemberAccess() = doTest()

  fun testChainedCalls() = doTest()

  fun testWhileInsideIf() = doTest()

  fun testDoubleStarts() = doTest()

  fun testEmptyArguments() = doTest()

  fun testRawString() = doTest()

  fun testCorrectNewLines() = doTest()

  fun testLongBlockStatement() = doTest()

  //----------Syntax error tests-----------
  fun testErrorFor() = doTest()

  fun testErrorWhile() = doTest()

  fun testErrorRepeat() = doTest()

  fun testErrorIf() = doTest()

  // grammar-kit version should be newer 2020 May 9th
  fun testErrorMethod1() = doTest()

  fun testErrorMethod2() = doTest()

  // grammar-kit version should be newer 2020 May 9th
  fun testErrorMethod3() = doTest()

  // grammar-kit version should be newer 2020 May 9th
  fun testErrorMethod4() = doTest()

  fun testErrorMethod5() = doTest()

  fun testErrorMethod6() = doTest()

  // grammar-kit version should be newer 2020 May 9th
  fun testErrorMethod7() = doTest()

  fun testErrorCall1() = doTest()

  fun testErrorCall2() = doTest()

  fun testErrorCall3() = doTest()

  fun testArgumentsWithoutComma() = doTest()

  fun testUnclosedRawString() = doTest()

  fun testUnclosedSingleQuotedString() = doTest()

  fun testUnclosedDoubleQuotedString() = doTest()

  fun testIncorrectNewLines() = doTest()

  fun testForwardPipe() = doTest()

  fun testShorthandFunction() = doTest()

  //---------------------------------------
  private fun doTest() {
    val testName = getTestName(false)
    myFixture.configureByFile("$testName.r")

    ParsingTestCase.doCheckResult(
      testDataPath,
      myFixture.file,
      true,
      testName,
      false,
      false,
      false)
  }
}