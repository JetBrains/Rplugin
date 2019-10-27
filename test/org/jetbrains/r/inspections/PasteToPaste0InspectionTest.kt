/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.r.inspections

class PasteToPaste0InspectionTest : RInspectionTest() {

  override fun setUp() {
    super.setUp()
    addLibraries()
  }

  fun testSingleQuotes() {
    doReplacementTest("paste('aa', 'b', sep = '')", "paste0('aa', 'b')")
  }

  fun testDoubleQuotes() {
    doReplacementTest("paste(\"aa\", 'b', sep = \"\")", "paste0(\"aa\", 'b')")
  }

  fun testSepInTheMiddle() {
    doReplacementTest("paste('aa', 'b', sep = '', 'da', collapse = ', ')", "paste0('aa', 'b', 'da', collapse = ', ')")
  }

  fun testInnerCall() {
    doReplacementTest("substr(paste('a', 'b', sep = ''), 2, 2)", "substr(paste0('a', 'b'), 2, 2)")
  }

  fun testQualifiedName() {
    doReplacementTest("base::paste('a', sep = '')", "base::paste0('a')")
    doReplacementTest("substr(base::paste(sep = ''), 2, 2)", "substr(base::paste0(), 2, 2)")
  }

  fun testNoWarningForNonEmptySeparator() {
    doReplacementTest("""
      # single quotes
      paste(sep = ' ')
      
      # double quotes
      paste(sep = " ")
      
      # comma
      paste('a', 'b', sep = ', ', 'c')
      
      # qualified
      base::paste('a', 'b', sep = ', ', 'c')
    """.trimIndent())
  }

  fun testOverriddenPaste() {
    doWeakTest()
  }

  override val inspection = PasteToPaste0Inspection::class.java
}