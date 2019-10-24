/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.structureView

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ui.tree.TreeUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RUsefulTestCase
import org.jetbrains.r.psi.RGlobalVariablesFilterId

class RStructureViewTest : RUsefulTestCase() {
  fun testJustFunctions() {
    doTest("""
      func1 <- function() {
        func.internal <- function() {} # do not include internal functions
      }
      func2 <- function() {<caret>
      }
      var <- 10 # do not show variables without filter
      func1 <- function() {
      }
    """, """
      -test.R
       func1
       [func2]
       func1      
    """, false)
  }

  fun testGlobalVariables() {
    doTest("""
      func1 <- function() {
      }
      var1 <- 10
      func2 <- function() {<caret>
        inside <- 5
      }
      var1 <- 20
      var2 <- 13
    """, """
      -test.R
       func1
       var1
       [func2]
       var2
    """, true)
  }

  fun testCodeSections() {
    doTest("""
      f1 <- function() {}
      # Header 1 ----
      f2 <- function() {}
      # Header 2 ====
      f3 <- function() {}
      ### Header 3 ####
      f4 <- function() {}
      # Header 4 ====
      f5 <- function() {}
    """, """
      -test.R
       f1
       -Header 1
        f2
       -Header 2
        f3
       -Header 3
        f4
       -Header 4
        f5
    """, true)
  }

  private fun doTest(@Language("RMarkdown") source: String,
                     @Language("RMarkdown") structure: String,
                     showGlobalVariables: Boolean) {
    myFixture.configureByText("test.R", source.trimIndent())
    myFixture.testStructureView { svc ->
      svc.setActionActive(RGlobalVariablesFilterId, !showGlobalVariables)
      TreeUtil.expandAll(svc.tree)
      PlatformTestUtil.waitForPromise(svc.select(svc.treeModel.currentEditorElement, false))
      UsefulTestCase.assertSameLines(structure.trimIndent(), PlatformTestUtil.print(svc.tree, true))
    }
  }
}