// Copyright (c) 2017, Holger Brandl, Ekaterina Tuzova
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.codestyle

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLanguage
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.editor.formatting.RCodeStyleSettings

class FormatterTest : RUsefulTestCase() {
  fun testDefaultSpacing() {
    doTest("""
      tab.prior <- table(df[num<0, "campaign.id"])
      tab.prior <- table(df[num < 0,"campaign.id"])
      tab.prior<- table(df[num < 0, "campaign.id"])
      tab.prior<-table(df[num < 0, "campaign.id"])
      total <- sum(x[,1])
      total <- sum(x[ ,1])
      total <- sum (x[1])
      total <- sum(x [1])
      if ( ! TRUE ) print( - 1 )
      hello -> function() {print("Hello!"); 42}
      ~ xxx
      if( debug ){
        hello()
      }
    """, """
      tab.prior <- table(df[num < 0, "campaign.id"])
      tab.prior <- table(df[num < 0, "campaign.id"])
      tab.prior <- table(df[num < 0, "campaign.id"])
      tab.prior <- table(df[num < 0, "campaign.id"])
      total <- sum(x[, 1])
      total <- sum(x[, 1])
      total <- sum(x[1])
      total <- sum(x[1])
      if (!TRUE) print(-1)
      hello -> function() { print("Hello!"); 42 }
      ~xxx
      if (debug) {
        hello()
      }
    """)
  }

  fun testSpaceBeforeFunctionDefinitionParentheses() {
    doOptTest("function() foo()", "function () foo()") { common.SPACE_BEFORE_METHOD_PARENTHESES = it }
  }

  fun testSpaceBeforeFunctionCallParentheses() {
    doOptTest("foo(1, 2)", "foo (1, 2)") { common.SPACE_BEFORE_METHOD_CALL_PARENTHESES = it }
  }

  fun testSpaceBeforeIfParentheses() {
    doOptTest("if(debug) foo()", "if (debug) foo()") { common.SPACE_BEFORE_IF_PARENTHESES = it }
  }

  fun testSpaceBeforeWhileParentheses() {
    doOptTest("while(foo()) boo()", "while (foo()) boo()") { common.SPACE_BEFORE_WHILE_PARENTHESES = it }
  }

  fun testSpaceBeforeForParentheses() {
    doOptTest("for(i in range) foo(i)", "for (i in range) foo(i)") { common.SPACE_BEFORE_FOR_PARENTHESES = it }
  }

  fun testSpaceAroundAssignment() {
    doOptTest("x<-10", "x <- 10") { common.SPACE_AROUND_ASSIGNMENT_OPERATORS = it }
  }

  fun testSpaceAroundParameterAssignment() {
    doOptTest("function(x=10) x + 1", "function(x = 10) x + 1") { common.SPACE_AROUND_ASSIGNMENT_OPERATORS = it }
  }

  fun testSpaceAroundArgumentAssignment() {
    doOptTest("foo(x=10)", "foo(x = 10)") { common.SPACE_AROUND_ASSIGNMENT_OPERATORS = it }
  }

  fun testSpaceAroundBinaryTilde() {
    doOptTest("x <- a~b", "x <- a ~ b") { custom.SPACE_AROUND_BINARY_TILDE_OPERATOR = it }
  }

  fun testSpaceAroundCompare() {
    doOptTest("x <- a<=b", "x <- a <= b") { common.SPACE_AROUND_RELATIONAL_OPERATORS = it }
  }

  fun testSpaceAroundDisjunction() {
    doOptTest("x <- a||b", "x <- a || b") { custom.SPACE_AROUND_DISJUNCTION_OPERATORS = it }
  }

  fun testSpaceAroundConjunction() {
    doOptTest("x <- a&&b", "x <- a && b") { custom.SPACE_AROUND_CONJUNCTION_OPERATORS = it }
  }

  fun testSpaceAroundAdditive() {
    doOptTest("x <- a+b", "x <- a + b") { common.SPACE_AROUND_ADDITIVE_OPERATORS = it }
  }

  fun testSpaceAroundMultiplicative() {
    doOptTest("x <- a*b", "x <- a * b") { common.SPACE_AROUND_MULTIPLICATIVE_OPERATORS = it }
  }

  fun testSpaceAroundInfix() {
    doOptTest("x <- a%%b", "x <- a %% b") { custom.SPACE_AROUND_INFIX_OPERATOR = it }
  }

