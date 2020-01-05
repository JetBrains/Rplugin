/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.resolve

import com.intellij.openapi.fileTypes.LanguageFileType
import junit.framework.TestCase
import org.jetbrains.r.RFileType
import org.jetbrains.r.RLightCodeInsightFixtureTestCase
import org.jetbrains.r.rmarkdown.RMarkdownFileType

class RLocalResolveTest: RLightCodeInsightFixtureTestCase() {

  fun testSimple() {
    doTest("xxx <- 123", """
      func <- function() {
        xxx <- 123
        print(xx<caret>x)
      }
    """.trimIndent())
  }

  fun testParameter() {
    doTest("xxx", """
      func <- function(xxx) {
        print(xx<caret>x)
      }
    """.trimIndent())
  }

  fun testNamedParameter() {
    doTest("xxx = 123", """
      func <- function(xxx = 123) {
        print(xx<caret>x)
      }
    """.trimIndent())
  }

  fun testLocalAndParameter() {
    doTest("xxx = 123", """
      func <- function(xxx = 123) {
        xxx <- 1232312
        print(xx<caret>x)
      }
    """.trimIndent())
  }

  fun testMultipleDefinitions() {
    doTest("xxx <- 1", """
      func <- function() {
        xxx <- 1
        xxx <- 2
        print(xx<caret>x)
      }
    """.trimIndent())
  }

  fun testMultipleDefinitionsFirstAssignment() {
    doTest("", """
      xxx <- 1
      func <- function() {
        x<caret>xx <- 2
        xxx <- 3
        print(xxx)
      }
    """.trimIndent())
  }

  fun testClosure() {
    doTest("xxx <- 1", """
      func <- function() {
        xxx <- 1
        xxx <- 2
        yyy <- function () {
          print(xx<caret>x)
        }
      }
    """.trimIndent())
  }

  fun testClosureInner() {
    doTest("xxx <- 3", """
      func <- function() {
        xxx <- 1
        xxx <- 2
        yyy <- function () {
          xxx <- 3
          print(xx<caret>x)
        }
      }
    """.trimIndent())
  }

  fun testClosureParameter() {
    doTest("xxx", """
      func <- function(xxx) {
        yyy <- function () {
          print(xx<caret>x)
        }
      }
    """.trimIndent())
  }

  fun testFor() {
    doTest("iii", """
      func <- function(xxx) {
        for (iii in 1:10) { print(i<caret>ii) }
      }
    """.trimIndent())
  }

  fun testForInMethod() {
    doTest("le <- 1", """
     func <- function(xxx) {
        for (i in c(1, 5)) {
          le <- 1
          print(l<caret>e)
        }
     }
    """.trimIndent())
  }

  fun testMultipleAssignments() {
    doTest("le <- 1", """
      func <- function(xxx) {
        le <- 1
        if (xxx > 10) {
          l<caret>e <- 20
        }
        print(le)
      }
    """.trimIndent())
  }

  fun testToplevelResolve() {doTest("le <- 1", """
    for (i in c(1, 5)) {
      le <- 1
      print(l<caret>e)   # <-- try to navigate to the definition on the `le`
    }
""".trimIndent())
  }

  fun testCannotFindResults() {
    doTest("df", """
function(x){
  if (!is.list(x)){
    stop("")
  }
  if (!all(unlist(lapply(x, function(df){all(d<caret>f${'$'}values == x[[1]]${'$'}values)})))){
    stop("")
  }
}
    """.trimIndent())
  }

  fun testResolveRMarkdown() {
    doTest("xxx <- function() 123", """
      ```{r}
        xxx <- function() 123
        print(x<caret>xx)
      ```
    """.trimIndent(), RMarkdownFileType)
  }

  fun testUnresolvedNamedAccess() {
    doTest("", """
        xxx <- 232312
        foo${'$'}xx<caret>x
    """.trimIndent())
  }

  fun testNamespaceAccessFirst() {
    doTest("", """
       xxx <- 232312
       x<caret>xx::foo      
    """.trimIndent())
  }

  fun testNamespaceAccessSecond() {
    doTest("", """
       xxx <- 232312
       foo::xx<caret>x      
    """.trimIndent())
  }

  fun testIndexAccess() {
    doTest("", """
      xxx <- 321321
      yyy[[, x<caret>xx == 42 ]]
    """.trimIndent())
  }

  fun testCallInsideSubscription() {
    doTest("xxx <- function() 42", """
          xxx <- function() 42
          yyy[[, x<caret>xx() == zzz ]]
    """.trimIndent())
  }

  fun testMemberAccessInsideSubscription() {
    doTest("xxx <- list(foo = 42, bar = 43)", """
      xxx <- list(foo = 42, bar = 43)
      yyy[[, x<caret>xx${'$'}foo == zzz ]]
    """.trimIndent())
  }

  fun testParameterImportantThanExternalAssignment() {
    doTest("", """
      test_function <- function(pa<caret>ram, y, z, d) {
          print(x + 1 + 1)
      }
      param <- 10
      y <- param + param
    """.trimIndent())

    doTest("param <- 10", """
      test_function <- function(param, y, z, d) {
          print(x + 1 + 1)
      }
      param <- 10
      y <- param + pa<caret>ram
    """.trimIndent())
  }

  private fun doTest(resolveTargetParentText: String, text: String, fileType: LanguageFileType? = RFileType) {
    fileType?.let { myFixture.configureByText(it, text) }
    val results = resolve()
    if (resolveTargetParentText.isBlank()) {
      TestCase.assertEquals(results.size, 0)
      return
    }
    TestCase.assertEquals(results.size, 1)
    val element = results[0].element!!
    TestCase.assertTrue(element.isValid)
    TestCase.assertEquals(resolveTargetParentText, element.text)
  }

}