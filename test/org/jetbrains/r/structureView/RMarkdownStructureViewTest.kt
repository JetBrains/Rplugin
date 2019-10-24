/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.structureView

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ui.tree.TreeUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.r.RUsefulTestCase

class RMarkdownStructureViewTest : RUsefulTestCase() {
  fun testHeadersLadder() {
    doTest("""
      ###### h6
      
      ##### h5
      
      #### h4
      
      ### h3
      
      ```{r}
      print("Hello world")
      ```
      
      ## h2

      ```{r r1}
      print("Hello world")<caret>
      fff <- function() {}
      ```
      
      # h11
      
      ## h22
      
      ```{python python1}
      print("Hello world")
      ```
      
      ### h33
      
      ```{python}
      print("Hello world")
      ```
      
      #### h44
      
      ##### h55
      
      ###### h66
      
      ##### h555
      
      #### h444
      
      ### h333
      
      ## h222
      
      # h111
      
      ## h2222
      
      ### h3333
      
      #### h44444
      
      ##### h5555
      
      ###### h666
    """, """
      -test.rmd
       h6
       h5
       h4
       h3
       unnamed-chunk-1
       [h2]
       -r1
        fff
       -h11
        -h22
         -h33
          -h44
           -h55
            h66
           h555
          h444
         h333
        h222
       python1
       unnamed-chunk-2
       -h111
        -h2222
         -h3333
          -h44444
           -h5555
            h666
    """)
  }

  fun testHeadersUnderBlockquotesAndLists() {
    doTest("""
      # A list
      
      * ## Subsection
        paragraph
        
        * ### Subsubsection
        
          paragraph
        * * #### H4 here
        
      * ## And one more h2
      
      > # Testing under blockquotes
      > Paragraph
      > ## A quoted susbection
      > # One more h1
      
      > ## Should this h2 be under previous section?
      > This is a separate blockquote so probably it does not relate to the previous text at all.
      > ><caret>
      > > # This h1
      > > Is a higher level header but is quoted relative to the previous quote
      > > so it's a child of it.      
    """, """
      -test.rmd
       -A list
        -Subsection
         Subsubsection
         H4 here
        And one more h2
        -Testing under blockquotes
         A quoted susbection
        One more h1
        -[Should this h2 be under previous section?]
         This h1
    """)
  }

  fun testNormalSetextDocument() {
    doTest("""
      Paragraph
      
         Header 1
      =====
      
      Paragraph
      
         Header 2
      ----
      
      Paragraph
      
      Header 1
      =====
      
      Header 2
      ----
      
      
      Header 1
      ====
    """, """
      -test.rmd
       -Header 1
        Header 2
       -Header 1
        Header 2
       Header 1
    """)
  }

  fun testVariablesInDifferentFences() {
    doTest("""
      # Header1
      ```{r hello}
      globalVar <- 10
      foo <- function() {}
      ```
      # Header2
      ```{r world}
      globalVar <- 5
      foo <- function() {}
      ```
    """, """
      -test.rmd
       [Header1]
       -hello
        globalVar
        foo
       Header2
       -world
        globalVar
        foo
    """.trimIndent())
  }

  private fun doTest(@Language("RMarkdown") source: String,
                     @Language("RMarkdown") structure: String) {
    myFixture.configureByText("test.rmd", source.trimIndent())
    myFixture.testStructureView { svc ->
      TreeUtil.expandAll(svc.tree)
      PlatformTestUtil.waitForPromise(svc.select(svc.treeModel.currentEditorElement, false))
      UsefulTestCase.assertSameLines(structure.trimIndent(), PlatformTestUtil.print(svc.tree, true))
    }
  }
}