  fun testSpaceAroundColon() {
    doOptTest("x <- a:b", "x <- a : b") { custom.SPACE_AROUND_COLON_OPERATOR = it }
  }

  fun testSpaceAroundExponentiation() {
    doOptTest("x <- a^b", "x <- a ^ b") { custom.SPACE_AROUND_EXPONENTIATION_OPERATOR = it }
  }

  fun testSpaceAroundSubset() {
    doOptTest("x <- a${'$'}b", "x <- a ${'$'} b") { custom.SPACE_AROUND_SUBSET_OPERATOR = it }
  }

  fun testSpaceAroundAt() {
    doOptTest("x <- a@b", "x <- a @ b") { custom.SPACE_AROUND_AT_OPERATOR = it }
  }

  fun testSpaceAroundUnary() {
    doOptTest("x <- -a", "x <- - a") { common.SPACE_AROUND_UNARY_OPERATOR = it }
  }

  fun testSpaceBeforeFunctionLBrace() {
    doOptTest("function(){ }", "function() { }") { common.SPACE_BEFORE_METHOD_LBRACE = it }
  }

  fun testSpaceBeforeIfLBrace() {
    doOptTest("if (debug){ }", "if (debug) { }") { common.SPACE_BEFORE_IF_LBRACE = it }
  }

  fun testSpaceBeforeWhileLBrace() {
    doOptTest("while (foo()){ }", "while (foo()) { }") { common.SPACE_BEFORE_WHILE_LBRACE = it }
  }

  fun testSpaceBeforeForLBrace() {
    doOptTest("for (i in range){ }", "for (i in range) { }") { common.SPACE_BEFORE_FOR_LBRACE = it }
  }

  fun testSpaceBeforeRepeatLBrace() {
    doOptTest("repeat{ foo() }", "repeat { foo() }") { custom.SPACE_BEFORE_REPEAT_LBRACE = it }
  }

  fun testSpaceBeforeLBracket() {
    doOptTest("x[1]", "x [1]") { custom.SPACE_BEFORE_LEFT_BRACKET = it }
  }

  fun testSpaceWithinBraces() {
    doOptTest("if (debug) {x}", "if (debug) { x }") { common.SPACE_WITHIN_BRACES = it }
  }

  fun testSpaceWithinBrackets() {
    doOptTest("x[1]", "x[ 1 ]") { common.SPACE_WITHIN_BRACKETS = it }
  }

  fun testSpaceWithinParentheses() {
    doOptTest("f(1, 2)", "f( 1, 2 )") { common.SPACE_WITHIN_PARENTHESES = it }
  }

  fun testSpaceAfterComma() {
    doOptTest("f(1,2)", "f(1, 2)") { common.SPACE_AFTER_COMMA = it }
  }

  fun testSpaceBeforeComma() {
    doOptTest("f(1, 2)", "f(1 , 2)") { common.SPACE_BEFORE_COMMA = it }
  }

  fun testArgumentsAlignment() {
    doOptTest("""
      someMethod(argument1,
          argument2, argument3,
          argument4)
    """, """
      someMethod(argument1,
                 argument2, argument3,
                 argument4)
    """) { common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = it }
  }

  fun testArgumentsAlignmentR344() {
    // Note: Kotlin has same formatting rules :)
    doOptTest("""
      someMethod(argument1, (function(a) {
        a
      })("A very long line"), argument4,
          argument5)
    """, """
      someMethod(argument1, (function(a) {
        a
      })("A very long line"), argument4,
                 argument5)
    """) { common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = it }
  }

  fun testAssignmentAlignmentInParameters() {
    doOptTest("""
      foo(a = 1,
          bb = 10,
          ccc = 100)
    """, """
      foo(a   = 1,
          bb  = 10,
          ccc = 100)
    """) { custom.ALIGN_ASSIGNMENT_OPERATORS = it }
  }

  fun testAssignmentsInRowAlignment() {
    doOptTest("""
      a <- 10 # just some comment
      bb <- 20
      print(a + bb) # separator
      ccc <- 30 +
          a # this line is not a separator (it is continue of the assignment expression)
      dddd <- 50 # empty line will be separator
      
      zzzzzzzzzzzzz = 20
      ppp = (a <- 20)
      
      x <- 10
      yy <- 200
      f <- function() 10 # function declaration is considered as separator
      zzz <- 10000
      a <- 2
      
      func <- function() {
        x <- 10
        yyy <- 200
        zz <- 1
      }
    """, """
      a  <- 10 # just some comment
      bb <- 20
      print(a + bb) # separator
      ccc  <- 30 +
          a # this line is not a separator (it is continue of the assignment expression)
      dddd <- 50 # empty line will be separator
      
      zzzzzzzzzzzzz = 20
      ppp           = (a <- 20)
      
      x  <- 10
      yy <- 200
      f <- function() 10 # function declaration is considered as separator
      zzz <- 10000
      a   <- 2
      
      func <- function() {
        x   <- 10
        yyy <- 200
        zz  <- 1
      }
    """) { custom.ALIGN_ASSIGNMENT_OPERATORS = it }
  }

