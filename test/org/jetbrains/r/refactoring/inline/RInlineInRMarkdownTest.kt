/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import org.jetbrains.r.RUsefulTestCase

class RInlineInRMarkdownTest : RUsefulTestCase() {

  fun testBlock() {
    doTest("""
      ```{r}
      <caret>a = 10 + 10
      a + a * a
      ```
    """.trimIndent(), """
      ```{r}
      
      10 + 10 + (10 + 10) * (10 + 10)
      ```
    """.trimIndent())
  }

  fun testFunInBlock() {
    doTest("""
      ```{r}
      aa <- 15
      <caret>f <- function(aa, bb) {
        if (T) {
          aa * bb
        }
        else {
          return(aa)
        }
      }
      f(5, 3) * f(10 + 4, 12 + 6)
      ```
    """.trimIndent(), """
      ```{r}
      aa <- 15
      
      aa1 <- 10 + 4
      bb <- 12 + 6
      if (T) {
        result <- aa1 * bb
      }
      else {
        result <- aa1
      }
      if (T) {
        result1 <- 5 * 3
      }
      else {
        result1 <- 5
      }
      result1 * result
      ```
    """.trimIndent())
  }

  fun testDifferentBlocks() {
    doTest("""
      ```{r}
      <caret>a <- 10
      a + 5
      ```
      
      ```{r}
      a + 6
      a <- 15
      a + 7
      ```  
    """.trimIndent(), """
      ```{r}
      
      10 + 5
      ```
      
      ```{r}
      10 + 6
      a <- 15
      a + 7
      ```  
    """.trimIndent())

    doTest("""
      ```{r}
      f <- function(aa) { aa }
      f(10) + 4
      ```
      
      ```{r}
      f(15) + 4
      f <- function(aa) { 0 }
      f(6) + 7
      ```  
    """.trimIndent(),"""
      ```{r}
      
      10 + 4
      ```
      
      ```{r}
      15 + 4
      f <- function(aa) { 0 }
      f(6) + 7
      ```  
    """.trimIndent())
  }

  private fun doTest(text: String, expected: String, inlineThisOnly: Boolean = false, removeDeclaration: Boolean = true) {
    val file = myFixture.configureByText("a.Rmd", text)

    val rule = TargetElementUtil.findTargetElement(myFixture.editor, TargetElementUtil.getInstance().getAllAccepted())
    assertNotNull(rule)
    RInlineAssignmentHandler().withInlineThisOnly(inlineThisOnly).withRemoveDefinition(removeDeclaration)
      .inlineElement(project, myFixture.editor, rule!!)
    assertSameLines(expected, file.text)
  }
}