/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
package org.jetbrains.r.highlighting

import com.intellij.lexer.Lexer
import junit.framework.TestCase
import com.intellij.r.psi.lexer.RLexer

class RHighlightingLexerTest : TestCase() {

  fun testLogicTrue() {
    doTest("TRUE", "TRUE")
  }


  fun testLogicFalse() {
    doTest("FALSE", "FALSE")
  }


  fun testNumeric1() {
    doTest("1", "NUMERIC")
  }


  fun testZeroNumeric() {
    doTest("01234", "NUMERIC")
  }


  fun testNumeric10() {
    doTest("10", "NUMERIC")
  }


  fun testNumericFloat() {
    doTest("0.1", "NUMERIC")
  }


  fun testNumericFloat2() {
    doTest(".2", "NUMERIC")
  }


  fun testNumericExponent() {
    doTest("1e-7", "NUMERIC")
  }


  fun testNumericFloatExponent() {
    doTest("1.2e+7", "NUMERIC")
  }


  fun testNumericHexExponent() {
    doTest("0x1.1p-2", "NUMERIC")
  }


  fun testNumericBinaryExponent() {
    doTest("0x123p456", "NUMERIC")
  }


  fun testNumericHex() {
    doTest("0x1", "NUMERIC")
  }


  fun testInteger1() {
    doTest("1L", "INTEGER")
  }


  fun testIntegerHex() {
    doTest("0x10L", "INTEGER")
  }


  fun testIntegerLong() {
    doTest("1000000L", "INTEGER")
  }


  fun testIntegerExponent() {
    doTest("1e6L", "INTEGER")
  }


  fun testNumericWithWarn() {         // TODO: inspection. Actually, it's numeric one
    doTest("1.1L", "INTEGER")
  }


  fun testNumericWithWarnExp() {      // TODO: inspection. Actually, it's numeric one
    doTest("1e-3L", "INTEGER")
  }


  fun testSyntaxError() {
    doTest("12iL", "COMPLEX", "identifier")
  }


  fun testUnnecessaryDecimalPoint() {  // TODO: inspection. Unnecessary Decimal Point warning runtime
    doTest("1.L", "INTEGER")
  }


  fun testComplex() {
    doTest("1i", "COMPLEX")
  }


  fun testFloatComplex() {
    doTest("4.1i", "COMPLEX")
  }


  fun testExponentComplex() {
    doTest("1e-2i", "COMPLEX")
  }


  fun testHexLong() {
    doTest("0xFL", "INTEGER")
  }


  fun testSingleQuotedString() {
    doTest("'qwerty'", "STRING")
  }


  fun testDoubleQuotedString() {
    doTest("\"qwerty\"", "STRING")
  }


  fun testEscapeStringDouble() {
    doTest("\"\\\"\"", "STRING")
  }


  fun testEscapeStringSingle() {
    doTest("'\\\''", "STRING")
  }


  fun testEscapeString() {
    doTest("'\\r\\n\\t\\b\\a\\f\\v'", "STRING")
  }


  fun testEscapeOctString() {
    doTest("'\\123'", "STRING")
  }


  fun testEscapeHexString() {
    doTest("'\\x1'", "STRING")
  }


  fun testEscapeUnicodeString() {
    doTest("'\\u1234'", "STRING")
  }


  fun testEscapeBigUnicodeString() {
    doTest("'\\u12345678'", "STRING")
  }


  fun testErrorInString() {             //TODO: inspection. string errors
    doTest("'\\0'", "STRING")
  }


  fun testIdentifier() {
    doTest("a1", "identifier")
  }


  fun testIdentifierDot() {
    doTest("a.1", "identifier")
  }


  fun testIdentifierUnderscore() {
    doTest("a_1", "identifier")
  }


  fun testIdentifierDotDot() {
    doTest("..", "identifier")
  }


  fun testIdentifierDotUnderscore() {
    doTest("._", "identifier")
  }


  fun testIdentifierDotLetter() {
    doTest(".x", "identifier")
  }


  fun testIdentifierDotDigit() {
    doTest(".1", "NUMERIC")
  }


  fun testBackticks() {
    doTest("""`Hello \` ' " world `""", "identifier")
  }

  fun testAssignment() {
    doTest("a <- 42\n", "identifier", "WHITE_SPACE", "<-", "WHITE_SPACE", "NUMERIC", "WHITE_SPACE")
  }


  fun testAssignmentComment() {
    doTest("A <- a * 2  # R is case sensitive\n", "identifier", "WHITE_SPACE", "<-", "WHITE_SPACE", "identifier", "WHITE_SPACE", "*", "WHITE_SPACE", "NUMERIC",
           "WHITE_SPACE", "END_OF_LINE_COMMENT", "WHITE_SPACE")
  }


  fun testPrintFunction() {
    doTest("print(a)\n", "identifier", "(", "identifier", ")", "WHITE_SPACE")
  }


  fun testCat() {
    doTest("cat(A, \"\\n\") # \"84\" is concatenated with \"\\n\"\n", "identifier", "(", "identifier", ",", "WHITE_SPACE", "STRING", ")", "WHITE_SPACE",
           "END_OF_LINE_COMMENT", "WHITE_SPACE")
  }


  fun testDoubleBrackets() {
    doTest("profile[[pnames[pm]]]", "identifier", "[[", "identifier", "[", "identifier", "]", "]]")
  }


  fun testDoubleBracketsSeparated() {
    doTest("return(invisible(dll_list[[ seq_along(dll_list)[ind] ]]))", "identifier", "(", "identifier", "(", "identifier", "[[", "WHITE_SPACE",
           "identifier", "(", "identifier", ")", "[", "identifier", "]", "WHITE_SPACE", "]]", ")", ")")
  }

  fun testIf() {
    doTest("""
        if(A>a) # true, 84 > 42
        {
          cat(A, ">", a, "\n")
        }
      """.trimIndent(),
           "if", "(", "identifier", ">", "identifier", ")", "WHITE_SPACE", "END_OF_LINE_COMMENT", "WHITE_SPACE", "{", "WHITE_SPACE", "identifier",
           "(", "identifier", ",", "WHITE_SPACE", "STRING", ",", "WHITE_SPACE", "identifier", ",", "WHITE_SPACE", "STRING", ")", "WHITE_SPACE", "}")
  }

  private fun doTest(text: String, vararg expectedTokens: String) {
    doLexerTest(text, RLexer(), *expectedTokens)
  }

  private fun doLexerTest(text: String,
                          lexer: Lexer,
                          vararg expectedTokens: String) {

    lexer.start(text)
    val actual = ArrayList<String>()
    while (true) {
      val tokenName = lexer.tokenType?.toString() ?: break
      actual.add(tokenName)
      lexer.advance()
    }
    assertEquals(expectedTokens.toList(), actual)
  }
}
