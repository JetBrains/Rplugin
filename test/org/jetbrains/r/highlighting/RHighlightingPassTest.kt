/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.highlighting

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPassFactory
import junit.framework.TestCase
import org.jetbrains.r.console.RConsoleBaseTestCase

class RHighlightingPassTest : RConsoleBaseTestCase() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testIdentifierHighlighting() {
    IdentifierHighlighterPassFactory.doWithIdentifierHighlightingEnabled(project, Runnable {
      myFixture.setReadEditorMarkupModel(true)
      myFixture.configureByText("foo.R", """
        library(ggplot2)
        ggplot(mtcars, aes(x=`car name`, y=mpg_z, label=mpg_z))
        ggp<caret>lot(mtcars, aes(x=`car name`, y=mpg_z, label=mpg_z)) 
      """.trimIndent())
      val infos = myFixture.doHighlighting()
      TestCase.assertEquals(2, infos.count{  info -> info.severity === HighlightInfoType.ELEMENT_UNDER_CARET_SEVERITY })
    })
  }
}