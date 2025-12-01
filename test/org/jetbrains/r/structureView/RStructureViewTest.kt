/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.structureView

import com.intellij.lang.LanguageStructureViewBuilder
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.r.psi.psi.RGlobalVariablesFilterId
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RUsefulTestCase

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
       [f1]
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

  fun testCodeSectionsCornerCases() {
    doTest("""
      f1 <- function() {}
      # Test -1 -------
      f2 <- function() {}
      # Test åß∂ç√  ------
      f3 <- function() {}
    """, """
      -test.R
       [f1]
       -Test -1
        f2
       -Test åß∂ç√
        f3
    """, true)
  }


  fun testCurrentStructureElement() {
    myFixture.configureByText("test.R", """
      #<caret> Header 1 ----
      
      <caret>fun1 <- function() {
      }
      
      noCaretHere <- function() {
      }
      
      fun2 <- function () {
        foo()<caret>
      }
    """.trimIndent())
    val builder = LanguageStructureViewBuilder.getInstance().getStructureViewBuilder(myFixture.file)
    val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(myFixture.file.virtualFile)
    val structureView = builder!!.createStructureView(fileEditor, myFixture.project)

    val caretModel = myFixture.editor.caretModel

    val offsets = caretModel.allCarets.map { it.offset }

    val actual = offsets.joinToString(separator = "") {
      caretModel.moveToOffset(it)
      val currentEditorElement = structureView.treeModel.currentEditorElement
      (currentEditorElement as? PsiElement)?.text + "\n***\n"
    }

    UsefulTestCase.assertSameLines("""
      # Header 1 ----
      ***
      fun1 <- function() {
      }
      ***
      fun2 <- function () {
        foo()
      }
      ***
    """.trimIndent(), actual)
  }

  private fun doTest(@Language("RMarkdown") source: String,
                     @Language("RMarkdown") structure: String,
                     showGlobalVariables: Boolean) {
    myFixture.configureByText("test.R", source.trimIndent())
    myFixture.testStructureView { svc ->
      svc.setActionActive(RGlobalVariablesFilterId, !showGlobalVariables)
      PlatformTestUtil.expandAll(svc.tree)
      PlatformTestUtil.waitForPromise(svc.select(svc.treeModel.currentEditorElement, false))
      UsefulTestCase.assertSameLines(structure.trimIndent(), PlatformTestUtil.print(svc.tree, true))
    }
  }
}