  fun testParametersAlignment() {
    doOptTest("""
      someMethod <- function(parameter1,
          parameter2, parameter3,
          parameter4) {
        parameter1 + parameter4
      }
    """, """
      someMethod <- function(parameter1,
                             parameter2, parameter3,
                             parameter4) {
        parameter1 + parameter4
      }
    """) { common.ALIGN_MULTILINE_PARAMETERS = it }
  }

  fun testContinuationIndentInFunctionParameters() {
    doTest("""
      x <- function(a,
      b
      ,c
      ) {
        a + b +
        c
      }
    """, """
      x <- function(a,
          b
          , c
      ) {
        a + b +
            c
      }
    """) { common.ALIGN_MULTILINE_PARAMETERS = false }
  }

  fun testLongPipeWrap() {
    doTest("""
      someVariable = bar %>% mutate() %>% group_by() %>% filter() %>% head
    """, """
      someVariable = bar %>%
          mutate() %>%
          group_by() %>%
          filter() %>%
          head
    """)
  }

  fun testGgChainWrap() {
    doTest("""
      require(ggplot2)

      iris %>% ggplot()  + geom_point() +ggtitle("iris plot")+ facet_grid(~Species) + scale_x_log10()
    """, """
      require(ggplot2)
      
      iris %>% ggplot() +
          geom_point() +
          ggtitle("iris plot") +
          facet_grid(~Species) +
          scale_x_log10()
    """)
  }

  private fun doOptTest(@Language("R") falseText: String,
                        @Language("R") trueText: String,
                        config: SCtx.(parameter: Boolean) -> Unit) {
    myFixture.configureByText(RFileType, "")
    doCheck(trueText , falseText) { config(false) }
    doCheck(falseText, trueText ) { config(true ) }

    // Just for sure:
    doCheck(falseText, falseText) { config(false) }
    doCheck(trueText , trueText ) { config(true ) }
  }

  private fun doTest(@Language("R") fileText: String,
                     @Language("R") expected: String,
                     config: SCtx.() -> Unit) {
    myFixture.configureByText(RFileType, "")
    doCheck(fileText, expected, config)
  }

  private fun doTest(@Language("R") fileText: String,
                     @Language("R") expected: String) {
    doTest(fileText, expected) {}
  }

  private fun doCheck(@Language("R") fileText: String,
                     @Language("R") expected: String,
                     config: SCtx.() -> Unit) {
    try {
      val settings: CodeStyleSettings = CodeStyle.getSettings(myFixture.file).clone()
      CodeStyleSettingsManager.getInstance(project).setTemporarySettings(settings)

      val sCtx = SCtx(settings)

      // set different indent settings to test difference between indent and continuation indent
      sCtx.common.indentOptions?.INDENT_SIZE = 2
      sCtx.common.indentOptions?.CONTINUATION_INDENT_SIZE = 4

      config(sCtx)

      WriteCommandAction.runWriteCommandAction(project) {
        myFixture.getDocument(myFixture.file).let {
          it.setText(fileText.trimIndent())
          PsiDocumentManager.getInstance(project).commitAllDocuments()
        }
      }

      WriteCommandAction.runWriteCommandAction(project) {
        val myTextRange = myFixture.file.textRange
        CodeStyleManager.getInstance(myFixture.file.project).reformatText(myFixture.file, myTextRange.startOffset, myTextRange.endOffset)
      }
      myFixture.checkResult(expected.trimIndent())
    }
    finally {
      CodeStyleSettingsManager.getInstance(project).dropTemporarySettings()
    }
  }

  private class SCtx(settings: CodeStyleSettings) {
    val common: CommonCodeStyleSettings = settings.getCommonSettings(RLanguage.INSTANCE)
    val custom: RCodeStyleSettings = settings.getCustomSettings(RCodeStyleSettings::class.java)
  }
}
